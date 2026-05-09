# Tenpack Create Addon Audit

This records the progression audit behind `server/world/datapacks/tenpack-create-progression`.

## Audited source

Recipe ids were checked against the locally installed 1.21.1 Create-family jars in the CurseForge test instance. The datapack mirrors upstream recipe ids so each override replaces exactly one recipe.

Validation performed:

- all datapack JSON parses
- every overridden recipe id exists upstream
- referenced non-vanilla item ids exist in installed mod assets

## Findings and decisions

### Create core

Decision: CABIN-lite, no custom mechanism items yet.

Actions:
- keep belts and starter motion early
- increase costs for the first kinetic machine suite
- make precision mechanisms consume electron tubes
- move mechanical crafters, package logistics, stock links, steam, elevators, and train controls into precision/brass

### Aeronautics / Simulated / Offroad

Decision: basic vehicles/physics can arrive before full aircraft mastery; controlled flight is a major precision-era milestone.

Actions:
- physics assembler requires mechanical bearing
- gyroscopic mechanism starts from precision mechanism
- engine assembly starts from sturdy sheet and uses precision tech
- propeller bearing, gyro bearing, smart propeller, burners, and vents require precision/gyro progression
- offroad tires/wheel mounts are lightly gated

### Create Encased

Problem: Create Encased adds alternate casing variants for core Create machines. Those recipes could bypass Tenpack's tuned base machine recipes.

Decision: turn the relevant variants into upgrades from the tuned base machine plus the desired casing/block.

Actions:
- override variants for presses, mixers, saws, drills, fans, harvesters, ploughs, portable storage interfaces, deployers, and rollers
- leave decorative/shaft/gearbox-style casing variants mostly alone, because they do not bypass the main machine gates in a server-warping way

### Create Diesel Generators

Problem: diesel/oil infrastructure and diesel engines are compact power and heavy logistics. If left too cheap, diesel can undercut the steam/precision era and support early vehicles too strongly.

Decision: move oil/diesel infrastructure into precision-era heavy industry.

Actions:
- pumpjack bearing requires precision mechanism
- distillation controller requires precision mechanism
- diesel engine requires precision mechanism while retaining brass, engine piston, fluid tank, flint/steel, and blackstone flavor

### Create Big Cannons

Problem: cannon production is faction-war technology. It should not be available as soon as someone can make andesite casings and iron plates.

Decision: move cannon-production machinery into precision-era faction warfare.

Actions:
- cannon builder, cannon drill, cannon mount, fixed cannon mount, cannon mount extension, and cannon welder require precision-era parts
- cannon drill also upgrades from the tuned mechanical drill
- cannon mount uses mechanical bearing to tie artillery control into contraption knowledge

### Create Enchantment Industry

Finding: main advanced blocks already depend on spouts, blaze burners, experience infrastructure, or Dragon Plus templates. No immediate Create-era bypass found in the first pass.

Decision: leave for now; revisit after playtesting if XP automation becomes too strong.

### Create Integrated Farming

Finding: fishing nets/roosts are low-tech farm utility. Potentially useful but not currently a flight/heavy-industry bypass.

Decision: leave for now; revisit if resource generation feels too fast.

### Create Cobblestone

Finding: mechanical generator depends on brass/electron tube/mechanical drill/lava/water. Since the tuned drill and brass/precision path already slow the setup indirectly, it is not a top bypass.

Decision: leave for now. If infinite stone becomes too central too early, add precision mechanism to the generator.

### Create Aquatic Ambitions

Finding: mostly aquatic material processing and channeling recipes. No immediate Create progression bypass.

Decision: leave for now.

## Remaining risks

- This is still a recipe-level audit, not an in-game playtest.
- Create addon config behavior can matter as much as recipes; configs still need a pass if something feels off.
- Actual aircraft construction depends on block counts and Sable/Aeronautics mechanics, not just recipe gates.
- Ground vehicles may compete with horses. If horses should dominate early travel, Offroad should be pushed later.

## Playtest checklist

1. New player can make belts and simple motion quickly.
2. First press/mixer/saw/drill require effort but not wiki-level planning.
3. Precision requires deployer/sequenced assembly knowledge.
4. Mechanical crafters/package logistics are not available before precision.
5. Create Encased variants cannot bypass the tuned base machine recipes.
6. Physics assembler appears after basic contraption knowledge.
7. First controlled flight requires precision + gyro work.
8. Diesel and cannon production do not arrive before precision-era infrastructure.
9. The first functional plane feels like a server milestone.
