#!/usr/bin/env python3
"""Food-system validation harness for Tenpack.

This is a focused, human-readable wrapper around the pack invariants that make the
current food pass work as a system instead of as a pile of unrelated mods:

- AppleSkin makes hunger/saturation visible, but stays client-only.
- CraftTweaker ships a server-side food-value pressure script.
- Legendary Survival Overhaul adds activity pressure without thirst or idle tax.
- Supplementaries provides a six-slot lunch basket without becoming a backpack.
- Farmer's Delight + Create heated-mixer recipes make prepared meals scalable.

The broader check scripts still matter.  This harness exists so a future food pass
can run one command and see whether the food loop is intact end-to-end, including
whether selected public manifest entries still publish the current source files.
"""

from __future__ import annotations

import hashlib
import json
import re
import sys
import tomllib
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]


REQUIRED_BOTH_SIDE_MODS = {
    "FarmersDelight-1.21.1-1.3.1.jar": "Farmer's Delight meal baseline",
    "create-central-kitchen-2.4.0.jar": "Create/Farmer's Delight integration",
    "sliceanddice-forge-4.2.4.jar": "Create-powered cutting/kitchen automation",
    "supplementaries-neoforge-1.21.1-3.6.4.jar": "six-slot portable lunch basket",
    "moonlight-neoforge-1.21.1-3.0.7.jar": "Supplementaries dependency",
    "CraftTweaker-neoforge-1.21.1-21.0.38.jar": "food-value pressure script runtime",
    "legendarysurvivaloverhaul-1.21.1-2.4.2.jar": "temperature/activity survival pressure",
}

REQUIRED_CLIENT_ONLY_MODS = {
    "appleskin-neoforge-mc1.21-3.0.9.jar": "hunger/saturation HUD and food tooltips",
}

FORBIDDEN_FOOD_MOD_TOKENS = {
    "spiceoflife": "no diet-rotation/Nutrition-style chore layer",
    "spice-of-life": "no diet-rotation/Nutrition-style chore layer",
    "solonion": "Supplementaries lunch basket is the chosen portable food layer",
    "sol-onion": "Supplementaries lunch basket is the chosen portable food layer",
    "nutrition": "no Nutrition nutrient categories",
    "toughasnails": "no thirst survival stack",
    "tough-as-nails": "no thirst survival stack",
}

EXPECTED_FOOD_VALUES = {
    # Simple/staple foods: can prevent starvation, weak saturation ceiling.
    "minecraft:bread": (3, 0.2),
    "minecraft:baked_potato": (3, 0.3),
    "minecraft:potato": (1, 0.1),
    "minecraft:carrot": (2, 0.2),
    "minecraft:beetroot": (1, 0.2),
    "minecraft:melon_slice": (1, 0.1),
    "minecraft:apple": (3, 0.2),
    # Single-ingredient cooked foods: useful rations, not the final answer.
    "minecraft:cooked_beef": (6, 0.5),
    "minecraft:cooked_porkchop": (6, 0.5),
    "minecraft:cooked_chicken": (5, 0.45),
    "minecraft:cooked_cod": (4, 0.4),
    "minecraft:cooked_salmon": (5, 0.45),
    # Prepared meals: high ceiling that makes kitchens/logistics worthwhile.
    "minecraft:mushroom_stew": (7, 0.7),
    "minecraft:rabbit_stew": (10, 0.8),
    "farmersdelight:beef_stew": (10, 0.85),
    "farmersdelight:vegetable_soup": (8, 0.85),
    "farmersdelight:chicken_soup": (9, 0.85),
    "farmersdelight:noodle_soup": (10, 0.85),
    "farmersdelight:baked_cod_stew": (10, 0.85),
    "farmersdelight:bone_broth": (7, 0.7),
    "farmersdelight:squid_ink_pasta": (12, 0.85),
    "farmersdelight:pasta_with_meatballs": (12, 0.85),
    "farmersdelight:steak_and_potatoes": (12, 0.85),
    "farmersdelight:stuffed_potato": (8, 0.7),
}

