# Create farm and kitchen pass

Purpose: document the Farmer's Delight + Create: Central Kitchen angle as a separate pressure-point pass.

## Installed locally in this branch

- Farmer's Delight `1.21.1-1.3.1`
- Create: Central Kitchen `2.4.0` for Create `6.0.10`
- Create: Dragons Plus `1.10.0b`, required by Central Kitchen

## Why this belongs in Create workflow

Create farms are one of the easiest ways for new players to understand automation:

- harvesters visibly harvest crops;
- belts visibly move crops;
- presses/mixers/fans visibly process ingredients;
- kitchens make output legible and socially useful.

This is not meant to become a hard hunger economy. It is meant to create visible faction districts:

- farm fields;
- mills;
- kitchens;
- food storage;
- delivery routes to oilfields, forts, and travel parties.

## Design target

Food infrastructure should be useful and stylish without being mandatory homework.

Good outcomes:

- a faction builds a visible farm/kitchen district because it is convenient;
- prepared food becomes something worth trading or shipping;
- Create machines make farming feel like an industrial craft, not hand chores;
- farms are attractive raid/defense targets without becoming server-critical grief bait.

Bad outcomes:

- hunger becomes annoying;
- one compact hidden kitchen solves all food forever;
- Farmer's Delight recipes bypass Create progression;
- food buffs trivialize survival/combat;
- farms become mandatory but boring.

## First audit notes

Create: Central Kitchen 2.4.0 appears mostly code/tag integration rather than a large datapack recipe pack; the jar contains only a few data JSON files:

- `data/create/tags/item/handheld_in_deployer_use.json`
- `data/create/tags/item/upright_on_belt.json`
- `data/create/tags/block/passive_boiler_heaters.json`

That suggests its main value is compatibility behavior between Create and Farmer's Delight blocks/items rather than a huge recipe tree. Still test in-game with JEI.

## Questbook chapter sketch

### Farm Power

Text: Create harvesters turn fields into infrastructure. A farm is no longer just crops; it can become a district.

Completion ideas:

- hold `create:mechanical_harvester`;
- hold Farmer's Delight crop crates;
- optional/manual: build a moving harvester or stationary farm line.

### Kitchen Line

Text: Farmer's Delight makes food meaningful; Create makes it scalable. Belts, basins, fans, and deployers can turn ingredients into repeatable supply.

Completion ideas:

- hold `farmersdelight:cooking_pot`;
- hold `farmersdelight:skillet`;
- hold a prepared meal.

### Food Logistics

Text: Food is not meant to be a forced tax. It is a reason to build farms, kitchens, crates, and delivery routes if your faction wants reliable long trips, sieges, oilfield work, or airfield crews.

Completion ideas:

- manual/team quest: deliver a crate/shulker/chest of prepared food to another faction location;
- optional if a future quest mod supports location/team tasks.

## Later mechanical checks

1. Verify Farmer's Delight comfort/nourishment effects do not clash with Tenpack survival/death goals.
2. Verify cooking recipes do not create overpowered combat food too early.
3. Verify Create deployers/harvesters interact correctly with Farmer's Delight crops.
4. Verify Central Kitchen compatibility is visible in JEI/Ponder or needs questbook explanation.
5. Decide whether any Farmer's Delight tools/knives should have recipe tweaks.
