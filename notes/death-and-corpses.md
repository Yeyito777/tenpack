# Death, corpses, and lifesteal

Status: implemented/pushed. For the current player-facing design, see `notes/death-mechanics.md`.

## Implemented

- Lifesteal is installed on both client and server:
  - `lifesteal-9.3.3+1.21.1.jar`
  - Config: `client/config/lifesteal-common.toml`, `server/config/lifesteal-common.toml`
  - `Number of HitPoints lost/given upon death/kill = 2` (1 heart)
  - Player deaths, mob deaths, and environmental deaths all remove hearts from the victim.
  - Lifesteal is enabled, so player kills transfer the heart to the killer.
  - Losing all hearts currently sends the player to spectator instead of banning them.

- Corpse is installed on both client and server:
  - `corpse-neoforge-1.21.1-1.1.13.jar`
  - Config: `client/config/corpse-server.toml`, `server/config/corpse-server.toml`
  - Corpses store player item drops instead of letting them scatter as normal item entities.
  - Corpse native owner-only messages are disabled; TenpackDeath enforces silent owner-only protection until skeleton stage.
  - `lava_damage = false` and `fall_into_void = false`, matching the desired lava/void safety baseline.
  - Empty corpses are kept around for 120 seconds so empty-inventory deaths can still reach skeleton stage and be broken for XP.

- Tenpack Death is installed on both client and server:
  - `tenpackdeath-0.1.0.jar`
  - Source: `mods-src/tenpack-death/`
  - Config: `client/config/tenpackdeath.properties`, `server/config/tenpackdeath.properties`
  - Requires Corpse and layers Tenpack-specific behavior on top of it.
  - Non-owners are silently blocked from opening a corpse until Corpse marks it as a skeleton.
  - Public looting is therefore tied to the visible skeleton state, not just a hidden timer.
  - Ops do not bypass protection by default, so OP testing should still show the protection.
  - After a corpse has existed for 5 minutes, decay silently starts. Chat notifications are disabled by default.
  - From then on, the corpse loses one random stored item stack every 30 seconds until it is looted or empty. It does not notify each lost stack.
  - Once the corpse is a skeleton, attacking it or shift-right-clicking it spills all remaining items and drops all stored XP as orbs.
  - Vanilla partial death XP drops are suppressed for players; the full total XP is recorded into TenpackDeath and paid out through the corpse.
  - Player death plays the custom `tenpackdeath:return_by_death` sound.
  - Simple Voice Chat speaking/name-tag icons are cancelled for Corpse entities, matching both corpse entity UUID and corpse owner UUID, so corpses do not look like talking players.

## Heart ore / crystal economy

Added server datapack: `server/world/datapacks/tenpack-lifesteal-balance/`.

This overrides Lifesteal's default heart crystal worldgen so heart ore is closer to old-school diamond hunting:

- Overworld ore: from 6 attempts per chunk at Y -50..70 to 1 attempt every ~6 chunks at Y -58..-16.
- Nether ore: from 6 attempts per chunk at Y 20..100 to 1 attempt every ~4 chunks at Y 8..40.
- Overworld heart geodes: from chance 50 to chance 256.
- Nether heart geodes: from chance 30 to chance 192.

This keeps revive crystals possible, but makes heart crystal production a faction-level mining/project goal. It only affects newly generated chunks.

## Revive heads

Lifesteal's `Spawn Revive Head upon player elimination` option means that when a player loses all hearts, the mod can place/drop a special revive head tied to that eliminated player. Other players use the Lifesteal revive mechanics/items around that head to bring the eliminated player back. This is separate from the Corpse loot system.

For now, zero-heart behavior is set to spectator, not ban, so we can test the revive flow without accidentally locking someone out of the server.

Revive heads are pick-up-able/movable because Lifesteal `Indestructible Revive Heads` is disabled. Lifesteal loot tables copy the player profile component, so broken revive heads should preserve who they revive.

## Experience / enchanting interaction

TenpackDeath suppresses vanilla's partial player-death XP drop, records the dead player's full `totalExperience`, stores it on the corpse, and pays it out through the corpse when a skeletonized corpse is attacked or shift-right-clicked.

## Test checklist

- Die to environment: lose 1 heart, no killer gains a heart, items are in corpse.
- Die to mob: lose 1 heart, no player gains a heart, items are in corpse.
- Die to player: victim loses 1 heart, killer gains 1 heart, items are in corpse.
- Owner can open their corpse before skeleton stage.
- Non-owner cannot open/loot before skeleton stage, even if OP.
- After skeleton stage, another player can open/loot.
- At 5 minutes, decay starts silently by default.
- After 5 minutes, one random corpse item stack disappears every 30 seconds with no per-stack notification.
- Once the corpse is a skeleton, attacking or shift-right-clicking the corpse drops all remaining items and all stored XP.
- Death plays the Return-by-Death-inspired sound cue.
- Die in lava: corpse survives and items are recoverable.
- Check XP dropped/stored behavior and decide whether this fits the enchanting system.
- Check what Lifesteal does at zero hearts: currently spectator, revive head enabled, ban disabled.
