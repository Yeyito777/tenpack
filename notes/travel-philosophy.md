# Tenpack travel philosophy

Status: design draft for travel, roads, camps, and animal utility.

Related note: `notes/animal-travel-audit.md`

## North star

Travel in Tenpack should feel like **crossing a place**, not selecting a destination.

The player should learn the world through:

- maps they personally made,
- signs they or other players placed,
- roads worn into the terrain,
- camps and stables they recognize,
- rivers, mountains, forests, coastlines, and skyline landmarks,
- animals they prepared and cared about.

Travel should not become:

- a coordinate/GPS chase,
- a minimap waypoint treadmill,
- a backpack/inventory bypass,
- a teleport menu,
- a passive Jade tooltip optimization game.

The ideal player story is:

> “We packed the mule, took the ridge road by the old sign, stopped at the river camp, lost time in the forest, sent the eagle up to check the valley, and barely got the elephant home before night.”

## Core rule: the best path should be the cool path

Players follow the path of least resistance. If the cool option is meaningfully worse, it becomes decoration.

So Tenpack should not say:

- “Mules are better than backpacks.” They are not, in pure convenience.
- “Animals are useful because they are cute.” Cute is not enough.
- “Roads matter because we like roads.” They matter only if they reduce real friction.

Instead, because backpacks/GPS/teleport convenience are absent, the best available practical path should become:

1. carry only what fits on your body,
2. use a mule/donkey/llama/camel when you outgrow that,
3. inspect and breed better animals,
4. improve tack and cargo gear,
5. build roads/camps/stables because animals make them valuable,
6. graduate to elephants, caravans, carts, wagons, stagecoaches, or faction transport.

This keeps friction while making the friction point toward interesting infrastructure.

## What travel should feel like

### On foot

Fantasy: “I am vulnerable, limited, and attentive.”

Use case:

- early exploration,
- dangerous terrain,
- stealth/scouting,
- final approach to a cave/ruin/base.

Strengths:

- precise movement,
- can climb/sneak/fight easily,
- no animal to protect,
- easiest in dense forests/caves.

Weaknesses:

- limited inventory,
- fatigue/food/exposure pressure,
- slow over distance,
- poor for hauling.

Design implication:

- Foot travel should not be miserable. It should be the baseline.
- The pressure should come from **distance + cargo + route risk**, not from constant annoyance.

### Horse / fast personal mount

Fantasy: “I know the road and I can move.”

Use case:

- personal overworld distance,
- scouting roads,
- messengers,
- escaping weather/night/danger without teleporting.

Strengths:

- speed,
- road payoff,
- emotional attachment through breeding/stats/name/gear.

Weaknesses:

- poor cargo,
- must be parked/protected,
- bad in dense terrain/water/caves,
- easy to lose without infrastructure.

Design implication:

- Horses need stat readability, but not Jade.
- Add a deliberate tool: Grooming Brush / Horseman’s Glass-style tool.
- Road quality should noticeably affect horse convenience.

### Mule / donkey / llama / camel

Fantasy: “This trip has a loadout.”

Use case:

- early expeditions,
- mining trips,
- moving camp supplies,
- small trade runs.

Strengths:

- practical cargo,
- obtainable before elephants/carts,
- naturally supports camps and routes.

Weaknesses:

- worse than a backpack if backpacks existed,
- slower/clumsier than a horse,
- needs handling and protection.

Design implication:

- Do not pretend the inconvenience is not real.
- Make them the best available answer to inventory pressure.
- Later, let players improve them with better tack/saddlebags/stable knowledge.

### Elephant / heavy pack animal

Fantasy: “We are moving a serious expedition.”

Use case:

- base moves,
- trade convoys,
- faction logistics,
- long roads,
- big projects.

Strengths:

- huge cargo,
- visually impressive,
- road/stable infrastructure payoff,
- social/faction identity.

Weaknesses:

- large, awkward, slower than personal mounts,
- requires route planning,
- harder to tame/breed,
- a liability in bad terrain.

Design implication:

- Keep elephants powerful. The 54-slot cargo is the point.
- Their friction should be physical and logistical, not arbitrary stat nerfs.
- If an elephant is hard to use, roads and stables become valuable.

### Cart / wagon / stagecoach

Fantasy: “The road is now civilization.”

Use case:

- group transport,
- trade lanes,
- faction routes,
- regular supply movement.

Strengths:

- makes roads matter more than isolated waypoints,
- supports player-built towns/camps,
- encourages raidable/protectable logistics.

Weaknesses:

- should require road width/turning space,
- should be bad off-road,
- should be vulnerable enough to plan around.

Design implication:

- If no compatible mod exists, this is worth custom implementation later.
- It should be a road reward, not a universal vehicle.

### Boats / water travel

Fantasy: “Rivers and coasts are natural highways.”

Use case:

- early long-distance travel,
- low-cargo movement,
- coastal exploration.

Design implication:

- Water should remain a strong natural route.
- Animals like crocodiles, orcas, catfish, seals, and aquatic pets can make water routes feel alive without needing GPS.

### Nether / End travel

Fantasy: “Each dimension has its own travel logic.”

Nether:

- Straddleboard = fast, risky solo lava movement.
- Laviathan = expensive, safer, group lava transport.
- Warped Toad = anti-insect expedition pet.
- Tusklin = hostile/bucking mount with gear progression.

End:

- Endergrade = slow vertical utility.
- Cosmaw = fall-rescue companion.
- Spectre = physical void towing between islands.

Design implication:

- Do not flatten dimensions into the same travel system.
- Each dimension should have different animals and different fears.

## Good friction vs bad friction

### Good friction

Good friction creates planning, stories, and infrastructure:

- “Can this road fit the elephant?”
- “Do we have enough food for the animals?”
- “Where is the next camp?”
- “Can we cross this river?”
- “Do we risk the shortcut through the forest?”
- “Should we bring the eagle, crow, or sugar glider?”
- “Do we take two mules or one faster horse?”

### Bad friction

Bad friction makes players quit the system:

- animals constantly vanish,
- mounts get stuck on every block,
- no way to know animal stats,
- no way to secure animals at camp,
- cargo UI is too annoying,
- animals die cheaply to random nonsense,
- the player must babysit pathfinding more than travel.

Tenpack should keep the discomfort, but remove the bullshit.

## UI / information philosophy

No Jade-style always-on entity math.

Players should get information through **tools and rituals**:

- brush the horse,
- inspect the saddle,
- compare breeding records,
- check cargo tack,
- read the Animal Dictionary for Alex's Mobs behavior.

Recommended item chain:

### Grooming Brush

Early item.

Use on a mount/pet to reveal coarse survival stats:

- Health: frail / healthy / sturdy / massive
- Speed: slow / steady / swift / exceptional
- Jump: poor / fair / strong / remarkable
- Temperament: skittish / calm / loyal / aggressive
- Cargo: none / light / pack / freight

No raw decimals.

### Measuring Tack

Later item.

Gives more precise bands or exact values only after effort:

- repeated measurements,
- animal trust,
- stable workbench,
- creative/admin exact mode.

## Road and camp philosophy

Roads should be useful before they are beautiful, then become beautiful because they are useful.

### Road tiers

#### Trail

- dirt/path blocks,
- signs or marked trees,
- basic campfire stops,
- good for foot travel and horses.

#### Road

- wider path,
- bridges over bad water crossings,
- fences or markers near cliffs,
- hitching points,
- good for horses and mules.

#### Freight road

- elephant-width turns,
- clear overhead branches,
- periodic stables,
- troughs and feed racks,
- safe night stops,
- good for elephants/carts/wagons.

#### Faction road

- watch posts,
- toll gates or checkpoints,
- trade signs,
- protected bridges,
- stagecoach stops,
- visible infrastructure worth defending.

### Camps / waystations

A camp should solve specific travel problems:

- sleep/shelter,
- food refill,
- animal parking,
- animal healing,
- map table / atlas check,
- road sign updates,
- cargo sorting,
- emergency chest/cache.

Possible blocks/items later:

- hitching post,
- feed trough,
- hay rack,
- stable marker,
- camp crate,
- road sign post,
- animal bell/whistle,

## Pet design rules

Animals should be loved because they are alive, but used because they solve real problems.

A good Tenpack pet should have at least one of these:

1. **Travel role** — faster, safer, vertical, aquatic, Nether, End.
2. **Cargo role** — pouch, chest, freight, weird storage.
3. **Scouting role** — active information gathering without GPS.
4. **Camp role** — guarding, gathering, sorting, healing, farming.
5. **Comfort role** — fall safety, speed buff, weather/biome comfort, danger warning.
6. **Progression role** — better gear, better breeding, rare tame, special saddle.

