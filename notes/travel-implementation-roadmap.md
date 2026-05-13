# Tenpack travel implementation roadmap

Status: concrete backlog derived from `notes/animal-travel-audit.md` and `notes/travel-philosophy.md`.

Goal: make animal travel, roads, camps, and logistics become the practical path of least resistance while preserving friction. No GPS compasses, no backpacks, no Jade-style passive overlays.

## Product pillars

1. **Information is intentional**
   - Players learn animal quality by using tools, records, and stables.
   - No always-on entity stat overlay.

2. **Cargo is physical**
   - Cargo lives on animals, carts, elephants, camps, caches, roads, trains, etc.
   - No personal backpack bypass.
   - Narrow food exception: the Supplementaries lunch basket is an allowed six-slot field-ration carrier, not general cargo; `tools/check-pack-configs.py` keeps it enabled/placeable/overlay-readable and capped at 6 slots.
   - Supplementaries' separate sack is disabled because it is nine-slot general portable storage, not field-ration logistics.

3. **Roads reduce pain**
   - Roads do not teleport anyone.
   - Roads make animals/carts easier enough that players naturally build them.

4. **Camps make distance livable**
   - Camps solve food, rest, animal parking, cargo sorting, and route memory.
   - Camps are social landmarks, not UI menus.

5. **Better animals and better gear are progression**
   - A mule is not better than a backpack would be. It is simply the best early answer because backpacks are absent.
   - The player can then improve the mule system: stats, tack, saddlebags, roads, stables, elephants, carts.

## Implementation order

### Milestone 0 — documentation and no-risk pack guidance

Status: mostly done.

Files:

- `notes/animal-travel-audit.md`
- `notes/travel-philosophy.md`
- this roadmap

Tasks:

- [x] Audit all 89 Alex's Mobs spawn-egg mobs.
- [x] Identify strong travel/cargo/utility animals.
- [x] Write travel philosophy.
- [x] Define no-GPS/no-backpack/no-Jade constraints.
- [x] Turn selected sections into questbook / guide source text in `notes/travel-questbook-guide.md`.

Acceptance criteria:

- Anyone reading the notes can explain why Tenpack wants animals/roads/camps instead of backpacks and minimaps.

### Milestone 1 — live Alex's Mobs config control

Purpose: make Alex's Mobs behavior deterministic in the actual pack.

Current finding:

- `tenpack-specs/overrides/config/alexsmobs-common.toml` exists.
- Active `client/config/alexsmobs-common.toml` and `server/config/alexsmobs-common.toml` were not found during audit.

Tasks:

- [ ] Decide whether to copy the common config into active `client/config/` and `server/config/`.
- [ ] If copied, keep weather/worldgen/biome mutation values unchanged unless partner approves.
- [ ] Explicitly preserve:
  - `straddleboardEnchants = true`
  - `falconryTeleportsBack = false`
  - `tusklinShoesBarteringChance = 0.025` until playtested
  - `raccoonStealFromChests`, `crowsStealCrops`, `seagullStealing` unless too annoying in practice
- [ ] Do **not** tune:
  - `limitGusterSpawnsToWeather`
  - `mungusBiomeTransformationType`
  - named-region/worldgen/weather-adjacent spawn rules

Acceptance criteria:

- Active client/server config exists if we choose deterministic Alex behavior.
- The config does not step on partner-owned weather/worldgen/named-region design.

### Milestone 2 — animal inspection item

Status: v1 implemented locally as `mods-src/tenpack-travel` / `tenpack_travel-0.1.0.jar`. `gradle build` passes and the standalone NeoForge dev server boots with Tenpack Travel loaded. Full-pack in-game interaction playtest is still needed before treating as finished.

Purpose: replace Jade-style stat overlays with a deliberate item interaction.

Recommended mod:

- New small mod: `tenpack-travel`.
- Alternative: add to an existing Tenpack utility mod only if that mod is intended to become general-purpose. Do not overload `tenpack-death` unless it is renamed/reframed.

Recommended item names:

- `Grooming Brush` — early, cheap, coarse bands.
- `Measuring Tack` — later, more precise inspection.

#### Grooming Brush v1

Player uses item on an animal/mount.

Shows a chat/actionbar/system message, not a persistent overlay:

```text
Chestnut Horse
Health: Sturdy
Speed: Swift
Jump: Strong
Temperament: Calm
Cargo: None
```

For cargo animals:

```text
Mule
Health: Healthy
Speed: Steady
Jump: Fair
Temperament: Calm
Cargo: Pack (15 slots)
```

For Alex's Mobs:

```text
Elephant
Health: Massive
Speed: Steady
Temperament: Gentle
Cargo: Freight (54 slots when chested)
Notes: needs wide roads
```

Survival/adventure output:

- Use bands only.
- No raw decimals.
- No coordinates.
- No hidden debug values.

Creative/spectator/admin output:

- Can include exact values for debugging:
  - health/max health,
  - movement speed attribute,
  - jump strength if present,
  - cargo slots,
  - owner UUID/name if available.

Suggested stat bands:

#### Health

- `Frail`: < 15 HP
- `Healthy`: 15–25 HP
- `Sturdy`: 25–40 HP
- `Massive`: > 40 HP

#### Speed

For vanilla horse-like mounts, tune after playtest:

- `Slow`
- `Steady`
- `Swift`
- `Exceptional`

Do not expose raw Minecraft movement-speed decimals in survival.

#### Jump

- `Poor`
- `Fair`
- `Strong`
- `Remarkable`
- `Unknown` for non-jump mounts.

#### Cargo

- `None`
- `Pocket` — tiny/weird storage, e.g. catfish tricks not necessarily exposed in v1.
- `Pouch` — Kangaroo 9 slots.
- `Pack` — donkey/mule/llama style.
- `Freight` — Elephant 54 slots.

Target entity support for v1:

- `minecraft:horse`
- `minecraft:donkey`
- `minecraft:mule`
- `minecraft:llama`
- `minecraft:trader_llama`
- `minecraft:camel`
- `alexsmobs:elephant`
- `alexsmobs:kangaroo`
- `alexsmobs:grizzly_bear`
- `alexsmobs:komodo_dragon`
- `alexsmobs:endergrade`
- `alexsmobs:laviathan`
- `alexsmobs:tusklin`

Implementation notes:

- NeoForge event likely needed: entity interaction / item use on entity.
- Read vanilla mount attributes with `Attributes.MAX_HEALTH`, `Attributes.MOVEMENT_SPEED`, and horse jump attribute where available.
- For Alex's Mobs, avoid fragile deep integration at first:
  - identify by entity type registry ID,
  - use known audit-derived cargo values,
  - use general health/speed attributes,
  - only use reflection/direct compile dependency later if needed.
- Add a tag or config list for inspectable animal entity IDs so the set can grow without code changes.

Recipe idea:

- Grooming Brush:
  - stick
  - string
  - copper ingot or brush-like ingredient
  - leather / bristles substitute
