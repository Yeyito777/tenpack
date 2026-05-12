#!/usr/bin/env python3
"""Run an automated Tenpack client performance profile.

This prepares a disposable Prism instance, injects the dev-only
Tenpack Perf Harness mod, launches Minecraft in an xenv, waits for the
harness to complete, and collects frame/Spark/log artifacts into perf-runs/.
"""

from __future__ import annotations

import argparse
import configparser
import csv
import datetime as dt
import json
import os
import shutil
import subprocess
import sys
import time
from pathlib import Path
from urllib.request import pathname2url

REPO = Path(__file__).resolve().parents[2]
PRISM_ROOT = Path("/home/yeyito/Workspace/active-development/prism-launcher")
PRISM = PRISM_ROOT / "prism"
PRISM_DATA = PRISM_ROOT / "prism-data"
INSTANCES = PRISM_DATA / "instances"
BASE_INSTANCE = INSTANCES / "tenpack"
DEFAULT_INSTANCE = "tenpack-perf"
JAVA_HOME = PRISM_DATA / "java" / "java-runtime-delta"
HARNESS_SRC = REPO / "mods-src" / "tenpack-perf-harness"
HARNESS_JAR = HARNESS_SRC / "build" / "libs" / "tenpack_perf_harness-0.1.0.jar"
PUBLIC_MANIFEST = REPO / "public" / "client-manifest.json"
PERF_RUNS = REPO / "perf-runs"
HOST_DISPLAY = os.environ.get("TENPACK_XENV_HOST_DISPLAY", ":1")


def run(cmd: list[str], *, cwd: Path | None = None, env: dict[str, str] | None = None, check: bool = True, capture: bool = False) -> subprocess.CompletedProcess[str]:
    print("$", " ".join(map(str, cmd)))
    return subprocess.run(
        [str(c) for c in cmd],
        cwd=str(cwd) if cwd else None,
        env=env,
        check=check,
        text=True,
        stdout=subprocess.PIPE if capture else None,
        stderr=subprocess.STDOUT if capture else None,
    )


def build_harness() -> None:
    env = os.environ.copy()
    env["JAVA_HOME"] = str(JAVA_HOME)
    env["PATH"] = f"{JAVA_HOME / 'bin'}:{env.get('PATH', '')}"
    run(["./gradlew", "--no-daemon", "build"], cwd=HARNESS_SRC, env=env)
    if not HARNESS_JAR.exists():
        raise SystemExit(f"Harness jar was not built: {HARNESS_JAR}")


def copy_file_if_exists(src: Path, dst: Path) -> None:
    if src.exists():
        dst.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(src, dst)


def copy_file_if_missing(src: Path, dst: Path) -> None:
    if src.exists() and not dst.exists():
        dst.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(src, dst)


def copy_tree_if_missing_or_refresh(src: Path, dst: Path, refresh: bool) -> None:
    if refresh and dst.exists():
        shutil.rmtree(dst)
    if not dst.exists():
        if not src.exists():
            raise SystemExit(f"Required source path does not exist: {src}")
        print(f"copy {src} -> {dst}")
        shutil.copytree(src, dst, symlinks=True)


def read_instance_cfg(path: Path) -> configparser.ConfigParser:
    cfg = configparser.ConfigParser(interpolation=None)
    cfg.optionxform = str
    cfg.read(path)
    return cfg


def write_instance_cfg(cfg: configparser.ConfigParser, path: Path) -> None:
    with path.open("w", encoding="utf-8") as f:
        cfg.write(f, space_around_delimiters=False)


