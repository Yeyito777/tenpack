#!/usr/bin/env python3
"""Sync a Minecraft instance/server root from a Tenpack manifest URL."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import shutil
import sys
import tempfile
from datetime import datetime, timezone
from pathlib import Path, PurePosixPath
from urllib.parse import quote, urljoin, urlsplit, urlunsplit
from urllib.request import urlopen


USER_AGENT = "tenpack-sync/0.1"


def safe_url(url: str) -> str:
    parts = urlsplit(url)
    return urlunsplit(
        (
            parts.scheme,
            parts.netloc,
            quote(parts.path, safe="/%:@"),
            quote(parts.query, safe="=&?/%:@"),
            quote(parts.fragment, safe=""),
        )
    )


def fetch_bytes(url: str) -> bytes:
    request_url = safe_url(url)
    with urlopen(request_url, timeout=60) as response:  # nosec - intentional user-provided URL
        return response.read()


def sha256_file(path: Path) -> str | None:
    if not path.exists() or not path.is_file():
        return None
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def validate_manifest_path(path: str) -> PurePosixPath:
    rel = PurePosixPath(path)
    if rel.is_absolute() or ".." in rel.parts or not rel.parts:
        raise ValueError(f"unsafe manifest path: {path!r}")
    return rel


def local_path(root: Path, manifest_path: str) -> Path:
    rel = validate_manifest_path(manifest_path)
    return root.joinpath(*rel.parts)


def load_state(state_path: Path) -> dict:
    if not state_path.exists():
        return {"files": {}}
    try:
        return json.loads(state_path.read_text(encoding="utf-8"))
    except Exception as exc:  # noqa: BLE001
        print(f"warning: could not read state {state_path}: {exc}", file=sys.stderr)
        return {"files": {}}


def write_state(state_path: Path, manifest_url: str, manifest: dict) -> None:
    state_path.parent.mkdir(parents=True, exist_ok=True)
    files = {entry["path"]: entry["sha256"] for entry in manifest["files"]}
    state = {
        "schema": 1,
        "pack": manifest.get("pack"),
        "side": manifest.get("side"),
        "manifestUrl": manifest_url,
        "syncedAt": datetime.now(timezone.utc).isoformat(),
        "files": files,
    }
    tmp = state_path.with_suffix(".tmp")
    tmp.write_text(json.dumps(state, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    tmp.replace(state_path)


def prune_empty_dirs(root: Path, start: Path) -> None:
    try:
        current = start if start.is_dir() else start.parent
        while current != root and root in current.parents:
            try:
                current.rmdir()
            except OSError:
                break
            current = current.parent
    except Exception:
        return


def delete_stale_managed(root: Path, previous: dict, desired_paths: set[str], dry_run: bool) -> tuple[int, int]:
    deleted = skipped = 0
    for path, old_hash in sorted(previous.get("files", {}).items()):
        if path in desired_paths:
            continue
        dest = local_path(root, path)
        current_hash = sha256_file(dest)
        if current_hash is None:
            continue
        if current_hash != old_hash:
            print(f"skip stale modified file: {path}")
            skipped += 1
            continue
        print(f"delete stale managed file: {path}")
        if not dry_run:
            dest.unlink()
            prune_empty_dirs(root, dest)
        deleted += 1
    return deleted, skipped


def mirror_dirs(root: Path, dirs: list[str], desired_paths: set[str], dry_run: bool) -> int:
    removed = 0
    for dirname in dirs:
        rel_dir = validate_manifest_path(dirname)
        base = root.joinpath(*rel_dir.parts)
        if not base.exists():
            continue
        for path in sorted(base.rglob("*")):
            if not path.is_file():
                continue
            rel = path.relative_to(root).as_posix()
            rel_parts = path.relative_to(base).parts
            if any(part.startswith(".") for part in rel_parts):
                # Some loaders/mods maintain their own hidden cache dirs under
                # managed folders. For example Connector writes generated jars
                # under mods/.connector; deleting that every launch is noisy and
                # can make startup slower. Tenpack never publishes hidden files,
                # so leave hidden local/cache paths alone while mirroring.
                continue
            if rel in desired_paths:
                continue
            print(f"delete unlisted file from mirrored dir: {rel}")
            if not dry_run:
                path.unlink()
                prune_empty_dirs(root, path)
            removed += 1
    return removed


def connector_source_path(cache_name: str) -> str | None:
    """Return the source mods/<jar> path for a Sinytra Connector cache file.

    Connector writes generated artifacts under mods/.connector and names them
    after the original Fabric jar, for example:

      example-1.0.0_mapped_moj_1.21.1.jar
      example-1.0.0$nested-lib_mapped_moj_1.21.1.jar

    The sync intentionally leaves hidden folders alone when mirroring mods/, but
    stale Connector artifacts can otherwise keep removed Fabric mods loadable.
    This helper maps a cache artifact back to its original mods/<jar> entry so
    stale caches can be removed safely.
    """
    marker = "_mapped_moj_"
    if marker not in cache_name:
        return None
    source_stem = cache_name.split(marker, 1)[0].split("$", 1)[0]
    if not source_stem:
        return None
    return f"mods/{source_stem}.jar"


def clean_connector_cache(root: Path, desired_paths: set[str], dry_run: bool) -> int:
    cache_dir = root / "mods" / ".connector"
    if not cache_dir.exists():
        return 0

    removed = 0
    for path in sorted(cache_dir.iterdir()):
        if not path.is_file():
            continue
        source = connector_source_path(path.name)
        if source is None or source in desired_paths:
            continue
        print(f"delete stale Connector cache: {path.relative_to(root).as_posix()} (source {source} is not in manifest)")
        if not dry_run:
            path.unlink()
        removed += 1

    if not dry_run:
        prune_empty_dirs(root, cache_dir)
    return removed


def sync_file(root: Path, manifest_url: str, entry: dict, dry_run: bool) -> bool:
    path = entry["path"]
    expected = entry["sha256"]
    dest = local_path(root, path)
    current = sha256_file(dest)
    if current == expected:
        return False

    source_url = urljoin(manifest_url, entry["url"])
    print(f"sync {path}")
    if dry_run:
        return True

    data = fetch_bytes(source_url)
    actual = hashlib.sha256(data).hexdigest()
    if actual != expected:
        raise RuntimeError(f"hash mismatch for {path}: expected {expected}, got {actual}")
    if len(data) != entry.get("size"):
        raise RuntimeError(f"size mismatch for {path}: expected {entry.get('size')}, got {len(data)}")

    dest.parent.mkdir(parents=True, exist_ok=True)
    fd, tmp_name = tempfile.mkstemp(prefix=dest.name + ".", suffix=".tmp", dir=str(dest.parent))
    try:
        with os.fdopen(fd, "wb") as f:
            f.write(data)
        Path(tmp_name).replace(dest)
    except Exception:
        try:
            Path(tmp_name).unlink(missing_ok=True)
        finally:
            raise
    return True


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("manifest_url", help="URL to client-manifest.json or server-manifest.json")
    parser.add_argument(
        "target",
        nargs="?",
        default=".",
        type=Path,
        help="Minecraft instance/server root; default: current directory",
    )
    parser.add_argument("--dry-run", action="store_true", help="show changes without writing files")
    parser.add_argument(
        "--mirror-dir",
        action="append",
        default=[],
        help="delete unlisted files under this target directory; repeatable, e.g. --mirror-dir mods",
    )
    args = parser.parse_args()

    root = args.target.resolve()
    root.mkdir(parents=True, exist_ok=True)

    manifest = json.loads(fetch_bytes(args.manifest_url).decode("utf-8"))
    if manifest.get("schema") != 1:
        raise SystemExit(f"unsupported manifest schema: {manifest.get('schema')}")

    side = manifest.get("side", "unknown")
    state_path = root / ".tenpack-sync" / f"state-{side}.json"
    previous = load_state(state_path)
    desired_paths = {entry["path"] for entry in manifest["files"]}

    changed = 0
    for entry in manifest["files"]:
        changed += int(sync_file(root, args.manifest_url, entry, args.dry_run))

    deleted, skipped = delete_stale_managed(root, previous, desired_paths, args.dry_run)
    mirrored = mirror_dirs(root, args.mirror_dir, desired_paths, args.dry_run)
    connector_cleaned = clean_connector_cache(root, desired_paths, args.dry_run)

    if not args.dry_run:
        write_state(state_path, args.manifest_url, manifest)

    print(
        f"tenpack sync complete: {changed} changed/downloaded, "
        f"{deleted} stale deleted, {mirrored} mirrored-dir deleted, "
        f"{connector_cleaned} Connector cache deleted, {skipped} stale modified skipped"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
