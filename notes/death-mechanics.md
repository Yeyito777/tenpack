# Tenpack death mechanics

This document describes the intended player-facing death rules for Tenpack's factions/Create SMP.

## Quick summary

Death is meant to hurt, but not instantly end participation:

- Every death costs the dead player 1 heart.
- If the death was caused by another player, that heart is transferred to the killer.
- If the death was caused by mobs, lava, fall damage, machines, or other environment, the victim still loses 1 heart and nobody gains it.
- The dead player's items go into a Corpse entity instead of scattering as loose drops.
- The corpse is private until it visibly becomes a skeleton.
- Once skeletonized, anyone can loot it or break it open.
- Full player XP is paid out through the corpse, not through vanilla's capped death XP drop.
- Reaching zero hearts puts the player in spectator for now, and creates a revive-head objective.

## Mods and custom layer

The system is built from three pieces:

- **Lifesteal**
  - Handles heart loss, heart transfer on PvP kills, elimination, revive heads, heart crystals, and `/ls withdraw`.
- **Corpse**
  - Stores a dead player's items in a corpse entity.
- **Tenpack Death**
  - Local custom mod at `mods-src/tenpack-death/`.
  - Adds Tenpack-specific corpse protection, corpse decay, corpse breaking, full XP storage/drop, death sound, and Simple Voice Chat corpse-icon suppression.

Config files:

- `server/config/lifesteal-common.toml`
- `server/config/corpse-server.toml`
- `server/config/tenpackdeath.properties`
- client copies exist under `client/config/` for sync/local parity.

## Heart loss and lifesteal

Current Lifesteal values:

```toml
"Number of HitPoints lost/given upon death/kill:" = 2
"Disable Lifesteal:" = false
```

Minecraft uses 2 hitpoints per heart, so this means:

- death costs 1 heart;
- PvP kills give the killer 1 heart;
- mob/environment deaths remove 1 heart from the victim without giving it to anyone.

This makes environmental danger drain hearts out of the economy while PvP transfers hearts between players.

## Zero hearts and revival

Current zero-heart behavior:

```toml
"Spawn Revive Head upon player elimination:" = true
"Ban players upon losing all hearts:" = false
```

So when a player loses all hearts:

1. They are eliminated into spectator mode rather than banned.
2. TenpackDeath re-enforces spectator on respawn/login while Lifesteal's `TIME_KILLED` marker is still set.
3. Lifesteal creates a revive head tied to that player.
4. Another player/faction can use Lifesteal's revive mechanics to bring them back.

Revive heads are intentionally movable:

```toml
"Indestructible Revive Heads:" = false
```

Lifesteal's revive-head loot table copies the player profile component, so a broken/picked-up revive head should preserve who it revives.

## Heart transfer without killing

Players can transfer hearts with Lifesteal's withdraw command:

```mcfunction
/ls withdraw
/ls withdraw <amount>
```

Current config unlocks this from day one:

```toml
"Advancement needed to unlock Withdrawing:" = ""
"Permission Level:" = 0
```

This turns the player's own hearts into physical heart-crystal items. Those items can be gifted, traded, ransomed, stolen, transported, or stored in faction vaults.

## Heart crystals, revive crystals, and ore economy

Heart crystal progression still exists, but its ore source is intentionally rare.

Tenpack adds a server datapack:

```text
server/world/datapacks/tenpack-lifesteal-balance/
```

It changes Lifesteal's default heart crystal worldgen:

- Overworld heart ore:
  - default: 6 attempts per chunk, Y -50..70
  - Tenpack: 1 attempt every ~6 chunks, Y -58..-16
- Nether heart ore:
  - default: 6 attempts per chunk, Y 20..100
  - Tenpack: 1 attempt every ~4 chunks, Y 8..40
- Overworld heart geodes:
  - default chance 50
  - Tenpack chance 256
- Nether heart geodes:
  - default chance 30
  - Tenpack chance 192

The goal is old-school-diamond rarity: heart crystals and revive crystals remain possible, but producing them should be a faction-level project.

Worldgen changes only affect newly generated chunks.

## Corpse creation

On death:

- Corpse captures the player's inventory.
- Items should not scatter normally.
- The corpse appears at/near the death location.
- Corpse lava/void protection is enabled:

```toml
fall_into_void = false
lava_damage = false
```

So lava deaths should still leave a recoverable corpse.

## Corpse protection and looting

Public looting is tied to visible skeleton state.

Current Corpse/TenpackDeath settings:

```toml
# corpse-server.toml
skeleton_time = 1200
only_owner = false
skeleton = false
```

