# Tenpack mod-stack accessibility/playability audit

Date: 2026-05-09

Purpose: audit the whole active mod stack after the Create progression rewrite. The goal is not just "does the jar list exist?" but whether the additions are categorized, understandable, mirrored correctly between client/server, and still support Tenpack's faction pressure-point philosophy.

This pass intentionally does **not** approve unrelated uncommitted Tenpack Travel work; the audit was run from a clean tree with that work stashed aside.

For a player/progression-facing map of how these categories sit around the Create eras, see `notes/create-era-accessibility-map.md`.
For the direct recipe scan of installed Create addon jars, see `notes/create-addon-recipe-era-audit.md`.
For the non-Create world/survival/death/client stack audit, see `notes/non-create-stack-era-audit.md`.
For the complete per-jar mod catalog, see `notes/installed-mod-catalog.md`.

## Static integrity result

Command:

```bash
./tools/check-mod-integrity.py
```

Result after this pass:

- client jars: 90
- server jars: 71
- client-only jars: 19
- all server jars are mirrored to the client by filename
- required metadata dependencies are satisfied after accounting for JarJar/nested jars and Connector/Forgified Fabric API
- current client-only jars are allowlisted so accidental client-only content additions get noticed
- hard-no/deferred addon categories are blocked from silently entering the mod list
- installed Create addon recipe outputs/era anchors are checked separately by `./tools/check-create-addon-recipes.py`
- non-recipe pack config policy is checked separately by `./tools/check-pack-configs.py`
- every active mod jar must be categorized by `./tools/check-mod-catalog.py`

Create-specific command:

```bash
./tools/check-create-progression.py
```

Still required before deployment because it enforces the Create era/progression invariants.

## Immediate fixes from this audit

### Camera Overhaul

Changed client jar:

- removed `CameraOverhaul-v2.0.6-neoforge+mc[1.21.0-1.21.1].jar`
- added `CameraOverhaul-v2.0.6-fabric+mc[1.21.0-1.21.2].jar`

Reason: the NeoForge jar requires NeoForge mod id `cloth_config`. The pack already carries Fabric Cloth Config through Sinytra Connector for Fabric mods (`cloth-config` / `cloth-config2`). Adding a second NeoForge Cloth Config jar would duplicate the same library family and is riskier than using Camera Overhaul's Fabric build through the existing Connector stack.

Expected player effect: same client-side camera tilt/motion feature, less dependency ambiguity.

### LambDynamicLights

Changed client jar:

- removed `lambdynamiclights-4.8.8+1.21.1.jar`
- added `lambdynamiclights-3.1.4-neo-0+1.21.1.jar`

Reason: the 4.8.8 multiloader wrapper has nested runtime/library metadata that is easy to misread and can look like missing client dependencies in static checks. The unofficial NeoForge 3.1.4 build is simpler for this pack's current goal: client-side dynamic lights without extra storage/progression impact.

Expected player effect: same broad "held torches glow" polish, less launcher/dependency ambiguity.

## Top-level categories

### 1. Create progression backbone

Purpose: machinery ladder, faction industry, physical infrastructure, oil/fuel pressure, trains, aircraft, and cannons.

Mods:

- Create `create-1.21.1-6.0.10.jar`
- Create Encased `Create Encased-1.21.1-1.8-ht2.jar`
- Create Aeronautics bundled `create-aeronautics-bundled-1.21.1-1.2.1.jar`
- Sable `sable-neoforge-1.21.1-1.2.2.jar`
- Create Diesel Generators `createdieselgenerators-1.21.1-1.3.11.jar`
- Create Big Cannons `createbigcannons-5.11.3+mc.1.21.1.jar`
- Ritchie's Projectile Library `ritchiesprojectilelib-2.1.2+mc.1.21.1-neoforge.jar`

Era fit:

- starter kinetics remain early and readable;
- kinetic workshop machines are mildly more expensive but not expert-pack hostile;
- fluid machines introduce copper/plumbing;
- precision/brass unlocks crafters, package logistics, trains, steam, and advanced controls;
- Aeronautics control, oil/diesel, and cannons stay late enough to become faction pressure points.

