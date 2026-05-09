# Create next integrations

Status note for follow-up passes after the Create progression/oilfield work.

## Immediate: JEI

Installed in the local working tree:

- Just Enough Items `19.27.0.340` for Minecraft `1.21.1` NeoForge
- File: `client/mods/jei-1.21.1-neoforge-19.27.0.340.jar`
- Source: `https://cdn.modrinth.com/data/u6dRKJwZ/versions/YAcQ6elZ/jei-1.21.1-neoforge-19.27.0.340.jar`

Reason: Tenpack Create progression depends on players being able to inspect modified recipes. Advancements and a future questbook can explain the path, but JEI should be the recipe source of truth.

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

Available for Minecraft `1.21.1` NeoForge.

Latest compatible version found:

- Farmer's Delight `1.21.1-1.3.1`
- File: `FarmersDelight-1.21.1-1.3.1.jar`
- Source: `https://cdn.modrinth.com/data/R2OftAxM/versions/9gp7w8NC/FarmersDelight-1.21.1-1.3.1.jar`

Do not just add it blindly. It changes food economy, farming incentives, comfort effects, and early-game survival pacing. Tenpack already has Legendary Survival Overhaul temperature enabled and various survival/world-atmosphere systems, so food effects should be checked before deployment.

Recommended add pass:

1. Add Farmer's Delight to both client and server.
2. Add Create: Central Kitchen if compatible with the installed Create version.
3. Audit recipes for Create progression bypasses.
4. Check food effects against Legendary Survival Overhaul and Tenpack death/faction pacing.
5. Add a short questbook chapter for cooking/farms only if it creates useful player-facing progression.

## Create: Central Kitchen

Available for Minecraft `1.21.1` NeoForge and Create `6.0.10`.

Latest compatible version found:

- Create: Central Kitchen `2.4.0`
- File: `create-central-kitchen-2.4.0.jar`
- Source: `https://cdn.modrinth.com/data/btq68HMO/versions/TUJIHmUh/create-central-kitchen-2.4.0.jar`
- Required dependency from Modrinth API: Create project `dzb1a5WV` / installed Create.

This is the obvious Farmer's Delight/Create bridge. Add it in the same pass as Farmer's Delight, not separately, so recipe/functionality interactions can be audited together.

## Important current repo caveat

The current checked-in `client/mods` and `server/mods` directories do not contain Create-family jars even though the Create progression datapack is present. The previous recipe audit was done against local/test-instance Create-family jars. Before deploying a full Create pack state, verify which Create jars are meant to be checked into this repo versus supplied by another sync/source.

Known target from the progression audit:

- Create `1.21.1-6.0.10`

Related addons referenced by the datapack/checker include:

- Create Aeronautics / Simulated / Offroad
- Create Encased
- Create Diesel Generators
- Create Big Cannons

If those are not actually in the deployed pack, the conditional datapack recipes will fail gracefully where possible, but players will not have the intended Create experience.
