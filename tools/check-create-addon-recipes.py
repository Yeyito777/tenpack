#!/usr/bin/env python3
"""Guardrails for installed Create addon recipes.

This is a static recipe audit, not a Minecraft recipe loader. It catches the
things that matter for Tenpack's era ladder:

- addon recipes should not output core Create progression blocks/items that are
  controlled by the Tenpack progression datapack;
- useful rail/navigation/vehicle helpers should stay behind the intended era
  anchors (electron tubes, precision mechanisms, item drains, etc.);
- harmless decorative Create-namespace compat outputs are explicitly allowlisted.
"""

from __future__ import annotations

import json
import sys
import zipfile
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
SERVER_MODS = ROOT / "server" / "mods"

ADDON_JARS = {
    "CreateDragonsPlus-1.10.0b.jar",
    "create-central-kitchen-2.4.0.jar",
    "railways-0.2.0-beta.2+neoforge-mc1.21.1.jar",
    "createrailwaysnavigator-neoforge-1.21.1-beta-0.9.0-C6.jar",
    "copycats-3.0.4+mc.1.21.1-neoforge.jar",
    "createdeco-2.1.3.jar",
    "bellsandwhistles-0.4.7-1.21.1.jar",
    "rechiseledcreate-1.1.0-neoforge-mc1.21.jar",
    "sliceanddice-forge-4.2.4.jar",
    "create-confectionery1.21.1_v1.1.2.jar",
    "create_winery-2.0.2-neoforge-1.21.1.jar",
    "create_bic_bit-1.0.2C.jar",
}

# Core progression outputs controlled by Tenpack's datapack. Addons should not
# introduce parallel recipes for these unless the recipe is explicitly audited
# and moved into server/world/datapacks/tenpack-create-progression.
CONTROLLED_CREATE_OUTPUTS = {
    "create:mechanical_press",
    "create:mechanical_mixer",
    "create:mechanical_saw",
    "create:mechanical_drill",
    "create:encased_fan",
    "create:deployer",
    "create:mechanical_roller",
    "create:mechanical_bearing",
    "create:mechanical_piston",
    "create:gantry_carriage",
    "create:mechanical_harvester",
    "create:mechanical_plough",
    "create:portable_storage_interface",
    "create:portable_fluid_interface",
    "create:spout",
    "create:item_drain",
    "create:steam_engine",
    "create:elevator_pulley",
    "create:track_station",
    "create:track_signal",
    "create:track_observer",
    "create:precision_mechanism",
    "create:electron_tube",
    "create:brass_hand",
    "create:mechanical_crafter",
    "create:packager",
    "create:package_frogport",
    "create:stock_link",
    "create:factory_gauge",
    "create:redstone_requester",
    "create:rotation_speed_controller",
    "create:mechanical_arm",
    "create:controls",
    "create:blaze_burner",
}

# Compat/decor outputs in the create namespace that are safe for this pack.
ALLOWED_CREATE_NAMESPACE_OUTPUTS = {
    "create:copycat_panel",
    "create:copycat_step",
    "create:industrial_iron_block",
    "create:placard",
    "create:chocolate",
}

# Recipe path -> required references. These anchor addon utility to the intended
# era without rewriting every decorative recipe in each addon.
RECIPE_REF_INVARIANTS = {
    "railways-0.2.0-beta.2+neoforge-mc1.21.1.jar": {
        "data/railways/recipe/crafting/handcar.json": {"create:contraption_controls"},
        "data/railways/recipe/crafting/fuel_tank.json": {"create:fluid_tank", "create:sturdy_sheet"},
        "data/railways/recipe/crafting/semaphore.json": {"create:electron_tube"},
        "data/railways/recipe/crafting/track_switch_brass.json": {"create:precision_mechanism"},
        "data/railways/recipe/crafting/remote_lens.json": {"create:precision_mechanism"},
    },
    "createrailwaysnavigator-neoforge-1.21.1-beta-0.9.0-C6.jar": {
        "data/createrailwaysnavigator/recipe/navigator.json": {"create:precision_mechanism"},
        "data/createrailwaysnavigator/recipe/train_station_clock.json": {"create:precision_mechanism"},
        "data/createrailwaysnavigator/recipe/advanced_display.json": {"create:display_board"},
    },
    "CreateDragonsPlus-1.10.0b.jar": {
        "data/create_dragons_plus/recipe/crafting/fluid_hatch.json": {"create:item_drain"},
        "data/create_dragons_plus/recipe/crafting/levitite_fragile_fluid_tank.json": {"aeronautics:levitite_blend_bucket"},
    },
    "copycats-3.0.4+mc.1.21.1-neoforge.jar": {
        "data/copycats/recipe/crafting/copycat_cogwheel.json": {"create:cogwheel", "create:zinc_ingot"},
        "data/copycats/recipe/crafting/copycat_large_cogwheel.json": {"create:large_cogwheel", "create:zinc_ingot"},
        "data/copycats/recipe/crafting/copycat_fluid_pipe.json": {"create:fluid_pipe", "create:zinc_ingot"},
    },
    "sliceanddice-forge-4.2.4.jar": {
        "data/sliceanddice/recipe/slicer.json": {"create:andesite_casing", "create:cogwheel", "create:turntable"},
    },
}

