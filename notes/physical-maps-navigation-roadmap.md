# Physical Maps & No-GPS Navigation Roadmap

This is the working plan for Tenpack's physical-map/navigation pillar. It exists so navigation work does not collapse into either raw F3 coordinate hiding or decorative map props with no gameplay value.

## Product goal

Replace survival GPS with physical, shared, player-made navigation: maps, charts, signs, route tables, landmarks, roads, rivers, docks, stables, camps, journals, and community knowledge.

The pack should help players build navigation infrastructure. It should not hand them a live dot, destination arrow, minimap, death waypoint, structure pointer, or coordinate readout.

## Current pack baseline

- No dedicated physical atlas mod is installed yet.
- Map Atlases remains a candidate/reference, but must be audited/patched/configured so it does not become minimap/GPS gameplay. Public project text currently advertises mini-map/world-map behavior, banner waypoints, current-position updates, and coordinate display, so `tools/check-mod-integrity.py` now blocks Map Atlases/Antique Atlas-style jars until that audit or fork is complete.
- Eli's Immersive Navigation remains a technical reference for coordinate/F3 discipline, but prior context noted Fabric/GPL considerations.
- Immersive Travel Overhaul remains an asset/concept reference, not a selected implementation base.
- Tenpack Travel now includes physical infrastructure blocks that support navigation without GPS:
  - `Trail Marker` for roads, docks, crossings, camps, and player-built routes, with only a very subtle local cap light for night readability;
  - `Mooring Post` for physical boat tie-up at docks/ferries/ports;
  - `Channel Marker` for canals, harbor mouths, ferry lanes, shallows, and dangerous bends;
  - `Chart Table` for map-room/cartography work using vanilla map mechanics;
  - `Route Journal` as a styled vanilla writable book recipe for player-authored route notes.
- Route signage should use vanilla signs, hanging signs, Supplementaries way signs, Trail Markers, maps, and Route Journals. Clickable/command/waypoint sign mods are intentionally guarded against because route signs should be world text and infrastructure, not command surfaces or GPS links.
- Config/datapack policy now closes several non-mod-source GPS leaks: Legendary Survival Overhaul hides F3 position/direction info; Supplementaries globes no longer print coordinates; generated structure road signs, random adventurer structure maps, death-map markers, and pet-teleport flutes are disabled; vanilla `minecraft:recovery_compass` crafting is overridden by the `tenpack-navigation-policy` datapack so death recovery does not become a compass pointer; vanilla and Trade Rebalance cartographer/explorer/village/trial map destination tags, plus buried-treasure map tags, are preserved as physical paper-map clues rather than disabled target systems.

## Chart Table acceptance model

The Chart Table is intentionally a vanilla cartography-table variant with Tenpack presentation, not a magic route computer.

Allowed:

- copying maps;
- scaling maps;
- locking maps;
- staging map rooms at camps, docks, road houses, stables, and ports;
- encouraging shared route planning in-world.

Forbidden/default-not-implemented:

- live player dots beyond vanilla map behavior;
- route arrows;
- coordinate display;
- waypoints;
- remote map updates;
- death/structure/biome pointers;
- automatic trail discovery.

## Route Journal acceptance model

The Route Journal is deliberately implemented as a styled `minecraft:writable_book`, not as a custom writable-book item. Vanilla's server edit/sign handlers only persist edits for the exact vanilla writable book item, so this keeps route notes reliable without adding packet mixins or a custom text editor. A custom model-data override gives the crafted book Tenpack route-journal presentation while preserving vanilla book behavior.

Static guardrails now enforce the fragile parts of that decision: the recipe must continue to output `minecraft:writable_book` with Tenpack custom model data `1778601`, and both vanilla `writable_book` and `written_book` item models must keep overrides pointing that model data at `tenpack_travel:item/route_journal`. This does not prove the client preserves the model through signing; it prevents the known static regressions before the remaining in-world client test.

Allowed:

- hand-written landmark directions;
- dock/port/ferry notes;
- stable and camp stop lists;
- road-building plans;
- copied/shared written books after signing;
- player memory on paper.

Forbidden/default-not-implemented:

- automatic coordinates;
- waypoint storage;
- route tracking;
- auto-filled last-known position logs;
- remote map or animal tracking.

## No-GPS guardrails

`tools/check-mod-integrity.py` should reject obvious navigation bypasses before they enter the pack, including:

