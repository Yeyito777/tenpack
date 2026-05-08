# Death, corpses, and lifesteal

Status: local implementation, not pushed. Needs in-game testing before deployment.

## Implemented

- Lifesteal is installed on both client and server:
  - `lifesteal-9.3.3+1.21.1.jar`
  - Config: `client/config/lifesteal-common.toml`, `server/config/lifesteal-common.toml`
  - `Number of HitPoints lost/given upon death/kill = 2` (1 heart)
  - Player deaths, mob deaths, and environmental deaths all remove hearts from the victim.
  - Lifesteal is enabled, so player kills transfer the heart to the killer.

- Corpse is installed on both client and server:
  - `corpse-neoforge-1.21.1-1.1.13.jar`
  - Config: `client/config/corpse-server.toml`, `server/config/corpse-server.toml`
  - Corpses store player item drops instead of letting them scatter as normal item entities.
  - Corpses are owner-only for 60 seconds.
  - After 60 seconds, the corpse reaches skeleton stage and becomes lootable by anyone.
  - Empty corpses despawn after 30 seconds.
  - `lava_damage = false` and `fall_into_void = false`, matching the desired lava/void safety baseline.

- Tenpack Death is installed on both client and server:
  - `tenpackdeath-0.1.0.jar`
  - Source: `mods-src/tenpack-death/`
  - Requires Corpse and layers Tenpack-specific behavior on top of it.
  - After a corpse has existed for 5 minutes, it warns the corpse owner if online.
  - From then on, the corpse loses one random stored item stack every 30 seconds until it is looted or empty.

## Experience / enchanting interaction

Corpse/CoreLib records the player's experience level in the stored death snapshot (`Experience = player.experienceLevel`), but the normal Corpse item-drop hook only clears item drops. The public Corpse documentation and the inspected CoreLib death hook do not show XP-orb interception.

Practical expectation for testing: items go into the corpse; vanilla XP behavior likely still applies unless another mod changes it. This needs an in-game death test against the final enchanting setup.

## Test checklist

- Die to environment: lose 1 heart, no killer gains a heart, items are in corpse.
- Die to mob: lose 1 heart, no player gains a heart, items are in corpse.
- Die to player: victim loses 1 heart, killer gains 1 heart, items are in corpse.
- During first 60 seconds, only corpse owner can open/loot.
- After 60 seconds, another player can open/loot.
- At 5 minutes, owner receives decomposition warning if online.
- After 5 minutes, one random corpse item stack disappears every 30 seconds.
- Die in lava: corpse survives and items are recoverable.
- Check XP dropped/stored behavior and decide whether this fits the enchanting system.
- Check what Lifesteal does at zero hearts: ban vs spectator vs revive flow.
