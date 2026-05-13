# Tenpack travel overhaul subtask A — companion/animal source audit

Date: 2026-05-12
Pack target: NeoForge 1.21.1
Scope: audit the current companion/animal source stack for Tenpack Travel overhaul work. This is a source and integration map, not a balance pass.

## Summary

The companion/animal foundation is strong and already mostly aligned with the travel philosophy:

- **Alex's Mobs** is the main gameplay source for exotic companions, mounts, pack animals, scouts, and dimension-specific travel.
- **Vanilla horses/donkeys/mules/llamas/camels** remain the early/mainline travel baseline.
- **Tenpack Travel** is the correct custom layer for pack-specific inspection, bonding, whistle, camp care, and role glue.
- **LeashAll** is the right third-party source for broad physical leading, provided the blocklist survives multiplayer playtest.
- **Respawning Animals** supports ecology/farming availability but is not a companion-control system.
- **Carry On** is adjacent physical logistics, but should not become an animal/backpack bypass.

The main integration gap is not lack of animals. It is **explicit source-of-truth boundaries**: Tenpack Travel currently keeps a curated subset of Alex animal role cards in code with localized text keys, while bonding/whistle logic uses broad Minecraft class checks. That is a good v1, but the next overhaul should turn role/bond eligibility into a small data/config surface so adding or excluding animals does not require chasing code paths.

## Active companion/animal jars

All relevant jars are present on both client and server unless noted.

| Source | Jar | License from jar metadata | Tenpack role | Audit read |
| --- | --- | --- | --- | --- |
| Vanilla Minecraft | built-in | proprietary game code | horse/donkey/mule/llama/camel baseline | Baseline remains important; Tenpack Travel should not skip vanilla animals while adding exotic roles. |
| Alex's Mobs | `alexsmobs-1.22.17.jar` | GPL-3.0 | primary animal/ecology/companion source | Best source for travel identity. Integrate by registry ID and optional behavior, not by vendoring code. |
| Citadel | `citadel-1.21.1-2.7.6.jar` | LGPL | Alex's Mobs dependency | Library only; avoid direct Tenpack logic unless needed. |
| Tenpack Travel | `tenpack_travel-0.1.0.jar` / `mods-src/tenpack-travel` | MIT | pack-specific animal travel glue | Correct home for brush/bond/whistle/camp blocks/role actions. Build currently passes. |
| LeashAll | `leashall-neoforge-1.21.1-1.3.1-1.21.1.jar` | MIT | expanded physical lead handling | Good fit: physical, configurable, no teleport. Needs multiplayer abuse test. |
| Respawning Animals | `RespawningAnimals-v21.1.2-1.21.1-NeoForge.jar` | MPL-2.0 | passive ecology replenishment | Supports farms/ecology, not companion control. Watch spawn/despawn interactions with named/tamed animals in playtest. |
| Carry On | `carryon-neoforge-1.21.1-2.2.4.4.jar` | LGPLv3 | adjacent physical logistics | Useful vibe, but potential container/animal/logistics abuse vector. Not a companion system. |

## Config source state

### Alex's Mobs

- Found: `tenpack-specs/overrides/config/alexsmobs-common.toml`.
- Not found as active live config: `client/config/alexsmobs-common.toml` or `server/config/alexsmobs-common.toml`.
- Important existing override values:
  - `giveBookOnStartup = true`
  - `raccoonStealFromChests = true`
  - `crowsStealCrops = true`
  - `seagullStealing = true`
  - `straddleboardEnchants = true`
  - `falconryTeleportsBack = false`
  - `tusklinShoesBarteringChance = 0.025...`
  - weather/worldgen-adjacent knobs exist (`limitGusterSpawnsToWeather`, `mungusBiomeTransformationType`) and should remain partner/deferred territory.

Audit conclusion: if deterministic Alex behavior matters for the live pack, copy or otherwise materialize the common config into active client/server config. Do not tune weather/worldgen settings during the travel overhaul.

### LeashAll

Found active defaults:

- `server/defaultconfigs/leashall-server.toml`
- `client/defaultconfigs/leashall-server.toml`

Current stance:

```toml
useEntityAllowList = false
maxLeashesPerPlayer = 6
blockedEntities = ["#c:bosses", "#minecraft:raiders", "minecraft:warden", "minecraft:ender_dragon", "minecraft:wither", "minecraft:elder_guardian", "minecraft:villager", "minecraft:wandering_trader", "minecraft:player"]
allowedEntities = []
```

