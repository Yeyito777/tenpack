#!/usr/bin/env python3
"""Build static HTTP assets for Tenpack file sync.

This scans the repo's client/ and server/ directories directly, writes
client/server manifests, and copies file payloads to a content-addressed
public/files tree.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import subprocess
from pathlib import Path


PACK = "tenpack"
MINECRAFT = "1.21.1"
LOADER = "neoforge"
SIDES = ("client", "server")


class BuildError(RuntimeError):
    pass


def sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def git_ignored(repo: Path, path: Path) -> bool:
    rel = path.relative_to(repo).as_posix()
    result = subprocess.run(
        ["git", "check-ignore", "-q", rel],
        cwd=repo,
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
        check=False,
    )
    return result.returncode == 0


def collect_files(repo: Path, side: str) -> dict[str, Path]:
    base = repo / side
    if not base.exists():
        raise BuildError(f"missing required directory: {side}/")

    files: dict[str, Path] = {}
    for path in sorted(base.rglob("*")):
        if not path.is_file():
            continue
        if git_ignored(repo, path):
            continue
        rel = path.relative_to(base).as_posix()
        files[rel] = path
    return files


def validate_server_mods_are_in_client(repo: Path) -> None:
    """Tenpack rule: every server mod jar must exist identically on client."""
    client_mods = repo / "client" / "mods"
    server_mods = repo / "server" / "mods"
    if not server_mods.exists():
        return

    missing: list[str] = []
    mismatched: list[str] = []
    for server_mod in sorted(server_mods.glob("*.jar")):
        if git_ignored(repo, server_mod):
            continue
        client_mod = client_mods / server_mod.name
        if not client_mod.exists():
            missing.append(server_mod.name)
            continue
        if sha256_file(server_mod) != sha256_file(client_mod):
            mismatched.append(server_mod.name)

    if missing or mismatched:
        lines = ["server mods must be mirrored into client/mods before publishing"]
        if missing:
            lines.append("missing from client/mods:")
            lines.extend(f"  - {name}" for name in missing)
        if mismatched:
            lines.append("different contents in client/mods:")
            lines.extend(f"  - {name}" for name in mismatched)
        raise BuildError("\n".join(lines))


def copy_payload(path: Path, out: Path, digest: str) -> str:
    rel = Path("files") / digest[:2] / digest
    dest = out / rel
    if not dest.exists():
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(path, dest)
    return rel.as_posix()


def build_side(repo: Path, out: Path, side: str) -> dict:
    entries = []
    for rel, path in collect_files(repo, side).items():
        digest = sha256_file(path)
        entries.append(
            {
                "path": rel,
                "sha256": digest,
                "size": path.stat().st_size,
                "url": copy_payload(path, out, digest),
            }
        )

    entries.sort(key=lambda e: e["path"])
    manifest = {
        "schema": 1,
        "pack": PACK,
        "side": side,
        "minecraft": MINECRAFT,
        "loader": LOADER,
        "sourceDirectory": side,
        "managedDirectories": ["mods", "resourcepacks", "shaderpacks"],
        "files": entries,
    }
    (out / f"{side}-manifest.json").write_text(
        json.dumps(manifest, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )
    return manifest


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--repo",
        type=Path,
        default=Path(__file__).resolve().parents[1],
        help="tenpack repo root; defaults to this script's parent repo",
    )
    parser.add_argument(
        "--out",
        type=Path,
        default=Path("public"),
        help="output directory to generate; default: public",
    )
    parser.add_argument(
        "--no-clean",
        action="store_true",
        help="do not delete the output directory before generating",
    )
    parser.add_argument(
        "--skip-server-mod-validation",
        action="store_true",
        help="do not enforce that every server mod jar exists identically in client/mods",
    )
    args = parser.parse_args()

    repo = args.repo.resolve()
    out = args.out if args.out.is_absolute() else repo / args.out

    for side in SIDES:
        if not (repo / side).exists():
            raise SystemExit(f"{side}/ not found under {repo}")

    if not args.skip_server_mod_validation:
        validate_server_mods_are_in_client(repo)

    if out.exists() and not args.no_clean:
        shutil.rmtree(out)
    out.mkdir(parents=True, exist_ok=True)

    sync_src = repo / "tools" / "tenpack-sync.py"
    if sync_src.exists():
        shutil.copy2(sync_src, out / "tenpack-sync.py")

    for side in SIDES:
        manifest = build_side(repo, out, side)
        total = sum(file["size"] for file in manifest["files"])
        print(f"wrote {side}-manifest.json: {len(manifest['files'])} files, {total:,} bytes")

    print(f"static sync output: {out}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except BuildError as exc:
        raise SystemExit(f"error: {exc}")