- Measuring Tack:
  - leather
  - string
  - brass/copper precision ingredient if Create integration is desired later

Acceptance criteria:

- Player can inspect a horse/mule without Jade.
- Player gets enough information to care about breeding and choosing animals.
- Survival output remains approximate and flavorful.

### Milestone 3 — animal parking and anti-loss infrastructure

Status: first small pieces implemented locally in `tenpack_travel-0.1.0.jar`: a craftable `tenpack_travel:hitching_post` fence-like block and `tenpack_travel:feed_trough` camp-care block. They keep animals physical; there is no recall, no teleport, and no tracking UI. Standalone dev-server smoke test passes; full-pack in-game interaction playtest is still needed.

Purpose: reduce bad friction without deleting good friction.

Bad friction to remove:

- animal wanders off while player sleeps/explores nearby,
- mount gets lost around camp,
- no clear place to park a cargo animal,
- animal handling feels worse than just staying home.

Good friction to keep:

- you still have to bring the animal,
- you still have to protect it,
- it still occupies space,
- you still need roads/bridges/stables,
- there is no global teleport button.

#### Hitching Post

Status: implemented as a first-pass block.

Function:

- A fence-like block intended for camps/stables/roads.
- Uses vanilla lead/fence-knot behavior: lead animals nearby, then use lead/post behavior to tether them.
- Empty-hand interaction hitches nearby animals currently led by the player, or lists animals already hitched to that post.
- Appears in Functional Blocks creative tab.
- Crafting recipe: oak fence + leads + iron nugget, yields 2.
- Animals remain physical and vulnerable.

Deferred:

- Custom stable ownership/records.
- Nearby pathable return behavior.

Rules:

- No cross-dimensional recall.
- No long-range teleport.
- Optional: if animal is within a short radius and pathable, it can walk back to post.
- Optional: if pathing fails and animal is very close, a small unstuck nudge is acceptable.

Recipe:

- fence + lead + iron nugget/copper nugget.

#### Feed Trough

Status: block-entity camp object implemented locally in `tenpack_travel-0.1.0.jar`.

Function:

- A camp/stable block that stores up to 12 real feed item stacks, persists them to NBT, and drops leftovers if broken.
- Works in a small radius around the trough.
- Animals physically path toward it, pause at the trough before eating, and consume one stored feed item per meal.
- Eligible animals: tamed horses/donkeys/mules/llamas, tamed Alex's Mobs pets/mounts, camels, and leashed animals.
- Does not breed automatically.
- Has no screen inventory; players insert feed by using it on the trough, check status with an empty hand, and sneak-empty-hand to withdraw one meal.
- Exposes `fill=0..3` visual states for empty/low/medium/full feed levels.

Rules:

- Consumes real food.
- Does not make animals immortal.
- Works only in a small radius.
- No automation, no recall, no teleport safety.

Recipe:

- oak slabs + hay block.

Deferred:

- Better per-species feed validation.
- Richer eating animation/sound polish after in-world playtest.
- Tuning of range, wait time, care XP, and eligible species after multiplayer camp use.

Acceptance criteria:

- Players can park a mule/horse/elephant at a camp and reasonably expect it to stay there.
- The infrastructure feels physical and buildable.
- No teleporting convenience loop is introduced.

### Milestone 4 — road/camp gameplay support

Purpose: make roads and camps mechanically useful before they become aesthetic.

#### Road Sign Pack

Current narrow sign mod:

- Current pack audit found no Click Signs/signpost dependency installed. Route signage should use vanilla signs, hanging signs, Supplementaries way signs, Trail Markers, maps, and Route Journals. Clickable/command/waypoint sign mods are now deferred/blocked by mod-integrity unless a future deliberate audit adopts one without command, teleport, waypoint, or GPS behavior.
- Sailing/water travel is now tracked as a first-class pillar in `notes/sailing-water-travel-roadmap.md`. Current pack baseline has no dedicated ship/cargo-vessel mod installed; Small Ships has a NeoForge 1.21.1 candidate build but needs license/config/gameplay audit before adoption. Jumpy Boats is explicitly guarded as a water-route infrastructure bypass, and deferred ship stacks such as Small Ships plus Eureka/Valkyrien-style moving ships are now blocked by mod-integrity unless a future deliberate water-vehicle adoption pass replaces that policy.
- Physical maps/no-GPS navigation is now tracked in `notes/physical-maps-navigation-roadmap.md`. Current pack baseline has no dedicated atlas mod installed; Map Atlases remains a candidate/reference but must be audited/forked/configured so it does not become minimap/GPS gameplay. Obvious minimap/waypoint/biome-compass/structure-compass mods are guarded in mod integrity, and Map Atlases / Antique Atlas-style atlas mods are now deferred by the same guardrail until the physical-atlas no-GPS audit is complete.

Needed content/design:

- Standard route sign templates:
  - `North Road`
  - `River Camp`
  - `Elephant Crossing`
  - `Danger: Swamp`
  - `Stable Ahead`
  - `Ferry`
- Encourage sign placement in quest text.

No GPS behavior.

No command/click behavior by default: route signs are text and physical landmarks for everyone, not private waypoint buttons or command surfaces.

#### Camp Kit

Potential crafted kit or guide objective, not necessarily a new item:

Minimum camp:

- bed/sleeping setup,
- campfire,
- chest/barrel/cache,
- hitching post,
- feed trough,
- sign,
- Chart Table or vanilla cartography table.

Acceptance criteria:

- A camp solves repeated route problems.
- Camps become player-made landmarks.

#### Freight road standards

Recommended build guidance:

- Horse road: 2–3 blocks wide, low branches cleared.
- Mule road: 3 blocks wide, safe bridges.
- Elephant road: 4+ blocks wide, wider turns, 4-block vertical clearance where possible.
- Wagon/stagecoach road later: width/turning radius determined by vehicle implementation.

Acceptance criteria:

- Players understand why road architecture matters.
- Elephants remain powerful because the route had to be prepared.

### Milestone 5 — animal gear progression

Purpose: let players improve animals instead of adding backpacks.

Early gear:

- basic saddle / vanilla saddle access as-is or via current pack progression,
- simple saddlebags for mules/donkeys if custom system is built,
- Grooming Brush.

Mid gear:

- reinforced saddlebags,
- better hitching/stable infrastructure,
- elephant carpet/tack identity,
- kangaroo combat kit via existing pouch weapon/armor.

Late gear:

- Measuring Tack,
- elephant freight harness,
- Tusklin hogshoes support/signposting,
- Laviathan saddle+tack guidance,
- wagon harness.

Balance rule:

- Upgrade animal logistics, not player inventory.
- Any extra cargo should remain physically tied to a vulnerable entity or vehicle.

Acceptance criteria:

- A player who wants more carrying capacity thinks “better animal / better tack / better route,” not “where backpack?”

