#!/usr/bin/env python3
"""Ensure every installed Tenpack mod jar has an explicit audit category.

The catalog is intentionally filename-based because Tenpack deploys exact jars.
When a jar is added/renamed, this check forces a maintainer to decide what role
it plays in the Create-era/factions design before publishing.
"""

from __future__ import annotations

from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

ALLOWED_CATEGORIES = {
    "create_core_progression",
    "create_mobility_heavy_industry",
    "create_rails_logistics",
    "create_architecture_identity",
    "create_food_farm_culture",
    "world_weather_routes",
    "faction_death_social",
    "client_readability_vibes",
    "learning_tooling",
    "library_loader_dependency",
    "optional_uncommitted_travel",
}

# side: "both" means server jar must be mirrored to client; "client" means
# client-only visual/tooling/library; "optional-both" is for known local WIP that
# may be present in the working tree but is not part of the committed pack yet.
CATALOG: dict[str, dict[str, str]] = {
    # Create backbone / progression.
    "create-1.21.1-6.0.10.jar": {"category": "create_core_progression", "side": "both", "role": "base Create mechanics and Ponder learning layer"},
    "Create Encased-1.21.1-1.8-ht2.jar": {"category": "create_core_progression", "side": "both", "role": "casing variants guarded as upgrades from tuned base machines"},
    "CreateDragonsPlus-1.10.0b.jar": {"category": "create_core_progression", "side": "both", "role": "Create utility/compat processing; hidden footprint audited separately"},

    # Create mobility / oil / cannons.
    "create-aeronautics-bundled-1.21.1-1.2.1.jar": {"category": "create_mobility_heavy_industry", "side": "both", "role": "late controlled mobility, aircraft, vehicles, physics"},
    "sable-neoforge-1.21.1-1.2.2.jar": {"category": "create_mobility_heavy_industry", "side": "both", "role": "Aeronautics sub-level/physics library"},
    "createdieselgenerators-1.21.1-1.3.11.jar": {"category": "create_mobility_heavy_industry", "side": "both", "role": "oilfields, fuel logistics, diesel pressure point"},
    "createbigcannons-5.11.3+mc.1.21.1.jar": {"category": "create_mobility_heavy_industry", "side": "both", "role": "late heavy industry / faction deterrence"},
    "ritchiesprojectilelib-2.1.2+mc.1.21.1-neoforge.jar": {"category": "create_mobility_heavy_industry", "side": "both", "role": "required projectile library for Create Big Cannons"},
    "do_a_barrel_roll-neoforge-3.7.3+1.21.jar": {"category": "create_mobility_heavy_industry", "side": "both", "role": "flight control feel; thrust disabled by config policy"},

    # Rails/logistics.
    "railways-0.2.0-beta.2+neoforge-mc1.21.1.jar": {"category": "create_rails_logistics", "side": "both", "role": "train identity, rail infrastructure, supply-line pressure"},
    "createrailwaysnavigator-neoforge-1.21.1-beta-0.9.0-C6.jar": {"category": "create_rails_logistics", "side": "both", "role": "precision-era route navigation/readability for train networks"},
    "dragonlib-neoforge-1.21.1-beta-3.0.26.jar": {"category": "create_rails_logistics", "side": "both", "role": "library required by Railways Navigator"},

    # Architecture/faction identity.
    "copycats-3.0.4+mc.1.21.1-neoforge.jar": {"category": "create_architecture_identity", "side": "both", "role": "copycat architecture without bypassing base parts"},
    "createdeco-2.1.3.jar": {"category": "create_architecture_identity", "side": "both", "role": "industrial decoration, catwalks, factory/station identity"},
    "bellsandwhistles-0.4.7-1.21.1.jar": {"category": "create_architecture_identity", "side": "both", "role": "train/station decorative blocks"},
    "rechiseled-1.2.4-neoforge-mc1.21.jar": {"category": "create_architecture_identity", "side": "both", "role": "decorative block variants for faction builds"},
    "rechiseledcreate-1.1.0-neoforge-mc1.21.jar": {"category": "create_architecture_identity", "side": "both", "role": "Create-themed Rechiseled variants"},
    "fusion-1.2.12-neoforge-mc1.21.1.jar": {"category": "create_architecture_identity", "side": "client", "role": "client connected-texture support for Rechiseled"},
    "supermartijn642corelib-1.1.21-neoforge-mc1.21.jar": {"category": "library_loader_dependency", "side": "both", "role": "library for Rechiseled stack"},
    "supermartijn642configlib-1.1.8-neoforge-mc1.21.jar": {"category": "library_loader_dependency", "side": "both", "role": "config library for Rechiseled stack"},

    # Food/farm/culture.
    "FarmersDelight-1.21.1-1.3.1.jar": {"category": "create_food_farm_culture", "side": "both", "role": "farm/kitchen baseline and social food economy"},
    "create-central-kitchen-2.4.0.jar": {"category": "create_food_farm_culture", "side": "both", "role": "Create/Farmer's Delight integration"},
    "sliceanddice-forge-4.2.4.jar": {"category": "create_food_farm_culture", "side": "both", "role": "Create-powered farm/kitchen automation"},
    "create-confectionery1.21.1_v1.1.2.jar": {"category": "create_food_farm_culture", "side": "both", "role": "confectionery/tavern/trade-goods flavor"},
    "create_winery-2.0.2-neoforge-1.21.1.jar": {"category": "create_food_farm_culture", "side": "both", "role": "winery/tavern/faction culture flavor"},
    "create_bic_bit-1.0.2C.jar": {"category": "create_food_farm_culture", "side": "both", "role": "Bitterballen novelty/culture food addon"},
    "supplementaries-neoforge-1.21.1-3.6.4.jar": {"category": "create_food_farm_culture", "side": "both", "role": "portable lunch basket for field rations plus physical food/logistics props; basket capped at 6 slots by config"},
    "CraftTweaker-neoforge-1.21.1-21.0.38.jar": {"category": "create_food_farm_culture", "side": "both", "role": "server-authoritative food value tuning: weak staples, stronger prepared meals, no custom mod"},
    "kotlinforforge-5.11.0-all.jar": {"category": "library_loader_dependency", "side": "both", "role": "library required by Slice & Dice"},

    # World/weather/routes/ecology.
    "NeoForge-projectatmosphere-0.8.1.0.jar": {"category": "world_weather_routes", "side": "both", "role": "dynamic climate/weather; supports physical geography"},
    "simpleclouds-0.8.0f-b5-all.jar": {"category": "world_weather_routes", "side": "both", "role": "custom all-jar cloud simulation for Atmosphere"},
    "SereneSeasons-neoforge-1.21.1-10.1.0.3.jar": {"category": "world_weather_routes", "side": "both", "role": "seasonal cycle for climate/temperature context"},
    "NeoForge-Version-Serene Seasons Plus-1.21.1-4.2.3.jar": {"category": "world_weather_routes", "side": "both", "role": "Serene Seasons integration required by Atmosphere stack"},
    "betterdays-1.21.1-3.3.6.3-NEOFORGE.jar": {"category": "world_weather_routes", "side": "both", "role": "seasonal day/night timing support"},
    "lithosphere-1.7.jar": {"category": "world_weather_routes", "side": "both", "role": "world terrain/geography flavor"},
    "terrain_slabs-neoforge-3.0.3.jar": {"category": "world_weather_routes", "side": "both", "role": "terrain slab generation and physical terrain nuance"},
    "YungsCaveBiomes-1.21.1-NeoForge-3.1.1.jar": {"category": "world_weather_routes", "side": "both", "role": "cave biome exploration/geography"},
    "alexsmobs-1.22.17.jar": {"category": "world_weather_routes", "side": "both", "role": "ecology/animals/world life"},
    "RespawningAnimals-v21.1.2-1.21.1-NeoForge.jar": {"category": "world_weather_routes", "side": "both", "role": "passive animals replenish for farms/ecology"},
    "PassableFoliage-1.21.1-NeoForge-9.1.3.jar": {"category": "world_weather_routes", "side": "both", "role": "physical foliage traversal"},
    "lily_pads_expansion-1.0.0-neoforge-1.21-1.21.1.jar": {"category": "world_weather_routes", "side": "both", "role": "wetland/water traversal flavor"},
    "legendarysurvivaloverhaul-1.21.1-2.4.2.jar": {"category": "world_weather_routes", "side": "both", "role": "temperature survival pressure; thirst/health overhaul disabled by config"},
    "windy-1.1.1+1.21-neoforge.jar": {"category": "world_weather_routes", "side": "both", "role": "high-altitude wind ambience"},
    "trmt-0.4-1.21+1.21.1.jar": {"category": "world_weather_routes", "side": "both", "role": "desire paths / visible route pressure"},
    "burnt-fabric-0.1.3.jar": {"category": "world_weather_routes", "side": "both", "role": "fire/smoldering physical world pressure"},
    "danger_close-neoforge-1.21.1-3.1.3.jar": {"category": "world_weather_routes", "side": "both", "role": "ambient danger/proximity pressure for caves and wilderness readability"},
    "nyfsspiders-neoforge-1.21.1-3.0.1.jar": {"category": "world_weather_routes", "side": "both", "role": "spider movement/world danger behavior for harsher wilderness traversal"},
    "simpleblockphysics-1.21.1-neoforge-1.2.0.jar": {"category": "world_weather_routes", "side": "both", "role": "physical block-collapse pressure for terrain, mines, and construction"},
    "still-life-0.1.1.jar": {"category": "world_weather_routes", "side": "both", "role": "biome/worldgen flavor patched for Tenpack"},
    "voxy-worldgen-v2-1.21.1-2.2.1.jar": {"category": "world_weather_routes", "side": "both", "role": "server/world LoD generation support for large terrain visibility"},

    # Faction/death/social/combat.
    "lifesteal-9.3.3+1.21.1.jar": {"category": "faction_death_social", "side": "both", "role": "heart stakes for faction conflict"},
    "corpse-neoforge-1.21.1-1.1.13.jar": {"category": "faction_death_social", "side": "both", "role": "physical corpse storage and death markers"},
    "tenpackdeath-0.1.0.jar": {"category": "faction_death_social", "side": "both", "role": "Tenpack corpse protection/decay behavior"},
    "locksmith-1.0.3.jar": {"category": "faction_death_social", "side": "both", "role": "keys/locks for faction infrastructure"},
    "carryon-neoforge-1.21.1-2.2.4.4.jar": {"category": "faction_death_social", "side": "both", "role": "physical carrying; watch for container/lock abuse"},
    "weaponmaster_ydm-1.21.1-neoforge-4.2.7.jar": {"category": "faction_death_social", "side": "both", "role": "weapon display/readability"},
    "voicechat-neoforge-1.21.1-2.6.17.jar": {"category": "faction_death_social", "side": "both", "role": "proximity voice for diplomacy and local conflict"},
    "vcinteraction-fabric-1.21.1-1.0.8.jar": {"category": "faction_death_social", "side": "both", "role": "voice chat world interaction"},
    "Voiceless Survival-1.21.1-neoforge-2.0.1.jar": {"category": "faction_death_social", "side": "both", "role": "voice-reactive survival behavior"},
    "curios-neoforge-9.5.1+1.21.1.jar": {"category": "library_loader_dependency", "side": "both", "role": "required by Locksmith and accessory integrations"},

    # Client readability/vibes.
    "sodium-neoforge-0.6.13+mc1.21.1.jar": {"category": "client_readability_vibes", "side": "client", "role": "client renderer/performance"},
    "iris-neoforge-1.8.12+mc1.21.1.jar": {"category": "client_readability_vibes", "side": "client", "role": "shader support"},
    "voxy-0.2.14-alpha-d85ce91c2f24603e75c9da4d15babaf3844922d0.jar": {"category": "client_readability_vibes", "side": "both", "role": "distant terrain/LoD client plus server-side benchmark/runtime support"},
    "autohud-8.11+1.21.1-neoforge.jar": {"category": "client_readability_vibes", "side": "client", "role": "HUD readability"},
    "CameraOverhaul-v2.0.6-fabric+mc[1.21.0-1.21.2].jar": {"category": "client_readability_vibes", "side": "client", "role": "camera motion; watch nausea/accessibility"},
    "continuity-3.0.0+1.21.neoforge.jar": {"category": "client_readability_vibes", "side": "client", "role": "connected texture support for roads, buildings, and resource packs"},
    "dense-flowers-0.2.2+mc1.21.0.jar": {"category": "client_readability_vibes", "side": "client", "role": "client-side floral density/visual atmosphere"},
    "Drip Sounds-0.5.2+1.21.8-NeoForge.jar": {"category": "client_readability_vibes", "side": "client", "role": "water drip ambience and cave/weather sound feedback"},
    "entity_model_features-3.2.4-1.21-neoforge.jar": {"category": "client_readability_vibes", "side": "client", "role": "Fresh Animations entity model support"},
    "entity_texture_features_1.21-neoforge-7.1.jar": {"category": "client_readability_vibes", "side": "client", "role": "Fresh Animations/entity texture feature support"},
    "keybindsearch-1.0.0.jar": {"category": "client_readability_vibes", "side": "client", "role": "keybind search/readability for a large modpack"},
    "lambdynamiclights-3.1.4-neo-0+1.21.1.jar": {"category": "client_readability_vibes", "side": "client", "role": "dynamic lights for darkness readability"},
    "fallingleaves-1.17.1+1.21.1.jar": {"category": "client_readability_vibes", "side": "client", "role": "leaf particles / ambience"},
    "make_bubbles_pop-0.3.0-fabric-mc1.19.4-1.21.jar": {"category": "client_readability_vibes", "side": "client", "role": "water bubble particle ambience through Connector"},
    "particlerain-4.0.0-beta.9+1.21.1-neoforge.jar": {"category": "client_readability_vibes", "side": "client", "role": "weather particles / ambience"},
    "eg_particle_interactions-0.4.1-neoforge-mc1.21.1.jar": {"category": "client_readability_vibes", "side": "client", "role": "particle interaction ambience"},
    "AmbientSounds_NEOFORGE_v6.3.8_mc1.21.1.jar": {"category": "client_readability_vibes", "side": "client", "role": "ambient soundscape"},
    "sounds-2.4.22+lts+1.21.1-neoforge.jar": {"category": "client_readability_vibes", "side": "client", "role": "soundscape additions"},
    "sound-physics-remastered-neoforge-1.21.1-1.5.1.jar": {"category": "client_readability_vibes", "side": "both", "role": "sound occlusion/reflection; ambience and spatial awareness"},
    "PresenceFootsteps-1.21.1-1.12.0-beta.1-1.21NeoForge.jar": {"category": "client_readability_vibes", "side": "client", "role": "footstep ambience / spatial feedback"},
    "more_darkness-neoforge-1.21.1-1.0.0.jar": {"category": "client_readability_vibes", "side": "client", "role": "darkness pressure/readability risk"},
    "notenoughanimations-neoforge-1.12.3-mc1.21.1.jar": {"category": "client_readability_vibes", "side": "client", "role": "third-person animation readability and vibe"},
    "PickUpNotifier-v21.1.1-1.21.1-NeoForge.jar": {"category": "client_readability_vibes", "side": "client", "role": "pickup feedback/readability for loot and field work"},
    "MouseTweaks-neoforge-mc1.21-2.26.1.jar": {"category": "client_readability_vibes", "side": "client", "role": "inventory interaction quality-of-life without portable storage bypass"},
    "skinlayers3d-neoforge-1.11.1-mc1.21.1.jar": {"category": "client_readability_vibes", "side": "client", "role": "3D skin layer player readability/vibe"},
    "tia-neoforge-1.21-1.2.1.jar": {"category": "client_readability_vibes", "side": "client", "role": "client visual/interface atmosphere addon"},
    "tightfire-1.21.1-1.0-SNAPSHOT.jar": {"category": "client_readability_vibes", "side": "client", "role": "compact fire rendering for visibility/readability"},
    "wakes-0.4.1+1.21.1.jar": {"category": "client_readability_vibes", "side": "client", "role": "water wake visual feedback for boats and movement"},
    "appleskin-neoforge-mc1.21-3.0.9.jar": {"category": "client_readability_vibes", "side": "client", "role": "hunger/saturation/exhaustion HUD and food tooltip visibility"},
    "LegendaryTooltips-1.21.1-neoforge-1.5.5.jar": {"category": "client_readability_vibes", "side": "client", "role": "item tooltip presentation"},
    "Prism-1.21.1-neoforge-1.0.11.jar": {"category": "client_readability_vibes", "side": "client", "role": "tooltip/rendering library for Legendary Tooltips"},
    "CreativeCore_NEOFORGE_v2.13.38_mc1.21.1.jar": {"category": "library_loader_dependency", "side": "client", "role": "client library required by AmbientSounds"},
    "Iceberg-1.21.1-neoforge-1.3.2.jar": {"category": "library_loader_dependency", "side": "client", "role": "library required by Legendary Tooltips"},
    "mru-1.0.19+LTS+1.21.1+neoforge.jar": {"category": "library_loader_dependency", "side": "client", "role": "client library dependency"},

    # Learning/tooling.
    "jei-1.21.1-neoforge-19.27.0.340-tenpack-mcrangefix.jar": {"category": "learning_tooling", "side": "client", "role": "client recipe/accessibility browser; Tenpack metadata patched; server-side JEI trips Create/Aeronautics JEI registration"},
    "ftb-quests-neoforge-2101.1.24.jar": {"category": "learning_tooling", "side": "both", "role": "FTB questbook for Tenpack Create-era guidance; mirrors advancements but keeps design intent visible"},
    "spark-1.10.124-neoforge.jar": {"category": "learning_tooling", "side": "both", "role": "server/client profiling for performance audits"},
    "Chunky-NeoForge-1.4.23.jar": {"category": "learning_tooling", "side": "both", "role": "chunk pregeneration/tooling"},

    # Loader/general libraries.
    "connector-2.0.0-beta.14+1.21.1-full.jar": {"category": "library_loader_dependency", "side": "both", "role": "Sinytra Connector for Fabric mods on NeoForge"},
    "forgified-fabric-api-0.116.7+2.2.4+1.21.1.jar": {"category": "library_loader_dependency", "side": "both", "role": "Fabric API compatibility for Connector"},
    "cloth-config-15.0.140-fabric.jar": {"category": "library_loader_dependency", "side": "both", "role": "Fabric config library used through Connector"},
    "architectury-13.0.8-neoforge.jar": {"category": "library_loader_dependency", "side": "both", "role": "cross-platform library dependency"},
    "c2me-neoforge-mc1.21.1-0.3.0+alpha.0.91.jar": {"category": "library_loader_dependency", "side": "both", "role": "server/client chunk generation performance library; monitor experimental status"},
    "ftb-library-neoforge-2101.1.31.jar": {"category": "library_loader_dependency", "side": "both", "role": "required library for FTB Quests and FTB Teams"},
    "ftb-teams-neoforge-2101.1.10.jar": {"category": "library_loader_dependency", "side": "both", "role": "required team/progress data layer for FTB Quests"},
    "lithium-neoforge-0.15.3+mc1.21.1.jar": {"category": "library_loader_dependency", "side": "both", "role": "general game-logic performance optimization"},
    "monolib-neoforge-1.21.1-4.0.2.jar": {"category": "library_loader_dependency", "side": "both", "role": "library required by baseline atmosphere/client stack"},
    "moonlight-neoforge-1.21.1-3.0.7.jar": {"category": "library_loader_dependency", "side": "both", "role": "library required by Supplementaries lunch basket"},
    "noisium-neoforge-2.7.0+mc1.21-1.21.1.jar": {"category": "library_loader_dependency", "side": "both", "role": "worldgen/performance optimization for terrain generation"},
    "PuzzlesLib-v21.1.39-1.21.1-NeoForge.jar": {"category": "library_loader_dependency", "side": "both", "role": "library dependency"},
    "citadel-1.21.1-2.7.6.jar": {"category": "library_loader_dependency", "side": "both", "role": "library for Alex's Mobs"},
    "geckolib-neoforge-1.21.1-4.8.4.jar": {"category": "library_loader_dependency", "side": "both", "role": "animation/library dependency"},
    "Kiwi-1.21.1-NeoForge-15.8.3.jar": {"category": "library_loader_dependency", "side": "both", "role": "library dependency"},
    "TerraBlender-neoforge-1.21.1-4.1.0.8.jar": {"category": "library_loader_dependency", "side": "both", "role": "biome/worldgen library dependency"},
    "GlitchCore-neoforge-1.21.1-2.1.0.0.jar": {"category": "library_loader_dependency", "side": "both", "role": "library required by Serene Seasons"},
    "gaboulibs-neoforge-1.4.jar": {"category": "library_loader_dependency", "side": "both", "role": "library required by Serene Seasons Plus"},
    "YungsApi-1.21.1-NeoForge-5.1.6.jar": {"category": "library_loader_dependency", "side": "both", "role": "library required by YUNG's Cave Biomes"},
    "yet_another_config_lib_v3-3.8.2+1.21.1-neoforge.jar": {"category": "library_loader_dependency", "side": "both", "role": "config UI library used by Windy/Sounds"},

    # Known local WIP restored in the working tree sometimes; not part of pushed pack until committed.
    "astikorcartsredux-1.2.2.jar": {"category": "optional_uncommitted_travel", "side": "optional-both", "role": "AstikorCarts Redux land caravan layer; physical carts/wagons/draft travel candidate for Tenpack Travel, no teleport or progression gate"},
    "leashall-neoforge-1.21.1-1.3.1-1.21.1.jar": {"category": "optional_uncommitted_travel", "side": "optional-both", "role": "local Tenpack Travel WIP dependency; group animal leading for caravan handling"},
    "tenpack_travel-0.1.0.jar": {"category": "optional_uncommitted_travel", "side": "optional-both", "role": "local Tenpack Travel WIP; physical animal-travel tooling, not deployed yet"},
}


