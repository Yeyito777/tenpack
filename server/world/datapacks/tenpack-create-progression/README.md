# Tenpack Create Progression

CABIN-lite recipe overrides for Create and Create Aeronautics.

This pack intentionally uses **vanilla datapack recipe overrides** instead of KubeJS/custom items. The goal is to get the strongest part of CABIN's pacing—clear machine tiers—without turning Tenpack into a full expert pack or adding another scripting dependency.

## Design goals

- Keep starter Create approachable: shafts, cogs, belts, water wheels, and basic power are not expert-gated.
- Slow the first big automation jump by making core kinetic machines cost real casings/sheets/components.
- Make brass/precision the natural tier for compact power, smart logistics, trains, and advanced contraptions.
- Tie Aeronautics/Simulated/Offroad into those same tiers so vehicles and flight do not bypass Create progression.
- Prefer recipes that are explainable in JEI at a glance. If a recipe needs a paragraph to justify it, it is probably too expert for Tenpack.

## Layout

The files under `data/<modid>/recipe/...` deliberately mirror upstream recipe ids. That makes each file a direct replacement for exactly one upstream recipe.

- `data/create/recipe/...` — Create core pacing.
- `data/simulated/recipe/...` — Create Simulated physics/engine/gyro pacing.
- `data/aeronautics/recipe/...` — Create Aeronautics flight-control pacing.
- `data/offroad/recipe/...` — Create Offroad vehicle/bore pacing.
- `data/createcasing/recipe/...` — Create Encased variants rewritten as upgrades from the tuned base machines, preventing alternate casings from bypassing Tenpack's era gates.
- `data/createdieselgenerators/recipe/...` — diesel/oil power nudged into precision-era heavy industry.
- `data/createbigcannons/recipe/...` — cannon production machinery nudged into precision-era faction warfare.

The repeated `neoforge:conditions` blocks are intentional. Datapack JSON has no shared helper/import system, and conditional recipes fail more gracefully if one of the Create addon modules is removed later.

## Tiers

| Tier | Player-facing meaning | Representative recipe changes |
| --- | --- | --- |
| 0. Starter kinetics | Basic rotation remains easy and noob-friendly. | Shafts, cogwheels, belts, water wheels, gearboxes remain mostly vanilla. |
| 1. Kinetic machines | First real automation step. | Press, mixer, saw, drill, fan, mechanical bearing, piston, gantry, portable storage interfaces, harvester, plough ask for a little more casing/alloy/sheet investment. |
| 2. Fluid/sealed machines | Fluid handling becomes a second learning step. | Spout, item drain, portable fluid interface require copper sheets/pipes/plumbing parts. |
| 3. Precision/brass | Advanced automation and logistics. | Precision mechanisms require electron tubes; mechanical crafters, steam engines, elevators, package logistics, stock links, train controls/signals/stations lean on precision. |
| 4. Heavy industry and Aeronautics control | Vehicles are accessible, controlled flight/artillery/diesel power are earned. | Physics assembler starts after basic bearings; propeller bearing, gyro bearing, smart propeller, engines, burners, vents, diesel oil infrastructure, and cannon production use precision/gyro progression. |

## Player-facing guidance

This datapack adds a small advancement chain under the `tenpack_create:create_progression/*` namespace. It is intentionally lightweight: enough to communicate eras without adding a full quest book.

Milestones include starter logistics, kinetic workshop, fluid handling, precision engineering, smart logistics, physics age, controlled flight, first-plane moment, and heavy industry.

## Important recipe anchors

### Create

- `create:precision_mechanism` is the main tier gate. It now consumes `create:electron_tube` during sequenced assembly.
- `create:mechanical_bearing` is the early contraption/physics bridge.
- `create:portable_storage_interface` is still early, but costs a modest andesite-tier frame instead of only casing + chute.
- `create:mechanical_crafter`, `create:packager`, `create:package_frogport`, and `create:stock_link` are precision/brass tier so Create 6 logistics do not skip the progression curve.
- Create Encased variants of presses/mixers/saws/drills/fans/harvesters/ploughs/deployers/rollers/storage interfaces are upgrades from the tuned base machine, not alternate cheap recipes.
- `create:steam_engine`, `create:elevator_pulley`, and train infrastructure are precision/brass tier.

### Aeronautics / Simulated / Offroad

- `simulated:physics_assembler` requires `create:mechanical_bearing`, making physics contraptions follow basic Create knowledge.
- `simulated:gyroscopic_mechanism` starts from `create:precision_mechanism`, making controlled flight a precision-tier reward.
- `simulated:engine_assembly` starts from `create:sturdy_sheet` and uses precision tech, so powered vehicles sit after basic Create automation.
- `aeronautics:propeller_bearing`, `aeronautics:gyroscopic_propeller_bearing`, and `aeronautics:smart_propeller` are intentionally tied to brass/precision/gyro progression.
- Offroad tires and wheel mounts are lightly gated so cars are not day-zero dried-kelp crafts, but they should still arrive before full aircraft mastery.

### Heavy industry / warfare

- `createdieselgenerators:pumpjack_bearing`, `createdieselgenerators:distillation_controller`, and `createdieselgenerators:diesel_engine` require precision-era parts so diesel/oil power does not undercut steam and early Aeronautics pacing.
- Create Big Cannons production blocks (`cannon_builder`, `cannon_drill`, `cannon_mount`, `fixed_cannon_mount`, `cannon_welder`, and mount extension) require precision-era parts so faction artillery arrives as a serious industrial milestone.

## Tuning guidelines

If this feels too hard:

1. Reduce the number of precision requirements on Aeronautics recipes first.
2. Keep the precision mechanism electron-tube change; that is the cleanest CABIN-lite tier marker.
3. Loosen early kinetic machines before touching flight control.

If this feels too fast:

1. Push `simulated:physics_assembler` from mechanical bearing to precision mechanism.
2. Add `create:precision_mechanism` to `offroad:wheel_mount`.
3. Increase `simulated:engine_assembly` loops or ingredients.

Avoid adding custom mechanism items unless Tenpack intentionally becomes a full expert/progression pack. That would be a separate design pass, likely with KubeJS or a small Tenpack balance mod.

## Maintenance checks

Run this after editing recipes:

```bash
./tools/check-create-progression.py
./tools/tenpack-build-public.py --out public
```

The checker enforces the design invariants that are easiest to break: Create Encased variants must upgrade from tuned base machines, heavy industry must stay behind precision, and controlled flight must stay behind precision/gyro progression.