- JourneyMap;
- Xaero minimap/world map;
- VoxelMap;
- Nature's Compass;
- Explorer's Compass;
- structure-compass style mods;
- Waystones/teleportation shortcuts;
- boat-jump/water-route bypasses.

Do not ban Map Atlases blindly forever: it is still a candidate for physical atlas gameplay, but only after a focused audit of player-dot/minimap/waypoint/coordinate behavior and patch/config options. Until then, mod-integrity treats Map Atlases and Antique Atlas-style atlas mods as deferred so a casual install cannot reintroduce GPS.

Create Railways Navigator is treated separately from GPS/minimap mods: it is allowed as precision-era rail schedule/station infrastructure for networks players physically build. It remains suspect if it ever becomes a general overland pathfinder, coordinate HUD, waypoint arrow, minimap substitute, or teleport/debug shortcut. Its CRN admin mode is disabled, and Tenpack Travel cancels CRN's registered teleport packet as defense-in-depth.

`tools/check-pack-configs.py` should also enforce active config behavior, not just installed mod names. Current config guardrails require:

- LSO F3 debug position/direction hiding on;
- LSO filled-map coordinate output off;
- LSO compass info mode `NONE`;
- Supplementaries globe coordinate output off;
- Supplementaries generated road signs and exact distance text off;
- Supplementaries death-map markers off;
- Supplementaries random adventurer structure maps off;
- Supplementaries pet-teleport flute disabled.
- Supplementaries compass right-click coordinate output off. Clock right-click time output is deliberately left on because time planning is not GPS/location leakage.
- The `tenpack-navigation-policy` datapack overrides `minecraft:recovery_compass` to require a creative-only barrier ingredient, keeping it uncraftable in survival.
- The same datapack overrides vanilla `minecraft:lodestone` crafting with a creative-only barrier ingredient, and Tenpack Travel blocks compass-on-lodestone binding. Ordinary compasses and placed/decorative lodestones are left alone, but lodestone compasses are not normal survival navigation because they become persistent waypoint arrows.
- The `tenpack-navigation-policy` datapack does **not** override vanilla cartographer/explorer/village/trial structure-map destination tags or buried-treasure map tags. Traded and looted maps are primitive physical navigation and belong beside filled maps, the Chart Table, and Route Journals; they are kept as world-found paper clues, not GPS, teleport, recall, or waypoint systems.
- Create Railways Navigator common config is mirrored between client/server; global settings require permission level `2`, admin mode is disabled with `-1`, the active route overlay is explicitly documented as train-schedule feedback rather than GPS, and Tenpack Travel must ship the CRN teleport-packet cancellation mixin on both sides.

## Future implementation directions

1. **Map Atlases deep audit** — loader/version/license/source/configs, live-dot behavior, waypoints, minimap risks, and patchability.
2. **F3 coordinate discipline** — native NeoForge solution or clean-room implementation inspired by Eli's Immersive Navigation, respecting license constraints.
3. **Route notes/journals** — player-authored directions and shared route descriptions, not automatic coordinate logs. First pass is the Route Journal recipe/model override; future work can add better route-board storage/display if needed.
4. **Map boards / chart rooms** — shared display/storage of physical maps at camps, ports, and stables without remote tracking.
5. **Asset direction** — chart clutter, pins/strings/labels as static visuals, folded maps, map tubes, table props, paper sounds, page turns.

## Deliberately not done yet

- No atlas mod has been installed.
- Map Atlases / Antique Atlas-style mods are guarded as deferred, not permanently rejected. Adoption requires a deliberate audit/fork/config pass that preserves physical atlas/map-stitching value while removing minimap, waypoint, coordinate, and live-location behavior.
- F3 position/direction hiding is enabled through Legendary Survival Overhaul config, but it still needs client/in-world confirmation.
- No minimap/waypoint/live-dot feature is allowed.
- No third-party navigation assets have been copied in this slice.
- No automatic route generation has been added; players build and document routes themselves.
- Buried treasure and its vanilla red-X maps are preserved as physical loot/map play. They should not be expanded into HUD arrows, waypoints, coordinates, teleport, or recall.
- Dolphin-led exploration and Eyes of Ender are deliberately preserved by checker. They are embodied vanilla exploration/progression loops rather than map/compass items that print a persistent target; changing them would require a separate progression redesign, not a broad no-GPS sweep.
