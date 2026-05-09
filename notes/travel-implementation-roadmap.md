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

Status: first-pass block implemented locally in `tenpack_travel-0.1.0.jar`.

Function:

- A camp/stable block that consumes one real feed item to heal nearby working animals.
- Works in a small radius around the trough.
- Heals up to three wounded eligible animals per feed item.
- Eligible animals: tamed horses/donkeys/mules/llamas, tamed Alex's Mobs pets/mounts, camels, and leashed animals.
- Does not breed automatically.
- Has no inventory UI yet; feed is deliberately consumed through interaction.

Rules:

- Consumes real food.
- Does not make animals immortal.
- Works only in a small radius.
- No automation, no recall, no teleport safety.

Recipe:

- oak slabs + hay block.

Deferred:

- True stored feed inventory.
- Better per-species feed validation.
- Visual fill levels.

Acceptance criteria:

- Players can park a mule/horse/elephant at a camp and reasonably expect it to stay there.
- The infrastructure feels physical and buildable.
- No teleporting convenience loop is introduced.

### Milestone 4 — road/camp gameplay support

Purpose: make roads and camps mechanically useful before they become aesthetic.

#### Road Sign Pack

Current narrow sign mod:

- Click Signs is already selected for clickable signs.

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

#### Camp Kit

Potential crafted kit or guide objective, not necessarily a new item:

Minimum camp:

- bed/sleeping setup,
- campfire,
- chest/barrel/cache,
- hitching post,
- feed trough,
- sign,
- map/atlas table or cartography table.

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

- Prior mod search did not find a solid ready-made 1.21.1 NeoForge horse cart/stagecoach option.
- This may need custom implementation later.

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
| Seagull | treasure-map clue behavior | physical clue/navigation flavor | keep stealing unless too much |

## Questbook / guide chapter draft

If/when Tenpack has a questbook, travel can be a practical chapter.

### Chapter: First Roads

1. **A Map That Is Yours**
   - Make a map/atlas.
   - Text: “Tenpack does not hand you a destination arrow. Your first map is a memory you can hold.”
2. **Leave a Sign**
   - Place a sign/click sign.
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

Validation status as of 2026-05-09:

- `gradle build --no-daemon` passes in `mods-src/tenpack-travel`.
- Built jar hash copied to both client and server after active animal-role/scout eagle work: `6e498ed4ef56adcf436114d3cec1ae0640a8e549a0d51d4525d1834ea36865e5`.
- Standalone `gradle runServer --no-daemon` loads Minecraft 1.21.1 + NeoForge 21.1.228 + Tenpack Travel 0.1.0 and reaches `Done`.
- The earlier standalone-server missing-tag warning for Alex's Mobs feed items was fixed by marking those optional in `data/tenpack_travel/tags/item/feed_trough_food.json`.
- Remaining validation: boot the actual full Tenpack client/server with Alex's Mobs and test item/block interactions in world.

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
  - `TenpackTravel.java` now handles registration, creative tabs, the brush interaction event, and the mount-use hook.
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
  - per-player last riding XP tick.
- Grooming Brush shows a banded `Bond:` line:
  - `Unbonded`,
  - `New bond`,
  - `Familiar`,
  - `Trusted`,
  - `Loyal`,
  - `Companion`.
- Mounting a bondable animal starts that player's bond.
- Continued mounted travel strengthens that player's bond on a cooldown, but only while the animal is actually moving horizontally enough to count as travel.
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

1. Mount and ride a horse/mule/elephant/kangaroo where possible and confirm `Bond:` changes from `Unbonded` to `New bond`.
2. Keep riding/moving and confirm bond XP can rise on the riding cooldown, while standing still does not farm riding XP.
3. Brush before any relationship and confirm it does not claim/start bond by itself.
4. Brush after riding/owning and confirm care XP can improve bond once per in-game day.
5. Feed/heal through a trough after relationship exists and confirm bond can improve once per day.
6. Saddle a horse-like mount, ride it, restart server, and confirm primary owner/bond data persists.
7. Confirm a different player can still use the animal but has their own lower/no bond.

## Phase 2 — whistle keybind, active animal roles, and lead handling

Status: implemented locally in source/config after Phase 1. Needs full-pack in-game playtest.

Existing-mod search:

- Horse whistle/call search found `Call Your Horse`, but it teleports horses to the player, so it was rejected for Tenpack.
- Lead/leash search found `LeashAll` for NeoForge 1.21.1. It is MIT licensed, server-required/client-optional, configurable, and keeps animals physical, so it was adopted instead of custom lead mechanics for now.

Whistle behavior implemented in Tenpack Travel:

- Keybind: `Whistle` under the `Tenpack Travel` controls category, default key `H`.
- Client sends a small server packet; server owns all response logic.
- No item is required.
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
- Pings are short-lived note/glow particles sent only to the whistling player; no coordinates, no text target names, no permanent marker, and no target glow effect through walls.
- Successful scout warnings can add a tiny role-use bond XP on a cooldown, reinforcing actual use.

Lead handling implemented via LeashAll:

- Added `leashall-neoforge-1.21.1-1.3.1-1.21.1.jar` to both client and server mods.
- Added pack default config at `server/defaultconfigs/leashall-server.toml` and mirrored to client defaults.
- Config stance:
  - expanded physical leash handling,
  - max 6 leashes per player,
  - bosses/raiders/warden/dragon/wither/elder guardian blocked,
  - villagers/wandering traders blocked,
  - players blocked.

Acceptance tests still needed in the full pack:

1. Confirm keybind appears in Controls as `Tenpack Travel` → `Whistle`.
2. Confirm pressing whistle with no Familiar+ bonded animal gives only local feedback and no error.
3. Build bond to Familiar, whistle nearby, and confirm the animal pathfinds toward the player without teleporting.
4. Confirm out-of-range or blocked-path animals do not teleport.
5. Confirm a Familiar+ bonded bald eagle can give a short danger ping only for hostiles it can see.
6. Confirm eagle/scout pings do not show coordinates, target names, persistent markers, or through-wall target glow.
7. Confirm LeashAll config loads on the server and expanded leads work on intended animals.
8. Confirm blocked entities cannot be leashed.

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
4. **Feed Trough v2** with stored feed and visual fill levels, still no automation/breeding.
