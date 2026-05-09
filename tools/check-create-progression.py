#!/usr/bin/env python3
"""Sanity checks for Tenpack's Create progression datapack.

This is not a Minecraft datapack validator. It checks the design invariants that are
easy to accidentally break while editing JSON by hand:

- Create Encased machine variants must upgrade from the tuned base Create machine.
- diesel/oil and cannon production must stay behind precision mechanisms.
- controlled-flight recipes must stay behind precision/gyro progression.
- Create Diesel Generators oilfield tags/config must keep rich oil in dry pressure biomes.
"""

from __future__ import annotations

import json
import sys
import tomllib
from pathlib import Path
from typing import Any


DATAPACK_REL = Path("server/datapacks/tenpack-create-progression")
CDG_SERVER_CONFIG_REL = Path("server/config/createdieselgenerators-server.toml")
CDG_COMMON_CONFIG_RELS = [
    Path("client/config/createdieselgenerators-common.toml"),
    Path("server/config/createdieselgenerators-common.toml"),
]
EXPECTED_CREATE_STACK_MODS = {
    "create-1.21.1-6.0.10.jar",
    "sable-neoforge-1.21.1-1.2.2.jar",
    "create-aeronautics-bundled-1.21.1-1.2.1.jar",
    "createdieselgenerators-1.21.1-1.3.11.jar",
    "ritchiesprojectilelib-2.1.2+mc.1.21.1-neoforge.jar",
    "createbigcannons-5.11.3+mc.1.21.1.jar",
    "Create Encased-1.21.1-1.8-ht2.jar",
    "FarmersDelight-1.21.1-1.3.1.jar",
    "CreateDragonsPlus-1.10.0b.jar",
    "create-central-kitchen-2.4.0.jar",
}
EXPECTED_CLIENT_ONLY_CREATE_TOOLS = {
    "jei-1.21.1-neoforge-19.27.0.340-tenpack-mcrangefix.jar",
}

CREATECASING_BASE_MACHINES = {
    "press": "create:mechanical_press",
    "mixer": "create:mechanical_mixer",
    "saw": "create:mechanical_saw",
    "drill": "create:mechanical_drill",
    "encased_fan": "create:encased_fan",
    "harvester": "create:mechanical_harvester",
    "plough": "create:mechanical_plough",
    "portable_storage_interface": "create:portable_storage_interface",
    "deployer": "create:deployer",
    "roller": "create:mechanical_roller",
}

PRECISION_REQUIRED_RECIPES = [
    "data/createdieselgenerators/recipe/crafting/pumpjack_bearing.json",
    "data/createdieselgenerators/recipe/crafting/distillation_controller.json",
    "data/createdieselgenerators/recipe/crafting/diesel_engine.json",
    "data/createbigcannons/recipe/cannon_builder.json",
    "data/createbigcannons/recipe/cannon_drill.json",
    "data/createbigcannons/recipe/cannon_mount.json",
    "data/createbigcannons/recipe/fixed_cannon_mount.json",
    "data/createbigcannons/recipe/cannon_mount_extension.json",
    "data/createbigcannons/recipe/cannon_welder.json",
    "data/minecraft/recipe/cannon_welder_mirrored.json",
]

PRECISION_OR_GYRO_REQUIRED_RECIPES = [
    "data/aeronautics/recipe/propeller_bearing.json",
    "data/aeronautics/recipe/gyroscopic_propeller_bearing.json",
    "data/aeronautics/recipe/smart_propeller.json",
    "data/aeronautics/recipe/steam_vent.json",
    "data/aeronautics/recipe/adjustable_burner.json",
    "data/simulated/recipe/sequenced_assembly/gyroscopic_mechanism.json",
]

OIL_BIOMES_REL = "data/createdieselgenerators/tags/worldgen/biome/oil_biomes.json"
DENY_OIL_BIOMES_REL = "data/createdieselgenerators/tags/worldgen/biome/deny_oil_biomes.json"
OIL_SCANNER_RECIPE_REL = "data/createdieselgenerators/recipe/crafting/oil_scanner.json"
CANISTER_RECIPE_REL = "data/createdieselgenerators/recipe/crafting/canister.json"
OIL_BARREL_RECIPE_REL = "data/createdieselgenerators/recipe/crafting/oil_barrel.json"
DIESEL_FUEL_TYPE_REL = "data/createdieselgenerators/createdieselgenerators/fuel_type/diesel.json"
GASOLINE_FUEL_TYPE_REL = "data/createdieselgenerators/createdieselgenerators/fuel_type/gasoline.json"

