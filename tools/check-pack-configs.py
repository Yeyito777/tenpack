#!/usr/bin/env python3
"""Policy checks for Tenpack non-recipe configs.

These checks encode the current pack philosophy around factions/Create eras:
travel pressure should stay physical, survival pressure should be readable rather
than chore-heavy, death should create interaction without admin bypasses, and
stale configs for uninstalled mods should not ship to clients.
"""

from __future__ import annotations

import json
import sys
import tomllib
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]

ALLOWED_ACTIVE_CONFIG_FILES = {
    "client/config/connector.json",
    "client/config/corpse-server.toml",
    "client/config/createdieselgenerators-common.toml",
    "client/config/do_a_barrel_roll-client.json",
    "client/config/legendarysurvivaloverhaul-client.toml",
    "client/config/legendarysurvivaloverhaul-common.toml",
    "client/config/lifesteal-common.toml",
    "client/config/more_darkness.json",
    "client/config/projectatmosphere/biome_temps.json",
    "client/config/sound_physics_remastered/occlusion.properties",
    "client/config/sound_physics_remastered/reflectivity.properties",
    "client/config/sound_physics_remastered/soundphysics.properties",
    "client/config/sound_physics_remastered/sound_rates.properties",
    "client/config/tenpackdeath.properties",
    "client/config/trmt.json",
    "client/config/voxyworldgenv2.json",
    "client/config/windy-config.json",
    "server/config/corpse-server.toml",
    "server/config/createdieselgenerators-common.toml",
    "server/config/createdieselgenerators-server.toml",
    "server/config/do_a_barrel_roll-server.json",
    "server/config/legendarysurvivaloverhaul-common.toml",
    "server/config/lifesteal-common.toml",
    "server/config/projectatmosphere/biome_temps.json",
    "server/config/sound_physics_remastered/occlusion.properties",
    "server/config/sound_physics_remastered/reflectivity.properties",
    "server/config/sound_physics_remastered/soundphysics.properties",
    "server/config/sound_physics_remastered/sound_rates.properties",
    "server/config/tenpackdeath.properties",
    "server/config/trmt.json",
    "server/config/voxyworldgenv2.json",
    "server/config/windy-config.json",
}

FORBIDDEN_ACTIVE_CONFIG_FRAGMENTS = {
    "improvedmobs": "Improved Mobs is not installed; stale difficulty configs confuse the pack and sync clients unnecessarily",
    "waystones": "teleportation is a hard no",
    "sophisticatedbackpacks": "backpacks are a hard no",
}


