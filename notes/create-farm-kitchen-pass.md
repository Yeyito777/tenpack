# Create farm and kitchen pass

Purpose: document the Farmer's Delight + Create kitchen pressure-point pass.

## Installed/used for this pass

- Farmer's Delight `1.21.1-1.3.1`
- Create: Central Kitchen `2.4.0` for Create `6.0.10`
- Slice & Dice `4.2.4`
- Create Confectionery / Winery / Bitterballen for tavern/culture foods
- AppleSkin `3.0.9`, visibility only
- Supplementaries `1.21.1-3.6.4` + Moonlight `1.21.1-3.0.7`, for the portable six-slot lunch basket
- CraftTweaker `21.0.38`, for server-authoritative food-value tuning without a custom mod

Related dedicated pass: `notes/hunger-pressure-points.md`.

## Why this belongs in Create workflow

Create farms are one of the easiest ways for new players to understand automation:

- harvesters visibly harvest crops;
- belts visibly move crops;
- presses/mixers/fans visibly process ingredients;
- kitchens make output legible and socially useful.

The goal is ROTN-like food presence without Nutrition/thirst. Hunger should make farms, kitchens, baskets, roads, mounts, carts, trains, and supply depots useful.

Good outcomes:

- a faction builds a visible farm/kitchen district because it is convenient;
- prepared food becomes something worth trading or shipping;
- Create machines make farming feel like an industrial craft, not hand chores;
- farms are attractive raid/defense targets without becoming server-critical grief bait;
- long mining/front/travel sessions naturally create food demand;
- good meals solve that pressure cleanly instead of asking players to memorize Nutrition categories.

Bad outcomes:

- hunger becomes an idle tax or constant chore;
- one compact hidden wheat/bread line solves all food forever;
- food buffs trivialize survival/combat;
- farms become mandatory but boring;
- players feel forced to rotate foods for arbitrary variety instead of preparing useful meals.

## Central Kitchen audit note

Create: Central Kitchen 2.4.0 appears mostly code/tag integration rather than a large datapack recipe pack; the jar contains only a few data JSON files:

- `data/create/tags/item/handheld_in_deployer_use.json`
- `data/create/tags/item/upright_on_belt.json`
- `data/create/tags/block/passive_boiler_heaters.json`

That means Tenpack needs explicit datapack recipes if we want the Create mixer to become a clear meal-production goal.

## Implemented Tenpack additions

The pack now has a controlled Create-kitchen bridge in
`server/world/datapacks/tenpack-create-progression/data/farmersdelight/recipe/integration/create/mixing/`.

It adds heated `create:mixing` recipes for the cooking-pot meal family, including:

- `baked_cod_stew_from_mixing.json`
- `beef_stew_from_mixing.json`
- `beetroot_soup_from_mixing.json`
- `bone_broth_from_mixing.json`
- `chicken_soup_from_mixing.json`
- `fish_stew_from_mixing.json`
- `fried_rice_from_mixing.json`
- `mushroom_rice_from_mixing.json`
- `mushroom_stew_from_mixing.json`
- `noodle_soup_from_mixing.json`
- `onion_soup_from_mixing.json`
- `pasta_with_meatballs_from_mixing.json`
- `pasta_with_mutton_chop_from_mixing.json`
- `pumpkin_soup_from_mixing.json`
- `rabbit_stew_from_mixing.json`
- `ratatouille_from_mixing.json`
- `squid_ink_pasta_from_mixing.json`
- `vegetable_noodles_from_mixing.json`
- `vegetable_soup_from_mixing.json`

Those bowl-meal recipes are intentionally shaped like kitchen servings:

- heated mixer required;
- one bowl consumed;
- 250 mB water consumed;
- original Farmer's Delight/common ingredients retained;
- one meal output.

The same directory also adds one tiny ration-line bridge instead of a giant food tree:

- `steak_and_potatoes_from_mixing.json` keeps bowl + baked potato + cooked beef + onion + cooked rice.

This is a heated mixer recipe with one ration output. It exists because `Food Logistics` and `Ration Routes` already point players at steak-and-potatoes as a high-value expedition meal; the datapack should let a faction scale that meal with visible Create infrastructure rather than only hand-craft it. `stuffed_potato` stays a manual/simple kitchen milestone for now because doing it as Create automation cleanly probably wants a later filling/deployer pass around milk semantics.

