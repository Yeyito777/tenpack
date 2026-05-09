# Create-era accessibility map

Purpose: player-facing/maintainer-facing map of how the installed mods fit around Tenpack's Create era ladder. This is the bridge between the recipe gates and actual play: what should players notice, what should JEI/Ponder/advancements teach, and what must not bypass the pressure points.

This is not a new progression system. It documents the current intent so future addon additions can be judged quickly.

For a direct recipe-level audit of the installed Create addon jars, see `notes/create-addon-recipe-era-audit.md`.

## Era summary

| Era | Player fantasy | Core unlocks | Social/faction pressure | Teaching surface |
| --- | --- | --- | --- | --- |
| 0 | Survival camp + starter motion | shafts, cogs, belts, water wheels, basic farms/routes | local roads, animal movement, early trade | JEI, Ponder, `Starter Logistics` advancement |
| 1 | Kinetic workshop | press, mixer, saw, drill, fan, bearings, pistons, contraption basics | first public workshops, shared mills, small farms | `Kinetic Workshop` advancement, Ponder scenes |
| 2 | Fluids and sealed machines | spout, item drain, portable fluid interfaces, tanks/pipes | fuel/scouting prep, fluid rooms, early logistics | `Fluid Handling` advancement |
| 3 | Precision/brass/logistics/rail | precision mechanisms, brass logistics, steam, mechanical crafters, trains, stock/package tools | rail hubs, markets, depots, shared factories, tolls | `Precision Engineering`, `Smart Logistics`, train-related recipes |
| 4 | Heavy industry and controlled mobility | oil extraction/refining, diesel, cannons, Aeronautics control, gyro parts | oilfields, refineries, airfields, cannon foundries, convoys | `Black Gold`, `Physics Age`, `Controlled Flight`, `First Plane Moment`, `Heavy Industry` |

## What each mod group is supposed to do

### Base Create + Create Encased

Era role: **Eras 0-3 backbone**.

Installed:

- Create
- Create Encased

Accessibility intent:

- Create's built-in Ponder teaches mechanics.
- JEI must show Tenpack recipes, not upstream cheap bypasses.
- Encased machine variants must be upgrades from the tuned base machines, not alternate cheap entries.

Guardrail:

- `tools/check-create-progression.py` enforces Encased upgrade recipes and major precision gates.

### Create Diesel Generators

Era role: **Era 4 oil/fuel pressure point**, with early prospecting.

Installed:

- Create Diesel Generators

Accessibility intent:

- The oil scanner is early and readable so players can scout before they can extract.
- Pumpjacks, distillation, and diesel engines stay precision-era or later.
- Dry biomes become visible faction pressure points: deserts, badlands, savannas.

Pressure intent:

- oilfields should create diplomacy, raids, convoys, rail lines, storage tanks, and refineries.
- canisters are scouting/emergency containers, not long-term logistics.
- barrels/tanks/trains should be better for bulk oil.

Guardrail:

- `tools/check-create-progression.py` checks oil biome tags, deny tags, scanner recipe, canister/barrel recipes, burn rates, and CDG configs.

### Create Aeronautics / Sable / Simulated / Offroad

Era role: **Era 4 controlled mobility**, with ground/offroad/physics experimentation before serious aircraft.

Installed:

- Create Aeronautics bundled
- Sable

Accessibility intent:

- The first aircraft should feel earned after Create precision/control infrastructure.
- Ground vehicles/test rigs should be easier to understand than fixed-wing plane projects.
- Flight should create airfields, hangars, fuel depots, repair bases, and patrol routes.

Pressure intent:

- aircraft are powerful because they are logistical projects, not because they erase geography.
- no teleportation, no personal jetpack shortcut, no recall systems.

Guardrail:

- `tools/check-create-progression.py` checks propeller/gyro/control recipes.
- `tools/check-mod-integrity.py` blocks Waystones/Sable teleportation and jetpack-style bypasses.

### Create Big Cannons

Era role: **Era 4 deterrence / faction military infrastructure**.

Installed:

- Create Big Cannons
- Ritchie's Projectile Library

Accessibility intent:

- cannons should read as heavy industry, not early grief toys.
- important cannon-production blocks are precision-gated.

Pressure intent:

- cannon foundries, ammo lines, fixed defenses, and transport are visible faction infrastructure.
- cannons should support deterrence/cold-war politics as much as raids.

Guardrail:

- `tools/check-create-progression.py` checks cannon builder/drill/mount/welder precision references.

### Rails and train identity

Era role: **Era 3+ public logistics**.

Installed:

- Steam 'n' Rails
- Create Railways Navigator
- DragonLib
- Architectury

Accessibility intent:

- rail hubs should be legible enough for new players.
- Navigator should help players understand shared train networks.
- rail routes should connect oilfields, farms, markets, refineries, cannon works, and airfields.

Pressure intent:

- trains create attackable/defensible supply lines without requiring direct base griefing.
- stations and routes can become tolls, embassies, borders, and trade agreements.

Risk:

- both rail additions are beta-ish in this Minecraft/Create version range; runtime launch and multiplayer train tests matter.

### Architecture and faction identity

Era role: **All eras, mostly cosmetic/identity**.

Installed:

