# Tenpack Travel Overhaul Master Plan

Status: active planning + source audit. Do not treat current `tenpack_travel` as finished.

## Core correction

Tenpack Travel is currently a prototype. The right direction is not “add a brush, fence, trough, and chat messages.” Travel must feel like a polished player-facing ecosystem.

The goal is **choice-based travel**, not progression-gated travel. Players should be able to choose their style:

- walking / climbing / roads
- horses and mounted travel
- animal companions and working animals
- carts / carriages / pack cargo
- camps and rest stops
- maps and natural navigation
- Create rails
- Create-built ships / water movement where possible
- Create Aeronautics for air

No feature ships unless it has real assets, real UI/feedback, real gameplay purpose, and real in-game accessibility.

## Non-negotiable design rules

1. **No half-baked placeholders**
   - No renamed vanilla fences.
   - No vanilla placeholder item textures.
   - No chat-spam-only interactions pretending to be UI.

2. **No traditional progression ladder**
   - Travel is not Create progression.
   - Do not make players unlock travel modes in a tech tree.
   - Let players choose how they travel.
   - Any expensive option should be expensive because it is powerful or physical, not because a quest says so.

3. **Physical travel stays physical**
   - No global recall teleport.
   - No Waystones-style fast travel.
   - No pet teleport from anywhere.
   - No GPS/minimap default behavior.

4. **Storage stays limited and physical**
   - Bulk cargo belongs in carts, animals, ships, trains, camps, or Create infrastructure.
   - Small pouches/bundles are acceptable only if they stay narrow; the Supplementaries lunch basket is the current six-slot prepared-food exception.
   - Supplementaries sacks are disabled because they are general portable storage, not travel preparation.
   - Backpacks/Enderbags/remote storage must be disabled, removed, or patched out if using mods that include them.

5. **Aesthetics are gameplay**
   - Good assets matter.
   - Good UI matters.
   - Players should see and feel that an animal/camp/travel tool is special.

6. **Private-use source adaptation is allowed**
   - We can inspect, fork, patch, or adapt open/available mods for this private pack.
   - Source/jar inspection should be a primary tool, not a last resort.

## Current Tenpack Travel failures

### Grooming Brush

Current problem:

- weak visual identity
- no proper UI
- no meaningful animal reaction
- brushing mostly means “print info / tiny bond logic”
- no reason to care

Target:

- real item art
- brushing animation/particles/sound
- animal mood reaction
- animal care state
- compact animal status UI
- mode/role controls where appropriate

### Hitching Post

Current problem:

- effectively a renamed fence
- no meaningful block entity state
- no stable/camp identity
- no UI
- no animal management

Target:

- real stable/camp anchor block
- block entity storing anchored animal UUIDs/state
- UI listing hitched animals
- modes: stay / roam around post / follow / work
- visible ropes/tie rings/occupied state
- integration with nearby trough/camp objects

### Feed Trough

Current local status:

- no screen inventory; feed is inserted/withdrawn through physical block interaction
- stores real feed item stacks with persistence and drop-on-break behavior
- exposes empty/low/medium/full fill visuals
- animals path to the trough and pause before consuming a stored meal

Target:

- keep the physical block-entity storage model instead of a menu/storage bypass
- tune visible fill readability after in-world playtest
- tune animal pathing, wait time, and species eligibility after camp playtest
- richer eating particles/sound if the trough still feels too quiet
- direct mood/care/bond effects
- no auto-breeding by default

### Whistle

Current problem:

- weak payoff
- limited call behavior
- no control layer
- no tracking/control depth

Target:

- physical whistle item plus keybind
- survival/adventure keybind actions require carrying the physical whistle; creative/spectator can test without one
- tap = call/regroup nearby bonded animals
- hold = command radial or compact command UI
- sneak/targeted command for one animal/group
- tracking clues, not GPS
- no teleport

### Animal bonding

Current problem:

- bond gives almost nothing
- animals do not visibly react to care
- no command modes
- no meaningful roles beyond first eagle scout experiment

Target:

- bond + mood + mode + role + reaction + memory
- animals respond differently when groomed, fed, hurt, scared, rested, called, hitched, worked
- more bond = more reliable response, better tracking, more command options, more trust

## Desired Tenpack Travel v0.2 feature set

This is the rescue release. It should focus on making the existing mod feel real before expanding wildly.

### 1. In-game availability and polish baseline

