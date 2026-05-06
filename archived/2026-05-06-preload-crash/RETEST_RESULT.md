# Retest result

User requested trying to re-add the disabled mods with an updated Cloth Config.

Findings:

- There is no compatible Cloth Config v16+ for Minecraft 1.21.1 / NeoForge 21.1.x.
- Cloth Config `16.0.143-neoforge` exists, but it is for Minecraft 1.21.2/1.21.3 and declares `neoforge >=21.2.0-beta`, so it cannot be used on the current MC 1.21.1 / NeoForge 21.1.228 instance.

Working experiment:

- Restored Camera Overhaul and Falling Leaves.
- Replaced NeoForge Cloth Config v15 with Fabric Cloth Config v15 (`cloth-config-15.0.140-fabric.jar`) through Sinytra Connector.
- Locally patched Camera Overhaul's `META-INF/neoforge.mods.toml` dependency from `cloth_config >=16.0.0` to `cloth_config >=15.0.0`.

Result:

- Offline Prism launch as `ExoTest2` reached client/resource loading successfully with no NeoForge pre-loading incompatibility block.
- Log showed resource packs loading with `mod/cameraoverhaul`, `mod/cloth_config`, and `mod/fallingleaves` present.
- Test process was terminated after confirming startup success.
