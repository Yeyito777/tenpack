#!/usr/bin/env python3
"""Validate Tenpack's generated FTB Quests questbook.

The questbook is generated from tools/generate-ftb-questbook.py. This check keeps
client/server quest data identical, verifies it was regenerated after source
changes, and catches easy-to-miss pack issues like missing icon item models or
advancement task targets.
"""

from __future__ import annotations

import hashlib
import re
import runpy
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
QUEST_REL = Path("config/ftbquests/quests")
GENERATOR = ROOT / "tools" / "generate-ftb-questbook.py"
PROGRESSION_ADVANCEMENTS = (
    ROOT
    / "server"
    / "world"
    / "datapacks"
    / "tenpack-create-progression"
    / "data"
)

REQUIRED_MOD_JARS = {
    "ftb-library-neoforge-2101.1.31.jar",
    "ftb-teams-neoforge-2101.1.10.jar",
    "ftb-quests-neoforge-2101.1.24.jar",
}

BANNED_GENERATED_TEXT = {
    "core stocked meal": "food logistics must not imply a narrow official ration",
    "core stocked meals": "food logistics must not imply a narrow official ration",
}


def rel_files(root: Path) -> set[Path]:
    return {path.relative_to(root) for path in root.rglob("*") if path.is_file()}


def file_hash(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def expected_files(module: dict[str, Any]) -> dict[Path, str]:
    chapters = module["CHAPTERS"]
    files: dict[Path, str] = {
        Path("data.snbt"): module["data_snbt"](),
        Path("chapter_groups.snbt"): module["chapter_groups_snbt"](),
        Path("lang/en_us.snbt"): module["lang_snbt"](chapters),
    }
    for index, chapter in enumerate(chapters):
        files[Path("chapters") / f"{chapter.filename}.snbt"] = module["chapter_snbt"](chapter, index)
    return files


def advancement_path(advancement_id: str) -> Path:
    namespace, path = advancement_id.split(":", 1)
    return PROGRESSION_ADVANCEMENTS / namespace / "advancement" / f"{path}.json"


def main() -> int:
    errors: list[str] = []
    module = runpy.run_path(str(GENERATOR))

    try:
        module["validate_chapters"](module["CHAPTERS"])
    except Exception as exc:  # pragma: no cover - this is a command-line guardrail
        errors.append(f"generator chapter validation failed: {exc}")

    expected = expected_files(module)

    for jar in sorted(REQUIRED_MOD_JARS):
        client = ROOT / "client" / "mods" / jar
        server = ROOT / "server" / "mods" / jar
        if not client.exists():
            errors.append(f"missing client FTB dependency jar: {jar}")
        if not server.exists():
            errors.append(f"missing server FTB dependency jar: {jar}")
        if client.exists() and server.exists() and file_hash(client) != file_hash(server):
            errors.append(f"client/server FTB dependency jar hash mismatch: {jar}")

    side_roots = {side: ROOT / side / QUEST_REL for side in ("client", "server")}
    for side, quest_root in side_roots.items():
        if not quest_root.exists():
            errors.append(f"{side}: missing questbook root {quest_root}")
            continue
        actual = rel_files(quest_root)
        expected_set = set(expected)
        for extra in sorted(actual - expected_set):
            errors.append(f"{side}: unexpected generated questbook file {extra}")
        for missing in sorted(expected_set - actual):
            errors.append(f"{side}: missing generated questbook file {missing}")
        for rel in sorted(actual & expected_set):
            text = (quest_root / rel).read_text(encoding="utf-8")
            if text != expected[rel]:
                errors.append(f"{side}: {rel} is stale; rerun tools/generate-ftb-questbook.py")

    if all(root.exists() for root in side_roots.values()):
        client_files = rel_files(side_roots["client"])
        server_files = rel_files(side_roots["server"])
        for rel in sorted(client_files & server_files):
            if file_hash(side_roots["client"] / rel) != file_hash(side_roots["server"] / rel):
                errors.append(f"client/server questbook mismatch: {rel}")

    generated_text = ""
    server_root = side_roots["server"]
    if server_root.exists():
        generated_text = "\n".join(
            (server_root / rel).read_text(encoding="utf-8") for rel in sorted(rel_files(server_root))
        )
        if "Count:" in generated_text:
            errors.append("questbook uses legacy ItemStack Count field; use lowercase count")
        for phrase, reason in BANNED_GENERATED_TEXT.items():
            if phrase.lower() in generated_text.lower():
                errors.append(f"banned questbook phrase {phrase!r}: {reason}")

        for task_block in re.finditer(r'type: "advancement"\s+advancement: "([^"]+)"', generated_text):
            advancement_id = task_block.group(1)
            if not advancement_path(advancement_id).exists():
                errors.append(f"advancement task target missing: {advancement_id}")

    if errors:
        print("FTB questbook check failed:")
        for error in errors:
            print(f"- {error}")
        return 1

    print(f"FTB questbook checks passed: {len(expected)} generated files, {len(REQUIRED_MOD_JARS)} required FTB jars verified.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