- All Tenpack Travel items/blocks must appear in creative.
- `/give @s tenpack_travel:<id>` must work for every item/block.
- Recipes must exist where survival crafting is intended.
- Item/block names/tooltips must be clear.
- Client/server/public jar sync must be verified before any push.

### 2. Custom visual assets

Required assets:

- Grooming Brush item texture/model
- Whistle item texture/model
- Hitching Post block model/textures
- Hitching Post occupied/rope visual states if feasible
- Feed Trough block model/textures
- Feed Trough fill-level visuals
- UI icons:
  - bond
  - mood
  - follow
  - stay
  - roam
  - work
  - guard
  - scout
  - pack/cargo
  - hungry
  - hurt
  - content

Asset direction:

- grounded Minecraft style
- warm wood/leather/brass/iron materials
- stable/camp vocabulary
- no vanilla item placeholders

Image generation should be used for concept sheets and icon direction, then converted/refined into actual pack textures.

### 3. Animal status UI

Replace chat-first grooming output with a real screen.

Grooming Brush UI should show:

- animal name/species
- health band
- mood
- bond tier
- current command mode
- roles
- care state
- equipment/cargo state
- relationship owner/trust summary
- action buttons where allowed

Example:

```text
[Juniper]
Horse

Health: Healthy / fresh
Mood: Calm
Bond: Familiar
Mode: Follow
Roles: Mount, Draft
Care: Groomed today, fed recently
Gear: Saddle, iron armor

[Follow] [Stay] [Roam] [Work]
```

Creative/debug view may show exact numbers.
Survival view should use bands and icons.

### 4. Animal mood and care state

Add persistent animal state:

- mood
- last groomed time
- last fed time
- stress / alertness
- current command mode
- optional camp/hitch anchor
- optional role cooldowns

Mood labels:

- Content
- Calm
- Hungry
- Tired
- Stressed
- Hurt
- Alert
- Afraid

Effects:

- groomed animals calm faster and respond better
- fed animals heal/recover and trust more
- hurt/stressed animals may ignore risky commands
- high-trust animals recover/obey more reliably

### 5. Command modes

Base modes:

- **Follow**: physically follows owner/player, no teleport
- **Stay**: remains near current position/anchor
- **Roam**: wanders in local radius
- **Work**: enables role-specific task behavior
- **Guard**: only for appropriate animals, balanced and explicit

Mode changes should be visible:

- sound
- particles
- UI icon
- animal look/pose response where possible

### 6. Hitching Post v2

Real behavior:

- block entity
- stores hitched animals by UUID
- shows animals in UI
- controls anchor radius/mode
- supports “roam near post”
- integrates with trough if nearby
- optionally applies a “stable” mood recovery bonus

UI:

```text
Hitching Post

Hitched animals:
- Juniper — Calm — Stay
- Mule — Hungry — Roam

[Call Nearby] [Set Roam Radius] [Release Selected]
```

No teleport recall.
If an animal is missing/out of range, UI should say last known direction/status only if we have enough data.

### 7. Feed Trough playtest/tuning

Implemented real behavior:

- inventory of feed items
- visible fill state
- animals path to it when hungry/eligible
- eating takes time
- consumes feed
- no breeding by default
- improves mood/care/bond

Remaining tuning:

- range and pathing behavior at real camps/stables
- wait time before eating
- species eligibility and per-species feed validation
- richer eating animation/sound polish if needed

Potential block states:

- empty
- low
- medium
- full

### 8. Carry On physical-carry policy

Carry On may stay as an embodied "carry the object with your hands" convenience, but not as portable storage.

Implemented policy:

- `tenpack-carryon-policy` extends Carry On's block blacklist on the server.
- Core storage/container blocks are blocked from pickup: chests, trapped chests, barrels, shulker boxes, ender chests, hoppers, droppers, dispensers, furnaces, smokers, blast furnaces, crafters, brewing stands, Farmer's Delight cabinets/baskets, Supplementaries safes/sacks, and Create item vaults.
- The goal is to preserve physical object handling while preventing filled-container backpack behavior and claim/lock bypasses.

Playtest:

- confirm ordinary non-storage objects remain carryable enough to justify the mod;
- confirm filled storage blocks cannot be picked up;
- confirm claims/locks/corpses/Create contraptions do not create multiplayer abuse.

### 9. LeashAll physical-lead policy

