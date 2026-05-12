#!/usr/bin/env python3
"""Compare two Tenpack perf harness run directories."""

from __future__ import annotations

import argparse
import csv
import json
from pathlib import Path


def percentile(values: list[float], pct: float) -> float:
    if not values:
        return 0.0
    values = sorted(values)
    k = (len(values) - 1) * (pct / 100.0)
    lo = int(k)
    hi = min(lo + 1, len(values) - 1)
    frac = k - lo
    return values[lo] * (1.0 - frac) + values[hi] * frac


def load_run(path: Path) -> dict:
    frames: list[float] = []
    csv_path = path / "frametimes.csv"
    if csv_path.exists():
        with csv_path.open(newline="", encoding="utf-8") as f:
            for row in csv.DictReader(f):
                try:
                    frames.append(float(row["dt_ms"]))
                except (KeyError, ValueError):
                    pass
    summary = {}
    for candidate in [path / "frame-summary.json", path / "summary.json", path / "done.json"]:
        if candidate.exists():
            try:
                summary.update(json.loads(candidate.read_text(encoding="utf-8")))
            except json.JSONDecodeError:
                pass
    if frames:
        summary.update({
            "frames": len(frames),
            "meanMs": sum(frames) / len(frames),
            "p50Ms": percentile(frames, 50),
            "p90Ms": percentile(frames, 90),
            "p95Ms": percentile(frames, 95),
            "p99Ms": percentile(frames, 99),
            "maxMs": max(frames),
            "over16_7ms": sum(v > 16.6667 for v in frames),
            "over25ms": sum(v > 25.0 for v in frames),
            "over33_3ms": sum(v > 33.3333 for v in frames),
            "over50ms": sum(v > 50.0 for v in frames),
        })
    return summary


def pct_delta(a: float, b: float) -> str:
    if a == 0:
        return "n/a"
    d = (b - a) / a * 100.0
    return f"{d:+.1f}%"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("baseline", type=Path)
    parser.add_argument("candidate", type=Path)
    args = parser.parse_args()

    base = load_run(args.baseline)
    cand = load_run(args.candidate)
    keys = [
        ("frames", "frames", False),
        ("meanMs", "mean", True),
        ("p50Ms", "p50", True),
        ("p90Ms", "p90", True),
        ("p95Ms", "p95", True),
        ("p99Ms", "p99", True),
        ("maxMs", "max", True),
        ("over16_7ms", ">16.7ms", False),
        ("over25ms", ">25ms", False),
        ("over33_3ms", ">33.3ms", False),
        ("over50ms", ">50ms", False),
    ]

    print(f"# Perf comparison\n")
    print(f"Baseline: `{args.baseline}`")
    print(f"Candidate: `{args.candidate}`\n")
    print("| metric | baseline | candidate | delta |")
    print("|---|---:|---:|---:|")
    for key, label, ms in keys:
        if key not in base or key not in cand:
            continue
        a = base[key]
        b = cand[key]
        suffix = " ms" if ms else ""
        if isinstance(a, float) or isinstance(b, float):
            av = f"{float(a):.3f}{suffix}"
            bv = f"{float(b):.3f}{suffix}"
        else:
            av = f"{a}{suffix}"
            bv = f"{b}{suffix}"
        print(f"| {label} | {av} | {bv} | {pct_delta(float(a), float(b))} |")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
