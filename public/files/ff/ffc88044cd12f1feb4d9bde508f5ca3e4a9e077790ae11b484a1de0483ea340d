# Tenpack stronghold startup optimization

Minecraft precomputes concentric-ring stronghold positions during world load.
With Tenpack's TerraBlender/Still Life/Lithosphere biome stack, the vanilla
128-ring search spends a long time sampling noise/biomes on every server boot.

This datapack keeps vanilla-style concentric-ring strongholds, but:

- reduces the stronghold count from 128 to 16;
- allows the ring search to use any overworld biome via `#minecraft:is_overworld`.

Still Life already adds its biomes to `#minecraft:is_overworld`, and Lithosphere
changes the terrain/biome stack without adding separate biome JSONs, so this tag
is the broadest available overworld biome set for the current pack.
