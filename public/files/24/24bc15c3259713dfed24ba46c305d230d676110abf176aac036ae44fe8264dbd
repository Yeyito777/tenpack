// Tenpack food pressure pass.
//
// Goal: ROTN-like food presence without Nutrition categories or thirst.
// Simple, one-farm foods can keep you alive but have weak saturation, so
// sprinting/combat/travel chew through them. Cooked single-ingredient foods are
// useful field rations but no longer solve the whole pack. Bowled/plated meals
// keep their existing effects/components and get the high ceiling, making
// Farmer's Delight + Create kitchen automation a real progression goal.
//
// Runtime layer:
// - active/non-idle drain only; standing AFK has no Tenpack drain;
// - recent diet balance is tracked as broad staple/protein/produce points;
// - one-note food, e.g. bread-only or meat-only, increases active drain softly;
// - balanced food reduces active drain softly;
// - meal quality is hidden and immediate: real meals feel luxurious through a
//   short, non-combat-breaking Satisfaction pulse instead of a visible comfort meter.

import crafttweaker.api.entity.effect.MobEffectInstance;
import crafttweaker.api.item.IItemStack;
import crafttweaker.neoforge.api.event.entity.use.LivingEntityUseItemFinishEvent;
import crafttweaker.neoforge.api.event.tick.PlayerTickPostEvent;

// --- raw/basic crops and trivially automated staples ---
<item:minecraft:bread>.definition.food = <item:minecraft:bread>.food.withNutrition(3).withSaturation(0.2);
<item:minecraft:baked_potato>.definition.food = <item:minecraft:baked_potato>.food.withNutrition(3).withSaturation(0.3);
<item:minecraft:potato>.definition.food = <item:minecraft:potato>.food.withNutrition(1).withSaturation(0.1);
<item:minecraft:carrot>.definition.food = <item:minecraft:carrot>.food.withNutrition(2).withSaturation(0.2);
<item:minecraft:beetroot>.definition.food = <item:minecraft:beetroot>.food.withNutrition(1).withSaturation(0.2);
<item:minecraft:melon_slice>.definition.food = <item:minecraft:melon_slice>.food.withNutrition(1).withSaturation(0.1);
<item:minecraft:sweet_berries>.definition.food = <item:minecraft:sweet_berries>.food.withNutrition(1).withSaturation(0.1);
<item:minecraft:glow_berries>.definition.food = <item:minecraft:glow_berries>.food.withNutrition(1).withSaturation(0.1);
<item:minecraft:dried_kelp>.definition.food = <item:minecraft:dried_kelp>.food.withNutrition(1).withSaturation(0.1);
<item:minecraft:apple>.definition.food = <item:minecraft:apple>.food.withNutrition(3).withSaturation(0.2);
<item:minecraft:cookie>.definition.food = <item:minecraft:cookie>.food.withNutrition(1).withSaturation(0.1);
<item:minecraft:pumpkin_pie>.definition.food = <item:minecraft:pumpkin_pie>.food.withNutrition(5).withSaturation(0.3);
<item:minecraft:golden_carrot>.definition.food = <item:minecraft:golden_carrot>.food.withNutrition(5).withSaturation(0.5);

// Emergency raw animal foods. Chicken keeps its vanilla bad effect component.
<item:minecraft:beef>.definition.food = <item:minecraft:beef>.food.withNutrition(2).withSaturation(0.15);
<item:minecraft:porkchop>.definition.food = <item:minecraft:porkchop>.food.withNutrition(2).withSaturation(0.15);
<item:minecraft:mutton>.definition.food = <item:minecraft:mutton>.food.withNutrition(2).withSaturation(0.15);
<item:minecraft:chicken>.definition.food = <item:minecraft:chicken>.food.withNutrition(1).withSaturation(0.1);
<item:minecraft:cod>.definition.food = <item:minecraft:cod>.food.withNutrition(1).withSaturation(0.1);
<item:minecraft:salmon>.definition.food = <item:minecraft:salmon>.food.withNutrition(2).withSaturation(0.15);
<item:minecraft:rabbit>.definition.food = <item:minecraft:rabbit>.food.withNutrition(2).withSaturation(0.15);

// Farmer's Delight raw crops: edible in an emergency, poor as a real ration.
<item:farmersdelight:cabbage>.definition.food = <item:farmersdelight:cabbage>.food.withNutrition(2).withSaturation(0.25);
<item:farmersdelight:tomato>.definition.food = <item:farmersdelight:tomato>.food.withNutrition(1).withSaturation(0.2);
<item:farmersdelight:onion>.definition.food = <item:farmersdelight:onion>.food.withNutrition(1).withSaturation(0.2);
<item:farmersdelight:cabbage_leaf>.definition.food = <item:farmersdelight:cabbage_leaf>.food.withNutrition(1).withSaturation(0.15);
<item:farmersdelight:pumpkin_slice>.definition.food = <item:farmersdelight:pumpkin_slice>.food.withNutrition(1).withSaturation(0.1);
// Farmer's Delight rice, wheat dough, and raw pasta are ingredients rather than
// edible items in this version; do not assign food properties to null food data.

