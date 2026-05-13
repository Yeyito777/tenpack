# Tenpack travel questbook / guide draft

Status: source text for a future guidebook/questbook. This assumes the current local Tenpack Travel implementation exists:

- `tenpack_travel:grooming_brush`
- `tenpack_travel:hitching_post`
- `tenpack_travel:feed_trough`
- `tenpack_travel:trail_marker`
- `tenpack_travel:mooring_post`
- `tenpack_travel:channel_marker`
- `tenpack_travel:chart_table`
- Route Journal recipe, which outputs a styled vanilla writable book

Purpose: make the travel loop discoverable without GPS, backpacks, or Jade-style overlays.

Tone:

- Practical, not tutorial-bloated.
- Explain why travel friction exists.
- Point players toward animals, roads, signs, maps, camps, and stables.
- Do not promise comfort. Promise that preparation makes discomfort manageable.

## Chapter: First Roads

Goal: teach that navigation is built from memory, maps, signs, and repeated travel.

### 1. A map that is yours

Completion ideas:

- Hold a filled map.
- Use a Chart Table to copy, scale, or lock a map if the questbook can detect it.
- Optional companion task: craft or hold a Route Journal for handwritten route notes.

Text:

> Tenpack does not hand you a destination arrow. A map is not GPS; it is a memory you can carry. Fill one yourself, keep it local, and learn what the land looks like from the road.

Reward ideas:

- paper
- empty map
- sign materials

Design note:

- Do not reward target-finder compasses, including lodestone compass anchors.
- Do not mention coordinates.

### 2. Leave a sign

Completion ideas:

- Place or hold a vanilla sign, hanging sign, or Supplementaries way sign.
- Optional companion task: place a `Trail Marker` near the sign so the route is visible before the text is read; its tiny cap light is local night readability, not a waypoint.

Text:

> A waypoint only helps the player who owns it. A sign changes the world for everyone. Mark roads, rivers, camps, danger, bridges, and crossings.

Design note:

- Do not depend on command-button sign mods. Route signs should be physical player-authored text, not command buttons, teleporters, waypoint links, or server-side click actions.

Suggested sign examples:

- `North Road`
- `River Camp`
- `Elephant Crossing`
- `Swamp Route — slow`
- `Bridge Ahead`
- `Stable / Feed`

Reward ideas:

- signs
- dye/glow ink
- torches/lanterns

### 2b. Write the route down

Completion ideas:

- Craft or hold the Route Journal recipe output.
- Write a book page with landmark directions if quest completion can inspect book use.

Text:

> A route is more than a line on a screen. Write what worked: the ford before the pine hill, the dock with two lanterns, the stable after the swamp. A good journal turns one person's mistake into the next person's road.

Design note:

- The Route Journal is a styled vanilla writable book so handwritten notes save/sign correctly.
- Do not reward coordinates, waypoint items, or structure-finder compasses here.

### 3. First camp

Completion ideas:

- Hold/place campfire.
- Hold/place bed.
- Hold/place chest or barrel.
- Hold/place sign.

Text:

> A camp turns one hard trip into a route. Sleep, cache supplies, mark the road, and make the next journey less stupid than the first.

Minimum camp checklist:

- bed or sleeping plan
- campfire
- chest/barrel/cache
- sign
- nearby water or food plan
- room for animals later

Reward ideas:

- hay bale
- lead
- lantern

### 4. The road is the tool

Completion ideas:

- Hold dirt path-related block, gravel, slabs, bridge material, or shovel.
- Build objective if quest system supports block placement.

Text:

> Roads are not decoration. They are tools. A good road saves food, daylight, animal pathing, cargo risk, and arguments.

Road advice:

- Horse road: clear branches, avoid holes.
- Mule road: safe bridges and fewer sharp drops.
- Elephant road: wider turns, higher clearance.
- Wagon road later: width matters more than beauty.

### 5. Mark the water road

Completion ideas:

- Craft or hold `tenpack_travel:mooring_post`.
- Craft or hold `tenpack_travel:channel_marker`.
- Place a sign at a dock, ferry crossing, canal, or harbor mouth.

Text:

> Rivers and coasts are roads too. A dock with a mooring post keeps boats physical. A channel marker turns a dangerous bend, shallow, canal mouth, or ferry lane into shared world knowledge without becoming a waypoint.

Water-route advice:

- Mooring posts tie boats at docks; they are not ship recall.
- Channel markers are dumb buoys; they do not track players, point to targets, or hold cargo.
- Good ports still need signs, charts, lanterns, roads, and animal/cart staging on shore.

## Chapter: Stablecraft

Goal: make animal information and animal parking discoverable while keeping them physical.

### 1. Grooming Brush

Completion ideas:

- Craft or hold `tenpack_travel:grooming_brush`.
- Use it on a horse/mule if interaction completion is supported.

