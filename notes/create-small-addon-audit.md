# Create small/specialized addon audit

Purpose: quick triage for the tiny/specialized Create addons that are easy to accidentally add as bloat. Nothing in this note means "installed"; these are disposition notes before any future add pass.

Tenpack filters:

- does it create visible faction infrastructure or identity?
- does it preserve roads, rails, animals, Aeronautics, fuel, and logistics pressure?
- does it avoid early resource loops, remote transfer, personal flight, teleportation, backpack bypasses, and cheap storage superpowers?
- is it small enough to test safely?

## Clean yes / likely yes, after launch testing

These look small and aligned, but still need a NeoForge launch test before inclusion.

### Create: Framed

Disposition: **likely yes**.

Reason: adds more framed glass variants. This is visual/factory architecture support, not a progression bypass. It helps stations, refineries, greenhouses, hangars, and faction builds look intentional.

Risk/check: recipe IDs and block count only.

### Create: Oxidized

Disposition: **likely yes**.

Reason: adds oxidizing recipes for copper blocks. This is tasteful Create QoL and fits the copper/brass aesthetic.

Risk/check: confirm it does not add cheap copper duplication; recipe-only QoL should be fine.

### Create: Vibrant Vaults

Disposition: **likely yes**.

Reason: colored/decorated item vaults help faction logistics read visually. This is good for train yards, warehouses, refineries, and market districts.

Risk/check: confirm recipes upgrade normal vaults instead of creating cheaper vault equivalents.

### Create: Blocks & Bogies

Disposition: **likely yes**.

Reason: rail visual identity matters in factions. More wheelsets/bogies can help factions make distinct trains without changing the rail economy much.

Risk/check: launch test with Steam 'n' Rails and Railways Navigator; remove if it causes train rendering/network instability.

### Create Train Utilities

Disposition: **likely yes / rail architecture pass**.

Reason: sliding doors, platform blocks, and station/train building blocks support public rail hubs, border stations, and faction terminals.

Risk/check: launch test with Steam 'n' Rails and make sure it is not adding hidden automation/teleport behavior.

### Create More: Parallel Pipes

Disposition: **likely yes, but recipe-check first**.

Reason: non-auto-connecting pipe variants are a real factory readability tool. They can make compact refineries and chemical rooms less awful without invalidating progression.

Risk/check: ensure recipes require normal pipes/casings and do not bypass fluid handling gates.

### Create: Extended Wrenches

Disposition: **likely yes / cosmetic QoL**.

Reason: aesthetic wrench variants are harmless faction identity flavor if recipes are normal.

Risk/check: confirm no overpowered reach/mode changes beyond visuals/QoL.

## Performance / technical candidates

These are not gameplay adds. Treat them as server/client stability experiments, not content.

### CreateBetterFps

Disposition: **test separately**.

Reason: client-side shader/Create FPS improvement could help, especially with factories and Aeronautics builds.

Risk/check: client-only behavior, shader compatibility, and no rendering bugs around contraptions.

### Create: LazyTick

Disposition: **test separately**.

Reason: performance optimization for large Create setups may matter on a factions server.

Risk/check: must be tested under real contraptions. Optimization mods can create subtle desync bugs.

### Create: Threaded Trains

Disposition: **test separately**.

Reason: if Tenpack leans into faction rail networks, train simulation performance could matter.

Risk/check: server-side train desync/crashes. Only consider after actual train load becomes an issue.

### Create: Big Contraptions

Disposition: **test separately if contraption rendering becomes painful**.

Reason: render/carry support for large contraptions may help aircraft/vehicles visually.

Risk/check: client-only, Aeronautics/Sable interactions, and whether it masks builds that are too large for server health.

### Create: Dynamic Lights / Create Sable Dynamic Lights

Disposition: **optional client polish, not priority**.

Reason: visual polish for moving contraptions can be nice.

Risk/check: client performance. Do not let it become another required unstable client mod unless it is clearly worth it.

## Soft maybe / defer for a themed pass

These might fit later, but not as casual tiny-addon additions.

### Create Goggles

Disposition: **soft maybe**.

Reason: goggle helmets and armored backtanks are convenient and thematic.

Risk/check: armor/backtank convenience can affect combat and gear progression. Review with the equipment/XP pass.

### Create: Liquid Fuel

Disposition: **defer to fuel/oil pass**.

Reason: liquid blaze burner fuel could connect nicely to oil logistics.

Risk/check: may make blaze burner operation too easy or undermine existing burner/fuel pacing. If added, gate it around diesel/oil infrastructure.

### Create: Aquatic Ambitions

Disposition: **defer to ocean/resource pass**.

Reason: prismarine/coral/copper processing could make ocean infrastructure more meaningful.

Risk/check: renewable coral/prismarine/copper loops might affect build economy and ocean pressure. Needs recipe/output audit.

### Create: Radars

Disposition: **defer to faction-defense/Aeronautics pass**.

Reason: radar could be very cool for aircraft/vehicle warfare, borders, and scouting.

Risk/check: privacy, griefing, balance, and whether it exposes too much information. Needs multiplayer testing.

### Create: Tweaked Controllers

Disposition: **defer to vehicle/control testing**.

Reason: better contraption control may be useful for vehicles.

Risk/check: could simplify Aeronautics/offroad handling too much or add control bugs.

### Create: Mobile Packages / Create Factory Logistics

Disposition: **defer until base Create package logistics are playtested**.

Reason: mobile packages and logistics jars may be genuinely useful in a rail/faction economy.

Risk/check: package logistics are already part of the precision/smart-logistics gate. Extra package tools could bypass the intended ladder or make stock logistics too strong too early.

### Create Horse Power

Disposition: **defer to animal/travel pass**.

Reason: animal-powered machinery is charming and fits early/medieval faction identity.

