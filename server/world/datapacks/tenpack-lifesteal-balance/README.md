# Tenpack Lifesteal Balance Datapack

Overrides Lifesteal's default heart crystal worldgen so heart ore feels closer to old-school diamond hunting instead of a common modded ore.

## Changes

- Overworld `lifesteal:heart_ore_placed`
  - Default: 6 attempts per chunk, Y -50..70.
  - Tenpack: 1 attempt every ~6 chunks, Y -58..-16.

- Nether `lifesteal:nether_heart_ore_placed`
  - Default: 6 attempts per chunk, Y 20..100.
  - Tenpack: 1 attempt every ~4 chunks, Y 8..40.

- Overworld heart geodes
  - Default: chance 50.
  - Tenpack: chance 256.

- Nether heart geodes
  - Default: chance 30.
  - Tenpack: chance 192.

This keeps revive crystals possible, but makes heart crystal production a faction-level mining/project goal.

Worldgen changes only affect newly generated chunks.