Accessibility:

- JEI is installed.
- Create Ponder is JarJar-bundled inside Create.
- Tenpack adds advancement milestones under the Create progression datapack.
- The custom checker protects major bypasses, especially Create Encased variants.

Risk/watch:

- needs actual launch/playtest;
- Aeronautics/Sable vehicle play must be checked for grief, performance, and noob readability;
- trains and aircraft should be taught by guide/quest text, not just recipes.

### 2. Create rail/logistics/architecture batch

Purpose: visible faction infrastructure, social train networks, refineries, bridges, depots, markets, and faction identity.

Mods:

- Steam 'n' Rails `railways-0.2.0-beta.2+neoforge-mc1.21.1.jar`
- Create Railways Navigator `createrailwaysnavigator-neoforge-1.21.1-beta-0.9.0-C6.jar`
- DragonLib `dragonlib-neoforge-1.21.1-beta-3.0.26.jar`
- Architectury `architectury-13.0.8-neoforge.jar`
- Copycats+ `copycats-3.0.4+mc.1.21.1-neoforge.jar`
- Create Deco `createdeco-2.1.3.jar`
- Bells & Whistles `bellsandwhistles-0.4.7-1.21.1.jar`
- Rechiseled `rechiseled-1.2.4-neoforge-mc1.21.jar`
- Rechiseled: Create `rechiseledcreate-1.1.0-neoforge-mc1.21.jar`
- Fusion `fusion-1.2.12-neoforge-mc1.21.1.jar` client-only connected-texture support
- SuperMartijn642 Core/Config libs

Era fit:

- mostly decorative/rail readability;
- should not bypass Create progression directly;
- rail networks become valuable after precision/train gates.

Accessibility:

- good for readable physical infrastructure;
- Navigator can help players understand rail networks if it launches cleanly.

Risk/watch:

- Steam 'n' Rails and Railways Navigator are beta-versioned for 1.21.1; launch test is mandatory before push/deploy.

### 3. Create food/farm/kitchen flavor

Purpose: make farms, kitchens, taverns, markets, and trade goods feel social without turning hunger into a punishment tax.

Mods:

- Farmer's Delight `FarmersDelight-1.21.1-1.3.1.jar`
- Create: Central Kitchen `create-central-kitchen-2.4.0.jar`
- Create Slice & Dice `sliceanddice-forge-4.2.4.jar`
- Kotlin for Forge `kotlinforforge-5.11.0-all.jar`
- Create Confectionery `create-confectionery1.21.1_v1.1.2.jar`
- Create: Winery `create_winery-2.0.2-neoforge-1.21.1.jar`
- Create: Bitterballen `create_bic_bit-1.0.2C.jar`

Era fit:

- acceptable as culture/infrastructure content;
- not part of the hard Create progression ladder;
- should create trade/social goods rather than mandatory chores.

Risk/watch:

- do not keep adding food addons casually; the current food stack is enough for first playtest.

### 4. Factions, death, combat, ownership, and interaction

Purpose: make multiplayer stakes and player interaction matter.

Mods:

- Lifesteal `lifesteal-9.3.3+1.21.1.jar`
- Corpse `corpse-neoforge-1.21.1-1.1.13.jar`
- Tenpack Death `tenpackdeath-0.1.0.jar`
- Lucky's Locksmith `locksmith-1.0.3.jar`
- Curios API `curios-neoforge-9.5.1+1.21.1.jar`
- YDM's Weapon Master `weaponmaster_ydm-1.21.1-neoforge-4.2.7.jar`
- Carry On `carryon-neoforge-1.21.1-2.2.4.4.jar`
- Simple Voice Chat `voicechat-neoforge-1.21.1-2.6.17.jar`
- Voice Chat Interaction `vcinteraction-fabric-1.21.1-1.0.8.jar`
- Voiceless Survival `Voiceless Survival-1.21.1-neoforge-2.0.1.jar`

Create-era interaction:

