#!/usr/bin/env python3
"""Policy checks for Tenpack non-recipe configs.

These checks encode the current pack philosophy around factions/Create eras:
travel pressure should stay physical, survival pressure should be readable rather
than chore-heavy, death should create interaction without admin bypasses, and
stale configs for uninstalled mods should not ship to clients.
"""

from __future__ import annotations

import json
import re
import sys
import tomllib
import zipfile
import hashlib
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]

CREATE_KITCHEN_MEAL_OUTPUTS = {
    "farmersdelight:baked_cod_stew",
    "farmersdelight:beef_stew",
    "farmersdelight:bone_broth",
    "farmersdelight:chicken_soup",
    "farmersdelight:fish_stew",
    "farmersdelight:fried_rice",
    "farmersdelight:mushroom_rice",
    "farmersdelight:noodle_soup",
    "farmersdelight:onion_soup",
    "farmersdelight:pasta_with_meatballs",
    "farmersdelight:pasta_with_mutton_chop",
    "farmersdelight:pumpkin_soup",
    "farmersdelight:ratatouille",
    "farmersdelight:squid_ink_pasta",
    "farmersdelight:vegetable_noodles",
    "farmersdelight:vegetable_soup",
}

ALLOWED_ACTIVE_CONFIG_FILES = {
    "client/config/astikorcartsredux-common.toml",
    "client/config/connector.json",
    "client/config/corpse-server.toml",
    "client/config/createdieselgenerators-common.toml",
    "client/config/createrailwaysnavigator-client.toml",
    "client/config/createrailwaysnavigator-common.toml",
    "client/config/do_a_barrel_roll-client.json",
    "client/config/farmersdelight-common.toml",
    "client/config/ftbquests/quests/chapter_groups.snbt",
    "client/config/ftbquests/quests/chapters/00_tenpack_primer.snbt",
    "client/config/ftbquests/quests/chapters/01_create_eras.snbt",
    "client/config/ftbquests/quests/chapters/02_food_supply.snbt",
    "client/config/ftbquests/quests/chapters/03_rails_territory.snbt",
    "client/config/ftbquests/quests/chapters/04_oil_vehicles_war.snbt",
    "client/config/ftbquests/quests/data.snbt",
    "client/config/ftbquests/quests/lang/en_us.snbt",
    "client/config/legendarysurvivaloverhaul-client.toml",
    "client/config/legendarysurvivaloverhaul-common.toml",
    "client/config/lifesteal-common.toml",
    "client/config/moonlight-client.toml",
    "client/config/moonlight-common.toml",
    "client/config/more_darkness.json",
    "client/config/projectatmosphere/biome_temps.json",
    "client/config/simpleclouds-client.toml",
    "client/config/sound_physics_remastered/occlusion.properties",
    "client/config/sound_physics_remastered/reflectivity.properties",
    "client/config/sound_physics_remastered/soundphysics.properties",
    "client/config/sound_physics_remastered/sound_rates.properties",
    "client/config/supplementaries-client.toml",
    "client/config/supplementaries-common.toml",
    "client/config/tenpackdeath.properties",
    "client/config/trmt.json",
    "client/config/voxyworldgenv2.json",
    "client/config/windy-config.json",
    "server/config/astikorcartsredux-common.toml",
    "server/config/corpse-server.toml",
    "server/config/createdieselgenerators-common.toml",
    "server/config/createdieselgenerators-server.toml",
    "server/config/createrailwaysnavigator-common.toml",
    "server/config/do_a_barrel_roll-server.json",
    "server/config/farmersdelight-common.toml",
    "server/config/ftbquests/quests/chapter_groups.snbt",
    "server/config/ftbquests/quests/chapters/00_tenpack_primer.snbt",
    "server/config/ftbquests/quests/chapters/01_create_eras.snbt",
    "server/config/ftbquests/quests/chapters/02_food_supply.snbt",
    "server/config/ftbquests/quests/chapters/03_rails_territory.snbt",
    "server/config/ftbquests/quests/chapters/04_oil_vehicles_war.snbt",
    "server/config/ftbquests/quests/data.snbt",
    "server/config/ftbquests/quests/lang/en_us.snbt",
    "server/config/legendarysurvivaloverhaul-common.toml",
    "server/config/lifesteal-common.toml",
    "server/config/moonlight-common.toml",
    "server/config/projectatmosphere/biome_temps.json",
    "server/config/sound_physics_remastered/occlusion.properties",
    "server/config/sound_physics_remastered/reflectivity.properties",
    "server/config/sound_physics_remastered/soundphysics.properties",
    "server/config/sound_physics_remastered/sound_rates.properties",
    "server/config/supplementaries-common.toml",
    "server/config/tenpackdeath.properties",
    "server/config/trmt.json",
    "server/config/voxyworldgenv2.json",
    "server/config/windy-config.json",
}

EXPECTED_ASTIKOR_PULL_ANIMALS = {
    "supply_cart": ["minecraft:horse", "minecraft:donkey", "minecraft:mule", "minecraft:camel"],
    "animal_cart": ["minecraft:horse", "minecraft:donkey", "minecraft:mule", "minecraft:camel"],
    "plow": ["minecraft:horse", "minecraft:donkey", "minecraft:mule", "minecraft:cow"],
    "hand_cart": ["minecraft:player"],
    "reaper": ["minecraft:horse", "minecraft:donkey", "minecraft:mule", "minecraft:cow"],
    "seed_drill": ["minecraft:horse", "minecraft:donkey", "minecraft:mule", "minecraft:cow"],
}
ROUTE_JOURNAL_MODEL_DATA = 1778601
NAVIGATION_POLICY_DATAPACK = ROOT / "server/world/datapacks/tenpack-navigation-policy"
CARRYON_POLICY_DATAPACK = ROOT / "server/world/datapacks/tenpack-carryon-policy"
PRESERVED_VANILLA_STRUCTURE_MAP_TAGS = [
    "on_desert_village_maps",
    "on_jungle_explorer_maps",
    "on_ocean_explorer_maps",
    "on_plains_village_maps",
    "on_savanna_village_maps",
    "on_snowy_village_maps",
    "on_swamp_explorer_maps",
    "on_taiga_village_maps",
    "on_treasure_maps",
    "on_trial_chambers_maps",
    "on_woodland_explorer_maps",
]
PRESERVED_EMBODIED_NAVIGATION_TAGS = {
    "dolphin_located": "dolphin-led exploration is an embodied water-travel loop, not a map/compass target pointer",
    "eye_of_ender_located": "Eyes of Ender are a consumable progression mechanic, not a normal survival GPS/map system",
}
PRESERVED_NAVIGATION_POLICY_FILES = {
    "data/minecraft/recipe/ender_eye.json": "Eye of Ender crafting should not be disabled by the no-GPS datapack without a separate progression redesign",
    "data/minecraft/recipe/eye_of_ender.json": "Eye of Ender crafting should not be disabled by the no-GPS datapack without a separate progression redesign",
}
CARRYON_REQUIRED_CONTAINER_BLACKLIST = {
    "#farmersdelight:cabinets",
    "#minecraft:shulker_boxes",
    "create:item_vault",
    "farmersdelight:bamboo_basket",
    "farmersdelight:wooden_basket",
    "minecraft:barrel",
    "minecraft:blast_furnace",
    "minecraft:brewing_stand",
    "minecraft:chest",
    "minecraft:crafter",
    "minecraft:dispenser",
    "minecraft:dropper",
    "minecraft:ender_chest",
    "minecraft:furnace",
    "minecraft:hopper",
    "minecraft:smoker",
    "minecraft:trapped_chest",
    "supplementaries:sack",
    "supplementaries:safe",
}
LEASHALL_REQUIRED_BLOCKED_ENTITIES = {
    "#c:bosses",
    "#minecraft:raiders",
    "minecraft:elder_guardian",
    "minecraft:ender_dragon",
    "minecraft:player",
    "minecraft:villager",
    "minecraft:wandering_trader",
    "minecraft:warden",
    "minecraft:wither",
}

