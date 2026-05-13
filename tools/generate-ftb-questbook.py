#!/usr/bin/env python3
"""Generate Tenpack's initial FTB Quests questbook.

FTB Quests loads pack-authored quests from config/ftbquests/quests on the
logical side's config folder. Tenpack ships the same source questbook in both
client/ and server/ so single-player/client UI and dedicated-server delivery stay
in sync.
"""

from __future__ import annotations

import hashlib
import io
import re
import shutil
import zipfile
from dataclasses import dataclass, field
from pathlib import Path
from typing import Iterable


ROOT = Path(__file__).resolve().parents[1]
SIDES = ("client", "server")
QUEST_REL = Path("config/ftbquests/quests")
VERSION = 13
ITEM_ID_RE = re.compile(r"^[a-z0-9_.-]+:[a-z0-9_./-]+$")

# Vanilla assets are not shipped in Tenpack's mod jars, so the local jar asset
# scan cannot prove these. Keep this list intentionally tiny: every modded icon
# should be backed by an installed jar model, including JarJar/nested mods.
VANILLA_ICON_IDS = {
    "minecraft:filled_map",
}


@dataclass(frozen=True)
class Task:
    kind: str
    target: str | None = None
    item: str | None = None
    count: int = 1


@dataclass(frozen=True)
class Quest:
    slug: str
    title: str
    desc: tuple[str, ...]
    x: float
    y: float
    icon: str
    tasks: tuple[Task, ...]
    deps: tuple[str, ...] = ()
    subtitle: str = ""
    optional: bool = False
    shape: str = ""
    disable_toast: bool = False


@dataclass(frozen=True)
class Chapter:
    slug: str
    filename: str
    title: str
    subtitle: tuple[str, ...]
    icon: str
    quests: tuple[Quest, ...]
    default_shape: str = ""


def code_id(slug: str) -> str:
    raw = int.from_bytes(hashlib.sha1(slug.encode("utf-8")).digest()[:8], "big")
    raw &= 0x7FFF_FFFF_FFFF_FFFF
    if raw in (0, 1):
        raw += 2
    return f"{raw:016X}"


def q(s: str) -> str:
    return '"' + s.replace('\\', '\\\\').replace('"', '\\"').replace('\n', '\\n') + '"'


def item_stack(item: str) -> str:
    return f'{{id: {q(item)}, count: 1}}'


def list_of_strings(values: Iterable[str]) -> str:
    values = list(values)
    if not values:
        return "[]"
    return "[\n" + "\n".join(f"    {q(v)}" for v in values) + "\n  ]"


def task_snbt(task: Task, quest_slug: str, index: int) -> str:
    tid = code_id(f"task:{quest_slug}:{index}:{task.kind}:{task.target or task.item or ''}")
    lines = [f"      id: {q(tid)}", f"      type: {q(task.kind)}"]
    if task.kind == "advancement":
        if not task.target:
            raise ValueError(f"advancement task for {quest_slug} is missing target")
        lines.append(f"      advancement: {q(task.target)}")
        lines.append('      criterion: ""')
    elif task.kind == "item":
        if not task.item:
            raise ValueError(f"item task for {quest_slug} is missing item")
        lines.append(f"      item: {item_stack(task.item)}")
        if task.count > 1:
            lines.append(f"      count: {task.count}L")
    elif task.kind == "checkmark":
        pass
    else:
        raise ValueError(f"unsupported task kind {task.kind!r}")
    return "    {\n" + "\n".join(lines) + "\n    }"


def quest_snbt(quest: Quest) -> str:
    qid = code_id(f"quest:{quest.slug}")
    lines = [
        f"    id: {q(qid)}",
        f"    x: {quest.x:.1f}d",
        f"    y: {quest.y:.1f}d",
        f"    icon: {item_stack(quest.icon)}",
    ]
    if quest.shape:
        lines.append(f"    shape: {q(quest.shape)}")
    if quest.disable_toast:
        lines.append("    disable_toast: true")
    if quest.optional:
        lines.append("    optional: true")
    if quest.deps:
        deps = ", ".join(q(code_id(f"quest:{dep}")) for dep in quest.deps)
        lines.append(f"    dependencies: [{deps}]")
    if quest.tasks:
        tasks = ",\n".join(task_snbt(task, quest.slug, i) for i, task in enumerate(quest.tasks))
        lines.append("    tasks: [\n" + tasks + "\n    ]")
    return "  {\n" + "\n".join(lines) + "\n  }"


