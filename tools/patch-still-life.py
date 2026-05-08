#!/usr/bin/env python3
"""Apply Tenpack's Still Life worldgen compatibility patch.

Still Life is distributed as an ARR datapack/mod jar and does not publish a
source repository. For this pack, the datapack JSON inside the jar *is* the
worldgen source we can patch reproducibly.

Patch goals:
- Disable still_life:pf_terrain/red_sand_patch, which repeatedly attempts
  far-chunk writes during feature generation and stalls Voxy-driven chunkgen.
- Keep Still Life installed/enabled; only remove the offending feature.
- Leave Voxy/VoxyWorldGen free to generate at full speed.
"""
from __future__ import annotations

import argparse
import json
import zipfile
from pathlib import Path
from zipfile import ZipInfo, ZIP_DEFLATED

BAD_CONFIGURED = "data/still_life/worldgen/configured_feature/cf_terrain/red_sand_patch.json"
BAD_PLACED = "data/still_life/worldgen/placed_feature/pf_terrain/red_sand_patch.json"
SCORCHED_CAVES = "data/still_life/worldgen/biome/scorched_caves.json"
BAD_FEATURE_ID = "still_life:pf_terrain/red_sand_patch"
PATCH_VERSION = "tenpack.2"

PATCH_NOTE = f"""Tenpack Still Life patch {PATCH_VERSION}

Disabled {BAD_FEATURE_ID}.
Reason: in Still Life 0.1.1 this feature is a very high-count underground
random disk patch that attempts many writes outside the active WorldGenRegion
write radius. Minecraft correctly refuses those writes, but the repeated
far-chunk setBlock attempts produce massive log spam and chunkgen stalls,
especially when Voxy WorldGen is aggressively pre-generating real chunks.

Changes:
- Removed {BAD_FEATURE_ID} from biome still_life:scorched_caves.
- Replaced its configured feature with minecraft:no_op as a safety net.
- Kept Still Life itself enabled.
"""


def as_json_bytes(obj: object) -> bytes:
    return (json.dumps(obj, indent=2, ensure_ascii=False) + "\n").encode("utf-8")


def patch_json(name: str, data: bytes) -> bytes:
    if name == BAD_CONFIGURED:
        return as_json_bytes({"type": "minecraft:no_op", "config": {}})

    if name == BAD_PLACED:
        # Keep the id valid for any accidental references, but make it cheap.
        return as_json_bytes({"feature": "still_life:cf_terrain/red_sand_patch", "placement": []})

    if name == SCORCHED_CAVES:
        obj = json.loads(data.decode("utf-8"))
        removed = 0
        for step in obj.get("features", []):
            if isinstance(step, list):
                before = len(step)
                step[:] = [f for f in step if f != BAD_FEATURE_ID]
                removed += before - len(step)
        if removed == 0:
            raise SystemExit(f"expected to remove {BAD_FEATURE_ID} from {SCORCHED_CAVES}, removed 0")
        return as_json_bytes(obj)

    return data


def copy_info(src: ZipInfo, filename: str | None = None) -> ZipInfo:
    info = ZipInfo(filename or src.filename, date_time=src.date_time)
    info.compress_type = ZIP_DEFLATED
    info.comment = src.comment
    info.extra = src.extra
    info.internal_attr = src.internal_attr
    info.external_attr = src.external_attr
    return info


def patch_jar(src: Path, dst: Path) -> None:
    seen = set()
    with zipfile.ZipFile(src, "r") as zin, zipfile.ZipFile(dst, "w") as zout:
        for info in zin.infolist():
            name = info.filename
            seen.add(name)
            data = zin.read(info) if not info.is_dir() else b""
            if not info.is_dir():
                data = patch_json(name, data)
            zout.writestr(copy_info(info), data)

        missing = {BAD_CONFIGURED, BAD_PLACED, SCORCHED_CAVES} - seen
        if missing:
            raise SystemExit(f"input jar missing expected Still Life paths: {sorted(missing)}")

        note = ZipInfo("META-INF/tenpack-still-life-patch.txt", date_time=(2026, 5, 7, 0, 0, 0))
        note.compress_type = ZIP_DEFLATED
        zout.writestr(note, PATCH_NOTE.encode("utf-8"))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("src", type=Path)
    parser.add_argument("dst", type=Path)
    args = parser.parse_args()
    patch_jar(args.src, args.dst)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
