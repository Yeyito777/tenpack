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

CLIENT_BASE_RESOURCE_PACKS = [
    "vanilla",
    "fabric",
    "mod_resources",
    "moonlight:merged_pack",
    "continuity:default",
    "continuity:glass_pane_culling_fix",
]


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


def read_json_file(path: Path) -> dict | None:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError:
        return None
    except Exception as exc:  # noqa: BLE001
        print(f"warning: could not read {path}: {exc}", file=sys.stderr)
        return None


def write_key_value_lines(path: Path, updates: dict[str, str], initial_lines: list[str] | None = None) -> None:
    if path.exists():
        lines = path.read_text(encoding="utf-8").splitlines()
    else:
        lines = list(initial_lines or [])

    seen: set[str] = set()
    next_lines: list[str] = []
    for line in lines:
        key = line.split(":", 1)[0] if ":" in line else line.split("=", 1)[0]
        if key in updates:
            separator = "=" if "=" in line and ":" not in line else ":"
            next_lines.append(f"{key}{separator}{updates[key]}")
            seen.add(key)
        else:
            next_lines.append(line)

    for key, value in updates.items():
        if key not in seen:
            next_lines.append(f"{key}:{value}")

    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(next_lines) + "\n", encoding="utf-8")


def write_properties(path: Path, updates: dict[str, str], header: list[str] | None = None) -> None:
    if path.exists():
        lines = path.read_text(encoding="utf-8").splitlines()
    else:
        lines = list(header or [])

    seen: set[str] = set()
    next_lines: list[str] = []
    for line in lines:
        stripped = line.strip()
        if not stripped or stripped.startswith("#") or "=" not in line:
            next_lines.append(line)
            continue
        key = line.split("=", 1)[0]
        if key in updates:
            next_lines.append(f"{key}={updates[key]}")
            seen.add(key)
        else:
            next_lines.append(line)

    for key, value in updates.items():
        if key not in seen:
            next_lines.append(f"{key}={value}")

    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(next_lines) + "\n", encoding="utf-8")


def apply_client_visual_defaults(root: Path, dry_run: bool) -> None:
    """Enable Tenpack's default resource-pack stack and shader after sync.

    Minecraft keeps enabled resource packs in options.txt, which is normally
    a local/user file and intentionally ignored by git. Tenpack still needs a
    pack-level visual baseline (Fresh Animations/EMF, connected textures, crop
    models, lantern models, etc.), so patch only the resource-pack keys instead
    of replacing the whole options file.
    """
    resourcepacks = read_json_file(root / "resourcepacks" / "tenpack-resourcepacks.json") or {}
    order = resourcepacks.get("recommendedEnabledOrderLowToHighPriority") or []
    if isinstance(order, list) and order:
        accepted_incompatible = {
            entry.get("filename")
            for entry in resourcepacks.get("resourcepacks", [])
            if isinstance(entry, dict) and entry.get("acceptedIncompatible")
        }
        enabled = CLIENT_BASE_RESOURCE_PACKS + [f"file/{filename}" for filename in order]
        incompatible = [f"file/{filename}" for filename in order if filename in accepted_incompatible]
        print("apply client visual defaults: enable Tenpack resource-pack stack")
        if not dry_run:
            write_key_value_lines(
                root / "options.txt",
                {
                    "resourcePacks": json.dumps(enabled, ensure_ascii=False, separators=(",", ":")),
                    "incompatibleResourcePacks": json.dumps(incompatible, ensure_ascii=False, separators=(",", ":")),
                },
                initial_lines=["version:3955"],
            )

    shaderpacks = read_json_file(root / "shaderpacks" / "tenpack-shaderpacks.json") or {}
    default_shaders = shaderpacks.get("recommendedEnabledByDefault") or []
    shader = default_shaders[0] if isinstance(default_shaders, list) and default_shaders else ""
    if shader:
        print(f"apply client visual defaults: enable shader {shader}")
        if not dry_run:
            write_properties(
                root / "config" / "iris.properties",
                {
                    "allowUnknownShaders": "false",
                    "colorSpace": "SRGB",
                    "disableUpdateMessage": "false",
                    "enableDebugOptions": "false",
                    "enableShaders": "true",
                    "maxShadowRenderDistance": "32",
                    "shaderPack": shader,
                },
                header=[
                    "# Tenpack default Iris shader configuration",
                    "# Players can change this locally, but the sync step reapplies Tenpack defaults before launch.",
                ],
            )


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

    if side == "client":
        apply_client_visual_defaults(root, args.dry_run)

    if not args.dry_run:
        write_state(state_path, args.manifest_url, manifest)

    print(
        f"tenpack sync complete: {changed} changed/downloaded, "
        f"{deleted} stale deleted, {mirrored} mirrored-dir deleted, {skipped} stale modified skipped"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