Audit conclusion: this is a reasonable default for caravan handling. Playtest should specifically try leashing hostile Alex mobs, livestock groups, tamed pets, raiders, bosses, villagers, and players.

## Tenpack Travel source map

Current source files under `mods-src/tenpack-travel/src/main/java/dev/yeyito/tenpacktravel/`:

| File | Current responsibility | Source audit notes |
| --- | --- | --- |
| `TenpackTravel.java` | mod registration, blocks/items, creative tabs, client registration, event handler registration | Main class is now appropriately thin. Good direction. |
| `AnimalInspectionReport.java` | builds brush output for vanilla mounts/camels and curated Alex roles | Uses registry IDs for Alex's Mobs, avoiding hard dependency. Good v1. Role text is now localized, while role coverage remains code-curated. |
| `AlexAnimalRole.java` | curated Alex role-card lookup backed by `en_us.json` keys | Good documentation-in-code, now covered by a regression checklist; should eventually become data-driven if the role list grows. |
| `AnimalStatBands.java` | health/speed/jump/debug formatting bands | Correctly separated. Keep survival output approximate. |
| `GroomingBrushHandler.java` | entity interaction with brush, inspection display, brush damage, brush care XP | Clean split from main class. Cancels even unsupported entities after message, which is acceptable but should be verified against modded interaction edge cases. |
| `AnimalBond.java` | persistent per-player bond data on animals | Stores `tenpack_travel_bond`; good embodiment-first model. Eligibility is broad (`Animal`, `Camel`, `AbstractHorse`). |
| `MountedTravelBondHandler.java` | ride-start and moving-mounted bond XP | Good anti-idle-farm threshold. Broad bondability means many ride-hook Alex animals can start relationships. |
| `WhistlePayload.java` / `TenpackTravelNetwork.java` / `WhistleHandler.java` | client keybind to server whistle handling | No teleport; range/response limited by XP and loaded entities. Good Tenpack fit. |
| `AnimalRoles.java` / `AnimalRoleActions.java` | active role lookup and bald eagle scout behavior | Excellent start; currently only active role is `SCOUT` for `alexsmobs:bald_eagle`. |
| `HitchingPostBlock.java` | fence-like physical lead parking and listing | Uses vanilla lead/fence-knot behavior; correct no-recall posture. |
| `FeedTroughBlock.java` | consumes tagged feed, heals nearby eligible working animals, care XP | Good physical camp-care loop; feed eligibility is broad enough for v1 but should be playtested with exotic carnivores/aquatics. |
| `TenpackTravelClient.java` | client keybind registration/send | Client-only reflection registration avoids server classloading issue. |

Build validation run during this audit:

```text
cd /home/sisyphus/Workspace/tenpack/mods-src/tenpack-travel && gradle build --no-daemon
BUILD SUCCESSFUL
```

## Alex's Mobs source/class audit

Method used:

- Read jar metadata.
- Used `javap` against `client/mods/alexsmobs-1.22.17.jar` and `client/mods/citadel-1.21.1-2.7.6.jar` to identify inheritance/interfaces and public hooks.
- Compared interesting Alex entity classes against Tenpack Travel's current `AlexAnimalRole` curated role table.

### Current Tenpack Travel role coverage

`AlexAnimalRole` currently has 28 role entries:

- `bald_eagle`
- `bison`
- `caiman`
- `capuchin_monkey`
- `catfish`
- `cosmaw`
- `crocodile`
- `crow`
- `elephant`
- `emu`
- `kangaroo`
- `grizzly_bear`
- `gorilla`
- `komodo_dragon`
- `endergrade`
- `laviathan`
- `mantis_shrimp`
- `mimic_octopus`
- `mudskipper`
- `raccoon`
- `rhinoceros`
- `seagull`
- `seal`
- `spectre`
- `sugar_glider`
- `tarantula_hawk`
- `tusklin`
- `warped_toad`

This covers the important travel design candidates from the earlier `animal-travel-audit.md`.

### Interesting Alex classes not currently in the role table

These are not necessarily bugs. Most are ambient, resource, bucket fish, hostile, or gear-source creatures. They are listed because their bytecode/class signatures show taming, bucket, flying, container, or ride hooks and they may come up during playtest.

