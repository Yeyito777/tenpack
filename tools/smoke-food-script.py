#!/usr/bin/env python3
"""Minimal runtime smoke test for Tenpack's CraftTweaker food script.

The full pack smoke is still valuable, but it can be blocked by unrelated Create
or addon startup problems before CraftTweaker reaches the script loader.  This
focused smoke installs a disposable NeoForge server with only the mods needed to
compile and execute `server/scripts/tenpack_food_pressure.zs`:

- CraftTweaker
- Farmer's Delight
- Supplementaries + Moonlight, because the food pass documents and guards the
  lunch-basket layer alongside the script

It then boots until `Done (...)`, inspects `logs/crafttweaker.log`, and fails if
the script is missing or CraftTweaker reports errors/exceptions.
"""

from __future__ import annotations

import argparse
import hashlib
import re
import shutil
import subprocess
import sys
import time
import zipfile
from pathlib import Path
from urllib.request import urlretrieve


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_NEOFORGE = "21.1.228"
DEFAULT_PORT = 25570
DONE_PATTERN = re.compile(r"\[Server thread/INFO\].*\bDone \([0-9.]+s\)!")
CRAFTTWEAKER_ERROR_PATTERN = re.compile(r"(\[(ERROR|FATAL)\]|/(ERROR|FATAL)\b|\b(ERROR|FATAL)\b|\bException\b)")
SCRIPT_NAME = "tenpack_food_pressure.zs"

MINIMAL_MODS = [
    "CraftTweaker-neoforge-1.21.1-21.0.38.jar",
    "FarmersDelight-1.21.1-1.3.1.jar",
    "moonlight-neoforge-1.21.1-3.0.7.jar",
    "supplementaries-neoforge-1.21.1-3.6.4.jar",
]

CONFIGS_TO_COPY = [
    "farmersdelight-common.toml",
    "moonlight-common.toml",
    "supplementaries-common.toml",
]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--neoforge", default=DEFAULT_NEOFORGE, help=f"NeoForge version to install (default: {DEFAULT_NEOFORGE})")
    parser.add_argument("--work-dir", type=Path, default=Path("/tmp/tenpack-food-script-smoke"), help="temporary minimal server directory")
    parser.add_argument("--log", type=Path, default=Path("/tmp/tenpack-food-script-smoke.log"), help="server stdout/stderr log path")
    parser.add_argument("--install-log", type=Path, default=Path("/tmp/tenpack-food-script-install.log"), help="NeoForge installer log path")
    parser.add_argument("--installer-cache", type=Path, default=Path.home() / ".cache" / "tenpack", help="directory for downloaded NeoForge installer jars")
    parser.add_argument("--timeout", type=int, default=180, help="seconds before killing the boot attempt")
    parser.add_argument("--post-done-seconds", type=int, default=2, help="seconds to keep the server alive after Done before stopping")
    parser.add_argument("--port", type=int, default=DEFAULT_PORT, help=f"temporary server/query port (default: {DEFAULT_PORT})")
    parser.add_argument("--xmx", default="3G", help="temporary max heap in user_jvm_args.txt")
    parser.add_argument("--keep-work-dir", action="store_true", help="do not delete the temporary server directory after the run")
    return parser.parse_args()


def sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def validate_work_dir(path: Path) -> Path:
    resolved = path.expanduser().resolve()
    if resolved in {Path("/"), Path.home().resolve(), ROOT.resolve()}:
        raise RuntimeError(f"refusing dangerous --work-dir: {resolved}")
    tmp_roots = [Path("/tmp").resolve(), Path("/var/tmp").resolve()]
    if not any(resolved == tmp or tmp in resolved.parents for tmp in tmp_roots):
        raise RuntimeError(f"refusing --work-dir outside /tmp or /var/tmp: {resolved}")
    if ROOT.resolve() in resolved.parents:
        raise RuntimeError(f"refusing --work-dir inside repository: {resolved}")
    return resolved


def valid_installer(path: Path) -> bool:
    if not path.exists() or path.stat().st_size < 1_000_000:
        return False
    try:
        with zipfile.ZipFile(path) as jar:
            return "META-INF/MANIFEST.MF" in jar.namelist()
    except zipfile.BadZipFile:
        return False


