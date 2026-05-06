# 2026-05-06 preload crash archive

Temporarily removed from Tenpack client and live Prism instance after a NeoForge/Sinytra Connector preload crash.

Removed mods:

- `CameraOverhaul-v2.0.6-neoforge+mc[1.21.0-1.21.1].jar`
- `cloth-config-15.0.140-neoforge.jar`
- `fallingleaves-1.17.1+1.21.1.jar`

Reason:

- The crash log's mod-resolution failure recommended removing `cloth_config` and `fallingleaves`.
- CameraOverhaul's NeoForge metadata declares mandatory `cloth_config >=16.0.0`, but the available MC 1.21.1 NeoForge Cloth Config build is `15.0.140`, so leaving CameraOverhaul without Cloth Config would immediately fail dependency resolution.
- Falling Leaves was a Fabric mod being loaded through Sinytra Connector and participated in the Fabric resolution failure.

Live instance copies were moved to:

`/home/yeyito/Workspace/active-development/prism-launcher/prism-data/instances/tenpack/minecraft/mods.disabled/2026-05-06-preload-crash/`

Connector's `cloth_config` alias was also removed from `connector.json` because no Fabric mod in the active pack currently needs it.
