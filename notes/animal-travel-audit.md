# Alex's Mobs travel / logistics audit

Date: 2026-05-09
Pack target: NeoForge 1.21.1
Installed jars audited:

- `client/mods/alexsmobs-1.22.17.jar`
- `client/mods/citadel-1.21.1-2.7.6.jar`

Method:

- Read the Alex's Mobs jar language file and Animal Dictionary text.
- Inspected class signatures / bytecode for tameable, ridable, inventory, saddle, passenger, and command behavior.
- Checked item tags for tame / breed / food inputs on important animals.
- Checked Tenpack tree for live config state.

## Tenpack design constraints

- No target-finder / GPS navigation loops. Navigation should remain personal: limited maps, roads, signs, landmarks, animals, camps, and player knowledge.
- Do not touch storms/weather gameplay, worldgen, or named-region mechanics here. Those belong to the partner.
- Do not pull in Jade-style passive entity stat overlays. They do not match the philosophy.
- We still want friction and discomfort. The goal is **not** to pretend that a mule is better than a backpack. The goal is to make the coolest lower-friction option be the natural one: a mule, a better mule, better tack, an elephant, a caravan, a stable network, etc.
- No backpacks as a bypass for travel logistics.
- Horse/mount stats should probably be revealed through an item or deliberate interaction, not an always-on overlay.

## Current config state

Important: Alex's Mobs config is currently present under `tenpack-specs/overrides/config/`, but I did **not** find an active `client/config/alexsmobs-common.toml` or `server/config/alexsmobs-common.toml` in the live pack tree.

That means Tenpack is likely running Alex's Mobs defaults unless something else copies those overrides in a path I missed. If we want deterministic balance, add active configs to both client and server.

Relevant default/common settings found in the spec override:

- `giveBookOnStartup = true` for the Animal Dictionary.
- `raccoonStealFromChests = true`.
- `crowsStealCrops = true`.
- `seagullStealing = true`.
- `limitGusterSpawnsToWeather = true` — weather-adjacent; do not adjust here without partner involvement.
- `straddleboardEnchants = true`.
- `falconryTeleportsBack = false`.
- `tusklinShoesBarteringChance = 0.025`.
- `addLootToChests = true`.
- `mungusBiomeTransformationType = 2` — worldgen/biome mutation; defer to partner/worldgen owner.

## Strong travel / logistics candidates

### Elephant — primary heavy cargo animal

Audit result: **excellent fit**.

Detected behavior:

- Tameable.
- Ridable / controllable.
- Chested inventory.
- Huge `SimpleContainer`: 54 slots.
- Carpet decoration.
- Trader elephant variant.
- Tusked elephant progression: wild tusked adults cannot be tamed, but a tamed calf can become tusked later.
- Tamed tusked elephants can charge when fed wheat while ridden.
- Bytecode attributes observed: 85 HP, 32 follow range, 10 attack damage, 0.35 movement speed, very high knockback resistance.

Taming / food tags:

- Tame / breed: `alexsmobs:acacia_blossom`.
- General food includes leaves, saplings, sugar cane, apple, beets, potatoes, carrots, hay blocks, bamboo, bananas, melon, berries, acacia blossom.

Tenpack role:

- Late-early / midgame freight breakthrough.
- Not a backpack replacement in the pocket; a project that requires animal care, route planning, road width, stables, and risk management.
- Best for base moves, expeditions, and trade convoys.

Recommendation:

- Keep elephants valuable and a little awkward: large, pathing-sensitive, hard to tame/breed, needs road/stable infrastructure.
- Do not nerf cargo too hard. The inconvenience is already spatial and logistical. Their 54 slots are what makes them worth planning around.

### Kangaroo — early/mid personal companion + light pouch

Audit result: **good fit as a small pack/fighter companion, not as main freight**.

Detected behavior:

- Tameable.
- Commanded stay/follow/wander.
- Pouch inventory: 9 slots.
- Can use a melee weapon placed in pouch.
- Adult can equip helmet/chestplate from pouch.
- Eats non-meat food from pouch to heal.
- Bytecode attributes observed: 22 HP, 32 follow range, 4 attack damage, 0.5 movement speed.

