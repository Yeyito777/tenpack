#!/usr/bin/env python3
"""Check client resourcepack/shaderpack metadata and packaging.

The client visual stack affects accessibility: it can help Create factories,
travel, darkness, and faction identity read better, or it can hide mechanics and
hurt performance. This checker makes the resource/shader choices explicit.
"""

from __future__ import annotations

import json
import sys
import zipfile
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
RESOURCEPACK_DIR = ROOT / "client" / "resourcepacks"
SHADERPACK_DIR = ROOT / "client" / "shaderpacks"
MC = "1.21.1"
MC_RESOURCE_PACK_FORMAT = 34


def read_json(errors: list[str], path: Path) -> Any | None:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except Exception as exc:  # noqa: BLE001
        errors.append(f"invalid JSON {path.relative_to(ROOT)}: {exc}")
        return None


def read_pack_mcmeta(errors: list[str], zip_path: Path) -> dict[str, Any] | None:
    try:
        with zipfile.ZipFile(zip_path) as zf:
            names = set(zf.namelist())
            if "pack.mcmeta" not in names:
                errors.append(f"{zip_path.relative_to(ROOT)} missing top-level pack.mcmeta")
                return None
            return json.loads(zf.read("pack.mcmeta").decode("utf-8-sig", "replace"))
    except Exception as exc:  # noqa: BLE001
        errors.append(f"cannot read {zip_path.relative_to(ROOT)} pack.mcmeta: {exc}")
        return None


def supported_formats_include(meta: dict[str, Any], pack_format: int) -> bool:
    pack = meta.get("pack", {}) if isinstance(meta, dict) else {}
    if pack.get("pack_format") == pack_format:
        return True
    supported = pack.get("supported_formats")
    if isinstance(supported, list) and len(supported) == 2:
        return supported[0] <= pack_format <= supported[1]
    if isinstance(supported, dict):
        min_value = supported.get("min_inclusive", supported.get("min"))
        max_value = supported.get("max_inclusive", supported.get("max"))
        if isinstance(min_value, int) and isinstance(max_value, int):
            return min_value <= pack_format <= max_value
    # Some packs use older overlay fields.
    for min_key, max_key in (("min_format", "max_format"),):
        min_value = pack.get(min_key)
        max_value = pack.get(max_key)
        if isinstance(min_value, int) and isinstance(max_value, int) and min_value <= pack_format <= max_value:
            return True
    return False


def check_resourcepacks(errors: list[str]) -> None:
    metadata_path = RESOURCEPACK_DIR / "tenpack-resourcepacks.json"
    data = read_json(errors, metadata_path)
    if not isinstance(data, dict):
        return
    if data.get("minecraft") != MC:
        errors.append(f"{metadata_path.relative_to(ROOT)} minecraft must be {MC}")
    packs = data.get("resourcepacks")
    if not isinstance(packs, list):
        errors.append("resourcepacks metadata must contain a resourcepacks list")
        return

    actual_zips = {path.name for path in RESOURCEPACK_DIR.glob("*.zip")}
    listed = [pack.get("filename") for pack in packs if isinstance(pack, dict)]
    listed_set = {name for name in listed if isinstance(name, str)}
    if actual_zips != listed_set:
        errors.append(f"resourcepack metadata mismatch: actual={sorted(actual_zips)} listed={sorted(listed_set)}")
    if len(listed) != len(listed_set):
        errors.append("resourcepack metadata has duplicate or invalid filenames")

    order = data.get("recommendedEnabledOrderLowToHighPriority")
    if not isinstance(order, list) or set(order) != listed_set or len(order) != len(listed_set):
        errors.append("resourcepack recommendedEnabledOrderLowToHighPriority must list every pack exactly once")

    for pack in packs:
        if not isinstance(pack, dict):
            continue
        filename = pack.get("filename")
        if not isinstance(filename, str):
            errors.append("resourcepack entry missing filename")
            continue
        for field in ("name", "version", "source", "loadOrderNote", "compatibility", "acceptedIncompatible"):
            if field not in pack:
                errors.append(f"resourcepack {filename} missing metadata field {field}")
        zip_path = RESOURCEPACK_DIR / filename
        meta = read_pack_mcmeta(errors, zip_path)
        if meta is None:
            continue
        compatible = supported_formats_include(meta, MC_RESOURCE_PACK_FORMAT)
        accepted = pack.get("acceptedIncompatible") is True
        if not compatible and not accepted:
            errors.append(f"resourcepack {filename} does not advertise pack format {MC_RESOURCE_PACK_FORMAT}; mark acceptedIncompatible if intentional")
        if compatible and accepted:
            errors.append(f"resourcepack {filename} is compatible with pack format {MC_RESOURCE_PACK_FORMAT} but acceptedIncompatible=true")


def check_shaderpacks(errors: list[str]) -> None:
    metadata_path = SHADERPACK_DIR / "tenpack-shaderpacks.json"
    data = read_json(errors, metadata_path)
    if not isinstance(data, dict):
        return
    if data.get("minecraft") != MC:
        errors.append(f"{metadata_path.relative_to(ROOT)} minecraft must be {MC}")
    packs = data.get("shaderpacks")
    if not isinstance(packs, list):
        errors.append("shaderpack metadata must contain a shaderpacks list")
        return
    actual_zips = {path.name for path in SHADERPACK_DIR.glob("*.zip")}
    listed = [pack.get("filename") for pack in packs if isinstance(pack, dict)]
    listed_set = {name for name in listed if isinstance(name, str)}
    if actual_zips != listed_set:
        errors.append(f"shaderpack metadata mismatch: actual={sorted(actual_zips)} listed={sorted(listed_set)}")
    enabled_default = data.get("recommendedEnabledByDefault")
    if not isinstance(enabled_default, list):
        errors.append("shaderpack metadata needs recommendedEnabledByDefault list")
        enabled_default = []
    for filename in enabled_default:
        if filename not in listed_set:
            errors.append(f"shaderpack recommended default references unknown file {filename}")

    for pack in packs:
        if not isinstance(pack, dict):
            continue
        filename = pack.get("filename")
        if not isinstance(filename, str):
            errors.append("shaderpack entry missing filename")
            continue
        for field in ("name", "version", "source", "recommendedDefault", "performanceWarning", "loadOrderNote"):
            if field not in pack:
                errors.append(f"shaderpack {filename} missing metadata field {field}")
        if pack.get("recommendedDefault") is True and pack.get("performanceWarning") is True:
            errors.append(f"shaderpack {filename} has performanceWarning but is recommendedDefault=true")
        try:
            with zipfile.ZipFile(SHADERPACK_DIR / filename) as zf:
                names = set(zf.namelist())
                if "shaders/shaders.properties" not in names:
                    errors.append(f"shaderpack {filename} missing shaders/shaders.properties")
                if not any(name.startswith("shaders/") for name in names):
                    errors.append(f"shaderpack {filename} has no shaders/ directory")
        except Exception as exc:  # noqa: BLE001
            errors.append(f"cannot read shaderpack {filename}: {exc}")


def main() -> int:
    errors: list[str] = []
    check_resourcepacks(errors)
    check_shaderpacks(errors)
    if errors:
        print("Client asset check failed:", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1
    print("Client asset checks passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