- Copycats+
- Create Deco
- Bells & Whistles
- Rechiseled
- Rechiseled: Create
- Fusion client-side
- SuperMartijn642 libraries

Accessibility intent:

- factories, stations, bridges, refineries, and hangars should look like places.
- decoration should not hide the progression ladder.

Pressure intent:

- visible infrastructure makes power legible: smoke stacks, vault warehouses, rail depots, refinery tanks, cannon foundries.

Guardrail:

- future tiny addons should prefer this low-risk visual/infrastructure role; see `notes/create-small-addon-audit.md`.
- addon recipes are scanned by `tools/check-create-addon-recipes.py` so decorative compat outputs do not accidentally become progression bypasses.

### Food/farm/kitchen Create ecosystem

Era role: **Culture and trade goods; not a hard progression pillar**.

Installed:

- Farmer's Delight
- Create: Central Kitchen
- Slice & Dice
- Create Confectionery
- Create: Winery
- Create: Bitterballen
- Kotlin for Forge

Accessibility intent:

- farms/kitchens/taverns should be fun social spaces.
- do not turn food into an oppressive chore.

Pressure intent:

- food/drink can become trade goods and faction culture.
- farm districts and kitchens create reasons for non-industrial players to participate.

Risk:

- enough food addons are already installed; more food mods are likely bloat until playtested.

### JEI / Ponder / advancements

Era role: **Learning layer for all eras**.

Installed/available:

- JEI, with Tenpack metadata range fix
- Create's bundled Ponder
- Tenpack Create progression advancements

Accessibility intent:

- JEI answers "how do I craft this now?"
- Ponder answers "what does this do?"
- advancements answer "what broad era am I in?"

Pre-deploy test:

1. Open JEI.
2. Check mechanical press/mixer/saw/drill recipes.
3. Check precision mechanism recipe.
4. Check train station/signal recipes.
5. Check oil scanner vs pumpjack bearing recipes.
6. Check Aeronautics propeller/gyro recipes.
7. Trigger/open the Tenpack Create advancement tab.

## Non-Create mods around the eras

These are not Create progression mods, but they affect whether the Create ladder is playable and social.

### World/weather/terrain stack

Era role: **Makes geography and routes matter**.

Includes:

- Project Atmosphere, Simple Clouds, Serene Seasons, Serene Seasons Plus, Better Days
- Lithosphere, Terrain Slabs, YUNG's Cave Biomes
- TRMT, Burnt, Windy, Still Life, Lily Pads Expansion, Passable Foliage
- Alex's Mobs, Respawning Animals
- Voxy WorldGen V2

Pressure support:

- weather/terrain make travel and infrastructure feel physical.
- dry-biome oilfields now matter more because geography is meaningful.
- roads and repeated travel are supported by TRMT.

Risk:

- this is the most performance-sensitive part of the pack; use spark on real tests.

### Death/combat/ownership/social stack

Era role: **Makes pressure points matter**.

Includes:

- Lifesteal
- Corpse + Tenpack Death
- Locksmith + Curios
- YDM's Weapon Master
- Carry On
- Simple Voice Chat + Voice Chat Interaction + Voiceless Survival

Pressure support:

- death and loot risk make distant industry/scouting matter.
- locks and voice chat support diplomacy, theft, trade, negotiation, and faction identity.

Risk:

- Carry On should be watched for storage/container bypasses.
- Lifesteal/death/corpse rules need multiplayer feel testing.

### Client presentation stack

Era role: **Readability and vibes**.

Includes:

- Sodium, Iris, Voxy
- Camera Overhaul, Auto HUD
- LambDynamicLights, Falling Leaves, Particle Rain, Particle Interactions
- AmbientSounds, Sounds, Sound Physics, Presence Footsteps
- More Darkness, Legendary Tooltips, Prism

Accessibility support:

- makes the pack feel alive and modern.
- should never make factories/caves unreadable.

Risk:

- if launch/client stability is poor, remove visual polish before removing progression content.

## Guardrails now enforced

`tools/check-mod-integrity.py` now blocks silent additions of several known hard-no/deferred categories:

- Waystones / Sable teleportation
- backpack mods and backpack Create integrations
- AE2 / Create Applied Kinetics unless explicitly adopted later
- Create: Escalated
- Create Enchantment Industry / Enchantable Machinery until an XP pass
- Create Jetpack / Stuff & Additions personal mobility bypasses
- storage-network Create integrations until a storage pass
- remote/ender machine transfer
- casual resource generators like mechanical extruders/sifting/spawners

It also has an allowlist for current client-only jars so accidental client-only content additions are visible in CI/manual checks.

## Playability questions for the first server test

1. Can a first-timer build belts/water wheels without feeling punished?
2. Does the press/mixer/saw/drill tier feel like a workshop upgrade, not a wall?
3. Do players understand that oil can be scouted early but extracted later?
4. Are rail recipes visible at the moment precision/brass arrives?
5. Does the food stack create fun trade/culture, or JEI clutter?
6. Are aircraft obviously late infrastructure projects, not day-one mobility?
7. Do cannons feel like faction deterrence rather than casual grief tools?
8. Are client visuals helping immersion without hiding danger/factory state?
9. Do death/corpse/lock rules create player interaction without making exploration miserable?
10. Are oilfields far enough to create routes, but not so rare that players assume the pack is broken?