It should also have at least one cost:

- food,
- training/taming difficulty,
- terrain limitation,
- vulnerability,
- awkward size,
- gear requirement,
- behavior risk,
- region/dimension limitation.

No animal should be “just worse horse.” If an animal is worse at speed, it needs another job.

## Animal role cards

### Bald Eagle — active scout / falconry pet

What it does:

- Tamed with fish oil.
- Falconry glove carries and launches it.
- Falconry hood enables direct piloting at limited range.

Why it fits:

- It gives information through player action.
- It does not place GPS markers.
- It creates a bond: you launch your eagle, not a UI scan.

Tenpack use:

- Scout a valley before descending.
- Check road ahead for mobs/players/terrain.
- Identify landmarks from the air.

Do not overbuff it into remote chunk radar.

### Crow — camp logistics bird

What it does:

- Tamed with pumpkin seeds.
- Can gather items and deposit into framed containers.
- Uses hay blocks as a home/healing anchor.
- Can ride shoulders.

Why it fits:

- A crow makes a camp feel alive.
- Item sorting is physical: containers, item frames, home blocks.
- It is useful without being a backpack.

Tenpack use:

- Camp cleanup bird.
- Farm edge utility.
- Small base automation pet.

Keep wild crop-stealing if it is not too annoying; friction makes tamed crows feel earned.

### Elephant — freight animal

What it does:

- Ridable.
- Chest gives 54 slots.
- Carpet decoration.
- Tusked progression through calf taming.

Why it fits:

- It is powerful enough to justify infrastructure.
- Its size makes routes matter.
- It turns logistics into a visible event.

Tenpack use:

- Long expedition supply carrier.
- Faction freight.
- Base relocation.
- Caravan centerpiece.

Do not nerf cargo just because it is strong. Make route planning the cost.

### Kangaroo — early pouch companion

What it does:

- Tame with carrots.
- 9-slot pouch.
- Can use weapon/armor from pouch.
- Eats non-meat food from pouch to heal.

Why it fits:

- Teaches animal utility early.
- Light cargo plus personality.
- Useful, but not a logistics solution by itself.

Tenpack use:

- First “my pet has a job” animal.
- Short expedition buddy.
- Combat/light cargo hybrid.

### Mule / donkey / llama — honest pack animals

What they do:

- Carry early cargo.
- Are worse than a backpack would be, but backpacks are not in the pack.

Why they fit:

- They are the first real answer to inventory limits.
- They make camps and roads useful before exotic animals.

Tenpack use:

- Mining trips.
- Starter trade routes.
- First moving day.

Needed support:

- Stat-inspection item.
- Better hitching/parking.
- Better gear/tack progression.

### Horse — personal road speed

What it does:

- Fast personal travel.
- Breeding/stat chase.

Why it fits:

- Roads become valuable.
- A good horse becomes named and loved.

Needed support:

- No Jade.
- Add Grooming Brush-style deliberate inspection.
- Add call/whistle behavior carefully: not global teleport, but reduce stupid loss.

### Sugar Glider — vertical comfort pet

What it does:

- Tamed with sweet berries.
- Head perch gives slow falling.
- Forages leaves.

Why it fits:

- A comfort pet with real exploration value.
- Good for cliffs, trees, ravines, and mountains.

### Catfish — weird cargo trick

What it does:

- Small/medium catfish store 3/9 stacks.
- Large catfish can store one small mob.
- Belly contents persist when bucketed.

Why it fits:

- It is not clean convenience.
- It is strange, physical, and funny.

Risk:

- If players mass-bucket medium catfish, it may become backpack-like.

Recommendation:

- Keep for now. Watch actual behavior before nerfing.

### Laviathan — Nether group ferry

What it does:

- Lava travel.
- Up to four passengers.
- Requires saddle + tack.

Why it fits:

- Makes Nether expeditions prepared and social.
- Different dimension, different travel solution.

### Straddleboard — risky Nether speed

What it does:

- Fast lava board.
- Can snap/tumble on collision.

Why it fits:

- Pure good friction: powerful but dangerous.

### Tusklin — gear-progression battle mount

What it does:

- Hostile/bucking mount.
- Pigshoes improve ride and can be enchanted.

Why it fits:

- Clear “bad animal -> better gear -> powerful mount” arc.

### Spectre — End void tow

What it does:

- Lured with Soul Heart.
- Lead-tows player across End void.

