# Non-Create stack era/pressure audit

Date: 2026-05-09

Purpose: audit the installed non-Create mods around the new Create era ladder. These mods do not directly define the Create recipes, but they determine whether the era ladder feels physical, social, readable, and fair in a factions environment.

Added guardrail:

```bash
./tools/check-pack-configs.py
```

Current result:

- active config allowlist passes
- no stale `improvedmobs` config ships after this pass
- travel/world/death/survival config policies pass

## Cleanup from this pass

Removed stale Improved Mobs config files:

- `client/config/improvedmobs/common.toml`
- `client/config/improvedmobs/equipment.json`
- `server/config/improvedmobs/common.toml`
- `server/config/improvedmobs/equipment.json`
- `tenpack-specs/overrides/config/improvedmobs/common.toml`
- `tenpack-specs/overrides/config/improvedmobs/equipment.json`

Reason: Improved Mobs is not installed in the current active mod stack. Shipping its configs to clients/server makes the pack look harder/more complex than it actually is and creates maintenance noise. If Improved Mobs is reconsidered later, it should be a deliberate combat/difficulty pass, not a leftover config artifact.

## Current validation command set

For normal pre-push/deploy validation, run:

```bash
./tools/check-mod-integrity.py
./tools/check-mod-catalog.py
./tools/check-pack-configs.py
./tools/check-create-progression.py
./tools/check-create-addon-recipes.py
./tools/tenpack-build-public.py --out public
```

## Category audit

### 1. Route/world physicality

Mods/configs:

- TRMT `trmt-0.4-1.21+1.21.1.jar`
- Windy `windy-1.1.1+1.21-neoforge.jar`
- Project Atmosphere `NeoForge-projectatmosphere-0.8.1.0.jar`
- Simple Clouds `simpleclouds-0.8.0f-b5-all.jar`
- Serene Seasons / Serene Seasons Plus / Better Days
- Voxy WorldGen V2 `voxy-worldgen-v2-1.21.1-2.2.1.jar`

Important config anchors now checked:

- TRMT player erosion multiplier = `0.25`
- TRMT mounted erosion multiplier = `0.75`
- Voxy generation radius = `128`
- Voxy max active tasks = `20`
- Windy minimum wind height = `130`

Era/pressure role:

- TRMT makes repeated foot/mount traffic visibly form paths, supporting faction roads, oilfield approaches, farm lanes, rail yards, and airfield service roads.
- Mounted traffic is intentionally more visible than foot traffic, reinforcing animal/vehicle infrastructure instead of teleportation.
- Weather/season/atmosphere stack makes geography feel like a place, so dry oilfield biomes and long travel routes have identity.
- Voxy supports large-world visibility, useful for aircraft, scouting, rail routes, and settlement planning.

Playability/accessibility:

- Players can understand paths visually without reading a quest book.
- Weather and clouds are vibe/physicality, not a hard progression gate.
- The main accessibility risk is performance, not recipe confusion.

Watch list:

- Voxy + Project Atmosphere + Simple Clouds has history of performance/compat tuning. Use `spark` on real server tests.
- If roads erode too fast, players may feel terrain griefed; if too slow, the pressure-point storytelling is lost.

### 2. Survival pressure

Mods/configs:

- Legendary Survival Overhaul `legendarysurvivaloverhaul-1.21.1-2.4.2.jar`
- More Darkness `more_darkness-neoforge-1.21.1-1.0.0.jar`
- Burnt `burnt-fabric-0.1.3.jar`
- Windy / Project Atmosphere / Seasons as ambience and temperature context

Important config anchors now checked:

- LSO thirst = disabled
- LSO health overhaul = disabled
- LSO temperature = enabled
- More Darkness caveDarkness = `0.0`
- More Darkness disableWithShaders = `false`

Era/pressure role:

- Survival pressure should come from temperature, darkness, weather, travel, death risk, and geography, not a chore stack.
- Thirst is disabled because it would add constant busywork and compete with the more interesting faction/Create pressure points.
- Temperature pairs well with oilfields/deserts, cold biomes, seasonal travel, and infrastructure planning.
- Darkness makes roads, lanterns, rail stations, base lighting, and cave prep matter.