- corpse/death pressure makes distant oilfields, train routes, and raids meaningful;
- locks support faction bases and industrial access control;
- voice chat supports negotiation, intimidation, trade, and diplomacy.

Risk/watch:

- Carry On can be useful but should be watched for container/contraption abuse;
- Lifesteal + corpse rules must be checked against the intended faction death loop;
- Tenpack Death has separate uncommitted work history; do not mix with Create commits without reviewing.

### 5. World, weather, terrain, animals, and routes

Purpose: make the world feel physical and make roads/routes/geography matter.

Mods:

- Project Atmosphere `NeoForge-projectatmosphere-0.8.1.0.jar`
- Simple Clouds `simpleclouds-0.8.0f-b5-all.jar`
- Serene Seasons `SereneSeasons-neoforge-1.21.1-10.1.0.3.jar`
- Serene Seasons Plus `NeoForge-Version-Serene Seasons Plus-1.21.1-4.2.3.jar`
- Better Days `betterdays-1.21.1-3.3.6.3-NEOFORGE.jar`
- GlitchCore `GlitchCore-neoforge-1.21.1-2.1.0.0.jar`
- Gabou's Libs `gaboulibs-neoforge-1.4.jar`
- Lithosphere `lithosphere-1.7.jar`
- Terrain Slabs `terrain_slabs-neoforge-3.0.3.jar`
- YUNG's API `YungsApi-1.21.1-NeoForge-5.1.6.jar`
- YUNG's Cave Biomes `YungsCaveBiomes-1.21.1-NeoForge-3.1.1.jar`
- Alex's Mobs `alexsmobs-1.22.17.jar`
- Respawning Animals `RespawningAnimals-v21.1.2-1.21.1-NeoForge.jar`
- Passable Foliage `PassableFoliage-1.21.1-NeoForge-9.1.3.jar`
- Lily Pads Expansion `lily_pads_expansion-1.0.0-neoforge-1.21-1.21.1.jar`
- Windy `windy-1.1.1+1.21-neoforge.jar`
- TRMT `trmt-0.4-1.21+1.21.1.jar`
- Burnt `burnt-fabric-0.1.3.jar`
- Still Life `still-life-0.1.1.jar`
- Voxy WorldGen V2 `voxy-worldgen-v2-1.21.1-2.2.1.jar`

Create-era interaction:

- oilfields are now dry-biome pressure points;
- terrain and roads make rail/truck/animal infrastructure matter;
- weather and wind reinforce the physical-world fantasy around Aeronautics.

Risk/watch:

- world/weather stack is ambitious and performance-sensitive;
- Project Atmosphere/Simple Clouds/Voxy already have historical patches, so launch profiling matters;
- structure/worldgen additions should be tested separately, not mixed into the Create addon batch.

### 6. Client presentation, readability, and accessibility

Purpose: make the pack feel alive without hiding mechanics or hurting performance.

Client-only mods:

- Sodium `sodium-neoforge-0.6.13+mc1.21.1.jar`
- Iris `iris-neoforge-1.8.12+mc1.21.1.jar`
- Voxy `voxy-0.2.14-alpha-mc_1211-f308c254.jar`
- Auto HUD `autohud-8.11+1.21.1-neoforge.jar`
- Camera Overhaul `CameraOverhaul-v2.0.6-fabric+mc[1.21.0-1.21.2].jar`
- LambDynamicLights `lambdynamiclights-3.1.4-neo-0+1.21.1.jar`
- Falling Leaves `fallingleaves-1.17.1+1.21.1.jar`
- Particle Rain `particlerain-4.0.0-beta.9+1.21.1-neoforge.jar`
- Particle Interactions `eg_particle_interactions-0.4.1-neoforge-mc1.21.1.jar`
- AmbientSounds `AmbientSounds_NEOFORGE_v6.3.8_mc1.21.1.jar`
- Sounds `sounds-2.4.22+lts+1.21.1-neoforge.jar`
- Sound Physics Remastered `sound-physics-remastered-neoforge-1.21.1-1.5.1.jar`
- Presence Footsteps `PresenceFootsteps-1.21.1-1.12.0-beta.1-1.21NeoForge.jar`
- More Darkness `more_darkness-neoforge-1.21.1-1.0.0.jar`
- Legendary Tooltips `LegendaryTooltips-1.21.1-neoforge-1.5.5.jar`
- Prism `Prism-1.21.1-neoforge-1.0.11.jar`
- CreativeCore / MRU / Iceberg support libraries

