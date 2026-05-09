# Create next integrations

Status note for follow-up passes after the Create progression/oilfield work.

## Immediate: JEI

Installed in the local working tree:

- Just Enough Items `19.27.0.340` for Minecraft `1.21.1` NeoForge
- File: `client/mods/jei-1.21.1-neoforge-19.27.0.340-tenpack-mcrangefix.jar`
- Source: `https://cdn.modrinth.com/data/u6dRKJwZ/versions/YAcQ6elZ/jei-1.21.1-neoforge-19.27.0.340.jar`
- Local patch: only `META-INF/neoforge.mods.toml` is changed. The upstream file is labeled `1.21.1` on Modrinth but declares Minecraft `versionRange="[1.21, 1.21.1)"`, which would exclude Tenpack's actual Minecraft `1.21.1`. Tenpack changes that range to `"[1.21.1, 1.21.2)"`.

Reason: Tenpack Create progression depends on players being able to inspect modified recipes. Advancements and a future questbook can explain the path, but JEI should be the recipe source of truth.

## Current local Create stack

Installed locally in this branch as the concrete Create workflow baseline:

- Create `6.0.10+mc1.21.1`
- Sable `1.2.2+mc1.21.1`
- Create Aeronautics bundled `1.2.1+mc1.21.1`
- Create: Diesel Generators `1.21.1-1.3.11`
- Create Big Cannons `5.11.3+mc.1.21.1`
- Ritchie's Projectile Library `2.1.2+mc.1.21.1`, required by Create Big Cannons
- Create Encased `1.8-ht2`
- Farmer's Delight `1.21.1-1.3.1`
- Create: Dragons Plus `1.10.0b`, required by Central Kitchen
- Create: Central Kitchen `2.4.0`

Installed in the first curated addon batch:

- Create: Steam 'n' Rails `0.2.0-beta.2+neoforge-mc1.21.1`
- Create Railways Navigator `1.21.1-beta-0.9.0-C6`
- DragonLib `1.21.1-beta-3.0.26`, required by Railways Navigator
- Architectury API `13.0.8+neoforge`, required by DragonLib
- Create: Copycats+ `3.0.4+mc.1.21.1-neoforge`
- Create Deco `2.1.3`
- Create: Bells & Whistles `0.4.7`
- Rechiseled `1.2.4-neoforge-mc1.21`
- Rechiseled: Create `1.1.0-neoforge-mc1.21`
- SuperMartijn642's Core Lib `1.1.21-neoforge-mc1.21`, required by Rechiseled
- SuperMartijn642's Config Lib `1.1.8-neoforge-mc1.21`, required by Rechiseled
- Fusion `1.2.12-neoforge-mc1.21.1`, client-side dependency required by Rechiseled
- Kotlin for Forge `5.11.0`, required by Slice & Dice
- Create Slice & Dice `4.2.4`
- Create Confectionery `1.1.2`
- Create: Winery `2.0.2-neoforge-1.21.1`
- Create: Bitterballen `1.0.2C`

The important caveat is still runtime testing. The static pack build can verify mirroring/manifests, but it does not prove that the full Create stack launches cleanly together.

## Create: Estrogen

Desired, but not an immediate drop-in.

Findings from Modrinth/API audit:

- No Minecraft `1.21.1` NeoForge build found.
- Public project metadata is centered around older `1.20.1` Fabric/Forge/Quilt support.
- Treat as a **porting project**, not a normal mod-add.

Recommended next step:

1. Clone/audit `MayaqqDev/Create-Estrogen` source.
2. Check license and dependencies.
3. Identify target Create version delta: Tenpack is currently designing against Create `1.21.1-6.0.10`.
4. Estimate whether porting is realistic as a small compat port or a larger rewrite.
5. Do not add to the public pack until it builds and launches against NeoForge 1.21.1.

Design note: if ported, audit its recipes immediately. Estrogen/Create addons tend to add machines, fluids, and novelty processing that can accidentally bypass Tenpack's precision/fluid progression.

## Farmer's Delight

Available for Minecraft `1.21.1` NeoForge and installed locally in this branch.

Latest compatible version found:

- Farmer's Delight `1.21.1-1.3.1`
- File: `FarmersDelight-1.21.1-1.3.1.jar`
- Source: `https://cdn.modrinth.com/data/R2OftAxM/versions/9gp7w8NC/FarmersDelight-1.21.1-1.3.1.jar`

It changes food economy, farming incentives, comfort effects, and early-game survival pacing. Tenpack already has Legendary Survival Overhaul temperature enabled and various survival/world-atmosphere systems, so food effects should be checked in-game before deployment.

Recommended follow-up pass:

1. Audit recipes for Create progression bypasses.
2. Check food effects against Legendary Survival Overhaul and Tenpack death/faction pacing.
3. Playtest Create harvester/deployer/belt interactions with Farmer's Delight crops/foods.
4. Convert `notes/create-farm-kitchen-pass.md` into a questbook chapter if the gameplay feels good.

## Create: Central Kitchen

Available for Minecraft `1.21.1` NeoForge and Create `6.0.10`; installed locally in this branch.

Latest compatible version found:

- Create: Central Kitchen `2.4.0`
- File: `create-central-kitchen-2.4.0.jar`
- Source: `https://cdn.modrinth.com/data/btq68HMO/versions/TUJIHmUh/create-central-kitchen-2.4.0.jar`
- Required dependency from Modrinth API: Create project `dzb1a5WV` / installed Create.

This is the obvious Farmer's Delight/Create bridge. It appears mostly code/tag integration rather than a large recipe datapack, but it still needs in-game JEI/playtesting.