// --- single-ingredient cooked foods: decent rations, not permanent solutions ---
<item:minecraft:cooked_beef>.definition.food = <item:minecraft:cooked_beef>.food.withNutrition(6).withSaturation(0.5);
<item:minecraft:cooked_porkchop>.definition.food = <item:minecraft:cooked_porkchop>.food.withNutrition(6).withSaturation(0.5);
<item:minecraft:cooked_mutton>.definition.food = <item:minecraft:cooked_mutton>.food.withNutrition(5).withSaturation(0.5);
<item:minecraft:cooked_chicken>.definition.food = <item:minecraft:cooked_chicken>.food.withNutrition(5).withSaturation(0.45);
<item:minecraft:cooked_cod>.definition.food = <item:minecraft:cooked_cod>.food.withNutrition(4).withSaturation(0.4);
<item:minecraft:cooked_salmon>.definition.food = <item:minecraft:cooked_salmon>.food.withNutrition(5).withSaturation(0.45);
<item:farmersdelight:fried_egg>.definition.food = <item:farmersdelight:fried_egg>.food.withNutrition(3).withSaturation(0.35);
<item:farmersdelight:smoked_ham>.definition.food = <item:farmersdelight:smoked_ham>.food.withNutrition(8).withSaturation(0.55);
<item:farmersdelight:cooked_rice>.definition.food = <item:farmersdelight:cooked_rice>.food.withNutrition(4).withSaturation(0.35);
<item:farmersdelight:beef_patty>.definition.food = <item:farmersdelight:beef_patty>.food.withNutrition(5).withSaturation(0.55);
<item:farmersdelight:cooked_chicken_cuts>.definition.food = <item:farmersdelight:cooked_chicken_cuts>.food.withNutrition(4).withSaturation(0.45);
<item:farmersdelight:cooked_bacon>.definition.food = <item:farmersdelight:cooked_bacon>.food.withNutrition(4).withSaturation(0.5);
<item:farmersdelight:cooked_cod_slice>.definition.food = <item:farmersdelight:cooked_cod_slice>.food.withNutrition(3).withSaturation(0.35);
<item:farmersdelight:cooked_salmon_slice>.definition.food = <item:farmersdelight:cooked_salmon_slice>.food.withNutrition(4).withSaturation(0.45);
<item:farmersdelight:cooked_mutton_chops>.definition.food = <item:farmersdelight:cooked_mutton_chops>.food.withNutrition(4).withSaturation(0.5);

// --- snacks, desserts, drinks: flavor/utility, not primary expedition food ---
<item:farmersdelight:sweet_berry_cookie>.definition.food = <item:farmersdelight:sweet_berry_cookie>.food.withNutrition(1).withSaturation(0.15);
<item:farmersdelight:honey_cookie>.definition.food = <item:farmersdelight:honey_cookie>.food.withNutrition(1).withSaturation(0.15);
<item:farmersdelight:cake_slice>.definition.food = <item:farmersdelight:cake_slice>.food.withNutrition(2).withSaturation(0.2);
<item:farmersdelight:apple_pie_slice>.definition.food = <item:farmersdelight:apple_pie_slice>.food.withNutrition(3).withSaturation(0.35);
<item:farmersdelight:sweet_berry_cheesecake_slice>.definition.food = <item:farmersdelight:sweet_berry_cheesecake_slice>.food.withNutrition(3).withSaturation(0.35);
<item:farmersdelight:chocolate_pie_slice>.definition.food = <item:farmersdelight:chocolate_pie_slice>.food.withNutrition(3).withSaturation(0.35);
<item:farmersdelight:pumpkin_pie_slice>.definition.food = <item:farmersdelight:pumpkin_pie_slice>.food.withNutrition(3).withSaturation(0.35);
<item:farmersdelight:melon_popsicle>.definition.food = <item:farmersdelight:melon_popsicle>.food.withNutrition(2).withSaturation(0.2);
<item:farmersdelight:glow_berry_custard>.definition.food = <item:farmersdelight:glow_berry_custard>.food.withNutrition(4).withSaturation(0.45);
<item:farmersdelight:apple_cider>.definition.food = <item:farmersdelight:apple_cider>.food.withNutrition(1).withSaturation(0.1);
// Melon juice is not an edible/drinkable food item in this Farmer's Delight build.
// Hot cocoa and milk bottles are also utility/drink containers here, not food-bearing items.

// --- prepared foods: preserve existing effects/components, raise the payoff ---
<item:minecraft:mushroom_stew>.definition.food = <item:minecraft:mushroom_stew>.food.withNutrition(7).withSaturation(0.7);
<item:minecraft:beetroot_soup>.definition.food = <item:minecraft:beetroot_soup>.food.withNutrition(7).withSaturation(0.7);
<item:minecraft:rabbit_stew>.definition.food = <item:minecraft:rabbit_stew>.food.withNutrition(10).withSaturation(0.8);