Taming / food tags:

- Tame: carrots.
- Breed: dead bush / short grass.

Tenpack role:

- Good early animal-care tutorial: carrot taming, 9-slot pouch, simple gear, self-healing food.
- Better as a “travelling partner” than a cargo solution.

Recommendation:

- Keep useful but not frictionless. It should make a small expedition nicer, not solve logistics.

### Grizzly Bear — war mount / dangerous prestige mount

Audit result: **good, but combat-focused**.

Detected behavior:

- Tameable after honey/salmon interaction.
- Ridable / controllable; described as war mount.
- Command/follower behavior detected.
- Bytecode attributes observed: 55 HP, 8 attack damage, 0.25 movement speed, strong knockback resistance.

Taming / food tags:

- Honey interaction: honeycomb, honey block, honeycomb block, honey bottle.
- Tame / breed: salmon.

Tenpack role:

- A high-risk/high-reward mount for dangerous forests/frontiers.
- Should not beat horses for convenient road travel.
- Good for “I prepared for this expedition” friction.

Recommendation:

- Leave as combat mount. Do not make it the general best transport.

### Komodo Dragon — hostile-to-tamed savage mount

Audit result: **strong hostile-region mount**.

Detected behavior:

- Tameable with many stacks of rotten flesh.
- Saddled mount.
- Stay/follow/wander commands.
- Surprisingly quick per Animal Dictionary, though bytecode base movement speed is 0.23.
- Bytecode attributes observed: 30 HP, 4 attack damage, 0.23 movement speed.

Taming / food tags:

- Tame / breed: rotten flesh.

Tenpack role:

- A friction mount: expensive, gross, dangerous to acquire, but memorable.
- Good for jungle/sparse jungle and combat travel.

Recommendation:

- Keep as specialty predator mount. It is not a mule/backpack answer; it is a trophy with utility.

### Laviathan — Nether group/lava travel mount

Audit result: **excellent dimension-specific group transport**.

Detected behavior:

- Ridable / controllable with both Straddlite Saddle and Straddlite Tack.
- Up to four passengers.
- Lava-lake transport; less likely to flee/submerge when fully equipped.
- Bytecode attributes observed: 60 HP, 10 armor, 0.3 movement speed.

Equipment / food:

- Requires `alexsmobs:straddle_saddle` and `alexsmobs:straddle_helmet` / Straddlite Tack.
- Breed: mosquito larva.
- Heal: magma cream.

Tenpack role:

- Natural answer to Nether distance without waystones/GPS.
- Group travel is the important unique feature.

Recommendation:

- Keep equipment cost meaningful. This is perfect “friction with payoff.”

### Straddleboard — risky fast Nether lava board

Audit result: **excellent danger-sport travel tool**.

Detected behavior:

- Ridable entity item.
- Floats on lava, sinks in water.
- Faster/riskier than safe strider-style travel.
- Can snap/tumble if hitting a wall/ceiling too hard.
- Enchants enabled in common config.

Tenpack role:

- Solo Nether scouting / emergency travel.
- Friction is built in: risk of crashing and dying in lava.

Recommendation:

- Keep. It fits the pack better than frictionless teleports.

### Endergrade — slow End vertical mount

Audit result: **niche but good**.

Detected behavior:

- Saddled and ridden like a pig.
- Controlled with `alexsmobs:chorus_on_a_stick`.
- Useful for shulker evasion and height changes.
- Slow.
- Bytecode attributes observed: 20 HP, 2 attack damage, 0.15 movement speed.

Tenpack role:

- End exploration comfort without deleting danger.

Recommendation:

- Keep slow. Its value is safe verticality, not speed.

### Tusklin — dangerous bucking battle mount

Audit result: **good friction mount**.

Detected behavior:

- Hostile.
- Can be saddled and ridden temporarily before bucking rider off.
- Pigshoes / Ancient Hogshoes improve ride and can be enchanted.
- Bytecode attributes observed: 40 HP, 9 attack damage, 0.3 movement speed, very high knockback resistance.

Food / gear:

- Pacify: brown mushroom.
- Breed: red mushroom.
- Gear: saddle and pigshoes.
- Pigshoes bartering chance in config: 2.5%.

Tenpack role:

- Cold-biome prestige mount / battle hog.
- Great for “better animal / better gear” progression.

Recommendation:

- Keep bucking behavior. Make pigshoes/tack acquisition legible through quests/recipes/signposting if needed.

### Spectre — End void tow

Audit result: **highly relevant but dimension-specific**.

Detected behavior:

- Passive End void creature.
- Follows player holding Soul Heart.
- Player can attach to it with a lead and be pulled across the void.
- Sneak/interact to detach.

Tenpack role:

- A fantastic alternative to coordinates/teleports for End island discovery.
- It is risky, physical, and memorable.

Recommendation:

- Keep as-is unless it trivializes End progression. It fits the philosophy.

### Cosmaw — End fall-rescue companion

Audit result: **excellent safety companion, not cargo**.

Detected behavior:

- Tameable with Cosmic Cod.
- Follower that does not normally fight.
- Rescues owner who falls off End islands, carrying them back to land.
- Bytecode detected tame/command/rider-like control hooks, but Animal Dictionary frames it as rescue behavior rather than a normal mount.

Food:

- Tame / breed: Cosmic Cod.
- Heal: Chorus Fruit.

Tenpack role:

- End exploration insurance.
- Strong support for “go prepare for the journey” gameplay.

Recommendation:

- Keep. This is exactly the kind of non-GPS navigation support Tenpack wants.

### Catfish — weird bucketable cargo

Audit result: **novel logistics, potentially exploitable but interesting**.

Detected behavior:

- Small catfish can store 3 item stacks.
- Medium catfish can store 9 item stacks.
- Large catfish can store one small mob.
- Catfish preserve belly contents when bucketed.
- Sea pickles or injury make them regurgitate contents.

Tenpack role:

- Aquatic “smuggler cargo” or novelty transport.
- Not reliable enough to replace pack animals, but could be a fun hidden trick.

Recommendation:

- Keep. Watch for exploitiness if players bucket many 9-stack catfish; still has enough weird friction to be okay unless it becomes dominant.

## Useful support animals / navigation-adjacent tools

### Bald Eagle

- Tame with fish oil.
- Falconry glove carries/launches it.
- Falconry hood enables direct piloting up to about 150 blocks before returning.
- Best role: scouting and target marking by player skill, not GPS.
- Recommendation: very good fit. Consider it a deliberate scouting item chain, not a passive minimap.

### Crow

- Tame with pumpkin seeds.
- Commands: sit, wander, follow, gather items.
- Hay-block home/healing behavior.
- Can deposit items into framed containers.
- Best role: base/camp item gathering automation, not travel cargo.
- Warning: wild crows stealing crops is enabled in the spec config.

### Sugar Glider

- Tame with sweet berries.
- Can ride on owner’s head and gives slow falling.
- Forages leaves for small items.
- Best role: early vertical safety / cliff travel comfort.

### Capuchin Monkey

- Tame with bananas.
- Shoulder companion.
- Uses ranged/melee attacks; can throw stones.
- Best role: jungle companion / light defense.

### Caiman

- Tamed by imprinting from hatched eggs.
- Defensive grappler that holds targets still.
- Best role: camp/base defense near water, not travel.

### Mantis Shrimp

- Tame with tropical fish.
- Commands include break blocks.
- Can be kept out of water with water bucket interaction.
- Best role: utility companion / block-breaking novelty.
- Caution: block-breaking pets need griefing/server-rule review.

### Mimic Octopus

- Tame with lobster tails when not camouflaged.
- Can follow owner onto land but dries out.
- Scare/defense companion.
- Best role: aquatic expedition companion; discomfort is built in.

### Mudskipper

- Tame with lobster tails.
- Stay/follow/wander.
- Bucketable.
- Best role: small amphibious pet, not travel.

### Warped Toad

- Tame/breed with mosquito larva.
- Nether insect defense; skilled in lava and water.
- Best role: Nether anti-bug companion.

### Raccoon

