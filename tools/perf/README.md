# Tenpack client perf harness

This directory contains the automation runner for the dev-only `tenpack_perf_harness` client mod in `mods-src/tenpack-perf-harness/`.

Typical intended run:

```bash
./tools/perf/run-client-profile.py \
  --instance tenpack-perf \
  --scenario cloud_pan \
  --warmup 30 \
  --duration 120 \
  --world Test3
```

For uncapped client FPS/frame-time work on the GPU-capable xenv/Xephyr setup,
use the server-backed path and disable VSync in the copied disposable options:

```bash
./tools/perf/run-client-profile.py \
  --run-id gpu-server-nospark-vsync-off \
  --disable-vsync \
  --max-fps 170 \
  --warmup 10 \
  --duration 30 \
  --no-spark \
  --world '' \
  --server yeyito.dev:25566 \
  --offline '' \
  --keep-xenv
```

The runner:

1. Builds the harness mod.
2. Syncs a disposable Prism instance from the local `public/client-manifest.json`.
3. Copies local runtime-only config/options/world data needed for a realistic launch.
4. Injects the harness jar after mod mirroring.
5. Launches Prism/Minecraft in an `xenv` nested X11 environment.
6. Waits for `done.json` from the harness.
7. Collects logs, Spark profiles, Minecraft profiler zips, frame CSVs, and writes `report.md`.

Run outputs go under `perf-runs/<run-id>/` and are gitignored.

## Profiling notes

`xenv`/Xephyr now exposes the real AMD GPU through DRI3/render-node acceleration. Representative Tenpack client profiling should verify the log contains an AMD `OpenGL Renderer`, not `llvmpipe`.

Spark profiles are useful for call-tree attribution, but the Spark sampler itself can add visible frame-time overhead in uncapped runs. Prefer no-Spark runs for before/after FPS comparisons, then use shorter Spark runs only to identify likely bottleneck paths.

Do not treat llvmpipe runs as representative for frame-time optimization.

The runner can temporarily patch copied client settings for experiments without touching the base Prism instance:

- `--disable-vsync` and `--max-fps N` patch `options.txt` for uncapped FPS profiling.
- `--simpleclouds-preset medium|low|ultra_low` applies Simple Clouds' built-in client presets to the copied `simpleclouds-client.toml` for A/B testing.

## Compare two completed runs

```bash
./tools/perf/compare-runs.py perf-runs/baseline perf-runs/candidate
```