<item:farmersdelight:beef_stew>.definition.food = <item:farmersdelight:beef_stew>.food.withNutrition(10).withSaturation(0.85);
<item:farmersdelight:chicken_soup>.definition.food = <item:farmersdelight:chicken_soup>.food.withNutrition(9).withSaturation(0.85);
<item:farmersdelight:vegetable_soup>.definition.food = <item:farmersdelight:vegetable_soup>.food.withNutrition(8).withSaturation(0.85);
<item:farmersdelight:fish_stew>.definition.food = <item:farmersdelight:fish_stew>.food.withNutrition(9).withSaturation(0.85);
<item:farmersdelight:pumpkin_soup>.definition.food = <item:farmersdelight:pumpkin_soup>.food.withNutrition(9).withSaturation(0.85);
<item:farmersdelight:noodle_soup>.definition.food = <item:farmersdelight:noodle_soup>.food.withNutrition(10).withSaturation(0.85);
<item:farmersdelight:onion_soup>.definition.food = <item:farmersdelight:onion_soup>.food.withNutrition(8).withSaturation(0.8);
<item:farmersdelight:fried_rice>.definition.food = <item:farmersdelight:fried_rice>.food.withNutrition(9).withSaturation(0.75);
<item:farmersdelight:ratatouille>.definition.food = <item:farmersdelight:ratatouille>.food.withNutrition(9).withSaturation(0.8);
<item:farmersdelight:pasta_with_meatballs>.definition.food = <item:farmersdelight:pasta_with_meatballs>.food.withNutrition(12).withSaturation(0.85);
<item:farmersdelight:pasta_with_mutton_chop>.definition.food = <item:farmersdelight:pasta_with_mutton_chop>.food.withNutrition(12).withSaturation(0.85);
<item:farmersdelight:steak_and_potatoes>.definition.food = <item:farmersdelight:steak_and_potatoes>.food.withNutrition(12).withSaturation(0.85);
<item:farmersdelight:vegetable_noodles>.definition.food = <item:farmersdelight:vegetable_noodles>.food.withNutrition(10).withSaturation(0.8);
<item:farmersdelight:mushroom_rice>.definition.food = <item:farmersdelight:mushroom_rice>.food.withNutrition(9).withSaturation(0.75);
<item:farmersdelight:baked_cod_stew>.definition.food = <item:farmersdelight:baked_cod_stew>.food.withNutrition(10).withSaturation(0.85);
<item:farmersdelight:bone_broth>.definition.food = <item:farmersdelight:bone_broth>.food.withNutrition(7).withSaturation(0.7);
<item:farmersdelight:squid_ink_pasta>.definition.food = <item:farmersdelight:squid_ink_pasta>.food.withNutrition(12).withSaturation(0.85);
<item:farmersdelight:fruit_salad>.definition.food = <item:farmersdelight:fruit_salad>.food.withNutrition(5).withSaturation(0.55);
<item:farmersdelight:mixed_salad>.definition.food = <item:farmersdelight:mixed_salad>.food.withNutrition(5).withSaturation(0.55);
<item:farmersdelight:nether_salad>.definition.food = <item:farmersdelight:nether_salad>.food.withNutrition(5).withSaturation(0.55);
<item:farmersdelight:barbecue_stick>.definition.food = <item:farmersdelight:barbecue_stick>.food.withNutrition(7).withSaturation(0.65);
<item:farmersdelight:dumplings>.definition.food = <item:farmersdelight:dumplings>.food.withNutrition(6).withSaturation(0.55);
<item:farmersdelight:cabbage_rolls>.definition.food = <item:farmersdelight:cabbage_rolls>.food.withNutrition(5).withSaturation(0.45);
<item:farmersdelight:kelp_roll>.definition.food = <item:farmersdelight:kelp_roll>.food.withNutrition(5).withSaturation(0.45);
<item:farmersdelight:kelp_roll_slice>.definition.food = <item:farmersdelight:kelp_roll_slice>.food.withNutrition(2).withSaturation(0.25);
<item:farmersdelight:cod_roll>.definition.food = <item:farmersdelight:cod_roll>.food.withNutrition(6).withSaturation(0.5);
<item:farmersdelight:salmon_roll>.definition.food = <item:farmersdelight:salmon_roll>.food.withNutrition(7).withSaturation(0.55);
<item:farmersdelight:egg_sandwich>.definition.food = <item:farmersdelight:egg_sandwich>.food.withNutrition(7).withSaturation(0.65);
<item:farmersdelight:chicken_sandwich>.definition.food = <item:farmersdelight:chicken_sandwich>.food.withNutrition(8).withSaturation(0.7);
<item:farmersdelight:bacon_sandwich>.definition.food = <item:farmersdelight:bacon_sandwich>.food.withNutrition(8).withSaturation(0.7);
<item:farmersdelight:mutton_wrap>.definition.food = <item:farmersdelight:mutton_wrap>.food.withNutrition(8).withSaturation(0.7);
<item:farmersdelight:hamburger>.definition.food = <item:farmersdelight:hamburger>.food.withNutrition(9).withSaturation(0.75);
<item:farmersdelight:bacon_and_eggs>.definition.food = <item:farmersdelight:bacon_and_eggs>.food.withNutrition(10).withSaturation(0.8);
<item:farmersdelight:grilled_salmon>.definition.food = <item:farmersdelight:grilled_salmon>.food.withNutrition(10).withSaturation(0.8);
<item:farmersdelight:roasted_mutton_chops>.definition.food = <item:farmersdelight:roasted_mutton_chops>.food.withNutrition(10).withSaturation(0.8);
<item:farmersdelight:stuffed_potato>.definition.food = <item:farmersdelight:stuffed_potato>.food.withNutrition(8).withSaturation(0.7);
<item:farmersdelight:roast_chicken>.definition.food = <item:farmersdelight:roast_chicken>.food.withNutrition(12).withSaturation(0.9);
<item:farmersdelight:honey_glazed_ham>.definition.food = <item:farmersdelight:honey_glazed_ham>.food.withNutrition(12).withSaturation(0.9);
<item:farmersdelight:shepherds_pie>.definition.food = <item:farmersdelight:shepherds_pie>.food.withNutrition(12).withSaturation(0.9);
<item:farmersdelight:stuffed_pumpkin>.definition.food = <item:farmersdelight:stuffed_pumpkin>.food.withNutrition(12).withSaturation(0.9);
<item:farmersdelight:gleaming_salad>.definition.food = <item:farmersdelight:gleaming_salad>.food.withNutrition(10).withSaturation(0.8);

