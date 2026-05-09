#!/usr/bin/env python3
"""Static integrity checks for Tenpack mod jars.

This intentionally stays conservative: it cannot prove Minecraft will launch, but it
catches the easy-to-miss problems that matter while curating a large client/server
NeoForge + Connector pack:

- every server mod jar must also be mirrored to the client;
- required metadata dependencies should be present, including JarJar/nested jars;
- Fabric mods running through Connector should have their direct Fabric deps present;
- client-only jars should not be treated as server omissions.
"""

from __future__ import annotations

import io
import json
import sys
import tomllib
import zipfile
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
SIDES = ("client", "server")
IGNORED_DEP_IDS = {
    "minecraft",
    "java",
    "forge",
    "neoforge",
    "fabricloader",
    "quilt_loader",
}

# Forgified Fabric API is represented to NeoForge as fabric_api, while many
# Fabric mods depend on individual fabric-* module ids.
FABRIC_API_MODULE_PREFIX = "fabric-"


@dataclass
class ModJar:
    side: str
    path: Path
    ids: set[str] = field(default_factory=set)
    provides: set[str] = field(default_factory=set)
    required_deps: list[tuple[str, str]] = field(default_factory=list)
    nested_ids: set[str] = field(default_factory=set)
    parse_errors: list[str] = field(default_factory=list)

    @property
    def all_ids(self) -> set[str]:
        return self.ids | self.provides | self.nested_ids


def _as_list(value: Any) -> list[Any]:
    if value is None:
        return []
    if isinstance(value, list):
        return value
    return [value]


def _required_neoforge_deps(meta: dict[str, Any]) -> list[tuple[str, str]]:
    out: list[tuple[str, str]] = []
    deps = meta.get("dependencies", {}) or {}
    if not isinstance(deps, dict):
        return out
    for owner, dep_entries in deps.items():
        for dep in _as_list(dep_entries):
            if not isinstance(dep, dict):
                continue
            mod_id = dep.get("modId") or dep.get("modid") or dep.get("id")
            if not isinstance(mod_id, str):
                continue
            typ = str(dep.get("type", "")).lower()
            mandatory = dep.get("mandatory") is True
            if typ in {"required", "mandatory"} or mandatory:
                side = str(dep.get("side", "BOTH")).upper() or "BOTH"
                out.append((mod_id, side))
    return out


def _parse_fabric_json(raw: bytes, jar: ModJar, source: str) -> None:
    data = json.loads(raw.decode("utf-8", "replace"))
    mod_id = data.get("id")
    if isinstance(mod_id, str):
        jar.ids.add(mod_id)
    for provided in _as_list(data.get("provides")):
        if isinstance(provided, str):
            jar.provides.add(provided)
    depends = data.get("depends", {}) or {}
    if isinstance(depends, dict):
        for dep_id in depends:
            if isinstance(dep_id, str):
                jar.required_deps.append((dep_id, "CLIENT" if data.get("environment") == "client" else "BOTH"))


def _parse_neoforge_toml(raw: bytes, jar: ModJar) -> None:
    data = tomllib.loads(raw.decode("utf-8", "replace"))
    for mod in _as_list(data.get("mods")):
        if isinstance(mod, dict) and isinstance(mod.get("modId"), str):
            jar.ids.add(mod["modId"])
    jar.required_deps.extend(_required_neoforge_deps(data))


def _parse_nested_ids(raw: bytes) -> set[str]:
    nested = ModJar(side="nested", path=Path("<nested>"))
    try:
        with zipfile.ZipFile(io.BytesIO(raw)) as zf:
            names = set(zf.namelist())
            if "META-INF/neoforge.mods.toml" in names:
                _parse_neoforge_toml(zf.read("META-INF/neoforge.mods.toml"), nested)
            elif "META-INF/mods.toml" in names:
                _parse_neoforge_toml(zf.read("META-INF/mods.toml"), nested)
            if "fabric.mod.json" in names:
                _parse_fabric_json(zf.read("fabric.mod.json"), nested, "nested")
            for name in names:
                if name.endswith(".jar") and (name.startswith("META-INF/jarjar/") or name.startswith("META-INF/jars/")):
                    nested.nested_ids |= _parse_nested_ids(zf.read(name))
    except Exception:
        return set()
    return nested.all_ids


def parse_jar(side: str, path: Path) -> ModJar:
    jar = ModJar(side=side, path=path)
    try:
        with zipfile.ZipFile(path) as zf:
            names = set(zf.namelist())
            if "META-INF/neoforge.mods.toml" in names:
                _parse_neoforge_toml(zf.read("META-INF/neoforge.mods.toml"), jar)
            elif "META-INF/mods.toml" in names:
                _parse_neoforge_toml(zf.read("META-INF/mods.toml"), jar)
            if "fabric.mod.json" in names:
                _parse_fabric_json(zf.read("fabric.mod.json"), jar, path.name)
            for name in names:
                if name.endswith(".jar") and (name.startswith("META-INF/jarjar/") or name.startswith("META-INF/jars/")):
                    jar.nested_ids |= _parse_nested_ids(zf.read(name))
    except Exception as exc:  # noqa: BLE001 - report parser issue with filename
        jar.parse_errors.append(f"{path}: {exc}")
    return jar


def dep_satisfied(dep_id: str, present: set[str]) -> bool:
    if dep_id in IGNORED_DEP_IDS:
        return True
    if dep_id in present:
        return True
    # Common naming differences between Fabric metadata and Forgified API.
    if dep_id == "fabric-api" and "fabric_api" in present:
        return True
    if dep_id.startswith(FABRIC_API_MODULE_PREFIX) and "fabric_api" in present:
        return True
    return False


def main() -> int:
    errors: list[str] = []
    jars_by_side: dict[str, list[ModJar]] = {}
    filenames_by_side: dict[str, set[str]] = {}
    present_by_side: dict[str, set[str]] = {}

    for side in SIDES:
        mod_dir = ROOT / side / "mods"
        jars = [parse_jar(side, path) for path in sorted(mod_dir.glob("*.jar"))]
        jars_by_side[side] = jars
        filenames_by_side[side] = {jar.path.name for jar in jars}
        present = set(IGNORED_DEP_IDS)
        for jar in jars:
            present |= jar.all_ids
            errors.extend(jar.parse_errors)
            if not jar.all_ids:
                errors.append(f"{side}: could not parse any mod id from {jar.path.name}")
        present_by_side[side] = present

    missing_from_client = filenames_by_side["server"] - filenames_by_side["client"]
    for filename in sorted(missing_from_client):
        errors.append(f"server mod is not mirrored to client/mods: {filename}")

    for side, jars in jars_by_side.items():
        for jar in jars:
            for dep_id, dep_side in jar.required_deps:
                dep_side = dep_side.upper()
                if side == "server" and dep_side == "CLIENT":
                    continue
                if not dep_satisfied(dep_id, present_by_side[side]):
                    errors.append(f"{side}: {jar.path.name} requires missing dependency {dep_id} (side={dep_side})")

    print(f"client jars: {len(jars_by_side['client'])}; mod ids/provides incl nested: {len(present_by_side['client'])}")
    print(f"server jars: {len(jars_by_side['server'])}; mod ids/provides incl nested: {len(present_by_side['server'])}")
    client_only = filenames_by_side["client"] - filenames_by_side["server"]
    print(f"client-only jars: {len(client_only)}")

    if errors:
        print("\nMod integrity check failed:", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1

    print("Mod integrity checks passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