REQUIRED_RICH_OILFIELD_BIOMES = {
    "minecraft:desert",
    "minecraft:badlands",
    "minecraft:eroded_badlands",
    "minecraft:wooded_badlands",
    "minecraft:savanna",
    "minecraft:windswept_savanna",
}
OPTIONAL_RICH_OILFIELD_TAGS = {
    # Optional because some biome tags vary by loader/version/modded worldgen.
    "#minecraft:is_badlands",
    "#minecraft:is_savanna",
}
FORBIDDEN_RICH_OIL_BIOMES = {
    "minecraft:plains",
    "minecraft:ocean",
    "minecraft:deep_ocean",
}

REQUIRED_DENIED_OIL_BIOMES = {
    "minecraft:ocean",
    "minecraft:deep_ocean",
    "minecraft:river",
    "minecraft:snowy_plains",
}
OPTIONAL_DENIED_OIL_TAGS = {
    "#minecraft:is_ocean",
    "#minecraft:is_river",
    "#minecraft:is_beach",
}

EXPECTED_CDG_OIL_CONFIG = {
    "Normal oil chunks oil amount multiplier": 0.75,
    "High oil chunks oil amount multiplier": 2.25,
    "Disable high oil chunks": False,
}
EXPECTED_CDG_COMMON_CONFIG = {
    "Capacity of Canisters": 2000,
    "Capacity Addition of Capacity Enchantment in Canisters": 500,
}

EARLY_OIL_SCANNER_REFS = {
    "createdieselgenerators:oil_scanner",
    "create:andesite_alloy",
    "minecraft:clock",
    "c:plates/iron",
    "c:ingots/iron",
}
CANISTER_REFS = {
    "createdieselgenerators:canister",
    "create:andesite_alloy",
    "create:copper_sheet",
    "c:barrels/wooden",
    "c:plates/iron",
}
OIL_BARREL_REFS = {
    "createdieselgenerators:oil_barrel",
    "c:barrels/wooden",
    "c:plates/iron",
}
EXPECTED_FUEL_BURN_RATE = 0.1


def read_json(errors: list[str], path: Path, label: str | None = None) -> Any | None:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except Exception as exc:  # noqa: BLE001 - report all parse failures
        errors.append(f"invalid JSON {label or path}: {exc}")
        return None


def collect_refs(path: Path) -> list[str]:
    try:
        obj = json.loads(path.read_text(encoding="utf-8"))
    except Exception:  # noqa: BLE001 - parse failures are reported by check_all_datapack_json_parses
        return []
    refs: list[str] = []

    def visit(value: Any) -> None:
        if isinstance(value, dict):
            for key, child in value.items():
                if key in {"item", "id", "tag", "items"} and isinstance(child, str):
                    refs.append(child)
                visit(child)
        elif isinstance(value, list):
            for child in value:
                visit(child)

    visit(obj)
    return refs


def tag_values(tag_json: dict[str, Any]) -> set[str]:
    """Return string IDs from a Minecraft tag file.

    Tag values can be plain strings or optional-entry objects such as
    {"id": "#minecraft:is_ocean", "required": false}.
    """
    values: set[str] = set()
    for value in tag_json.get("values", []):
        if isinstance(value, str):
            values.add(value)
        elif isinstance(value, dict) and isinstance(value.get("id"), str):
            values.add(value["id"])
    return values


def require_ref(errors: list[str], root: Path, rel: str, required: str) -> None:
    path = root / rel
    if not path.exists():
        errors.append(f"missing expected file: {rel}")
        return
    if required not in collect_refs(path):
        errors.append(f"{rel} must reference {required}")


def require_any_ref(errors: list[str], root: Path, rel: str, required: set[str]) -> None:
    path = root / rel
    if not path.exists():
        errors.append(f"missing expected file: {rel}")
        return
    refs = set(collect_refs(path))
    if not refs.intersection(required):
        errors.append(f"{rel} must reference one of: {', '.join(sorted(required))}")


def require_tag_entries(errors: list[str], rel: str, actual: set[str], required: set[str], reason: str) -> None:
    missing = required - actual
    if missing:
        errors.append(f"{rel} missing {reason}: {', '.join(sorted(missing))}")