### Milestone 6 — carts, wagons, and stagecoaches

Purpose: make roads become civilization-scale logistics.

Status:

- AstikorCarts Redux is now the selected first land-cart integration candidate and has been copied into both `client/mods/` and `server/mods/` for WIP testing.
- Added jar: `astikorcartsredux-1.2.2.jar` (Modrinth `astikorcarts-redux`, version `1.2.2`, NeoForge 1.21.1, MIT, required client/server).
- Jar hashes: SHA-256 `44b2db02a6699481d212f0fa0a766b52776650396fa67951a428ad32b2d7dc24`; Modrinth SHA-512 `c3cbee867aa4ef73606105247c9c1106dfcb7d3e9f6bf387f3285ba1425f9d9966beb335fc01c8d314a56251933dcc712384eb4b6953fa948a0191b915f5e9ea`; SHA-1 `a120524992f6010de34a5f36f8789d7ab95f99db`.
- Added mirrored `client/config/astikorcartsredux-common.toml` and `server/config/astikorcartsredux-common.toml` so `pull_animals` is explicit instead of using Astikor's broad empty-list default. Transport carts currently allow horse/donkey/mule/camel; farm implements allow horse/donkey/mule/cow; hand cart remains player-only.
- Catalog role: optional uncommitted travel WIP until full in-game testing proves that carts feel good and do not bypass the physical travel/cargo plan.

Design requirements:

- Animal-drawn.
- Strong road bias.
- Bad off-road.
- Physical collision/turning constraints.
- Cargo and passenger variants.
- Repairable/breakable.
- Not a portable backpack: if left behind, the cargo is left behind.

Suggested vehicle tiers:

1. Hand cart
   - small cargo,
   - player-pulled or animal-pulled,
   - awkward but early.
2. Mule cart
   - medium cargo,
   - needs mule/donkey/horse harness,
   - road-biased.
3. Wagon
   - large cargo,
   - bad off-road,
   - needs wider roads.
4. Stagecoach
   - passengers + limited cargo,
   - faction/town travel identity,
   - needs maintained roads/stations.

Acceptance criteria:

- Roads become the easiest way to move groups/goods.
- Off-road remains possible but painful.
- Players can raid/defend/escort logistics physically.

AstikorCarts test checklist:

1. Boot full Tenpack client/server with AstikorCarts Redux present.
2. Verify recipes/JEI for hand cart, supply cart, animal cart, plow/seed drill/reaper are discoverable and not progression-gated by unrelated Create parts.
3. Test horse/donkey/mule/camel pull feel on flat ground, rough ground, and TRMT road surfaces.
4. Confirm cart cargo is useful but not backpack-tier personal storage; tune recipes/config or fork if needed.
5. Confirm carts interact sanely with Tenpack Travel bond/care/hitching behavior before exposing `Work` command mode.
6. Confirm draft/cart bond XP requires sustained route-scale hauling distance, not stationary cart jitter, wall-bumping, tiny pen loops, or short back-and-forth wiggles.
7. Confirm no teleport/recall/remote storage behavior exists.

## Alex's Mobs role implementation matrix

### Core travel/logistics animals

| Animal | Existing utility | Tenpack role | Needed support |
| --- | --- | --- | --- |
| Elephant | Ride + 54-slot chest | Freight / caravan anchor | wide roads, hitching, stable support |
| Kangaroo | 9-slot pouch + gear use | early companion cargo | brush/bond support, guide text |
| Grizzly Bear | war mount | prestige combat mount | brush/bond support, taming guide |
| Komodo Dragon | saddled hostile mount | dangerous jungle mount | guide text, brush/bond support |
| Laviathan | 4-passenger lava mount | Nether group ferry | guide text, route/camp framing |
| Straddleboard | risky lava board | Nether solo speed | guide text, keep enchants |
| Endergrade | slow End mount | End vertical travel | guide text |
| Tusklin | bucking/hogshoes mount | gear-progression battle mount | guide text, hogshoes signposting |
| Spectre | End lead tow | End island navigation | guide text |
| Cosmaw | End fall rescue | expedition safety | guide text |
| Catfish | bucket-preserved belly cargo | weird aquatic cargo | monitor exploitiness |

### Camp/pet utility animals

| Animal | Existing utility | Tenpack role | Needed support |
| --- | --- | --- | --- |
| Bald Eagle | falconry scouting | active scout | guide text, no radar buffs |
| Crow | gather/deposit items | camp logistics bird | hay/camp guide, crop-friction tuning after playtest |
| Sugar Glider | head slow-fall | vertical safety pet | guide text |
| Capuchin Monkey | shoulder ranged/melee | jungle companion | guide text |
| Caiman | imprint defender/grappler | water camp defense | guide text |
| Warped Toad | Nether anti-insect | Nether expedition pet | guide text |
| Mantis Shrimp | block-breaking utility | aquatic utility pet | griefing review |
| Mimic Octopus | aquatic defense, dries out | aquatic expedition pet | guide text |
| Raccoon | theft/friction + speed cap resource | charming nuisance / minor gear | tune only if annoying |
| Seagull | coastal clue/flavor behavior | physical clue/navigation flavor | keep stealing unless too much; vanilla buried-treasure maps are allowed physical clues |

## Questbook / guide chapter draft

If/when Tenpack has a questbook, travel can be a practical chapter.

### Chapter: First Roads

1. **A Map That Is Yours**
   - Make a filled map; optionally use a Chart Table and Route Journal once available.
   - Text: “Tenpack does not hand you a destination arrow. Your first map is a memory you can hold.”
2. **Leave a Sign**
   - Place a vanilla sign, hanging sign, Supplementaries way sign, or Trail Marker.
   - Text: “A sign is better than a waypoint because everyone can find it later.”
3. **Pack Animal**
   - Use or tame a donkey/mule/llama/camel.
   - Text: “This is not as convenient as a backpack. That is the point: the cargo is now in the world.”
4. **First Camp**
   - Build campfire + bed + chest/barrel + sign.
   - Text: “A camp turns one dangerous trip into a route.”

### Chapter: Stablecraft

1. **Grooming Brush**
   - Inspect a horse/mule.
   - Text: “Good riders learn their animals. The brush tells enough, not everything.”
2. **Hitching Post**
   - Park an animal.
   - Text: “Bad friction is losing the mule to nonsense. Good friction is deciding where to leave it.”
3. **Better Breeding**
   - Breed/select a stronger/faster animal.
   - Text: “The upgrade path is not a backpack. It is a better companion.”

### Chapter: Freight

1. **Elephant Road**
   - Tame/prepare elephant or build wide road segment.
   - Text: “An elephant does not fit through a shortcut. It makes you build civilization.”
2. **Waystation**
   - Build a camp with animal parking and cargo cache.
   - Text: “Routes are made of repeated stops.”
3. **Convoy**
   - Move goods with multiple animals.
   - Text: “A convoy is powerful because it is visible, vulnerable, and worth protecting.”

