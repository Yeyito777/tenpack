# Hunger pressure-point pass

Date: 2026-05-13

Goal: make food **present and intentionally hard** in the useful ROTN sense, without copying ROTN's Nutrition/spreadsheet system, thirst, or a custom Tenpack hunger mod.

## What we liked from ROTN

- Food was always part of the survival loop.
- Players thought about stack size, saturation, duration, and what food to bring on trips.
- Cooking and food logistics felt like real preparation instead of cosmetic clutter.
- Inventory pressure made a portable food container feel valuable.
- A long trip with bad food felt different from a long trip with real meals.

## What we do not want from ROTN

- No Nutrition nutrient categories.
- No thirst.
- No constant idle hunger drain.
- No nausea spam or hidden low-food punishment stack.
- No custom Tenpack hunger Java mod for this pass.
- No requirement that a player carry six hotbar/inventory slots of different food just to play.

## Current Tenpack stack that already helps

- **Farmer's Delight**: meals, soups, feasts, Comfort, Nourishment, baskets/crates/cabinets, cooking pot/skillet/stove, and strong meal identity.
- **Create: Central Kitchen + Slice & Dice**: food can become belts, basins, deployers, cutting automation, and visible kitchen lines.
- **Create Confectionery / Winery / Bitterballen**: tavern, snack, trade, and faction-culture foods.
- **Serene Seasons**: light crop-season pressure; Farmer's Delight crops already have season tags.
- **Legendary Survival Overhaul**: temperature is enabled; thirst/body-damage/health-overhaul remain disabled. Farmer's Delight foods already integrate as warming/cooling food.
- **Travel/death stack**: roads, carts/mounts/rails, corpse pressure, and Lifesteal make expeditions and logistics matter, so hunger should support those pressure points instead of becoming the only punishment.

## Implemented direction

### AppleSkin: visibility

AppleSkin is client-only and changes no gameplay. It makes hunger, saturation, exhaustion, and food tooltips readable so players can actually see why bread is a low-ceiling ration and meals are better.

### Supplementaries lunch basket: portable food logistics

The correct portable basket layer is **Supplementaries** `supplementaries:lunch_basket`, not Spice of Life Onion.

Confirmed/guarded behavior:

- item id: `supplementaries:lunch_basket`;
- recipe in the mod is bamboo + wool carpet;
- common config keeps `[tools.lunch_basket] enabled = true`, `placeable = true`, `slots = 6`;
- left-click toggles the basket open/closed;
- open mode lets players eat/select from the stored food;
- accepts ordinary EAT/DRINK items except the mod's blacklist;
- can be placed physically as a block;
- six slots helps trips without becoming a backpack.

The separate Supplementaries sack is disabled in active client/server configs and the reference override. It is a nine-slot general portable container, not a ration basket, so it would undercut the animal/cart/camp cargo loop.

Farmer's Delight baskets still matter, but they are a different thing: 27-slot placeable/automatable kitchen storage blocks, not a carried picnic/lunch basket. Tenpack's Carry On policy blacklists them so filled kitchen/depot baskets cannot become a handheld backpack.

### CraftTweaker food values and runtime pressure

`server/scripts/tenpack_food_pressure.zs` now changes the actual food ceiling:

- bread and baked potatoes are still useful, but their saturation is much worse;
- raw crops and quick snacks are emergency food, not expedition food;
- cooked single-ingredient meats/fish are decent field rations, but no longer solve the whole server alone;
- vanilla stews and Farmer's Delight prepared meals are the high-ceiling foods;
- existing food effects/components are preserved by modifying each item's existing `FoodProperties` instead of replacing everything from scratch.

Key examples:

- `minecraft:bread`: `5 / 0.6` vanilla-ish role becomes `3 / 0.2`.
- `minecraft:baked_potato`: becomes `3 / 0.3`.
- `minecraft:cooked_beef` and `minecraft:cooked_porkchop`: become `6 / 0.5`.
- `minecraft:mushroom_stew`: becomes `7 / 0.7`.
- `minecraft:rabbit_stew`: becomes `10 / 0.8`.
- `farmersdelight:beef_stew`: becomes `10 / 0.85`.
- `farmersdelight:pasta_with_meatballs` and `farmersdelight:steak_and_potatoes`: become `12 / 0.85`.

This means a single wheat farm can still prevent starvation, but it is no longer the comfortable sprint/combat/travel solution. Real kitchens, mixed ingredients, and meal automation become the practical answer.

The same script also owns the light ROTN-like runtime layer:

- standing still/AFK adds no Tenpack drain;
- every 20 ticks, active play can add exhaustion from movement, riding/passenger travel, item use, sprinting, and swimming;
- recent diet balance is tracked as three broad session-local groups: **staple**, **protein**, and **produce**;
- eating only one group, e.g. bread-only or meat-only, increases active drain softly;
- eating across all three groups reduces active drain softly;
- group memory decays only while active, so AFK standing neither drains food nor erases preparation;
- hidden meal quality applies a short `minecraft:saturation` "satisfied" pulse for real meals without creating a visible comfort meter.

Current balance multipliers are intentionally soft instead of Nutrition-style chores:

- no recent group coverage: `1.40x` active drain;
- one broad group covered: `1.28x`;
- two broad groups covered: `1.08x`;
- all three groups covered: `0.92x`;
- all three groups robustly stocked: `0.80x`.

