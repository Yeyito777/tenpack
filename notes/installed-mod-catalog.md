# Installed mod catalog and Create-era categorization

Date: 2026-05-09

Purpose: every active mod jar is assigned a role in the Tenpack Create-era/factions design. This is the maintainer-facing inventory for “what does this mod do for the pack, and what pressure/readability risk does it carry?”

Guardrail: `./tools/check-mod-catalog.py` fails if a jar is added, removed, renamed, or made client-only without updating the catalog.

Current clean-pack counts:

- client jars: 90
- server jars: 71
- total active jar filenames: 90

## Category counts

- Create core progression: 3
- Create mobility / oil / heavy industry: 6
- Create rails and logistics: 3
- Create architecture and faction identity: 6
- Food, farm, and culture: 6
- World, weather, routes, ecology: 18
- Faction death, ownership, combat, social: 9
- Client readability and vibes: 16
- Learning and server tooling: 3
- Libraries / loader dependencies: 20

## Catalog

### Create core progression

| Jar | Side | Role |
| --- | --- | --- |
| `Create Encased-1.21.1-1.8-ht2.jar` | both | casing variants guarded as upgrades from tuned base machines |
| `CreateDragonsPlus-1.10.0b.jar` | both | Create utility/compat processing; hidden footprint audited separately |
| `create-1.21.1-6.0.10.jar` | both | base Create mechanics and Ponder learning layer |

### Create mobility / oil / heavy industry

| Jar | Side | Role |
| --- | --- | --- |
| `create-aeronautics-bundled-1.21.1-1.2.1.jar` | both | late controlled mobility, aircraft, vehicles, physics |
| `createbigcannons-5.11.3+mc.1.21.1.jar` | both | late heavy industry / faction deterrence |
| `createdieselgenerators-1.21.1-1.3.11.jar` | both | oilfields, fuel logistics, diesel pressure point |
| `do_a_barrel_roll-neoforge-3.7.3+1.21.jar` | both | flight control feel; thrust disabled by config policy |
| `ritchiesprojectilelib-2.1.2+mc.1.21.1-neoforge.jar` | both | required projectile library for Create Big Cannons |
| `sable-neoforge-1.21.1-1.2.2.jar` | both | Aeronautics sub-level/physics library |

### Create rails and logistics

| Jar | Side | Role |
| --- | --- | --- |
| `createrailwaysnavigator-neoforge-1.21.1-beta-0.9.0-C6.jar` | both | precision-era route navigation/readability for train networks |
| `dragonlib-neoforge-1.21.1-beta-3.0.26.jar` | both | library required by Railways Navigator |
| `railways-0.2.0-beta.2+neoforge-mc1.21.1.jar` | both | train identity, rail infrastructure, supply-line pressure |

### Create architecture and faction identity

| Jar | Side | Role |
| --- | --- | --- |
| `bellsandwhistles-0.4.7-1.21.1.jar` | both | train/station decorative blocks |
| `copycats-3.0.4+mc.1.21.1-neoforge.jar` | both | copycat architecture without bypassing base parts |
| `createdeco-2.1.3.jar` | both | industrial decoration, catwalks, factory/station identity |
| `fusion-1.2.12-neoforge-mc1.21.1.jar` | client-only | client connected-texture support for Rechiseled |
| `rechiseled-1.2.4-neoforge-mc1.21.jar` | both | decorative block variants for faction builds |
| `rechiseledcreate-1.1.0-neoforge-mc1.21.jar` | both | Create-themed Rechiseled variants |

### Food, farm, and culture

| Jar | Side | Role |
| --- | --- | --- |
| `FarmersDelight-1.21.1-1.3.1.jar` | both | farm/kitchen baseline and social food economy |
| `create-central-kitchen-2.4.0.jar` | both | Create/Farmer's Delight integration |
| `create-confectionery1.21.1_v1.1.2.jar` | both | confectionery/tavern/trade-goods flavor |
| `create_bic_bit-1.0.2C.jar` | both | Bitterballen novelty/culture food addon |
| `create_winery-2.0.2-neoforge-1.21.1.jar` | both | winery/tavern/faction culture flavor |
| `sliceanddice-forge-4.2.4.jar` | both | Create-powered farm/kitchen automation |

### World, weather, routes, ecology

