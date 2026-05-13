# Create rail pressure points

Goal: make rails a natural answer to faction logistics, not a public-infrastructure chore.

## Design rule

A rail line should exist because it solves a repeated bulk movement problem.

Create Railways Navigator is allowed as precision-era rail readability, not as general survival GPS: it should help players use a rail network they physically built with stations, displays, schedules, and trains. It should not replace roads, animals, maps, or overland route knowledge before that rail network exists.

Good rail pressure points have:

1. a fixed origin — oilfield, mine, farm district, refinery, cannon foundry;
2. a fixed destination — capital, airfield, front, market, depot;
3. repeated volume — fuel, food, stone, cannon supplies, building blocks;
4. exposure — bridges, tunnels, stations, and loaders that can be defended or raided;
5. visible value — other players can see why the route matters.

## Current concrete drivers

### Oilfield to refinery

Rich oil is concentrated in dry biomes. Pumpjacks are fixed infrastructure. This naturally creates the first long-haul rail reason:

- desert/badlands oilfield;
- refinery/tank farm;
- train loading platform;
- guarded route back to a safer base or trade depot.

### Refinery to airfield

Aircraft should not just be personal toys. Once a faction has flight, fuel has to move from refinery to where aircraft operate:

- airfield tank farm;
- fuel depot;
- loading/unloading pumps;
- visible hangars and repair pads.

### Farm district to faction center

With Farmer's Delight + Create: Central Kitchen, large farms can become actual infrastructure instead of just hand-harvest plots:

- wheat/rice/cabbage/tomato/onion fields;
- Create harvesters and belts;
- kitchens/cooking lines;
- food crates or prepared food shipments;
- rail/road delivery to fronts or remote oilfields.

This should stay soft. We do not need to force hunger economics; it is enough that food production can become a visible faction district.

### Foundry to fort/front

Create Big Cannons should eventually create heavy routes:

- foundry/cannon workshop;
- ammo storage;
- rail delivery to border forts;
- cannon batteries that imply a supply chain.

This probably needs a later ammo/logistics audit.

## What not to do

- Do not make every resource regional just to force trains.
- Do not make trains mandatory for basic play.
- Do not make public rail the primary design goal; faction-owned routes are more likely to survive.
- Do not over-tax players with chores. The route should feel powerful once built.
- Do not leave global rail-navigator settings open to every player. Players can build and use routes, but global/admin CRN controls should stay op-gated so one passerby cannot rewrite shared rail-network behavior.

## Later implementation checks

1. Are train stations/signals/tracks reachable at the correct Create era?
2. Are fluid loading/unloading setups understandable through JEI/Ponder/quest text?
3. Can oil/fuel be moved by train conveniently enough that players prefer it over many hand trips?
4. Are airfields and refineries far enough apart in practice to create routes?
5. Do cannon/farm logistics need additional recipe/quest support, or is player motivation enough?

## Current CRN policy

- `client/config/createrailwaysnavigator-common.toml` mirrors `server/config/createrailwaysnavigator-common.toml`.
- `global_settings_permission_level = 2` keeps global route-network settings op-gated while normal station/schedule use remains available.
- `admin_mode_permission_level = -1` disables CRN admin features. Static jar audit found a registered `teleport_player` packet whose handler calls `Player.teleportTo(...)`; admin mode should not expose teleport/debug surfaces in normal Tenpack play.
- Tenpack Travel also ships an optional `CrnTeleportPlayerPacketMixin` that cancels CRN's teleport packet server-side. This is defense-in-depth because hiding/admin-gating UI is not enough for a C2S packet surface.
- The client route overlay remains enabled for active train journeys because it is schedule/next-stop feedback, not a minimap, waypoint arrow, coordinate HUD, or overland pathfinder.
- `tools/check-pack-configs.py` enforces the above, including that the rebuilt client/server Tenpack Travel jars contain the CRN teleport-blocking mixin.