// --- active drain, lightweight diet balance, and hidden meal quality ---
// Runtime maps intentionally reset on server restart. This is a session pressure
// loop, not permanent Nutrition data.  Only staple/protein/produce are tracked;
// meal quality is a hidden per-food immediate reward, not a fourth checklist.
var tenpackTickBucket as int[string] = {};
var tenpackLastX as double[string] = {};
var tenpackLastY as double[string] = {};
var tenpackLastZ as double[string] = {};
var tenpackDietStaple as int[string] = {};
var tenpackDietProtein as int[string] = {};
var tenpackDietProduce as int[string] = {};
var tenpackDietDecayClock as int[string] = {};

// Food-group points are intentionally broad.  They answer "have you eaten a
// sane mix lately?" rather than "did you hit exact nutrient categories?".
// Cheap staples add only staple.  Cooked meats add only protein.  Real meals
// often add multiple groups, so Farmer's Delight/Create kitchens solve the
// pressure naturally.
var tenpackFoodStaple as int[string] = {
    "minecraft:bread": 8,
    "minecraft:baked_potato": 7,
    "minecraft:potato": 4,
    "minecraft:cookie": 2,
    "minecraft:pumpkin_pie": 5,
    "farmersdelight:cooked_rice": 8,
    "farmersdelight:sweet_berry_cookie": 2,
    "farmersdelight:honey_cookie": 2,
    "farmersdelight:cake_slice": 3,
    "farmersdelight:apple_pie_slice": 4,
    "farmersdelight:sweet_berry_cheesecake_slice": 4,
    "farmersdelight:chocolate_pie_slice": 4,
    "farmersdelight:pumpkin_pie_slice": 4,
    "minecraft:rabbit_stew": 6,
    "farmersdelight:noodle_soup": 8,
    "farmersdelight:fried_rice": 10,
    "farmersdelight:pasta_with_meatballs": 12,
    "farmersdelight:pasta_with_mutton_chop": 12,
    "farmersdelight:steak_and_potatoes": 10,
    "farmersdelight:vegetable_noodles": 10,
    "farmersdelight:mushroom_rice": 10,
    "farmersdelight:squid_ink_pasta": 12,
    "farmersdelight:dumplings": 5,
    "farmersdelight:kelp_roll": 4,
    "farmersdelight:kelp_roll_slice": 2,
    "farmersdelight:cod_roll": 4,
    "farmersdelight:salmon_roll": 4,
    "farmersdelight:egg_sandwich": 8,
    "farmersdelight:chicken_sandwich": 8,
    "farmersdelight:bacon_sandwich": 8,
    "farmersdelight:mutton_wrap": 8,
    "farmersdelight:hamburger": 8,
    "farmersdelight:stuffed_potato": 9,
    "farmersdelight:shepherds_pie": 10,
    "farmersdelight:stuffed_pumpkin": 6,
    "create_bic_bit:frikandel_sandwich": 8,
    "create_bic_bit:kroket_sandwich": 8,
    "create_bic_bit:ketchup_topped_frikandel_sandwich": 8,
    "create_bic_bit:ketchup_topped_kroket_sandwich": 8,
    "create_bic_bit:mayonnaise_topped_frikandel_sandwich": 8,
    "create_bic_bit:mayonnaise_topped_kroket_sandwich": 8,
    "create_bic_bit:mayonnaise_ketchup_topped_frikandel_sandwich": 8,
    "create_bic_bit:mayonnaise_ketchup_topped_kroket_sandwich": 8,
    "create_bic_bit:stamppot_bowl": 10,
    "create_bic_bit:chocolate_glazed_stroopwafel": 4,
    "create_bic_bit:wrapped_chocolate_glazed_stroopwafel": 4,
    "create_bic_bit:coated_churros": 4,
    "create_bic_bit:wrapped_coated_churros": 4,
};