FORBIDDEN_TENPACK_TRAVEL_SOURCE_PATTERNS = [
    (r"\bParticleTypes\.GLOW\b", "role/scout feedback must not mark exact hostile target positions"),
    (r"\bMobEffects\.GLOWING\b", "no glowing-effect target highlights in natural travel systems"),
    (r"\bsetGlowingTag\s*\(", "no entity outline/highlight target markers"),
    (r"\bteleportTo\s*\(", "animal travel tools must not teleport animals or players"),
    (r"\brandomTeleport\s*\(", "animal travel tools must not teleport animals or players"),
    (r"\brequestTeleport\s*\(", "animal travel tools must not teleport animals or players"),
    (r"\bchangeDimension\s*\(", "animal travel tools must not dimension-transfer/recall animals"),
    (r"\btoShortString\s*\(", "survival travel UI must not expose exact BlockPos coordinate strings"),
]
REQUIRED_TENPACK_TRAVEL_BLOCK_DATA = {
    "channel_marker": ["recipe/channel_marker.json", "loot_table/blocks/channel_marker.json"],
    "chart_table": ["recipe/chart_table.json", "loot_table/blocks/chart_table.json"],
    "feed_trough": ["recipe/feed_trough.json", "loot_table/blocks/feed_trough.json"],
    "hitching_post": ["recipe/hitching_post.json", "loot_table/blocks/hitching_post.json"],
    "mooring_post": ["recipe/mooring_post.json", "loot_table/blocks/mooring_post.json"],
    "trail_marker": ["recipe/trail_marker.json", "loot_table/blocks/trail_marker.json"],
}
REQUIRED_TENPACK_TRAVEL_ITEM_DATA = {
    "route_journal": ["recipe/route_journal.json"],
}
TENPACK_TRAVEL_FENCE_TAG_POLICY = {
    "channel_marker": False,
    "chart_table": False,
    "hitching_post": True,
    "mooring_post": True,
    "trail_marker": False,
}

FORBIDDEN_ACTIVE_CONFIG_FRAGMENTS = {
    "improvedmobs": "Improved Mobs is not installed; stale difficulty configs confuse the pack and sync clients unnecessarily",
    "waystones": "teleportation is a hard no",
    "sophisticatedbackpacks": "backpacks are a hard no",
}

EXPECTED_CRAFTTWEAKER_SCRIPTS = {
    "scripts/tenpack_food_pressure.zs",
}