CREATE_KITCHEN_RECIPES = {
    "baked_cod_stew_from_mixing.json": "farmersdelight:baked_cod_stew",
    "beef_stew_from_mixing.json": "farmersdelight:beef_stew",
    "beetroot_soup_from_mixing.json": "minecraft:beetroot_soup",
    "bone_broth_from_mixing.json": "farmersdelight:bone_broth",
    "chicken_soup_from_mixing.json": "farmersdelight:chicken_soup",
    "fish_stew_from_mixing.json": "farmersdelight:fish_stew",
    "fried_rice_from_mixing.json": "farmersdelight:fried_rice",
    "mushroom_rice_from_mixing.json": "farmersdelight:mushroom_rice",
    "mushroom_stew_from_mixing.json": "minecraft:mushroom_stew",
    "noodle_soup_from_mixing.json": "farmersdelight:noodle_soup",
    "onion_soup_from_mixing.json": "farmersdelight:onion_soup",
    "pasta_with_meatballs_from_mixing.json": "farmersdelight:pasta_with_meatballs",
    "pasta_with_mutton_chop_from_mixing.json": "farmersdelight:pasta_with_mutton_chop",
    "pumpkin_soup_from_mixing.json": "farmersdelight:pumpkin_soup",
    "rabbit_stew_from_mixing.json": "minecraft:rabbit_stew",
    "ratatouille_from_mixing.json": "farmersdelight:ratatouille",
    "squid_ink_pasta_from_mixing.json": "farmersdelight:squid_ink_pasta",
    "vegetable_noodles_from_mixing.json": "farmersdelight:vegetable_noodles",
    "vegetable_soup_from_mixing.json": "farmersdelight:vegetable_soup",
}

CREATE_RATION_RECIPES = {
    "steak_and_potatoes_from_mixing.json": "farmersdelight:steak_and_potatoes",
}

CREATE_RATION_INGREDIENT_REFS = {
    "steak_and_potatoes_from_mixing.json": {
        "minecraft:bowl",
        "minecraft:baked_potato",
        "minecraft:cooked_beef",
        "c:crops/onion",
        "farmersdelight:cooked_rice",
    },
}

