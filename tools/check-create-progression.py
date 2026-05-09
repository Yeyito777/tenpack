#!/usr/bin/env python3
"""Sanity checks for Tenpack's Create progression datapack.

This is not a Minecraft datapack validator. It checks the design invariants that are
easy to accidentally break while editing JSON by hand:

- Create Encased machine variants must upgrade from the tuned base Create machine.
- diesel/oil and cannon production must stay behind precision mechanisms.
- controlled-flight recipes must stay behind precision/gyro progression.
"""

from __future__ import annotations

import json
import sys
from pathlib import Path


def collect_refs(path: Path) -> list[str]:
    obj = json.loads(path.read_text(encoding="utf-8"))
    refs: list[str] = []

    def visit(value):
        if isinstance(value, dict):
            for key, child in value.items():
                if key in {"item", "id", "tag", "items"} and isinstance(child, str):
                    refs.append(child)
                visit(child)
        elif isinstance(value, list):
            for child in value:
                visit(child)

    visit(obj)
    return refs


def require_ref(errors: list[str], root: Path, rel: str, required: str) -> None:
    path = root / rel
    if not path.exists():
        errors.append(f"missing expected file: {rel}")
        return
    if required not in collect_refs(path):
        errors.append(f"{rel} must reference {required}")


def require_any_ref(errors: list[str], root: Path, rel: str, required: set[str]) -> None:
    path = root / rel
    if not path.exists():
        errors.append(f"missing expected file: {rel}")
        return
    refs = set(collect_refs(path))
    if not refs.intersection(required):
        errors.append(f"{rel} must reference one of: {', '.join(sorted(required))}")


def main() -> int:
    repo = Path(__file__).resolve().parents[1]
    root = repo / "server/datapacks/tenpack-create-progression"
    if not root.exists():
        print(f"missing datapack: {root}", file=sys.stderr)
        return 1

    errors: list[str] = []

    # All JSON must parse.
    for path in sorted(root.rglob("*.json")):
        try:
            json.loads(path.read_text(encoding="utf-8"))
        except Exception as exc:  # noqa: BLE001 - report all parse failures
            errors.append(f"invalid JSON {path.relative_to(root)}: {exc}")

    # Create Encased variants can otherwise bypass Tenpack's tuned base recipes.
    createcasing_base_machines = {
        "press": "create:mechanical_press",
        "mixer": "create:mechanical_mixer",
        "saw": "create:mechanical_saw",
        "drill": "create:mechanical_drill",
        "encased_fan": "create:encased_fan",
        "harvester": "create:mechanical_harvester",
        "plough": "create:mechanical_plough",
        "portable_storage_interface": "create:portable_storage_interface",
        "deployer": "create:deployer",
        "roller": "create:mechanical_roller",
    }
    for category, base_machine in createcasing_base_machines.items():
        category_dir = root / f"data/createcasing/recipe/crafting/{category}"
        if not category_dir.exists():
            errors.append(f"missing Create Encased category override dir: {category_dir.relative_to(root)}")
            continue
        for path in sorted(category_dir.glob("*.json")):
            if base_machine not in collect_refs(path):
                errors.append(f"{path.relative_to(root)} must reference {base_machine}")

    precision_required = [
        "data/createdieselgenerators/recipe/crafting/pumpjack_bearing.json",
        "data/createdieselgenerators/recipe/crafting/distillation_controller.json",
        "data/createdieselgenerators/recipe/crafting/diesel_engine.json",
        "data/createbigcannons/recipe/cannon_builder.json",
        "data/createbigcannons/recipe/cannon_drill.json",
        "data/createbigcannons/recipe/cannon_mount.json",
        "data/createbigcannons/recipe/fixed_cannon_mount.json",
        "data/createbigcannons/recipe/cannon_mount_extension.json",
        "data/createbigcannons/recipe/cannon_welder.json",
        "data/minecraft/recipe/cannon_welder_mirrored.json",
    ]
    for rel in precision_required:
        require_ref(errors, root, rel, "create:precision_mechanism")

    precision_or_gyro_required = [
        "data/aeronautics/recipe/propeller_bearing.json",
        "data/aeronautics/recipe/gyroscopic_propeller_bearing.json",
        "data/aeronautics/recipe/smart_propeller.json",
        "data/aeronautics/recipe/steam_vent.json",
        "data/aeronautics/recipe/adjustable_burner.json",
        "data/simulated/recipe/sequenced_assembly/gyroscopic_mechanism.json",
    ]
    for rel in precision_or_gyro_required:
        require_any_ref(errors, root, rel, {"create:precision_mechanism", "simulated:gyroscopic_mechanism"})

    if errors:
        for error in errors:
            print(error, file=sys.stderr)
        return 1

    print("Create progression invariants passed")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