LeashAll may stay because it helps caravans and animal handling remain physical: players still use leads, walk the route, and manage vulnerable animals in-world.

Implemented policy:

- client/server default configs are mirrored;
- allowlist mode stays off so ordinary animal handling is broad enough to be useful;
- max leashes per player stays at 6 so it helps small caravans without becoming a mass-mob dragnet;
- bosses, raiders, warden, dragon, wither, elder guardian, villagers, wandering traders, and players stay blocked;
- `tools/check-pack-configs.py` enforces these defaults.

Playtest:

- confirm intended caravan animals can be led together without teleporting;
- confirm blocked entities cannot be leashed;
- confirm multiplayer use does not create griefing/claim bypasses.

### 10. Whistle v2

Add physical whistle item and keep keybind.

Controls:

- survival/adventure: keybind packets only do useful server-side work if the player has `tenpack_travel:whistle`
- creative/spectator: bypass item requirement for testing
- tap: nearby bonded animals respond/regroup
- hold: command radial/compact command menu
- sneak + use: targeted command to looked-at animal or current group

Commands:

- Follow
- Stay
- Roam
- Work
- Guard, if appropriate
- Return to Post, if anchored
- Track, if high enough bond / appropriate animal

Tracking design:

- no live map marker
- no coordinates
- no teleport
- use rough clues:
  - “tracks lead north-east”
  - sound/particle ping in direction
  - last-seen map note if manually recorded
  - animal calls back if in range

## Companion / animal source adaptation plan

### Doggy Talents Next

Research target for:

- pet command system
- training/talents
- tracking
- pet UI/feedback
- animation/reaction ideas
- whistle behavior

Likely keep/adapt:

- command modes
- pet identity and training concepts
- tracking concepts
- care/reaction depth

Patch/disable/remove:

- teleport from anywhere
- GPS-like tracking if too exact
- dog-only assumptions if porting to all animals

### Petting - Tame Any Mob

Research target for:

- any-mob taming
- follow/wait/wander/guard modes
- status reports
- tethers/roam radius
- hostile pet balance
- equipment/inventory code

Likely keep/adapt:

- any-mob companion possibility
- effort-based taming
- modes
- guard/defense toggles
- status UI concepts

Patch/disable/rebalance:

- teleport orb
- goat horn global teleport
- pet bed death teleport/immortality
- boss-pet abuse
- excessive portable inventory
- infinite pet caps

Any-mob taming principle:

- let people have pets
- make stronger pets require more effort/risk
- no instant hostile army
- no teleport swarm

## External travel integration candidates

### AstikorCarts Redux — priority candidate

Selected for first in-pack land-cart testing.

Why:

- NeoForge 1.21.1
- MIT
- real carts/carriages
- supply cart
- animal cart
- plow
- physical cargo
- animal-powered travel

Integration goals:

- bond/role recognizes draft work
- hitching post can anchor carts nearby
- animals pulling carts gain work/travel trust
- carts become reason to build roads/camps

Current WIP integration:

- Added `astikorcartsredux-1.2.2.jar` to both client and server mods as optional uncommitted travel WIP.
- Modrinth project/version: `astikorcarts-redux`, `1.2.2`, published 2025-09-05, required client/server.
- Hashes: SHA-256 `44b2db02a6699481d212f0fa0a766b52776650396fa67951a428ad32b2d7dc24`; Modrinth SHA-512 `c3cbee867aa4ef73606105247c9c1106dfcb7d3e9f6bf387f3285ba1425f9d9966beb335fc01c8d314a56251933dcc712384eb4b6953fa948a0191b915f5e9ea`; SHA-1 `a120524992f6010de34a5f36f8789d7ab95f99db`.
- Added mirrored `astikorcartsredux-common.toml` configs to lock down cart pullers: horse/donkey/mule/camel for transport carts, horse/donkey/mule/cow for farm implements, and player-only hand carts. This avoids Astikor's broad empty-list default.
- Do not expose a Tenpack `Work` command just because the jar exists. Add it only after playtesting cart attachment, cargo limits, road feel, and draft-animal care/bond hooks.
- First soft hook now exists in Tenpack Travel: `AstikorIntegration` centralizes the Astikor reflection bridge (`AbstractDrawnEntity`, public `getPulling()`, active-cart/draft-role helpers), and `AstikorDraftWorkHandler` gives player-controlled draft animals slow work-care/bond credit only while real Astikor carts are moving. Whistle/group/direct commands respect active draft animals. Keep this as behind-the-scenes care logic until playtest proves command-mode/work UI should be exposed.