```properties
# tenpackdeath.properties
ownerProtectionEnabled=true
publicLootRequiresSkeleton=true
publicLootAfterSeconds=60
opsBypassProtection=false
notifyDeniedAccess=false
```

Important details:

- `1200` ticks = 60 seconds.
- Corpse's built-in owner-only access is disabled to avoid Corpse's own chat messages.
- TenpackDeath enforces the protection silently.
- The dead player can open their own corpse immediately.
- The dead player can attack or shift-right-click their own corpse to spill/destroy it at any time.
- Non-owners cannot open it until the corpse is skeletonized.
- OPs do not bypass corpse protection by default, so OP testing should reflect normal player behavior.
- No access-denied chat is shown by default.

## Empty corpses

Empty corpses need to survive long enough to become skeletons, because a player may die with no items but still have XP stored in the corpse.

Current setting:

```toml
time = 2400
```

`2400` ticks = 120 seconds before an empty corpse despawns.

## Corpse breaking and XP payout

Once a corpse is skeletonized, it can be broken/opened by other players.

Current settings:

```properties
breakCorpseDropsAfterDecay=true
requireDecayStartedToBreakCorpse=true
breakCorpseAtSkeleton=true
breakCorpseDropsExperience=true
```

Behavior:

- attacking a skeletonized corpse spills remaining items and XP;
- shift-right-clicking a skeletonized corpse also spills remaining items and XP;
- XP is stored as the dead player's current spendable XP at death, calculated from level + XP-bar progress;
- vanilla's partial/capped player-death XP drop is suppressed;
- the full stored XP is awarded from the corpse as XP orbs.

This means killing/looting a player can recover the full XP value if the corpse is opened/broken after it becomes public.

## Corpse decomposition

Corpse decomposition is handled by TenpackDeath.

Current settings:

```properties
decayEnabled=true
decayStartSeconds=300
decayIntervalSeconds=30
randomDecay=true
notifyDecayStarted=false
notifyInternalErrors=false
```

Behavior:

- decay starts after 5 minutes;
- once decay starts, one random stored item stack disappears every 30 seconds;
- there are no per-stack chat messages;
- decay-start chat is also disabled by default;
- internal corpse-decay errors are logged server-side, not shown to players by default.

## Death sound

On player death, TenpackDeath plays a custom Return-by-Death-style sound.

Source asset:

```text
mods-src/tenpack-death/src/main/resources/assets/tenpackdeath/sounds/return_by_death.ogg
```

The sound is registered as:

```text
tenpackdeath:return_by_death
```

Config:

```properties
deathSoundEnabled=true
deathSoundVolume=1.0
```

## Simple Voice Chat speaking icon fix

Simple Voice Chat can display speaking/name-tag icons above entities. Corpse entities can resemble players, so TenpackDeath includes a Simple Voice Chat plugin that cancels voice icons for corpses.

The plugin checks both:

- the corpse entity UUID;
- the corpse owner's player UUID.

Source:

```text
mods-src/tenpack-death/src/main/java/dev/yeyito/tenpackdeath/voice/TenpackVoicechatPlugin.java
```

## Player-facing timeline

Assuming default current config:

1. **Death happens**
   - victim loses 1 heart;
   - killer gains 1 heart if it was a PvP kill;
   - current spendable XP is captured for the corpse;
   - custom death sound plays;
   - inventory goes into corpse.

2. **0-60 seconds**
   - only corpse owner can open corpse;
   - non-owners are silently blocked.

3. **After ~60 seconds**
   - corpse becomes a skeleton;
   - anyone can open/loot it;
   - anyone can attack or shift-right-click it to spill remaining items and full stored XP.

4. **After 5 minutes**
   - corpse silently starts decomposing;
   - one random item stack disappears every 30 seconds.

5. **At zero hearts**
   - player goes spectator;
   - respawn/login keeps them in spectator until revived;
   - revive head exists as the faction/social objective for bringing them back.

## Things to test after changes

- PvE/environment death removes exactly 1 heart and gives nobody a heart.
- PvP death removes 1 heart from victim and gives 1 heart to killer.
- `/ls withdraw <amount>` works for new players without an advancement.
- Owner can open corpse immediately.
- Non-owner cannot open corpse before skeleton stage.
- No corpse-protection chat appears by default.
- Corpse becomes public around 60 seconds.
- Skeletonized corpse can be attacked/shift-right-clicked to spill remaining items.
- Empty-inventory corpse can still be broken after skeleton stage for stored XP.
- Current spendable XP, not vanilla partial XP, comes from the corpse.
- Lava deaths leave recoverable corpses.
- Player at zero hearts becomes spectator and can be revived through revive head flow.
- Revive heads can be picked up and still preserve the player they revive.
- Corpse does not show Simple Voice Chat speaking icons.