Gameplay accessibility mods:

- JEI `jei-1.21.1-neoforge-19.27.0.340-tenpack-mcrangefix.jar`
- Cloth Config `cloth-config-15.0.140-fabric.jar`
- YetAnotherConfigLib `yet_another_config_lib_v3-3.8.2+1.21.1-neoforge.jar`

Create-era interaction:

- JEI + Ponder + advancements are the main noob-friendly layer;
- visuals should help immersion, not obscure factory readability.

Risk/watch:

- client-only visual stack is large; if Prism launch is unstable, remove visual polish before removing Create content;
- More Darkness plus shaders should be checked for readability in factories/caves.

### 7. Libraries / compatibility foundation

Purpose: make the mixed NeoForge + Connector stack work.

Mods/libraries:

- Sinytra Connector `connector-2.0.0-beta.14+1.21.1-full.jar`
- Forgified Fabric API `forgified-fabric-api-0.116.7+2.2.4+1.21.1.jar`
- Architectury, Puzzles Lib, GeckoLib, Citadel, Kiwi, TerraBlender, Curios, SuperMartijn libs, DragonLib, GlitchCore, Gabou's Libs, Kotlin for Forge, YetAnotherConfigLib

Risk/watch:

- Connector is powerful but adds compatibility risk;
- prefer native NeoForge jars when they are clean, but Fabric-through-Connector is acceptable when it avoids duplicate libraries or when no native jar exists.

### 8. Server tooling

Purpose: support deploy/test/profiling.

Mods:

- spark `spark-1.10.124-neoforge.jar`
- Chunky `Chunky-NeoForge-1.4.23.jar`

Risk/watch:

- use `/spark profiler` after deployment to observe Create/Aeronautics/worldgen load;
- Chunky is tooling, not progression.

## Explicit non-goals kept out

The current installed stack still avoids the major bypass classes discussed in addon triage:

- no backpack mod / backpack Create integration;
- no waystones or Sable teleportation compatibility;
- no AE2 / Applied Kinetics;
- no Create: Escalated;
- no Create Enchantment Industry / Enchantable Machinery;
- no Create Ore Excavation, Molten Vents, Mechanical Extruder, Sifting, or other casual infinite-resource systems;
- no Create Jetpack / Stuff 'N Additions personal-flight bypass.

## Accessibility/playability checklist before push/deploy

Static:

- `./tools/check-mod-integrity.py`
- `./tools/check-create-progression.py`
- `./tools/tenpack-build-public.py --out public`

Runtime:

1. Launch client once from Prism.
2. Create a throwaway world or join a local test server.
3. Confirm JEI opens and shows Tenpack Create recipes, not upstream bypass recipes.
4. Confirm Ponder opens for starter Create machines.
5. Confirm Tenpack Create advancement tab appears.
6. Confirm oil scanner is visible/understandable before pumpjack extraction.
7. Confirm train station/signal recipes are precision-era and visible in JEI.
8. Confirm Aeronautics/offroad/Simulated items appear and the early/late split is readable.
9. Confirm the visual client stack is not making caves/factories unreadable.
10. Profile first real server test with spark if Aeronautics/trains/worldgen feel heavy.

## Current conclusion

The installed stack is coherent for a factions Create pack: the Create era ladder has support tools, JEI/Ponder/advancement readability, faction architecture, rail logistics, food culture, oil pressure, and late Aeronautics/cannon goals. The main remaining risk is not static dependency shape; it is runtime stability/playfeel from the ambitious world/weather/visual stack plus beta rail/Aeronautics components.