FORBIDDEN_FOOD_REMAINDER_PATH_TOKENS = {
    "spiceoflife": "no Spice of Life / Onion diet-rotation layer",
    "spice-of-life": "no Spice of Life / Onion diet-rotation layer",
    "solonion": "Supplementaries lunch basket is the chosen portable food layer",
    "sol-onion": "Supplementaries lunch basket is the chosen portable food layer",
    "toughasnails": "no thirst survival stack",
    "tough-as-nails": "no thirst survival stack",
    "hungeroverhaul": "no custom/legacy hunger-overhaul stack",
    "hunger-overhaul": "no custom/legacy hunger-overhaul stack",
    "tenpackhunger": "no custom Tenpack hunger mod/script for this pass",
    "tenpack-hunger": "no custom Tenpack hunger mod/script for this pass",
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


def read_json_from_zip(errors: list[str], zip_path: Path, member: str) -> Any | None:
    try:
        with zipfile.ZipFile(zip_path) as zf:
            return json.loads(zf.read(member).decode("utf-8"))
    except Exception as exc:  # noqa: BLE001
        errors.append(f"invalid/missing JSON {member} in {rel(zip_path)}: {exc}")
        return None


def item_tag_values(tag_json: dict[str, Any]) -> set[str]:
    values: set[str] = set()
    for value in tag_json.get("values", []):
        if isinstance(value, str):
            values.add(value)
        elif isinstance(value, dict) and isinstance(value.get("id"), str):
            values.add(value["id"])
    return values


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


def file_sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def require_same_file(errors: list[str], left: Path, right: Path, reason: str) -> None:
    if not left.exists():
        errors.append(f"missing expected file {rel(left)}")
        return
    if not right.exists():
        errors.append(f"missing expected file {rel(right)}")
        return
    if file_sha256(left) != file_sha256(right):
        errors.append(f"{rel(left)} and {rel(right)} must match: {reason}")


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


def normalized_token(text: str) -> str:
    return re.sub(r"[^a-z0-9]+", "", text.lower())


def load_manifest_paths(errors: list[str], path: Path) -> dict[str, dict[str, Any]]:
    data = read_json(errors, path)
    if not isinstance(data, dict) or not isinstance(data.get("files"), list):
        errors.append(f"{rel(path)} must be a public sync manifest with a files list")
        return {}
    entries: dict[str, dict[str, Any]] = {}
    for entry in data["files"]:
        if not isinstance(entry, dict) or not isinstance(entry.get("path"), str):
            errors.append(f"{rel(path)} contains a manifest entry without a string path")
            continue
        entries[entry["path"]] = entry
    return entries


def require_manifest_source_match(errors: list[str], manifest: dict[str, dict[str, Any]], source_root: Path, rel_path: str, side: str) -> None:
    entry = manifest.get(rel_path)
    if not entry:
        errors.append(f"{side} public manifest missing {rel_path}")
        return
    source = source_root / rel_path
    if not source.exists():
        errors.append(f"{side} public manifest lists missing source file {rel_path}")
        return
    actual_hash = file_sha256(source)
    if entry.get("sha256") != actual_hash:
        errors.append(f"{side} public manifest hash for {rel_path} is stale: expected current source sha256 {actual_hash}, got {entry.get('sha256')!r}")
    if entry.get("size") != source.stat().st_size:
        errors.append(f"{side} public manifest size for {rel_path} is stale: expected {source.stat().st_size}, got {entry.get('size')!r}")
    url = entry.get("url")
    if not isinstance(url, str):
        errors.append(f"{side} public manifest entry for {rel_path} missing string url")
        return
    blob = ROOT / "public" / url
    if not blob.exists():
        errors.append(f"{side} public manifest blob missing for {rel_path}: {url}")
        return
    if file_sha256(blob) != actual_hash:
        errors.append(f"{side} public manifest blob for {rel_path} does not match current source sha256 {actual_hash}")
    if blob.stat().st_size != source.stat().st_size:
        errors.append(f"{side} public manifest blob for {rel_path} does not match current source size {source.stat().st_size}")


def check_tenpack_travel_source_policy(errors: list[str]) -> None:
    """Keep local travel systems physical and non-GPS by construction.

    Tenpack Travel is local WIP, so a source-level guard catches accidental
    reintroduction of exact target cues, animal recall, or coordinate UI before
    the jar is rebuilt and mirrored into the pack.
    """
    source_dir = ROOT / "mods-src/tenpack-travel/src/main/java/dev/yeyito/tenpacktravel"
    if not source_dir.exists():
        errors.append(f"missing Tenpack Travel source directory {rel(source_dir)}")
        return
    compiled = [(re.compile(pattern), reason) for pattern, reason in FORBIDDEN_TENPACK_TRAVEL_SOURCE_PATTERNS]
    for path in sorted(source_dir.glob("*.java")):
        text = path.read_text(encoding="utf-8")
        for pattern, reason in compiled:
            if pattern.search(text):
                errors.append(f"{rel(path)} matches forbidden Tenpack Travel pattern {pattern.pattern!r}: {reason}")

    mounted_handler = source_dir / "MountedTravelBondHandler.java"
    if mounted_handler.exists():
        text = mounted_handler.read_text(encoding="utf-8")
        mount_method = re.search(r"onEntityMount\s*\([^)]*\)\s*\{(?P<body>.*?)\n\s*\}", text, re.DOTALL)
        if mount_method and "AnimalBond.ride" in mount_method.group("body"):
            errors.append(f"{rel(mounted_handler)} must not award ride XP from the mount event; mounting should only baseline the relationship")

    bond_source = source_dir / "AnimalBond.java"
    if bond_source.exists():
        text = bond_source.read_text(encoding="utf-8")
        for required in ["RIDE_XP_MIN_DISTANCE_SQR", "LAST_RIDE_X_KEY", "LAST_RIDE_Z_KEY"]:
            if required not in text:
                errors.append(f"{rel(bond_source)} must keep sustained-displacement guard {required} for mounted travel XP")
        for required in ["DRAFT_WORK_XP_MIN_DISTANCE_SQR", "LAST_DRAFT_X_KEY", "LAST_DRAFT_Z_KEY"]:
            if required not in text:
                errors.append(f"{rel(bond_source)} must keep sustained-displacement guard {required} for draft/cart work XP")

    mixin_config = ROOT / "mods-src/tenpack-travel/src/main/resources/tenpack_travel.mixins.json"
    mixin_data = read_json(errors, mixin_config)
    if not isinstance(mixin_data, dict) or "CrnTeleportPlayerPacketMixin" not in mixin_data.get("mixins", []):
        errors.append(f"{rel(mixin_config)} must register CrnTeleportPlayerPacketMixin to block CRN's teleport packet")

    mods_toml = ROOT / "mods-src/tenpack-travel/src/main/resources/META-INF/neoforge.mods.toml"
    if mods_toml.exists() and "tenpack_travel.mixins.json" not in mods_toml.read_text(encoding="utf-8"):
        errors.append(f"{rel(mods_toml)} must declare tenpack_travel.mixins.json")

    crn_mixin = source_dir / "mixin/CrnTeleportPlayerPacketMixin.java"
    if crn_mixin.exists():
        text = crn_mixin.read_text(encoding="utf-8")
        for required in ["TeleportPlayerPacket packet", "NetworkPacketContext context", "callback.cancel()"]:
            if required not in text:
                errors.append(f"{rel(crn_mixin)} must keep hard cancellation for CRN's teleport packet ({required})")
    else:
        errors.append(f"missing {rel(crn_mixin)}; CRN registers an otherwise unguarded teleport_player packet")

    build_gradle = ROOT / "mods-src/tenpack-travel/build.gradle"
    if build_gradle.exists():
        build_text = build_gradle.read_text(encoding="utf-8")
        for required in ["createrailwaysnavigator-neoforge-1.21.1-beta-0.9.0-C6.jar", "dragonlib-neoforge-1.21.1-beta-3.0.26.jar"]:
            if required not in build_text:
                errors.append(f"{rel(build_gradle)} must compileOnly {required} so the CRN teleport-blocking mixin uses the exact packet signature")

    crn_installed = any((ROOT / side / "mods/createrailwaysnavigator-neoforge-1.21.1-beta-0.9.0-C6.jar").exists() for side in ("client", "server"))
    if crn_installed:
        required_jar_members = {
            "tenpack_travel.mixins.json",
            "dev/yeyito/tenpacktravel/mixin/CrnTeleportPlayerPacketMixin.class",
        }
        for side in ("client", "server"):
            jar_path = ROOT / side / "mods/tenpack_travel-0.1.0.jar"
            if not jar_path.exists():
                errors.append(f"missing {rel(jar_path)}; CRN teleport-blocking mixin must ship on both sides")
                continue
            try:
                with zipfile.ZipFile(jar_path) as jar:
                    members = set(jar.namelist())
            except zipfile.BadZipFile as exc:
                errors.append(f"invalid Tenpack Travel jar {rel(jar_path)}: {exc}")
                continue
            missing = sorted(required_jar_members - members)
            if missing:
                errors.append(f"{rel(jar_path)} missing CRN teleport-blocking mixin member(s): {', '.join(missing)}")

    lodestone_handler = source_dir / "LodestoneCompassPolicyHandler.java"
    if lodestone_handler.exists():
        text = lodestone_handler.read_text(encoding="utf-8")
        for required in ["PlayerInteractEvent.RightClickBlock", "Items.COMPASS", "Blocks.LODESTONE", "message.tenpack_travel.lodestone_compass.disabled", "setCanceled(true)"]:
            if required not in text:
                errors.append(f"{rel(lodestone_handler)} must keep lodestone-compass waypoint binding blocked ({required})")
    else:
        errors.append(f"missing {rel(lodestone_handler)}; vanilla lodestone compasses create persistent waypoint arrows")

    tenpack_travel_source = source_dir / "TenpackTravel.java"
    if tenpack_travel_source.exists() and "LodestoneCompassPolicyHandler" not in tenpack_travel_source.read_text(encoding="utf-8"):
        errors.append(f"{rel(tenpack_travel_source)} must register LodestoneCompassPolicyHandler")

    travel_main = source_dir / "TenpackTravel.java"
    if travel_main.exists():
        text = travel_main.read_text(encoding="utf-8")
        trail_registration = re.search(r'TRAIL_MARKER\s*=\s*BLOCKS\.registerBlock\(.*?\);', text, re.DOTALL)
        if not trail_registration:
            errors.append(f"{rel(travel_main)} must keep an explicit Trail Marker registration")
        else:
            body = trail_registration.group(0)
            if "lightLevel(state -> 3)" not in body:
                errors.append(f"{rel(travel_main)} Trail Marker should stay at subtle light level 3 for local road readability")
            if re.search(r"lightLevel\s*\(\s*state\s*->\s*(?:1[0-5]|[4-9])\s*\)", body):
                errors.append(f"{rel(travel_main)} Trail Marker light must stay very subtle; bright beacon-like lighting is not route navigation")

        channel_registration = re.search(r'CHANNEL_MARKER\s*=\s*BLOCKS\.registerBlock\(.*?\);', text, re.DOTALL)
        if not channel_registration:
            errors.append(f"{rel(travel_main)} must keep an explicit Channel Marker registration")
        else:
            body = channel_registration.group(0)
            if "lightLevel(state -> 4)" not in body:
                errors.append(f"{rel(travel_main)} Channel Marker should stay at subtle light level 4 for local buoy visibility")
            if re.search(r"lightLevel\s*\(\s*state\s*->\s*(?:1[0-5]|[5-9])\s*\)", body):
                errors.append(f"{rel(travel_main)} Channel Marker light must stay subtle; bright beacon-like lighting is not water-route navigation")

    channel_model_path = ROOT / "mods-src/tenpack-travel/src/main/resources/assets/tenpack_travel/models/block/channel_marker.json"
    channel_model = read_json(errors, channel_model_path)
    if isinstance(channel_model, dict):
        textures = channel_model.get("textures")
        if not isinstance(textures, dict) or textures.get("lantern") != "tenpack_travel:block/channel_marker_lantern":
            errors.append(f"{rel(channel_model_path)} must keep the local lantern texture for low-light channel-marker readability")

    trail_model_path = ROOT / "mods-src/tenpack-travel/src/main/resources/assets/tenpack_travel/models/block/trail_marker_post.json"
    trail_model = read_json(errors, trail_model_path)
    if isinstance(trail_model, dict):
        textures = trail_model.get("textures")
        if not isinstance(textures, dict) or textures.get("lantern") != "tenpack_travel:block/trail_marker_lantern":
            errors.append(f"{rel(trail_model_path)} must keep the local lantern texture for low-light trail-marker readability")


def check_tenpack_travel_block_data(errors: list[str]) -> None:
    """Travel infrastructure blocks/items must be craftable and recoverable where relevant."""
    data_dir = ROOT / "mods-src/tenpack-travel/src/main/resources/data/tenpack_travel"
    for block_id, relative_paths in REQUIRED_TENPACK_TRAVEL_BLOCK_DATA.items():
        for relative in relative_paths:
            path = data_dir / relative
            if not path.exists():
                errors.append(f"Tenpack Travel block {block_id} missing data file {rel(path)}")
            elif read_json(errors, path) is None:
                errors.append(f"Tenpack Travel block {block_id} has invalid data file {rel(path)}")

    for item_id, relative_paths in REQUIRED_TENPACK_TRAVEL_ITEM_DATA.items():
        for relative in relative_paths:
            path = data_dir / relative
            if not path.exists():
                errors.append(f"Tenpack Travel item {item_id} missing data file {rel(path)}")
            elif read_json(errors, path) is None:
                errors.append(f"Tenpack Travel item {item_id} has invalid data file {rel(path)}")

    route_recipe_path = data_dir / "recipe/route_journal.json"
    route_recipe = read_json(errors, route_recipe_path)
    if isinstance(route_recipe, dict):
        result = route_recipe.get("result")
        if not isinstance(result, dict):
            errors.append(f"{rel(route_recipe_path)} must use a result object for styled vanilla-book components")
        else:
            if result.get("id") != "minecraft:writable_book":
                errors.append(f"{rel(route_recipe_path)} must output minecraft:writable_book so vanilla book edit/sign packets persist route notes")
            components = result.get("components")
            if not isinstance(components, dict):
                errors.append(f"{rel(route_recipe_path)} must set route-journal components on the vanilla writable book")
            else:
                if components.get("minecraft:custom_model_data") != ROUTE_JOURNAL_MODEL_DATA:
                    errors.append(f"{rel(route_recipe_path)} must set minecraft:custom_model_data={ROUTE_JOURNAL_MODEL_DATA}")
                custom_name = components.get("minecraft:custom_name")
                if not isinstance(custom_name, str) or "item.tenpack_travel.route_journal" not in custom_name:
                    errors.append(f"{rel(route_recipe_path)} must keep the translatable Route Journal name component")
                lore = components.get("minecraft:lore")
                if not isinstance(lore, list) or not any(isinstance(line, str) and "no_gps" in line for line in lore):
                    errors.append(f"{rel(route_recipe_path)} must keep no-GPS route-journal lore")

    fences_path = ROOT / "mods-src/tenpack-travel/src/main/resources/data/minecraft/tags/block/fences.json"
    fences = read_json(errors, fences_path)
    fence_values = set(fences.get("values", [])) if isinstance(fences, dict) else set()
    wooden_fences_path = ROOT / "mods-src/tenpack-travel/src/main/resources/data/minecraft/tags/block/wooden_fences.json"
    wooden_fences = read_json(errors, wooden_fences_path)
    wooden_fence_values = set(wooden_fences.get("values", [])) if isinstance(wooden_fences, dict) else set()
    for block_id, should_be_fence in TENPACK_TRAVEL_FENCE_TAG_POLICY.items():
        key = f"tenpack_travel:{block_id}"
        in_fences = key in fence_values
        if should_be_fence and not in_fences:
            errors.append(f"{key} must stay in minecraft:fences so vanilla lead knots work for physical tethering")
        if not should_be_fence and in_fences:
            errors.append(f"{key} must not be in minecraft:fences; it is a visual route marker, not a hitch/mooring post")
        if should_be_fence and key not in wooden_fence_values:
            errors.append(f"{key} should stay in minecraft:wooden_fences so fence connectivity matches its wooden post role")


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
    check_tenpack_travel_source_policy(errors)
    check_tenpack_travel_block_data(errors)
    check_navigation_policy_datapack(errors)
    check_carryon_policy_datapack(errors)
    check_leashall_default_config(errors)
    check_rail_navigation_configs(errors)
    check_travel_docs_policy(errors)

    require_same_file(
        errors,
        ROOT / "client/config/astikorcartsredux-common.toml",
        ROOT / "server/config/astikorcartsredux-common.toml",
        "Astikor draft-animal policy must be identical on both pack sides",
    )
    astikor = read_toml(errors, ROOT / "server/config/astikorcartsredux-common.toml")
    carts = astikor.get("carts", {}) if isinstance(astikor, dict) else {}
    for cart, expected in EXPECTED_ASTIKOR_PULL_ANIMALS.items():
        require_equal(
            errors,
            f"Astikor {cart} pull_animals",
            get_nested(carts, [cart, "pull_animals"]),
            expected,
            "cart pullers should be explicit so upstream empty-list defaults cannot become universal saddleable-animal logistics",
        )

    trmt = read_json(errors, ROOT / "server/config/trmt.json") or {}
    require_equal(errors, "TRMT player erosion multiplier", get_nested(trmt, ["erosionMultipliers", "player"]), 0.25, "roads should form slowly on foot")
    require_equal(errors, "TRMT mounted erosion multiplier", get_nested(trmt, ["erosionMultipliers", "mounted"]), 0.75, "animal/vehicle traffic should visibly shape routes")

    voxy = read_json(errors, ROOT / "server/config/voxyworldgenv2.json") or {}
    require_equal(errors, "Voxy WorldGen autoStartOnLoad", voxy.get("autoStartOnLoad"), True, "background LoD generation is expected")
    require_equal(errors, "Voxy WorldGen generationRadius", voxy.get("generationRadius"), 128, "known tested radius; changing affects server load")
    require_equal(errors, "Voxy WorldGen maxActiveTasks", voxy.get("maxActiveTasks"), 8, "smoothed client-pull concurrency; changing affects server load")

    windy = read_json(errors, ROOT / "server/config/windy-config.json") or {}
    require_equal(errors, "Windy minimumWindHeight", windy.get("minimumWindHeight"), 130, "wind should be ambience, not constant ground clutter")

    dabr = read_json(errors, ROOT / "client/config/do_a_barrel_roll-client.json") or {}
    require_equal(errors, "Do a Barrel Roll thrust", get_nested(dabr, ["general", "thrust", "enable_thrust"]), False, "client flight controls must not add personal thrust bypasses")


def check_travel_docs_policy(errors: list[str]) -> None:
    """Player-facing travel guidance must not advertise deferred/blocked navigation conveniences."""
    guide_paths = [
        ROOT / "notes/travel-questbook-guide.md",
        ROOT / "notes/travel-implementation-roadmap.md",
    ]
    feed_trough_status_paths = [
        ROOT / "notes/travel-implementation-roadmap.md",
        ROOT / "notes/travel-overhaul-master-plan.md",
    ]
    forbidden_guidance = [
        (re.compile(r"\bMap Atlas\b|\bmap/atlas\b", re.IGNORECASE), "Map Atlases are deferred until a no-GPS audit/fork; guide players to filled maps, Chart Tables, and Route Journals"),
        (re.compile(r"click\s*sign|clickable\s+sign", re.IGNORECASE), "click/command/waypoint sign mods are deferred; guide players to physical signs, way signs, Trail Markers, maps, and Route Journals"),
    ]
    stale_feed_trough_claims = [
        (re.compile(r"\bno stored feed\b", re.IGNORECASE), "Feed Trough now stores physical feed item stacks"),
        (re.compile(r"\bno fill visuals\b", re.IGNORECASE), "Feed Trough now exposes empty/low/medium/full fill visuals"),
        (re.compile(r"True stored feed inventory", re.IGNORECASE), "stored feed is implemented; remaining work is playtest/tuning"),
        (re.compile(r"Feed Trough v2.*stored feed and visual fill", re.IGNORECASE), "stored feed and visual fill are implemented; remaining work is playtest/tuning"),
        (re.compile(r"feed is deliberately consumed through interaction", re.IGNORECASE), "feed is inserted into a small physical trough store, then consumed by animals"),
    ]
    required_lunch_basket_guidance = {
        ROOT / "notes/travel-implementation-roadmap.md": [
            (re.compile(r"Supplementaries lunch basket", re.IGNORECASE), "name the allowed ration-carrier exception explicitly"),
            (re.compile(r"six-slot|6-slot|6 slots", re.IGNORECASE), "keep the basket framed as a capped six-slot food carrier"),
            (re.compile(r"field-ration|field ration|prepared food|rations", re.IGNORECASE), "frame the exception as food logistics, not general cargo"),
            (re.compile(r"Supplementaries'? (separate )?sack.*disabled|Supplementaries sacks are disabled", re.IGNORECASE), "say the general-storage sack is disabled, not a second pocket carrier"),
        ],
        ROOT / "notes/travel-questbook-guide.md": [
            (re.compile(r"Supplementaries lunch basket", re.IGNORECASE), "player-facing guide should explain the allowed ration basket"),
            (re.compile(r"six-slot|6-slot|6 slots", re.IGNORECASE), "player-facing guide should keep the basket capped at six slots"),
            (re.compile(r"not tools, ores, blocks, or general cargo", re.IGNORECASE), "player-facing guide should distinguish rations from backpack cargo"),
            (re.compile(r"Supplementaries sacks are disabled", re.IGNORECASE), "player-facing guide should point cargo pressure back to animals/carts/camps"),
        ],
        ROOT / "notes/hunger-pressure-points.md": [
            (re.compile(r"Supplementaries sack.*disabled|Supplementaries sacks are disabled", re.IGNORECASE), "food-pressure notes should preserve the disabled general-storage sack policy"),
            (re.compile(r"nine-slot|9-slot|9 slots", re.IGNORECASE), "food-pressure notes should explain why the sack is not the six-slot food exception"),
            (re.compile(r"general portable (container|storage)", re.IGNORECASE), "food-pressure notes should distinguish sacks from ration logistics"),
        ],
    }
    for path in guide_paths:
        if not path.exists():
            errors.append(f"missing travel guidance doc {rel(path)}")
            continue
        for lineno, line in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1):
            normalized = line.strip()
            if not normalized or normalized.startswith("- Current pack audit found no Click Signs"):
                continue
            for pattern, reason in forbidden_guidance:
                if pattern.search(line):
                    errors.append(f"{rel(path)}:{lineno} must not advertise deferred navigation/sign convenience {pattern.pattern!r}: {reason}")

    for path, expectations in required_lunch_basket_guidance.items():
        if not path.exists():
            errors.append(f"missing travel guidance doc {rel(path)}")
            continue
        text = path.read_text(encoding="utf-8")
        for pattern, reason in expectations:
            if not pattern.search(text):
                errors.append(f"{rel(path)} must document Supplementaries lunch basket policy: {reason}")

    for path in feed_trough_status_paths:
        if not path.exists():
            errors.append(f"missing travel status doc {rel(path)}")
            continue
        for lineno, line in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1):
            for pattern, reason in stale_feed_trough_claims:
                if pattern.search(line):
                    errors.append(f"{rel(path)}:{lineno} has stale Feed Trough docs {pattern.pattern!r}: {reason}")


