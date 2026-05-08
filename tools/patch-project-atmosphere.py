#!/usr/bin/env python3
"""Apply Tenpack's Project Atmosphere compatibility patch.

Project Atmosphere 0.8.1.0 is ARR and does not publish a source repository, so
Tenpack patches a small set of compiled classes in-place.

Current replacements:
- InstrumentBlockItem: avoid a dedicated-server AbstractMethodError when using
  Barometre/other instrument items.
- SimpleCloudsCompat + EventHandler + SimpleCloudSpawner: fix Atmosphere's
  empty SimpleClouds spawn-region loop by doing real player-region recovery,
  preventing the useless every-tick booster path while SimpleClouds has no spawn
  regions, and keeping async cloud-spawn callbacks safe if the region list empties.
- TornadoRenderHandler: keep Project Atmosphere's tornado renderer from leaking
  a shared level-render PoseStack frame when tornado mesh/shader rendering throws.

Pass a compiled class directory containing these package paths as the third
argument. For backwards compatibility, passing a single class file still patches
only InstrumentBlockItem.
"""
from __future__ import annotations

import argparse
import zipfile
from pathlib import Path
from zipfile import ZipInfo, ZIP_DEFLATED

TARGETS = (
    "net/Gabou/projectatmosphere/items/InstrumentBlockItem.class",
    "net/Gabou/projectatmosphere/compat/SimpleCloudsCompat.class",
    "net/Gabou/projectatmosphere/event/EventHandler.class",
    "net/Gabou/projectatmosphere/manager/SimpleCloudSpawner.class",
    "net/Gabou/projectatmosphere/manager/SimpleCloudSpawner$CloudSpawnRequest.class",
    "net/Gabou/projectatmosphere/client/TornadoRenderHandler.class",
)
INSTRUMENT_TARGET = "net/Gabou/projectatmosphere/items/InstrumentBlockItem.class"
PATCH_NOTE = """Tenpack Project Atmosphere patch tenpack.3

Patched net.Gabou.projectatmosphere.items.InstrumentBlockItem.
Reason: Project Atmosphere 0.8.1.0 crashed the dedicated server with
AbstractMethodError when a player used the Barometre instrument item.
The patched class preserves intended instrument readouts by dispatching the
known instrument subclasses directly through InstrumentUtils and fails closed
for unknown subclasses instead of crashing the server.

Patched net.Gabou.projectatmosphere.compat.SimpleCloudsCompat,
net.Gabou.projectatmosphere.event.EventHandler,
net.Gabou.projectatmosphere.manager.SimpleCloudSpawner, and its cloud-spawn
request record.
Reason: when SimpleClouds reported no spawn regions, Atmosphere's cloud booster
could re-enter the spawn path every tick and spam "No spawn regions available".
The patch seeds weather around real overworld players to recover from the empty
spawn-region cache, skips only the useless empty-region retry loop while
SimpleClouds has no usable regions, and avoids an async spawn callback crash if
the SimpleClouds region list empties between weather sampling and cloud add.

Patched net.Gabou.projectatmosphere.client.TornadoRenderHandler.
Reason: Project Atmosphere's tornado renderer pushed the shared level-render
PoseStack without a try/finally guard. If shader/mesh rendering threw, the caller
would catch and log the original exception but leave a pose frame on the stack,
causing Minecraft to crash later with "Pose stack not empty". The patched class
always restores the pose stack and render cull/blend state after tornado render
attempts.
"""


def copy_info(src: ZipInfo, filename: str | None = None) -> ZipInfo:
    info = ZipInfo(filename or src.filename, date_time=src.date_time)
    info.compress_type = ZIP_DEFLATED
    info.comment = src.comment
    info.extra = src.extra
    info.internal_attr = src.internal_attr
    info.external_attr = src.external_attr
    return info


def load_replacements(path: Path) -> dict[str, bytes]:
    if path.is_file():
        return {INSTRUMENT_TARGET: path.read_bytes()}
    if not path.is_dir():
        raise SystemExit(f"replacement path does not exist: {path}")

    replacements: dict[str, bytes] = {}
    for target in TARGETS:
        class_file = path / target
        if not class_file.is_file():
            raise SystemExit(f"replacement dir missing {target}")
        replacements[target] = class_file.read_bytes()
    return replacements


def patch_jar(src: Path, dst: Path, replacement_path: Path) -> None:
    replacements = load_replacements(replacement_path)
    replaced: set[str] = set()
    with zipfile.ZipFile(src, "r") as zin, zipfile.ZipFile(dst, "w") as zout:
        for info in zin.infolist():
            data = zin.read(info) if not info.is_dir() else b""
            if info.filename in replacements:
                data = replacements[info.filename]
                replaced.add(info.filename)
            if info.filename == "META-INF/tenpack-project-atmosphere-patch.txt":
                continue
            zout.writestr(copy_info(info), data)
        missing = set(replacements) - replaced
        if missing:
            raise SystemExit(f"input jar missing expected class(es): {', '.join(sorted(missing))}")
        note = ZipInfo("META-INF/tenpack-project-atmosphere-patch.txt", date_time=(2026, 5, 8, 0, 0, 0))
        note.compress_type = ZIP_DEFLATED
        zout.writestr(note, PATCH_NOTE.encode("utf-8"))


def main() -> int:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("src", type=Path)
    p.add_argument("dst", type=Path)
    p.add_argument("replacement_path", type=Path, help="compiled class dir, or InstrumentBlockItem.class for legacy one-class patching")
    args = p.parse_args()
    patch_jar(args.src, args.dst, args.replacement_path)
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