def prepare_instance(args: argparse.Namespace, run_dir: Path) -> Path:
    instance_dir = INSTANCES / args.instance
    mc_dir = instance_dir / "minecraft"
    instance_dir.mkdir(parents=True, exist_ok=True)
    mc_dir.mkdir(parents=True, exist_ok=True)

    copy_file_if_exists(BASE_INSTANCE / "mmc-pack.json", instance_dir / "mmc-pack.json")
    if not (instance_dir / "instance.cfg").exists():
        copy_file_if_exists(BASE_INSTANCE / "instance.cfg", instance_dir / "instance.cfg")

    cfg_path = instance_dir / "instance.cfg"
    cfg = read_instance_cfg(cfg_path)
    if not cfg.has_section("General"):
        cfg.add_section("General")
    g = cfg["General"]
    g["name"] = args.instance
    g["ExportName"] = args.instance
    g["PreLaunchCommand"] = ""
    g["PostExitCommand"] = ""
    g["OverrideCommands"] = "true"
    g["OverrideJavaArgs"] = "true"
    g["OverrideJavaLocation"] = "true"
    g["JavaPath"] = str(JAVA_HOME / "bin" / "java")
    g["OverrideMemory"] = "true"
    g["MaxMemAlloc"] = str(args.max_mem)
    g["MinMemAlloc"] = "512"
    g["OverrideWindow"] = "true"
    g["MinecraftWinWidth"] = str(args.width)
    g["MinecraftWinHeight"] = str(args.height)
    g["LaunchMaximized"] = "false"
    g["CloseAfterLaunch"] = "false"
    g["QuitAfterGameStop"] = "true"
    g["JoinServerOnLaunch"] = "false"
    g["JoinServerOnLaunchAddress"] = ""
    g["JoinWorldOnLaunch"] = ""

    jvm_args = [
        "-Dtenpack.perfharness=true",
        f"-Dtenpack.perfharness.runId={args.run_id}",
        f"-Dtenpack.perfharness.outputDir={run_dir}",
        f"-Dtenpack.perfharness.scenario={args.scenario}",
        f"-Dtenpack.perfharness.warmupSeconds={args.warmup}",
        f"-Dtenpack.perfharness.durationSeconds={args.duration}",
        f"-Dtenpack.perfharness.maxStartupSeconds={args.max_startup}",
        f"-Dtenpack.perfharness.shutdownDelaySeconds={args.shutdown_delay}",
        f"-Dtenpack.perfharness.spark={'true' if args.spark else 'false'}",
        f"-Dtenpack.perfharness.quit={'true' if args.quit else 'false'}",
    ]
    if args.pitch is not None:
        jvm_args.append(f"-Dtenpack.perfharness.pitch={args.pitch}")
    if args.yaw_degrees_per_second is not None:
        jvm_args.append(f"-Dtenpack.perfharness.yawDegreesPerSecond={args.yaw_degrees_per_second}")
    if args.command_file:
        jvm_args.append(f"-Dtenpack.perfharness.commandFile={Path(args.command_file).resolve()}")
    if args.simpleclouds_stress_clouds:
        jvm_args.append("-Dsimpleclouds.stressClouds=true")
    if args.simpleclouds_stress_cloud_type:
        jvm_args.append(f"-Dsimpleclouds.stressCloudType={args.simpleclouds_stress_cloud_type}")
    g["JvmArgs"] = " ".join(jvm_args)
    write_instance_cfg(cfg, cfg_path)

    # Sync local, freshly-built public pack files into the perf instance.
    manifest_url = "file://" + pathname2url(str(PUBLIC_MANIFEST.resolve()))
    run([
        sys.executable,
        str(REPO / "tools" / "tenpack-sync.py"),
        manifest_url,
        str(mc_dir),
        "--mirror-dir", "mods",
        "--mirror-dir", "resourcepacks",
        "--mirror-dir", "shaderpacks",
    ])

    # Copy a small local world for deterministic --world launches.
    if args.world:
        copy_tree_if_missing_or_refresh(
            BASE_INSTANCE / "minecraft" / "saves" / args.world,
            mc_dir / "saves" / args.world,
            args.refresh_world,
        )

    for rel in ["options.txt", "servers.dat"]:
        copy_file_if_exists(BASE_INSTANCE / "minecraft" / rel, mc_dir / rel)
    for rel in ["config/simpleclouds-client.toml", "config/simpleclouds-server.toml"]:
        copy_file_if_missing(BASE_INSTANCE / "minecraft" / rel, mc_dir / rel)

    if args.disable_vsync or args.max_fps:
        patch_options(mc_dir / "options.txt", args)
    patch_simpleclouds_config(mc_dir / "config" / "simpleclouds-client.toml", args)

    # Inject dev-only harness after mirroring, otherwise the sync would delete it.
    mods_dir = mc_dir / "mods"
    mods_dir.mkdir(parents=True, exist_ok=True)
    shutil.copy2(HARNESS_JAR, mods_dir / HARNESS_JAR.name)
    return mc_dir