### Chapter: Other Worlds

1. **Lava Route**
   - Straddleboard/Laviathan guidance.
2. **End Rescue**
   - Cosmaw guidance.
3. **Void Tow**
   - Spectre guidance.

## Concrete first code ticket

**Implemented locally.** Grooming Brush v1 now exists as the first Tenpack travel feature.

Scope implemented:

- New `mods-src/tenpack-travel` NeoForge mod.
- Item: `tenpack_travel:grooming_brush`.
- Recipe + lang + model.
- Entity interaction handler.
- Supports vanilla horses/donkeys/mules/llamas/camels.
- Supports key Alex's Mobs animals by registry ID and known cargo/role labels: elephant, kangaroo, grizzly bear, komodo dragon, endergrade, laviathan, tusklin, bald eagle, crow, sugar glider, cosmaw, spectre, catfish, caiman, capuchin monkey, crocodile, gorilla, mantis shrimp, mimic octopus, mudskipper, raccoon, rhinoceros, seagull, seal, tarantula hawk, warped toad, bison, and emu.
- Sends survival banded message to player.
- Creative/spectator gets exact debug values appended.
- Has a small tooltip explaining its no-GPS/no-Jade purpose.
- Appears in the vanilla Tools & Utilities creative tab for testing.
- No persistent HUD.
- No coordinates.
- No map markers.

Out of scope for v1:

- Stable Ledger / record-book systems; current direction is embodied use + bond, not paperwork.
- Hitching posts.
- Long-range calls.
- Exact stat UI for survival.
- Carts/wagons.

Suggested acceptance test:

1. Boot dev client/server.
2. Spawn/tame horse, mule, elephant, kangaroo.
3. Use Grooming Brush.
4. Confirm chat output is banded and useful.
5. Confirm no overlay/minimap/waypoint behavior exists.
6. Build public manifests.

Validation status as of 2026-05-13:

- `gradle build --no-daemon` passes in `mods-src/tenpack-travel`.
- Built jar hash copied to both client and server after first v0.2 rescue increment plus whistle/ownership/hitching-post safety gates, exact-cue cleanup, feed-trough item persistence, timed trough eating, stable-board memory cleanup, grooming/care presentation polish, the first player-built route marker, the first functional dock/mooring infrastructure, the first physical map-room Chart Table, a styled vanilla Route Journal for handwritten route notes, localized command/stable-board UI labels, localized whistle/call feedback messages, localized animal-command actionbar feedback, localized hitching-post command/forget feedback, localized feed-trough feedback/status text, localized scout warning feedback, the first waterloggable Channel Marker buoy/channel infrastructure block, localized Whistle item tooltips, localized Grooming Brush fallback animal-inspection report lines, mounted-travel bond anti-farm gating, draft/cart work bond anti-farm gating, CRN teleport-packet cancellation, Channel Marker lantern/night-readability polish, lodestone-compass waypoint blocking, Trail Marker night-readability polish, Astikor draft-role localization, vanilla pack-role localization, vanilla/camel inspection text localization, Alex animal role-card localization, bond/mood/care status localization, stable-board status/proximity localization, animal stat-band localization, and animal/stable-board field-format localization: `de52339ad8038c3d9a7b6e5ebe6affb43322cf03823ba127d0fe669884bb4969`.
- Standalone `timeout 75s gradle runServer --no-daemon` loads Minecraft 1.21.1 + NeoForge 21.1.228 + Tenpack Travel 0.1.0 and reaches `Done`; exit code 124 is expected from the timeout after successful boot.
- Full-pack disposable production-server smoke test passes. Manual run: temporary NeoForge 21.1.228 server with actual `server/` payload (`76` jars, `18` config files, `3` datapacks) reached `Done (29.364s)`. Scripted run via `tools/smoke-full-server.py` also passes after the grooming/care presentation, food-pressure cleanup, Trail Marker addition, Mooring Post addition, Chart Table addition, Route Journal recipe/presentation layer, localized command/stable-board UI pass, localized whistle feedback pass, localized animal-command feedback pass, localized hitching-post feedback pass, localized feed-trough feedback pass, localized scout feedback pass, Channel Marker addition, Whistle tooltip localization, Grooming Brush fallback report localization, the no-GPS config policy pass, the recovery-compass datapack denial, mounted-travel bond anti-farm gating, draft/cart work bond anti-farm gating, cartographer/explorer/buried-treasure map preservation, Supplementaries compass coordinate-readout guardrail, and Create Railways Navigator governance config, the CRN teleport-packet cancellation pass, the Channel Marker lantern/night-readability pass, the lodestone-compass waypoint-blocking pass, the Trail Marker night-readability pass, the Astikor draft-role localization pass, the vanilla pack-role localization pass, the vanilla/camel inspection text localization pass, the Alex animal role-card localization pass, the bond/mood/care status localization pass, the stable-board status/proximity localization pass, the animal stat-band localization pass, the animal/stable-board field-format localization pass, Carry On container-blacklist datapack pass, Carry On modded-storage blacklist extension, LeashAll physical-lead guardrail, Supplementaries lunch-basket docs-policy pass, and Supplementaries sack-disable pass. The latest scripted run uses the actual server payload with JEI kept client-only (`76` server jars, `20` config files, `5` datapacks), reaches `Done (25.880s)` with clean `stop` handling (`run_exit: 0`), mentions the CraftTweaker food-pressure script, reports CraftTweaker error lines `0`, and reports Tenpack Travel mixin failure lines `0` after the vanilla structure-map preservation / push-clean validation pass. The tool uses isolated/offline smoke properties on port `25569` and avoids the Tenpack Travel dev run, which only loads local mod source.
- Full-pack smoke notes: Startup is not clean of log noise yet. Steam 'n' Rails emits non-fatal optional-compat loot-table errors for absent BYG/Nature's Spirit track items, Still Life can log far-chunk feature placement errors during spawn generation, and NeoForge rewrites comments/default formatting for Corpse/Create Diesel/LeashAll temp configs without changing the Tenpack-checked values. One immediate smoke run after the scout localization hit a transient Create advancement initializer crash on an unbound optional bucket (`create:empty_blaze_burner`/`create:chocolate_bucket` family) before datapack load; a no-JEI manual run and the subsequent scripted rerun reached `Done`, so the durable fix was to keep JEI out of `server/mods` and leave the smoke tool strict rather than masking repeated failures. A later first smoke after the Trail Marker night-readability pass hit the same Create advancement-initializer family on `create:schedule`, and the first smoke after the stable-board status/proximity localization pass hit it on `create:wrench`; both immediate reruns reached `Done`, and the stacks had no Tenpack Travel involvement.
- `tools/tenpack-build-public.py --out public` has been rerun; public manifests include the current Tenpack Travel hash, AstikorCarts Redux payload, and mirrored Astikor pull-policy configs.
- Pack guardrail scripts pass: mod catalog, pack config policy, Create progression invariants, Create addon recipe guardrails, client assets, and mod integrity. Pack config policy now also scans Tenpack Travel source for forbidden GPS/recall/exact-target-cue APIs such as target glow particles, entity glow outlines, teleport/recall calls, and exact BlockPos UI strings; checks that key Tenpack Travel infrastructure blocks have recipes plus loot tables; enforces fence-tag policy so Hitching/Mooring Posts keep physical lead-knot support while Trail Markers and Chart Tables cannot become hitching substitutes; verifies the Route Journal recipe stays a styled vanilla `minecraft:writable_book` with custom model data `1778601` plus no-GPS lore; verifies mounted-travel bond XP is not awarded directly from mount events and keeps sustained-displacement guards for both riding and draft/cart work; enforces Create Railways Navigator global setting permission gating, disabled admin mode, and packaged Tenpack Travel CRN teleport-packet cancellation so rail schedule navigation stays governed player-built infrastructure instead of a teleport/debug shortcut; and mod-integrity blocks obvious minimap/GPS compass bypasses. Create progression and mod-catalog/integrity policy now explicitly keep JEI client-only. Client asset policy also checks Tenpack Travel language/message keys used from Java, bans hard-coded `Component.literal` text inside Tenpack Travel `appendHoverText` tooltip methods and the Grooming Brush fallback animal-inspection report, checks blockstate/model presence, exact vanilla writable/written book model overrides for Route Journal presentation before/after signing, and item/block texture dimensions for key travel tools so presentation does not silently regress to placeholders.
- Active no-GPS config/datapack policy now closes several non-source navigation leaks: LSO hides F3 position/direction info and blocks filled-map coordinate output/compass info; Supplementaries globe coordinate output, compass right-click coordinate output, generated structure road signs/distance text, death-map markers, random adventurer structure maps, and pet-teleport flutes are disabled on both client and server; `tenpack-navigation-policy` overrides vanilla `minecraft:recovery_compass` crafting with a creative-only barrier ingredient so death recovery cannot become a craftable compass pointer; the same datapack disables survival lodestone crafting while Tenpack Travel blocks compass-on-lodestone binding so lodestone compasses cannot become persistent waypoint arrows; and the datapack preserves vanilla cartographer/explorer/village/trial plus buried-treasure structure-map destination tags as primitive physical maps. The checker also preserves dolphin-led exploration and Eyes of Ender from broad no-GPS datapack overrides because they are embodied/progression mechanics, not persistent map or compass target pointers. These decisions are enforced by `tools/check-pack-configs.py`. The same checker now also enforces `tenpack-carryon-policy` so Carry On's physical-object handling cannot regress into filled-container backpack cargo, including installed modded storage surfaces from Farmer's Delight, Supplementaries, and Create item vaults; it also keeps the Supplementaries lunch basket as the only pocket carrier by disabling the separate nine-slot general-storage sack in active configs and the reference override.
- The earlier standalone-server missing-tag warning for Alex's Mobs feed items was fixed by marking those optional in `data/tenpack_travel/tags/item/feed_trough_food.json`.
- The full-pack smoke also caught a real CraftTweaker food-pressure script failure: several Farmer's Delight ingredient/drink items in this version do not expose food data. Those entries are now deliberately left alone so the expedition-food pressure layer loads cleanly instead of failing at startup.
- Remaining validation: full-pack client join plus in-world playtest with Alex's Mobs/Astikor present: item/block/cart interactions, animal ownership/trust, whistle commands, hitching-board memory cleanup, and trough eating.