| Entity path inferred from class | Source signal | Current recommendation |
| --- | --- | --- |
| `flutter` | `TamableAnimal`, `IFollower`, flying | Optional cute/ecology companion. Add a brush role only if players actually use it. |
| `orca` | `TamableAnimal` signal in bytecode | Earlier audit treated as ocean ally/flavor, not mount/cargo. Verify actual trust behavior before adding role text. |
| `blobfish`, `comb_jelly`, `cosmic_cod`, `devils_hole_pupfish`, `flying_fish`, `frilled_shark`, `lobster`, `platypus`, `stradpole`, `terrapin`, `triops` | bucketable/fish/resource signals | Do not add to Tenpack Travel core unless quest/guide wants bucket-pet flavor. |
| `enderiophage`, `sunbird`, `soul_vulture` | flying/support signals | Keep as ambient/gear/mythic support unless travel role is promoted. User TODO mentions vulture; soul vulture is the likely candidate to review separately. |
| `skelewag`, `warped_mosco` | ride-hook signal | Likely internal/hostile passenger behavior, not player companion source. Do not promote without direct playtest. |
| `straddleboard` | ride-hook entity | Not an animal; travel item/entity already covered in design docs, but not brush role material. |
| `murmur_head`, `fly` | flying/non-companion signal | Ignore for companion system. |

### Important class-source takeaways

- Several Alex animals implement `TamableAnimal` and/or `IFollower`; Tenpack Travel can detect vanilla ownership through `OwnableEntity` without compiling against Alex.
- Some Alex travel entities are not `TamableAnimal` (`endergrade`, `laviathan`, `tusklin`, `spectre`) but are still `Animal`/ride-hook entities; broad Tenpack bonding can include them if the player can ride/use them.
- Some classes expose ride hooks for internal behavior or non-player passengers. Do not assume every `getControllingPassenger` / `positionRider` class is a Tenpack mount.
- Direct compile dependency on GPL Alex source is unnecessary for current needs. Keep registry-ID/attribute integration unless a very specific behavior requires an optional compat module.

## Source boundary recommendations

### Keep as source of truth

1. `notes/animal-travel-audit.md` remains the design/balance source for which animals matter and why.
2. `AlexAnimalRole.java` is currently the code source for brush role coverage; `en_us.json` is the text source for those cards.
3. `AnimalRoles.java` / `AnimalRoleActions.java` is the code source for active roles.
4. `AnimalBond.java` is the source for bond eligibility/persistence.
5. `server/defaultconfigs/leashall-server.toml` is the source for broad lead permissions.

### Make explicit in the next subtask

- Add a small data/config layer for Tenpack animal role metadata, or at minimum centralize all animal eligibility in one code table.
- Separate concepts that are currently adjacent but not identical:
  - inspectable by brush,
  - bondable,
  - whistle responder,
  - trough eligible,
  - active-role capable,
  - leash allowed by LeashAll.
- Keep a fallback for vanilla `Animal`/`AbstractHorse`, but use explicit registry IDs for exotic active roles.

## Risk register

| Risk | Why it matters | Suggested test/mitigation |
| --- | --- | --- |
| Broad `AnimalBond.isBondable` includes nearly every `Animal` | Whistle may eventually involve animals that should not behave like companions once they have XP. | Keep whistle XP-gated; consider explicit deny/allow table before adding more active roles. |
| `FeedTroughBlock` heals any adult leashed `Animal` | Could heal odd/hostile/exotic animals if LeashAll permits them. | Multiplayer test with LeashAll; maybe restrict trough eligibility to role table + vanilla livestock/mounts. |
| LeashAll allowlist is off | Physical caravan power may be too broad on hostile/rare mobs. | Try obvious abuse cases; switch to allowlist if needed. |
| Alex config exists only in spec overrides | Live pack may not match documented config assumptions. | Materialize active client/server config if deterministic behavior is required. |
| Respawning Animals interactions with tamed/named animals are unverified | Companion loss would be catastrophic bad friction. | Confirm named/tamed/owned/leashed animals do not despawn unexpectedly. |
| Role cards are hardcoded | Easy to drift from docs after future animal decisions. | Data-drive or add checklist/script comparing docs/code. |
| GPL Alex source vs MIT Tenpack code | Avoid license ambiguity. | Do not copy Alex source into Tenpack Travel. Use registry IDs, attributes, public Minecraft/NeoForge APIs, and observed behavior. |

## Subtask A conclusion

No new animal content is needed before the travel overhaul continues. The current sources are enough:

- Alex's Mobs supplies the animal behaviors.
- Tenpack Travel supplies the pack-specific ritual/control layer.
- LeashAll supplies physical handling.
- Vanilla supplies the early mount/cargo baseline.

Recommended next subtask: **define the Tenpack animal eligibility matrix** as code/data: inspectable, bondable, whistle responder, trough eligible, leash policy, active roles. That will make the next implementation pass safer than adding more behavior directly to broad `Animal` checks.
