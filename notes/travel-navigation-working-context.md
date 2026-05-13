# Travel & Natural Navigation Working Context

Current working module for the Tenpack travel/navigation overhaul. This note exists so future work does not collapse the project into only code mechanics or only no-GPS rules.

## Operating mode

- Manage assistant context aggressively: summarize/strip tool output, avoid dumping old conversations into the active context, and keep long logs in `/tmp` when possible.
- Use focused subagents for audits/recovery/review, with strict concise reporting or file outputs.
- Prefer small, coherent, validated increments over broad speculative edits.
- Regenerate `public/` with `tools/tenpack-build-public.py --out public` after pack content changes.
- Run relevant guardrails before stopping.
- Never push without explicit permission; git-pull before any approved push.

## One-line thesis

Build a visually compelling, player-infrastructure-driven travel ecosystem where land animals, carts, ships, maps, signs, roads, docks, camps, stables, and route knowledge replace GPS/teleport convenience.

## Non-negotiable design rules

- No survival GPS: no minimap dot, exact coordinate UI, waypoint arrow, exact target marker, or global tracking as normal gameplay.
- No travel teleport/recall shortcuts for animals, carts, ships, corpses, or players.
- Travel friction should create player stories and infrastructure, not busywork.
- The pack should enable players to build routes, roads, bridges, crossings, docks, ports, camps, stables, map rooms, signs, and waystations; it should not author every route for them.
- Animal roles must be real mechanics, not fake buttons.
- Assets, models, animation, sounds, and UI are part of gameplay adoption, not late cosmetic polish.

## Prior reference/mod context to preserve

### Immersive Travel Overhaul

- Important as an immersive-travel concept/art/assets reference.
- License/source situation was favorable in the prior audit; implementation/version alignment was weaker.
- Do not dismiss it just because it may not be the direct technical base.
- Mine it for presentation direction, models/textures/props where license-compatible, and the general feel of travel as an experience.

### Eli's Immersive Navigation

- Important as a technical/navigation-behavior reference, especially around no-F3/no-coordinate discipline.
- Prior audit found it targeted Fabric 1.21.1 and GPL-3.0-only, so a native NeoForge reimplementation/fork requires license/platform care.
- Use it to understand desired UX and implementation shape, not as an unexamined dependency.

### Map Atlases

- Still a likely reference/base for physical atlas gameplay.
- Must be configured/forked/patched so it does not become minimap/GPS gameplay.
- Desired direction: physical atlas, shared maps, annotations/labels, cartography progression, region knowledge.
- Avoid: live exact player dot, waypoint arrows, global minimap, exact tracking.

## Major work pillars

### 1. Animal travel, bonding, care, and labor

- Bond/care/mood progression tied to real care, feeding, grooming, travel, and work.
- Trust/ownership rules for multiplayer safety.
- Draft work through real carts, especially AstikorCarts Redux.
- Pack animals, saddlebags/cargo, visible travel gear, stable/camp integration.
- Scout/guard/work roles only when they have actual behavior.
- Hitching posts, stable boards, troughs, grooming, rest, persistence, and missing-animal handling without GPS/recall.

### 2. Land vehicles and cargo

- Carts/wagons/stagecoach-like travel as physical transport.
- Cargo capacity, loading/unloading, repair, upgrades, terrain constraints, road value.
- Roads are player-built infrastructure; mechanics should make them worth building.

### 3. Sailing and water travel

- Sailing/boats/ships are a full pillar, not a footnote.
- The current water-travel working plan lives in `notes/sailing-water-travel-roadmap.md`; update it when auditing or adding any ship/dock/canal/ferry feature.
- Audit NeoForge 1.21.1 water-travel mods for ships, cargo vessels, docks/ports, canals, ferries, and water logistics.
- Water navigation should use charts, coastlines, landmarks, lighthouses, buoys/markers, compass/sun/stars, and player route knowledge rather than GPS.
- Ports/docks/canals/ferries should be valuable player-built infrastructure.

### 4. Natural navigation

- F3/coordinate discipline still needs a real solution.
- The current physical-map/no-GPS working plan lives in `notes/physical-maps-navigation-roadmap.md`; update it when auditing or adding any atlas/map/F3/navigation feature.
- Replace GPS with physical maps/atlases, compass/instruments, landmarks, signs, route journals, roads, rivers, docks, camps, and player memory.
- Audit corpse/death systems, structure-finding, map mods, HUD mods, and quest markers for GPS/coordinate leakage.

### 5. Assets, animation, sound, and UI

- Travel must look and feel worth using.
- Horses/animals may need tack, harnesses, packs, blankets, collars, role gear, or other visible state so the system is tempting.
- Vehicles need strong models/visual upgrades/cargo props/lanterns/banners/canopies where possible.
- Ships need sails, hull identity, harbor/dock props, cargo visuals, and water ambience.
- Stable board, animal inspection, atlas/maps, route notes, cart/ship inventory, and command UI should feel in-world rather than debug-like.

### 6. Player infrastructure and community logistics

- Encourage roads, bridges, river crossings, ports, docks, stables, camps, map rooms, signs, waystations, trade routes, shipping lanes, and caravans.
- The pack should make those builds mechanically valuable through travel constraints, cargo logistics, animal/craft needs, and navigation tools.

### 7. Validation and guardrails

- Current server smoke only proves boot; still need full client join and in-world playtesting.
- Test Astikor carts, animal commands, bonding/care XP, draft blocking, hitching/stable memory, trough persistence/eating, scout pings, multiplayer trust, and persistence across restart.
- Continue guardrails for no GPS/no recall/no exact coordinate leaks and pack consistency.

## Current validated checkpoint

- Tenpack repo: `/home/sisyphus/Workspace/tenpack`.
- Latest Tenpack Travel jar hash known from the current work: `a20a05f7644a96131b0f61dd841fda3786eaf30119c6c74c854247be07376f83`.
- Full-pack disposable NeoForge server smoke passed manually and via `tools/smoke-full-server.py`.
- Public manifests and guardrails passed after the smoke-tool work.
- No commit or push has been performed.