First v0.2 rescue increment after the quality complaint:

- Added a dedicated `Tenpack Travel` creative tab so the mod is visible as a real pack feature instead of being buried only inside vanilla tabs.
- Added `tenpack_travel:whistle` as a physical item with custom texture/model, tooltip, recipe, and use behavior. The server now requires this item for survival/adventure whistle calls and group commands; creative/spectator remain allowed for testing.
- Kept `grooming_brush`, `whistle`, `hitching_post`, and `feed_trough` in vanilla creative tabs as well for search/discoverability.
- Added `AnimalEligibility` as the central policy surface for inspectable/bondable/trough-eligible animals, so future any-mob taming and command-mode work does not keep scattering broad `Animal` checks through handlers.
- Added `AnimalCare`, a persistent care/mood layer. Grooming and trough feeding now record daily care, show mood/care state in the brush report, play animal-facing reactions (look/sound/particles), slightly heal/calm where appropriate, and let trough feed healthy-but-unfed working animals instead of only wounded ones.
- Replaced Grooming Brush chat-report spam with a client animal care screen opened by a server-to-client `animal_inspection` payload. The screen shows health, mood, care, a coarse bond bar, temperament, role, movement bands, notes, and creative/spectator debug details. Survival presentation deliberately avoids exact bond XP; the brush action-bar now surfaces existing care summary/mood so grooming feels like animal care instead of debug inspection.
- Added a Hitching Post stable-board screen opened by a server-to-client `hitching_post` payload. Empty-hand use now shows a camp/stable UI listing hitched animals with species, coarse proximity, health, mood, care, bond, and role instead of only printing a comma-separated chat line. The board deliberately avoids exact block coordinates/distances and still uses physical lead-knot tethering: no recall, no teleport.
- Added a craftable `Trail Marker` as the first dedicated player-built route infrastructure block. It is intentionally dumb and physical: cheap bulk road/dock/camp/crossing marker, no UI, no coordinates, no particles, no waypoint integration, no teleport, and not a fence/hitching substitute. It has block/item models, generated 16x16 block textures, a recipe, mineable data, and loot tables so travel infrastructure can be recovered instead of disappearing when roads are rebuilt. It now has a tiny lantern/reflective cap and very subtle local light level for night readability only, not a beacon, route arrow, live marker, or GPS cue.
- Added a craftable `Mooring Post` as the first functional dock/landing infrastructure block. It intentionally uses vanilla physical lead mechanics: Minecraft 1.21.1 boats are `Leashable`, and the post stays fence-tagged so leads create normal leash knots for boat tie-up. It has no custom capture scan, no recall, no coordinates, no remote storage, and no UI; it exists to make player-built docks/ferries/ports more useful before any ship mod is selected.
- Added a craftable `Channel Marker` as the first buoy/channel-marking infrastructure block. It is waterloggable and recoverable, but intentionally dumb: no beacon beam, no waypoint, no tracking, no recall, no inventory, and no fence/mooring tag. It has a subtle local lantern cap/light level so canals, harbor mouths, ferry lanes, shallows, and dangerous bends stay readable at night without becoming GPS or a lighthouse beam.
- Added a craftable `Chart Table` as the first physical map-room infrastructure block. It subclasses vanilla cartography-table behavior so players can copy, scale, and lock maps at camps/docks/road houses using existing map mechanics, but it deliberately adds no GPS dot, waypoint, route arrow, coordinate display, or remote tracking.
- Added a `Route Journal` recipe/presentation layer for handwritten route notes. It deliberately outputs a styled `minecraft:writable_book` with Tenpack custom model data instead of a custom book item, because vanilla 1.21.1 only persists book edits/signing for the exact writable-book item. This preserves reliable player-authored notes while avoiding coordinates, waypoints, automatic route logs, or a custom text-editor packet path.
- Localized the main animal command/stable-board presentation path: Animal Care screen fields/buttons, Whistle Commands labels/help text, Hitching Post stable-board labels, Feed Trough/Hitching Post tooltips, and the Grooming Brush fallback animal-inspection chat report now use `en_us.json` keys. This does not change mechanics; it keeps the player-facing travel UI from feeling like temporary debug text and lets the asset checker catch missing Tenpack Travel translation keys used from Java.
- Localized the Whistle item tooltip and the Whistle/Whistle Commands server feedback messages for missing whistle item, cooldown, no-answer/no-target cases, path failure, and command/call summaries. The client asset checker now includes `message.tenpack_travel.*` keys in its Java translatable-key scan and rejects hard-coded tooltip literals in `appendHoverText`, so future actionbar/chat/item-tooltip presentation does not silently fall back to raw keys or temporary English.
- Localized direct animal-command actionbar feedback for missing/too-far/low-trust/draft-cart-blocked/set-mode cases. The success message now passes the animal display component plus a translated mode label instead of flattening the animal name into an English string.
- Localized Hitching Post command/maintenance feedback for distance, unavailable post, unsupported mode, low-trust post commands, mode-set summaries, missing-memory cleanup, and fallback hitched-count messages. The sneak-use block path and packet button path now share the same post-mode summary helper, avoiding duplicate English grammar in two places.
- Localized Feed Trough actionbar/status feedback for full/empty/add/remove/no-state/status cases. Mechanics are unchanged: inserted feed still remains stored as physical item stacks, animals still walk over and pause before eating, and status text still reports coarse stored contents without GPS/automation behavior.
- Localized scout warning feedback while preserving the no-GPS scout design: the scout still requires bond XP, range, and line of sight; feedback remains a generic warning with sound plus note particles at the scout only, with no target names, target-side particles/glow, coordinates, direction arrow, or persistent marker.
- Localized Astikor draft-role labels shown in animal inspection/stable-board role text. Active draft animals still show only physical labor context such as the cart entity description, not coordinates, destination, inventory telemetry, or any recall/teleport affordance.
- Localized vanilla pack-role labels for llamas, donkeys, and mules so the same animal inspection/stable-board role path reports pack slots or chest-readiness through translation keys instead of hard-coded English. This remains cargo context only, not remote inventory or backpack behavior.
- Localized vanilla horse and camel inspection temperament/role/notes text for the same report path: camel passenger role, camel road-travel note, horse tamed/untamed note, and coarse taming temperament bands now use translation keys. This keeps inspection presentation player-facing and localizable without adding exact stats or GPS/cargo telemetry.
- Localized the curated Alex's Mobs role-card table by registry path. `AlexAnimalRole.java` now keeps coverage in code while `en_us.json` owns role/temperament/notes text; the asset checker verifies every curated Alex path has role, temperament, and notes keys. Seagull wording now stays coastal clue/flavor while vanilla buried-treasure maps remain allowed physical clues.
- Localized bond labels plus animal mood/care summaries shown in the Animal Care screen, Grooming Brush actionbar, and Hitching Post stable-board rows. This does not change bond/care pacing or exact-stat policy; it keeps coarse player-facing animal-care states localizable and checker-covered.
- Localized stable-board command mode labels, hitched/remembered row states, missing-memory fallback care text, remembered-animal age text, and coarse proximity bands. The proximity model remains deliberately coarse (`at post`, `near post`, `nearby`, `away`) with no coordinates, exact distances, direction arrows, map markers, recall, or teleport.
- Localized coarse animal stat bands for health state, max-health class, movement-speed class, and jump-strength class. Survival/stable-board presentation still uses bands only; exact decimals remain isolated to the existing creative/spectator debug line via `AnimalStatBands.rounded(...)`.
- Localized animal-care and stable-board field-format glue: not-available placeholders, health-summary separator, stable-board species/role separator, and creative/spectator debug grammar now use translation keys. This is presentation-only and does not expose exact stats to survival, coordinates, markers, or any recall behavior.
- Added the first real animal command-mode foundation: persistent `Follow`, `Stay`, `Roam`, and `Free` modes stored on animals under `tenpack_travel_command`; a server tick handler enforces follow/stay/roam physically with pathfinding/restrictions, never teleporting.
- Added brush-screen command buttons and a C2S `animal_command` payload. Commanding requires an existing bond/ownership unless in creative/spectator. The hitching-post board now displays each animal's command mode. Whistle now respects `Stay`/`Roam` instead of overriding them.
- Deliberately did **not** add fake `Work`/`Guard` buttons yet; those need real Astikor/Create Horse Power/companion-role integration instead of placeholder labels.
- Upgraded Feed Trough into a real block-entity camp object: it stores up to 12 actual feed item stacks, persists item IDs/components/counts to NBT, has `fill=0..3` blockstate visuals, supports empty/low/medium/full models, and shows stored-feed contents/status on empty-hand use. Legacy abstract `feed_units` NBT migrates to wheat rather than being lost.
- Feed inserted into the trough is stored as retrievable physical items instead of instantly disappearing into a hidden effect. Sneak empty-hand use removes one stored meal, and breaking the trough drops remaining feed. Nearby eligible animals walk toward the trough, stop/look at it for a short eating pause, then consume one stored item when hungry/hurt. Autonomous trough feeding updates animal care/mood and plays eating reactions, while player-filled trough feeding can still grow familiar trust once the animal actually eats.
- Upgraded Hitching Post into a real block-entity stable anchor. It now persists post mode (`Stay` or `Roam`) plus remembered animal UUID/name/species/last-seen/care/command state, refreshes from physical lead knots, and applies anchored Tenpack command modes to currently hitched animals without teleporting.
- Hitching Post stable-board now shows post mode/radius and remembers animals last seen at the post even if they are not currently loaded/visible. Currently hitched animals are shown as current rows; stale memory rows stay last-seen-only instead of becoming a pseudo tracker when the animal is loaded elsewhere. Sneak-use toggles Stay/Roam, and the stable-board has Set Stay / Set Roam buttons backed by a C2S `hitching_post_command` packet plus a `Forget Missing` board-maintenance button backed by `hitching_post_forget` to clear memories for animals no longer physically hitched. Post mode changes only apply to animals the player can command; animals owned by another player are left unchanged and a stranger cannot flip a post containing only animals that do not trust them.
- Added a local Whistle Commands screen. Tapping the whistle key calls nearby animals only if the player has a physical travel whistle; holding it opens a command UI. Sneak-using the physical whistle item also opens this UI.
- Added C2S `whistle_command` payload for local group commands: `Call`, `Follow`, `Stay`, `Roam`, and `Free`. It requires a physical whistle in survival/adventure, has server-side cooldown/rate limiting, targets nearby familiar unhitched animals only, validates command trust server-side, rejects animals owned by another player, excludes hitched/draft animals so stable posts and Astikor carts are respected, and uses normal pathing/restriction behavior with no teleport.
- Added `astikorcartsredux-1.2.2.jar` to client/server as the first real cart/wagon travel integration candidate; documented it in the installed-mod catalog and kept it marked as optional uncommitted travel WIP until full in-game testing passes.
- Added `AstikorIntegration`, a central soft reflection bridge for AstikorCarts Redux (`AbstractDrawnEntity` + `getPulling()`), plus `AstikorDraftWorkHandler`. When a real Astikor drawn entity is moving and is pulled by a bondable animal controlled by a player, Tenpack records draft work in `AnimalCare`, awards slow draft-work bond XP through `AnimalBond.draftWork`, and updates brush/stable mood text (`Worked`/`Tired`, "worked today; needs camp care"). Whistle call/group commands now ignore active draft animals, and direct non-`Free` commands tell the player to detach the cart first. This does not add a fake `Work` command yet and does not make Tenpack Travel compile-depend on Astikor.
- Updated vanilla horse inspection role from `None` to `Mount / draft candidate` so the brush UI reflects the cart integration direction without pretending every animal has finished work behavior.
- This is still a v0.2 foundation pass; it does not replace the planned work for brush UI polish, hitching post UI polish/rope visuals, fuller animal command modes, whistle UI polish/radial art, tracking, Astikor/Create Horse Power behavior integration, and deeper companion behavior.