def rel(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def read_json(errors: list[str], path: Path) -> Any | None:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except Exception as exc:  # noqa: BLE001
        errors.append(f"invalid JSON {rel(path)}: {exc}")
        return None


def read_toml(errors: list[str], path: Path) -> dict[str, Any]:
    try:
        return tomllib.loads(path.read_text(encoding="utf-8"))
    except Exception as exc:  # noqa: BLE001
        errors.append(f"invalid TOML {rel(path)}: {exc}")
        return {}


def get_nested(obj: Any, keys: list[str]) -> Any:
    cur = obj
    for key in keys:
        if not isinstance(cur, dict) or key not in cur:
            return None
        cur = cur[key]
    return cur


def require_equal(errors: list[str], label: str, actual: Any, expected: Any, reason: str) -> None:
    if actual != expected:
        errors.append(f"{label} expected {expected!r}, got {actual!r}: {reason}")


def parse_properties(path: Path) -> dict[str, str]:
    props: dict[str, str] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        if "=" in line:
            key, value = line.split("=", 1)
            props[key.strip()] = value.strip()
    return props


def check_active_config_allowlist(errors: list[str]) -> None:
    active = {rel(path) for side in ("client", "server") for path in (ROOT / side / "config").rglob("*") if path.is_file()}
    unexpected = active - ALLOWED_ACTIVE_CONFIG_FILES
    missing = ALLOWED_ACTIVE_CONFIG_FILES - active
    for path in sorted(unexpected):
        errors.append(f"unexpected active config file {path}; audit it and add to ALLOWED_ACTIVE_CONFIG_FILES if intentional")
    for path in sorted(missing):
        errors.append(f"missing expected active config file {path}")
    for path in sorted(active):
        lower = path.lower()
        for fragment, reason in FORBIDDEN_ACTIVE_CONFIG_FRAGMENTS.items():
            if fragment in lower:
                errors.append(f"forbidden active config path {path}: {reason}")


def check_travel_and_world_configs(errors: list[str]) -> None:
    trmt = read_json(errors, ROOT / "server/config/trmt.json") or {}
    require_equal(errors, "TRMT player erosion multiplier", get_nested(trmt, ["erosionMultipliers", "player"]), 0.25, "roads should form slowly on foot")
    require_equal(errors, "TRMT mounted erosion multiplier", get_nested(trmt, ["erosionMultipliers", "mounted"]), 0.75, "animal/vehicle traffic should visibly shape routes")

    voxy = read_json(errors, ROOT / "server/config/voxyworldgenv2.json") or {}
    require_equal(errors, "Voxy WorldGen autoStartOnLoad", voxy.get("autoStartOnLoad"), True, "background LoD generation is expected")
    require_equal(errors, "Voxy WorldGen generationRadius", voxy.get("generationRadius"), 128, "known tested radius; changing affects server load")
    require_equal(errors, "Voxy WorldGen maxActiveTasks", voxy.get("maxActiveTasks"), 20, "known tested concurrency; changing affects server load")

    windy = read_json(errors, ROOT / "server/config/windy-config.json") or {}
    require_equal(errors, "Windy minimumWindHeight", windy.get("minimumWindHeight"), 130, "wind should be ambience, not constant ground clutter")

    dabr = read_json(errors, ROOT / "client/config/do_a_barrel_roll-client.json") or {}
    require_equal(errors, "Do a Barrel Roll thrust", get_nested(dabr, ["general", "thrust", "enable_thrust"]), False, "client flight controls must not add personal thrust bypasses")


def check_survival_pressure_configs(errors: list[str]) -> None:
    lso = read_toml(errors, ROOT / "server/config/legendarysurvivaloverhaul-common.toml")
    core = lso.get("core", {}) if isinstance(lso, dict) else {}
    require_equal(errors, "LSO thirst", core.get("Thirst Enabled"), False, "thirst is chore pressure and is disabled")
    require_equal(errors, "LSO health overhaul", core.get("Health Overhaul Enabled"), False, "avoid hidden health-system complexity with Lifesteal")
    require_equal(errors, "LSO temperature", core.get("Temperature Enabled"), True, "temperature is the chosen survival pressure")

    more_darkness = read_json(errors, ROOT / "client/config/more_darkness.json") or {}
    require_equal(errors, "More Darkness caveDarkness", more_darkness.get("caveDarkness"), 0.0, "pitch-black caves are intentional but should be tracked")
    require_equal(errors, "More Darkness disableWithShaders", more_darkness.get("disableWithShaders"), False, "shader users should see the same darkness pressure unless changed intentionally")


def check_death_and_faction_configs(errors: list[str]) -> None:
    corpse = read_toml(errors, ROOT / "server/config/corpse-server.toml")
    require_equal(errors, "Corpse skeleton_time", get_nested(corpse, ["corpse", "skeleton_time"]), 1200, "public-loot marker timing")
    require_equal(errors, "Corpse native only_owner", get_nested(corpse, ["corpse", "access", "only_owner"]), False, "TenpackDeath handles protection silently")
    require_equal(errors, "Corpse force despawn", get_nested(corpse, ["corpse", "despawn", "force_time"]), -1, "full corpses persist until Tenpack decay/break rules")

    death = parse_properties(ROOT / "server/config/tenpackdeath.properties")
    for key, expected, reason in [
        ("ownerProtectionEnabled", "true", "dead player gets initial recovery window"),
        ("publicLootRequiresSkeleton", "true", "corpse skeleton stage is the public-loot signal"),
        ("opsBypassProtection", "false", "admins should not silently bypass faction loot rules"),
        ("decayEnabled", "true", "stale corpses should become pressure over time"),
        ("breakCorpseDropsAfterDecay", "true", "after decay, corpses become contestable"),
    ]:
        require_equal(errors, f"TenpackDeath {key}", death.get(key), expected, reason)

    lifesteal = read_toml(errors, ROOT / "server/config/lifesteal-common.toml")
    require_equal(errors, "Lifesteal death hp transfer", get_nested(lifesteal, ["General Settings", "Number of HitPoints lost/given upon death/kill:"]), 2, "every death is one heart")
    require_equal(errors, "Lifesteal mob hearts", get_nested(lifesteal, ["Misc/Fun", "Killing mobs gives hearts:"]), False, "hearts should come from player stakes, not mob farming")
    require_equal(errors, "Lifesteal enabled", get_nested(lifesteal, ["Lifesteal Related", "Disable Lifesteal:"]), False, "player kills should transfer hearts")


def check_connector_config(errors: list[str]) -> None:
    connector = read_json(errors, ROOT / "client/config/connector.json") or {}
    require_equal(errors, "Connector mixin safeguard", connector.get("enableMixinSafeguard"), True, "mixed NeoForge/Fabric stack should keep safeguard on")
    aliases = connector.get("globalModAliases") or {}
    if not isinstance(aliases, dict):
        errors.append("Connector globalModAliases must be an object")


def main() -> int:
    errors: list[str] = []
    check_active_config_allowlist(errors)
    check_connector_config(errors)
    check_travel_and_world_configs(errors)
    check_survival_pressure_configs(errors)
    check_death_and_faction_configs(errors)

    if errors:
        print("Pack config policy check failed:", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1
    print("Pack config policy checks passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
