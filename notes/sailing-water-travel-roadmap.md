# Sailing & Water Travel Roadmap

This is the working plan for Tenpack's water-travel pillar. It exists because natural navigation is not only land animals/carts: rivers, coasts, docks, ferries, ships, charts, and ports should become player-made infrastructure and logistics stories.

## Product goal

Make water travel a physical, visually tempting alternative to roads/rails/animal caravans without turning boats into backpacks, teleporters, or GPS vehicles.

Players should build and maintain the routes:

- river landings;
- docks and piers;
- ferry crossings;
- canals and locks;
- ports/harbors;
- lighthouses, buoys, and coast markers;
- warehouses, cart ramps, stables, and map boards near water.

Tenpack should provide the mechanics, constraints, visuals, and progression pressure that make those builds worth doing.

## Current pack baseline

- No dedicated ship/sailing/cargo-vessel mod is currently installed.
- Current water-adjacent pack content is mostly indirect:
  - `lily_pads_expansion` for wetland/water traversal flavor;
  - Supplementaries/building mods for dock/harbor decoration potential;
  - Create/Railways/Aeronautics for broader logistics/vehicle context;
  - Alex's Mobs aquatic creatures for water-route ecology/danger/flavor.
- The pack already rejects teleport/backpack convenience mods through mod-integrity checks; water travel uses the same discipline. `tools/check-mod-integrity.py` now blocks Jumpy Boats and deferred ship stacks such as Small Ships and Eureka/Valkyrien-style moving ships unless a future deliberate audit replaces that policy.
- Tenpack Travel now includes a craftable `Mooring Post` as the first functional dock/landing tool. It is deliberately simple: a styled fence-tagged post that uses vanilla lead/leash-knot mechanics. Since vanilla boats are `Leashable` in 1.21.1, players can physically tie boats at docks without adding ship recall, remote inventory, coordinates, or a custom capture scan.
- Tenpack Travel also includes a craftable waterloggable `Channel Marker` as the first buoy/channel-marking tool. It is a dumb physical marker for canals, harbor mouths, ferry lanes, and dangerous shallows: no beacon beam, waypoint, tracking, recall, remote inventory, or mooring/fence tag. Its small lantern cap emits only subtle local light so a placed buoy remains readable at night without becoming a lighthouse, GPS marker, or beacon.

## Candidate snapshot

Initial Modrinth API check for NeoForge 1.21.1 water/ship candidates found:

| Candidate | Status | Notes |
| --- | --- | --- |
| Small Ships (`small-ships`) | Candidate to audit, not selected; guarded against accidental install | NeoForge 1.21.1 version exists (`smallships-neoforge-1.21.1-2.0.0-b2.1.jar`). Physical ship fantasy is relevant, but it is beta, client+server required, Modrinth metadata reports all-rights-reserved/custom license and no source URL, so it needs careful license/config/gameplay review before adoption. |
| Eureka / Valkyrien-style moving ships | Deferred; guarded against accidental install | Potentially compelling physical-ship direction, but it is a larger physics stack with ownership, cargo, griefing, performance, and no-teleport/no-recall questions. It should not slide in as a casual water-travel dependency. |
| Unwrecked Ships / Antique Trading Ship / Sky Whale Ship | World-structure flavor, not core sailing | Search results are structure/content flavor, not player-built physical sailing/cargo systems. |
| Jumpy Boats | Explicit bypass risk | Makes boats jump; this fights canals, docks, crossings, and river route constraints. Guardrail should reject it. |
| Move Boats / Boat Item View / Boat Break Fix / boat utility mods | Deferred; guarded against accidental install until audited | May improve handling/readability but do not solve sailing/cargo. Move Boats is especially sensitive because pickup/relocation can become boat itemization, chest-boat portable storage, portage, and dock/canal bypass. Boat Item View and Boat Break Fix may be harmless only after confirming no remote inventory, cargo-information leak, cargo-preservation pickup, or easy recovery bypass. |

This list is not final. A deeper candidate audit should inspect jars/source/configs/licenses, especially for ship storage, ownership, performance, server behavior, and compatibility with no-GPS navigation.

## Acceptance model for water travel

A water-travel addition should answer these before being installed/enabled:

1. **Physicality** — Does the vessel exist in-world and travel through terrain/water rather than teleporting or becoming an inventory shortcut?
2. **Cargo limits** — Does cargo feel like freight, not a backpack/storage-network bypass?
3. **Docking/mooring** — Are docks/ports/landings useful for boarding, loading, unloading, repairs, or staging carts/animals?
4. **Navigation** — Does it rely on charts, coastline, rivers, buoys, lighthouses, compass/sun/stars, and player route knowledge rather than GPS dots/waypoints?
5. **Infrastructure value** — Does it make player-built ports, canals, locks, ferry crossings, and harbors worth building?
6. **Progression** — Does it fit walking/animal/cart/rail/Create-era travel instead of skipping the progression curve?
7. **Failure states** — Can ships get stuck, damaged, expensive, cargo-limited, weather-limited, or route-limited enough to create stories instead of perfect convenience?
8. **Multiplayer safety** — Are ownership/theft/grief/cargo rules acceptable?
9. **Presentation** — Are models, sails, hulls, cargo props, ropes, lanterns, flags, sounds, and UI compelling enough that players want to use it?
10. **License/source** — Are assets/code allowed to be redistributed or adapted for Tenpack's pack workflow?

## Player-built infrastructure tiers

Water progression should reward builds like:

1. **River landing** — safe boarding/unboarding, trail marker/sign, small chest/barrel, nearby hitching post/trough.
2. **Dock** — larger loading platform, mooring posts for vanilla boat tie-up, cart access, lights, signs, basic cargo staging.
3. **Ferry crossing** — repeated crossing route, visible ramps/signs, animal/cart staging on both banks.
4. **Canal/lock/towpath** — expensive player-made shortcut requiring labor and route maintenance.
5. **Port/harbor** — warehouses, ship repair, map/charts, stables, roads/rails connection, public signage.

The pack should not prebuild these routes. It should make them valuable.

## No-GPS water navigation rules

Allowed direction:

- charts/atlases;
- named coasts/rivers/ports;
- lighthouses and buoys;
- signs/trail markers/dock names;
- compass, sun, moon, stars;
- route journals and player maps;
- local danger/weather cues.
- dolphin-led exploration as an embodied, nearby water loop rather than a map item or target pointer.

Avoid:

- live ship/player dots;
- exact coordinate UI;
- waypoint arrows;
- remote ship tracking;
- teleport-to-ship / recall-ship;
- remote ship inventory;
- itemized ships that preserve cargo as a backpack unless explicitly constrained.

## Next concrete steps

1. Perform a deeper read-only Small Ships audit: license text, jar contents, configs, recipes, storage/cargo behavior, entity ownership, server performance, and whether GPS/remote inventory exists.
2. Perform a separate Eureka/Valkyrien-style ships audit only if the pack is ready to consider a larger moving-contraption/physics stack for water travel.
3. Audit water utility mods separately: Move Boats, Boat Item View, Boat Break Fix. They are blocked by mod-integrity until an audit proves they support physical travel without remote inventory, itemized chest-boat storage, portage, or dock/canal bypasses.
4. Playtest Mooring Post and Channel Marker with vanilla boats/chest boats and leads: tie-up, release, restart persistence, waterlogging, dock/channel placement, visibility at night, multiplayer ownership/theft expectations, and interaction with LeashAll config.
5. Design the next dock/harbor support increment only after playtest. Candidate: cargo-transfer staging that makes docks useful without remote storage.
6. Add asset direction board for water travel: sails, ropes, dock clutter, crates/barrels, harbor signs, buoys, lanterns, wake/creak/splash sounds, chart table/map board.
7. Decide whether water travel should remain vanilla-boat-plus-infrastructure first, or move to ship-mod-first after audit.

## Deliberately not done yet

- No ship/sailing mod has been selected.
- No water-travel jar has been added.
- No configs have been changed for ship behavior.
- No assets from Small Ships or any all-rights-reserved project have been copied.
- No remote inventory, teleport, or GPS behavior is allowed by default.
- Channel Marker night visibility is intentionally local and low-power: a subtle lantern cap for physical buoy readability, not a beam, map marker, live dot, waypoint, or remote tracking system.