def download_installer(version: str, cache_dir: Path) -> Path:
    cache_dir.mkdir(parents=True, exist_ok=True)
    target = cache_dir / f"neoforge-{version}-installer.jar"
    if valid_installer(target):
        return target
    url = f"https://maven.neoforged.net/releases/net/neoforged/neoforge/{version}/neoforge-{version}-installer.jar"
    print(f"Downloading {url}")
    tmp = target.with_suffix(target.suffix + ".tmp")
    urlretrieve(url, tmp)
    if not valid_installer(tmp):
        tmp.unlink(missing_ok=True)
        raise RuntimeError(f"downloaded installer is not a valid jar: {url}")
    tmp.replace(target)
    return target


def install_server(java: str, installer: Path, work_dir: Path, install_log: Path) -> None:
    install_log.parent.mkdir(parents=True, exist_ok=True)
    with install_log.open("w", encoding="utf-8", errors="replace") as log:
        result = subprocess.run(
            [java, "-jar", str(installer), "--installServer", "--debug"],
            cwd=work_dir,
            stdout=log,
            stderr=subprocess.STDOUT,
            text=True,
        )
    if result.returncode != 0:
        tail = install_log.read_text(encoding="utf-8", errors="replace").splitlines()[-80:]
        raise RuntimeError("NeoForge install failed:\n" + "\n".join(tail))


def copy_minimal_payload(work_dir: Path, port: int, xmx: str) -> tuple[int, int]:
    mods_dir = work_dir / "mods"
    config_dir = work_dir / "config"
    scripts_dir = work_dir / "scripts"
    mods_dir.mkdir(parents=True, exist_ok=True)
    config_dir.mkdir(parents=True, exist_ok=True)
    scripts_dir.mkdir(parents=True, exist_ok=True)

    for filename in MINIMAL_MODS:
        src = ROOT / "server/mods" / filename
        if not src.exists():
            raise RuntimeError(f"missing required minimal food-smoke mod {src.relative_to(ROOT)}")
        shutil.copy2(src, mods_dir / filename)

    copied_configs = 0
    for filename in CONFIGS_TO_COPY:
        src = ROOT / "server/config" / filename
        if src.exists():
            shutil.copy2(src, config_dir / filename)
            copied_configs += 1

    script = ROOT / "server/scripts" / SCRIPT_NAME
    if not script.exists():
        raise RuntimeError(f"missing {script.relative_to(ROOT)}")
    shutil.copy2(script, scripts_dir / SCRIPT_NAME)

    server_props = ROOT / "server/server.properties"
    if server_props.exists():
        lines = server_props.read_text(encoding="utf-8").splitlines()
    else:
        lines = []
    overrides = {
        "online-mode": "false",
        "view-distance": "3",
        "simulation-distance": "3",
        "max-tick-time": "-1",
        "server-port": str(port),
        "query.port": str(port),
    }
    out: list[str] = []
    seen: set[str] = set()
    for line in lines:
        if "=" in line and not line.lstrip().startswith("#"):
            key = line.split("=", 1)[0]
            if key in overrides:
                out.append(f"{key}={overrides[key]}")
                seen.add(key)
                continue
        out.append(line)
    for key, value in overrides.items():
        if key not in seen:
            out.append(f"{key}={value}")
    (work_dir / "server.properties").write_text("\n".join(out) + "\n", encoding="utf-8")
    (work_dir / "eula.txt").write_text("eula=true\n", encoding="utf-8")
    (work_dir / "user_jvm_args.txt").write_text(
        f"-Xms1G\n-Xmx{xmx}\n-Djava.net.preferIPv4Stack=true\n-XX:+UseG1GC\n",
        encoding="utf-8",
    )
    return len(MINIMAL_MODS), copied_configs