def chapter_snbt(chapter: Chapter, order_index: int) -> str:
    cid = code_id(f"chapter:{chapter.slug}")
    quests = ",\n".join(quest_snbt(quest) for quest in chapter.quests)
    return "\n".join(
        [
            "{",
            f"  id: {q(cid)}",
            '  group: ""',
            f"  order_index: {order_index}",
            f"  filename: {q(chapter.filename)}",
            f"  icon: {item_stack(chapter.icon)}",
            f"  default_quest_shape: {q(chapter.default_shape)}",
            "  default_hide_dependency_lines: false",
            "  quests: [",
            quests,
            "  ]",
            "  quest_links: []",
            "  images: []",
            "}",
            "",
        ]
    )


def data_snbt() -> str:
    return """{
  version: 13
  default_reward_team: false
  default_consume_items: false
  default_autoclaim_rewards: "disabled"
  default_quest_shape: ""
  default_quest_disable_jei: false
  emergency_items_cooldown: 300
  drop_loot_crates: false
  loot_crate_no_drop: {passive: 4000, monster: 600, boss: 0}
  disable_gui: false
  grid_scale: 0.5d
  pause_game: false
  lock_message: ""
  progression_mode: "flexible"
  detection_delay: 20
  show_lock_icons: false
  drop_book_on_death: false
  hide_excluded_quests: false
  fallback_locale: "en_us"
  verify_on_load: true
}
"""


def chapter_groups_snbt() -> str:
    return "{\n  chapter_groups: []\n}\n"


def lang_snbt(chapters: tuple[Chapter, ...]) -> str:
    entries: dict[str, str | tuple[str, ...]] = {}
    for chapter in chapters:
        cid = code_id(f"chapter:{chapter.slug}")
        entries[f"chapter.{cid}.title"] = chapter.title
        if chapter.subtitle:
            entries[f"chapter.{cid}.chapter_subtitle"] = chapter.subtitle
        for quest in chapter.quests:
            qid = code_id(f"quest:{quest.slug}")
            entries[f"quest.{qid}.title"] = quest.title
            if quest.subtitle:
                entries[f"quest.{qid}.quest_subtitle"] = quest.subtitle
            if quest.desc:
                entries[f"quest.{qid}.quest_desc"] = quest.desc

    lines = ["{"]
    for key in sorted(entries):
        value = entries[key]
        if isinstance(value, tuple):
            lines.append(f"  {q(key)}: {list_of_strings(value)}")
        else:
            lines.append(f"  {q(key)}: {q(value)}")
    lines.append("}")
    lines.append("")
    return "\n".join(lines)


def adv(name: str) -> Task:
    return Task("advancement", f"tenpack_create:create_progression/{name}")


def check() -> Task:
    return Task("checkmark")


def iter_declared_item_ids(chapters: tuple[Chapter, ...]) -> Iterable[str]:
    for chapter in chapters:
        yield chapter.icon
        for quest in chapter.quests:
            yield quest.icon
            for task in quest.tasks:
                if task.kind == "item" and task.item:
                    yield task.item


def collect_item_model_ids(mods_dir: Path) -> set[str]:
    ids: set[str] = set()

    def scan_zip(data: zipfile.ZipFile) -> None:
        for name in data.namelist():
            match = re.fullmatch(r"assets/([^/]+)/models/item/(.+)\.json", name)
            if match:
                namespace, path = match.groups()
                ids.add(f"{namespace}:{path}")
            elif name.startswith("META-INF/jarjar/") and name.endswith(".jar"):
                try:
                    with zipfile.ZipFile(io.BytesIO(data.read(name))) as nested:
                        scan_zip(nested)
                except zipfile.BadZipFile:
                    pass

    for jar in sorted(mods_dir.glob("*.jar")):
        try:
            with zipfile.ZipFile(jar) as data:
                scan_zip(data)
        except zipfile.BadZipFile:
            pass

    return ids