def check_rail_navigation_configs(errors: list[str]) -> None:
    """Rail schedule navigation is allowed as built infrastructure, not global GPS or open global settings."""
    server_common_path = ROOT / "server/config/createrailwaysnavigator-common.toml"
    client_common_path = ROOT / "client/config/createrailwaysnavigator-common.toml"
    require_same_file(
        errors,
        client_common_path,
        server_common_path,
        "Create Railways Navigator common rail policy should not diverge between client and server packs",
    )

    common = read_toml(errors, server_common_path)
    require_equal(errors, "CRN global settings permission", get_nested(common, ["createrailwaysnavigator_common_config", "permissions", "global_settings_permission_level"]), 2, "global rail navigator settings should be op-gated, not editable by every passerby")
    require_equal(errors, "CRN admin mode permission", get_nested(common, ["createrailwaysnavigator_common_config", "permissions", "admin_mode_permission_level"]), -1, "CRN admin features include teleport/debug surfaces and should stay disabled")
    require_equal(errors, "CRN train exclusion search", get_nested(common, ["createrailwaysnavigator_common_config", "navigation", "exclude_trains"]), False, "normal route search should remain train-network infrastructure, not artificial friction")
    require_equal(errors, "CRN advanced logging", get_nested(common, ["createrailwaysnavigator_common_config", "debug", "advanced_logging"]), False, "rail navigator should not ship noisy debug logging")
    require_equal(errors, "CRN experimental simulation", get_nested(common, ["createrailwaysnavigator_common_config", "experimental", "use_new_simulation_algorithm"]), False, "do not silently enable experimental train-navigation algorithms")
    require_equal(errors, "CRN auto display type", get_nested(common, ["createrailwaysnavigator_common_config", "advanced_display", "auto_change_display_type"]), True, "station display infrastructure should keep the tested automatic display-type helper")

    client = read_toml(errors, ROOT / "client/config/createrailwaysnavigator-client.toml")
    route_overlay = get_nested(client, ["Create Railways Navigator Config", "route_overlay"])
    if not isinstance(route_overlay, dict):
        errors.append("client/config/createrailwaysnavigator-client.toml must keep explicit route_overlay settings for CRN")
    else:
        require_equal(errors, "CRN route overlay notifications", route_overlay.get("notifications"), True, "active train journey toasts are allowed station/schedule feedback, not passive GPS")
        require_equal(errors, "CRN route overlay position", route_overlay.get("position"), "TOP_LEFT", "keep current tested route overlay placement unless deliberately retuned")


