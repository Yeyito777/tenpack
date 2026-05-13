# Survival atmosphere mod intake

Requested pass: environmental danger / survival tension, exploration atmosphere, interaction/animation immersion, and utility/QoL. Epic Siege was intentionally excluded.

## Added to server and mirrored to client

Re-added by request after the stability sanity check:

- Simple Block Physics `simpleblockphysics-1.21.1-neoforge-1.2.0.jar`
- Nyf's Spiders `nyfsspiders-neoforge-1.21.1-3.0.1.jar`
- Danger Close `danger_close-neoforge-1.21.1-3.1.3.jar`
- MonoLib `monolib-neoforge-1.21.1-4.0.2.jar` — Danger Close dependency

Still quarantined from the server-side atmosphere batch pending one-at-a-time soak tests:

- Oh My, Meteors! `ohmymeteors-1.4.0+1.21+neo.jar`
- Grim kingdoms: Lost structures & ruins `grim-kingdoms-lost-structures-ruins-v1.0.3.jar`
- Wildex Bestiary `wildex-3.0.0.jar`
- Immersive Enchanting `immersiveenchanting-5.0.1-neoforge-1.21.1.jar`

## Added to client only

- Entity Texture Features `entity_texture_features_1.21-neoforge-7.1.jar`
- Entity Model Features `entity_model_features-3.2.4-1.21-neoforge.jar`
- Not Enough Animations `notenoughanimations-neoforge-1.12.3-mc1.21.1.jar`
- 3D Skin Layers `skinlayers3d-neoforge-1.11.1-mc1.21.1.jar`
- Tiny Item Animations `tia-neoforge-1.21-1.2.1.jar`
- Mouse Tweaks `MouseTweaks-neoforge-mc1.21-2.26.1.jar`
- Keybind Search `keybindsearch-1.0.0.jar`
- Pick Up Notifier `PickUpNotifier-v21.1.1-1.21.1-NeoForge.jar`
- Drip Sounds `Drip Sounds-0.5.2+1.21.8-NeoForge.jar`
- Make Bubbles Pop `make_bubbles_pop-0.3.0-fabric-mc1.19.4-1.21.jar` — Fabric build through Sinytra Connector
- Wakes `wakes-0.4.1+1.21.1.jar` — Fabric build through Sinytra Connector
- Tightfire `tightfire-1.21.1-1.0-SNAPSHOT.jar` — Fabric build through Sinytra Connector
- Dense Flowers `dense-flowers-0.2.2+mc1.21.0.jar` — Fabric build through Sinytra Connector
- Continuity `continuity-3.0.0+1.21.neoforge.jar` — connected texture support for the new connected packs

## Added resource packs

- Fresh Animations `FreshAnimations_v1.10.4.zip`
- Fresh Animations: Extensions `FA+All_Extensions-v1.8.1.zip`
- Extended Illumina `§eExtended_Illumina_1.21x.zip`
- Bee's Fancy Crops `Fancy Crops v1.3.zip`
- Connected Bricks `Connected-Bricks 1.14-1.21.3 v1.0.zip`
- Connected Paths `Connected-Paths 1.14-1.21.8 v2.1.1.zip`
- Connected Rocks `Connected-Rocks 1.14-1.21.8 v1.1.1.zip`
- Os' Colorful Grasses `Os' Colorful Grasses (Mix).zip`

## Added shader packs

Added but left disabled by default in `client/shaderpacks/tenpack-shaderpacks.json`:

- Amethyst Shaders `AmethystShaders_2.4.zip`
- Rethinking Voxels `rethinking-voxels_r0.1-beta9.zip`

## Added datapacks

- VeinMiner datapack `Veinminer-1.2.3.zip`
- VeinMiner Enchantment addon datapack `veinminer-enchantment-1.2.6.zip`

There was no NeoForge 1.21.1 VeinMiner mod jar in the Modrinth version list. The datapack route keeps the requested functionality available and the enchantment addon gives Tenpack an obvious balance hook.

## Not added in this pass

- Epic Siege — explicitly excluded by request.
- Flimsy Torches — no Minecraft 1.21.1 build found; Modrinth latest was 1.20.1.
- Light Dust — no Minecraft 1.21.1 build found; Modrinth latest was 1.20.1.
- Enhanced Visuals — no Minecraft 1.21.1 compatible mod build found during this pass.
- Live Texture Editor — no Minecraft 1.21.1 build found; also a developer/debug-oriented client tool, so not forced into the shipped client.
- Big Water — available builds found were for newer Minecraft versions, not 1.21.1.
- Seamless Sleep — removed after the initial deploy at user request.
- Cosy Critters & Creepy Crawlies [Neo] — removed after client crash: NPE in `CosyCritters.trySpawnBird` during leaf animate ticks.

## Risk notes for playtest

- Several visual client mods are Fabric builds running through Sinytra Connector because no NeoForge 1.21.1 build was available; if startup fails, start by temporarily removing `make_bubbles_pop`, `wakes`, `tightfire`, `swinginglanterns`, or `dense-flowers`.
- Connected texture packs may need client-side tuning with Continuity/Sodium; Indium was not added because this pack uses NeoForge Sodium, not Fabric Sodium.

## Stability quarantine notes

- Removed Leawind's Third Person after a client camera crash with Sable mixins.
- Removed Swinging Lanterns after smoke-test logs showed a Sodium mixin injection failure; it was cosmetic and not worth keeping as a known broken mixin.
- Quarantined the remaining server-side survival-atmosphere gameplay mods after the live server began refusing connections during smoke testing; Danger Close, MonoLib, Nyf's Spiders, and Simple Block Physics were later re-added by request.