Risk/check: Tenpack already has active animal/travel design work. Decide there, not in a Create addon batch.

### Create: Sound of Steam

Disposition: **soft maybe / culture add only**.

Reason: pipe organs are funny and can support faction culture/church/town-hall builds.

Risk/check: pure bloat unless players actually want it.

### Create: Cafe / Create: Food / Create: Garnished / Create Factory / Create Ratatouille / Some Assembly Required / Create: Integrated Farming

Disposition: **defer; food stack is already large enough for now**.

Reason: Slice & Dice, Confectionery, Winery, Bitterballen, Farmer's Delight, and Central Kitchen already provide enough farm/kitchen identity for a first pass.

Risk/check: these could be fun later, but adding all of them now would turn the pack into food-addon soup and increase JEI noise.

## Structure/worldgen candidates: separate check only

These may be good for exploration, but they need careful worldgen/loot/playtest review. Do not add in the normal addon batch.

### Create: Easy Structures

Disposition: **separate check / maybe**.

Reason: not a horrendous idea; small Create structures could teach machinery and make the world feel more industrial.

Risk/check: could be bloat, too extra, ugly, too common, or loot/progression-breaking. Needs a temporary test world and structure frequency/loot inspection.

### Create: Let The Adventure Begin

Disposition: **separate check / maybe**.

Reason: Create-themed structures can provide exploration hooks.

Risk/check: same as above, but larger surface area. Must inspect generated structures and loot before adding.

### Create: Structures Arise

Disposition: **separate check / cautious maybe**.

Reason: adds many Create structures, which could be great or overwhelming.

Risk/check: 28 structures means high bloat/progression risk. Requires the most testing of the structure candidates.

### Create: Rustic Structures

Disposition: **separate check / maybe**.

Reason: functional/decorative Create structures could fit abandoned infrastructure themes.

Risk/check: inspect spawn rates, loot, and visual style.

## Out / probably no

### Waystones: Sable

Disposition: **no**.

Reason: teleportation is a hard no. Sable/Aeronautics compatibility does not change the travel philosophy: Tenpack protects roads, rails, mounts, vehicles, aircraft, borders, and fuel logistics.

### Backpacks and backpack Create integrations

Disposition: **no**.

Reason: no backpacks. This includes Sophisticated Backpacks Create Integration and similar.

### Create: Applied Kinetics / AE2 integration

Disposition: **defer with AE2**.

Reason: AE2 was recommended, but we are not sure Tenpack wants AE2. Applied Kinetics should not be considered unless the pack intentionally adopts AE2.

### Create: Escalated

Disposition: **no / unnecessary**.

Reason: it is literally functional rotation-powered escalators/stairs, not escalation/combat. Cute, but unnecessary for the current pack.

### Create: Contraption Terminals / storage-network Create integrations

Disposition: **defer/no unless the storage system is approved first**.

Reason: storage networks can erase logistics pressure if added casually. Do not add contraption terminal integrations before deciding whether that storage mod belongs.

### Created Simple Storage Network resource pack

Disposition: **approved only as cosmetic support if Simple Storage Network is ever present**.

Reason: the resource pack itself is fine. It is not a reason to add Simple Storage Network, and it is useless if Simple Storage Network is not installed.

### Tom's Simple Storage Create Recipes / Tom's Storage Create GUI resource packs

Disposition: **defer with storage-system decision**.

Reason: cosmetic/recipe support is fine only if that storage mod is intentionally selected. Do not let a texture pack pull the pack toward easy storage networks by accident.

### Sophisticated Storage Create Integration

Disposition: **defer/no for now**.

Reason: not backpacks, but still a full-featured storage integration. Review only in a storage/logistics pass.

### Create: Ender Transmission

Disposition: **no**.

Reason: remote/undimensional machine transfer fights physical logistics, rail routes, exposed supply lines, and faction pressure points.

### Create: Power Loader

Disposition: **no for now**.

Reason: immersive chunk loaders are tempting, but chunk loading changes server performance and raid/automation balance. Revisit only with a server policy.

### Create: Cobblestone / Create Sifting / Create Mechanical Spawner / Create Mechanical Extruder / Create: Molten Vents / Create: Copper & Zinc / Create: Ultimate Factory / Create Compressed / renewable netherite/brass-style addons

Disposition: **no or pressure-point redesign only**.

Reason: these are resource-generation/automation addons. They can be good in expert packs, but in Tenpack they risk erasing mining, trade, territory, and build-material economics unless redesigned into visible, claimable pressure points.

### Create: Fishing Bobber Detector

Disposition: **no**.

Reason: auto-fishing is resource/loot automation noise and does not support the Create/factions identity.

### Create: Trimmed

Disposition: **no / not worth it**.

Reason: using Create materials as armor trims is harmless, but too small to justify another required mod.

### Create: Misc and Things / Create: Bits 'n' Bobs / Create: Dreams & Desires-style grab bags

Disposition: **no for now**.

Reason: broad grab-bag addons are exactly how bloat and hidden bypasses enter the pack. Only reconsider individual mechanics if a specific need appears.

## Current shortlist from this audit

Most promising future tiny pass, if we want one:

1. Create: Framed
2. Create: Oxidized
3. Create: Vibrant Vaults
4. Create: Blocks & Bogies
5. Create Train Utilities
6. Create More: Parallel Pipes
7. Create: Extended Wrenches

Separate non-content test bucket:

1. CreateBetterFps
2. Create: LazyTick
3. Create: Threaded Trains, only if train load becomes real
4. Big Contraptions, only if contraption rendering/carrying becomes a real pain

Separate structure test bucket:

1. Create: Easy Structures
2. Create: Rustic Structures
3. Create: Let The Adventure Begin
4. Create: Structures Arise