def patch_options(options: Path, args: argparse.Namespace) -> None:
    if not options.exists():
        return
    text = options.read_text(encoding="utf-8")
    if args.disable_vsync:
        text = replace_options_value(text, "enableVsync", "false")
    if args.max_fps:
        text = replace_options_value(text, "maxFps", str(args.max_fps))
    options.write_text(text, encoding="utf-8")
    print("patched options.txt: " + ", ".join(
        part for part in [
            "enableVsync=false" if args.disable_vsync else "",
            f"maxFps={args.max_fps}" if args.max_fps else "",
        ] if part
    ))


def replace_options_value(text: str, key: str, value: str) -> str:
    lines = []
    changed = False
    for line in text.splitlines(keepends=True):
        if line.startswith(f"{key}:"):
            newline = "\n" if line.endswith("\n") else ""
            lines.append(f"{key}:{value}{newline}")
            changed = True
        else:
            lines.append(line)
    if not changed:
        suffix = "\n" if text and not text.endswith("\n") else ""
        lines.append(f"{suffix}{key}:{value}\n")
    return "".join(lines)


def toml_replace_value(text: str, key: str, value: str) -> str:
    lines = []
    changed = False
    for line in text.splitlines(keepends=True):
        stripped = line.lstrip("\t ")
        if stripped.startswith(f"{key} = "):
            indent = line[: len(line) - len(stripped)]
            newline = "\n" if line.endswith("\n") else ""
            lines.append(f"{indent}{key} = {value}{newline}")
            changed = True
        else:
            lines.append(line)
    if not changed:
        print(f"warning: simpleclouds-client.toml did not contain {key}")
    return "".join(lines)


def patch_simpleclouds_config(cfg: Path, args: argparse.Namespace) -> None:
    if not cfg.exists():
        return
    text = cfg.read_text(encoding="utf-8")
    changes: dict[str, str] = {}

    # These presets mirror Simple Clouds' own client config presets. They are a
    # convenient way to benchmark quality/performance tradeoffs reproducibly.
    if args.simpleclouds_preset == "medium":
        changes.update({
            "generationInterval": '"STATIC"',
            "framesToGenerateMesh": "10",
            "levelOfDetail": '"MEDIUM"',
            "shadowDistance": "2500",
        })
    elif args.simpleclouds_preset == "low":
        changes.update({
            "generationInterval": '"DYNAMIC"',
            "framesToGenerateMesh": "20",
            "levelOfDetail": '"LOW"',
            "renderLodClouds": "false",
            "transparency": "false",
            "atmosphericClouds": "false",
            "shadowDistance": "2500",
            "distantShadows": "false",
        })
    elif args.simpleclouds_preset == "ultra_low":
        changes.update({
            "generationInterval": '"DYNAMIC"',
            "framesToGenerateMesh": "20",
            "levelOfDetail": '"LOW"',
            "renderLodClouds": "false",
            "transparency": "false",
            "renderStormFog": "false",
            "atmosphericClouds": "false",
            "shadowDistance": "1000",
            "distantShadows": "false",
        })

    if args.disable_simpleclouds_mesh:
        changes["generateMesh"] = "false"
    if args.simpleclouds_generation_interval:
        changes["generationInterval"] = f'"{args.simpleclouds_generation_interval}"'
    if args.simpleclouds_frames_to_generate_mesh:
        changes["framesToGenerateMesh"] = str(args.simpleclouds_frames_to_generate_mesh)
    if args.simpleclouds_level_of_detail:
        changes["levelOfDetail"] = f'"{args.simpleclouds_level_of_detail}"'
    if args.simpleclouds_shadow_distance:
        changes["shadowDistance"] = str(args.simpleclouds_shadow_distance)

    for key, value in changes.items():
        text = toml_replace_value(text, key, value)
    if changes:
        cfg.write_text(text, encoding="utf-8")
        print("patched simpleclouds-client.toml: " + ", ".join(f"{k}={v}" for k, v in changes.items()))


def ensure_xenv(name: str) -> None:
    env = os.environ.copy()
    env["DISPLAY"] = HOST_DISPLAY
    status = run(["xenv", "status", name], env=env, check=False, capture=True)
    if status.returncode == 0 and "running" in (status.stdout or ""):
        return
    run(["xenv", "start", name], env=env)