## Phase 1 — bonding foundation

Status: implemented locally in source. Needs full-pack in-game persistence/playtest.

Design direction:

- Bonding follows the Red Dead model: trust grows from actually using and caring for the animal.
- The Grooming Brush is an inspection/care tool, not a claim item.
- No Stable Ledger is planned; the system should stay embodied in riding, saddling, brushing, feeding, calling, and animal behavior.
- Ownership is lightweight/social: vanilla/modded owner is respected, and saddling a horse-like mount can mark a Tenpack primary owner, but other players can still use the animal and build their own lower bond.
- Bond is per-player trust on the animal, not a single exclusive handler lock.

Scope implemented:

- Code-quality pass split animal inspection out of the main mod class:
  - `TenpackTravel.java` now handles registration, custom creative tab wiring, client/network registration, and common event handler wiring; brush and mount hooks are split into dedicated handlers.
  - `AnimalInspectionReport.java` builds and formats vanilla/Alex animal reports.
  - `AlexAnimalRole.java` contains the Alex's Mobs role table.
  - `AnimalBond.java` owns bond persistence/rate limits.
- `AnimalBond` stores persistent relationship data on the animal under `tenpack_travel_bond`.
- Bond data currently tracks:
  - optional `primary_owner` UUID,
  - per-player bond entries under `players`,
  - per-player bond XP,
  - per-player last brush day,
  - per-player last feed day,
  - per-player last riding XP tick/position,
  - per-player last draft-work XP tick/position.