def require_recipe_refs(errors: list[str], root: Path, rel: str, required_refs: set[str], reason: str) -> set[str]:
    path = root / rel
    if not path.exists():
        errors.append(f"missing expected file: {rel}")
        return set()

    refs = set(collect_refs(path))
    missing = required_refs - refs
    if missing:
        errors.append(f"{rel} missing {reason}: {', '.join(sorted(missing))}")
    return refs


def check_all_datapack_json_parses(errors: list[str], root: Path) -> None:
    for path in sorted(root.rglob("*.json")):
        read_json(errors, path, str(path.relative_to(root)))


def check_required_create_mods(errors: list[str], repo: Path) -> None:
    for filename in sorted(EXPECTED_CREATE_STACK_MODS):
        client_mod = repo / "client/mods" / filename
        server_mod = repo / "server/mods" / filename
        if not client_mod.exists():
            errors.append(f"missing expected Create stack client mod: {client_mod.relative_to(repo)}")
        if not server_mod.exists():
            errors.append(f"missing expected Create stack server mod: {server_mod.relative_to(repo)}")

    for filename in sorted(EXPECTED_CLIENT_ONLY_CREATE_TOOLS):
        client_mod = repo / "client/mods" / filename
        if not client_mod.exists():
            errors.append(f"missing expected client Create workflow tool: {client_mod.relative_to(repo)}")


def check_create_encased_variants(errors: list[str], root: Path) -> None:
    # Create Encased variants can otherwise bypass Tenpack's tuned base recipes.
    for category, base_machine in CREATECASING_BASE_MACHINES.items():
        category_dir = root / f"data/createcasing/recipe/crafting/{category}"
        if not category_dir.exists():
            errors.append(f"missing Create Encased category override dir: {category_dir.relative_to(root)}")
            continue
        for path in sorted(category_dir.glob("*.json")):
            if base_machine not in collect_refs(path):
                errors.append(f"{path.relative_to(root)} must reference {base_machine}")


def check_recipe_gates(errors: list[str], root: Path) -> None:
    for rel in PRECISION_REQUIRED_RECIPES:
        require_ref(errors, root, rel, "create:precision_mechanism")

    for rel in PRECISION_OR_GYRO_REQUIRED_RECIPES:
        require_any_ref(errors, root, rel, {"create:precision_mechanism", "simulated:gyroscopic_mechanism"})


def check_oilfield_tags(errors: list[str], root: Path) -> None:
    oil_biomes_path = root / OIL_BIOMES_REL
    if not oil_biomes_path.exists():
        errors.append(f"missing expected file: {OIL_BIOMES_REL}")
    else:
        oil_biomes = read_json(errors, oil_biomes_path, OIL_BIOMES_REL) or {}
        values = tag_values(oil_biomes)
        if oil_biomes.get("replace") is not True:
            errors.append(f"{OIL_BIOMES_REL} must set replace=true so upstream plains/ocean rich oil is removed")
        require_tag_entries(errors, OIL_BIOMES_REL, values, REQUIRED_RICH_OILFIELD_BIOMES, "dry oilfield biomes")
        require_tag_entries(errors, OIL_BIOMES_REL, values, OPTIONAL_RICH_OILFIELD_TAGS, "optional dry biome tags")
        forbidden = values.intersection(FORBIDDEN_RICH_OIL_BIOMES)
        if forbidden:
            errors.append(f"{OIL_BIOMES_REL} must not keep rich oil in plains/oceans: {', '.join(sorted(forbidden))}")

    deny_oil_path = root / DENY_OIL_BIOMES_REL
    if not deny_oil_path.exists():
        errors.append(f"missing expected file: {DENY_OIL_BIOMES_REL}")
    else:
        deny_oil = read_json(errors, deny_oil_path, DENY_OIL_BIOMES_REL) or {}
        denied = tag_values(deny_oil)
        require_tag_entries(errors, DENY_OIL_BIOMES_REL, denied, REQUIRED_DENIED_OIL_BIOMES, "oil-denied biomes")
        require_tag_entries(errors, DENY_OIL_BIOMES_REL, denied, OPTIONAL_DENIED_OIL_TAGS, "optional oil-denied biome tags")


def check_oil_scanner_stays_early(errors: list[str], root: Path) -> None:
    refs = require_recipe_refs(errors, root, OIL_SCANNER_RECIPE_REL, EARLY_OIL_SCANNER_REFS, "early prospecting refs")
    if "create:precision_mechanism" in refs:
        errors.append(f"{OIL_SCANNER_RECIPE_REL} must stay pre-precision; scouting should come before extraction")