- Tame with egg near water.
- Stay/follow/wander, carpet bandana.
- Wild raccoons steal from chests if config active.
- Drops raccoon tail for Frontiersman’s Cap: +0.1 speed while sneaking.
- Best role: friction/noise animal plus minor travel gear.

### Tarantula Hawk

- Tame with 15–25 spider eyes.
- Strong anti-arthropod defense.
- Drops wings for Tarantula Hawk Elytra upgrade.
- Best role: desert/combat companion and late mobility crafting path.

## Complete mob-by-mob audit

Legend:

- **Core travel/logistics**: directly usable for movement, cargo, group travel, or expedition safety.
- **Support**: useful for scouting, defense, camp/base logistics, or travel gear.
- **Ambient/resource/hazard**: keep for ecology/resources/danger, but not a main travel-system actor.
- **Defer**: touches weather/worldgen/biome mutation or needs partner/design review.

| Mob | Audit role | Travel/logistics read | Tenpack recommendation |
| --- | --- | --- | --- |
| Alligator Snapping Turtle | Ambient/resource | Spiked shell/armor resource; no mount/cargo. | Keep as ecology/resource. |
| Anaconda | Support | Its ecosystem points to Vine Lasso: captures/transports mobs including hostiles, but cannot tie to fences like a lead. | Strong handling tool for animal logistics; keep friction. |
| Anteater | Ambient/resource | Anthill digging, baby rides parent; no player transport. | Keep. |
| Bald Eagle | Support / scout | Falconry glove + hood allow carried eagle and short-range piloted scouting. | Use as deliberate scouting; good anti-Jade/anti-minimap info tool. |
| Banana Slug | Support / infrastructure | Slime/mucus can drain water. | Possible road/harbor/camp construction utility; not travel animal. |
| Bison | Support / travel comfort | Bison fur insulates boots from powdered snow. | Useful cold-travel resource; not a mount. |
| Blobfish | Ambient/resource | Pressure gimmick; bucket/fish resource. | Keep ambient. |
| Blue Jay | Support | Temporary ally/follower with glow berries; not tame/cargo. | Minor navigation flavor; not core. |
| Bone Serpent | Hazard/resource | Nether threat; resource drops. | Keep as travel danger. |
| Bunfungus | Support/combat | Transformed rabbit; strong combat, not transport. | Keep as weird combat pet/hazard. |
| Cachalot Whale | Support/hazard | Breaks boats/wood when charging; ambergris for Echolocator/fuel/trade. | Ocean travel hazard and resource; not mount. |
| Caiman | Support/defense | Hatchling imprint tame; grabs/holds enemies. | Camp/water defense. |
| Capuchin Monkey | Support/defense | Tame, shoulder, ranged/melee attacks. | Companion, not logistics. |
| Catfish | Core logistics, niche | Small/medium store 3/9 stacks; large stores one small mob; belly preserved in bucket. | Keep as weird bucket cargo; monitor exploitiness. |
| Cave Centipede | Support / gear | Drops legs for wall-climbing leggings. | Vertical expedition gear, not animal logistics. |
| Cockroach | Ambient/friction | Eats dropped food; maggots for breeding other creatures. | Keep as cave/camp nuisance. |
| Comb Jelly | Ambient/resource | Bucket/rainbow jelly; no logistics. | Keep ambient. |
| Cosmaw | Core End safety | Tamed with Cosmic Cod; rescues owner falling off End islands. | Excellent End exploration prep. |
| Cosmic Cod | Support/resource | Food for Cosmaw; teleports away in schools. | Resource gate for Cosmaw. |
| Crimson Mosquito | Hazard/resource | Nether flying threat; larva for Laviathan/Warped Toad. | Keep as dangerous progression resource. |
| Crocodile | Support/defense/resource | Hatch imprint tame; defends birth area; scutes for swim-speed chestplate. | Water-base defense and swim gear; not normal mount in dictionary despite ride hooks. Verify before promoting. |
| Crow | Support/base logistics | Tame; gather items; deposit into framed containers; shoulder/follow. | Great camp/base logistics bird. Keep crop friction. |
| Devil's Hole Pupfish | Ambient/resource | Moss/slime interaction, bucket fish. | Keep ambient. |
| Dropbear | Hazard/resource | Ambush predator/resource. | Keep as travel danger. |
| Elephant | Core freight | Tame/ride/chest; 54-slot cargo; carpet; tusked charge progression. | Primary heavy pack animal / caravan anchor. |
| Emu | Support/resource | Fast animal; feathers for projectile-dodge leggings. No tame/mount in dictionary. | Travel-comfort gear, not mount. |
| Endergrade | Core End mount | Saddled; controlled with Chorus on a Stick; slow vertical/end mount. | Keep slow niche mount. |
| Enderiophage | Support/infrastructure | Capsid item elevator; Enderiophage rockets for Elytra. | Interesting vertical logistics; not animal transport. |
| Farseer | Defer / world-distance | Dimensional Carver / far portal / transmutation table. | High risk to travel philosophy; review separately. |
| Flutter | Support/ambient | Tame flower follower; azalea bloom chance. | Cute companion/ecology. |
| Fly | Ambient/resource | Maggots; can become Crimson Mosquito in Nether. | Breeding/resource and nuisance. |
| Flying Fish | Support / gear | Boots enable water leap/glide, better with Elytra. | Good traversal gear. |
| Frilled Shark | Hazard/resource | Bucketable; Shield of the Deep. | Ocean hazard/resource. |
| Froststalker | Support/hazard | Pack predator; helmet disguise. | Cold-biome danger/gear. |
| Gazelle | Support/resource | Horn brews Speed III. | Travel potion resource. |
| Gelada Monkey | Ambient/hazard | Territorial highland primate. | Keep ambient/hazard. |
| Giant Squid | Hazard/resource | Ocean predator, pressure behavior. | Keep as ocean danger. |
| Gorilla | Support/defense | Trust/tame-ish with bananas; commands sit/wander; defends friend; bytecode has rider hooks but dictionary does not sell it as travel. | Companion/defender; verify mount before using in plan. |
| Grizzly Bear | Core combat mount | Honey then salmon trust; war mount; ridable. | Prestige war mount, not road default. |
| Guster | Defer / weather-adjacent | Weather-limited spawns in config; Gustmaker pushes mobs/items. | Useful but weather-adjacent; partner review before tuning. |
| Hammerhead Shark | Hazard/resource | Underwater arrows/shark teeth. | Ocean resource. |
| Hummingbird | Support/ecology | Feeders keep nearby; pollination. | Camp/garden support. |
| Jerboa | Support / buff | Grants Fleet-Footed speed when fed. | Small travel buff, okay. |
| Kangaroo | Core light logistics | Tame; 9-slot pouch; equips weapon/armor; self-heals from pouch food. | Early/mid companion cargo and animal-care tutorial. |
| Komodo Dragon | Core hostile mount | Tame with lots of rotten flesh; saddled quick savage mount. | Dangerous specialty mount. |
| Laviathan | Core Nether group mount | Straddlite Saddle + Tack; up to 4 passengers; lava travel. | Excellent group expedition mount. |
| Leafcutter Ant | Defer / ecology-world blocks | Colony/chambers/fungus; can alter local blocks. | Partner/worldgen/ecology review before tuning. |
| Lobster | Ambient/resource | Food/resource; lobster tails tame aquatic pets. | Resource gate. |
| Maned Wolf | Ambient/support | Apple interaction; not tame. | Keep ambient. |
| Mantis Shrimp | Support/utility | Tame; can break blocks and hold block; anti-aquatic. | Interesting utility; review griefing before leaning on it. |
| Mimic Octopus | Support/aquatic defense | Tame; follows onto land but dries; scares attackers. | Aquatic expedition pet. |
| Mimicube | Defer / item duplication | Mimicream copies damageable items with enchantments. | Balance review; not travel. |
| Moose | Support/resource | Antler headdress knockback, ribs. | Cold/wilderness resource. |
| Mudskipper | Support/pet | Tame amphibious follower, bucketable. | Flavor/defense, not logistics. |
| Mungus | Defer / biome mutation | Can transform blocks/biomes per config. | Partner/worldgen owner. Do not tune here. |
| Murmur | Support/gear | Unsettling Kimono increases placement reach and neutralizes undead. | Expedition gear; not animal logistics. |
| Orca | Support/ocean | Swimming with pod grants Orca's Might; strong ocean hazard/ally. | Ocean travel flavor; no mount/cargo. |
| Platypus | Support/resource | Redstone-charged clay digging; bucketable. | Resource pet, not travel. |
| Potoo | Ambient/support | Falconry glove can move it. | Flavor. |
| Raccoon | Support/friction | Tame near water; bandana; wild theft; tail cap gives sneak speed. | Keep as charming friction + minor gear. |
| Rain Frog | Ambient | Breeds with maggots; poor swimmer/jumper. | Keep. |
| Rattlesnake | Hazard/resource | Rattle/resource; desert danger. | Keep as travel hazard. |
| Rhinoceros | Support/defense | Pacified with wheat; protects feeder/village. | Village/frontier defense, not mount. |
| Roadrunner | Support / gear | Feathers craft sand-speed boots. | Desert travel gear. |
| Rocky Roller | Support / gear | Chestplate enables rolling movement, reduced fall damage, sinks in water. | Traversal armor with discomfort. |
| Seagull | Support / navigation clue | Steals food; if fed lobster while holding buried treasure map, sits over treasure. | Nice physical clue mechanic; keep. |
| Seal | Support / trade | Fish-for-seabed-item trade. | Coastal camp/trade support. |
| Shoebill | Ambient/resource | Luck/lure food interactions; fish bird. | Minor. |
| Skelewag | Hazard/resource | Drowned may ride it; weapon/shield item. | Ocean combat hazard. |
| Skreecher | Hazard/sculk | Sculk/warden interaction. | Dungeon danger; not travel. |
| Skunk | Support/friction | Stink Ray redirects hostile/neutral aggression. | Camp/travel emergency tool. |
| Snow Leopard | Ambient/hazard | Breed with moose ribs; not tame. | Mountain ecology. |
| Soul Vulture | Ambient/resource | Nether fossil/perch creature. | Keep. |
| Spectre | Core End travel | Lead-tow across End void when lured with Soul Heart. | Excellent physical End navigation. |
| Straddler | Support/resource/hazard | Hostile source of Straddlite for Straddleboard. | Nether travel progression gate. |
| Stradpole | Support/resource | Bucketable lava creature; can grow into Straddler. | Nether progression ingredient. |
| Sugar Glider | Support / safety | Tame; head perch grants slow falling; leaf foraging. | Early vertical safety companion. |
| Sunbird | Support / flight | Blessing improves Elytra/slow fall; curse punishes attacking. | Mythic flight aid; keep rare. |
| Tarantula Hawk | Support/combat/gear | Tame; anti-arthropod; wings upgrade Elytra. | Desert combat pet and late travel gear path. |
| Tasmanian Devil | Ambient/hazard | Cannot tame; breed with meat. | Keep ecology. |
| Terrapin | Ambient/resource | Speedy collision turtle; bucket/eggs. | Flavor/resource. |
| Tiger | Support/hazard | Feeding grants temporary Tiger's Blessing; tigers protect but require feeding. | Good frictional relationship, not tame mount. |
| Toucan | Support/ecology | Converts fruit into saplings. | Camp/garden utility. |
| Triops | Ambient | Carrot breeding; bucketable. | Flavor. |
| Tusklin | Core friction mount | Hostile; saddled short ride; hogshoes make powerful mount. | Great gear-upgrade mount progression. |
| Underminer | Support/mining logistics | Reveals/mines toward dropped ore; Ghostly Pickaxe stores 9 stacks when inventory full. | Strong mining logistics; watch no-backpack philosophy but item friction may be okay. |
| Void Worm | Boss/resource | Dimensional Carver and boss loot. | Separate progression review. |
| Warped Mosco | Hazard/resource | Nether boss/monster, ride hooks only as internal/passenger behavior. | Keep as threat. |
| Warped Toad | Support/Nether defense | Tame; follows/stays/wanders; lava/water swimmer; anti-insect. | Good Nether expedition companion. |

