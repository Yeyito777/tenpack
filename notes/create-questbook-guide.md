# Tenpack Create questbook draft

Purpose: short source text for a later questbook. Keep the questbook practical: enough to explain the progression, not a full tutorial replacement for JEI/Create ponder scenes.

## Tone

- Tenpack Create is **CABIN-lite**, not full expert Create.
- Players should touch belts, cogs, shafts, and basic power early.
- Server-changing technology should feel like faction infrastructure: trains, oilfields, refineries, aircraft, artillery, and large logistics.
- JEI is the expected source of truth for exact recipes.
- Quest text should explain *why* a milestone matters, not list every ingredient.

## Chapter 0: Make motion

Goal: get players into Create without scaring them.

Suggested quests:

1. **Shafts and Cogwheels**
   - Text: Create starts with rotation. Shafts and cogs move power from one block to another.
   - Completion: hold `create:shaft` or `create:cogwheel`.
2. **Belts and Depots**
   - Text: Belts are intentionally early in Tenpack. Moving items around is the toybox, not the wall.
   - Completion: hold `create:belt_connector` or `create:depot`.
3. **Water Wheel Power**
   - Text: Basic power should be simple. Later power gets stronger, but early factories can run on water.
   - Completion: hold `create:water_wheel` or `create:large_water_wheel`.

## Chapter 1: Kinetic workshop

Goal: teach the first real machines.

Suggested quests:

1. **Mechanical Press**
   - Text: The press is the first step into real Create processing. Many later recipes expect pressed plates.
   - Completion: hold `create:mechanical_press`.
2. **Mixer and Basin**
   - Text: Mixing unlocks alloying and bulk processing. Basins are where Create starts feeling like a factory.
   - Completion: hold `create:mechanical_mixer`.
3. **Saw, Drill, Fan**
   - Text: Cutting, mining, and washing/smelting/smoking are your first automation branches.
   - Completion: hold one of `create:mechanical_saw`, `create:mechanical_drill`, `create:encased_fan`.
4. **Moving Contraptions**
   - Text: Bearings, pistons, gantries, and portable interfaces turn machines into moving machines.
   - Completion: hold `create:mechanical_bearing` or `create:portable_storage_interface`.

## Chapter 2: Fluids and sealed handling

Goal: make copper/fluid infrastructure legible.

Suggested quests:

1. **Pipes and Copper Casing**
   - Text: Fluids are the second layer of Create. Copper machines move, fill, drain, and store liquids.
   - Completion: hold `create:fluid_pipe` or `create:copper_casing`.
2. **Spout and Drain**
   - Text: Spouts fill items. Drains pull fluids out. Together they make fluid recipes understandable.
   - Completion: hold `create:spout` and/or `create:item_drain`.
3. **Portable Fluid Interface**
   - Text: Moving contraptions can transfer fluids too. This matters later for vehicles and fuel logistics.
   - Completion: hold `create:portable_fluid_interface`.

## Chapter 3: Precision engineering

Goal: explain the main Tenpack Create gate.

Suggested quests:

1. **Electron Tubes**
   - Text: Electron tubes mark the jump from workshop machinery to precise automation.
   - Completion: hold `create:electron_tube`.
2. **Precision Mechanism**
   - Text: In Tenpack, precision mechanisms are the main advanced Create gate. If a recipe asks for one, it is supposed to be a real milestone.
   - Completion: hold `create:precision_mechanism`.
3. **Mechanical Crafters**
   - Text: Mechanical crafting opens larger recipes and serious production chains.
   - Completion: hold `create:mechanical_crafter`.
4. **Smart Logistics**
   - Text: Packagers, stock links, and requesters belong to the smart logistics era. They are powerful because they coordinate whole bases.
   - Completion: hold `create:packager` or `create:stock_link`.

## Chapter 4: Trains and territory

Goal: tell players why rail matters.

Suggested quests:

1. **Train Station**
   - Text: Trains are not just decoration. They move bulk goods between faction locations.
   - Completion: hold `create:track_station`.
2. **Signals and Observers**
   - Text: Signals keep rail networks reliable. Rail is infrastructure worth defending.
   - Completion: hold `create:track_signal` or `create:track_observer`.
3. **First Freight Route**
   - Text: A good train route solves a real problem: oil to refinery, refinery to airfield, mine to foundry, or supplies to a front.
   - Completion: optional/manual quest.

## Chapter 4B: Farms and kitchens