def check_navigation_policy_datapack(errors: list[str]) -> None:
    """Vanilla/default recipes must not reintroduce GPS-like survival tools."""
    pack_meta = read_json(errors, NAVIGATION_POLICY_DATAPACK / "pack.mcmeta")
    if not isinstance(pack_meta, dict):
        errors.append(f"{rel(NAVIGATION_POLICY_DATAPACK / 'pack.mcmeta')} must exist for no-GPS datapack policy")

    recovery_recipe_path = NAVIGATION_POLICY_DATAPACK / "data/minecraft/recipe/recovery_compass.json"
    recipe = read_json(errors, recovery_recipe_path)
    if not isinstance(recipe, dict):
        errors.append(f"{rel(recovery_recipe_path)} must override vanilla recovery compass crafting")
        return

    result = recipe.get("result")
    if not isinstance(result, dict) or result.get("id") != "minecraft:recovery_compass":
        errors.append(f"{rel(recovery_recipe_path)} must still identify minecraft:recovery_compass as the overridden recipe")

    key = recipe.get("key")
    barrier_required = False
    if isinstance(key, dict):
        for ingredient in key.values():
            if isinstance(ingredient, dict) and ingredient.get("item") == "minecraft:barrier":
                barrier_required = True
                break
    if not barrier_required:
        errors.append(f"{rel(recovery_recipe_path)} must require minecraft:barrier so recovery compass stays uncraftable in survival")

    lodestone_recipe_path = NAVIGATION_POLICY_DATAPACK / "data/minecraft/recipe/lodestone.json"
    lodestone_recipe = read_json(errors, lodestone_recipe_path)
    if not isinstance(lodestone_recipe, dict):
        errors.append(f"{rel(lodestone_recipe_path)} must override vanilla lodestone crafting")
    else:
        result = lodestone_recipe.get("result")
        if not isinstance(result, dict) or result.get("id") != "minecraft:lodestone":
            errors.append(f"{rel(lodestone_recipe_path)} must still identify minecraft:lodestone as the overridden recipe")
        key = lodestone_recipe.get("key")
        barrier_required = False
        if isinstance(key, dict):
            for ingredient in key.values():
                if isinstance(ingredient, dict) and ingredient.get("item") == "minecraft:barrier":
                    barrier_required = True
                    break
        if not barrier_required:
            errors.append(f"{rel(lodestone_recipe_path)} must require minecraft:barrier so lodestone compass anchors stay uncraftable in survival")

    tag_root = NAVIGATION_POLICY_DATAPACK / "data/minecraft/tags/worldgen/structure"
    for tag_name in PRESERVED_VANILLA_STRUCTURE_MAP_TAGS:
        tag_path = tag_root / f"{tag_name}.json"
        if tag_path.exists():
            errors.append(f"{rel(tag_path)} must not override vanilla structure-map tags; traded and looted physical maps are allowed primitive navigation, unlike GPS/teleport/recall")

    for tag_name, reason in PRESERVED_EMBODIED_NAVIGATION_TAGS.items():
        tag_path = tag_root / f"{tag_name}.json"
        if tag_path.exists():
            errors.append(f"{rel(tag_path)} must not override vanilla {tag_name}; {reason}")

    for rel_path, reason in PRESERVED_NAVIGATION_POLICY_FILES.items():
        path = NAVIGATION_POLICY_DATAPACK / rel_path
        if path.exists():
            errors.append(f"{rel(path)} must not be overridden by tenpack-navigation-policy; {reason}")

    for path in NAVIGATION_POLICY_DATAPACK.rglob("*.json"):
        try:
            text = path.read_text(encoding="utf-8")
        except OSError:
            continue
        for token, reason in PRESERVED_EMBODIED_NAVIGATION_TAGS.items():
            if token in text:
                errors.append(f"{rel(path)} must not reference preserved vanilla locator tag {token}; {reason}")