The intent is not to delete the Cooking Pot. The intent is to make a visible factory-kitchen line possible for mines, oilfields, forts, and travel parties.

## Food values and why automation matters now

`server/scripts/tenpack_food_pressure.zs` lowers the ceiling on trivial staples and raises/preserves the ceiling on prepared foods:

- bread and baked potatoes are cheap survival food, not comfortable travel food;
- cooked single-ingredient meats/fish are useful rations, not the final answer;
- stews, soups, pasta, rice dishes, and plated Farmer's Delight meals become the reliable sprint/combat/travel foods.

That is what makes Create automation more than decoration: a wheat farm still helps, but a multi-ingredient automated kitchen is the high-quality supply solution.

## Guardrails

`tools/check-create-progression.py` guards the kitchen bridge so future edits keep recipes heated, Create-powered, pointed at the intended outputs, and shaped like normal kitchen servings. It also verifies recipe sanity for hand-authored datapack recipes.

`tools/check-pack-configs.py` guards the food-pressure layer:

- no thirst;
- no idle food drain;
- LSO sprint/combat food exhaustion values;
- Farmer's Delight stackable soup/vanilla soup config;
- Supplementaries lunch basket config;
- CraftTweaker food-value lines for key simple foods and meals.

The advancement checker keeps the food progression legible through held items: farm power, kitchen line, and food logistics.

## Questbook chapter sketch

### Farm Power

Text: Create harvesters turn fields into infrastructure. A farm is no longer just crops; it can become a district.

Completion ideas:

- hold `create:mechanical_harvester`;
- hold a Farmer's Delight crop crate;
- optional/manual: build a moving harvester or stationary farm line.

### Kitchen Line

Text: Farmer's Delight makes food meaningful; Create makes it scalable. Belts, basins, fans, and deployers can turn ingredients into repeatable supply.

Completion ideas:

- hold `farmersdelight:cooking_pot`;
- hold `create:mechanical_mixer`;
- hold `create:basin`;
- hold `create:blaze_burner`;
- hold `farmersdelight:skillet`;
- hold a prepared meal;
- optional/manual: build a heated Create mixer line for stew/soup/pasta/rice meals or the steak-and-potatoes dry ration line.

### Food Logistics

Text: Food is not meant to be an idle tax. Work, travel, mining, and fighting burn supplies. Farms and kitchens start early, but true food logistics now waits for the Smart Logistics era: stock links, crates, baskets, and prepared meals are how a faction turns that pressure into infrastructure.

Completion ideas:

- hold `create:portable_storage_interface`;
- hold `create:stock_link` for the late stock/depot anchor;
- hold `farmersdelight:bamboo_basket` for depot/kitchen storage;
- hold `supplementaries:lunch_basket` for field rations;
- hold one core stocked meal: `farmersdelight:beef_stew`, `farmersdelight:chicken_soup`, `farmersdelight:vegetable_soup`, or `farmersdelight:steak_and_potatoes`;

### Ration Routes

Text: Once rail logistics exists, food should become route infrastructure rather than inventory babysitting. A station, stock link, package handoff, depot basket, lunch basket, and a core stocked meal represent a kitchen connected to a mine, oilfield, fort, airfield, or forward camp.

Completion ideas:

- hold `create:track_station`;
- hold `create:stock_link`;
- hold `create:package_frogport`;
- hold `farmersdelight:bamboo_basket`;
- hold `supplementaries:lunch_basket`;
- hold one core stocked meal: `farmersdelight:beef_stew`, `farmersdelight:chicken_soup`, `farmersdelight:vegetable_soup`, or `farmersdelight:steak_and_potatoes`;
- manual/team quest: deliver a crate/chest/cart/train of prepared food to another faction location.

## Playtest checks

1. Verify CraftTweaker values load and AppleSkin displays the intended differences.
2. Verify the lunch basket is useful but not backpack-like.
3. Verify cooking recipes do not create overpowered combat food too early.
4. Verify Create deployers/harvesters interact correctly with Farmer's Delight crops.
5. Verify the heated-mixing meal recipes show clearly in JEI and do not duplicate/conflict with addon recipes.
6. Verify Central Kitchen compatibility is visible in JEI/Ponder or needs questbook explanation.