Food-group points cap at `80` per group and decay by one point every eight active seconds. This means six bread can make the player very stocked on **staple**, but it will not count as protein or produce. A real meal wins because it fills multiple groups and has hidden meal quality.

The hidden meal-quality ladder is not tracked as a diet category. Bread has no quality reward, cooked inventory meat is mostly just protein, bowl meals and sandwiches are comfortable, and plated/feast-tier foods are the "steak dinner" tier. Meal quality only affects the immediate satisfied pulse, lightly boosted by current staple/protein/produce balance.

The Supplementaries lunch basket remains unchanged at six slots. It is the ergonomic answer to carrying prepared food, but the script tracks foods actually eaten rather than basket contents. That avoids brittle Supplementaries component parsing and prevents hoarding foods from counting as a diet.

### LSO activity pressure

Thirst and idle hunger remain off. Activity pressure is now stronger than the first timid pass:

- `Base Food Exhaustion = 0.0`: no idle tax.
- `Sprinting Food Exhaustion = 0.01`: long on-foot travel burns weak rations noticeably.
- `On Attack Food Exhaustion = 0.05`: combat touches supplies enough that food quality matters.
- cold hunger secondary effects stay off; temperature pressure should be solved with clothing/shelter/warm-cool foods, not hidden hunger acceleration.

Important clarification: the old `0.0` sprinting value meant **no extra LSO sprint drain**, not necessarily no vanilla hunger drain.

## Create kitchen direction

The intended solution layer is not "eat six random foods." It is:

- farms with multiple crops;
- Farmer's Delight kitchens;
- Create-powered cutting/mixing/filling lines;
- prepared meals in lunch baskets, Farmer's Delight baskets/crates, carts, trains, and depots;
- delivery routes to mines, oilfields, forts, airfields, and expeditions.

The Create progression datapack now has a broad heated-mixer bridge for Farmer's Delight cooking-pot meals, not just three token recipes. Each recipe consumes a bowl, 250 mB water, the normal FD/common ingredients, and heated Create mixing.

Examples include:

- `farmersdelight:beef_stew`
- `farmersdelight:chicken_soup`
- `farmersdelight:vegetable_soup`
- `farmersdelight:fish_stew`
- `farmersdelight:pumpkin_soup`
- `farmersdelight:noodle_soup`
- `farmersdelight:fried_rice`
- `farmersdelight:pasta_with_meatballs`
- `farmersdelight:vegetable_noodles`
- vanilla `mushroom_stew`, `beetroot_soup`, and `rabbit_stew`

`tools/check-create-progression.py` pins the full bridge so it stays heated, bowl/water-costed, one-meal-per-serving, and ingredient-anchored. `tools/check-pack-configs.py` pins the food script, LSO values, Supplementaries lunch basket config, and Farmer's Delight soup logistics config.

Smoke validation booted a disposable full NeoForge server to `Done (...)` for the earlier food-value pass. During the active-drain/variety iteration, full-pack smoke hit unrelated Create registry-startup fragility in one run, so the script now has its own reproducible focused runtime check: `tools/smoke-food-script.py` installs a minimal NeoForge server with CraftTweaker, Farmer's Delight, Supplementaries, and Moonlight, verifies CraftTweaker loads `tenpack_food_pressure.zs` with no errors, and requires the server to reach `Done (...)`. A local `public/tenpack-sync.py` file-URL smoke test also confirmed the rebuilt server manifest downloads `scripts/tenpack_food_pressure.zs`, the new food mods, and no SOL Onion remnants.

## Bread problem stance

A single wheat farm plus water pipe should be useful, but not the final answer to food forever.

Implemented answer:

1. AppleSkin makes food/saturation values visible.
2. CraftTweaker lowers the ceiling on bread, potatoes, raw crops, snacks, and one-ingredient cooked food.
3. CraftTweaker adds AFK-safe active drain plus lightweight staple/protein/produce balance pressure.
4. LSO makes sprinting/combat consume enough food that weak saturation matters.
5. Supplementaries lunch basket makes carrying prepared meals practical without adding backpacks.
6. Create heated-mixer recipes make scalable prepared meals an actual faction infrastructure goal.

## Playtest checklist

- [ ] Confirm CraftTweaker script loads on the dedicated server and clients receive it.
- [ ] Confirm standing still/AFK causes no Tenpack runtime drain.
- [ ] Compare active drain after staple-only, protein-only, staple+protein, and staple+protein+produce diets.
- [ ] Confirm hidden-quality Farmer's Delight meals produce a short satisfied/saturation pulse without becoming a combat steroid.
- [ ] Confirm AppleSkin shows nerfed bread/saturation and stronger meals.
- [ ] Confirm Supplementaries lunch basket open/eat/select behavior is understandable.
- [ ] Sprint/travel for 20 minutes on foot with bread only vs prepared meals.
- [ ] Do a mining/front trip with bread only vs basketed Farmer's Delight meals.
- [ ] Build a heated mixer meal line and confirm it is worth the infrastructure.
- [ ] Verify warm/cool Farmer's Delight food remains useful with LSO temperature.
- [ ] Tune values after server playtest if bread is still too comfortable or meals feel mandatory in an annoying way.
