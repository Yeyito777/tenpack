# Tenpack Create + Aeronautics Progression

Status: working design draft. Do not treat as final until playtested.

## North star

Tenpack should let new players *touch Create early* but make the server-changing milestones feel earned.

People who finished CABIN should recognize a satisfying progression arc. People new to Create should not bounce off a wall of custom parts before they get to make something spin.

The first real plane should be a server moment.

Heavy industry should also create faction pressure points: resources should lead to architecture. A faction that controls something important should be forced to build visible infrastructure around it—pumpjacks, refineries, tanks, train spurs, airfields, cannon foundries—so other players can understand power by looking at the world.

## Audience split

- CABIN veterans: want meaningful automation work, not vanilla-fast Create.
- Create newcomers: need early wins, visible cause/effect, and enough guided stepping stones.
- SMP/faction players: mobility, automated mining, flight, trains, and logistics affect conflict and geography.

## Era model

### Era 0: Motion toys

Player fantasy: "I made things spin and move items."

Should be available very early:
- shafts
- cogwheels
- belts
- gearboxes
- water wheels/basic power
- simple chutes/depots

Do not over-gate this era. It is the Create tutorial.

### Era 1: Kinetic workshop

Player fantasy: "I built my first useful machine line."

Unlocks:
- press
- mixer
- saw
- drill
- encased fan
- mechanical bearing/piston/gantry
- portable storage interface
- basic farms/contraptions

Pacing goal: a player has to gather and craft deliberately, but can still reach this in a first serious session.

### Era 2: Fluids and sealed handling

Player fantasy: "My factory handles liquids and processing chains."

Unlocks:
- spout
- item drain
- portable fluid interface
- early fluid recipes

Pacing goal: make copper and pipes matter. Still pre-precision.

### Era 3: Brass / precision engineering

Player fantasy: "My factory can do smart/complex automation."

Unlocks:
- deployer-driven precision mechanisms
- mechanical crafters
- package logistics
- stock links
- steam engines
- elevators
- train controls/stations/signals

Pacing goal: this is the main CABIN-lite gate. It should require the player to understand press/mixer/deployer/sequenced assembly.

### Era 4: Vehicles and crude physics

Player fantasy: "We can make moving machines and land/water vehicles."

Unlocks:
- physics assembler
- basic Offroad parts
- basic engines

Pacing goal: vehicles should be exciting before full flight, but not day-zero. If horses are meant to matter, keep this era after basic Create competence.

### Era 5: Controlled flight and heavy industry

Player fantasy: "The first functional airship/plane exists, and factions start fielding serious industrial war/logistics tools."

Aeronautics references emphasize a practical build ladder of ground vehicles/test rigs, then airships, then fixed-wing planes. Tenpack's recipes follow that spirit: envelopes are cheap, crude physics comes first, propeller/burner control is precision-era, and gyroscopic/smart propeller flight is the later milestone.

Unlocks:
- propeller bearing
- gyroscopic mechanisms
- gyroscopic propeller bearing
- smart propeller
- stronger burners/vents/flight-control blocks
- diesel power / oil infrastructure
- Create Big Cannons production machinery

Pacing goal: the first plane should take faction-level or dedicated-player effort. It should feel like a technological threshold, not a JEI click. Heavy artillery and diesel infrastructure belong around this same threshold because they reshape faction conflict and logistics.

Oil has an extra territorial rule: rich Create Diesel Generators oilfields are now concentrated in deserts, badlands, and savannas. The oil scanner intentionally remains an early tool so factions can prospect, claim, bluff, and fight over dry land before they can fully exploit it. Pumpjacks, refineries, and diesel engines remain precision/heavy-industry milestones. Non-oilfield chunks still have a weak finite fallback chance, but they should not support a true fuel empire. Tenpack keeps portable canisters useful for scouting/emergencies and makes barrels cheap enough for visible tank farms, while diesel/gasoline burn rates are high enough to create recurring supply demand. The intended faction story is that oil groups become rich from fuel while creating exposed infrastructure, pumpjack defense, and long supply lines.

This is the preferred pressure-point pattern for Tenpack:

1. a resource is tied to visible geography,
2. exploiting it demands visible architecture,
3. moving it demands logistics,
4. enemies can raid, blockade, tax, or negotiate around that logistics,
5. ownership helps a faction without instantly winning the server.

## Current implementation philosophy

Use vanilla datapack recipe overrides first. Avoid custom tier items unless needed.

Pros:
- fewer dependencies
- easy to inspect in JEI
- lower risk for pack maintenance

Cons:
- progression eras are less legible than CABIN's custom mechanism items
- harder to express "tier tokens" without KubeJS or a small mod

## Future possible escalation

If datapack-only gets too blurry, add a tiny balance mod or KubeJS layer with only 2-3 custom components:

- Kinetic Mechanism
- Sealed Mechanism
- Flight Control Core

Avoid CABIN's full component tree unless Tenpack intentionally becomes an expert pack.

## Audit checklist

For every Create addon item:

- Does it provide mobility, automation, power, resource generation, logistics, or combat?
- What era should it be in?
- Does its recipe bypass a higher era?
- Does it require a block/item that naturally teaches the previous era?
- Is the recipe understandable from JEI?

## Open questions

- How early should ground vehicles arrive relative to horses?
- Should trains be Era 3 or their own Era 3B?
- Should Create Big Cannons be tied to a later faction-war era?
- Is Creating Space meant to be the long-term endpoint after controlled flight?