| Jar | Side | Role |
| --- | --- | --- |
| `NeoForge-Version-Serene Seasons Plus-1.21.1-4.2.3.jar` | both | Serene Seasons integration required by Atmosphere stack |
| `NeoForge-projectatmosphere-0.8.1.0.jar` | both | dynamic climate/weather; supports physical geography |
| `PassableFoliage-1.21.1-NeoForge-9.1.3.jar` | both | physical foliage traversal |
| `RespawningAnimals-v21.1.2-1.21.1-NeoForge.jar` | both | passive animals replenish for farms/ecology |
| `SereneSeasons-neoforge-1.21.1-10.1.0.3.jar` | both | seasonal cycle for climate/temperature context |
| `YungsCaveBiomes-1.21.1-NeoForge-3.1.1.jar` | both | cave biome exploration/geography |
| `alexsmobs-1.22.17.jar` | both | ecology/animals/world life |
| `betterdays-1.21.1-3.3.6.3-NEOFORGE.jar` | both | seasonal day/night timing support |
| `burnt-fabric-0.1.3.jar` | both | fire/smoldering physical world pressure |
| `legendarysurvivaloverhaul-1.21.1-2.4.2.jar` | both | temperature survival pressure; thirst/health overhaul disabled by config |
| `lily_pads_expansion-1.0.0-neoforge-1.21-1.21.1.jar` | both | wetland/water traversal flavor |
| `lithosphere-1.7.jar` | both | world terrain/geography flavor |
| `simpleclouds-0.8.0f-b5-all.jar` | both | custom all-jar cloud simulation for Atmosphere |
| `still-life-0.1.1.jar` | both | biome/worldgen flavor patched for Tenpack |
| `terrain_slabs-neoforge-3.0.3.jar` | both | terrain slab generation and physical terrain nuance |
| `trmt-0.4-1.21+1.21.1.jar` | both | desire paths / visible route pressure |
| `voxy-worldgen-v2-1.21.1-2.2.1.jar` | both | server/world LoD generation support for large terrain visibility |
| `windy-1.1.1+1.21-neoforge.jar` | both | high-altitude wind ambience |

### Faction death, ownership, combat, social

| Jar | Side | Role |
| --- | --- | --- |
| `Voiceless Survival-1.21.1-neoforge-2.0.1.jar` | both | voice-reactive survival behavior |
| `carryon-neoforge-1.21.1-2.2.4.4.jar` | both | physical carrying; watch for container/lock abuse |
| `corpse-neoforge-1.21.1-1.1.13.jar` | both | physical corpse storage and death markers |
| `lifesteal-9.3.3+1.21.1.jar` | both | heart stakes for faction conflict |
| `locksmith-1.0.3.jar` | both | keys/locks for faction infrastructure |
| `tenpackdeath-0.1.0.jar` | both | Tenpack corpse protection/decay behavior |
| `vcinteraction-fabric-1.21.1-1.0.8.jar` | both | voice chat world interaction |
| `voicechat-neoforge-1.21.1-2.6.17.jar` | both | proximity voice for diplomacy and local conflict |
| `weaponmaster_ydm-1.21.1-neoforge-4.2.7.jar` | both | weapon display/readability |

### Client readability and vibes

| Jar | Side | Role |
| --- | --- | --- |
| `AmbientSounds_NEOFORGE_v6.3.8_mc1.21.1.jar` | client-only | ambient soundscape |
| `CameraOverhaul-v2.0.6-fabric+mc[1.21.0-1.21.2].jar` | client-only | camera motion; watch nausea/accessibility |
| `LegendaryTooltips-1.21.1-neoforge-1.5.5.jar` | client-only | item tooltip presentation |
| `PresenceFootsteps-1.21.1-1.12.0-beta.1-1.21NeoForge.jar` | client-only | footstep ambience / spatial feedback |
| `Prism-1.21.1-neoforge-1.0.11.jar` | client-only | tooltip/rendering library for Legendary Tooltips |
| `autohud-8.11+1.21.1-neoforge.jar` | client-only | HUD readability |
| `eg_particle_interactions-0.4.1-neoforge-mc1.21.1.jar` | client-only | particle interaction ambience |
| `fallingleaves-1.17.1+1.21.1.jar` | client-only | leaf particles / ambience |
| `iris-neoforge-1.8.12+mc1.21.1.jar` | client-only | shader support |
| `lambdynamiclights-3.1.4-neo-0+1.21.1.jar` | client-only | dynamic lights for darkness readability |
| `more_darkness-neoforge-1.21.1-1.0.0.jar` | client-only | darkness pressure/readability risk |
| `particlerain-4.0.0-beta.9+1.21.1-neoforge.jar` | client-only | weather particles / ambience |
| `sodium-neoforge-0.6.13+mc1.21.1.jar` | client-only | client renderer/performance |
| `sound-physics-remastered-neoforge-1.21.1-1.5.1.jar` | both | sound occlusion/reflection; ambience and spatial awareness |
| `sounds-2.4.22+lts+1.21.1-neoforge.jar` | client-only | soundscape additions |
| `voxy-0.2.14-alpha-mc_1211-f308c254.jar` | client-only | client distant terrain/LoD |