def run_server(work_dir: Path, log_path: Path, timeout_seconds: int, post_done_seconds: int) -> tuple[bool, int | None, str | None]:
    log_path.parent.mkdir(parents=True, exist_ok=True)
    log_path.unlink(missing_ok=True)
    with log_path.open("wb") as log:
        process = subprocess.Popen(
            ["bash", "run.sh", "nogui"],
            cwd=work_dir,
            stdin=subprocess.PIPE,
            stdout=log,
            stderr=subprocess.STDOUT,
        )
        start = time.monotonic()
        cursor = 0
        done_at: float | None = None
        done_line: str | None = None
        stop_sent = False
        while True:
            time.sleep(1)
            text = log_path.read_text(encoding="utf-8", errors="replace") if log_path.exists() else ""
            new_text = text[cursor:]
            cursor = len(text)
            if done_at is None:
                for line in new_text.splitlines():
                    if DONE_PATTERN.search(line):
                        done_at = time.monotonic()
                        done_line = line
                        break
            if done_at is not None and not stop_sent and time.monotonic() - done_at >= post_done_seconds:
                if process.stdin:
                    try:
                        process.stdin.write(b"stop\n")
                        process.stdin.flush()
                    except BrokenPipeError:
                        pass
                stop_sent = True
            code = process.poll()
            if code is not None:
                return done_at is not None, code, done_line
            if time.monotonic() - start > timeout_seconds:
                process.terminate()
                try:
                    process.wait(timeout=20)
                except subprocess.TimeoutExpired:
                    process.kill()
                    process.wait(timeout=10)
                return done_at is not None, None, done_line


def inspect_crafttweaker_log(work_dir: Path) -> list[str]:
    errors: list[str] = []
    ct_log = work_dir / "logs/crafttweaker.log"
    if not ct_log.exists():
        return [f"CraftTweaker log missing: {ct_log}"]

    text = ct_log.read_text(encoding="utf-8", errors="replace")
    lines = text.splitlines()
    if f"Loading file '{SCRIPT_NAME}'" not in text:
        errors.append(f"CraftTweaker log did not mention {SCRIPT_NAME}")
    if "Execution for loader 'crafttweaker' completed successfully" not in text:
        errors.append("CraftTweaker loader did not report successful execution")
    bad_lines = [line for line in lines if CRAFTTWEAKER_ERROR_PATTERN.search(line)]
    if bad_lines:
        errors.append(f"CraftTweaker log contains {len(bad_lines)} error/fatal/exception line(s)")
        print("last_crafttweaker_error_lines:")
        for line in bad_lines[-20:]:
            print(line)
    print(f"crafttweaker_log: {ct_log}")
    print(f"crafttweaker_script_loaded: {SCRIPT_NAME in text}")
    print(f"crafttweaker_error_lines: {len(bad_lines)}")
    return errors


def main() -> int:
    args = parse_args()
    java = shutil.which("java")
    if not java:
        print("java not found on PATH", file=sys.stderr)
        return 2

    work_dir = validate_work_dir(args.work_dir)
    if work_dir.exists():
        shutil.rmtree(work_dir)
    work_dir.mkdir(parents=True)

    installer = download_installer(args.neoforge, args.installer_cache)
    print(f"installer: {installer} sha256={sha256_file(installer)}")

    try:
        install_server(java, installer, work_dir, args.install_log)
        mod_count, config_count = copy_minimal_payload(work_dir, args.port, args.xmx)
        print(f"staged_minimal_food_runtime: mods={mod_count} configs={config_count} work_dir={work_dir}")
        reached_done, code, done_line = run_server(work_dir, args.log, args.timeout, args.post_done_seconds)
        print(f"run_exit: {code if code is not None else 'timeout'}")
        print(f"done_line: {done_line or 'NONE'}")
        errors = []
        if not reached_done:
            errors.append("minimal food-script server did not reach Done")
        errors.extend(inspect_crafttweaker_log(work_dir))
        if errors:
            print("Food script smoke failed:", file=sys.stderr)
            for error in errors:
                print(f"- {error}", file=sys.stderr)
            print(f"log: {args.log}", file=sys.stderr)
            print(f"install_log: {args.install_log}", file=sys.stderr)
            return 1
        print("Food script smoke passed.")
        return 0
    finally:
        if not args.keep_work_dir:
            shutil.rmtree(work_dir, ignore_errors=True)


if __name__ == "__main__":
    raise SystemExit(main())