def launch_prism(args: argparse.Namespace) -> None:
    env = os.environ.copy()
    env["DISPLAY"] = HOST_DISPLAY
    # Xephyr currently exposes llvmpipe/OpenGL 4.5. Simple Clouds shaders ask
    # for GLSL 4.60; Mesa's software renderer can often run them when the GL
    # version is explicitly overridden. This keeps xenv runs functional, though
    # reports still record the renderer so we know when results are llvmpipe.
    if args.zink:
        env["MESA_LOADER_DRIVER_OVERRIDE"] = "zink"
        env["GALLIUM_DRIVER"] = "zink"
    if args.mesa_gl_override:
        env["MESA_GL_VERSION_OVERRIDE"] = "4.6"
        env["MESA_GLSL_VERSION_OVERRIDE"] = "460"
    cmd = ["xenv", "run", "-e", args.xenv, str(PRISM), "--launch", args.instance]
    if args.offline:
        cmd += ["--offline", args.offline]
    if args.server:
        cmd += ["--server", args.server]
    if args.world:
        cmd += ["--world", args.world]
    run(cmd, env=env)


def wait_for_done(run_dir: Path, mc_dir: Path, timeout: int) -> dict:
    done = run_dir / "done.json"
    latest_log = mc_dir / "logs" / "latest.log"
    deadline = time.time() + timeout
    last_size = -1
    while time.time() < deadline:
        if done.exists():
            data = json.loads(done.read_text(encoding="utf-8"))
            print(f"Harness done: failed={data.get('failed')} reason={data.get('reason')}")
            return data
        if latest_log.exists():
            size = latest_log.stat().st_size
            if size != last_size:
                last_size = size
                # show a little liveness without flooding
                print(f"waiting... latest.log={size} bytes")
        time.sleep(2)
    raise TimeoutError(f"Timed out waiting for {done}")


def copy_new_files(src_dir: Path, dst_dir: Path, since: float, patterns: tuple[str, ...]) -> list[Path]:
    copied: list[Path] = []
    if not src_dir.exists():
        return copied
    dst_dir.mkdir(parents=True, exist_ok=True)
    for pattern in patterns:
        for src in src_dir.rglob(pattern):
            if src.is_file() and src.stat().st_mtime >= since - 5:
                dst = dst_dir / src.name
                shutil.copy2(src, dst)
                copied.append(dst)
    return copied


def collect_artifacts(run_dir: Path, mc_dir: Path, started_at: float) -> dict:
    artifacts: dict[str, list[str] | str | None] = {}
    logs_dir = run_dir / "logs"
    logs_dir.mkdir(exist_ok=True)
    for name in ["latest.log", "debug.log"]:
        src = mc_dir / "logs" / name
        if src.exists():
            shutil.copy2(src, logs_dir / name)
    spark = copy_new_files(mc_dir / "config" / "spark", run_dir / "spark", started_at, ("*.sparkprofile",))
    profiler = copy_new_files(mc_dir / "debug" / "profiling", run_dir / "minecraft-profiler", started_at, ("*.zip",))
    artifacts["sparkProfiles"] = [str(p) for p in spark]
    artifacts["minecraftProfilerZips"] = [str(p) for p in profiler]
    artifacts["latestLog"] = str(logs_dir / "latest.log") if (logs_dir / "latest.log").exists() else None
    artifacts["debugLog"] = str(logs_dir / "debug.log") if (logs_dir / "debug.log").exists() else None
    (run_dir / "artifacts.json").write_text(json.dumps(artifacts, indent=2) + "\n", encoding="utf-8")
    return artifacts


def percentile(values: list[float], pct: float) -> float:
    if not values:
        return 0.0
    values = sorted(values)
    k = (len(values) - 1) * (pct / 100.0)
    lo = int(k)
    hi = min(lo + 1, len(values) - 1)
    frac = k - lo
    return values[lo] * (1.0 - frac) + values[hi] * frac