var tenpackFoodProtein as int[string] = {
    "minecraft:beef": 3,
    "minecraft:porkchop": 3,
    "minecraft:mutton": 3,
    "minecraft:chicken": 2,
    "minecraft:cod": 2,
    "minecraft:salmon": 3,
    "minecraft:rabbit": 3,
    "minecraft:cooked_beef": 10,
    "minecraft:cooked_porkchop": 10,
    "minecraft:cooked_mutton": 9,
    "minecraft:cooked_chicken": 8,
    "minecraft:cooked_cod": 7,
    "minecraft:cooked_salmon": 8,
    "farmersdelight:fried_egg": 5,
    "farmersdelight:smoked_ham": 11,
    "farmersdelight:beef_patty": 8,
    "farmersdelight:cooked_chicken_cuts": 7,
    "farmersdelight:cooked_bacon": 7,
    "farmersdelight:cooked_cod_slice": 5,
    "farmersdelight:cooked_salmon_slice": 6,
    "farmersdelight:cooked_mutton_chops": 7,
    "minecraft:rabbit_stew": 8,
    "farmersdelight:beef_stew": 12,
    "farmersdelight:chicken_soup": 10,
    "farmersdelight:fish_stew": 12,
    "farmersdelight:noodle_soup": 4,
    "farmersdelight:pasta_with_meatballs": 12,
    "farmersdelight:pasta_with_mutton_chop": 12,
    "farmersdelight:steak_and_potatoes": 14,
    "farmersdelight:baked_cod_stew": 14,
    "farmersdelight:bone_broth": 8,
    "farmersdelight:squid_ink_pasta": 10,
    "farmersdelight:barbecue_stick": 8,
    "farmersdelight:dumplings": 4,
    "farmersdelight:cabbage_rolls": 4,
    "farmersdelight:cod_roll": 8,
    "farmersdelight:salmon_roll": 9,
    "farmersdelight:egg_sandwich": 5,
    "farmersdelight:chicken_sandwich": 8,
    "farmersdelight:bacon_sandwich": 8,
    "farmersdelight:mutton_wrap": 8,
    "farmersdelight:hamburger": 9,
    "farmersdelight:bacon_and_eggs": 12,
    "farmersdelight:grilled_salmon": 13,
    "farmersdelight:roasted_mutton_chops": 13,
    "farmersdelight:roast_chicken": 14,
    "farmersdelight:honey_glazed_ham": 14,
    "farmersdelight:shepherds_pie": 10,
    "create_bic_bit:aged_cheese_wedge": 7,
    "create_bic_bit:young_cheese_wedge": 6,
    "create_bic_bit:unripe_cheese_wedge": 4,
    "create_bic_bit:bitterballen": 8,
    "create_bic_bit:cheese_souffle": 8,
    "create_bic_bit:frikandel": 8,
    "create_bic_bit:kroket": 8,
    "create_bic_bit:frikandel_sandwich": 8,
    "create_bic_bit:kroket_sandwich": 8,
    "create_bic_bit:ketchup_topped_frikandel_sandwich": 8,
    "create_bic_bit:ketchup_topped_kroket_sandwich": 8,
    "create_bic_bit:mayonnaise_topped_frikandel_sandwich": 8,
    "create_bic_bit:mayonnaise_topped_kroket_sandwich": 8,
    "create_bic_bit:mayonnaise_ketchup_topped_frikandel_sandwich": 8,
    "create_bic_bit:mayonnaise_ketchup_topped_kroket_sandwich": 8,
    "create_bic_bit:stamppot_bowl": 5,
};