def main() -> int:
    errors: list[str] = []
    client = {path.name for path in (ROOT / "client" / "mods").glob("*.jar")}
    server = {path.name for path in (ROOT / "server" / "mods").glob("*.jar")}
    present = client | server

    for filename in sorted(present - set(CATALOG)):
        errors.append(f"uncategorized mod jar: {filename}")

    for filename in sorted(present & set(CATALOG)):
        entry = CATALOG[filename]
        category = entry.get("category")
        expected_side = entry.get("side")
        if category not in ALLOWED_CATEGORIES:
            errors.append(f"{filename}: invalid category {category!r}")
        if not entry.get("role"):
            errors.append(f"{filename}: missing role text")
        in_client = filename in client
        in_server = filename in server
        if expected_side == "both" and not (in_client and in_server):
            errors.append(f"{filename}: expected both client and server, got client={in_client} server={in_server}")
        elif expected_side == "client" and not (in_client and not in_server):
            errors.append(f"{filename}: expected client-only, got client={in_client} server={in_server}")
        elif expected_side == "optional-both" and present and (in_client != in_server):
            errors.append(f"{filename}: optional WIP jar must be mirrored if present, got client={in_client} server={in_server}")
        elif expected_side not in {"both", "client", "optional-both"}:
            errors.append(f"{filename}: invalid side policy {expected_side!r}")

    # If a cataloged required jar disappears, make that intentional too.
    for filename, entry in sorted(CATALOG.items()):
        if entry.get("side") in {"both", "client"} and filename not in present:
            errors.append(f"cataloged required jar missing from active pack: {filename}")

    counts = Counter(CATALOG[name]["category"] for name in present if name in CATALOG)
    print(f"cataloged active jars: {sum(counts.values())}")
    for category, count in sorted(counts.items()):
        print(f"  {category}: {count}")

    if errors:
        print("\nMod catalog check failed:")
        for error in errors:
            print(f"- {error}")
        return 1

    print("Mod catalog checks passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
