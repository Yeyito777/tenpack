# Tenpack oilfield pressure point

Design rule: resources should lead to architecture.

A pressure point is strongest when it is:

1. **visible** — players can tell the place matters without reading configs;
2. **territorial** — tied to land, route, or a structure that cannot simply be pocketed;
3. **logistical** — needs transport, storage, escorts, and maintenance;
4. **contestable** — can be raided, blockaded, taxed, defended, or negotiated over;
5. **useful to outsiders** — creates trade leverage instead of pure self-sufficiency;
6. **not an instant win** — control helps, but also creates exposure and obligations.

## Implemented: dry-biome oilfields

Create Diesel Generators oil is tuned as Tenpack's first explicit faction resource pressure point.

- Rich oilfield biomes are deserts, badlands, eroded/wooded badlands, savannas, savanna plateaus, and windswept savannas.
- Optional vanilla badlands/savanna biome-tag references are included so compatible dry variants remain oil-rich.
- Plains and oceans are removed from the rich-oil biome set.
- Ocean, river, beach/shore, snowy, cold-peak, and mushroom biomes are denied oil, with optional vanilla ocean/river/beach tag references for broader coverage.
- Normal non-oilfield chunks remain technically enabled but weak/finite. They are a fallback, not a basis for a fuel empire.
- High oilfield chunks are boosted so deserts/drylands are where factions should scout, claim, rail to, and defend.
- Oil scanners intentionally remain early while pumpjacks/refineries/diesel engines remain precision-era. Prospecting should create claims and rumors before extraction creates refineries.
- Portable canisters are intentionally small emergency/scouting storage. Serious fuel wealth should show up as placed oil barrels, tank farms, rail loading stations, and depots.
- Diesel and gasoline burn faster than upstream defaults, not to be punitive, but to make fuel a recurring logistics input once a faction relies on diesel engines.

Player story: desert oil factions get fuel, aircraft range, diesel power, tanker trains, and refinery architecture, but the useful land is exposed, distant, and supply-line shaped.

## Why oil works better than abstract ore pressure

Brass/zinc scarcity is mechanically real but visually nebulous: players do not automatically understand that a mountain matters unless they know the ore table.

Oil is legible:

- pumpjacks are visible;
- refineries and tanks are visible;
- tanker trains are visible;
- desert occupation is visible;
- fuel embargoes, escorts, and raids are easy to understand.

## Future oil polish

### 1. Surface oil/tar tells

Create Diesel Generators oil chunks are procedural and scanner-driven. A future Tenpack datapack/mod pass could add visible dry-biome oil tells:

- dark sand/gravel patches;
- basalt/blackstone crusts;
- tar seep blocks or crude-oil decorative pools if available;
- dead-bush/smoke/flaring POIs;
- abandoned derricks or refinery ruins.

Goal: players should see an oil province before they fully quantify it.

### 2. Protected long-distance routes

The server cannot force public infrastructure, but oil can fertilize it. If oil is worth transporting, players may voluntarily build:

- desert rail spurs;
- tanker train routes;
- guarded bridges and mountain passes;
- airfields/refueling stations;
- toll roads and faction checkpoints.

Goal: routes exist because they solve a real resource problem, not because admins asked for roads.

### 3. Refinery/airfield fuel chain

Oil should produce recognizable faction districts:

- oilfield: pumpjacks and storage tanks;
- refinery: distillation, diesel engines, barrels, train loading;
- airfield: hangars, fuel tanks, repair shops;
- airfield: hangars, fuel tanks, repair shops, and refueling access.

Goal: a powerful faction skyline tells you what kind of power they have.