Note: The Animal Dictionary also contains a `sea_bear` gag entry, but there is no spawn egg in the 89 spawn eggs counted from the language file.

## What this means for Tenpack travel

The pack already has enough animal systems to build a strong travel identity without backpacks or minimaps:

1. **Personal fast travel:** vanilla horses, improved through a deliberate stat-inspection item and better tack/gear.
2. **Early light logistics:** mules/donkeys/llamas plus Kangaroo pouch; inconvenient but worth using when inventory is intentionally tight.
3. **Serious freight:** elephants as the first obvious “oh, this is why I build roads/stables” animal.
4. **Danger-region mounts:** grizzly, komodo, tusklin, endergrade, laviathan, straddleboard.
5. **Expedition safety:** sugar glider, cosmaw, spectre, crow/eagle scouting, warped toad, caiman/capuchin/etc.
6. **Road architecture support:** hitching posts, troughs, camps, stables, signposts, waystations, and wider roads matter because the best cargo animals are physical and awkward.

## Horse / mule / tack direction

Do not solve this with Jade.

Recommended Tenpack-specific item: **Stable Ledger**, **Horseman's Glass**, or **Grooming Brush**.

Behavior:

- Use item on horse/donkey/mule/llama/camel and selected Alex mounts.
- Show a compact, in-world-ish report:
  - hearts / health category,
  - speed category,
  - jump category,
  - temperament / trust / owner,
  - inventory slots if applicable,
  - gear slots if applicable.