def check_carryon_policy_datapack(errors: list[str]) -> None:
    """Carry On may help physical carrying, but must not become backpack/container cargo."""
    if not (ROOT / "server/mods/carryon-neoforge-1.21.1-2.2.4.4.jar").exists():
        return

    pack_meta_path = CARRYON_POLICY_DATAPACK / "pack.mcmeta"
    pack_meta = read_json(errors, pack_meta_path)
    if not isinstance(pack_meta, dict):
        errors.append(f"{rel(pack_meta_path)} must exist for Carry On container-cargo policy")

    for rel_path in [
        "data/carryon/tags/block/block_blacklist.json",
        "data/carryon/tags/blocks/block_blacklist.json",
    ]:
        tag_path = CARRYON_POLICY_DATAPACK / rel_path
        tag = read_json(errors, tag_path)
        if not isinstance(tag, dict):
            errors.append(f"{rel(tag_path)} must blacklist storage blocks from Carry On pickup")
            continue
        if tag.get("replace") is not False:
            errors.append(f"{rel(tag_path)} must use replace=false so Tenpack extends Carry On's own blacklist instead of deleting mod safety rules")
        values = tag.get("values")
        if not isinstance(values, list):
            errors.append(f"{rel(tag_path)} must contain a values list for Carry On block blacklist entries")
            continue
        value_set = set(values)
        missing = sorted(CARRYON_REQUIRED_CONTAINER_BLACKLIST - value_set)
        if missing:
            errors.append(f"{rel(tag_path)} missing Carry On container-blacklist entries: {', '.join(missing)}")
        allowed_missing = [value for value in values if not isinstance(value, str)]
        if allowed_missing:
            errors.append(f"{rel(tag_path)} Carry On blacklist values must be strings")


def check_leashall_default_config(errors: list[str]) -> None:
    """LeashAll may expand physical caravan handling, not boss/player/villager abuse."""
    leashall_present = any((ROOT / side / "mods").glob("leashall-*.jar") for side in ["client", "server"])
    client_config = ROOT / "client/defaultconfigs/leashall-server.toml"
    server_config = ROOT / "server/defaultconfigs/leashall-server.toml"
    config_present = client_config.exists() or server_config.exists()
    if not leashall_present and not config_present:
        return

    require_same_file(
        errors,
        client_config,
        server_config,
        "LeashAll default server policy must be mirrored so local testing and server deploys use the same physical lead rules",
    )
    config = read_toml(errors, server_config)
    require_equal(errors, "LeashAll allowlist mode", config.get("useEntityAllowList"), False, "Tenpack wants broad physical leash handling with explicit abuse blocks, not a tiny allowlist")
    require_equal(errors, "LeashAll max leashes", config.get("maxLeashesPerPlayer"), 6, "caravan handling should help groups of animals without becoming a mass-mob dragnet")
    require_equal(errors, "LeashAll allowed entity override list", config.get("allowedEntities"), [], "allowlist entries should not silently override the physical-lead policy")
    blocked_entities = config.get("blockedEntities")
    if not isinstance(blocked_entities, list):
        errors.append(f"{rel(server_config)} blockedEntities must be a TOML array")
        return
    blocked_set = set(blocked_entities)
    missing = sorted(LEASHALL_REQUIRED_BLOCKED_ENTITIES - blocked_set)
    if missing:
        errors.append(f"{rel(server_config)} missing LeashAll abuse-block entries: {', '.join(missing)}")
    non_strings = [entry for entry in blocked_entities if not isinstance(entry, str)]
    if non_strings:
        errors.append(f"{rel(server_config)} blockedEntities entries must be strings")


