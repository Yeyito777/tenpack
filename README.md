# tenpack

NeoForge **Minecraft 1.21.1** modpack files, configs, and reference specs for Tenpack.

## Layout

- `client/` — client-side mods, configs, resource packs, and shader packs.
- `server/` — server-side mods and configs.
- `tools/` — helper scripts for building and consuming static sync manifests.
- `tenpack-specs/` — reference/import specs and override files.
- `archived/` — removed or parked mods/configs kept for reference.

Server-side mods that clients need should be mirrored into both `server/mods/` and `client/mods/`.

Build publishable sync manifests directly from `client/` and `server/` with:

```bash
./tools/tenpack-build-public.py --out public
```

Food-system changes can be checked end-to-end with:

```bash
./tools/food-system-validation-harness.py
```

That harness verifies the AppleSkin/CraftTweaker/Supplementaries/LSO/Farmer's
Delight/Create food loop: visibility, no-thirst/no-idle-tax pressure, soft
variety bonuses without Nutrition-style categories, lunch basket config, pinned
food values, heated mixer meal recipes, food advancements, and sync-manifest
delivery against the current source files.

The initial FTB Quests book is generated, not hand-edited in place:

```bash
./tools/generate-ftb-questbook.py
./tools/check-ftb-questbook.py
```

It writes matching quest data under `client/config/ftbquests/quests/` and
`server/config/ftbquests/quests/`, using Tenpack advancements for concrete
milestones and checkmark quests for guidance/social build prompts. The check
verifies regenerated output, client/server parity, FTB dependency jars, icon
item models, and advancement-task targets.

`public/` is tracked in git because kitsune deploys directly from GitHub. Clients can sync before launch with `tools/tenpack-sync.py` and the hosted `client-manifest.json`. The build script validates that every `server/mods/*.jar` exists identically in `client/mods/` before publishing.

Canonical update flow:

```bash
# edit client/ and server/
./tools/tenpack-build-public.py --out public
git add -A
git commit -m "Describe the pack change"
git push
```

kitsune polls GitHub for `main`; when it sees a new commit, it pulls, deploys `public/` to `https://yeyito.dev/tenpack/`, and restarts the Tenpack server.

## License

This repository is MIT licensed for the original pack metadata, scripts, notes, and configuration authored here. Third-party Minecraft mods, resource packs, shader packs, and other bundled assets remain under their respective upstream licenses.