- Avoid exact raw decimals by default. Use bands like `Poor / Steady / Swift / Exceptional` unless creative/admin mode is active.
- Could require proximity and item durability, paper/ink, or stable workbench interaction for friction.

Progression ideas:

- Basic Grooming Brush: vague stats.
- Stable Ledger + ink/paper: records a named animal profile.
- Measuring Tack: more precise stats after repeated use/training.
- Breeder's Ledger: compares parents and foal potential.

This gives players the information they need to care about animals without turning every animal into a Jade tooltip.

## Suggested implementation phases

### Phase 1 — no new content, tune/readability

- Add live Alex's Mobs common config to client/server if we want deterministic behavior.
- Leave weather/worldgen/biome settings untouched unless partner approves.
- Document in-game animal roles through guidebook/quest text/signs.
- Keep Animal Dictionary available, but do not rely on it for vanilla horses/mules.

### Phase 2 — Tenpack stable item

- Implement a small Tenpack mod item for horse/mount inspection.
- Include vanilla horses, donkeys, mules, llamas, camels.
- Include Alex's Mobs mounts/pack animals where easy: elephant, kangaroo, grizzly, komodo, endergrade, laviathan, tusklin.
- Use coarse stat bands in survival/adventure and exact values only in creative/spectator/admin contexts.