def summarize_frames(run_dir: Path) -> dict:
    csv_path = run_dir / "frametimes.csv"
    values: list[float] = []
    if csv_path.exists():
        with csv_path.open(newline="", encoding="utf-8") as f:
            for row in csv.DictReader(f):
                try:
                    values.append(float(row["dt_ms"]))
                except (KeyError, ValueError):
                    pass
    if not values:
        return {"frames": 0}
    summary = {
        "frames": len(values),
        "meanMs": sum(values) / len(values),
        "minMs": min(values),
        "p50Ms": percentile(values, 50),
        "p90Ms": percentile(values, 90),
        "p95Ms": percentile(values, 95),
        "p99Ms": percentile(values, 99),
        "maxMs": max(values),
        "over16_7ms": sum(v > 16.6667 for v in values),
        "over25ms": sum(v > 25.0 for v in values),
        "over33_3ms": sum(v > 33.3333 for v in values),
        "over50ms": sum(v > 50.0 for v in values),
    }
    (run_dir / "frame-summary.json").write_text(json.dumps(summary, indent=2) + "\n", encoding="utf-8")
    return summary


def write_report(run_dir: Path, args: argparse.Namespace, done: dict, artifacts: dict, frame_summary: dict) -> None:
    def f(key: str) -> str:
        value = frame_summary.get(key)
        return "n/a" if value is None else f"{value:.3f}"

    report = f"""# Tenpack client performance run `{args.run_id}`

- Scenario: `{args.scenario}`
- Instance: `{args.instance}`
- World: `{args.world or ''}`
- Server: `{args.server or ''}`
- Warmup: {args.warmup}s
- Duration: {args.duration}s
- Spark enabled: {args.spark}
- Failed: {done.get('failed')}
- Reason: {done.get('reason')}

## Frame summary

| metric | value |
|---|---:|
| frames | {frame_summary.get('frames', 0)} |
| mean | {f('meanMs')} ms |
| p50 | {f('p50Ms')} ms |
| p90 | {f('p90Ms')} ms |
| p95 | {f('p95Ms')} ms |
| p99 | {f('p99Ms')} ms |
| max | {f('maxMs')} ms |
| >16.7ms | {frame_summary.get('over16_7ms', 'n/a')} |
| >25ms | {frame_summary.get('over25ms', 'n/a')} |
| >33.3ms | {frame_summary.get('over33_3ms', 'n/a')} |
| >50ms | {frame_summary.get('over50ms', 'n/a')} |

## Artifacts

- Frame CSV: `frametimes.csv`
- Harness events: `events.log`
- Summary JSON: `summary.json`
- Spark profiles: {', '.join('`' + Path(p).name + '`' for p in artifacts.get('sparkProfiles', [])) or 'none found'}
- Minecraft profiler zips: {', '.join('`' + Path(p).name + '`' for p in artifacts.get('minecraftProfilerZips', [])) or 'none found'}
- Latest log: `{artifacts.get('latestLog')}`
- Debug log: `{artifacts.get('debugLog')}`
"""
    (run_dir / "report.md").write_text(report, encoding="utf-8")


def kill_perf_processes(instance: str) -> None:
    # Best-effort cleanup, intentionally scoped to the perf instance name.
    subprocess.run(["pkill", "-f", f"instances/{instance}"], check=False)
    # Avoid a pattern beginning with '-' because procps pkill treats the pattern
    # as another option unless it is carefully separated. This regex matches the
    # normal Prism command line ("--launch tenpack-perf") without relying on a
    # leading-dash pattern.
    subprocess.run(["pkill", "-f", f"[-]-launch {instance}"], check=False)