Text:

> Good riders learn their animals. The Grooming Brush tells enough to make choices without turning every creature into a debug tooltip. Use it on mounts and working animals to read coarse travel notes.

What it shows:

- health band
- current condition
- speed band
- jump band where relevant
- bond band
- temperament
- role/cargo notes

Philosophy text:

> No Jade. No passive math overlay. Walk up, inspect the animal, and decide.

Bonding note:

> Bond grows by actually using the animal. Ride, travel, brush, and feed the animals that matter to you. Familiar animals can answer a local whistle, but nothing teleports and nothing becomes a GPS marker.

Reward ideas:

- lead
- wheat
- carrot
- name tag if generous

### 2. Honest pack animals

Completion ideas:

- Hold a chest while near donkey/mule/llama.
- Hold lead + chest.
- Tame/ride objective if supported.

Text:

> A mule is not better than a backpack would be. Tenpack does not have backpacks. That means cargo lives in the world: on animals, in carts, in camps, in stables, and on roads.

Exception:

- The Supplementaries lunch basket is allowed as a six-slot field-ration carrier. It is for prepared food on trips, not tools, ores, blocks, or general cargo.
- Supplementaries sacks are disabled. If you need more cargo, use animals, carts, camp caches, roads, or freight infrastructure.

Player lesson:

- Mules/donkeys/llamas are early logistics.
- They are not meant to be effortless.
- They become better when roads and camps exist.

Reward ideas:

- chest
- lead
- hay bale

### 3. Hitching Post

Completion ideas:

- Craft/hold/place `tenpack_travel:hitching_post`.
- Leash an animal nearby if supported.

Text:

> Bad friction is losing the mule to nonsense. Good friction is deciding where to park it. Hitching Posts make camps and stables legible without teleporting animals out of danger.

Rules:

- physical animal parking
- no recall
- no teleport
- no tracking UI
- empty-hand interaction can list animals hitched there

Reward ideas:

- lead
- fence gates
- hay bale

### 4. Feed Trough

Completion ideas:

- Craft/hold/place `tenpack_travel:feed_trough`.
- Use feed on it if interaction completion is supported.

Text:

> A camp should care for the animals that made the trip possible. Feed Troughs store a small amount of real feed, then nearby working animals walk over, wait, and eat when hurt or hungry. They do not breed for you, store infinite feed, or make animals immortal.

Rules:

- local radius
- stores a small physical feed supply
- animals must walk over and eat
- heals only wounded eligible animals
- no automation
- no teleport safety

Reward ideas:

- wheat
- carrots
- hay bale

### 5. Name the good ones

Completion ideas:

- Use/hold name tag.
- Build bond by actually riding/caring for it.
- Use the Whistle keybind once an animal is familiar enough.

Text:

> A good horse, mule, or elephant should become a character. Name the animals that earn it. The best travel system is one where losing the animal hurts because it mattered.

Future hook:

- Whistle is a local handling payoff for familiar animals, not recall magic.

## Chapter: Working Animals

Goal: make the Alex's Mobs roles visible to players.

### 1. Kangaroo pouch

Completion ideas:

- Observe/tame kangaroo if possible.
- Hold carrot + Grooming Brush.

Text:

> Kangaroos are early working companions. Their pouch is not freight, but it is enough to make short trips cleaner. A weapon, armor, and food in the pouch can turn one into a real travelling partner.

Role:

- light cargo
- companion fighter
- early animal-utility lesson

### 2. Elephant freight

Completion ideas:

- Hold acacia blossom.
- Use Grooming Brush on elephant if possible.
- Hold chest near elephant.

Text:

> Elephants are not pocket storage. They are freight. Their strength is huge cargo, and their cost is that roads, bridges, turns, and stables suddenly matter.

Role:

- 54-slot chested freight
- caravan anchor
- base moves and trade runs

Road reminder:

> If the elephant cannot fit, the road is not finished.

### 3. The scout bird

Completion ideas:

- Hold falconry glove / fish oil if detectable.
- Use Grooming Brush on Bald Eagle if possible.

Text:

> A Bald Eagle is not a minimap. It is active scouting. Bond with the bird, keep it near you, and a whistle can make a familiar eagle cry warning at danger it can actually see.

Role:

- short-range danger scouting
- falconry utility
- visible-world awareness without GPS

### 4. Crow camp logistics

Completion ideas:

- Hold pumpkin seeds.
- Use Grooming Brush on Crow if possible.

Text:

> A crow makes a camp feel alive. With a hay-block home and framed containers, it can gather and deposit items. That is logistics in the world, not a backpack in your pocket.

Role:

- camp cleanup
- base logistics
- shoulder companion

### 5. Vertical comfort pets

Completion ideas:

- Hold sweet berries.
- Use Grooming Brush on Sugar Glider if possible.

Text:

> Not every travel animal carries cargo. A Sugar Glider makes cliffs, ravines, trees, and mountains less punishing. Comfort is allowed when it comes from preparation.

Role:

- slow-fall safety
- forest/mountain travel support

### 6. Weird cargo is still cargo

Completion ideas:

- Hold bucket + fish/sea pickle if useful.
- Use Grooming Brush on Catfish if possible.

Text:

> Catfish cargo is strange, risky, and funny. Small and medium catfish can carry items in their bellies and keep them when bucketed. If this becomes too clean, it may be tuned later.

Role:

- weird aquatic cargo trick
- not the main logistics path

## Chapter: Dangerous Roads

Goal: teach that some animals are powerful because they are inconvenient.

### 1. War mount, not pack mule

Completion ideas:

- Use Grooming Brush on Grizzly Bear or Komodo Dragon if possible.

Text:

> Some animals do not solve cargo. They solve danger. A grizzly or komodo is not a better mule; it is a prepared answer to a hostile route.

Roles:

- Grizzly Bear: war mount
- Komodo Dragon: predator mount

### 2. Tusklin gear progression

Completion ideas:

- Hold saddle / pigshoes if detectable.
- Use Grooming Brush on Tusklin if possible.

Text:

> Tusklins start as a bad idea. Gear makes the bad idea useful. That is the kind of progression Tenpack wants: better animal, better tack, better route — not a backpack.

Role:

- bucking battle mount
- hogshoes upgrade path

### 3. The swamp and coast are alive

Completion ideas:

- Use Grooming Brush on Crocodile, Caiman, Seal, or Seagull if possible.

Text:

> Water routes are natural roads, but they are not empty. Caimans defend camps, crocodiles punish carelessness, seals trade, and seagulls add coastal clue/flavor without becoming a map arrow.

Roles:

- Caiman: water camp defense
- Crocodile: water danger / swim gear resource
- Seal: coastal trade support
- Seagull: coastal clue/flavor behavior

## Chapter: Other Worlds

Goal: teach that Nether and End travel have their own animal logic.

### 1. Lava routes

Completion ideas:

- Hold Straddleboard / Straddlite Saddle / Straddlite Tack if detectable.
- Use Grooming Brush on Laviathan if possible.

Text:

> The Nether should not travel like the Overworld. Straddleboards are risky solo speed. Laviathans are prepared group ferrying. Both are better than a teleport menu.

Roles:

- Straddleboard: fast, dangerous lava travel
- Laviathan: four-passenger lava ferry

### 2. Nether companion defense

Completion ideas:

- Use Grooming Brush on Warped Toad if possible.

Text:

> A Warped Toad is not a mount. It is a route companion for a dimension full of horrible insects. Bring the right animal for the place.

Role:

- anti-insect Nether defense

### 3. End safety without removing the End

Completion ideas:

- Use Grooming Brush on Cosmaw or Endergrade if possible.
- Hold Chorus on a Stick if detectable.

Text:

> The End should stay frightening. Preparation should make it survivable, not solved. Endergrades help with slow vertical travel. Cosmaws can rescue a falling owner.

Roles:

- Endergrade: slow vertical mount
- Cosmaw: fall rescue companion

### 4. Void tow

Completion ideas:

- Hold Soul Heart + lead if detectable.
- Use Grooming Brush on Spectre if possible.

Text:

> A Spectre is the opposite of a waypoint. You are still crossing the void; you just prepared a way to survive it.

Role:

- physical End island travel by lead tow

## Chapter: Freight Future

Goal: point toward later carts/wagons/stagecoaches without pretending they already exist.

### 1. When a road becomes civilization

Completion ideas:

- Build/use multiple camps or roads if supported.
- Hold Hitching Post + Feed Trough + sign.

Text:

> A road becomes civilization when it supports repeated travel: signs, camps, troughs, hitching posts, bridges, caches, and eventually carts or wagons.

### 2. Carts and stagecoaches later

Text:

> If Tenpack adds carts, wagons, or stagecoaches, they should reward roads rather than erase terrain. They should be awkward off-road, useful on-road, physical, vulnerable, and worth protecting.

Design constraints:

- animal-drawn
- road-biased
- physical cargo
- passenger variants
- no pocket storage bypass

## Short version for in-game book intro

> Tenpack travel is not about removing distance. It is about making distance playable. Maps, signs, animals, camps, and roads are the tools. The world should become easier because you learned it and built through it — not because a UI gave you an arrow.

## Things this chapter must not recommend

- Nature's/Explorer-style target-finder compass loops.
- Backpacks.
- Jade-style passive stat overlays.
- Global animal teleport recall.
- Coordinate reliance.
- Weather/storm progression changes without partner approval.