### Phase 3 — make roads/camps the path of least resistance

- Add recipes/blocks or a small utility mod/datapack for:
  - hitching posts,
  - troughs,
  - hay/feed racks,
  - animal-safe camp fences,
  - road signs / stable signs,
  - waystation kits.
- These should make animals easier to use, not remove all friction.

### Phase 4 — cargo/caravan progression

- Keep no backpacks.
- Improve animal logistics instead:
  - better saddlebags for mules/donkeys,
  - elephant tack upgrades,
  - cart/wagon/stagecoach if a compatible implementation is found or built,
  - group travel bonuses on roads/near waystations.

## Main recommendation

Lean hard into Alex's Mobs instead of adding generic convenience mods.

The strongest Tenpack animals are:

- **Elephant** for freight.
- **Kangaroo** for early companion inventory.
- **Laviathan** for Nether group travel.
- **Straddleboard** for risky Nether solo travel.
- **Tusklin** for gear-upgrade mount progression.
- **Endergrade / Spectre / Cosmaw** for End exploration without GPS.
- **Bald Eagle** for active scouting.
- **Crow** for camp/base logistics.
- **Sugar Glider** for vertical safety.

The pack should not claim that mules beat backpacks in pure convenience. Instead, since backpacks are absent, the best available solution should become: use a mule, improve the mule, breed a better animal, build roads and stables, graduate to elephants/carts/caravans, and make the whole route feel cool enough that players choose it because it is both effective and flavorful.