def check_survival_pressure_configs(errors: list[str]) -> None:
    lso = read_toml(errors, ROOT / "server/config/legendarysurvivaloverhaul-common.toml")
    core = lso.get("core", {}) if isinstance(lso, dict) else {}
    require_equal(errors, "LSO thirst", core.get("Thirst Enabled"), False, "thirst is chore pressure and is disabled")
    require_equal(errors, "LSO health overhaul", core.get("Health Overhaul Enabled"), False, "avoid hidden health-system complexity with Lifesteal")
    require_equal(errors, "LSO temperature", core.get("Temperature Enabled"), True, "temperature is the chosen survival pressure")
    misc = get_nested(lso, ["core", "misc"]) or {}
    require_equal(errors, "LSO F3 coordinate hiding", misc.get("Hide Info From Debug"), True, "survival navigation must not expose F3 position/direction as GPS")
    require_equal(errors, "LSO filled-map coordinates", misc.get("Show Coordinate On Filled Map"), False, "maps should stay physical and not print exact destination coordinates")
    require_equal(errors, "LSO compass info", misc.get("Compass Info Mode"), "NONE", "compasses must not print coordinate/direction readouts")

    food = lso.get("food", {}) if isinstance(lso, dict) else {}
    require_equal(errors, "LSO base food exhaustion", food.get("Base Food Exhaustion"), 0.0, "no idle hunger tax")
    require_equal(errors, "LSO sprinting food exhaustion", food.get("Sprinting Food Exhaustion"), 0.01, "long on-foot travel should burn weak rations fast enough that real meals matter")
    require_equal(errors, "LSO attack food exhaustion", food.get("On Attack Food Exhaustion"), 0.05, "combat should make supply quality matter without adding thirst or Nutrition categories")
    secondary = get_nested(lso, ["temperature", "secondary_effects"]) or {}
    require_equal(errors, "LSO cold hunger secondary effects", secondary.get("Cold Temperature Secondary Effects"), False, "temperature food should matter through warm/cool meals, not hidden hunger acceleration")
    require_equal(errors, "LSO cold hunger modifier", secondary.get("Cold Hunger Modifier"), 0.0, "cold should not secretly become Hunger Effect")

    require_same_file(
        errors,
        ROOT / "client/config/legendarysurvivaloverhaul-common.toml",
        ROOT / "server/config/legendarysurvivaloverhaul-common.toml",
        "LSO common survival tuning must be identical on both pack sides",
    )
    require_same_file(
        errors,
        ROOT / "client/config/farmersdelight-common.toml",
        ROOT / "server/config/farmersdelight-common.toml",
        "Farmer's Delight food-logistics config should not diverge between client and server packs",
    )

    require_same_file(
        errors,
        ROOT / "client/config/supplementaries-common.toml",
        ROOT / "server/config/supplementaries-common.toml",
        "Supplementaries lunch basket/common behavior should be identical on both pack sides",
    )
    require_same_file(
        errors,
        ROOT / "client/config/moonlight-common.toml",
        ROOT / "server/config/moonlight-common.toml",
        "Moonlight common config should not diverge between client and server packs",
    )
    supplementaries = read_toml(errors, ROOT / "server/config/supplementaries-common.toml")
    require_equal(errors, "Supplementaries lunch basket enabled", get_nested(supplementaries, ["tools", "lunch_basket", "enabled"]), True, "the portable held food basket is the intended ROTN-like basket layer")
    require_equal(errors, "Supplementaries lunch basket placeable", get_nested(supplementaries, ["tools", "lunch_basket", "placeable"]), True, "field kitchens should be able to set food down physically")
    require_equal(errors, "Supplementaries lunch basket slots", get_nested(supplementaries, ["tools", "lunch_basket", "slots"]), 6, "food carriage should help without becoming a backpack")
    require_equal(errors, "Supplementaries sack disabled", get_nested(supplementaries, ["functional", "sack", "enabled"]), False, "the lunch basket is the only allowed pocket carrier; sacks are general portable storage that bypass cart/animal cargo pressure")
    supplementaries_spec_path = ROOT / "tenpack-specs/overrides/config/supplementaries-common.toml"
    if supplementaries_spec_path.exists():
        supplementaries_spec = read_toml(errors, supplementaries_spec_path)
        require_equal(errors, "Supplementaries spec sack disabled", get_nested(supplementaries_spec, ["functional", "sack", "enabled"]), False, "reference/import overrides must not re-enable general portable storage sacks")
    require_equal(errors, "Supplementaries jar drink block", get_nested(supplementaries, ["functional", "jar", "drink_from_jar"]), False, "jars should not become a bypass food/drink dispenser")
    require_equal(errors, "Supplementaries jar drink item", get_nested(supplementaries, ["functional", "jar", "drink_from_jar_item"]), False, "jars should not become a held food/drink bypass")
    require_equal(errors, "Supplementaries globe coordinates", get_nested(supplementaries, ["building", "globe", "show_coordinates"]), False, "globe use must not print exact player coordinates")
    require_equal(errors, "Supplementaries generated road-sign structures", get_nested(supplementaries, ["building", "way_sign", "road_signs", "enabled"]), False, "navigation infrastructure should be player-built, not generated structure pointers")
    require_equal(errors, "Supplementaries generated road-sign distance", get_nested(supplementaries, ["building", "way_sign", "road_signs", "show_distance_text"]), False, "no exact structure-distance text from generated signs")
    require_equal(errors, "Supplementaries flute enabled", get_nested(supplementaries, ["tools", "flute", "enabled"]), False, "Tenpack whistle is physical pathing; Supplementaries flute can teleport/recall pets")
    require_equal(errors, "Supplementaries unbound flute", get_nested(supplementaries, ["tools", "flute", "unbound"]), False, "unbound flute would search and teleport pets")
    require_equal(errors, "Supplementaries bound flute distance", get_nested(supplementaries, ["tools", "flute", "bound_distance"]), 0, "bound flute pet teleport radius must stay disabled")
    require_equal(errors, "Supplementaries map death marker", get_nested(supplementaries, ["tweaks", "map_tweaks", "death_marker"]), "OFF", "death recovery must not become a map waypoint")
    require_equal(errors, "Supplementaries random adventurer maps", get_nested(supplementaries, ["tweaks", "map_tweaks", "random_adventurer_maps", "enabled"]), False, "cartographer trades must not become random structure-finder maps")
    require_equal(errors, "Supplementaries compass coordinate readout", get_nested(supplementaries, ["tweaks", "clock_and_compass", "compass_right_click"]), False, "right-click compass coordinates are GPS-style exact position leakage")

    supp_client = read_toml(errors, ROOT / "client/config/supplementaries-client.toml")
    require_equal(errors, "Supplementaries lunch basket overlay", get_nested(supp_client, ["items", "lunch_basket", "overlay"]), True, "players should be able to read selected basket food without inventory fiddling")

    food_script = ROOT / "server/scripts/tenpack_food_pressure.zs"
    if not food_script.exists():
        errors.append(f"missing CraftTweaker food pressure script {rel(food_script)}")
    else:
        script = food_script.read_text(encoding="utf-8")
        expected_food_sections = [
            "// --- raw/basic crops and trivially automated staples ---",
            "// Farmer's Delight raw crops: edible in an emergency, poor as a real ration.",
            "// --- single-ingredient cooked foods: decent rations, not permanent solutions ---",
            "// --- prepared foods: preserve existing effects/components, raise the payoff ---",
        ]
        for section_header in expected_food_sections:
            if section_header not in script:
                errors.append(f"CraftTweaker food script missing expected section marker: {section_header}")
        expected_food_lines = {
            "minecraft:bread": (3, 0.2),
            "minecraft:baked_potato": (3, 0.3),
            "minecraft:carrot": (2, 0.2),
            "minecraft:cooked_beef": (6, 0.5),
            "minecraft:cooked_porkchop": (6, 0.5),
            "minecraft:cooked_chicken": (5, 0.45),
            "minecraft:mushroom_stew": (7, 0.7),
            "minecraft:rabbit_stew": (10, 0.8),
            "farmersdelight:beef_stew": (10, 0.85),
            "farmersdelight:vegetable_soup": (8, 0.85),
            "farmersdelight:chicken_soup": (9, 0.85),
            "farmersdelight:noodle_soup": (10, 0.85),
            "farmersdelight:pasta_with_meatballs": (12, 0.85),
            "farmersdelight:steak_and_potatoes": (12, 0.85),
        }
        for item, (nutrition, saturation) in expected_food_lines.items():
            line = f"<item:{item}>.definition.food = <item:{item}>.food.withNutrition({nutrition}).withSaturation({saturation});"
            if line not in script:
                errors.append(f"CraftTweaker food script missing expected pressure line: {line}")

    farmers_meals = read_json_from_zip(
        errors,
        ROOT / "server/mods/FarmersDelight-1.21.1-1.3.1.jar",
        "data/farmersdelight/tags/item/meals.json",
    ) or {}
    missing_kitchen_meal_tags = CREATE_KITCHEN_MEAL_OUTPUTS - item_tag_values(farmers_meals)
    if missing_kitchen_meal_tags:
        errors.append(
            "Farmer's Delight meals tag missing Tenpack Create kitchen outputs: "
            + ", ".join(sorted(missing_kitchen_meal_tags))
        )

    farmers = read_toml(errors, ROOT / "server/config/farmersdelight-common.toml")
    require_equal(
        errors,
        "Farmer's Delight vanilla soup effects",
        get_nested(farmers, ["overrides", "enableVanillaSoupExtraEffects"]),
        True,
        "prepared/basic soups should participate in the Nourishment/Comfort food-solver loop",
    )
    require_equal(
        errors,
        "Farmer's Delight stackable soups",
        get_nested(farmers, ["overrides", "stack_size", "enableStackableSoupItems"]),
        True,
        "food logistics should be practical enough to ship without adding Nutrition-style chores",
    )

    more_darkness = read_json(errors, ROOT / "client/config/more_darkness.json") or {}
    require_equal(errors, "More Darkness caveDarkness", more_darkness.get("caveDarkness"), 0.0, "pitch-black caves are intentional but should be tracked")
    require_equal(errors, "More Darkness disableWithShaders", more_darkness.get("disableWithShaders"), False, "shader users should see the same darkness pressure unless changed intentionally")