- Grooming Brush shows a banded `Bond:` line:
  - `Unbonded`,
  - `New bond`,
  - `Familiar`,
  - `Trusted`,
  - `Loyal`,
  - `Companion`.
- Mounting a bondable animal establishes that player's relationship baseline and can mark a saddled horse-like primary owner, but mounting alone does not award bond XP.
- Continued mounted travel strengthens that player's bond on a cooldown, but only after the animal has sustained meaningful horizontal displacement from the last riding baseline/award position. This prevents remount spam, idle sitting, and tiny pen loops from becoming the best bond strategy.
- Draft/cart work strengthens that player's bond on a slower cooldown, but only after sustained meaningful hauling displacement from the last draft baseline/award position. Astikor's per-tick movement check is only a cheap moving-now prefilter, not the anti-farm rule.
- Saddled horse-like mounts can mark the first rider as Tenpack primary owner if no vanilla/modded owner already exists.
- Brushing only gives care XP when the player already has a bond or is the animal's owner; it no longer claims random animals.
- Feed Trough healing only gives care XP when the player already has a bond or is the animal's owner.
- Creative/spectator debug output includes that viewer's bond XP.

Design constraints preserved:

- Phase 1 itself defines bond; whistle and active role responses are handled by Phase 2 below.
- Eagle/scout pings are brief, role-based, and non-coordinate; no persistent HUD tracker.
- No teleport/recall.
- No stat buffs from bonding.
- No Stable Ledger.
- Bonding is a foundation for future reliability/handling, not a power grind.

Acceptance tests still needed in the full pack:

1. Mount a horse/mule/elephant/kangaroo where possible and confirm mounting alone does not immediately improve `Bond:`.
2. Keep riding/moving meaningful route distance and confirm `Bond:` can change from `Unbonded` to `New bond` on the riding cooldown, while standing still, remount spam, and tiny loops do not farm riding XP.
3. Pull an Astikor cart over meaningful route distance and confirm draft work can deepen bond slowly; confirm stationary cart jitter, wall-bumping, tiny pen loops, and short back-and-forth wiggles do not farm draft XP.
4. Brush before any relationship and confirm it does not claim/start bond by itself.
5. Brush after riding/owning and confirm care XP can improve bond once per in-game day.
6. Feed/heal through a trough after relationship exists and confirm bond can improve once per day.
7. Saddle a horse-like mount, ride it, restart server, and confirm primary owner/bond data persists.
8. Confirm a different player can still use the animal but has their own lower/no bond.

## Phase 2 — whistle keybind, active animal roles, and lead handling

Status: implemented locally in source/config after Phase 1. Needs full-pack in-game playtest.

Existing-mod search:

- Horse whistle/call search found `Call Your Horse`, but it teleports horses to the player, so it was rejected for Tenpack.
- Lead/leash search found `LeashAll` for NeoForge 1.21.1. It is MIT licensed, server-required/client-optional, configurable, and keeps animals physical, so it was adopted instead of custom lead mechanics for now.

Whistle behavior implemented in Tenpack Travel:

- Keybind: `Whistle` under the `Tenpack Travel` controls category, default key `H`.
- Client sends a small server packet; server owns all response logic.
- Survival/adventure players must carry `tenpack_travel:whistle`; creative/spectator can still test without one.
- No teleport, no coordinates, no GPS arrow.
- Same-dimension / loaded-area only because it searches nearby loaded entities.
- Requires at least `Familiar` bond (`20` XP) before an animal will answer.
- Range scales by that player's bond with that animal:
  - `Familiar`: 20 blocks,
  - `Trusted`: 32 blocks,
  - `Loyal`: 44 blocks,
  - `Companion`: 56 blocks.