CONFIG_EXPECTATIONS = {
    ROOT / "tenpack-specs/overrides/config/create_dragons_plus-common.toml": {
        '"item/blaze_upgrade_smithing_template" = false': "Create: Dragons Plus blaze smithing template duplication should stay disabled",
    },
}


def walk_refs(value: Any) -> set[str]:
    refs: set[str] = set()
    if isinstance(value, dict):
        for key, child in value.items():
            if key in {"item", "id", "tag"} and isinstance(child, str):
                refs.add(child)
            refs |= walk_refs(child)
    elif isinstance(value, list):
        for child in value:
            refs |= walk_refs(child)
    return refs


def recipe_outputs(obj: dict[str, Any]) -> set[str]:
    outputs: set[str] = set()

    def add(value: Any) -> None:
        if isinstance(value, str):
            outputs.add(value)
        elif isinstance(value, dict):
            if isinstance(value.get("id"), str):
                outputs.add(value["id"])
            elif isinstance(value.get("item"), str):
                outputs.add(value["item"])

    add(obj.get("result"))
    add(obj.get("output"))
    for result in obj.get("results", []) if isinstance(obj.get("results"), list) else []:
        add(result)
    return outputs


def read_recipe(zf: zipfile.ZipFile, path: str) -> dict[str, Any] | None:
    try:
        return json.loads(zf.read(path).decode("utf-8", "replace"))
    except Exception:
        return None


def main() -> int:
    errors: list[str] = []
    recipes_checked = 0

    for jar_name in sorted(ADDON_JARS):
        jar_path = SERVER_MODS / jar_name
        if not jar_path.exists():
            errors.append(f"missing expected Create addon jar: {jar_name}")
            continue
        with zipfile.ZipFile(jar_path) as zf:
            names = set(zf.namelist())
            invariants = RECIPE_REF_INVARIANTS.get(jar_name, {})
            for recipe_path, required_refs in invariants.items():
                if recipe_path not in names:
                    errors.append(f"{jar_name}: missing audited recipe {recipe_path}")
                    continue
                obj = read_recipe(zf, recipe_path)
                if obj is None:
                    errors.append(f"{jar_name}: invalid JSON in audited recipe {recipe_path}")
                    continue
                refs = walk_refs(obj)
                missing = required_refs - refs
                if missing:
                    errors.append(f"{jar_name}: {recipe_path} missing expected era anchor refs: {', '.join(sorted(missing))}")

            for name in names:
                if not (name.startswith("data/") and "/recipe/" in name and name.endswith(".json")):
                    continue
                obj = read_recipe(zf, name)
                if obj is None:
                    errors.append(f"{jar_name}: invalid recipe JSON {name}")
                    continue
                recipes_checked += 1
                for output in recipe_outputs(obj):
                    if output in CONTROLLED_CREATE_OUTPUTS:
                        errors.append(f"{jar_name}: {name} outputs controlled progression item {output}")
                    if output.startswith("create:") and output not in ALLOWED_CREATE_NAMESPACE_OUTPUTS and output not in CONTROLLED_CREATE_OUTPUTS:
                        errors.append(
                            f"{jar_name}: {name} outputs unclassified create namespace item {output}; "
                            "audit and add to ALLOWED_CREATE_NAMESPACE_OUTPUTS if harmless"
                        )

    for config_path, expected_lines in CONFIG_EXPECTATIONS.items():
        if not config_path.exists():
            errors.append(f"missing expected config: {config_path.relative_to(ROOT)}")
            continue
        text = config_path.read_text(encoding="utf-8")
        for line, reason in expected_lines.items():
            if line not in text:
                errors.append(f"{config_path.relative_to(ROOT)} missing `{line}`: {reason}")

    print(f"Create addon recipes checked: {recipes_checked}")
    if errors:
        print("\nCreate addon recipe audit failed:", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1

    print("Create addon recipe guardrails passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
