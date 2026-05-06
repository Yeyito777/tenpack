#!/usr/bin/env python3
"""Build static HTTP assets for Tenpack file sync.

This scans shared/common plus shared/<side>, writes client/server manifests,
and copies file payloads to a content-addressed public/files tree.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import shutil
from datetime import datetime, timezone
from pathlib import Path


PACK = "tenpack"
MINECRAFT = "1.21.1"
LOADER = "neoforge"
SIDES = ("client", "server")


def sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def collect_files(root: Path, side: str) -> dict[str, Path]:
    files: dict[str, Path] = {}
    for layer in ("common", side):
        base = root / "shared" / layer
        if not base.exists():
            continue
        for path in sorted(base.rglob("*")):
            if not path.is_file():
                continue
            rel = path.relative_to(base).as_posix()
            files[rel] = path
    return files


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
        "generatedAt": datetime.now(timezone.utc).isoformat(),
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
    args = parser.parse_args()

    repo = args.repo.resolve()
    out = args.out if args.out.is_absolute() else repo / args.out

    if not (repo / "shared").exists():
        raise SystemExit(f"shared/ not found under {repo}")

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
    raise SystemExit(main())