var tenpackFoodProduce as int[string] = {
    "minecraft:carrot": 5,
    "minecraft:beetroot": 4,
    "minecraft:melon_slice": 3,
    "minecraft:sweet_berries": 3,
    "minecraft:glow_berries": 3,
    "minecraft:dried_kelp": 2,
    "minecraft:apple": 5,
    "minecraft:golden_carrot": 6,
    "minecraft:pumpkin_pie": 2,
    "farmersdelight:cabbage": 5,
    "farmersdelight:tomato": 5,
    "farmersdelight:onion": 4,
    "farmersdelight:cabbage_leaf": 3,
    "farmersdelight:pumpkin_slice": 4,
    "farmersdelight:apple_pie_slice": 3,
    "farmersdelight:sweet_berry_cheesecake_slice": 3,
    "farmersdelight:pumpkin_pie_slice": 3,
    "farmersdelight:melon_popsicle": 4,
    "farmersdelight:glow_berry_custard": 5,
    "farmersdelight:apple_cider": 5,
    "minecraft:mushroom_stew": 8,
    "minecraft:beetroot_soup": 10,
    "minecraft:rabbit_stew": 6,
    "farmersdelight:beef_stew": 8,
    "farmersdelight:chicken_soup": 8,
    "farmersdelight:vegetable_soup": 14,
    "farmersdelight:fish_stew": 6,
    "farmersdelight:pumpkin_soup": 12,
    "farmersdelight:noodle_soup": 6,
    "farmersdelight:onion_soup": 12,
    "farmersdelight:fried_rice": 5,
    "farmersdelight:ratatouille": 16,
    "farmersdelight:pasta_with_meatballs": 4,
    "farmersdelight:pasta_with_mutton_chop": 4,
    "farmersdelight:vegetable_noodles": 10,
    "farmersdelight:mushroom_rice": 8,
    "farmersdelight:baked_cod_stew": 6,
    "farmersdelight:fruit_salad": 12,
    "farmersdelight:mixed_salad": 12,
    "farmersdelight:nether_salad": 8,
    "farmersdelight:barbecue_stick": 3,
    "farmersdelight:dumplings": 2,
    "farmersdelight:cabbage_rolls": 8,
    "farmersdelight:kelp_roll": 5,
    "farmersdelight:kelp_roll_slice": 2,
    "farmersdelight:chicken_sandwich": 3,
    "farmersdelight:bacon_sandwich": 3,
    "farmersdelight:mutton_wrap": 3,
    "farmersdelight:hamburger": 4,
    "farmersdelight:stuffed_potato": 5,
    "farmersdelight:shepherds_pie": 6,
    "farmersdelight:stuffed_pumpkin": 14,
    "farmersdelight:gleaming_salad": 16,
    "create_winery:red_grapes": 4,
    "create_winery:white_grapes": 4,
    "create_winery:apple_juice": 5,
    "create_winery:grape_juice": 5,
    "create_winery:cider": 3,
    "create_winery:bordeaux": 2,
    "create_winery:cabernet_sauvignon": 2,
    "create_winery:champaign": 2,
    "create_winery:chardonnay": 2,
    "create_winery:merlot": 2,
    "create_winery:pinot_noir": 2,
    "create_winery:riesling": 2,
    "create_winery:rose": 2,
    "create_winery:zinfandel": 2,
    "create_confectionery:black_chocolate_glazed_berries": 2,
    "create_confectionery:caramel_glazed_berries": 2,
    "create_confectionery:ruby_chocolate_glazed_berries": 2,
    "create_confectionery:white_chocolate_glazed_berries": 2,
};