### Learning and server tooling

| Jar | Side | Role |
| --- | --- | --- |
| `Chunky-NeoForge-1.4.23.jar` | both | chunk pregeneration/tooling |
| `jei-1.21.1-neoforge-19.27.0.340-tenpack-mcrangefix.jar` | both | recipe/accessibility browser; Tenpack metadata patched |
| `spark-1.10.124-neoforge.jar` | both | server/client profiling for performance audits |

### Libraries / loader dependencies

| Jar | Side | Role |
| --- | --- | --- |
| `CreativeCore_NEOFORGE_v2.13.38_mc1.21.1.jar` | client-only | library required by AmbientSounds |
| `GlitchCore-neoforge-1.21.1-2.1.0.0.jar` | both | library required by Serene Seasons |
| `Iceberg-1.21.1-neoforge-1.3.2.jar` | client-only | library required by Legendary Tooltips |
| `Kiwi-1.21.1-NeoForge-15.8.3.jar` | both | library dependency |
| `PuzzlesLib-v21.1.39-1.21.1-NeoForge.jar` | both | library dependency |
| `TerraBlender-neoforge-1.21.1-4.1.0.8.jar` | both | biome/worldgen library dependency |
| `YungsApi-1.21.1-NeoForge-5.1.6.jar` | both | library required by YUNG's Cave Biomes |
| `architectury-13.0.8-neoforge.jar` | both | cross-platform library dependency |
| `citadel-1.21.1-2.7.6.jar` | both | library for Alex's Mobs |
| `cloth-config-15.0.140-fabric.jar` | both | Fabric config library used through Connector |
| `connector-2.0.0-beta.14+1.21.1-full.jar` | both | Sinytra Connector for Fabric mods on NeoForge |
| `curios-neoforge-9.5.1+1.21.1.jar` | both | required by Locksmith and accessory integrations |
| `forgified-fabric-api-0.116.7+2.2.4+1.21.1.jar` | both | Fabric API compatibility for Connector |
| `gaboulibs-neoforge-1.4.jar` | both | library required by Serene Seasons Plus |
| `geckolib-neoforge-1.21.1-4.8.4.jar` | both | animation/library dependency |
| `kotlinforforge-5.11.0-all.jar` | both | library required by Slice & Dice |
| `mru-1.0.19+LTS+1.21.1+neoforge.jar` | client-only | client library dependency |
| `supermartijn642configlib-1.1.8-neoforge-mc1.21.jar` | both | config library for Rechiseled stack |
| `supermartijn642corelib-1.1.21-neoforge-mc1.21.jar` | both | library for Rechiseled stack |
| `yet_another_config_lib_v3-3.8.2+1.21.1-neoforge.jar` | both | config UI library used by Windy/Sounds |

## Audit interpretation

The active stack is not a random kitchen sink: the biggest content mass is deliberately split between Create progression, visible rail/architecture infrastructure, physical world pressure, faction/death/social systems, and client readability. Libraries are numerous because the pack mixes NeoForge, Sinytra Connector, Create addons, worldgen, and client presentation mods; they should not be interpreted as gameplay bloat by themselves.

Most important accessibility risks to test in-game:

1. Create era readability: JEI/Ponder/advancements must make the starter → workshop → fluid → precision/rail → oil/flight/cannon ladder obvious.
2. JEI clutter: Create Deco/Railways/food addons add many harmless recipes; players need guide text so the important era anchors are not buried.
3. World/performance: Atmosphere/Simple Clouds/Voxy/worldgen/client visuals need spark profiling under real server load.
4. Darkness/readability: More Darkness + shaders must not make factories/caves unreadable.
5. Faction harshness: Lifesteal + Corpse + Tenpack Death must create stakes without discouraging scouting and oilfield expeditions.
6. Carry On abuse: watch containers, locks, corpses, and Create contraptions.
7. Dragons Plus: keep an eye on its bulk ending/freezing/coloring utility because it is more than a simple library.

## Explicitly not present

The catalog confirms the current active pack still avoids the major hard-no/deferred classes:

- no backpacks or backpack Create integrations
- no Waystones/Sable teleportation
- no AE2 / Create Applied Kinetics
- no Create: Escalated
- no Create Enchantment Industry / Enchantable Machinery
- no Create Jetpack / Stuff & Additions personal flight/tool bypass
- no casual infinite-resource Create addons like sifting/extruders/spawners
- no storage-network Create integrations