Goal: make Farmer's Delight + Create farming feel like infrastructure, not hunger homework.

Suggested quests:

1. **Mechanical Harvester**
   - Text: Farms are one of the easiest ways to understand Create. A harvester turns a field into infrastructure.
   - Completion: hold `create:mechanical_harvester`.
2. **Cooking Pot**
   - Text: Farmer's Delight gives farms a reason to become kitchens. Food should be useful enough to ship, not annoying enough to become a chore.
   - Completion: hold `farmersdelight:cooking_pot`.
3. **Kitchen Line**
   - Text: Belts, basins, fans, deployers, and Farmer's Delight ingredients can become a visible faction food district.
   - Completion: hold a prepared Farmer's Delight meal or manual/team quest.

## Chapter 5: Black Gold

Goal: explain oil without turning the questbook into a spreadsheet.

Suggested quests:

1. **Oil Scanner**
   - Text: Rich oilfields are concentrated in deserts, badlands, and savannas. Scouting comes before extraction, so factions can claim and contest land before refineries exist.
   - Completion: hold `createdieselgenerators:oil_scanner`.
2. **Pumpjack Bearing**
   - Text: Finding oil is early. Extracting oil is precision-era heavy industry.
   - Completion: hold `createdieselgenerators:pumpjack_bearing`.
3. **Refinery Controller**
   - Text: Crude oil becomes useful through refining. This is where oil starts creating buildings: pipes, tanks, barrels, and train loaders.
   - Completion: hold `createdieselgenerators:distillation_controller`.
4. **Canisters and Barrels**
   - Text: Canisters are for scouting and emergency fuel. Serious fuel wealth belongs in oil barrels, tanks, depots, and trains.
   - Completion: hold `createdieselgenerators:canister` or `createdieselgenerators:oil_barrel`.
5. **Diesel Engine**
   - Text: Diesel power is compact and strong, but it creates a recurring fuel demand. A faction that relies on diesel should protect its supply line.
   - Completion: hold `createdieselgenerators:diesel_engine`.

## Chapter 6: Vehicles and physics

Goal: bridge Create into Aeronautics without skipping the factory arc.

Suggested quests:

1. **Physics Assembler**
   - Text: Vehicles start with understanding moving contraptions. The physics age follows basic Create machinery.
   - Completion: hold `simulated:physics_assembler`.
2. **Wheels and Ground Rigs**
   - Text: Ground vehicles and test rigs should arrive before full aircraft mastery.
   - Completion: hold `offroad:wheel_mount` or `offroad:tire`.
3. **Engine Assembly**
   - Text: Powered vehicles are not day-zero. They belong after basic automation and precision infrastructure.
   - Completion: hold `simulated:engine_assembly`.

## Chapter 7: Controlled flight

Goal: make the first real plane feel like a server moment.

Suggested quests:

1. **Propeller Bearing**
   - Text: Lifting something is easier than controlling it. Propeller bearings start the flight-control path.
   - Completion: hold `aeronautics:propeller_bearing`.
2. **Gyroscopic Mechanism**
   - Text: Stable flight needs precision. Gyros mark the jump from crude flying machine to controlled aircraft.
   - Completion: hold `simulated:gyroscopic_mechanism`.
3. **Smart Propeller**
   - Text: Smart propellers are late because aircraft reshape faction range, raids, trade, and scouting.
   - Completion: hold `aeronautics:smart_propeller`.
4. **First Plane Moment**
   - Text: The first functional aircraft should be a server story. Build an airfield, fuel it, protect it, and show off.
   - Completion: manual/team quest.

## Chapter 8: Heavy industry and war

Goal: make artillery and heavy production feel faction-scale.

Suggested quests:

1. **Cannon Builder / Cannon Drill**
   - Text: Artillery is a faction technology, not a starter weapon. It should require a visible industrial base.
   - Completion: hold `createbigcannons:cannon_builder` or `createbigcannons:cannon_drill`.
2. **Cannon Mounts**
   - Text: A cannon is power projection. A cannon line should have a foundry, ammo supply, and logistics behind it.
   - Completion: hold `createbigcannons:cannon_mount` or `createbigcannons:fixed_cannon_mount`.

## Sidebar: JEI and Ponder

Text for a questbook info page:

JEI is installed because Tenpack changes a lot of Create recipes. If a recipe looks unfamiliar, it is probably intentional. Use JEI for exact recipes and Create Ponder scenes for machine behavior. The questbook explains the era path; JEI explains the ingredients.