def parse_args() -> argparse.Namespace:
    now = dt.datetime.now(dt.UTC).strftime("%Y%m%d-%H%M%S")
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--instance", default=DEFAULT_INSTANCE)
    p.add_argument("--xenv", default="tenpack-perf")
    p.add_argument("--run-id", default=now)
    p.add_argument("--scenario", default="cloud_pan", choices=["cloud_pan", "static_pan", "fixed", "walk_pan"])
    p.add_argument("--warmup", type=int, default=30)
    p.add_argument("--duration", type=int, default=120)
    p.add_argument("--shutdown-delay", type=int, default=6)
    p.add_argument("--max-startup", type=int, default=300)
    p.add_argument("--timeout", type=int, default=600)
    p.add_argument("--world", default="Test3", help="Local world to copy from base instance and launch; empty to skip --world")
    p.add_argument("--server", default="", help="Server address for Prism --server; mutually exclusive with --world in practice")
    p.add_argument("--offline", default="ExoPerf")
    p.add_argument("--max-mem", type=int, default=12000)
    p.add_argument("--width", type=int, default=1280)
    p.add_argument("--height", type=int, default=720)
    p.add_argument("--disable-vsync", action="store_true", help="Patch copied options.txt with enableVsync:false for uncapped FPS profiling")
    p.add_argument("--max-fps", type=int, default=0, help="Patch copied options.txt maxFps for this run (0 leaves the copied value unchanged)")
    p.add_argument("--pitch", type=float)
    p.add_argument("--yaw-degrees-per-second", type=float)
    p.add_argument("--command-file", default="", help="Optional newline-delimited server commands for the harness to issue after joining")
    p.add_argument("--refresh-world", action="store_true")
    p.add_argument("--mesa-gl-override", action=argparse.BooleanOptionalAction, default=True)
    p.add_argument("--zink", action=argparse.BooleanOptionalAction, default=False)
    p.add_argument("--simpleclouds-preset", default="current", choices=["current", "medium", "low", "ultra_low"],
                   help="Patch the copied simpleclouds-client.toml with one of Simple Clouds' built-in client presets for this run")
    p.add_argument("--simpleclouds-generation-interval", choices=["STATIC", "DYNAMIC", "TARGET_FPS"], help="Override Simple Clouds mesh generationInterval in the copied config")
    p.add_argument("--simpleclouds-frames-to-generate-mesh", type=int, default=0, help="Override Simple Clouds framesToGenerateMesh in the copied config")
    p.add_argument("--simpleclouds-level-of-detail", choices=["LOW", "MEDIUM", "HIGH"], help="Override Simple Clouds levelOfDetail in the copied config")
    p.add_argument("--simpleclouds-shadow-distance", type=int, default=0, help="Override Simple Clouds shadowDistance in the copied config")
    p.add_argument("--simpleclouds-stress-clouds", action="store_true", help="Enable Simple Clouds' dev-only stress CloudGetter for cloud-heavy profiling")
    p.add_argument("--simpleclouds-stress-cloud-type", default="", help="Cloud type id for --simpleclouds-stress-clouds, default simpleclouds:cumulonimbus")
    p.add_argument("--disable-simpleclouds-mesh", action="store_true", help="xenv/llvmpipe smoke-test escape hatch; not representative for Simple Clouds profiling")
    p.add_argument("--no-build", action="store_true")
    p.add_argument("--no-spark", dest="spark", action="store_false")
    p.add_argument("--no-quit", dest="quit", action="store_false")
    p.add_argument("--keep-xenv", action="store_true")
    p.set_defaults(spark=True, quit=True)
    return p.parse_args()


def main() -> int:
    args = parse_args()
    run_dir = (PERF_RUNS / args.run_id).resolve()
    run_dir.mkdir(parents=True, exist_ok=True)
    (run_dir / "run-args.json").write_text(json.dumps(vars(args), indent=2) + "\n", encoding="utf-8")

    if args.world == "":
        args.world = None
    if args.server == "":
        args.server = None
    if args.world and args.server:
        raise SystemExit("Use either --world or --server, not both")

    started_at = time.time()
    try:
        kill_perf_processes(args.instance)
        if not args.no_build:
            build_harness()
        mc_dir = prepare_instance(args, run_dir)
        ensure_xenv(args.xenv)
        launch_prism(args)
        done = wait_for_done(run_dir, mc_dir, args.timeout)
        artifacts = collect_artifacts(run_dir, mc_dir, started_at)
        frame_summary = summarize_frames(run_dir)
        write_report(run_dir, args, done, artifacts, frame_summary)
        print(f"Run directory: {run_dir}")
        print((run_dir / "report.md").read_text(encoding="utf-8"))
        return 1 if done.get("failed") else 0
    except Exception as exc:  # noqa: BLE001
        print(f"ERROR: {exc}", file=sys.stderr)
        kill_perf_processes(args.instance)
        return 2
    finally:
        if not args.keep_xenv:
            env = os.environ.copy()
            env["DISPLAY"] = HOST_DISPLAY
            subprocess.run(["xenv", "stop", args.xenv], env=env, check=False)


if __name__ == "__main__":
    raise SystemExit(main())
