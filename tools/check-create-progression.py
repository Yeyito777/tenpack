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
import io
import struct
import sys
import tomllib
import zipfile
from pathlib import Path
from typing import Any


DATAPACK_REL = Path("server/world/datapacks/tenpack-create-progression")
OLD_DATAPACK_REL = Path("server/datapacks/tenpack-create-progression")
EXPECTED_PACK_FORMAT = 48
EXPECTED_PACK_ICON_SIZE = (64, 64)
MAX_MECHANICAL_CRAFTING_PATTERN_SIZE = 9
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
    "architectury-13.0.8-neoforge.jar",
    "railways-0.2.0-beta.2+neoforge-mc1.21.1.jar",
    "dragonlib-neoforge-1.21.1-beta-3.0.26.jar",
    "createrailwaysnavigator-neoforge-1.21.1-beta-0.9.0-C6.jar",
    "copycats-3.0.4+mc.1.21.1-neoforge.jar",
    "createdeco-2.1.3.jar",
    "bellsandwhistles-0.4.7-1.21.1.jar",
    "supermartijn642corelib-1.1.21-neoforge-mc1.21.jar",
    "supermartijn642configlib-1.1.8-neoforge-mc1.21.jar",
    "rechiseled-1.2.4-neoforge-mc1.21.jar",
    "rechiseledcreate-1.1.0-neoforge-mc1.21.jar",
    "kotlinforforge-5.11.0-all.jar",
    "sliceanddice-forge-4.2.4.jar",
    "create-confectionery1.21.1_v1.1.2.jar",
    "create_winery-2.0.2-neoforge-1.21.1.jar",
    "create_bic_bit-1.0.2C.jar",
}
EXPECTED_CLIENT_ONLY_CREATE_TOOLS = {
    "jei-1.21.1-neoforge-19.27.0.340-tenpack-mcrangefix.jar",
    "fusion-1.2.12-neoforge-mc1.21.1.jar",
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
    "data/simulated/recipe/sequenced_assembly/engine_assembly.json",
    "data/createdieselgenerators/recipe/crafting/pumpjack_bearing.json",
    "data/createdieselgenerators/recipe/crafting/distillation_controller.json",
    "data/createdieselgenerators/recipe/crafting/diesel_engine.json",
    "data/aeronautics/recipe/mechanical_crafting/mounted_potato_cannon.json",
    "data/createbigcannons/recipe/cannon_builder.json",
    "data/createbigcannons/recipe/cannon_drill.json",
    "data/createbigcannons/recipe/cannon_mount.json",
    "data/createbigcannons/recipe/fixed_cannon_mount.json",
    "data/createbigcannons/recipe/cannon_mount_extension.json",
    "data/createbigcannons/recipe/cannon_welder.json",
    "data/minecraft/recipe/cannon_welder_mirrored.json",
]

