#!/usr/bin/env python3
"""Check client resourcepack/shaderpack metadata and packaging.

The client visual stack affects accessibility: it can help Create factories,
travel, darkness, and faction identity read better, or it can hide mechanics and
hurt performance. This checker makes the resource/shader choices explicit.
"""

from __future__ import annotations

import json
import re
import struct
import sys
import zipfile
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
RESOURCEPACK_DIR = ROOT / "client" / "resourcepacks"
SHADERPACK_DIR = ROOT / "client" / "shaderpacks"
TENPACK_TRAVEL_ASSETS = ROOT / "mods-src" / "tenpack-travel" / "src" / "main" / "resources" / "assets" / "tenpack_travel"
TENPACK_TRAVEL_JAVA = ROOT / "mods-src" / "tenpack-travel" / "src" / "main" / "java" / "dev" / "yeyito" / "tenpacktravel"
MC = "1.21.1"
MC_RESOURCE_PACK_FORMAT = 34
ROUTE_JOURNAL_MODEL_DATA = 1778601
REQUIRED_TENPACK_TRAVEL_LANG_KEYS = {
    "block.tenpack_travel.channel_marker",
    "block.tenpack_travel.channel_marker.tooltip.infrastructure",
    "block.tenpack_travel.channel_marker.tooltip.no_gps",
    "block.tenpack_travel.chart_table",
    "block.tenpack_travel.chart_table.tooltip.infrastructure",
    "block.tenpack_travel.chart_table.tooltip.no_gps",
    "block.tenpack_travel.feed_trough",
    "block.tenpack_travel.hitching_post",
    "block.tenpack_travel.mooring_post",
    "block.tenpack_travel.mooring_post.tooltip.infrastructure",
    "block.tenpack_travel.mooring_post.tooltip.no_gps",
    "block.tenpack_travel.trail_marker",
    "block.tenpack_travel.trail_marker.tooltip.infrastructure",
    "block.tenpack_travel.trail_marker.tooltip.no_gps",
    "item.tenpack_travel.grooming_brush",
    "item.tenpack_travel.grooming_brush.already_groomed",
    "item.tenpack_travel.grooming_brush.groomed_today",
    "item.tenpack_travel.grooming_brush.no_notes",
    "item.tenpack_travel.grooming_brush.tooltip.groom",
    "item.tenpack_travel.grooming_brush.tooltip.notes",
    "item.tenpack_travel.route_journal",
    "item.tenpack_travel.route_journal.tooltip.write",
    "item.tenpack_travel.route_journal.tooltip.no_gps",
    "item.tenpack_travel.whistle",
    "item.tenpack_travel.whistle.tooltip.call",
    "item.tenpack_travel.whistle.tooltip.commands",
}
REQUIRED_TENPACK_TRAVEL_BLOCK_ASSETS = {
    "channel_marker": ["models/block/channel_marker.json", "models/item/channel_marker.json", "blockstates/channel_marker.json"],
    "chart_table": ["models/block/chart_table.json", "models/item/chart_table.json", "blockstates/chart_table.json"],
    "feed_trough": ["models/block/feed_trough.json", "models/item/feed_trough.json", "blockstates/feed_trough.json"],
    "hitching_post": ["models/block/hitching_post_post.json", "models/item/hitching_post.json", "blockstates/hitching_post.json"],
    "mooring_post": ["models/block/mooring_post_post.json", "models/item/mooring_post.json", "blockstates/mooring_post.json"],
    "trail_marker": ["models/block/trail_marker_post.json", "models/item/trail_marker.json", "blockstates/trail_marker.json"],
}
REQUIRED_TENPACK_TRAVEL_BLOCK_TEXTURES = {
    "channel_marker_wood.png": (16, 16),
    "channel_marker_paint.png": (16, 16),
    "channel_marker_lantern.png": (16, 16),
    "trail_marker_wood.png": (16, 16),
    "trail_marker_paint.png": (16, 16),
    "trail_marker_lantern.png": (16, 16),
    "mooring_post_wood.png": (16, 16),
    "mooring_post_metal.png": (16, 16),
    "chart_table_top.png": (16, 16),
    "chart_table_side.png": (16, 16),
}
REQUIRED_TENPACK_TRAVEL_ITEM_TEXTURES = {
    "grooming_brush.png": (16, 16),
    "route_journal.png": (16, 16),
    "whistle.png": (16, 16),
}
REQUIRED_TENPACK_TRAVEL_ITEM_MODELS = {
    "route_journal": ["models/item/route_journal.json"],
}
REQUIRED_MINECRAFT_MODEL_OVERRIDES = [
    "models/item/writable_book.json",
    "models/item/written_book.json",
]
ROUTE_JOURNAL_MODEL = "tenpack_travel:item/route_journal"


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