Why it fits:

- Physical navigation between islands.
- Scary and memorable.

### Cosmaw — End fall rescue

What it does:

- Tame with Cosmic Cod.
- Rescues owner falling off End islands.

Why it fits:

- Preparation makes exploration safer without removing danger.

### Endergrade — slow End climber

What it does:

- Saddled, controlled with Chorus on a Stick.
- Slow vertical travel.

Why it fits:

- Useful but uncomfortable, exactly right for End terrain.

## Optimized travel loop

### First trip: discover

Player takes minimal supplies, maybe on foot/horse.

Tools:

- empty maps,
- atlas,
- signs,
- bedroll/camp if available later,
- maybe eagle/crow/sugar glider.

Outcome:

- Find landmarks.
- Mark rough route.
- Learn terrain.

### Second trip: establish

Player returns with a pack animal.

Tools:

- mule/donkey/llama/kangaroo,
- sign materials,
- camp supplies,
- bridge materials,
- feed.

Outcome:

- Place signs.
- Improve crossings.
- Build first camp/cache.

### Third trip: regularize

Player turns route into infrastructure.

Tools:

- horse for speed,
- mule/elephant for cargo,
- crow for camp logistics,
- road blocks/bridges/fences.

Outcome:

- Road becomes easier than wilderness.
- Camps become recognizable.
- Animals feel like part of the route.

### Fourth trip: scale

Faction/group uses route.

Tools:

- elephant/carts/wagons/stagecoaches/trains eventually,
- stable network,
- guarded bridges,
- maps/atlases.

Outcome:

- Logistics becomes visible power.
- Roads become social/faction targets.

## Implementation backlog

### Now / low risk

- Keep Alex's Mobs; lean into its existing animal systems.
- Add active Alex's Mobs configs to client/server only if we need deterministic defaults.
- Do not change weather/worldgen/biome mutation configs in this pass.
- Write questbook/guide text around animal travel roles.
- Keep Animal Dictionary available.

### Small custom mod: animal inspection

Create a Tenpack item:

- Grooming Brush.
- Use on vanilla mounts and selected Alex's Mobs animals.
- Shows coarse stats in survival/adventure.
- Shows exact stats in creative/spectator/admin.
- Optional durability or paper/ink cost.

Priority entities:

- horse,
- donkey,
- mule,
- llama,
- camel,
- elephant,
- kangaroo,
- grizzly bear,
- komodo dragon,
- endergrade,
- laviathan,
- tusklin.

### Small custom mod/datapack: stable support

Add or find:

- hitching post,
- trough,
- feed rack,
- animal bell/nearby call,
- stable sign,
- road sign integration.

Rules:

- Reduce annoying animal loss.
- Do not add global teleport recall.
- Prefer “nearby call” or “return to bound stable/camp under safe conditions.”

### Medium custom mod: tack progression

Add progression rather than backpacks:

- simple saddlebags,
- reinforced saddlebags,
- weather/biome tack if partner approves weather interactions,
- elephant freight tack,
- mule harness,
- wagon harness.

Balance:

- Gear makes animals easier/better.
- Gear does not make inventory friction disappear.

### Large custom mod: carts/wagons/stagecoaches

If no compatible mod exists, this is a high-value Tenpack feature.

Rules:

- Road-biased.
- Bad off-road.
- Physical turning/width constraints.
- Animal-drawn.
- Cargo and passenger variants.
- Breakable/repairable enough to create stories.

## What not to do

- Do not add target-finder compasses as the main navigation answer.
- Do not add backpacks.
- Do not add Jade-style always-on animal stat overlays.
- Do not make every animal a generic mount.
- Do not flatten travel with easy teleport recall.
- Do not touch partner-owned storms/weather gameplay/worldgen/named regions here.
- Do not nerf strong animals until playtesting proves they are actually bypassing the intended loop.

## Summary

Tenpack travel should be a ladder from vulnerability to infrastructure:

1. **On foot** — personal risk and discovery.
2. **Horse** — speed and route knowledge.
3. **Mule/donkey/llama/kangaroo** — early cargo and pet utility.
4. **Camps/stables/roads** — friction turns into infrastructure.
5. **Elephant/caravan/wagon** — serious visible logistics.
6. **Dimension animals** — Nether and End have their own travel languages.

The heart of the system is not making travel effortless. It is making the practical answer to travel problems become beautiful, social, and memorable.
