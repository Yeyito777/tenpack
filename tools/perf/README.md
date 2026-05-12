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

The runner:

1. Builds the harness mod.
2. Syncs a disposable Prism instance from the local `public/client-manifest.json`.
3. Copies local runtime-only config/options/world data needed for a realistic launch.
4. Injects the harness jar after mod mirroring.
5. Launches Prism/Minecraft in an `xenv` nested X11 environment.
6. Waits for `done.json` from the harness.
7. Collects logs, Spark profiles, Minecraft profiler zips, frame CSVs, and writes `report.md`.

Run outputs go under `perf-runs/<run-id>/` and are gitignored.

## Current environment limitation

The current `xenv`/Xephyr environment exposes Mesa `llvmpipe` instead of the real AMD GPU. Full Tenpack + Simple Clouds currently aborts inside Mesa/LLVM while compiling Simple Clouds compute shader work under llvmpipe before the world is entered. Because of that, the runner and harness can be built and launched, but full representative Simple Clouds profiling needs either:

- a GPU-capable nested display/compositor, or
- explicit permission to run the perf instance on the real host X display, which is currently disallowed by Exocortex safety policy.

Do not treat llvmpipe runs as representative for frame-time optimization.

## Compare two completed runs

```bash
./tools/perf/compare-runs.py perf-runs/baseline perf-runs/candidate
```
