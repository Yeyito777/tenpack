# Tenpack Simple Clouds fork notes

This source tree is cloned from the public xvr6 Simple Clouds fork:

- Upstream: https://github.com/xvr6/simple-clouds
- Branch: `v0.8`
- Base commit: `9a8daa3de03ab53acb3e6f8b590d051d309b3368`
- Mod version: `0.8.0f-b5`

Tenpack uses the `simpleclouds-0.8.0f-b5-all.jar` JarJar artifact so the required CrackerLib and SimpleCloudsAPI dependencies stay bundled like the archived binary jar.

## Tenpack patch carried from the previous binary jar

`src/main/java/dev/nonamecrackers2/simpleclouds/mixin/MixinLevelRenderer.java` has a source-level version of the previous Tenpack binary patch from commit `4e8fb86` (`Patch Simple Clouds precipitation mixin conflict`).

The previous binary patch removed the active `@Redirect` annotations from the two configured-precipitation helper methods in `MixinLevelRenderer.class`:

- `simpleclouds$allowConfiguredDryBiomeRain_renderSnowAndRain`
- `simpleclouds$resolveConfiguredDryBiomePrecipitation`

The methods remain in source, but are no longer registered as mixin redirects. This avoids a startup-time precipitation mixin conflict with the rest of the Tenpack weather stack.

## Build

Use the Prism Java 21 runtime if the system default Java is newer than 21:

```bash
cd mods-src/simple-clouds
JAVA_HOME=/home/yeyito/Workspace/active-development/prism-launcher/prism-data/java/java-runtime-delta \
  PATH="$JAVA_HOME/bin:$PATH" \
  ./gradlew --no-daemon clean build
```

The jar to copy into the pack is:

```text
build/libs/simpleclouds-0.8.0f-b5-all.jar
```
