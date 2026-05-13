#!/usr/bin/env python3
"""Disposable full-pack NeoForge server smoke test for Tenpack.

This intentionally does *not* use the Tenpack Travel Gradle dev run: that run only
loads the local mod source and can miss production-pack mod discovery failures.
Instead, this script installs a temporary official NeoForge server, copies the
repo's server/ payload into it, applies isolated smoke-test properties, boots
until the dedicated server reaches `Done (...)`, then stops and cleans up.
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
DEFAULT_PORT = 25569
DONE_PATTERN = re.compile(r"\[Server thread/INFO\].*\bDone \([0-9.]+s\)!")
SMOKE_PROPERTY_OVERRIDES = {
    "online-mode": "false",
    "view-distance": "4",
    "simulation-distance": "4",
    "max-tick-time": "-1",
}
CRAFTTWEAKER_LOG_REL = Path("logs/crafttweaker.log")
CRAFTTWEAKER_ERROR_PATTERN = re.compile(r"(\[(ERROR|FATAL)\]|/(ERROR|FATAL)\b|\b(ERROR|FATAL)\b|\bException\b)")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--neoforge", default=DEFAULT_NEOFORGE, help=f"NeoForge version to install (default: {DEFAULT_NEOFORGE})")
    parser.add_argument("--work-dir", type=Path, default=Path("/tmp/tenpack-neoforge-full-smoke"), help="temporary server directory")
    parser.add_argument("--log", type=Path, default=Path("/tmp/tenpack-neoforge-full-smoke.log"), help="server stdout/stderr log path")
    parser.add_argument("--install-log", type=Path, default=Path("/tmp/tenpack-neoforge-install.log"), help="NeoForge installer log path")
    parser.add_argument("--installer-cache", type=Path, default=Path.home() / ".cache" / "tenpack", help="directory for downloaded NeoForge installer jars")
    parser.add_argument("--timeout", type=int, default=240, help="seconds before killing the boot attempt")
    parser.add_argument("--post-done-seconds", type=int, default=20, help="seconds to keep the server alive after Done before sending stop")
    parser.add_argument("--port", type=int, default=DEFAULT_PORT, help=f"temporary server/query port (default: {DEFAULT_PORT})")
    parser.add_argument("--xmx", default="6G", help="temporary max heap in user_jvm_args.txt")
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


def copy_pack_payload(root: Path, work_dir: Path, port: int, xmx: str) -> tuple[int, int, int]:
    source = root / "server"
    if not source.exists():
        raise RuntimeError(f"missing {source}")
    if not (source / "server.properties").exists():
        raise RuntimeError(f"missing {source / 'server.properties'}")

    for name in ("mods", "config", "defaultconfigs", "scripts", "world"):
        src = source / name
        if src.exists():
            dst = work_dir / name
            if dst.exists():
                shutil.rmtree(dst)
            shutil.copytree(src, dst)

    props = (source / "server.properties").read_text(encoding="utf-8").splitlines()
    overrides = {
        **SMOKE_PROPERTY_OVERRIDES,
        "server-port": str(port),
        "query.port": str(port),
    }
    out: list[str] = []
    seen: set[str] = set()
    for line in props:
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

    mods = len(list((work_dir / "mods").glob("*.jar"))) if (work_dir / "mods").exists() else 0
    configs = len([p for p in (work_dir / "config").rglob("*") if p.is_file()]) if (work_dir / "config").exists() else 0
    datapacks = len(list((work_dir / "world" / "datapacks").glob("*"))) if (work_dir / "world" / "datapacks").exists() else 0
    return mods, configs, datapacks


def install_server(java: str, installer: Path, work_dir: Path, install_log: Path) -> None:
    install_log.parent.mkdir(parents=True, exist_ok=True)
    with install_log.open("w", encoding="utf-8", errors="replace") as log:
        result = subprocess.run([java, "-jar", str(installer), "--installServer", "--debug"], cwd=work_dir, stdout=log, stderr=subprocess.STDOUT, text=True)
    if result.returncode != 0:
        tail = install_log.read_text(encoding="utf-8", errors="replace").splitlines()[-80:]
        raise RuntimeError("NeoForge install failed:\n" + "\n".join(tail))


def run_server(work_dir: Path, log_path: Path, timeout_seconds: int, post_done_seconds: int) -> tuple[bool, int | None, str | None]:
    log_path.parent.mkdir(parents=True, exist_ok=True)
    if log_path.exists():
        log_path.unlink()

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


def summarize_crafttweaker_log(work_dir: Path) -> list[str]:
    errors: list[str] = []
    ct_log = work_dir / CRAFTTWEAKER_LOG_REL
    scripts_dir = work_dir / "scripts"
    scripts = sorted(path.name for path in scripts_dir.glob("*.zs")) if scripts_dir.exists() else []

    if not scripts:
        print("crafttweaker_scripts: 0")
        return errors
    if not ct_log.exists():
        errors.append(f"CraftTweaker log missing: {ct_log}")
        print(f"crafttweaker_log: MISSING expected={ct_log}")
        print(f"crafttweaker_scripts: {len(scripts)}")
        return errors

    text = ct_log.read_text(encoding="utf-8", errors="replace")
    lines = text.splitlines()
    bad_lines = [line for line in lines if CRAFTTWEAKER_ERROR_PATTERN.search(line)]
    missing_scripts = [name for name in scripts if name not in text]
    if missing_scripts:
        errors.append("CraftTweaker log did not mention script(s): " + ", ".join(missing_scripts))
    if bad_lines:
        errors.append(f"CraftTweaker log contains {len(bad_lines)} error/fatal/exception line(s)")

    print(f"crafttweaker_log: {ct_log}")
    print(f"crafttweaker_scripts: {len(scripts)}")
    print(f"crafttweaker_missing_scripts: {len(missing_scripts)}")
    print(f"crafttweaker_error_lines: {len(bad_lines)}")
    if bad_lines:
        print("last_crafttweaker_error_lines:")
        for line in bad_lines[-20:]:
            print(line)
    return errors


def summarize_log(log_path: Path, done_line: str | None, work_dir: Path) -> list[str]:
    text = log_path.read_text(encoding="utf-8", errors="replace") if log_path.exists() else ""
    lines = text.splitlines()
    railways_loot_errors = sum("LootDataType" in line and "railways:blocks/track_" in line for line in lines)
    config_corrections = sum("Configuration file" in line and "is not correct. Correcting" in line for line in lines)
    tenpack_mixin_failures = [
        line for line in lines
        if "tenpack_travel.mixins.json" in line and ("Mixin apply" in line or "InvalidInjection" in line or "failed" in line.lower())
    ]
    error_lines = [line for line in lines if "/ERROR" in line or "[ERROR]" in line]
    print(f"done_line: {done_line or 'NONE'}")
    print(f"railways_optional_track_loot_errors: {railways_loot_errors}")
    print(f"config_corrections: {config_corrections}")
    print(f"tenpack_mixin_failures: {len(tenpack_mixin_failures)}")
    print(f"error_lines_total: {len(error_lines)}")
    if tenpack_mixin_failures:
        print("tenpack_mixin_failure_lines:")
        for line in tenpack_mixin_failures[-20:]:
            print(line)
    if error_lines:
        print("last_error_lines:")
        for line in error_lines[-20:]:
            print(line)
    print(f"log: {log_path}")
    errors = summarize_crafttweaker_log(work_dir)
    if tenpack_mixin_failures:
        errors.append(f"Tenpack Travel mixin failure lines: {len(tenpack_mixin_failures)}")
    return errors


def main() -> int:
    args = parse_args()
    java = shutil.which("java")
    if not java:
        print("java not found in PATH", file=sys.stderr)
        return 2

    work_dir = validate_work_dir(args.work_dir)
    if work_dir.exists():
        shutil.rmtree(work_dir)
    work_dir.mkdir(parents=True)

    try:
        installer = download_installer(args.neoforge, args.installer_cache)
        print(f"installer: {installer} sha256={sha256_file(installer)}")
        try:
            install_server(java, installer, work_dir, args.install_log)
        except RuntimeError as first_error:
            # The official installer occasionally fails during its postprocessors
            # after a transient/partial server-library download. Retry once from
            # a clean disposable server dir so smoke results are about Tenpack's
            # pack boot, not a flaky intermediate installer state.
            print(f"install attempt 1 failed; retrying once: {first_error}", file=sys.stderr)
            shutil.rmtree(work_dir)
            work_dir.mkdir(parents=True)
            install_server(java, installer, work_dir, args.install_log)
        mods, configs, datapacks = copy_pack_payload(ROOT, work_dir, args.port, args.xmx)
        print(f"staged: mods={mods} configs={configs} datapacks={datapacks} work_dir={work_dir}")
        done, code, done_line = run_server(work_dir, args.log, args.timeout, args.post_done_seconds)
        print(f"run_exit: {code if code is not None else 'timeout'}")
        smoke_errors = summarize_log(args.log, done_line, work_dir)
        if smoke_errors:
            print("Smoke post-check failed:", file=sys.stderr)
            for error in smoke_errors:
                print(f"- {error}", file=sys.stderr)
        if done and code == 0 and not smoke_errors:
            return 0
        if done and code != 0:
            print("server reached Done but did not stop cleanly", file=sys.stderr)
        return code if isinstance(code, int) and code != 0 else 1
    finally:
        if not args.keep_work_dir and work_dir.exists():
            shutil.rmtree(work_dir)


if __name__ == "__main__":
    raise SystemExit(main())
