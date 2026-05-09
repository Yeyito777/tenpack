# Create: Estrogen port plan

Status: **not added to Tenpack yet**.

Tenpack is Minecraft `1.21.1` NeoForge with Create `6.0.10`. Create: Estrogen currently does not have a known 1.21.1 NeoForge release, so this is a porting project rather than a normal mod-add.

## Source audit

Source checked locally from:

- `https://github.com/MayaqqDev/Create-Estrogen`
- cloned to `/tmp/Create-Estrogen` for audit

License:

- LGPL-3.0-or-later according to `LICENSE`

Current upstream build target from `gradle.properties` / `libs.versions.toml`:

- mod version: `1.0.1+1.20.1`
- Minecraft: `1.20.1`
- Forge: `47.4.0`
- Fabric loader: `0.17.2`
- Create Forge: `6.0.8-290` for `1.20.1`
- Create Fabric: `6.0.8.0+build.1734-mc1.20.1`
- Estrogen dependency: `5.0.8+1.20.1`
- KubeJS optional/runtime toggle exists and defaults enabled upstream
- item viewer support can be EMI/JEI/REI/disabled upstream; Tenpack uses JEI

Code size from quick audit:

- about 63 Java/Kotlin source files
- about 26 JSON/data-ish files
- common + Fabric + Forge source sets
- Forge-specific code uses Forge APIs, not NeoForge APIs
- build uses Cloche multi-loader setup

## Why this is not a quick drop-in

1. **Loader gap** — upstream has Forge/Fabric/Quilt-era packaging, not a published NeoForge 1.21.1 artifact.
2. **Minecraft jump** — 1.20.1 to 1.21.1 means registry/data/component/recipe/API drift.
3. **Create jump** — upstream targets Create 6.0.8 on 1.20.1, while Tenpack targets Create 6.0.10 on 1.21.1.
4. **Dependency gap** — the base Estrogen mod itself must also exist/build for 1.21.1 NeoForge; Create: Estrogen depends on it.
5. **Kotlin/build stack** — Kotlin, KFF/KotlinLangForge, Cloche, Kritter, Botarium, Baubly, Ponder/Flywheel, and item viewer dependencies all need 1.21.1/NeoForge-compatible coordinates.
6. **Balance audit required** — recipes and processing could bypass Tenpack's fluid/precision progression if added unreviewed.

## Minimum viable port checklist

1. Confirm or port base `Estrogen` to 1.21.1 NeoForge first.
2. Convert the Create: Estrogen Forge target to NeoForge or add a NeoForge target.
3. Update Minecraft/NeoForge/Create/Flywheel/Ponder/JEI/Kotlin dependency coordinates.
4. Disable optional KubeJS runtime for Tenpack unless explicitly adopting KubeJS later.
5. Build common + NeoForge jars.
6. Launch a clean NeoForge 1.21.1 client/server with Tenpack's Create stack.
7. Inspect all generated recipes in JEI.
8. Add datapack overrides if any Estrogen recipes bypass:
   - fluid/spout/drain era;
   - precision mechanism era;
   - Create logistics/packaging era;
   - flight/fuel/heavy industry gates.
9. Add questbook notes only after the mechanics are confirmed stable.

## Tenpack design target if ported

Create: Estrogen should be a flavor/processing side path, not a mainline bypass.

Good role:

- colorful factory chemistry;
- fun fluids/items that make Create builds feel weirder and more social;
- optional automation branch using existing Create machines;
- possible trade good if production has visible inputs/outputs.

Bad role:

- early access to advanced Create processing;
- free fluids/power/material loops;
- hidden one-block progression skips;
- mandatory joke chain that confuses new Create players.

## Recommendation

Do not block current Create rollout on Estrogen. Ship the stable Create/JEI/Farmer's Delight/Central Kitchen stack first, then treat Estrogen as a separate source-port branch.