- Up to three nearby bonded animals respond.
- Response uses pathfinding toward the player; if no path is found, the animal is not teleported.
- A short note particle/glow ping marks responders briefly without coordinates or persistent tracking.
- Player whistle has a short server-side cooldown.

Active animal-role foundation implemented in Tenpack Travel:

- Added a small `AnimalRoles`/`AnimalRoleActions` layer so special animal jobs are explicit instead of hardwired into the brush report.
- First active role: `SCOUT` for `alexsmobs:bald_eagle`.
- A bonded Familiar+ scout eagle that answers a whistle scans from the eagle's own position, not the player's.
- The scout only pings living hostile/danger targets it has direct line of sight to.
- Pings are short-lived note particles at the scout sent only to the whistling player; no coordinates, no text target names, no permanent marker, no target-side particles/glow, and no target glow effect through walls.
- Successful scout warnings can add a tiny role-use bond XP on a cooldown, reinforcing actual use.

Lead handling implemented via LeashAll:

- Added `leashall-neoforge-1.21.1-1.3.1-1.21.1.jar` to both client and server mods.
- Added pack default config at `server/defaultconfigs/leashall-server.toml` and mirrored to client defaults.
- `tools/check-pack-configs.py` now enforces the mirrored default config so this stays physical caravan handling instead of a broad mob-control exploit.
- Config stance:
  - expanded physical leash handling,
  - max 6 leashes per player,
  - bosses/raiders/warden/dragon/wither/elder guardian blocked,
  - villagers/wandering traders blocked,
  - players blocked.

Acceptance tests still needed in the full pack:

1. Confirm keybind appears in Controls as `Tenpack Travel` → `Whistle`.
2. Confirm pressing whistle with no physical whistle item in survival gives the "need a travel whistle" feedback and does not call/command animals.
3. Confirm pressing whistle with no Familiar+ bonded animal gives only local feedback and no error once the player has the item.
4. Build bond to Familiar, whistle nearby, and confirm the animal pathfinds toward the player without teleporting.
5. Confirm out-of-range or blocked-path animals do not teleport.
6. Confirm a Familiar+ bonded bald eagle can give a short danger ping only for hostiles it can see.
7. Confirm eagle/scout pings do not show coordinates, target names, persistent markers, or through-wall target glow.
8. Confirm LeashAll config loads on the server and expanded leads work on intended animals.
9. Confirm blocked entities cannot be leashed.

## Concrete first non-code ticket

Status: implemented as source text in `notes/travel-questbook-guide.md`.

Guide/quest text now covers:

- First Roads,
- Stablecraft,
- Working Animals,
- Dangerous Roads,
- Other Worlds,
- Freight Future.

This makes existing Alex's Mobs systems and the new Tenpack Travel items/blocks discoverable without changing mechanics.

## Open design questions

1. Should the Animal Dictionary stay granted on first join, or should it be craft-found? Current config says grant on startup.
2. Should Grooming Brush inspect only owned/tamed animals, or all animals? Recommendation: all non-hostile mounts; care/bond progression requires actual use or ownership/trust.
3. Should catfish bucket cargo be left alone? Recommendation: yes until observed abuse.
4. How much animal recall is acceptable? Recommendation: nearby call / stable return only, no global teleport.
5. Should carts/wagons be custom? Recommendation: likely yes later unless a good 1.21.1 NeoForge option appears.
6. Should weather affect travel tack? Defer to partner because storms/weather gameplay are partner-owned.

## Immediate recommendation

The first two implementation units are now locally implemented:

- **Grooming Brush v1** for deliberate no-Jade animal inspection.
- **Hitching Post + Feed Trough v1** for physical animal parking and camp care.

They directly address the user's concern:

- Horse/mule/animal info is available through an item/tool.
- The output avoids Jade-style passive overlays and raw survival math.
- Mules/horses/breeding become more usable without backpacks.
- Animal handling becomes less bullshit without recall, GPS, or teleport safety.
- Camps and roads become practical support for real animal logistics.

Next useful step: **full-pack in-game playtest of Red Dead-style bonding, whistle response, and LeashAll lead rules**. After that, pick one deferred system:

1. **More animal roles** after playtest: crow camp logistics, kangaroo pouch support, elephant freight cues, sugar glider safety, etc.
2. **Eagle/glove deep integration** if whistle-triggered scout pings feel too broad or not falconry-specific enough.
3. **LeashAll tuning** after multiplayer testing if the default blocklist is too loose/tight.
4. **Feed Trough playtest/tuning** for range, wait time, eligible species, and animation/sound polish; keep stored feed physical and still no automation/breeding.

Vehicle/infrastructure source audit result:

- AstikorCarts Redux is the ranked land/animal travel candidate: NeoForge 1.21.1, MIT, supply carts, animal carts, hand carts, plows, seed drills, reapers, cart sounds/assets, physical puller attachment, configurable pull entities. Integrate with Tenpack roles/hitching after playtest.
- Create Horse Power is useful animal-labor infrastructure, not travel progression: keep as optional stationary Create power style and tune animal-work care/fatigue later.
- Vertical travel should prefer physical ropes/ladders/grapples/climbing tools over universal wall-climb magic. Candidate order from audit: Climbable Ropes for Create Aeronautics if compatible, then carefully balanced Yori3o grappling hooks; Better Climbing is ladder QoL only.
- Let's Do Camping should not ship as-is. It requires private hard-disable/patch for backpacks/Enderbag/portable storage if used; `tools/check-mod-integrity.py` now blocks Let's Do Camping plus common backpack/Enderbag-style portable-storage mods so camp decor cannot slide in as a cargo bypass.
- Carry On remains allowed only as embodied physical carrying, not as portable cargo. `tenpack-carryon-policy` extends Carry On's block blacklist for core storage/container blocks (chests, barrels, shulker boxes, hoppers, droppers/dispensers, furnaces/smokers/blast furnaces, crafters, brewing stands, ender chests), plus installed modded storage surfaces such as Farmer's Delight cabinets/baskets, Supplementaries safes/sacks, and Create item vaults. `tools/check-pack-configs.py` enforces the datapack so players cannot move filled container blocks as a backpack substitute.
- Scout-style pouch, satchel, and bundle-expansion mods are now blocked/deferred by `tools/check-mod-integrity.py`. Vanilla bundles and a future deliberately capped Tenpack-native pouch remain possible, but drop-in pouch/bundle mods cannot casually become backpack-tier cargo.
- No polished 1.21.1 NeoForge Create-compatible ship replacement was found; do not add Small Ships/item ships as the default. Defer water vehicles until a Create/Aeronautics-compatible strategy is solid. `tools/check-mod-integrity.py` now enforces this by blocking Small Ships, Eureka/Valkyrien-style moving-ship stacks, and unaudited boat utility mods such as Move Boats / Boat Item View / Boat Break Fix unless the policy is deliberately revisited.