// Hidden meal quality is not diet memory.  It only asks whether the thing just
// eaten was a cool/complex/comforting meal and grants a short saturation pulse.
var tenpackMealQuality as int[string] = {
    "minecraft:golden_carrot": 1,
    "farmersdelight:apple_pie_slice": 1,
    "farmersdelight:sweet_berry_cheesecake_slice": 1,
    "farmersdelight:chocolate_pie_slice": 1,
    "farmersdelight:pumpkin_pie_slice": 1,
    "farmersdelight:glow_berry_custard": 1,
    "minecraft:mushroom_stew": 2,
    "minecraft:beetroot_soup": 2,
    "minecraft:rabbit_stew": 3,
    "farmersdelight:bone_broth": 2,
    "farmersdelight:fruit_salad": 2,
    "farmersdelight:mixed_salad": 2,
    "farmersdelight:nether_salad": 2,
    "farmersdelight:barbecue_stick": 2,
    "farmersdelight:dumplings": 2,
    "farmersdelight:cabbage_rolls": 2,
    "farmersdelight:kelp_roll": 2,
    "farmersdelight:cod_roll": 2,
    "farmersdelight:salmon_roll": 2,
    "farmersdelight:egg_sandwich": 2,
    "farmersdelight:beef_stew": 4,
    "farmersdelight:chicken_soup": 4,
    "farmersdelight:vegetable_soup": 4,
    "farmersdelight:fish_stew": 4,
    "farmersdelight:pumpkin_soup": 4,
    "farmersdelight:noodle_soup": 4,
    "farmersdelight:onion_soup": 4,
    "farmersdelight:fried_rice": 4,
    "farmersdelight:ratatouille": 4,
    "farmersdelight:vegetable_noodles": 4,
    "farmersdelight:mushroom_rice": 4,
    "farmersdelight:chicken_sandwich": 3,
    "farmersdelight:bacon_sandwich": 3,
    "farmersdelight:mutton_wrap": 3,
    "farmersdelight:hamburger": 3,
    "farmersdelight:stuffed_potato": 3,
    "farmersdelight:pasta_with_meatballs": 5,
    "farmersdelight:pasta_with_mutton_chop": 5,
    "farmersdelight:steak_and_potatoes": 5,
    "farmersdelight:baked_cod_stew": 5,
    "farmersdelight:squid_ink_pasta": 5,
    "farmersdelight:bacon_and_eggs": 5,
    "farmersdelight:grilled_salmon": 5,
    "farmersdelight:roasted_mutton_chops": 5,
    "farmersdelight:roast_chicken": 5,
    "farmersdelight:honey_glazed_ham": 5,
    "farmersdelight:shepherds_pie": 5,
    "farmersdelight:stuffed_pumpkin": 5,
    "farmersdelight:gleaming_salad": 5,
    "create_bic_bit:aged_cheese_wedge": 2,
    "create_bic_bit:young_cheese_wedge": 1,
    "create_bic_bit:bitterballen": 3,
    "create_bic_bit:cheese_souffle": 3,
    "create_bic_bit:frikandel": 2,
    "create_bic_bit:kroket": 2,
    "create_bic_bit:frikandel_sandwich": 3,
    "create_bic_bit:kroket_sandwich": 3,
    "create_bic_bit:ketchup_topped_frikandel_sandwich": 3,
    "create_bic_bit:ketchup_topped_kroket_sandwich": 3,
    "create_bic_bit:mayonnaise_topped_frikandel_sandwich": 3,
    "create_bic_bit:mayonnaise_topped_kroket_sandwich": 3,
    "create_bic_bit:mayonnaise_ketchup_topped_frikandel_sandwich": 4,
    "create_bic_bit:mayonnaise_ketchup_topped_kroket_sandwich": 4,
    "create_bic_bit:stamppot_bowl": 4,
    "create_bic_bit:chocolate_glazed_stroopwafel": 1,
    "create_bic_bit:wrapped_chocolate_glazed_stroopwafel": 1,
    "create_bic_bit:coated_churros": 1,
    "create_bic_bit:wrapped_coated_churros": 1,
    "create_confectionery:hot_chocolate_bottle": 2,
    "create_confectionery:soothing_hot_chocolate": 2,
    "create_confectionery:bar_of_black_chocolate": 1,
    "create_confectionery:bar_of_caramel": 1,
    "create_confectionery:bar_of_ruby_chocolate": 1,
    "create_confectionery:bar_of_white_chocolate": 1,
    "create_confectionery:full_black_chocolate_bar": 1,
    "create_confectionery:full_chocolate_bar": 1,
    "create_confectionery:full_ruby_chocolate_bar": 1,
    "create_confectionery:full_white_chocolate_bar": 1,
    "create_winery:apple_juice": 1,
    "create_winery:grape_juice": 1,
    "create_winery:cider": 2,
    "create_winery:bordeaux": 2,
    "create_winery:cabernet_sauvignon": 2,
    "create_winery:champaign": 2,
    "create_winery:chardonnay": 2,
    "create_winery:merlot": 2,
    "create_winery:pinot_noir": 2,
    "create_winery:riesling": 2,
    "create_winery:rose": 2,
    "create_winery:zinfandel": 2,
};

events.register<PlayerTickPostEvent>((event) => {
    val player = event.entity;
    val id = player.stringUUID;

    if !(id in tenpackTickBucket) {
        tenpackTickBucket[id] = 0;
        tenpackLastX[id] = player.x;
        tenpackLastY[id] = player.y;
        tenpackLastZ[id] = player.z;
        tenpackDietStaple[id] = 0;
        tenpackDietProtein[id] = 0;
        tenpackDietProduce[id] = 0;
        tenpackDietDecayClock[id] = 0;
    }

    tenpackTickBucket[id] = tenpackTickBucket[id] + 1;
    if tenpackTickBucket[id] < 20 {
        return;
    }
    tenpackTickBucket[id] = 0;

    val dx = player.x - tenpackLastX[id];
    val dy = player.y - tenpackLastY[id];
    val dz = player.z - tenpackLastZ[id];
    val movedSqr = dx * dx + dy * dy + dz * dz;
    tenpackLastX[id] = player.x;
    tenpackLastY[id] = player.y;
    tenpackLastZ[id] = player.z;

    if player.isCreative || player.isSpectator {
        return;
    }

    var baseDrain as double = 0.0;
    if movedSqr > 0.0004 {
        baseDrain = baseDrain + 0.004;
    }
    if player.isPassenger {
        baseDrain = baseDrain + 0.001;
    }
    if player.isUsingItem {
        baseDrain = baseDrain + 0.002;
    }
    if player.isSprinting || player.isSwimming {
        baseDrain = baseDrain + 0.008;
    }
    if baseDrain > 0.018 {
        baseDrain = 0.018;
    }
    if baseDrain <= 0.0 {
        return;
    }

    // Diet memory decays only while active, so AFK standing neither drains food nor
    // erases meal prep.  At one point every eight active seconds, a balanced meal
    // lasts long enough to matter without becoming a permanent buff.
    tenpackDietDecayClock[id] = tenpackDietDecayClock[id] + 1;
    if tenpackDietDecayClock[id] >= 8 {
        tenpackDietDecayClock[id] = 0;
        if tenpackDietStaple[id] > 0 { tenpackDietStaple[id] = tenpackDietStaple[id] - 1; }
        if tenpackDietProtein[id] > 0 { tenpackDietProtein[id] = tenpackDietProtein[id] - 1; }
        if tenpackDietProduce[id] > 0 { tenpackDietProduce[id] = tenpackDietProduce[id] - 1; }
    }

    var groups as int = 0;
    var robustGroups as int = 0;
    if tenpackDietStaple[id] >= 10 { groups = groups + 1; }
    if tenpackDietProtein[id] >= 10 { groups = groups + 1; }
    if tenpackDietProduce[id] >= 10 { groups = groups + 1; }
    if tenpackDietStaple[id] >= 28 { robustGroups = robustGroups + 1; }
    if tenpackDietProtein[id] >= 28 { robustGroups = robustGroups + 1; }
    if tenpackDietProduce[id] >= 28 { robustGroups = robustGroups + 1; }

    var multiplier as double = 1.4;
    if groups == 1 {
        multiplier = 1.28;
    } else if groups == 2 {
        multiplier = 1.08;
    } else if groups >= 3 {
        multiplier = 0.92;
    }
    if robustGroups >= 3 {
        multiplier = 0.8;
    }

    player.foodData.addExhaustion((baseDrain * multiplier) as float);
});