Playability/accessibility:

- Good: clear physical pressures: hot/cold/dark/fire.
- Good: no hidden thirst/health overhaul complexity.
- Risk: More Darkness + shaders could make factories/caves unreadable for new players. Test with the selected shaderpack.

### 3. Death, corpses, and faction stakes

Mods/configs:

- Lifesteal `lifesteal-9.3.3+1.21.1.jar`
- Corpse `corpse-neoforge-1.21.1-1.1.13.jar`
- Tenpack Death `tenpackdeath-0.1.0.jar`

Important config anchors now checked:

- Lifesteal transfers one heart per death/kill (`2` hitpoints).
- Mob kills do not grant hearts.
- Corpse skeleton marker time = `1200` ticks / 1 minute.
- Corpse native owner protection is off because Tenpack Death handles it silently.
- Tenpack Death owner protection is on.
- Public loot requires skeleton stage.
- Ops do not bypass protection by default.
- Decay and post-decay corpse breaking are enabled.

Era/pressure role:

- Distant oilfield scouting, rail logistics, aircraft expeditions, and cannon conflicts matter more when death has cost.
- Corpse staging creates a recovery window, then turns abandoned bodies into faction interaction points.
- Lifesteal keeps player conflict meaningful without making mobs an easy heart farm.

Playability/accessibility:

- The skeleton-stage visual marker is good because players can understand when a corpse becomes public.
- The decay timer prevents permanent safe storage in corpses.
- Risk: this needs actual multiplayer feel testing; too harsh will discourage exploration/oilfield scouting, too soft will make pressure points toothless.

### 4. Ownership, negotiation, and local social interaction

Mods/configs:

- Lucky's Locksmith `locksmith-1.0.3.jar`
- Curios API `curios-neoforge-9.5.1+1.21.1.jar`
- Simple Voice Chat `voicechat-neoforge-1.21.1-2.6.17.jar`
- Voice Chat Interaction `vcinteraction-fabric-1.21.1-1.0.8.jar`
- Voiceless Survival `Voiceless Survival-1.21.1-neoforge-2.0.1.jar`

Era/pressure role:

- Locks let factions create controlled storage/doors around refineries, rail depots, vaults, and workshops without immediately needing admin claims.
- Voice chat supports diplomacy at pressure points: borders, bridges, markets, rail stops, oilfields, airfields, and siege lines.
- Voice Chat Interaction/Voiceless Survival add world reactivity to voice without replacing physical infrastructure.

Playability/accessibility:

- Great for faction interaction because it creates local, spatial social pressure.
- Needs a clear server note that UDP voice port forwarding must be working.

Watch list:

- If locks become too strong, they can reduce raiding/story too much.
- If locks are too weak/confusing, players will not trust infrastructure.

### 5. Combat / equipment / carrying

Mods/configs:

- YDM's Weapon Master `weaponmaster_ydm-1.21.1-neoforge-4.2.7.jar`
- Carry On `carryon-neoforge-1.21.1-2.2.4.4.jar`
- Lifesteal / Corpse / Tenpack Death

Era/pressure role:

- Combat mods sit outside Create eras but shape the risk of oilfields, convoys, rail routes, and cannon bases.
- Carry On can make physical logistics feel more grounded, but it is also a possible storage/container abuse vector.

Playability/accessibility:

- Weapon Master is mostly presentation/readability for gear on players.
- Carry On should be explicitly watched in multiplayer tests around claims/locks, Create contraptions, corpses, and the container blacklist. Tenpack's `tenpack-carryon-policy` datapack now blocks the core vanilla storage/container blocks plus installed modded storage surfaces from Farmer's Delight, Supplementaries, and Create item vaults so the mod stays physical carrying instead of backpack cargo.

Watch list:

- Do not add backpacks. Carry On is not a backpack substitute and should not become one.
- XP/enchantment automation is still deferred; do not add Enchantment Industry/Enchantable Machinery until combat/equipment economy is discussed.

### 6. Animals and ecology

Mods/configs:

- Alex's Mobs `alexsmobs-1.22.17.jar`
- Respawning Animals `RespawningAnimals-v21.1.2-1.21.1-NeoForge.jar`
- Passable Foliage `PassableFoliage-1.21.1-NeoForge-9.1.3.jar`
- Lily Pads Expansion `lily_pads_expansion-1.0.0-neoforge-1.21-1.21.1.jar`

Era/pressure role:

- Animals and ecology make the world worth travelling through and occupying.
- Respawning animals keeps passive animals from being one-time worldgen depletion.
- This supports faction farms, food culture, mounts/travel, and local biome identity.

Playability/accessibility:

- Adds life and non-industrial reasons to explore.
- Risk is spawn clutter/noise. Alex's Mobs should be watched around base performance and hostile surprise density.

### 7. Worldgen / terrain / caves

Mods/configs:

- Lithosphere `lithosphere-1.7.jar`
- Terrain Slabs `terrain_slabs-neoforge-3.0.3.jar`
- YUNG's Cave Biomes `YungsCaveBiomes-1.21.1-NeoForge-3.1.1.jar`
- TerraBlender, YUNG's API, GeckoLib dependencies
- Still Life `still-life-0.1.1.jar`

Era/pressure role:

- Worldgen makes oilfields, rail corridors, roads, and air routes more interesting.
- Terrain Slabs and cave biomes add physical nuance without giving direct Create progression.
- Still Life/Atmosphere biome temp data exists so weather/temperature systems understand modded biomes.

Playability/accessibility:

- Good for immersion and faction geography.
- Risk: worldgen mods are hard to change after world start. Avoid adding structure/worldgen mods casually.

Structure addon reminder:

- Create: Easy Structures and similar are still in a separate test bucket. They need throwaway-world inspection for frequency, loot, and visual bloat before adding.

### 8. Client presentation and readability

Mods/configs:

- Sodium / Iris / Voxy
- Camera Overhaul
- LambDynamicLights
- Falling Leaves, Particle Rain, Particle Interactions
- AmbientSounds, Sounds, Sound Physics Remastered, Presence Footsteps
- More Darkness, Auto HUD, Legendary Tooltips, Prism

Era/pressure role:

- Client presentation should make physical infrastructure legible: dark caves, lit factories, noisy rail yards, weather, wind, footsteps, distant terrain.
- It should not hide Create mechanics or make factory debugging harder.

Playability/accessibility:

- JEI/Ponder/advancements teach mechanics; visual mods should support them, not compete.
- If launch stability or FPS is bad, remove visual polish before touching the Create progression content.

Watch list:

- More Darkness + shader readability.
- Camera Overhaul motion sensitivity for players prone to nausea.
- Sound stack volume/noise in busy factories and rail hubs.

### 9. Mixed loader / libraries

Mods/configs:

- Sinytra Connector
- Forgified Fabric API
- Cloth Config Fabric
- Architectury, Puzzles Lib, GeckoLib, Citadel, Kiwi, TerraBlender, Curios, SuperMartijn libs, DragonLib, GlitchCore, Gabou's Libs, Kotlin for Forge, YACL, etc.

Policy:

- Native NeoForge is preferred when clean.
- Fabric-through-Connector is acceptable when a native jar is absent or worse for dependency clarity.
- Connector mixin safeguard stays enabled.

This is why Camera Overhaul was moved to its Fabric jar earlier: it fits the existing Fabric Cloth Config/Connector stack better than adding duplicate NeoForge `cloth_config` alongside Fabric `cloth-config`.

## Final conclusion for this pass

The non-Create stack mostly supports the Create-era design:

- travel remains physical;
- no teleportation/backpack/storage-network bypass was introduced;
- survival pressure is environment/darkness/temperature rather than thirst chores;
- death/corpse/lifesteal rules create stakes around faction pressure points;
- voice/locks support local social interaction;
- visual/world mods reinforce geography and infrastructure.

Main risks for playtest:

1. performance from world/weather/visual stack;
2. darkness/shaders readability;
3. corpse/lifesteal harshness;
4. Carry On abuse around containers/locks/corpses;
5. Alex's Mobs spawn/noise density;
6. structure/worldgen additions should remain deferred until tested separately.
