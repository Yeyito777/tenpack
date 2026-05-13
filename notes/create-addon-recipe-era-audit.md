# Create addon recipe era audit

Date: 2026-05-09

Purpose: inspect the actual installed Create addon jars for recipe-level progression issues after the Create workflow was turned into eras.

This pass looked directly inside the server-side jars and added `tools/check-create-addon-recipes.py` so the most important findings become repeatable guardrails.

Command:

```bash
./tools/check-create-addon-recipes.py
```

Current result:

- 3,113 Create-addon recipe JSON files checked, including nested recipes inside the Aeronautics/Simulated/Offroad bundled jar
- per-jar and total recipe counts are pinned in
  `tools/check-create-addon-recipes.py`, so addon upgrades, removed addons, stale
  count guardrails, or nested-scan regressions force a new audit instead of
  silently inheriting old conclusions
- no installed addon recipe outputs a controlled Create progression item
- audited utility recipes still reference their intended era anchors
- Tenpack's progression datapack checker separately validates hand-authored
  shaped override basics, optional-mod conditions, advancement references, and
  the new Create-kitchen bridge so malformed overrides do not silently fall back
  to upstream addon recipes

## What counts as a controlled progression item

The checker treats the following as controlled by Tenpack's progression datapack and therefore disallows casual addon recipes that output them:

- workshop machines: mechanical press, mixer, saw, drill, encased fan, deployer, mechanical roller
- contraption machines: bearing, piston, gantry, harvester, plough
- portable interfaces, spout, item drain
- steam engine, elevator pulley
- train station/signal/observer
- precision mechanism, electron tube, brass hand, mechanical crafter
- package logistics: packager, package frogport, stock link, factory gauge, redstone requester
- rotation speed controller, mechanical arm, controls
- blaze burner

Allowed harmless Create-namespace compat outputs are explicitly listed:

- `create:copycat_panel`
- `create:copycat_step`
- `create:brass_nugget`
- `create:crushed_raw_iron`
- `create:industrial_iron_block`
- `create:iron_sheet`
- `create:placard`
- `create:white_sail`
- `create:chocolate`

If a future addon outputs another `create:*` item, the checker fails until that item is audited.

The checker now descends into nested jars, which matters for
`create-aeronautics-bundled-1.21.1-1.2.1.jar`. That makes the secondary
Aeronautics/Simulated/Offroad recipe audit repeatable instead of relying on a
one-off unzip pass.

Current extra Aeronautics-bundle invariants:

- `simulated:gimbal_sensor` stays behind `simulated:gyroscopic_mechanism`.
- `simulated:optical_sensor` keeps `create:electron_tube` and `create:brass_casing`.
- `simulated:red_portable_engine` keeps `simulated:engine_assembly`.
- `offroad:rockcutting_wheel` keeps `create:crushing_wheel`.
- `aeronautics:andesite_propeller` keeps `create:propeller`.

## Addon-by-addon findings

### Steam 'n' Rails

Installed jar:

- `railways-0.2.0-beta.2+neoforge-mc1.21.1.jar`

Recipe count in scan: 825.

Important audited recipes:

- `railways:handcar` requires `create:contraption_controls`
- `railways:fuel_tank` requires `create:fluid_tank` and `create:sturdy_sheet`
- `railways:semaphore` requires `create:electron_tube`
- `railways:track_switch_brass` requires `create:precision_mechanism`
- `railways:remote_lens` requires `create:precision_mechanism`

Era conclusion:

- mostly rail cosmetics, palettes, buffers, and train identity;
- handcar is not day-zero because it inherits the contraption-controls/electron-tube path;
- advanced rail control stays at precision/brass;
- fuel tanks depend on fluid/storage infrastructure.

Playability/accessibility:

- good fit for faction train identity and visible logistics;
- big JEI footprint from palettes/dyes/stonecutting, but mostly cosmetic and grouped around train building.

Risk:

- beta-version runtime stability still needs a launch/multiplayer train test.

### Create Railways Navigator

Installed jar:

- `createrailwaysnavigator-neoforge-1.21.1-beta-0.9.0-C6.jar`

Recipe count in scan: 9.

Important audited recipes:

- navigator requires `create:precision_mechanism`
- train station clock requires `create:precision_mechanism`
- advanced display requires `create:display_board`

Era conclusion:

- correctly sits with precision-era rail networks;
- does not make early rail logistics too easy;
- helps players navigate shared faction rails once rail infrastructure exists.

Playability/accessibility:

- likely very helpful for newcomers if it launches cleanly;
- should be mentioned in the rail guide/quest text as a way to understand routes.

### Copycats+

Installed jar:

- `copycats-3.0.4+mc.1.21.1-neoforge.jar`

Recipe count in scan: 68.

Important audited recipes:

- copycat cogwheel requires the real `create:cogwheel` plus zinc
- copycat large cogwheel requires the real `create:large_cogwheel` plus zinc
- copycat fluid pipe requires the real `create:fluid_pipe` plus zinc

Era conclusion:

- decorative/architectural copycat parts do not bypass base Create unlocks;
- functional-looking parts upgrade from real base parts instead of replacing them.

Playability/accessibility:

- strong faction-architecture fit;
- low progression risk.

### Create Deco

Installed jar:

- `createdeco-2.1.3.jar`

Recipe count in scan: 1,719.

Important notes:

- huge JEI footprint, but overwhelmingly decoration, palettes, doors, catwalks, metal blocks, coins, hulls, windows, lamps, and train/factory styling;
- outputs `create:industrial_iron_block` and `create:placard`, both allowed as harmless Create compat/decor outputs.

Era conclusion:

- belongs across all eras as faction identity/building infrastructure;
- does not output controlled machines or progression items.

Playability/accessibility:

- high build value, but high JEI noise;
- worth keeping because faction infrastructure needs visual language.

Risk:

- if players complain about JEI clutter, Create Deco is one source, but it is not a progression bypass.

### Bells & Whistles

Installed jar:

- `bellsandwhistles-0.4.7-1.21.1.jar`

Recipe count in scan: 53.

Important notes:

- train/station/metro/pilot/grab-rail decorative parts;
- uses appropriate material cues: andesite, copper, brass, iron sheets/nuggets.

Era conclusion:

- rail/station architecture support;
- no controlled Create outputs.

Playability/accessibility:

- good faction/station identity;
- low progression risk.

### Rechiseled: Create

Installed jar:

- `rechiseledcreate-1.1.0-neoforge-mc1.21.jar`

Recipe count in scan: 1.

Era conclusion:

- purely decorative compatibility footprint;
- low risk.

### Create: Central Kitchen

Installed jar:

- `create-central-kitchen-2.4.0.jar`

Recipe count in scan: bundled/conditional integration recipes did not show direct server recipe JSON in the same way as the other addons during this scan.

Era conclusion:

- integration support for Create + Farmer's Delight ecosystem;
- should be evaluated in JEI/playtest rather than only by static recipe files.

Playability/accessibility:

- supports kitchen/farm social spaces;
- current food stack is already big enough, so no more food addons until playtested.

### Slice & Dice

Installed jar:

- `sliceanddice-forge-4.2.4.jar`

Recipe count in scan: 8.

Important audited recipe:

- `sliceanddice:slicer` requires `create:andesite_casing`, `create:cogwheel`, and `create:turntable`

Era conclusion:

- early kinetic/farm workshop addon;
- acceptable because it supports food/farm automation rather than skipping precision, oil, rails, or flight.

Playability/accessibility:

- should be easy for farmers/builders to understand once they know basic Create motion.

### Create Confectionery

Installed jar:

- `create-confectionery1.21.1_v1.1.2.jar`

Recipe count in scan: 115.

Important notes:

- outputs `create:chocolate` via compat recipes, allowed as harmless food compatibility;
- otherwise food/drink/confectionery ecosystem.

Era conclusion:

- culture/trade-goods addon;
- not a hard progression pillar.

Risk:

- JEI clutter/food bloat, not progression bypass.

### Create: Winery

Installed jar:

- `create_winery-2.0.2-neoforge-1.21.1.jar`

Recipe count in scan: 28.

Era conclusion:

- tavern/farm/trade flavor;
- no controlled Create outputs.

Playability/accessibility:

- likely good for faction culture and markets if not made mandatory.

### Create: Bitterballen

Installed jar:

- `create_bic_bit-1.0.2C.jar`

Recipe count in scan: 113.

Era conclusion:

- funny/culture food addon;
- not a progression pillar.

Risk:

- mostly JEI clutter and novelty; keep through first playtest only if players enjoy it.

### Create: Dragons Plus

Installed jar:

- `CreateDragonsPlus-1.10.0b.jar`

Recipe count in scan: 49.

Important audited recipes/config:

- `fluid_hatch` requires `create:item_drain`, so it inherits the fluid era rather than bypassing it.
- `levitite_fragile_fluid_tank` requires `aeronautics:levitite_blend_bucket`, so it is Aeronautics-linked.
- `item/blaze_upgrade_smithing_template` stays disabled in `tenpack-specs/overrides/config/create_dragons_plus-common.toml`.

Important hidden-footprint note:

- this addon is not just a library. It adds Create-style processing families such as bulk ending/freezing/coloring/sanding and fluids like dragon breath/dye fluid.
- notable recipe examples found: end stone/chorus/phantom membrane via `create_dragons_plus:ending`; packed/blue ice, slime from magma cream, and breeze rods from blaze rods via `create_dragons_plus:freezing`.

Era conclusion:

- acceptable for now because the audited direct utility pieces inherit fluid/Aeronautics anchors and the blaze template duplication is disabled;
- however, it deserves runtime JEI review because its processing categories may surprise players.

Playability/accessibility:

- could be useful but should be documented if players see unfamiliar processing types.
- if JEI feels confusing, Dragons Plus processing categories are one place to simplify by config.

Risk:

- hidden utility footprint is higher than a pure dependency/deco addon;
- watch for unintended easy access to End/Trial/phantom resources depending on how the processing types are activated in-world.

## General conclusion

The curated Create addon stack is recipe-safe against the main era ladder right now:

- no addon recipe outputs Tenpack-controlled Create machines/logistics/precision items;
- rail helper items are mostly precision/electron-tube/fluid anchored where they matter;
- copycat/deco items upgrade or decorate real base items instead of replacing them;
- the food stack adds culture/trade goods, not hard progression;
- Dragons Plus is the only installed addon with a noteworthy hidden utility footprint and should get extra JEI/playtest attention.

## Added guardrail

`tools/check-create-addon-recipes.py` should be run with the normal validation set:

```bash
./tools/check-mod-integrity.py
./tools/check-create-progression.py
./tools/check-create-addon-recipes.py
./tools/tenpack-build-public.py --out public
```

If a future addon adds a new `create:*` recipe output, changes the scanned recipe
footprint, or is removed from the curated Create stack, the checker fails until we
decide whether it is harmless compat/decor, a progression bypass, or just a new
audited baseline.

The checker's recipe reference walker treats both string refs and list-style
`items` refs as anchors, so future addon recipes using modern ingredient arrays
do not silently evade the era checks.