### Create Horse Power — good candidate

Why:

- animal labor for Create
- fits working-animal fantasy
- no need for progression gating
- choice-based infrastructure

Integration goals:

- Tenpack Travel can show work fatigue/mood
- grooming/feed/rest affects work reliability
- animal labor is a chosen style, not a tech gate

### Better Climbing / climbing tools

Need deeper source/mod search.

Acceptable direction:

- polished climbing improvement
- ropes
- rope ladders
- climbing picks
- pitons/anchors

Avoid:

- lame universal “shift on wall = climb anything” if it has no polish/cost
- teleport/grapple cheese unless very grounded

### Camping mod patching

Let’s Do Camping has good assets/features but bad storage risk.

Keep if patchable:

- tents
- sleeping bags
- grill
- camp visuals

Disable/remove:

- backpacks
- Enderbag
- remote storage
- excessive portable inventory

If config cannot disable, patch source/jar privately.

### Scout pouches

Research and likely use/patch if it fits.

Desired:

- small pouches
- good visuals
- limited slots
- no backpack-tier capacity
- Curios/body-slot integration okay if modest

## Navigation/map plan

### Immersive Travel Overhaul

User found it and likes assets/UI. Need source/jar/version audit.

Known from quick search:

- older mod, likely 1.16.5–1.20.x, may not support 1.21.1
- features: hides F3 coords, realistic navigation items, sextant/barometer/dimension tools
- may have better visual assets than Eli

Use as:

- asset/UI reference
- possible private port if source/jar viable

### Eli’s Immersive Navigation

Good concept:

- hides F3 coordinates
- barometer
- sextant
- compass points north
- rough coordinates / sky requirement

Potential issue:

- Fabric only
- needs Connector test
- approximate coordinates still may need tuning

### Map Atlases

User likes map concept. Plan is not to reject outright.

Fork/patch direction:

Keep:

- physical atlas item
- map stitching
- manual map handling
- map page UI
- maybe manual annotations

Disable/remove:

- minimap
- live GPS
- waypoints
- automatic player radar

Goal:

- maps matter
- atlas is physical
- no always-on GPS/minimap

## Water and air correction

### Water

Do not default to Small Ships.

Reason:

- item ships miss the Create-built direction
- players should build ships/vehicles where possible

Research next:

- Create-compatible water vehicles
- Create Aeronautics/Simulated/Offroad water possibilities
- Valkyrien/Eureka-like options if compatible
- docks/mooring/cargo loading

If there is no good Create-built ship path, decide later. Do not rush item-ship mod adoption.

### Air

Use Create Aeronautics.

Do not add Immersive Aircraft/biplanes by default.

## Rails

Do not overcook rails.

Current rail stack is already strong:

- Create
- Steam ’n Rails
- Create Railways Navigator
- Bells & Whistles
- Create Deco

Only fix if broken.

## Storage policy

Reject or patch out:

- Sophisticated Backpacks
- Traveler’s Backpack unless brutally limited
- Let’s Do Camping Enderbag/backpacks
- Infinite Storage Bundle
- Better Bundle if it becomes portable chest
- Scout/pouch/satchel/bundle-expansion mods as drop-in dependencies
- remote Ender Chest access

Accept:

- Future Tenpack-native Scout-style pouches only if deliberately capped and non-backpack-tier
- vanilla bundles and vanilla-like bundle behavior
- quiver-like narrow storage
- physical carts
- animals/chests
- Carry On short-distance convenience
- trains/warehouses/camp crates

Current guardrails:

- Carry On remains allowed only as short-distance embodied carrying; Tenpack's Carry On datapack blocks filled-container pickup.
- Supplementaries lunch basket is the current six-slot prepared-food pocket exception; Supplementaries sacks are disabled.
- `tools/check-mod-integrity.py` blocks common backpack, pouch, satchel, infinite/expanded-bundle, and Scout-pouch-style mods unless a future deliberate audit adopts a narrow Tenpack-native form.

## Execution sequence

### Step 1 — source audits launched

Subagents launched for:

1. Doggy Talents Next + Petting companion source audit
2. Immersive Travel Overhaul + Eli + Map Atlases + Scout navigation/storage audit
3. AstikorCarts + Create Horse Power + climbing + camping + Create water audit
4. Current Tenpack Travel UI/assets/functionality rescue spec

### Step 2 — Tenpack Travel v0.2 spec

When audits return, write exact implementation spec:

- class/file changes
- UI screens
- packets
- block entities
- assets
- source patches/forks
- test checklist

### Step 3 — implement Tenpack Travel rescue

Do first:

- creative/give sanity
- real assets
- grooming UI/reactions
- hitching post block entity/UI
- trough inventory/fill/eating
- animal modes
- whistle v2

### Step 4 — companion adaptation

Then:

- fork/adapt Doggy/Petting pieces
- any-mob taming balance
- tracking without teleport/GPS
- command system

### Step 5 — external travel integration

Then test/add/patch:

- AstikorCarts Redux
- Create Horse Power
- Scout pouches
- Better Climbing/ropes/picks
- camping patch if worthwhile
- map/navigation fork

### Step 6 — Create vehicle direction

Then:

- water travel via Create-compatible construction if possible
- Aeronautics remains air travel direction
- rails left mostly alone

## Validation standard

Before any push, every travel feature needs:

- creative tab check
- `/give` check
- item/block model check
- recipe check if applicable
- full client launch check
- server load check
- multiplayer safety check where relevant
- actual in-world interaction test
- no missing assets
- no placeholder visuals
- no broken configs
- public rebuild
- no push without permission

## Current principle

Do not remove the existing Tenpack Travel idea. Rescue it.

The current version is a skeleton. v0.2 must make it feel like a real mod:

- visible
- usable
- reactive
- pretty enough
- meaningful enough
- integrated enough

No more half-baked placeholders.


## Navigation/maps source audit decision

Latest audit result:

- **Immersive Travel Overhaul** is not usable directly for 1.21.1 NeoForge: latest jar is Fabric 1.20/1.20.1 alpha, old Forge build only reaches 1.16.5, source is older, and the latest jar appears not to meaningfully hide F3 coordinates despite the description. However, it is Public Domain / Unlicense and has useful navigation-item concepts/assets.
- **Eli’s Immersive Navigation** is the best technical source reference for natural navigation/F3-coordinate hiding, but it is Fabric/GPL and should not become a Connector-dependent core pack pillar without careful porting/legal boundary decisions.
- Correct route: build or port a small native NeoForge 1.21.1 **Tenpack Immersive Navigation** module later. It should use Eli-style technical behavior as reference and ITO-style public-domain items/assets as inspiration/donors where useful.
- **Map Atlases Forge/NeoForge** is the best atlas base, but must be forked/patched: keep physical atlas, crafting, map storage/stitching, cartography/lectern UI, sounds/art; remove HUD minimap, coordinate text, pins/waypoints/entity radar/Xaero conversion, and reduce GPS-like current-location behavior.
- **Scout** pouches are not a good direct 1.21.1 NeoForge dependency. If limited pouches are still desired, either implement small Tenpack-native pouches or evaluate a narrow Bundle API route. Do not add backpack-tier storage.


## Vehicles/infrastructure/camping source audit decision

Latest audit result:

- **AstikorCarts Redux** is the top land/animal travel candidate: NeoForge 1.21.1, MIT, physical carts, supply carts, animal carts, hand carts, plows, seed drills, reapers, cart sounds/assets, and configurable pull entities. Use it as the default animal/cart travel layer after in-pack testing.
- **Create Horse Power** fits as optional animal labor infrastructure, not travel progression. It should integrate later with animal care/mood/work state rather than becoming a tech gate.
- Vertical travel should favor physical tools/infrastructure over magic wall climbing. Best candidates to evaluate later: Climbable Ropes for Create Aeronautics, then carefully balanced grappling hooks; Better Climbing is mostly ladder QoL.
- **Let’s Do Camping** should not ship as-is. Config cannot fully disable backpacks/Enderbag; if used, it requires private hard-disable patching for registrations/recipes/loot/trades/keybind/network/advancements. The mod-integrity checker now blocks accidental Let’s Do Camping/backpack/Enderbag-style installs until that audit/patch exists.
- No polished Create-compatible 1.21.1 NeoForge water/ship replacement for Small Ships was found. Do not add an item-ship placeholder. Defer water vehicles until Create/Aeronautics-compatible water strategy is solid.