def validate_chapters(chapters: tuple[Chapter, ...]) -> None:
    chapter_slugs = [chapter.slug for chapter in chapters]
    if len(set(chapter_slugs)) != len(chapter_slugs):
        raise ValueError("duplicate chapter slug in FTB questbook")

    quest_slugs = [quest.slug for chapter in chapters for quest in chapter.quests]
    if len(set(quest_slugs)) != len(quest_slugs):
        raise ValueError("duplicate quest slug in FTB questbook")
    quest_slug_set = set(quest_slugs)

    for chapter in chapters:
        for quest in chapter.quests:
            for dep in quest.deps:
                if dep not in quest_slug_set:
                    raise ValueError(f"{quest.slug}: unknown dependency {dep!r}")
            if not quest.tasks:
                raise ValueError(f"{quest.slug}: quest has no tasks")
            for task in quest.tasks:
                if task.kind == "advancement" and not task.target:
                    raise ValueError(f"{quest.slug}: advancement task has no target")
                if task.kind == "item" and not task.item:
                    raise ValueError(f"{quest.slug}: item task has no item")
                if task.kind not in {"advancement", "checkmark", "item"}:
                    raise ValueError(f"{quest.slug}: unsupported task kind {task.kind!r}")

    bad_ids = sorted(item for item in set(iter_declared_item_ids(chapters)) if not ITEM_ID_RE.fullmatch(item))
    if bad_ids:
        raise ValueError(f"invalid item id(s) in FTB questbook: {', '.join(bad_ids)}")

    available_items = collect_item_model_ids(ROOT / "client" / "mods")
    missing_items = sorted(
        item
        for item in set(iter_declared_item_ids(chapters))
        if item not in VANILLA_ICON_IDS and item not in available_items
    )
    if missing_items:
        raise ValueError(
            "FTB questbook uses item/icon IDs without installed item models: "
            + ", ".join(missing_items)
        )