def check_food_public_manifest_and_remnants(errors: list[str]) -> None:
    """Guard the next food pass against sync drift and old hunger-stack returns."""
    server_manifest = load_manifest_paths(errors, ROOT / "public/server-manifest.json")
    client_manifest = load_manifest_paths(errors, ROOT / "public/client-manifest.json")

    server_scripts_dir = ROOT / "server/scripts"
    actual_server_scripts = {
        path.relative_to(ROOT / "server").as_posix()
        for path in server_scripts_dir.glob("*.zs")
        if path.is_file()
    }
    missing_expected = EXPECTED_CRAFTTWEAKER_SCRIPTS - actual_server_scripts
    unexpected = actual_server_scripts - EXPECTED_CRAFTTWEAKER_SCRIPTS
    if missing_expected:
        errors.append("missing expected CraftTweaker script(s): " + ", ".join(sorted(missing_expected)))
    if unexpected:
        errors.append("unexpected unaudited CraftTweaker script(s): " + ", ".join(sorted(unexpected)))

    for rel_path in sorted(actual_server_scripts):
        require_manifest_source_match(errors, server_manifest, ROOT / "server", rel_path, "server")

    client_scripts_dir = ROOT / "client/scripts"
    client_scripts = {
        path.relative_to(ROOT / "client").as_posix()
        for path in client_scripts_dir.glob("*.zs")
        if path.is_file()
    } if client_scripts_dir.exists() else set()
    if client_scripts:
        errors.append("CraftTweaker scripts should remain server-authored only; unexpected client script(s): " + ", ".join(sorted(client_scripts)))

    client_manifest_scripts = {path for path in client_manifest if path.startswith("scripts/")}
    if client_manifest_scripts:
        errors.append("client public manifest must not ship CraftTweaker scripts: " + ", ".join(sorted(client_manifest_scripts)))

    for side in ("client", "server"):
        side_root = ROOT / side
        if not side_root.exists():
            continue
        for path in sorted(side_root.rglob("*")):
            if not path.is_file():
                continue
            rel_path = path.relative_to(side_root).as_posix()
            compact = normalized_token(rel_path)
            dashed = rel_path.lower()
            for token, reason in FORBIDDEN_FOOD_REMAINDER_PATH_TOKENS.items():
                if normalized_token(token) in compact or token in dashed:
                    errors.append(f"forbidden legacy food/hunger remnant {side}/{rel_path}: {reason}")


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


def check_client_performance_configs(errors: list[str]) -> None:
    simpleclouds = read_toml(errors, ROOT / "client/config/simpleclouds-client.toml")
    performance = simpleclouds.get("performance", {}) if isinstance(simpleclouds, dict) else {}
    mesh = performance.get("mesh_generation", {}) if isinstance(performance, dict) else {}
    distant_horizons = simpleclouds.get("distant_horizons", {}) if isinstance(simpleclouds, dict) else {}
    require_equal(errors, "Simple Clouds mesh generationInterval", mesh.get("generationInterval"), "TARGET_FPS", "restore default continuous cloud mesh pacing for visual comparison")
    require_equal(errors, "Simple Clouds framesToGenerateMesh", mesh.get("framesToGenerateMesh"), 1, "restore default one-frame mesh generation for visual comparison")
    require_equal(errors, "Simple Clouds levelOfDetail", performance.get("levelOfDetail"), "HIGH", "keep the intended Simple Clouds visual quality")
    require_equal(errors, "Simple Clouds shadowDistance", distant_horizons.get("shadowDistance"), 4096, "restore default cloud shadow distance for visual comparison")


def main() -> int:
    errors: list[str] = []
    check_active_config_allowlist(errors)
    check_connector_config(errors)
    check_client_performance_configs(errors)
    check_travel_and_world_configs(errors)
    check_survival_pressure_configs(errors)
    check_food_public_manifest_and_remnants(errors)
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
