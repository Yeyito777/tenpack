# Create progression curation plan

Purpose: command-module synthesis of the read-only subagent audits for Tenpack's Create stack. Food automation is one branch of the progression, not the whole progression.

## Current diagnosis

The Create datapack/checker work is useful and mostly correct, but the weak point is era legibility rather than JSON syntax:

- addon bypass protection is strong, including nested Aeronautics/Simulated/Offroad recipe scans;
- the Farmer's Delight heated-mixer bridge is broader than the early summaries implied: it covers the cooking-pot meal family as audited one-bowl/250 mB-water/one-meal recipes;
- players still need a clearer Create spine from workshop machines into brass/electronics, precision, logistics, oil, vehicles, flight, and heavy industry;
- food should teach logistics early, then become one supply-chain branch for mines, oilfields, forts, roads, trains, airfields, and expeditions.

## Target progression spine

1. Starter Logistics — shafts, belts, depots, movement.
2. Kinetic Workshop — press, mixer, saw/drill/fan, bearings, harvesters.
3. Fluid Handling — pipes, tanks, spouts, drains.
4. Brass & Electronics — brass casing, electron tube, deployer.
5. Precision Engineering — precision mechanism and sequenced assembly as the main gate.
6. Smart Logistics / Rail — packagers, stock links, stations, freight routes.
7. Oil — prospecting, pumpjacks, refining, barrels/tanks, diesel.
8. Physics / Ground Vehicles — physics assembler, wheel mounts, tires, steering, throttle, engines.
9. Controlled Flight — gyros, sensors, propeller bearings, smart propellers, airfield.
10. Heavy Industry / War — diesel power, cannon infrastructure, ammo/foundry, fort supply routes.
11. Food Logistics — farm/kitchen starts early, but real shipment/ration logistics belongs with storage, routes, lunch baskets, depots, and vehicles/trains.

## Implemented in this curation pass

- Added a `Brass & Electronics` advancement between `Fluid Handling` and `Precision Engineering`.
- Reparented `Precision Engineering` under `Brass & Electronics` so brass casing/electron tube/deployer are visible before precision mechanisms.
- Moved `Physics Age` behind `Precision Engineering` instead of directly behind `Kinetic Workshop`.
- Added `create:precision_mechanism` to `simulated:physics_assembler`, so vehicle/physics experimentation is actually precision-era instead of just visually later.
- Updated `tools/check-create-progression.py` to pin the new brass/precision/physics parent chain, requirements, titles/icons, and concrete item refs.
- Split oil into explicit advancement beats: `Oil Prospecting` for the early scanner, `Black Gold` for precision-era pumpjacks, `Oil Refining` for distillation/storage, and `Diesel Power` for recurring-fuel compact power.
- Reparented `Heavy Industry` under `Diesel Power` and made its held-item milestone the cannon builder, so artillery reads as diesel-era faction industry instead of another generic precision toast.
- Added `createdieselgenerators:diesel_engine` to the Create Big Cannons production machinery overrides and checker invariants, resolving the mismatch where Heavy Industry was visually diesel-era but the actual cannon recipes were still only precision-era.
- Updated `tools/check-create-progression.py` to pin the oil/heavy-industry parent chain, requirements, titles/icons, and concrete item refs.
- Corrected Tenpack's Create Diesel Generators fuel-type overrides to use the installed `#c:diesel` and `#c:gasoline` fluid tags instead of non-present `#forge:*` aliases, and updated the checker to guard those tags.
- Expanded `Smart Logistics` so it now requires mechanical crafting, package routing, stock-link networking, and either factory gauge or redstone requester stock-control as held-item milestones for coordinated bases.
- Added a `Rail Logistics` advancement for track stations plus signal/observer infrastructure, keeping train-route visibility separate from generic precision tech.
- Added checker recipe anchors for the hand-authored smart-logistics and rail overrides so packagers, frogports, stock links, gauges/requesters, mechanical crafters, stations, signals, and observers stay tied to precision/brass/railway components.
- Overrode Create's alternate `track_observer_from_other_plates` recipe so the pressure-plate path also requires `create:precision_mechanism` and cannot bypass the tuned track-observer gate.
- Added a `Ground Vehicles` advancement between `Physics Age` and `Controlled Flight`, requiring wheel mount, tire, steering wheel, throttle lever, and engine assembly.
- Strengthened `Controlled Flight` so it now requires gyroscopic mechanism, propeller bearing, throttle lever, altitude sensor, and velocity sensor; flight now means controlled aircraft systems, not only lift parts.
- Moved `aeronautics:mounted_potato_cannon` later by requiring `simulated:engine_assembly` in addition to swivel/precision anchors, and updated the checker to pin that vehicle-era weapon gate.
- Moved `Food Logistics` out of the early kitchen branch and under `Smart Logistics`, while keeping `Farm Power` and `Kitchen Line` early.
- Added `supplementaries:lunch_basket` as the field-ration criterion, kept `farmersdelight:bamboo_basket` as depot/kitchen storage, and made `Food Logistics` require the smart-logistics stock-link anchor without pretending a rail station is an alternate path after `Smart Logistics` is already complete.
- Added a `Ration Routes` milestone under `Rail Logistics` so food shipment has a real route-facing follow-up: station, stock link, package frogport, depot basket, lunch basket, and one core stocked meal.
- Added the first narrow ration-line automation gap: a heated Create mixer recipe for `farmersdelight:steak_and_potatoes`, preserving its real ingredients while making the Food Logistics/Ration Routes meal pick scalable.
- Added final checker guardrails for 1.21 singular datapack folder names, type-specific recipe outputs (`result` for vanilla/mechanical crafting; `results` for Create processing), shaped/shapeless ingredient structure, sequenced-assembly internals, Create fluid ingredients (`neoforge:single` + concrete fluid + positive amount), and non-additive recipe override paths.

## Next high-value passes

### Smart logistics and rail follow-up

The basic precision-era smart/rail milestones are now present. Future follow-up should decide whether to add guardrails/quests for:

- content observer / display board;
- manual first freight route.

This is where food, ore, oil, ammo, and travel supply routes should converge, but the current implementation intentionally stops at item-held milestones because the datapack cannot detect a real route.

### Vehicle and flight follow-up

The main ground-vehicle-before-flight advancement split is now present. Later passes should still consider:

- moving large/monstrous tires later;
- optionally adding a diesel/fuel-route quest beat before large powered vehicle use;
- deciding whether mounted potato cannon should be even later, e.g. behind `Controlled Flight` or heavy-industry cannon infrastructure rather than only `engine_assembly`.

### Food branch

Farm Power and Kitchen Line stay early; Food Logistics is now the later stock/depot branch under Smart Logistics, and Ration Routes is the route-facing branch under Rail Logistics. The broad heated-mixer meal bridge plus the single steak-and-potatoes dry ration line is enough for now. Do not add a giant food datapack. If more curated content is needed, prefer a few visible factory-line gaps:

- hamburger or sandwich assembly;
- stuffed potato line, but only if a later pass wants to solve milk/filling/deployer semantics cleanly;
- mixed salad / fruit salad cold line;
- one feast block line for faction kitchens.

Supplementaries `lunch_basket` is the personal ration container; Farmer's Delight baskets remain depot/kitchen storage.

## Guardrail backlog

- Add core Create recipe invariant checks to match the addon checker's strictness.
- Keep the public rebuild caveat in mind: `public/` reflects the whole dirty tree, not only Create.