DIESEL_HEAVY_INDUSTRY_RECIPES = [
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

# Secondary Aeronautics/Simulated/Offroad support parts can quietly move the
# vehicle/flight feel earlier than intended. These anchors keep the less obvious
# controls, sensors, tires, and physical-force pieces tied to Tenpack's Create
# ladder without turning every decorative recipe into a custom chain.
SECONDARY_VEHICLE_RECIPE_REFS = {
    "data/simulated/recipe/physics_assembler.json": {"create:mechanical_bearing", "create:precision_mechanism"},
    "data/offroad/recipe/tire.json": {"create:belt_connector"},
    "data/offroad/recipe/small_tire.json": {"offroad:tire"},
    "data/offroad/recipe/large_tire.json": {"offroad:tire", "create:belt_connector"},
    "data/offroad/recipe/monstrous_tire.json": {"offroad:large_tire"},
    "data/offroad/recipe/wheel_mount.json": {"create:mechanical_bearing"},
    "data/offroad/recipe/borehead_bearing.json": {"create:precision_mechanism"},
    "data/simulated/recipe/steering_wheel.json": {"create:mechanical_bearing"},
    "data/simulated/recipe/altitude_sensor.json": {"create:electron_tube", "create:brass_casing"},
    "data/simulated/recipe/velocity_sensor.json": {"create:electron_tube", "create:brass_casing"},
    "data/simulated/recipe/laser_sensor.json": {"create:electron_tube", "create:brass_casing"},
    "data/simulated/recipe/throttle_lever.json": {"create:precision_mechanism"},
    "data/simulated/recipe/rope_connector.json": {"create:mechanical_bearing"},
    "data/simulated/recipe/rope_coupling.json": {"create:iron_sheet"},
    "data/simulated/recipe/rope_winch.json": {"simulated:rope_connector"},
    "data/simulated/recipe/swivel_bearing.json": {"create:mechanical_bearing"},
    "data/simulated/recipe/redstone_magnet.json": {"create:electron_tube"},
    "data/aeronautics/recipe/mechanical_crafting/mounted_potato_cannon.json": {
        "simulated:swivel_bearing",
        "simulated:engine_assembly",
    },
}

SMART_LOGISTICS_RECIPE_REFS = {
    "data/create/recipe/crafting/kinetics/mechanical_crafter.json": {
        "create:electron_tube",
        "create:precision_mechanism",
        "create:brass_casing",
    },
    "data/create/recipe/crafting/logistics/packager.json": {
        "create:electron_tube",
        "create:precision_mechanism",
        "create:brass_sheet",
    },
    "data/create/recipe/crafting/logistics/package_frogport.json": {
        "create:precision_mechanism",
        "create:item_vault",
    },
    "data/create/recipe/crafting/logistics/stock_link.json": {
        "create:precision_mechanism",
        "create:item_vault",
        "create:transmitter",
    },
    "data/create/recipe/crafting/logistics/factory_gauge.json": {
        "create:stock_link",
        "create:precision_mechanism",
    },
    "data/create/recipe/crafting/logistics/redstone_requester.json": {
        "create:stock_link",
    },
    "data/create/recipe/crafting/kinetics/track_station.json": {
        "create:precision_mechanism",
        "create:railway_casing",
    },
    "data/create/recipe/crafting/kinetics/track_signal.json": {
        "create:electron_tube",
        "create:railway_casing",
    },
    "data/create/recipe/crafting/kinetics/track_observer.json": {
        "create:precision_mechanism",
        "create:railway_casing",
    },
    "data/create/recipe/crafting/kinetics/track_observer_from_other_plates.json": {
        "create:precision_mechanism",
        "create:railway_casing",
    },
}

ADVANCEMENT_REL = Path("data/tenpack_create/advancement/create_progression")
CREATE_KITCHEN_RECIPE_DIR = Path("data/farmersdelight/recipe/integration/create/mixing")

CREATE_KITCHEN_RECIPES = {
    'data/farmersdelight/recipe/integration/create/mixing/baked_cod_stew_from_mixing.json': 'farmersdelight:baked_cod_stew',
    'data/farmersdelight/recipe/integration/create/mixing/beef_stew_from_mixing.json': 'farmersdelight:beef_stew',
    'data/farmersdelight/recipe/integration/create/mixing/beetroot_soup_from_mixing.json': 'minecraft:beetroot_soup',
    'data/farmersdelight/recipe/integration/create/mixing/bone_broth_from_mixing.json': 'farmersdelight:bone_broth',
    'data/farmersdelight/recipe/integration/create/mixing/chicken_soup_from_mixing.json': 'farmersdelight:chicken_soup',
    'data/farmersdelight/recipe/integration/create/mixing/fish_stew_from_mixing.json': 'farmersdelight:fish_stew',
    'data/farmersdelight/recipe/integration/create/mixing/fried_rice_from_mixing.json': 'farmersdelight:fried_rice',
    'data/farmersdelight/recipe/integration/create/mixing/mushroom_rice_from_mixing.json': 'farmersdelight:mushroom_rice',
    'data/farmersdelight/recipe/integration/create/mixing/mushroom_stew_from_mixing.json': 'minecraft:mushroom_stew',
    'data/farmersdelight/recipe/integration/create/mixing/noodle_soup_from_mixing.json': 'farmersdelight:noodle_soup',
    'data/farmersdelight/recipe/integration/create/mixing/onion_soup_from_mixing.json': 'farmersdelight:onion_soup',
    'data/farmersdelight/recipe/integration/create/mixing/pasta_with_meatballs_from_mixing.json': 'farmersdelight:pasta_with_meatballs',
    'data/farmersdelight/recipe/integration/create/mixing/pasta_with_mutton_chop_from_mixing.json': 'farmersdelight:pasta_with_mutton_chop',
    'data/farmersdelight/recipe/integration/create/mixing/pumpkin_soup_from_mixing.json': 'farmersdelight:pumpkin_soup',
    'data/farmersdelight/recipe/integration/create/mixing/rabbit_stew_from_mixing.json': 'minecraft:rabbit_stew',
    'data/farmersdelight/recipe/integration/create/mixing/ratatouille_from_mixing.json': 'farmersdelight:ratatouille',
    'data/farmersdelight/recipe/integration/create/mixing/squid_ink_pasta_from_mixing.json': 'farmersdelight:squid_ink_pasta',
    'data/farmersdelight/recipe/integration/create/mixing/vegetable_noodles_from_mixing.json': 'farmersdelight:vegetable_noodles',
    'data/farmersdelight/recipe/integration/create/mixing/vegetable_soup_from_mixing.json': 'farmersdelight:vegetable_soup',
}
CREATE_RATION_RECIPES = {
    'data/farmersdelight/recipe/integration/create/mixing/steak_and_potatoes_from_mixing.json': 'farmersdelight:steak_and_potatoes',
}
INTENTIONAL_ADDITIVE_RECIPE_RELS = set(CREATE_KITCHEN_RECIPES) | set(CREATE_RATION_RECIPES)

CREATE_KITCHEN_BASE_INGREDIENT_REFS = {
    'data/farmersdelight/recipe/integration/create/mixing/baked_cod_stew_from_mixing.json': {
        'c:crops/potato',
        'c:crops/tomato',
        'c:eggs',
        'c:foods/raw_cod',
    },
    'data/farmersdelight/recipe/integration/create/mixing/beef_stew_from_mixing.json': {
        'c:crops/carrot',
        'c:crops/potato',
        'c:foods/raw_beef',
    },
    'data/farmersdelight/recipe/integration/create/mixing/beetroot_soup_from_mixing.json': {
        'c:crops/beetroot',
    },
    'data/farmersdelight/recipe/integration/create/mixing/bone_broth_from_mixing.json': {
        'c:bones',
        'c:mushrooms',
        'minecraft:glow_berries',
        'minecraft:glow_lichen',
        'minecraft:hanging_roots',
    },
    'data/farmersdelight/recipe/integration/create/mixing/chicken_soup_from_mixing.json': {
        'c:crops/carrot',
        'c:foods/leafy_green',
        'c:foods/raw_chicken',
        'c:foods/vegetable',
        'minecraft:melon_slice',
    },
    'data/farmersdelight/recipe/integration/create/mixing/fish_stew_from_mixing.json': {
        'c:crops/onion',
        'c:foods/safe_raw_fish',
        'farmersdelight:tomato_sauce',
    },
    'data/farmersdelight/recipe/integration/create/mixing/fried_rice_from_mixing.json': {
        'c:crops/carrot',
        'c:crops/onion',
        'c:crops/rice',
        'c:eggs',
    },
    'data/farmersdelight/recipe/integration/create/mixing/mushroom_rice_from_mixing.json': {
        'c:crops/rice',
        'minecraft:brown_mushroom',
        'minecraft:carrot',
        'minecraft:potato',
        'minecraft:red_mushroom',
    },
    'data/farmersdelight/recipe/integration/create/mixing/mushroom_stew_from_mixing.json': {
        'minecraft:brown_mushroom',
        'minecraft:red_mushroom',
    },
    'data/farmersdelight/recipe/integration/create/mixing/noodle_soup_from_mixing.json': {
        'c:eggs',
        'c:foods/pasta',
        'c:foods/raw_pork',
        'minecraft:dried_kelp',
    },
    'data/farmersdelight/recipe/integration/create/mixing/onion_soup_from_mixing.json': {
        'c:crops/onion',
        'c:drinks/milk',
        'c:foods/bread',
    },
    'data/farmersdelight/recipe/integration/create/mixing/pasta_with_meatballs_from_mixing.json': {
        'c:foods/pasta',
        'farmersdelight:minced_beef',
        'farmersdelight:tomato_sauce',
    },
    'data/farmersdelight/recipe/integration/create/mixing/pasta_with_mutton_chop_from_mixing.json': {
        'c:foods/pasta',
        'c:foods/raw_mutton',
        'farmersdelight:tomato_sauce',
    },
    'data/farmersdelight/recipe/integration/create/mixing/pumpkin_soup_from_mixing.json': {
        'c:drinks/milk',
        'c:foods/leafy_green',
        'c:foods/raw_pork',
        'farmersdelight:pumpkin_slice',
    },
    'data/farmersdelight/recipe/integration/create/mixing/rabbit_stew_from_mixing.json': {
        'c:crops/carrot',
        'c:crops/potato',
        'minecraft:brown_mushroom',
        'minecraft:rabbit',
        'minecraft:red_mushroom',
    },
    'data/farmersdelight/recipe/integration/create/mixing/ratatouille_from_mixing.json': {
        'c:crops/beetroot',
        'c:crops/onion',
        'c:crops/tomato',
        'c:foods/vegetable',
        'minecraft:melon_slice',
    },
    'data/farmersdelight/recipe/integration/create/mixing/squid_ink_pasta_from_mixing.json': {
        'c:crops/tomato',
        'c:foods/pasta',
        'c:foods/safe_raw_fish',
        'minecraft:ink_sac',
    },
    'data/farmersdelight/recipe/integration/create/mixing/vegetable_noodles_from_mixing.json': {
        'c:crops/carrot',
        'c:foods/leafy_green',
        'c:foods/pasta',
        'c:foods/vegetable',
        'c:mushrooms',
        'minecraft:melon_slice',
    },
    'data/farmersdelight/recipe/integration/create/mixing/vegetable_soup_from_mixing.json': {
        'c:crops/beetroot',
        'c:crops/carrot',
        'c:crops/potato',
        'c:foods/leafy_green',
    },
}

CREATE_RATION_BASE_INGREDIENT_REFS = {
    'data/farmersdelight/recipe/integration/create/mixing/steak_and_potatoes_from_mixing.json': {
        'minecraft:bowl',
        'minecraft:baked_potato',
        'minecraft:cooked_beef',
        'c:crops/onion',
        'farmersdelight:cooked_rice',
    },
}

TIRE_RECIPES_FORBID_KELP = [
    "data/offroad/recipe/small_tire.json",
    "data/offroad/recipe/tire.json",
    "data/offroad/recipe/large_tire.json",
    "data/offroad/recipe/monstrous_tire.json",
]

RECIPE_NAMESPACE_MOD_CONDITIONS = {
    "aeronautics": "aeronautics",
    "create": "create",
    "createbigcannons": "createbigcannons",
    "createcasing": "createcasing",
    "createdieselgenerators": "createdieselgenerators",
    "farmersdelight": "farmersdelight",
    "offroad": "offroad",
    "simulated": "simulated",
    "supplementaries": "supplementaries",
}

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
                if key in {"item", "id", "tag", "items", "fluid"} and isinstance(child, str):
                    refs.append(child)
                elif key == "items" and isinstance(child, list):
                    refs.extend(entry for entry in child if isinstance(entry, str))
                visit(child)
        elif isinstance(value, list):
            for child in value:
                visit(child)

    visit(obj)
    return refs


def mod_loaded_conditions(recipe: dict[str, Any]) -> set[str]:
    mods: set[str] = set()
    for condition in recipe.get("neoforge:conditions", []):
        if isinstance(condition, dict) and condition.get("type") == "neoforge:mod_loaded":
            modid = condition.get("modid")
            if isinstance(modid, str):
                mods.add(modid)
    return mods


def namespaced_refs_for_recipe(recipe: dict[str, Any]) -> set[str]:
    refs: set[str] = set()

    def visit(value: Any) -> None:
        if isinstance(value, dict):
            for key, child in value.items():
                if key in {"item", "id", "tag", "items", "fluid"} and isinstance(child, str):
                    refs.add(child)
                elif key == "items" and isinstance(child, list):
                    refs.update(entry for entry in child if isinstance(entry, str))
                visit(child)
        elif isinstance(value, list):
            for child in value:
                visit(child)

    visit(recipe)
    recipe_type = recipe.get("type")
    if isinstance(recipe_type, str):
        refs.add(recipe_type)
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


def is_namespaced(value: Any, *, allow_tag_prefix: bool = True) -> bool:
    if not isinstance(value, str):
        return False
    if value.startswith("#"):
        return allow_tag_prefix and ":" in value[1:]
    return ":" in value


def recipe_stack_refs(errors: list[str], rel: str, label: str, stack: Any) -> set[str]:
    if not isinstance(stack, dict):
        errors.append(f"{rel} {label} must be an object")
        return set()

    item_id = stack.get("id", stack.get("item"))
    if not is_namespaced(item_id, allow_tag_prefix=False):
        errors.append(f"{rel} {label} must include a concrete namespaced id/item")
        return set()

    count = stack.get("count", stack.get("amount", 1))
    if not isinstance(count, int) or count < 1:
        errors.append(f"{rel} {label} count/amount must be a positive integer, got {count!r}")

    return {item_id}


def item_ingredient_refs(errors: list[str], rel: str, label: str, ingredient: Any) -> set[str]:
    """Validate vanilla/NeoForge item ingredient shapes and return referenced ids."""
    if isinstance(ingredient, list):
        if not ingredient:
            errors.append(f"{rel} {label} alternative ingredient list must not be empty")
            return set()
        refs: set[str] = set()
        for index, alternative in enumerate(ingredient):
            if isinstance(alternative, list):
                errors.append(f"{rel} {label}[{index}] should not nest alternative ingredient lists")
                continue
            refs.update(item_ingredient_refs(errors, rel, f"{label}[{index}]", alternative))
        return refs

    if not isinstance(ingredient, dict):
        errors.append(f"{rel} {label} must be an ingredient object or non-empty alternative list")
        return set()

    if "fluid" in ingredient:
        fluid = ingredient.get("fluid")
        if is_namespaced(fluid, allow_tag_prefix=False):
            return {fluid}
        errors.append(f"{rel} {label} fluid ingredient must name a concrete namespaced fluid, got {fluid!r}")
        return set()

    refs: set[str] = set()
    ref_fields = [field for field in ("item", "tag", "items") if field in ingredient]
    if not ref_fields:
        errors.append(f"{rel} {label} must include item, tag, items, or fluid")
        return set()
    if len(ref_fields) > 1:
        errors.append(f"{rel} {label} should not mix ingredient ref fields: {', '.join(ref_fields)}")

    for field in ref_fields:
        value = ingredient[field]
        if isinstance(value, list):
            if not value:
                errors.append(f"{rel} {label}.{field} must not be an empty list")
                continue
            for index, entry in enumerate(value):
                if is_namespaced(entry):
                    refs.add(entry)
                else:
                    errors.append(f"{rel} {label}.{field}[{index}] must be a namespaced id/tag, got {entry!r}")
        elif is_namespaced(value):
            refs.add(value)
        else:
            errors.append(f"{rel} {label}.{field} must be a namespaced id/tag, got {value!r}")

    return refs


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


def check_datapack_folder_names(errors: list[str], root: Path) -> None:
    """Fail on pre-1.21 plural datapack folders in hand-authored content."""
    for path in sorted(root.rglob("*")):
        if not path.is_dir():
            continue
        rel = path.relative_to(root).as_posix()
        if path.name == "recipes":
            errors.append(f"{rel} should be singular 'recipe' for this 1.21 datapack")
        elif path.name == "advancements":
            errors.append(f"{rel} should be singular 'advancement' for this 1.21 datapack")


def check_datapack_metadata(errors: list[str], root: Path) -> None:
    """Keep the datapack visible and valid in Minecraft's datapack UI."""
    pack_meta_path = root / "pack.mcmeta"
    if not pack_meta_path.exists():
        errors.append("Create progression datapack missing pack.mcmeta")
    else:
        pack_meta = read_json(errors, pack_meta_path, "pack.mcmeta") or {}
        pack = pack_meta.get("pack", {}) if isinstance(pack_meta, dict) else {}
        if pack.get("pack_format") != EXPECTED_PACK_FORMAT:
            errors.append(f"pack.mcmeta expected pack_format {EXPECTED_PACK_FORMAT}, got {pack.get('pack_format')!r}")
        description = pack.get("description")
        if not isinstance(description, str) or "Tenpack Create progression" not in description:
            errors.append("pack.mcmeta description should identify the Tenpack Create progression datapack")

    pack_icon_path = root / "pack.png"
    if not pack_icon_path.exists():
        errors.append("Create progression datapack missing generated pack.png icon")
        return
    try:
        data = pack_icon_path.read_bytes()
    except OSError as exc:
        errors.append(f"could not read pack.png: {exc}")
        return
    if len(data) < 33 or data[:8] != b"\x89PNG\r\n\x1a\n":
        errors.append("pack.png must be a valid PNG file")
        return
    try:
        width, height = struct.unpack(">II", data[16:24])
    except struct.error as exc:
        errors.append(f"could not read pack.png dimensions: {exc}")
        return
    if (width, height) != EXPECTED_PACK_ICON_SIZE:
        errors.append(f"pack.png expected {EXPECTED_PACK_ICON_SIZE[0]}x{EXPECTED_PACK_ICON_SIZE[1]}, got {width}x{height}")


def check_pattern_recipe_sanity(errors: list[str], root: Path) -> None:
    """Catch invalid shaped/mechanical-crafting patterns before Minecraft does.

    This datapack has many hand-authored crafting overrides. A bad vanilla
    shaped recipe or Create mechanical-crafting pattern can make a recipe
    silently fail to load and reopen a progression bypass through upstream
    recipes.
    """
    for path in sorted(root.rglob("*.json")):
        recipe = read_json(errors, path, str(path.relative_to(root)))
        recipe_type = recipe.get("type") if isinstance(recipe, dict) else None
        if not isinstance(recipe, dict) or recipe_type not in {"minecraft:crafting_shaped", "create:mechanical_crafting"}:
            continue

        rel = path.relative_to(root).as_posix()
        pattern = recipe.get("pattern")
        key = recipe.get("key")
        if not isinstance(pattern, list) or not pattern or not all(isinstance(row, str) for row in pattern):
            errors.append(f"{rel} shaped recipe must have a non-empty string pattern")
            continue
        if recipe_type == "minecraft:crafting_shaped" and (len(pattern) > 3 or any(len(row) > 3 for row in pattern)):
            errors.append(f"{rel} shaped recipe pattern must fit vanilla 3x3 crafting grid: {pattern!r}")
        widths = {len(row) for row in pattern}
        if len(widths) > 1:
            errors.append(f"{rel} shaped recipe pattern rows must have equal width: {pattern!r}")
        if recipe_type == "create:mechanical_crafting" and (
            len(pattern) > MAX_MECHANICAL_CRAFTING_PATTERN_SIZE
            or any(len(row) > MAX_MECHANICAL_CRAFTING_PATTERN_SIZE for row in pattern)
        ):
            errors.append(
                f"{rel} mechanical crafting pattern should fit a practical "
                f"{MAX_MECHANICAL_CRAFTING_PATTERN_SIZE}x{MAX_MECHANICAL_CRAFTING_PATTERN_SIZE} grid: {pattern!r}"
            )
        if not isinstance(key, dict):
            errors.append(f"{rel} shaped recipe must have a key object")
            continue

        key_symbols = set(key)
        for symbol, ingredient in key.items():
            if not isinstance(symbol, str) or len(symbol) != 1 or symbol == " ":
                errors.append(f"{rel} shaped recipe key symbol {symbol!r} must be one non-space character")
                continue
            item_ingredient_refs(errors, rel, f"key[{symbol!r}]", ingredient)
        used_symbols = {symbol for row in pattern for symbol in row if symbol != " "}
        unused = key_symbols - used_symbols
        missing = used_symbols - key_symbols
        if unused:
            errors.append(f"{rel} shaped recipe has unused key symbols: {', '.join(sorted(unused))}")
        if missing:
            errors.append(f"{rel} shaped recipe has pattern symbols without keys: {', '.join(sorted(missing))}")


def check_shapeless_recipe_sanity(errors: list[str], root: Path) -> None:
    """Catch malformed shapeless ingredient lists before they reopen bypasses."""
    for path in sorted(root.rglob("*.json")):
        rel = path.relative_to(root).as_posix()
        recipe = read_json(errors, path, rel)
        if not isinstance(recipe, dict) or recipe.get("type") != "minecraft:crafting_shapeless":
            continue

        ingredients = recipe.get("ingredients")
        if not isinstance(ingredients, list) or not ingredients:
            errors.append(f"{rel} shapeless recipe must have a non-empty ingredients list")
            continue
        if len(ingredients) > 9:
            errors.append(f"{rel} shapeless recipe has {len(ingredients)} ingredients and must fit a 3x3 crafting grid")

        for index, ingredient in enumerate(ingredients):
            item_ingredient_refs(errors, rel, f"ingredients[{index}]", ingredient)


def check_recipe_output_sanity(errors: list[str], root: Path) -> None:
    """Catch recipes that parse but cannot produce a concrete output.

    Most Tenpack progression edits are recipe overrides. A valid JSON object with
    a typo in `result`/`results` or a missing recipe type can fail at load time and
    quietly expose an upstream addon recipe again.
    """
    for path in sorted(root.rglob("*.json")):
        rel = path.relative_to(root).as_posix()
        if "/recipe/" not in rel:
            continue
        recipe = read_json(errors, path, rel)
        if not isinstance(recipe, dict):
            continue
        if not isinstance(recipe.get("type"), str) or not recipe.get("type"):
            errors.append(f"{rel} recipe must declare a non-empty string type")

        output_keys = [key for key in ("result", "results", "output") if key in recipe]
        if not output_keys:
            errors.append(f"{rel} recipe must declare result/results/output")
            continue
        if len(output_keys) > 1:
            errors.append(f"{rel} recipe should not mix output fields: {', '.join(output_keys)}")

        result_only_types = {"minecraft:crafting_shaped", "minecraft:crafting_shapeless", "create:mechanical_crafting"}
        results_only_types = {"create:mixing", "create:sequenced_assembly"}
        if recipe.get("type") in result_only_types and output_keys != ["result"]:
            errors.append(f"{rel} {recipe.get('type')} must use a single result object")
        if recipe.get("type") in results_only_types and output_keys != ["results"]:
            errors.append(f"{rel} {recipe.get('type')} must use a results list")

        if "results" in recipe:
            results = recipe.get("results")
            if not isinstance(results, list) or not results:
                errors.append(f"{rel} results must be a non-empty list")
            else:
                for index, stack in enumerate(results):
                    recipe_stack_refs(errors, rel, f"results[{index}]", stack)
        elif "result" in recipe:
            recipe_stack_refs(errors, rel, "result", recipe.get("result"))
        elif "output" in recipe:
            recipe_stack_refs(errors, rel, "output", recipe.get("output"))


def check_sequenced_assembly_sanity(errors: list[str], root: Path) -> None:
    """Validate the Create sequenced-assembly structure used for mechanisms."""
    for path in sorted(root.rglob("*.json")):
        rel = path.relative_to(root).as_posix()
        recipe = read_json(errors, path, rel)
        if not isinstance(recipe, dict) or recipe.get("type") != "create:sequenced_assembly":
            continue

        item_ingredient_refs(errors, rel, "ingredient", recipe.get("ingredient"))

        loops = recipe.get("loops")
        if not isinstance(loops, int) or loops < 1:
            errors.append(f"{rel} sequenced assembly loops must be a positive integer, got {loops!r}")

        transitional_refs = recipe_stack_refs(errors, rel, "transitional_item", recipe.get("transitional_item"))
        transitional_id = next(iter(transitional_refs), None)

        sequence = recipe.get("sequence")
        if not isinstance(sequence, list) or not sequence:
            errors.append(f"{rel} sequenced assembly must have a non-empty sequence")
            continue

        for step_index, step in enumerate(sequence):
            label = f"sequence[{step_index}]"
            if not isinstance(step, dict):
                errors.append(f"{rel} {label} must be an object")
                continue
            step_type = step.get("type")
            if not is_namespaced(step_type, allow_tag_prefix=False):
                errors.append(f"{rel} {label}.type must be a namespaced recipe type, got {step_type!r}")

            input_refs: set[str] = set()
            ingredients = step.get("ingredients")
            if not isinstance(ingredients, list) or not ingredients:
                errors.append(f"{rel} {label}.ingredients must be a non-empty list")
            else:
                for ingredient_index, ingredient in enumerate(ingredients):
                    input_refs.update(item_ingredient_refs(errors, rel, f"{label}.ingredients[{ingredient_index}]", ingredient))

            output_refs: set[str] = set()
            results = step.get("results")
            if not isinstance(results, list) or not results:
                errors.append(f"{rel} {label}.results must be a non-empty list")
            else:
                for result_index, result in enumerate(results):
                    output_refs.update(recipe_stack_refs(errors, rel, f"{label}.results[{result_index}]", result))

            if transitional_id:
                if transitional_id not in input_refs:
                    errors.append(f"{rel} {label} must consume transitional item {transitional_id}")
                if transitional_id not in output_refs:
                    errors.append(f"{rel} {label} must return transitional item {transitional_id}")


def check_create_fluid_ingredients(errors: list[str], root: Path) -> None:
    """Validate the fluid ingredient shape used by Create processing recipes."""
    for path in sorted(root.rglob("*.json")):
        rel = path.relative_to(root).as_posix()
        if "/recipe/" not in rel:
            continue
        recipe = read_json(errors, path, rel)
        if not isinstance(recipe, dict):
            continue

        def visit(value: Any, trail: str) -> None:
            if isinstance(value, dict):
                if "fluid" in value:
                    if value.get("type") != "neoforge:single":
                        errors.append(f"{rel} {trail} fluid ingredient should declare type 'neoforge:single'")
                    fluid = value.get("fluid")
                    if not isinstance(fluid, str) or ":" not in fluid or fluid.startswith("#"):
                        errors.append(f"{rel} {trail} fluid ingredient must name a concrete namespaced fluid, got {fluid!r}")
                    amount = value.get("amount")
                    if not isinstance(amount, int) or amount < 1:
                        errors.append(f"{rel} {trail} fluid ingredient amount must be a positive integer, got {amount!r}")
                for key, child in value.items():
                    visit(child, f"{trail}.{key}")
            elif isinstance(value, list):
                for index, child in enumerate(value):
                    visit(child, f"{trail}[{index}]")

        visit(recipe, "$recipe")


def collect_upstream_recipe_paths(errors: list[str], repo: Path) -> set[str]:
    """Return data/*/recipe/*.json paths from installed server mods, including JarJar contents."""
    recipe_paths: set[str] = set()
    seen_labels: set[str] = set()

    def scan_zip(source: Path | io.BytesIO, label: str) -> None:
        if label in seen_labels:
            return
        seen_labels.add(label)
        try:
            with zipfile.ZipFile(source) as archive:
                for name in archive.namelist():
                    parts = name.split("/")
                    if len(parts) >= 4 and parts[0] == "data" and parts[2] == "recipe" and name.endswith(".json"):
                        recipe_paths.add(name)
                    elif name.endswith(".jar"):
                        try:
                            scan_zip(io.BytesIO(archive.read(name)), f"{label}!{name}")
                        except (OSError, zipfile.BadZipFile, KeyError) as exc:
                            errors.append(f"could not scan nested jar {label}!{name}: {exc}")
        except (OSError, zipfile.BadZipFile) as exc:
            errors.append(f"could not scan mod jar {label}: {exc}")

    for jar_path in sorted((repo / "server/mods").glob("*.jar")):
        scan_zip(jar_path, jar_path.relative_to(repo).as_posix())

    return recipe_paths


def check_recipe_override_paths(errors: list[str], repo: Path, root: Path) -> None:
    """Ensure intended recipe overrides actually shadow an installed upstream path."""
    upstream_recipe_paths = collect_upstream_recipe_paths(errors, repo)
    if not upstream_recipe_paths:
        errors.append("could not find upstream server mod recipe paths for override-path validation")
        return

    for path in sorted(root.rglob("*.json")):
        rel = path.relative_to(root).as_posix()
        if "/recipe/" not in rel:
            continue
        if rel in INTENTIONAL_ADDITIVE_RECIPE_RELS:
            continue
        if rel not in upstream_recipe_paths:
            errors.append(f"{rel} does not match an installed upstream recipe path; add to additive allowlist or fix the override path")


def check_recipe_mod_conditions(errors: list[str], root: Path) -> None:
    """Ensure optional Create-addon recipe overrides are gated by loaded mods.

    The progression datapack intentionally contains recipes for optional bundled
    namespaces such as Aeronautics/Simulated/Offroad/Farmer's Delight. Without
    matching NeoForge mod-loaded conditions, removing an addon could make a
    datapack fail noisily instead of degrading cleanly.
    """
    for path in sorted(root.rglob("*.json")):
        rel = path.relative_to(root).as_posix()
        if "/recipe/" not in rel:
            continue
        recipe = read_json(errors, path, rel)
        if not isinstance(recipe, dict):
            continue

        required_mods: set[str] = set()
        for ref in namespaced_refs_for_recipe(recipe):
            if ref.startswith("#") or ":" not in ref:
                continue
            namespace = ref.split(":", 1)[0]
            modid = RECIPE_NAMESPACE_MOD_CONDITIONS.get(namespace)
            if modid:
                required_mods.add(modid)

        missing = required_mods - mod_loaded_conditions(recipe)
        if missing:
            errors.append(f"{rel} missing neoforge:mod_loaded conditions for: {', '.join(sorted(missing))}")


def check_advancement_mod_conditions(errors: list[str], root: Path) -> None:
    """Ensure advancement criteria/icons that reference addon items are gated.

    Recipe overrides already have this guardrail. The advancement tree also
    references optional addon namespaces in inventory triggers and display icons,
    so it should degrade cleanly if a Create addon is removed during a future pass.
    """
    adv_dir = root / ADVANCEMENT_REL
    if not adv_dir.exists():
        # check_advancement_sanity reports the missing directory with a better message.
        return

    for path in sorted(adv_dir.glob("*.json")):
        rel = path.relative_to(root).as_posix()
        advancement = read_json(errors, path, rel)
        if not isinstance(advancement, dict):
            continue

        required_mods: set[str] = set()
        for ref in namespaced_refs_for_recipe(advancement):
            if ref.startswith("#") or ":" not in ref:
                continue
            namespace = ref.split(":", 1)[0]
            modid = RECIPE_NAMESPACE_MOD_CONDITIONS.get(namespace)
            if modid:
                required_mods.add(modid)

        missing = required_mods - mod_loaded_conditions(advancement)
        if missing:
            errors.append(f"{rel} missing neoforge:mod_loaded conditions for advancement refs: {', '.join(sorted(missing))}")


def check_advancement_sanity(errors: list[str], root: Path) -> None:
    """Validate the Tenpack Create advancement tree is internally coherent."""
    adv_dir = root / ADVANCEMENT_REL
    if not adv_dir.exists():
        errors.append(f"missing advancement directory: {ADVANCEMENT_REL}")
        return

    advancement_files = {path.stem: path for path in sorted(adv_dir.glob("*.json"))}
    if "root" not in advancement_files:
        errors.append(f"{ADVANCEMENT_REL} missing root.json")

    parent_by_name: dict[str, str | None] = {}
    for name, path in advancement_files.items():
        rel = path.relative_to(root).as_posix()
        advancement = read_json(errors, path, rel)
        if not isinstance(advancement, dict):
            continue

        criteria = advancement.get("criteria")
        if not isinstance(criteria, dict) or not criteria:
            errors.append(f"{rel} must have non-empty criteria")
            criteria = {}
        for criterion_name, criterion in criteria.items():
            if not isinstance(criterion, dict):
                errors.append(f"{rel} criterion {criterion_name!r} must be an object")
                continue
            if criterion.get("trigger") != "minecraft:inventory_changed":
                errors.append(f"{rel} criterion {criterion_name!r} should stay an inventory item milestone")
            conditions = criterion.get("conditions")
            if not isinstance(conditions, dict):
                errors.append(f"{rel} criterion {criterion_name!r} must have conditions")
                continue
            item_tests = conditions.get("items")
            if not isinstance(item_tests, list) or not item_tests:
                errors.append(f"{rel} criterion {criterion_name!r} must test a non-empty items list")
                continue
            for index, item_test in enumerate(item_tests):
                if not isinstance(item_test, dict):
                    errors.append(f"{rel} criterion {criterion_name!r} items[{index}] must be an object")
                    continue
                item_ref = item_test.get("items", item_test.get("item"))
                if not isinstance(item_ref, str) or ":" not in item_ref:
                    errors.append(f"{rel} criterion {criterion_name!r} items[{index}] must include a namespaced item ref")

        requirements = advancement.get("requirements")
        mentioned_criteria: set[str] = set()
        if not isinstance(requirements, list) or not requirements:
            errors.append(f"{rel} must have non-empty requirements")
        else:
            for group in requirements:
                if not isinstance(group, list) or not group or not all(isinstance(entry, str) for entry in group):
                    errors.append(f"{rel} requirements entries must be non-empty string lists")
                    continue
                for entry in group:
                    mentioned_criteria.add(entry)
                    if entry not in criteria:
                        errors.append(f"{rel} requirement references missing criterion {entry!r}")
        unmentioned = set(criteria) - mentioned_criteria
        if unmentioned:
            errors.append(f"{rel} criteria not referenced by requirements: {', '.join(sorted(unmentioned))}")

        display = advancement.get("display")
        if not isinstance(display, dict):
            errors.append(f"{rel} must have a display object so progression stays legible")
        else:
            for field in ["title", "description", "icon"]:
                if field not in display:
                    errors.append(f"{rel} display missing {field}")
            icon = display.get("icon")
            if isinstance(icon, dict) and not isinstance(icon.get("id"), str):
                errors.append(f"{rel} display.icon must include an item id")

        parent = advancement.get("parent")
        if name == "root":
            if parent is not None:
                errors.append(f"{rel} should not declare a parent")
            parent_by_name[name] = None
            continue
        if not isinstance(parent, str):
            errors.append(f"{rel} must declare a parent advancement")
            parent_by_name[name] = None
            continue
        prefix = "tenpack_create:create_progression/"
        if not parent.startswith(prefix):
            errors.append(f"{rel} parent should stay inside Tenpack Create progression tree: {parent}")
            parent_by_name[name] = None
            continue
        parent_name = parent.removeprefix(prefix)
        parent_by_name[name] = parent_name
        if parent_name not in advancement_files:
            errors.append(f"{rel} parent advancement does not exist: {parent}")

    for start in parent_by_name:
        seen: set[str] = set()
        cur: str | None = start
        while cur is not None:
            if cur in seen:
                errors.append(f"advancement parent cycle detected starting at {start}: {', '.join(sorted(seen))}")
                break
            seen.add(cur)
            cur = parent_by_name.get(cur)


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
        server_mod = repo / "server/mods" / filename
        if not client_mod.exists():
            errors.append(f"missing expected client Create workflow tool: {client_mod.relative_to(repo)}")
        if server_mod.exists():
            errors.append(f"client-only Create workflow tool must not be in server/mods: {server_mod.relative_to(repo)}")


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

    for rel in DIESEL_HEAVY_INDUSTRY_RECIPES:
        require_recipe_refs(
            errors,
            root,
            rel,
            {"createdieselgenerators:diesel_engine"},
            "diesel-era heavy-industry anchor",
        )

    for rel in PRECISION_OR_GYRO_REQUIRED_RECIPES:
        require_any_ref(errors, root, rel, {"create:precision_mechanism", "simulated:gyroscopic_mechanism"})

    for rel, required_refs in SECONDARY_VEHICLE_RECIPE_REFS.items():
        require_recipe_refs(errors, root, rel, required_refs, "secondary vehicle/flight era anchors")

    for rel, required_refs in SMART_LOGISTICS_RECIPE_REFS.items():
        require_recipe_refs(errors, root, rel, required_refs, "smart logistics/rail era anchors")

    for rel in TIRE_RECIPES_FORBID_KELP:
        refs = set(collect_refs(root / rel))
        forbidden = refs.intersection({"minecraft:dried_kelp", "minecraft:dried_kelp_block"})
        if forbidden:
            errors.append(f"{rel} must not use kelp as a cheap Offroad tire bypass: {', '.join(sorted(forbidden))}")

    mounted_cannon = read_json(errors, root / "data/aeronautics/recipe/mechanical_crafting/mounted_potato_cannon.json", "mounted potato cannon") or {}
    if mounted_cannon.get("type") != "create:mechanical_crafting":
        errors.append("mounted potato cannon should remain a mechanical crafting recipe")


def check_advancement_ladder(errors: list[str], root: Path) -> None:
    expected_progression_advancement_parents = {
        "brass_electronics.json": "tenpack_create:create_progression/fluid_handling",
        "precision_engineering.json": "tenpack_create:create_progression/brass_electronics",
        "physics_age.json": "tenpack_create:create_progression/precision_engineering",
        "ground_vehicles.json": "tenpack_create:create_progression/physics_age",
        "controlled_flight.json": "tenpack_create:create_progression/ground_vehicles",
        "first_plane.json": "tenpack_create:create_progression/controlled_flight",
    }
    expected_progression_advancement_refs = {
        "brass_electronics.json": {"create:brass_casing", "create:electron_tube", "create:deployer"},
        "precision_engineering.json": {"create:precision_mechanism"},
        "physics_age.json": {"simulated:physics_assembler"},
        "ground_vehicles.json": {
            "offroad:wheel_mount",
            "offroad:tire",
            "simulated:steering_wheel",
            "simulated:throttle_lever",
            "simulated:engine_assembly",
        },
        "controlled_flight.json": {
            "simulated:gyroscopic_mechanism",
            "aeronautics:propeller_bearing",
            "simulated:throttle_lever",
            "simulated:altitude_sensor",
            "simulated:velocity_sensor",
        },
        "first_plane.json": {"aeronautics:gyroscopic_propeller_bearing", "aeronautics:smart_propeller"},
    }
    expected_progression_advancement_requirements = {
        "brass_electronics.json": [["brass_casing"], ["electron_tube"], ["deployer"]],
        "precision_engineering.json": [["has_item"]],
        "physics_age.json": [["has_item"]],
        "ground_vehicles.json": [["wheel_mount"], ["tire"], ["steering_wheel"], ["throttle_lever"], ["engine_assembly"]],
        "controlled_flight.json": [["gyro"], ["bearing"], ["throttle"], ["altitude_sensor"], ["velocity_sensor"]],
        "first_plane.json": [["gyro_bearing"], ["smart_propeller"]],
    }
    expected_progression_advancement_display = {
        "brass_electronics.json": ("Brass & Electronics", "create:electron_tube"),
        "precision_engineering.json": ("Precision Engineering", "create:precision_mechanism"),
        "physics_age.json": ("Physics Age", "simulated:physics_assembler"),
        "ground_vehicles.json": ("Ground Vehicles", "offroad:wheel_mount"),
        "controlled_flight.json": ("Controlled Flight", "aeronautics:gyroscopic_propeller_bearing"),
        "first_plane.json": ("First Plane Moment", "aeronautics:smart_propeller"),
    }
    expected_progression_advancement_conditions = {
        "brass_electronics.json": {"create"},
        "precision_engineering.json": {"create"},
        "physics_age.json": {"create", "simulated"},
        "ground_vehicles.json": {"create", "simulated", "offroad"},
        "controlled_flight.json": {"create", "simulated", "aeronautics", "offroad"},
        "first_plane.json": {"create", "simulated", "aeronautics", "offroad"},
    }
    for rel, expected_parent in expected_progression_advancement_parents.items():
        path = root / ADVANCEMENT_REL / rel
        advancement = read_json(errors, path, rel) or {}
        if advancement.get("parent") != expected_parent:
            errors.append(f"{rel} should parent to {expected_parent} so the brass/precision/physics era order stays legible")
        if advancement.get("requirements") != expected_progression_advancement_requirements[rel]:
            errors.append(f"{rel} should keep expected progression requirements: {expected_progression_advancement_requirements[rel]!r}")
        expected_title, expected_icon = expected_progression_advancement_display[rel]
        display = advancement.get("display", {}) if isinstance(advancement, dict) else {}
        actual_title = display.get("title", {}).get("text") if isinstance(display, dict) and isinstance(display.get("title"), dict) else None
        actual_icon = display.get("icon", {}).get("id") if isinstance(display, dict) and isinstance(display.get("icon"), dict) else None
        if actual_title != expected_title:
            errors.append(f"{rel} should keep title {expected_title!r}, got {actual_title!r}")
        if actual_icon != expected_icon:
            errors.append(f"{rel} should keep display icon {expected_icon!r}, got {actual_icon!r}")
        refs = set(collect_refs(path))
        missing_refs = expected_progression_advancement_refs[rel] - refs
        if missing_refs:
            errors.append(f"{rel} missing progression advancement refs: {', '.join(sorted(missing_refs))}")
        missing_conditions = expected_progression_advancement_conditions[rel] - mod_loaded_conditions(advancement)
        if missing_conditions:
            errors.append(f"{rel} missing progression mod-loaded conditions: {', '.join(sorted(missing_conditions))}")

    expected_oil_advancement_parents = {
        "oil_prospecting.json": "tenpack_create:create_progression/starter_logistics",
        "black_gold.json": "tenpack_create:create_progression/precision_engineering",
        "oil_refining.json": "tenpack_create:create_progression/black_gold",
        "diesel_power.json": "tenpack_create:create_progression/oil_refining",
        "heavy_industry.json": "tenpack_create:create_progression/diesel_power",
    }
    expected_oil_advancement_refs = {
        "oil_prospecting.json": {"createdieselgenerators:oil_scanner"},
        "black_gold.json": {"createdieselgenerators:oil_scanner", "createdieselgenerators:pumpjack_bearing"},
        "oil_refining.json": {"createdieselgenerators:distillation_controller", "createdieselgenerators:oil_barrel"},
        "diesel_power.json": {"createdieselgenerators:diesel_engine"},
        "heavy_industry.json": {"createbigcannons:cannon_builder"},
    }
    expected_oil_advancement_requirements = {
        "oil_prospecting.json": [["oil_scanner"]],
        "black_gold.json": [["oil_scanner"], ["pumpjack"]],
        "oil_refining.json": [["distillation_controller"], ["oil_barrel"]],
        "diesel_power.json": [["diesel_engine"]],
        "heavy_industry.json": [["cannon_builder"]],
    }
    expected_oil_advancement_display = {
        "oil_prospecting.json": ("Oil Prospecting", "createdieselgenerators:oil_scanner"),
        "black_gold.json": ("Black Gold", "createdieselgenerators:pumpjack_bearing"),
        "oil_refining.json": ("Oil Refining", "createdieselgenerators:distillation_controller"),
        "diesel_power.json": ("Diesel Power", "createdieselgenerators:diesel_engine"),
        "heavy_industry.json": ("Heavy Industry", "createbigcannons:cannon_builder"),
    }
    for rel, expected_parent in expected_oil_advancement_parents.items():
        path = root / ADVANCEMENT_REL / rel
        advancement = read_json(errors, path, rel) or {}
        if advancement.get("parent") != expected_parent:
            errors.append(f"{rel} should parent to {expected_parent} so oil scouting, extraction, refining, diesel, and war industry stay staged")
        if advancement.get("requirements") != expected_oil_advancement_requirements[rel]:
            errors.append(f"{rel} should keep expected oil/heavy-industry requirements: {expected_oil_advancement_requirements[rel]!r}")
        expected_title, expected_icon = expected_oil_advancement_display[rel]
        display = advancement.get("display", {}) if isinstance(advancement, dict) else {}
        actual_title = display.get("title", {}).get("text") if isinstance(display, dict) and isinstance(display.get("title"), dict) else None
        actual_icon = display.get("icon", {}).get("id") if isinstance(display, dict) and isinstance(display.get("icon"), dict) else None
        if actual_title != expected_title:
            errors.append(f"{rel} should keep title {expected_title!r}, got {actual_title!r}")
        if actual_icon != expected_icon:
            errors.append(f"{rel} should keep display icon {expected_icon!r}, got {actual_icon!r}")
        refs = set(collect_refs(path))
        missing_refs = expected_oil_advancement_refs[rel] - refs
        if missing_refs:
            errors.append(f"{rel} missing oil/heavy-industry advancement refs: {', '.join(sorted(missing_refs))}")

    expected_logistics_advancement_parents = {
        "smart_logistics.json": "tenpack_create:create_progression/precision_engineering",
        "rail_logistics.json": "tenpack_create:create_progression/precision_engineering",
    }
    expected_logistics_advancement_refs = {
        "smart_logistics.json": {
            "create:mechanical_crafter",
            "create:packager",
            "create:package_frogport",
            "create:stock_link",
            "create:factory_gauge",
            "create:redstone_requester",
        },
        "rail_logistics.json": {"create:track_station", "create:track_signal", "create:track_observer"},
    }
    expected_logistics_advancement_requirements = {
        "smart_logistics.json": [["crafter"], ["packager", "frogport"], ["stock_link"], ["factory_gauge", "redstone_requester"]],
        "rail_logistics.json": [["station"], ["signal", "observer"]],
    }
    expected_logistics_advancement_display = {
        "smart_logistics.json": ("Smart Logistics", "create:stock_link"),
        "rail_logistics.json": ("Rail Logistics", "create:track_station"),
    }
    for rel, expected_parent in expected_logistics_advancement_parents.items():
        path = root / ADVANCEMENT_REL / rel
        advancement = read_json(errors, path, rel) or {}
        if advancement.get("parent") != expected_parent:
            errors.append(f"{rel} should parent to {expected_parent} so smart logistics and rail stay in the precision era")
        if advancement.get("requirements") != expected_logistics_advancement_requirements[rel]:
            errors.append(f"{rel} should keep expected logistics requirements: {expected_logistics_advancement_requirements[rel]!r}")
        expected_title, expected_icon = expected_logistics_advancement_display[rel]
        display = advancement.get("display", {}) if isinstance(advancement, dict) else {}
        actual_title = display.get("title", {}).get("text") if isinstance(display, dict) and isinstance(display.get("title"), dict) else None
        actual_icon = display.get("icon", {}).get("id") if isinstance(display, dict) and isinstance(display.get("icon"), dict) else None
        if actual_title != expected_title:
            errors.append(f"{rel} should keep title {expected_title!r}, got {actual_title!r}")
        if actual_icon != expected_icon:
            errors.append(f"{rel} should keep display icon {expected_icon!r}, got {actual_icon!r}")
        refs = set(collect_refs(path))
        missing_refs = expected_logistics_advancement_refs[rel] - refs
        if missing_refs:
            errors.append(f"{rel} missing logistics advancement refs: {', '.join(sorted(missing_refs))}")

    for rel in ["farm_power.json", "kitchen_line.json", "food_logistics.json", "ration_routes.json"]:
        path = root / ADVANCEMENT_REL / rel
        if not path.exists():
            errors.append(f"missing food/Create logistics advancement: {ADVANCEMENT_REL / rel}")

    expected_food_advancement_parents = {
        "farm_power.json": "tenpack_create:create_progression/kinetic_workshop",
        "kitchen_line.json": "tenpack_create:create_progression/farm_power",
        "food_logistics.json": "tenpack_create:create_progression/smart_logistics",
        "ration_routes.json": "tenpack_create:create_progression/rail_logistics",
    }
    expected_food_advancement_refs = {
        "farm_power.json": {
            "create:mechanical_harvester",
            "farmersdelight:cabbage_crate",
            "farmersdelight:carrot_crate",
            "farmersdelight:potato_crate",
        },
        "kitchen_line.json": {
            "farmersdelight:cooking_pot",
            "create:mechanical_mixer",
            "create:basin",
            "create:blaze_burner",
            "farmersdelight:beef_stew",
            "farmersdelight:stuffed_potato",
            "farmersdelight:pasta_with_meatballs",
        },
        "food_logistics.json": {
            "create:portable_storage_interface",
            "create:stock_link",
            "farmersdelight:bamboo_basket",
            "supplementaries:lunch_basket",
            "farmersdelight:beef_stew",
            "farmersdelight:chicken_soup",
            "farmersdelight:vegetable_soup",
            "farmersdelight:steak_and_potatoes",
        },
        "ration_routes.json": {
            "create:track_station",
            "create:stock_link",
            "create:package_frogport",
            "farmersdelight:bamboo_basket",
            "supplementaries:lunch_basket",
            "farmersdelight:beef_stew",
            "farmersdelight:chicken_soup",
            "farmersdelight:vegetable_soup",
            "farmersdelight:steak_and_potatoes",
        },
    }
    expected_food_advancement_requirements = {
        "farm_power.json": [["harvester"], ["cabbage_crate", "carrot_crate", "potato_crate"]],
        "kitchen_line.json": [["cooking_pot"], ["mechanical_mixer"], ["basin"], ["blaze_burner"], ["beef_stew", "stuffed_potato", "pasta"]],
        "food_logistics.json": [["portable_storage"], ["stock_link"], ["depot_storage"], ["field_rations"], ["beef_stew", "chicken_soup", "vegetable_soup", "steak_and_potatoes"]],
        "ration_routes.json": [["station"], ["stock_link"], ["frogport"], ["depot_storage"], ["field_rations"], ["beef_stew", "chicken_soup", "vegetable_soup", "steak_and_potatoes"]],
    }
    expected_food_advancement_display = {
        "farm_power.json": ("Farm Power", "create:mechanical_harvester"),
        "kitchen_line.json": ("Kitchen Line", "farmersdelight:cooking_pot"),
        "food_logistics.json": ("Food Logistics", "supplementaries:lunch_basket"),
        "ration_routes.json": ("Ration Routes", "create:package_frogport"),
    }
    expected_food_advancement_conditions = {
        "farm_power.json": {"create", "farmersdelight"},
        "kitchen_line.json": {"create", "farmersdelight"},
        "food_logistics.json": {"create", "farmersdelight", "supplementaries"},
        "ration_routes.json": {"create", "farmersdelight", "supplementaries"},
    }
    for rel, expected_parent in expected_food_advancement_parents.items():
        path = root / ADVANCEMENT_REL / rel
        advancement = read_json(errors, path, rel) or {}
        if advancement.get("parent") != expected_parent:
            errors.append(f"{rel} should parent to {expected_parent} so food logistics stays in the Create ladder")
        if advancement.get("requirements") != expected_food_advancement_requirements[rel]:
            errors.append(f"{rel} should keep expected food/Create milestone requirements: {expected_food_advancement_requirements[rel]!r}")
        expected_title, expected_icon = expected_food_advancement_display[rel]
        display = advancement.get("display", {}) if isinstance(advancement, dict) else {}
        actual_title = display.get("title", {}).get("text") if isinstance(display, dict) and isinstance(display.get("title"), dict) else None
        actual_icon = display.get("icon", {}).get("id") if isinstance(display, dict) and isinstance(display.get("icon"), dict) else None
        if actual_title != expected_title:
            errors.append(f"{rel} should keep title {expected_title!r}, got {actual_title!r}")
        if actual_icon != expected_icon:
            errors.append(f"{rel} should keep display icon {expected_icon!r}, got {actual_icon!r}")
        refs = set(collect_refs(path))
        missing_refs = expected_food_advancement_refs[rel] - refs
        if missing_refs:
            errors.append(f"{rel} missing food/Create advancement refs: {', '.join(sorted(missing_refs))}")
        missing_conditions = expected_food_advancement_conditions[rel] - mod_loaded_conditions(advancement)
        if missing_conditions:
            errors.append(f"{rel} missing food/Create mod-loaded conditions: {', '.join(sorted(missing_conditions))}")


def check_create_kitchen_recipes(errors: list[str], root: Path) -> None:
    kitchen_dir = root / CREATE_KITCHEN_RECIPE_DIR
    if not kitchen_dir.exists():
        errors.append(f"missing Create kitchen recipe directory: {CREATE_KITCHEN_RECIPE_DIR}")
    else:
        expected_paths = {
            Path(rel).relative_to(CREATE_KITCHEN_RECIPE_DIR.parent)
            for rel in (set(CREATE_KITCHEN_RECIPES) | set(CREATE_RATION_RECIPES))
        }
        actual_paths = {path.relative_to(kitchen_dir.parent) for path in kitchen_dir.glob("*.json")}
        unexpected = actual_paths - expected_paths
        missing = expected_paths - actual_paths
        if unexpected:
            errors.append(
                "unexpected unaudited Create kitchen recipe(s): "
                + ", ".join(sorted(path.as_posix() for path in unexpected))
            )
        if missing:
            errors.append(
                "missing expected Create kitchen recipe(s): "
                + ", ".join(sorted(path.as_posix() for path in missing))
            )

    for rel, output in CREATE_KITCHEN_RECIPES.items():
        path = root / rel
        if not path.exists():
            errors.append(f"missing Create kitchen recipe: {rel}")
            continue
        recipe = read_json(errors, path, rel) or {}
        if recipe.get("type") != "create:mixing":
            errors.append(f"{rel} should use create:mixing so kitchens scale through Create infrastructure")
        if recipe.get("heat_requirement") != "heated":
            errors.append(f"{rel} should require heated mixing so factory meals still need a real kitchen line")
        refs = set(collect_refs(path))
        for required in {"minecraft:bowl", "minecraft:water", output}:
            if required not in refs:
                errors.append(f"{rel} missing Create kitchen anchor/ref {required}")
        missing_base_ingredients = CREATE_KITCHEN_BASE_INGREDIENT_REFS[rel] - refs
        if missing_base_ingredients:
            errors.append(
                f"{rel} missing Farmer's Delight meal ingredient refs: "
                + ", ".join(sorted(missing_base_ingredients))
            )

        ingredients = recipe.get("ingredients")
        if not isinstance(ingredients, list):
            errors.append(f"{rel} should have an ingredients list")
            ingredients = []
        bowl_ingredients = [ingredient for ingredient in ingredients if isinstance(ingredient, dict) and ingredient.get("item") == "minecraft:bowl"]
        if len(bowl_ingredients) != 1:
            errors.append(f"{rel} should consume exactly one bowl so factory meals still pay the serving-container cost")
        water_ingredients = [ingredient for ingredient in ingredients if isinstance(ingredient, dict) and ingredient.get("fluid") == "minecraft:water"]
        if len(water_ingredients) != 1:
            errors.append(f"{rel} should use exactly one water fluid ingredient")
        elif water_ingredients[0].get("amount") != 250:
            errors.append(f"{rel} should use 250 mB water, got {water_ingredients[0].get('amount')!r}")

        results = recipe.get("results")
        if not isinstance(results, list) or len(results) != 1 or not isinstance(results[0], dict):
            errors.append(f"{rel} should output exactly one meal stack")
            continue
        result = results[0]
        if result.get("id") != output:
            errors.append(f"{rel} should output {output}, got {result.get('id')!r}")
        result_count = result.get("count", result.get("amount", 1))
        if result_count != 1:
            errors.append(f"{rel} should output one meal, got count/amount {result_count!r}")

    for rel, output in CREATE_RATION_RECIPES.items():
        path = root / rel
        if not path.exists():
            errors.append(f"missing Create ration recipe: {rel}")
            continue
        recipe = read_json(errors, path, rel) or {}
        if recipe.get("type") != "create:mixing":
            errors.append(f"{rel} should use create:mixing so ration assembly scales through Create infrastructure")
        if recipe.get("heat_requirement") != "heated":
            errors.append(f"{rel} should require heated mixing so ration lines still need a real kitchen line")
        refs = set(collect_refs(path))
        for required in CREATE_RATION_BASE_INGREDIENT_REFS[rel] | {output}:
            if required not in refs:
                errors.append(f"{rel} missing Create ration anchor/ref {required}")

        ingredients = recipe.get("ingredients")
        if not isinstance(ingredients, list):
            errors.append(f"{rel} should have an ingredients list")
        elif len(ingredients) != len(CREATE_RATION_BASE_INGREDIENT_REFS[rel]):
            errors.append(
                f"{rel} should consume the audited ration ingredient count "
                f"{len(CREATE_RATION_BASE_INGREDIENT_REFS[rel])}, got {len(ingredients)}"
            )

        results = recipe.get("results")
        if not isinstance(results, list) or len(results) != 1 or not isinstance(results[0], dict):
            errors.append(f"{rel} should output exactly one ration stack")
            continue
        result = results[0]
        if result.get("id") != output:
            errors.append(f"{rel} should output {output}, got {result.get('id')!r}")
        result_count = result.get("count", result.get("amount", 1))
        if result_count != 1:
            errors.append(f"{rel} should output one ration, got count/amount {result_count!r}")


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
    check_fuel_type(errors, root, DIESEL_FUEL_TYPE_REL, "#c:diesel")
    check_fuel_type(errors, root, GASOLINE_FUEL_TYPE_REL, "#c:gasoline")


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
    errors: list[str] = []

    old_root = repo / OLD_DATAPACK_REL
    if old_root.exists():
        errors.append(
            f"Create progression datapack is in old deploy path {OLD_DATAPACK_REL}; "
            f"move it under {DATAPACK_REL} so it loads inside the server world"
        )
    if not root.exists():
        errors.append(f"missing datapack: {DATAPACK_REL}")

    if not errors:
        check_datapack_metadata(errors, root)
        check_all_datapack_json_parses(errors, root)
        check_datapack_folder_names(errors, root)
        check_pattern_recipe_sanity(errors, root)
        check_shapeless_recipe_sanity(errors, root)
        check_recipe_output_sanity(errors, root)
        check_sequenced_assembly_sanity(errors, root)
        check_create_fluid_ingredients(errors, root)
        check_recipe_override_paths(errors, repo, root)
        check_recipe_mod_conditions(errors, root)
        check_advancement_sanity(errors, root)
        check_advancement_mod_conditions(errors, root)
        check_create_encased_variants(errors, root)
        check_recipe_gates(errors, root)
        check_advancement_ladder(errors, root)
        check_create_kitchen_recipes(errors, root)
        check_oilfield_tags(errors, root)
        check_oil_scanner_stays_early(errors, root)
        check_oil_logistics_recipes(errors, root)
        check_oil_fuel_types(errors, root)

    check_required_create_mods(errors, repo)
    check_oilfield_config(errors, repo)

    if errors:
        for error in errors:
            print(error, file=sys.stderr)
        return 1

    print("Create progression invariants passed")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