EXPECTED_ADVANCEMENT_REFS = {
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


@dataclass
class Section:
    name: str
    checks: int = 0
    errors: list[str] = field(default_factory=list)

    def ok(self, count: int = 1) -> None:
        self.checks += count

    def fail(self, message: str) -> None:
        self.checks += 1
        self.errors.append(message)


class FoodSystemValidationHarness:
    def __init__(self, root: Path = ROOT) -> None:
        self.root = root
        self.sections: list[Section] = []

    def run(self) -> int:
        self.check_mod_stack()
        self.check_survival_configs()
        self.check_food_pressure_script()
        self.check_create_kitchen_recipes()
        self.check_food_advancements()
        self.check_manifest_delivery()
        return self.report()

    def section(self, name: str) -> Section:
        section = Section(name)
        self.sections.append(section)
        return section

    def read_json(self, section: Section, path: Path) -> Any | None:
        try:
            return json.loads(path.read_text(encoding="utf-8"))
        except Exception as exc:  # noqa: BLE001 - validation should report all parse failures
            section.fail(f"invalid JSON {self.rel(path)}: {exc}")
            return None

    def read_toml(self, section: Section, path: Path) -> dict[str, Any]:
        try:
            return tomllib.loads(path.read_text(encoding="utf-8"))
        except Exception as exc:  # noqa: BLE001 - validation should report all parse failures
            section.fail(f"invalid TOML {self.rel(path)}: {exc}")
            return {}

    def rel(self, path: Path) -> str:
        return path.relative_to(self.root).as_posix()

    def require_path(self, section: Section, path: Path, reason: str) -> bool:
        if path.exists():
            section.ok()
            return True
        section.fail(f"missing {self.rel(path)}: {reason}")
        return False

    def require_equal(self, section: Section, label: str, actual: Any, expected: Any, reason: str) -> None:
        if actual == expected:
            section.ok()
        else:
            section.fail(f"{label} expected {expected!r}, got {actual!r}: {reason}")

    def get_nested(self, obj: Any, keys: list[str]) -> Any:
        cur = obj
        for key in keys:
            if not isinstance(cur, dict) or key not in cur:
                return None
            cur = cur[key]
        return cur

    def collect_refs(self, obj: Any) -> set[str]:
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

        visit(obj)
        return refs

    def check_mod_stack(self) -> None:
        section = self.section("food mod stack")

        for filename, reason in REQUIRED_BOTH_SIDE_MODS.items():
            for side in ("client", "server"):
                self.require_path(section, self.root / side / "mods" / filename, reason)

        for filename, reason in REQUIRED_CLIENT_ONLY_MODS.items():
            self.require_path(section, self.root / "client/mods" / filename, reason)
            server_path = self.root / "server/mods" / filename
            if server_path.exists():
                section.fail(f"{self.rel(server_path)} should stay client-only: {reason}")
            else:
                section.ok()

        for side in ("client", "server"):
            for jar in sorted((self.root / side / "mods").glob("*.jar")):
                normalized = re.sub(r"[^a-z0-9]+", "", jar.name.lower())
                dashed = jar.name.lower()
                for token, reason in FORBIDDEN_FOOD_MOD_TOKENS.items():
                    token_normalized = re.sub(r"[^a-z0-9]+", "", token.lower())
                    if token_normalized in normalized or token.lower() in dashed:
                        section.fail(f"forbidden food-pressure mod {self.rel(jar)}: {reason}")

    def check_survival_configs(self) -> None:
        section = self.section("survival and food configs")

        lso_server_path = self.root / "server/config/legendarysurvivaloverhaul-common.toml"
        lso_client_path = self.root / "client/config/legendarysurvivaloverhaul-common.toml"
        if self.require_path(section, lso_server_path, "LSO owns the no-thirst/activity-pressure layer"):
            lso = self.read_toml(section, lso_server_path)
            core = lso.get("core", {}) if isinstance(lso, dict) else {}
            food = lso.get("food", {}) if isinstance(lso, dict) else {}
            secondary = self.get_nested(lso, ["temperature", "secondary_effects"]) or {}
            self.require_equal(section, "LSO thirst", core.get("Thirst Enabled"), False, "no thirst chores")
            self.require_equal(section, "LSO health overhaul", core.get("Health Overhaul Enabled"), False, "avoid hidden body-system complexity")
            self.require_equal(section, "LSO temperature", core.get("Temperature Enabled"), True, "temperature is the chosen survival pressure")
            self.require_equal(section, "LSO base food exhaustion", food.get("Base Food Exhaustion"), 0.0, "no idle hunger tax")
            self.require_equal(section, "LSO sprinting food exhaustion", food.get("Sprinting Food Exhaustion"), 0.01, "travel should burn weak rations")
            self.require_equal(section, "LSO attack food exhaustion", food.get("On Attack Food Exhaustion"), 0.05, "combat should touch supplies")
            self.require_equal(section, "LSO cold hunger secondary effects", secondary.get("Cold Temperature Secondary Effects"), False, "cold should not secretly become hunger")
            self.require_equal(section, "LSO cold hunger modifier", secondary.get("Cold Hunger Modifier"), 0.0, "temperature pressure should be solved with temperature tools/foods")
        if self.require_path(section, lso_client_path, "client should receive identical common survival tuning") and lso_server_path.exists():
            self.require_equal(section, "LSO common config mirror", lso_client_path.read_bytes(), lso_server_path.read_bytes(), "client/server common config must match")

        supp_server_path = self.root / "server/config/supplementaries-common.toml"
        supp_client_path = self.root / "client/config/supplementaries-common.toml"
        if self.require_path(section, supp_server_path, "Supplementaries lunch basket behavior"):
            supp = self.read_toml(section, supp_server_path)
            self.require_equal(section, "Supplementaries lunch basket enabled", self.get_nested(supp, ["tools", "lunch_basket", "enabled"]), True, "portable food logistics should exist")
            self.require_equal(section, "Supplementaries lunch basket placeable", self.get_nested(supp, ["tools", "lunch_basket", "placeable"]), True, "field kitchens can set food down")
            self.require_equal(section, "Supplementaries lunch basket slots", self.get_nested(supp, ["tools", "lunch_basket", "slots"]), 6, "basket helps without becoming a backpack")
            self.require_equal(section, "Supplementaries sack disabled", self.get_nested(supp, ["functional", "sack", "enabled"]), False, "general portable sacks should not bypass animals/carts/camps")
            self.require_equal(section, "Supplementaries jar drink block", self.get_nested(supp, ["functional", "jar", "drink_from_jar"]), False, "jars should not bypass food/drink logistics")
            self.require_equal(section, "Supplementaries jar drink item", self.get_nested(supp, ["functional", "jar", "drink_from_jar_item"]), False, "held jars should not bypass food/drink logistics")
        if self.require_path(section, supp_client_path, "client should receive identical common basket behavior") and supp_server_path.exists():
            self.require_equal(section, "Supplementaries common config mirror", supp_client_path.read_bytes(), supp_server_path.read_bytes(), "client/server common config must match")

        supp_client_ui_path = self.root / "client/config/supplementaries-client.toml"
        if self.require_path(section, supp_client_ui_path, "basket selected-food overlay"):
            supp_client = self.read_toml(section, supp_client_ui_path)
            self.require_equal(section, "Supplementaries lunch basket overlay", self.get_nested(supp_client, ["items", "lunch_basket", "overlay"]), True, "players should read selected basket food")

        farmers_server_path = self.root / "server/config/farmersdelight-common.toml"
        farmers_client_path = self.root / "client/config/farmersdelight-common.toml"
        if self.require_path(section, farmers_server_path, "Farmer's Delight logistics behavior"):
            farmers = self.read_toml(section, farmers_server_path)
            self.require_equal(section, "Farmer's Delight vanilla soup effects", self.get_nested(farmers, ["overrides", "enableVanillaSoupExtraEffects"]), True, "soups should participate in Comfort/Nourishment loop")
            self.require_equal(section, "Farmer's Delight stackable soups", self.get_nested(farmers, ["overrides", "stack_size", "enableStackableSoupItems"]), True, "prepared food should be shippable")
        if self.require_path(section, farmers_client_path, "client should receive identical FD common config") and farmers_server_path.exists():
            self.require_equal(section, "Farmer's Delight common config mirror", farmers_client_path.read_bytes(), farmers_server_path.read_bytes(), "client/server common config must match")

    def check_food_pressure_script(self) -> None:
        section = self.section("CraftTweaker food pressure script")
        script_path = self.root / "server/scripts/tenpack_food_pressure.zs"
        if not self.require_path(section, script_path, "server-authoritative food values"):
            return

        script = script_path.read_text(encoding="utf-8")
        values: dict[str, tuple[int, float]] = {}
        assignment_re = re.compile(
            r"<item:([^>]+)>\.definition\.food\s*=\s*<item:([^>]+)>\.food\.withNutrition\((\d+)\)\.withSaturation\(([0-9.]+)\);"
        )
        for match in assignment_re.finditer(script):
            left, right, nutrition, saturation = match.groups()
            if left != right:
                section.fail(f"food assignment target mismatch: {left} vs {right}")
                continue
            if left in values:
                section.fail(f"duplicate food pressure assignment for {left}")
            values[left] = (int(nutrition), float(saturation))

        for item, expected in EXPECTED_FOOD_VALUES.items():
            actual = values.get(item)
            self.require_equal(section, f"CraftTweaker value {item}", actual, expected, "food pressure tier should stay pinned")

        if values.get("minecraft:bread", (0, 1))[1] < values.get("minecraft:cooked_beef", (0, 0))[1] < values.get("farmersdelight:beef_stew", (0, 0))[1]:
            section.ok()
        else:
            section.fail("food tier curve should be bread saturation < cooked beef saturation < prepared stew saturation")

        if values.get("minecraft:bread", (99, 99))[0] < values.get("farmersdelight:pasta_with_meatballs", (0, 0))[0]:
            section.ok()
        else:
            section.fail("prepared meals should have a higher nutrition ceiling than bread")

        if "events.register<PlayerTickPostEvent>" in script and "tenpackLastX[id] = player.x;" in script:
            section.ok()
        else:
            section.fail("PlayerTickPostEvent initialization should use the tick event player, not an undefined living entity")

        if '(event.entity.type.registryName as string) != "minecraft:player"' in script:
            section.ok()
        else:
            section.fail("LivingEntityUseItemFinishEvent should guard non-player entities before using player-only state")

        forbidden_runtime_tokens = {
            "tenpackRecentFood": "diet pressure should no longer be exact last-six item IDs",
            "tenpackFoodWeight": "hidden meal quality replaces the old exact-food weight map",
            "componenttype:supplementaries:lunch_basket_content": "basket internals should not be parsed by the diet script",
            "last-six": "docs/comments should describe staple/protein/produce balance instead of exact-food memory",
        }
        for token, reason in forbidden_runtime_tokens.items():
            if token in script:
                section.fail(f"food script still contains {token!r}: {reason}")
            else:
                section.ok()

        required_runtime_tokens = {
            "var tenpackDietStaple as int[string] = {};": "tracked staple balance",
            "var tenpackDietProtein as int[string] = {};": "tracked protein balance",
            "var tenpackDietProduce as int[string] = {};": "tracked produce balance",
            "var tenpackDietDecayClock as int[string] = {};": "active-only diet memory decay",
            "var tenpackFoodStaple as int[string] = {": "food-to-staple map",
            "var tenpackFoodProtein as int[string] = {": "food-to-protein map",
            "var tenpackFoodProduce as int[string] = {": "food-to-produce map",
            "var tenpackMealQuality as int[string] = {": "hidden immediate meal-quality map",
            '"minecraft:bread": 8,': "bread should count as staple only",
            '"minecraft:cooked_beef": 10,': "single cooked meat should count as protein",
            '"farmersdelight:beef_stew": 12,': "prepared meals should carry protein points",
            '"farmersdelight:beef_stew": 8,': "prepared meals should carry produce points",
            '"farmersdelight:steak_and_potatoes": 10,': "plated meals should carry staple points",
            '"farmersdelight:steak_and_potatoes": 14,': "plated meals should carry protein points",
            '"farmersdelight:steak_and_potatoes": 5,': "plated meals should have high hidden quality",
            "if tenpackDietDecayClock[id] >= 8 {": "diet memory should decay slowly and only during active play",
            "if tenpackDietStaple[id] > 80 { tenpackDietStaple[id] = 80; }": "staple memory should be capped",
            "if tenpackDietProtein[id] > 80 { tenpackDietProtein[id] = 80; }": "protein memory should be capped",
            "if tenpackDietProduce[id] > 80 { tenpackDietProduce[id] = 80; }": "produce memory should be capped",
            "if tenpackDietStaple[id] >= 10 { groups = groups + 1; }": "balance threshold should use broad food groups",
            "if tenpackDietStaple[id] >= 28 { robustGroups = robustGroups + 1; }": "robust balance threshold should exist",
            "multiplier = 1.28;": "one-group diet should be inefficient",
            "multiplier = 1.08;": "two-group diet should be close to baseline",
            "multiplier = 0.92;": "three-group diet should be rewarded",
            "multiplier = 0.8;": "robust three-group diet should be strongly rewarded but not free",
            "if mealQuality >= 5 {": "hidden meal quality should drive satisfaction",
            "duration = 1500;": "luxury meal satisfaction should be noticeable",
            "living.addEffect(MobEffectInstance.of(<mobeffect:minecraft:saturation>": "satisfaction should remain saturation-only, not combat buffs",
        }
        for token, reason in required_runtime_tokens.items():
            if token in script:
                section.ok()
            else:
                section.fail(f"food script missing {token!r}: {reason}")

        smoke_tool = self.root / "tools/smoke-food-script.py"
        if self.require_path(section, smoke_tool, "focused runtime smoke for the CraftTweaker food script"):
            text = smoke_tool.read_text(encoding="utf-8")
            if "tenpack_food_pressure.zs" in text and "CraftTweaker log" in text:
                section.ok()
            else:
                section.fail("tools/smoke-food-script.py should explicitly validate tenpack_food_pressure.zs through CraftTweaker log output")
            if smoke_tool.stat().st_mode & 0o111:
                section.ok()
            else:
                section.fail("tools/smoke-food-script.py should be executable")

    def check_create_kitchen_recipes(self) -> None:
        section = self.section("Create kitchen meal bridge")
        kitchen_dir = self.root / "server/world/datapacks/tenpack-create-progression/data/farmersdelight/recipe/integration/create/mixing"
        if not self.require_path(section, kitchen_dir, "Create-powered prepared meal recipes"):
            return

        actual_files = {path.name for path in kitchen_dir.glob("*.json")}
        audited_files = set(CREATE_KITCHEN_RECIPES) | set(CREATE_RATION_RECIPES)
        missing = audited_files - actual_files
        unexpected = actual_files - audited_files
        if missing:
            section.fail("missing Create kitchen recipe(s): " + ", ".join(sorted(missing)))
        else:
            section.ok()
        if unexpected:
            section.fail("unexpected unaudited Create kitchen recipe(s): " + ", ".join(sorted(unexpected)))
        else:
            section.ok()

        for filename, output in CREATE_KITCHEN_RECIPES.items():
            path = kitchen_dir / filename
            if not self.require_path(section, path, "expected heated-mixer meal recipe"):
                continue
            recipe = self.read_json(section, path) or {}
            refs = self.collect_refs(recipe)
            self.require_equal(section, f"{filename} type", recipe.get("type"), "create:mixing", "food should scale through Create mixing")
            self.require_equal(section, f"{filename} heat", recipe.get("heat_requirement"), "heated", "factory meals should require a real kitchen line")
            for required_ref in ("minecraft:bowl", "minecraft:water", output):
                if required_ref in refs:
                    section.ok()
                else:
                    section.fail(f"{filename} missing required ref {required_ref}")

            ingredients = recipe.get("ingredients") if isinstance(recipe, dict) else None
            if isinstance(ingredients, list):
                bowl_count = sum(1 for ingredient in ingredients if isinstance(ingredient, dict) and ingredient.get("item") == "minecraft:bowl")
                water = [ingredient for ingredient in ingredients if isinstance(ingredient, dict) and ingredient.get("fluid") == "minecraft:water"]
                self.require_equal(section, f"{filename} bowl count", bowl_count, 1, "one bowl per serving")
                self.require_equal(section, f"{filename} water ingredient count", len(water), 1, "one water ingredient")
                if water:
                    self.require_equal(section, f"{filename} water amount", water[0].get("amount"), 250, "250 mB water per serving")
            else:
                section.fail(f"{filename} should have an ingredients list")

            results = recipe.get("results") if isinstance(recipe, dict) else None
            if isinstance(results, list) and len(results) == 1 and isinstance(results[0], dict):
                self.require_equal(section, f"{filename} output id", results[0].get("id"), output, "recipe should produce the intended meal")
                self.require_equal(section, f"{filename} output count", results[0].get("count", results[0].get("amount", 1)), 1, "one meal per serving")
            else:
                section.fail(f"{filename} should output exactly one meal stack")

        for filename, output in CREATE_RATION_RECIPES.items():
            path = kitchen_dir / filename
            if not self.require_path(section, path, "expected heated-mixer ration recipe"):
                continue
            recipe = self.read_json(section, path) or {}
            refs = self.collect_refs(recipe)
            self.require_equal(section, f"{filename} type", recipe.get("type"), "create:mixing", "ration assembly should scale through Create mixing")
            self.require_equal(section, f"{filename} heat", recipe.get("heat_requirement"), "heated", "ration lines should still require a real kitchen line")
            for required_ref in CREATE_RATION_INGREDIENT_REFS[filename] | {output}:
                if required_ref in refs:
                    section.ok()
                else:
                    section.fail(f"{filename} missing required ref {required_ref}")

            ingredients = recipe.get("ingredients") if isinstance(recipe, dict) else None
            if isinstance(ingredients, list):
                self.require_equal(section, f"{filename} ingredient count", len(ingredients), len(CREATE_RATION_INGREDIENT_REFS[filename]), "one audited ingredient stack per ration input")
            else:
                section.fail(f"{filename} should have an ingredients list")

            results = recipe.get("results") if isinstance(recipe, dict) else None
            if isinstance(results, list) and len(results) == 1 and isinstance(results[0], dict):
                self.require_equal(section, f"{filename} output id", results[0].get("id"), output, "recipe should produce the intended ration")
                self.require_equal(section, f"{filename} output count", results[0].get("count", results[0].get("amount", 1)), 1, "one ration per serving")
            else:
                section.fail(f"{filename} should output exactly one ration stack")

    def check_food_advancements(self) -> None:
        section = self.section("food progression visibility")
        adv_dir = self.root / "server/world/datapacks/tenpack-create-progression/data/tenpack_create/advancement/create_progression"
        if not self.require_path(section, adv_dir, "food infrastructure should be visible in the advancement tree"):
            return

        for filename, required_refs in EXPECTED_ADVANCEMENT_REFS.items():
            path = adv_dir / filename
            if not self.require_path(section, path, "food/Create advancement milestone"):
                continue
            advancement = self.read_json(section, path) or {}
            refs = self.collect_refs(advancement)
            missing_refs = required_refs - refs
            if missing_refs:
                section.fail(f"{filename} missing food advancement refs: {', '.join(sorted(missing_refs))}")
            else:
                section.ok()
            criteria = advancement.get("criteria") if isinstance(advancement, dict) else None
            requirements = advancement.get("requirements") if isinstance(advancement, dict) else None
            if isinstance(criteria, dict) and criteria and isinstance(requirements, list) and requirements:
                section.ok()
            else:
                section.fail(f"{filename} should have non-empty criteria and requirements")

    def check_manifest_delivery(self) -> None:
        section = self.section("sync manifest delivery")
        server_manifest = self.load_manifest(section, self.root / "public/server-manifest.json")
        client_manifest = self.load_manifest(section, self.root / "public/client-manifest.json")
        if server_manifest is None or client_manifest is None:
            return

        required_server_paths = {
            "scripts/tenpack_food_pressure.zs",
            "mods/CraftTweaker-neoforge-1.21.1-21.0.38.jar",
            "mods/supplementaries-neoforge-1.21.1-3.6.4.jar",
            "config/supplementaries-common.toml",
            "config/legendarysurvivaloverhaul-common.toml",
            "world/datapacks/tenpack-create-progression/data/tenpack_create/advancement/create_progression/food_logistics.json",
            "world/datapacks/tenpack-create-progression/data/tenpack_create/advancement/create_progression/ration_routes.json",
            "world/datapacks/tenpack-create-progression/data/farmersdelight/recipe/integration/create/mixing/steak_and_potatoes_from_mixing.json",
        }
        required_client_paths = {
            "mods/appleskin-neoforge-mc1.21-3.0.9.jar",
            "mods/CraftTweaker-neoforge-1.21.1-21.0.38.jar",
            "mods/supplementaries-neoforge-1.21.1-3.6.4.jar",
            "config/supplementaries-client.toml",
            "config/supplementaries-common.toml",
            "config/legendarysurvivaloverhaul-common.toml",
        }
        for rel_path in sorted(required_server_paths):
            self.require_manifest_entry(section, server_manifest, rel_path, "server")
        for rel_path in sorted(required_client_paths):
            self.require_manifest_entry(section, client_manifest, rel_path, "client")

    def load_manifest(self, section: Section, path: Path) -> dict[str, dict[str, Any]] | None:
        if not self.require_path(section, path, "sync manifest should include food-system files"):
            return None
        data = self.read_json(section, path)
        if not isinstance(data, dict) or not isinstance(data.get("files"), list):
            section.fail(f"{self.rel(path)} must have a files list")
            return None
        manifest: dict[str, dict[str, Any]] = {}
        for entry in data["files"]:
            if isinstance(entry, dict) and isinstance(entry.get("path"), str):
                manifest[entry["path"]] = entry
        return manifest

    def require_manifest_entry(self, section: Section, manifest: dict[str, dict[str, Any]], rel_path: str, side: str) -> None:
        entry = manifest.get(rel_path)
        if not entry:
            section.fail(f"{side} manifest missing {rel_path}")
            return
        section.ok()

        url = entry.get("url")
        sha256 = entry.get("sha256")
        size = entry.get("size")
        if not isinstance(url, str) or not isinstance(sha256, str) or not isinstance(size, int):
            section.fail(f"{side} manifest entry {rel_path} missing url/sha256/size")
            return
        blob = self.root / "public" / url
        if not self.require_path(section, blob, f"{side} manifest blob for {rel_path}"):
            return
        data = blob.read_bytes()
        actual_hash = hashlib.sha256(data).hexdigest()
        self.require_equal(section, f"{side} manifest hash {rel_path}", actual_hash, sha256, "published blob hash should match manifest")
        self.require_equal(section, f"{side} manifest size {rel_path}", len(data), size, "published blob size should match manifest")

        source = self.root / side / rel_path
        if not self.require_path(section, source, f"{side} source for {rel_path}"):
            return
        source_data = source.read_bytes()
        source_hash = hashlib.sha256(source_data).hexdigest()
        self.require_equal(section, f"{side} manifest source hash {rel_path}", sha256, source_hash, "manifest should publish the current source hash")
        self.require_equal(section, f"{side} manifest source size {rel_path}", size, len(source_data), "manifest should publish the current source size")
        self.require_equal(section, f"{side} blob source hash {rel_path}", actual_hash, source_hash, "published blob should match the current source file")
        self.require_equal(section, f"{side} blob source size {rel_path}", len(data), len(source_data), "published blob should match the current source size")

    def report(self) -> int:
        total_checks = sum(section.checks for section in self.sections)
        total_errors = sum(len(section.errors) for section in self.sections)
        if total_errors:
            print("Food system validation failed:", file=sys.stderr)
            for section in self.sections:
                if section.errors:
                    print(f"\n[{section.name}]", file=sys.stderr)
                    for error in section.errors:
                        print(f"- {error}", file=sys.stderr)
            print(f"\n{total_checks} checks, {total_errors} failure(s).", file=sys.stderr)
            return 1

        print(f"Food system validation passed ({total_checks} checks across {len(self.sections)} sections).")
        return 0


def main() -> int:
    return FoodSystemValidationHarness().run()


if __name__ == "__main__":
    raise SystemExit(main())