def png_dimensions(path: Path) -> tuple[int, int] | None:
    try:
        with path.open("rb") as handle:
            header = handle.read(24)
        if header[:8] != b"\x89PNG\r\n\x1a\n" or header[12:16] != b"IHDR":
            return None
        return struct.unpack(">II", header[16:24])
    except OSError:
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


def check_tenpack_travel_assets(errors: list[str]) -> None:
    """Keep Tenpack Travel's animal/travel presentation from regressing to unnamed placeholders."""
    lang_path = TENPACK_TRAVEL_ASSETS / "lang" / "en_us.json"
    lang = read_json(errors, lang_path)
    if isinstance(lang, dict):
        missing = sorted(key for key in REQUIRED_TENPACK_TRAVEL_LANG_KEYS if key not in lang)
        if missing:
            errors.append(f"{lang_path.relative_to(ROOT)} missing Tenpack Travel language keys: {missing}")
        used_keys: set[str] = set()
        for java_path in sorted(TENPACK_TRAVEL_JAVA.glob("*.java")):
            text = java_path.read_text(encoding="utf-8")
            used_keys.update(re.findall(r'Component\.translatable\("((?:block|item|screen|message|key|itemGroup)\.tenpack_travel[^"]*)"', text))
        missing_used = sorted(key for key in used_keys if key not in lang)
        if missing_used:
            errors.append(f"{lang_path.relative_to(ROOT)} missing Tenpack Travel translatable keys used by Java: {missing_used}")

    literal_tooltip_pattern = re.compile(r'appendHoverText\s*\([^)]*\)\s*\{(?P<body>.*?)\n\s*\}', re.DOTALL)
    for java_path in sorted(TENPACK_TRAVEL_JAVA.glob("*.java")):
        text = java_path.read_text(encoding="utf-8")
        for match in literal_tooltip_pattern.finditer(text):
            if "Component.literal" in match.group("body"):
                errors.append(
                    f"{java_path.relative_to(ROOT)} appendHoverText must use translatable Tenpack Travel tooltip keys, not Component.literal"
                )

    report_path = TENPACK_TRAVEL_JAVA / "AnimalInspectionReport.java"
    if report_path.exists():
        report_text = report_path.read_text(encoding="utf-8")
        lines_match = re.search(r'List<Component>\s+lines\s*\(boolean exact\)\s*\{(?P<body>.*?)\n\s*return lines;', report_text, re.DOTALL)
        if lines_match and "Component.literal" in lines_match.group("body"):
            errors.append(
                f"{report_path.relative_to(ROOT)} lines(boolean exact) must keep fallback animal-inspection report text translatable"
            )
        if '"Pack — ' in report_text or '"Potential pack — add chest"' in report_text:
            errors.append(
                f"{report_path.relative_to(ROOT)} vanilla pack-role labels must use Tenpack Travel translation keys, not hard-coded English"
            )
        hardcoded_vanilla_inspection = [
            '"Steady"',
            '"Passenger mount"',
            '"two riders; poor cargo, good road travel comfort"',
            '"Calm"',
            '"tamed"',
            '"untamed; ride/training still matters"',
            '"Skittish"',
            '"Learning"',
            '"Trusting"',
            '"Nearly tamed"',
            '"Loyal"',
            '"Wild"',
        ]
        for literal in hardcoded_vanilla_inspection:
            if literal in report_text:
                errors.append(
                    f"{report_path.relative_to(ROOT)} vanilla/camel inspection label {literal} must use Tenpack Travel translation keys"
                )
        for key in [
            "message.tenpack_travel.animal_role.pack_slots",
            "message.tenpack_travel.animal_role.pack_candidate",
            "message.tenpack_travel.animal_role.passenger_mount",
            "message.tenpack_travel.animal_temperament.camel_steady",
            "message.tenpack_travel.animal_temperament.horse_calm",
            "message.tenpack_travel.animal_temperament.horse_skittish",
            "message.tenpack_travel.animal_temperament.horse_learning",
            "message.tenpack_travel.animal_temperament.horse_trusting",
            "message.tenpack_travel.animal_temperament.horse_nearly_tamed",
            "message.tenpack_travel.animal_notes.camel_two_riders",
            "message.tenpack_travel.animal_notes.tamed",
            "message.tenpack_travel.animal_notes.horse_untamed_training",
            "message.tenpack_travel.animal_temperament.tame_loyal",
            "message.tenpack_travel.animal_temperament.tame_wild",
        ]:
            if key not in report_text:
                errors.append(f"{report_path.relative_to(ROOT)} must keep localized vanilla/camel inspection key {key}")

    inspection_payload_path = TENPACK_TRAVEL_JAVA / "AnimalInspectionPayload.java"
    if inspection_payload_path.exists():
        payload_text = inspection_payload_path.read_text(encoding="utf-8")
        hardcoded_payload_formatting = [
            '"—"',
            '" — "',
            '"health "',
            '", speed "',
            '", jump "',
            '", bond xp "',
        ]
        for literal in hardcoded_payload_formatting:
            if literal in payload_text:
                errors.append(f"{inspection_payload_path.relative_to(ROOT)} UI/debug formatting {literal} must use Tenpack Travel translation keys")
        for key in [
            "screen.tenpack_travel.not_available",
            "screen.tenpack_travel.animal_care.health_summary",
            "screen.tenpack_travel.animal_care.debug_values",
            "screen.tenpack_travel.animal_care.debug_speed",
            "screen.tenpack_travel.animal_care.debug_jump",
        ]:
            if key not in payload_text:
                errors.append(f"{inspection_payload_path.relative_to(ROOT)} must keep localized animal-inspection payload formatting key {key}")

    alex_role_path = TENPACK_TRAVEL_JAVA / "AlexAnimalRole.java"
    if alex_role_path.exists():
        alex_text = alex_role_path.read_text(encoding="utf-8")
        if 'new AlexAnimalRole("' in alex_text:
            errors.append(
                f"{alex_role_path.relative_to(ROOT)} must use generated Tenpack Travel translation keys, not hard-coded Alex animal role strings"
            )
        if "message.tenpack_travel.alex_animal_role." not in alex_text:
            errors.append(f"{alex_role_path.relative_to(ROOT)} must keep the Alex animal role translation-key prefix")
        alex_paths = sorted(set(re.findall(r'case\s+"([^"]+)"\s*->\s*localized\(path\);', alex_text)))
        if isinstance(lang, dict):
            for path_name in alex_paths:
                for field in ("role", "temperament", "notes"):
                    key = f"message.tenpack_travel.alex_animal_role.{path_name}.{field}"
                    if key not in lang:
                        errors.append(f"{lang_path.relative_to(ROOT)} missing localized Alex animal role key {key}")

    astikor_path = TENPACK_TRAVEL_JAVA / "AstikorIntegration.java"
    if astikor_path.exists():
        astikor_text = astikor_path.read_text(encoding="utf-8")
        if '"Working draft' in astikor_text or '"Mount / draft candidate"' in astikor_text:
            errors.append(
                f"{astikor_path.relative_to(ROOT)} draft-role labels must use Tenpack Travel translation keys, not hard-coded English"
            )
        for key in ["message.tenpack_travel.animal_role.draft_active", "message.tenpack_travel.animal_role.draft_candidate"]:
            if key not in astikor_text:
                errors.append(f"{astikor_path.relative_to(ROOT)} must keep localized Astikor draft role key {key}")

    bond_path = TENPACK_TRAVEL_JAVA / "AnimalBond.java"
    if bond_path.exists():
        bond_text = bond_path.read_text(encoding="utf-8")
        for literal in ['"Companion"', '"Loyal"', '"Trusted"', '"Familiar"', '"New bond"', '"Unbonded"']:
            if literal in bond_text:
                errors.append(f"{bond_path.relative_to(ROOT)} bond label {literal} must use Tenpack Travel translation keys")
        for key in [
            "message.tenpack_travel.animal_bond.companion",
            "message.tenpack_travel.animal_bond.loyal",
            "message.tenpack_travel.animal_bond.trusted",
            "message.tenpack_travel.animal_bond.familiar",
            "message.tenpack_travel.animal_bond.new_bond",
            "message.tenpack_travel.animal_bond.unbonded",
        ]:
            if key not in bond_text:
                errors.append(f"{bond_path.relative_to(ROOT)} must keep localized bond label key {key}")

    care_path = TENPACK_TRAVEL_JAVA / "AnimalCare.java"
    if care_path.exists():
        care_text = care_path.read_text(encoding="utf-8")
        hardcoded_care_literals = [
            '"Gone"',
            '"Hurt"',
            '"Worked"',
            '"Tired"',
            '"Content"',
            '"Calm"',
            '"Settled"',
            '"Hungry"',
            '"Watchful"',
            '"worked today; groomed and fed"',
            '"groomed and fed today"',
            '"worked today; groomed, needs feed"',
            '"groomed today; feed if resting at camp"',
            '"worked today; fed, grooming would calm them"',
            '"fed today; grooming would calm them"',
            '"worked today; needs camp care"',
            '"needs daily care"',
            '"no Tenpack care recorded yet"',
        ]
        for literal in hardcoded_care_literals:
            if literal in care_text:
                errors.append(f"{care_path.relative_to(ROOT)} care/mood label {literal} must use Tenpack Travel translation keys")
        for key in [
            "message.tenpack_travel.animal_mood.gone",
            "message.tenpack_travel.animal_mood.hurt",
            "message.tenpack_travel.animal_mood.worked",
            "message.tenpack_travel.animal_mood.tired",
            "message.tenpack_travel.animal_mood.content",
            "message.tenpack_travel.animal_mood.calm",
            "message.tenpack_travel.animal_mood.settled",
            "message.tenpack_travel.animal_mood.hungry",
            "message.tenpack_travel.animal_mood.watchful",
            "message.tenpack_travel.animal_care.worked_groomed_fed",
            "message.tenpack_travel.animal_care.groomed_fed",
            "message.tenpack_travel.animal_care.worked_groomed_needs_feed",
            "message.tenpack_travel.animal_care.groomed_needs_feed",
            "message.tenpack_travel.animal_care.worked_fed_needs_grooming",
            "message.tenpack_travel.animal_care.fed_needs_grooming",
            "message.tenpack_travel.animal_care.worked_needs_care",
            "message.tenpack_travel.animal_care.needs_daily_care",
            "message.tenpack_travel.animal_care.no_care_recorded",
        ]:
            if key not in care_text:
                errors.append(f"{care_path.relative_to(ROOT)} must keep localized care/mood key {key}")

    stat_bands_path = TENPACK_TRAVEL_JAVA / "AnimalStatBands.java"
    if stat_bands_path.exists():
        stat_bands_text = stat_bands_path.read_text(encoding="utf-8")
        hardcoded_stat_band_literals = [
            '"unknown"',
            '"hurt"',
            '"worn"',
            '"healthy"',
            '"fresh"',
            '"Frail"',
            '"Healthy"',
            '"Sturdy"',
            '"Massive"',
            '"Slow"',
            '"Steady"',
            '"Swift"',
            '"Exceptional"',
            '"Poor"',
            '"Fair"',
            '"Strong"',
            '"Remarkable"',
        ]
        for literal in hardcoded_stat_band_literals:
            if literal in stat_bands_text:
                errors.append(f"{stat_bands_path.relative_to(ROOT)} stat-band label {literal} must use Tenpack Travel translation keys")
        if "String.format(Locale.ROOT, \"%.3f\", value)" not in stat_bands_text:
            errors.append(f"{stat_bands_path.relative_to(ROOT)} must keep exact numeric formatting isolated to rounded() debug output")
        for key in [
            "message.tenpack_travel.animal_stat.health_state.unknown",
            "message.tenpack_travel.animal_stat.health_state.hurt",
            "message.tenpack_travel.animal_stat.health_state.worn",
            "message.tenpack_travel.animal_stat.health_state.healthy",
            "message.tenpack_travel.animal_stat.health_state.fresh",
            "message.tenpack_travel.animal_stat.max_health.frail",
            "message.tenpack_travel.animal_stat.max_health.healthy",
            "message.tenpack_travel.animal_stat.max_health.sturdy",
            "message.tenpack_travel.animal_stat.max_health.massive",
            "message.tenpack_travel.animal_stat.speed.slow",
            "message.tenpack_travel.animal_stat.speed.steady",
            "message.tenpack_travel.animal_stat.speed.swift",
            "message.tenpack_travel.animal_stat.speed.exceptional",
            "message.tenpack_travel.animal_stat.jump.poor",
            "message.tenpack_travel.animal_stat.jump.fair",
            "message.tenpack_travel.animal_stat.jump.strong",
            "message.tenpack_travel.animal_stat.jump.remarkable",
        ]:
            if key not in stat_bands_text:
                errors.append(f"{stat_bands_path.relative_to(ROOT)} must keep localized stat-band key {key}")

    command_path = TENPACK_TRAVEL_JAVA / "AnimalCommand.java"
    if command_path.exists():
        command_text = command_path.read_text(encoding="utf-8")
        for literal in ['"Free"', '"Follow"', '"Stay"', '"Roam"']:
            if literal in command_text:
                errors.append(f"{command_path.relative_to(ROOT)} command mode label {literal} must use Tenpack Travel translation keys")
        for key in [
            "screen.tenpack_travel.command.free",
            "screen.tenpack_travel.command.follow",
            "screen.tenpack_travel.command.stay",
            "screen.tenpack_travel.command.roam",
        ]:
            if key not in command_text:
                errors.append(f"{command_path.relative_to(ROOT)} must keep localized command mode key {key}")

    hitch_payload_path = TENPACK_TRAVEL_JAVA / "HitchingPostPayload.java"
    if hitch_payload_path.exists():
        hitch_payload_text = hitch_payload_path.read_text(encoding="utf-8")
        for literal in ['"Hitched"', '"Remembered"', '"last state unknown"', '"at post"', '"near post"', '"nearby"', '"away"', '"—"']:
            if literal in hitch_payload_text:
                errors.append(f"{hitch_payload_path.relative_to(ROOT)} stable-board label {literal} must use Tenpack Travel translation keys")
        for key in [
            "screen.tenpack_travel.not_available",
            "message.tenpack_travel.hitching_post.row_state.hitched",
            "message.tenpack_travel.hitching_post.row_state.remembered",
            "message.tenpack_travel.hitching_post.row_state.unknown_care",
            "message.tenpack_travel.hitching_post.proximity.at_post",
            "message.tenpack_travel.hitching_post.proximity.near_post",
            "message.tenpack_travel.hitching_post.proximity.nearby",
            "message.tenpack_travel.hitching_post.proximity.away",
        ]:
            if key not in hitch_payload_text:
                errors.append(f"{hitch_payload_path.relative_to(ROOT)} must keep localized stable-board key {key}")

    hitch_screen_path = TENPACK_TRAVEL_JAVA / "HitchingPostScreen.java"
    if hitch_screen_path.exists():
        hitch_screen_text = hitch_screen_path.read_text(encoding="utf-8")
        if 'row.species() + " — " + row.role()' in hitch_screen_text:
            errors.append(f"{hitch_screen_path.relative_to(ROOT)} stable-board species/role separator must use a translation key")
        if "screen.tenpack_travel.hitching_post.row_species_role" not in hitch_screen_text:
            errors.append(f"{hitch_screen_path.relative_to(ROOT)} must keep localized row_species_role formatter")

    hitch_entity_path = TENPACK_TRAVEL_JAVA / "HitchingPostBlockEntity.java"
    if hitch_entity_path.exists():
        hitch_entity_text = hitch_entity_path.read_text(encoding="utf-8")
        for literal in ['"last seen earlier"', '"seen just now"', '"last seen "']:
            if literal in hitch_entity_text:
                errors.append(f"{hitch_entity_path.relative_to(ROOT)} remembered-animal age label {literal} must use Tenpack Travel translation keys")
        for key in [
            "message.tenpack_travel.hitching_post.memory.last_seen_earlier",
            "message.tenpack_travel.hitching_post.memory.seen_just_now",
            "message.tenpack_travel.hitching_post.memory.last_seen_minutes",
        ]:
            if key not in hitch_entity_text:
                errors.append(f"{hitch_entity_path.relative_to(ROOT)} must keep localized remembered-animal age key {key}")

    texture_dir = TENPACK_TRAVEL_ASSETS / "textures" / "item"
    for filename, expected_size in REQUIRED_TENPACK_TRAVEL_ITEM_TEXTURES.items():
        path = texture_dir / filename
        size = png_dimensions(path)
        if size is None:
            errors.append(f"{path.relative_to(ROOT)} is missing or not a readable PNG")
        elif size != expected_size:
            errors.append(f"{path.relative_to(ROOT)} must be {expected_size[0]}x{expected_size[1]} PNG, got {size[0]}x{size[1]}")

    block_texture_dir = TENPACK_TRAVEL_ASSETS / "textures" / "block"
    for filename, expected_size in REQUIRED_TENPACK_TRAVEL_BLOCK_TEXTURES.items():
        path = block_texture_dir / filename
        size = png_dimensions(path)
        if size is None:
            errors.append(f"{path.relative_to(ROOT)} is missing or not a readable PNG")
        elif size != expected_size:
            errors.append(f"{path.relative_to(ROOT)} must be {expected_size[0]}x{expected_size[1]} PNG, got {size[0]}x{size[1]}")

    for block_id, relative_paths in REQUIRED_TENPACK_TRAVEL_BLOCK_ASSETS.items():
        for relative in relative_paths:
            path = TENPACK_TRAVEL_ASSETS / relative
            if not path.exists():
                errors.append(f"Tenpack Travel block {block_id} missing asset {path.relative_to(ROOT)}")

    for item_id, relative_paths in REQUIRED_TENPACK_TRAVEL_ITEM_MODELS.items():
        for relative in relative_paths:
            path = TENPACK_TRAVEL_ASSETS / relative
            if not path.exists():
                errors.append(f"Tenpack Travel item {item_id} missing asset {path.relative_to(ROOT)}")

    route_model_path = TENPACK_TRAVEL_ASSETS / "models/item/route_journal.json"
    route_model = read_json(errors, route_model_path)
    if isinstance(route_model, dict):
        if route_model.get("parent") != "minecraft:item/generated":
            errors.append(f"{route_model_path.relative_to(ROOT)} must stay a generated item model")
        layer0 = route_model.get("textures", {}).get("layer0") if isinstance(route_model.get("textures"), dict) else None
        if layer0 != "tenpack_travel:item/route_journal":
            errors.append(f"{route_model_path.relative_to(ROOT)} must use tenpack_travel:item/route_journal as layer0")

    minecraft_assets = ROOT / "mods-src" / "tenpack-travel" / "src" / "main" / "resources" / "assets" / "minecraft"
    for relative in REQUIRED_MINECRAFT_MODEL_OVERRIDES:
        path = minecraft_assets / relative
        if not path.exists():
            errors.append(f"Tenpack Travel route journal missing vanilla model override {path.relative_to(ROOT)}")
            continue
        model = read_json(errors, path)
        if not isinstance(model, dict):
            continue
        overrides = model.get("overrides")
        if not isinstance(overrides, list):
            errors.append(f"{path.relative_to(ROOT)} must keep a route-journal custom_model_data override")
            continue
        found = False
        for override in overrides:
            if not isinstance(override, dict):
                continue
            predicate = override.get("predicate")
            if not isinstance(predicate, dict):
                continue
            if predicate.get("custom_model_data") == ROUTE_JOURNAL_MODEL_DATA and override.get("model") == ROUTE_JOURNAL_MODEL:
                found = True
                break
        if not found:
            errors.append(
                f"{path.relative_to(ROOT)} must map custom_model_data={ROUTE_JOURNAL_MODEL_DATA} "
                f"to {ROUTE_JOURNAL_MODEL} so route journals keep their presentation before/after signing"
            )


def main() -> int:
    errors: list[str] = []
    check_resourcepacks(errors)
    check_shaderpacks(errors)
    check_tenpack_travel_assets(errors)
    if errors:
        print("Client asset check failed:", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1
    print("Client asset checks passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