events.register<LivingEntityUseItemFinishEvent>((event) => {
    // This event can fire for non-player living entities too.  Avoid brittle
    // ZenScript player casts/null compares by only accepting the vanilla player
    // entity type, then using LivingEntity-safe operations below.
    if (event.entity.type.registryName as string) != "minecraft:player" {
        return;
    }
    val living = event.entity;

    val id = living.stringUUID;
    if !(id in tenpackTickBucket) {
        tenpackTickBucket[id] = 0;
        tenpackLastX[id] = living.x;
        tenpackLastY[id] = living.y;
        tenpackLastZ[id] = living.z;
        tenpackDietStaple[id] = 0;
        tenpackDietProtein[id] = 0;
        tenpackDietProduce[id] = 0;
        tenpackDietDecayClock[id] = 0;
    }

    // Track what the use-item event reports as eaten.  Do not inspect the
    // Supplementaries lunch basket internals here: its carried contents live in a
    // mod-specific data component, and food balance should reward actual
    // consumption rather than hoarding items in a container.
    val eaten = (event.item as IItemStack).registryName as string;

    var stapleGain as int = 0;
    var proteinGain as int = 0;
    var produceGain as int = 0;
    var mealQuality as int = 0;
    if eaten in tenpackFoodStaple { stapleGain = tenpackFoodStaple[eaten]; }
    if eaten in tenpackFoodProtein { proteinGain = tenpackFoodProtein[eaten]; }
    if eaten in tenpackFoodProduce { produceGain = tenpackFoodProduce[eaten]; }
    if eaten in tenpackMealQuality { mealQuality = tenpackMealQuality[eaten]; }

    if stapleGain <= 0 && proteinGain <= 0 && produceGain <= 0 && mealQuality <= 0 {
        return;
    }

    tenpackDietStaple[id] = tenpackDietStaple[id] + stapleGain;
    tenpackDietProtein[id] = tenpackDietProtein[id] + proteinGain;
    tenpackDietProduce[id] = tenpackDietProduce[id] + produceGain;
    if tenpackDietStaple[id] > 80 { tenpackDietStaple[id] = 80; }
    if tenpackDietProtein[id] > 80 { tenpackDietProtein[id] = 80; }
    if tenpackDietProduce[id] > 80 { tenpackDietProduce[id] = 80; }

    var groups as int = 0;
    var robustGroups as int = 0;
    if tenpackDietStaple[id] >= 10 { groups = groups + 1; }
    if tenpackDietProtein[id] >= 10 { groups = groups + 1; }
    if tenpackDietProduce[id] >= 10 { groups = groups + 1; }
    if tenpackDietStaple[id] >= 28 { robustGroups = robustGroups + 1; }
    if tenpackDietProtein[id] >= 28 { robustGroups = robustGroups + 1; }
    if tenpackDietProduce[id] >= 28 { robustGroups = robustGroups + 1; }

    var duration as int = 0;
    if mealQuality >= 5 {
        duration = 1500;
    } else if mealQuality == 4 {
        duration = 900;
    } else if mealQuality == 3 {
        duration = 500;
    } else if mealQuality == 2 {
        duration = 220;
    }

    if duration > 0 {
        if groups <= 1 {
            duration = (duration * 70) / 100;
        } else if groups == 3 {
            duration = (duration * 125) / 100;
        }
        if robustGroups >= 3 {
            duration = (duration * 150) / 100;
        }
        living.addEffect(MobEffectInstance.of(<mobeffect:minecraft:saturation>, duration, 0, false, true, true, null));
    }
});