def check_oil_logistics_recipes(errors: list[str], root: Path) -> None:
    require_recipe_refs(errors, root, CANISTER_RECIPE_REL, CANISTER_REFS, "logistics refs")

    oil_barrel_path = root / OIL_BARREL_RECIPE_REL
    if not oil_barrel_path.exists():
        errors.append(f"missing expected file: {OIL_BARREL_RECIPE_REL}")
    else:
        recipe = read_json(errors, oil_barrel_path, OIL_BARREL_RECIPE_REL) or {}
        require_recipe_refs(errors, root, OIL_BARREL_RECIPE_REL, OIL_BARREL_REFS, "storage refs")
        result = recipe.get("result", {})
        if result.get("count") != 2:
            errors.append(f"{OIL_BARREL_RECIPE_REL} should craft 2 oil barrels to encourage visible bulk storage")


def check_fuel_type(errors: list[str], root: Path, rel: str, expected_fluid: str) -> None:
    path = root / rel
    if not path.exists():
        errors.append(f"missing expected file: {rel}")
        return
    fuel_type = read_json(errors, path, rel) or {}
    if fuel_type.get("fluid") != expected_fluid:
        errors.append(f"{rel} expected fluid {expected_fluid}, got {fuel_type.get('fluid')!r}")
    for engine in ["normal", "modular", "huge"]:
        burn_rate = fuel_type.get(engine, {}).get("burn_rate")
        if burn_rate != EXPECTED_FUEL_BURN_RATE:
            errors.append(f"{rel} expected {engine}.burn_rate = {EXPECTED_FUEL_BURN_RATE}, got {burn_rate!r}")


def check_oil_fuel_types(errors: list[str], root: Path) -> None:
    check_fuel_type(errors, root, DIESEL_FUEL_TYPE_REL, "#forge:diesel")
    check_fuel_type(errors, root, GASOLINE_FUEL_TYPE_REL, "#forge:gasoline")


def check_oilfield_config(errors: list[str], repo: Path) -> None:
    cdg_config = repo / CDG_SERVER_CONFIG_REL
    if not cdg_config.exists():
        errors.append(f"missing {CDG_SERVER_CONFIG_REL} for oilfield pressure tuning")
    else:
        try:
            config = tomllib.loads(cdg_config.read_text(encoding="utf-8"))
        except Exception as exc:  # noqa: BLE001 - report parse/config failures clearly
            errors.append(f"invalid TOML {CDG_SERVER_CONFIG_REL}: {exc}")
        else:
            oil_config = config.get("Server Configs", {}).get("Oil Config", {})
            for key, expected in EXPECTED_CDG_OIL_CONFIG.items():
                actual = oil_config.get(key)
                if actual != expected:
                    errors.append(f"{CDG_SERVER_CONFIG_REL} expected {key} = {expected!r}, got {actual!r}")

    for common_rel in CDG_COMMON_CONFIG_RELS:
        common_config_path = repo / common_rel
        if not common_config_path.exists():
            errors.append(f"missing {common_rel} for portable fuel tuning")
            continue
        try:
            common_config = tomllib.loads(common_config_path.read_text(encoding="utf-8"))
        except Exception as exc:  # noqa: BLE001 - report parse/config failures clearly
            errors.append(f"invalid TOML {common_rel}: {exc}")
            continue
        common_values = common_config.get("Common Config", {})
        for key, expected in EXPECTED_CDG_COMMON_CONFIG.items():
            actual = common_values.get(key)
            if actual != expected:
                errors.append(f"{common_rel} expected {key} = {expected!r}, got {actual!r}")


def main() -> int:
    repo = Path(__file__).resolve().parents[1]
    root = repo / DATAPACK_REL
    if not root.exists():
        print(f"missing datapack: {root}", file=sys.stderr)
        return 1

    errors: list[str] = []
    check_required_create_mods(errors, repo)
    check_all_datapack_json_parses(errors, root)
    check_create_encased_variants(errors, root)
    check_recipe_gates(errors, root)
    check_oilfield_tags(errors, root)
    check_oil_scanner_stays_early(errors, root)
    check_oil_logistics_recipes(errors, root)
    check_oil_fuel_types(errors, root)
    check_oilfield_config(errors, repo)

    if errors:
        for error in errors:
            print(error, file=sys.stderr)
        return 1

    print("Create progression invariants passed")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