CHAPTERS: tuple[Chapter, ...] = (
    Chapter(
        slug="primer",
        filename="00_tenpack_primer",
        title="Tenpack Primer",
        subtitle=("A map for the pack, not a replacement for JEI, Create Ponder, or player-built projects.",),
        icon="ftbquests:book",
        quests=(
            Quest(
                slug="primer_welcome",
                title="Welcome to Tenpack",
                subtitle="Read me first",
                desc=(
                    "This questbook explains the shape of Tenpack: physical infrastructure, faction supply lines, and Create-era machines.",
                    "Most quests are signposts. JEI still gives exact recipes; Create Ponder still teaches how machines work.",
                ),
                x=0,
                y=0,
                icon="ftbquests:book",
                tasks=(check(),),
                disable_toast=True,
            ),
            Quest(
                slug="primer_jei_ponder",
                title="JEI Is the Recipe Source",
                desc=(
                    "Tenpack changes a lot of Create recipes. If an ingredient looks unfamiliar, trust JEI for the exact recipe.",
                    "Use Ponder for machine behavior; use this book for era order and design intent.",
                ),
                x=2,
                y=0,
                icon="create:goggles",
                tasks=(check(),),
                deps=("primer_welcome",),
                disable_toast=True,
            ),
            Quest(
                slug="primer_physical_infrastructure",
                title="Physical Infrastructure Wins",
                desc=(
                    "Tenpack intentionally avoids teleport/backpack/GPS shortcuts. Roads, rails, depots, baskets, carts, tanks, stations, and defended routes should matter.",
                    "If a quest asks for a route, supply line, or field kitchen, it is pointing at a build players can see and contest.",
                ),
                x=4,
                y=0,
                icon="minecraft:filled_map",
                tasks=(check(),),
                deps=("primer_jei_ponder",),
                disable_toast=True,
            ),
        ),
    ),
    Chapter(
        slug="create_eras",
        filename="01_create_eras",
        title="Create Eras",
        subtitle=("The CABIN-lite spine: simple kinetics stay approachable, precision and logistics become real milestones.",),
        icon="create:cogwheel",
        quests=(
            Quest("create_root", "Create Progression", ("Start here. The vanilla advancement tree mirrors this chapter, but FTB Quests lets the intent stay readable.",), 0, 0, "create:cogwheel", (adv("root"),), deps=("primer_physical_infrastructure",)),
            Quest("starter_logistics", "Starter Logistics", ("Moving items between machines should be early and readable. Learn belts, funnels, chutes, depots, and simple loading before smart networks.",), 2, 0, "create:belt_connector", (adv("starter_logistics"),), deps=("create_root",)),
            Quest("kinetic_workshop", "Kinetic Workshop", ("Your first real factory layer: press, mixer, saw, drill, fan, bearings, harvesters, and contraption loading.",), 4, 0, "create:andesite_casing", (adv("kinetic_workshop"),), deps=("starter_logistics",)),
            Quest("fluid_handling", "Fluid Handling", ("Copper plumbing is its own lesson. Spouts, item drains, and portable fluid interfaces mark the sealed/fluid era.",), 6, 0, "create:fluid_pipe", (adv("fluid_handling"),), deps=("kinetic_workshop",)),
            Quest("brass_electronics", "Brass & Electronics", ("Brass, electron tubes, and deployers are the programmable automation bridge before precision mechanisms.",), 8, 0, "create:brass_casing", (adv("brass_electronics"),), deps=("fluid_handling",)),
            Quest("precision_engineering", "Precision Engineering", ("Precision mechanisms are the main tier marker for compact power, mechanical crafting, trains, and advanced control.",), 10, 0, "create:precision_mechanism", (adv("precision_engineering"),), deps=("brass_electronics",)),
            Quest("smart_logistics", "Smart Logistics", ("Packagers, stock links, requesters, and factory gauges coordinate whole bases. They are powerful because they are late enough to need a real factory behind them.",), 12, -1, "create:stock_link", (adv("smart_logistics"),), deps=("precision_engineering",)),
            Quest("physics_age", "Physics Age", ("For now this is a generic vehicle-platform milestone, not an early-game detour. It says: you have enough precision control to start building with moving physics systems.",), 12, 1, "simulated:physics_assembler", (adv("physics_age"),), deps=("precision_engineering",)),
        ),
    ),
    Chapter(
        slug="food_supply",
        filename="02_food_supply",
        title="Farms, Kitchens, and Supply",
        subtitle=("Food should create useful infrastructure, not a Nutrition-style chore list or one official ration.",),
        icon="farmersdelight:cooking_pot",
        quests=(
            Quest("food_no_official_ration", "No Official Ration", ("Tenpack should not tell players there is one blessed food. Automate the foods your base likes: soups, stews, rice, pasta, sandwiches, tavern foods, or plated meals.", "Central Kitchen, Slice & Dice, Farmer's Delight, belts, deployers, basins, and stock logistics are the toolset. Tenpack may add missing recipes, but not a narrow approved ration list."), 0, 0, "farmersdelight:bamboo_basket", (check(),), disable_toast=True),
            Quest("farm_power", "Farm Power", ("Create harvesters turn fields into infrastructure. A farm is no longer only a starter chore; it becomes a visible supply district.",), 2, 0, "create:mechanical_harvester", (adv("farm_power"),), deps=("kinetic_workshop", "food_no_official_ration")),
            Quest("kitchen_line", "Kitchen Line", ("Farmer's Delight makes food meaningful; Create makes it scalable. Belts, basins, fans, deployers, mixers, and Cooking Pots can become a real kitchen district.",), 4, 0, "farmersdelight:cooking_pot", (adv("kitchen_line"),), deps=("farm_power",)),
            Quest("automate_any_food", "Automate Food You Actually Want", ("A good kitchen line is not defined by a single item. Build what your faction eats and ships.", "If JEI shows a Central Kitchen/Slice & Dice route, use it. If Tenpack adds a missing recipe, treat it as another option, not the canonical answer."), 6, 0, "create:mechanical_mixer", (check(),), deps=("kitchen_line",), disable_toast=True),
            Quest("food_logistics", "Food Logistics", ("Once smart logistics exists, meals stop being pocket clutter. Stock a depot, pack field rations, and keep substantial prepared food ready for mines, oilfields, forts, stations, and airfields.",), 8, -1, "supplementaries:lunch_basket", (adv("food_logistics"),), deps=("smart_logistics", "automate_any_food")),
            Quest("ration_routes", "Ration Routes", ("Rail and package logistics turn kitchens into route infrastructure. Send food to the places where work, combat, travel, and fuel extraction actually happen.",), 10, -1, "create:package_frogport", (adv("ration_routes"),), deps=("rail_logistics", "food_logistics")),
        ),
    ),
    Chapter(
        slug="rails_territory",
        filename="03_rails_territory",
        title="Rails and Territory",
        subtitle=("Trains are not just speed. They make territory, fuel, food, mines, and forts legible.",),
        icon="create:track_station",
        quests=(
            Quest("rail_logistics", "Rail Logistics", ("Stations, signals, observers, and route literacy turn precision engineering into actual territory control.",), 0, 0, "create:track_station", (adv("rail_logistics"),), deps=("precision_engineering",)),
            Quest("first_freight_route", "First Freight Route", ("A good first route solves a real problem: mine to foundry, oilfield to refinery, refinery to airfield, or food to a forward camp.",), 2, 0, "railways:conductor_whistle", (check(),), deps=("rail_logistics",), disable_toast=True),
            Quest("route_supply_identity", "Routes Have Identity", ("Use stations, signs, depots, baskets, tanks, and visible storage so other players can understand what a place does and why it matters.",), 4, 0, "supplementaries:way_sign_oak", (check(),), deps=("first_freight_route",), disable_toast=True),
        ),
    ),
    Chapter(
        slug="oil_vehicles_war",
        filename="04_oil_vehicles_war",
        title="Oil, Vehicles, Flight, and War",
        subtitle=("Heavy mobility and artillery belong to factions with fuel, routes, and industrial bases.",),
        icon="createdieselgenerators:diesel_engine",
        quests=(
            Quest("oil_prospecting", "Oil Prospecting", ("Oil starts as geography. Scout dry terrain before you can fully exploit it, so deserts, badlands, and savannas become strategic land.",), 0, 0, "createdieselgenerators:oil_scanner", (adv("oil_prospecting"),), deps=("precision_engineering",)),
            Quest("black_gold", "Black Gold", ("Pumpjacks turn discovered oil into a place worth claiming, defending, and connecting to routes.",), 2, 0, "createdieselgenerators:pumpjack_bearing", (adv("black_gold"),), deps=("oil_prospecting",)),
            Quest("oil_refining", "Oil Refining", ("Refineries and tank farms make fuel a logistics system instead of a one-bucket novelty.",), 4, 0, "createdieselgenerators:distillation_controller", (adv("oil_refining"),), deps=("black_gold",)),
            Quest("diesel_power", "Diesel Power", ("Diesel engines are strong because they ask for supply lines. If a faction relies on diesel, fuel movement should matter.",), 6, 0, "createdieselgenerators:diesel_engine", (adv("diesel_power"),), deps=("oil_refining",)),
            Quest("ground_vehicles", "Ground Vehicles", ("Ground vehicles sit behind the generic Physics Age for now: steering, tires, throttles, and engines should feel like a controlled platform, not day-zero magic.",), 2, 2, "simulated:steering_wheel", (adv("ground_vehicles"),), deps=("physics_age",)),
            Quest("controlled_flight", "Controlled Flight", ("Lift alone is not mastery. Throttle, sensors, gyro control, and vehicle literacy are what make aircraft faction-scale technology.",), 4, 2, "aeronautics:gyroscopic_propeller_bearing", (adv("controlled_flight"),), deps=("ground_vehicles",)),
            Quest("first_plane", "First Plane", ("The first plane is a server moment. Use it to connect places, scout routes, and make airfields worth building.",), 6, 2, "aeronautics:propeller_bearing", (adv("first_plane"),), deps=("controlled_flight",)),
            Quest("heavy_industry", "Heavy Industry", ("Cannon machinery now implies diesel-era industry. Artillery should have a foundry, ammo supply, fuel base, and logistics behind it.",), 8, 0, "createbigcannons:cannon_builder", (adv("heavy_industry"),), deps=("diesel_power",)),
            Quest("cannon_supply_line", "Cannon Supply Line", ("A cannon is power projection. Before using one casually, make the support visible: builder/drill, ammo flow, depot storage, and defended delivery routes.",), 10, 0, "createbigcannons:solid_shot", (check(),), deps=("heavy_industry",), disable_toast=True),
        ),
    ),
)


def generate_side(side_root: Path) -> None:
    out = side_root / QUEST_REL
    if out.exists():
        shutil.rmtree(out)
    (out / "chapters").mkdir(parents=True)
    (out / "reward_tables").mkdir()
    (out / "lang").mkdir()

    (out / "data.snbt").write_text(data_snbt(), encoding="utf-8")
    (out / "chapter_groups.snbt").write_text(chapter_groups_snbt(), encoding="utf-8")
    (out / "lang/en_us.snbt").write_text(lang_snbt(CHAPTERS), encoding="utf-8")
    for index, chapter in enumerate(CHAPTERS):
        (out / "chapters" / f"{chapter.filename}.snbt").write_text(chapter_snbt(chapter, index), encoding="utf-8")


def main() -> int:
    validate_chapters(CHAPTERS)
    for side in SIDES:
        generate_side(ROOT / side)
    print(f"generated FTB questbook in: {', '.join(str(ROOT / side / QUEST_REL) for side in SIDES)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
