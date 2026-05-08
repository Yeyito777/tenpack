#!/usr/bin/env python3
"""Apply Tenpack's Project Atmosphere compatibility patch.

Project Atmosphere 0.8.1.0 is ARR and does not publish a source repository.
This script patches the distributed jar in the smallest possible way: replace
InstrumentBlockItem.class with a recompiled equivalent that dispatches known
instrument display actions directly instead of invoking the subclass display
method virtually. This avoids an AbstractMethodError server crash observed when
right-clicking the Barometre/other instrument block items.

The replacement class is built from build/pa-patch-src during maintenance; this
script expects the compiled class bytes as an input so the jar rewrite remains
simple and deterministic.
"""
from __future__ import annotations

import argparse
import zipfile
from pathlib import Path
from zipfile import ZipInfo, ZIP_DEFLATED

TARGET = "net/Gabou/projectatmosphere/items/InstrumentBlockItem.class"
PATCH_NOTE = """Tenpack Project Atmosphere patch tenpack.1

Patched net.Gabou.projectatmosphere.items.InstrumentBlockItem.
Reason: Project Atmosphere 0.8.1.0 crashed the dedicated server with
AbstractMethodError when a player used the Barometre instrument item.
The patched class preserves intended instrument readouts by dispatching the
known instrument subclasses directly through InstrumentUtils and fails closed
for unknown subclasses instead of crashing the server.
"""


def copy_info(src: ZipInfo, filename: str | None = None) -> ZipInfo:
    info = ZipInfo(filename or src.filename, date_time=src.date_time)
    info.compress_type = ZIP_DEFLATED
    info.comment = src.comment
    info.extra = src.extra
    info.internal_attr = src.internal_attr
    info.external_attr = src.external_attr
    return info


def patch_jar(src: Path, dst: Path, replacement_class: Path) -> None:
    replacement = replacement_class.read_bytes()
    replaced = False
    with zipfile.ZipFile(src, "r") as zin, zipfile.ZipFile(dst, "w") as zout:
        for info in zin.infolist():
            data = zin.read(info) if not info.is_dir() else b""
            if info.filename == TARGET:
                data = replacement
                replaced = True
            zout.writestr(copy_info(info), data)
        if not replaced:
            raise SystemExit(f"input jar missing expected class {TARGET}")
        note = ZipInfo("META-INF/tenpack-project-atmosphere-patch.txt", date_time=(2026, 5, 7, 0, 0, 0))
        note.compress_type = ZIP_DEFLATED
        zout.writestr(note, PATCH_NOTE.encode("utf-8"))


def main() -> int:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("src", type=Path)
    p.add_argument("dst", type=Path)
    p.add_argument("replacement_class", type=Path)
    args = p.parse_args()
    patch_jar(args.src, args.dst, args.replacement_class)
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
