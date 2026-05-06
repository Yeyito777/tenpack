# tenpack

NeoForge **Minecraft 1.21.1** modpack files, configs, and reference specs for Tenpack.

## Layout

- `client/` — client-side mods, configs, resource packs, and shader packs.
- `server/` — server-side mods and configs.
- `shared/` — intended source-of-truth for synchronized files:
  - `shared/common/` installs on both client and server.
  - `shared/client/` installs only on clients.
  - `shared/server/` installs only on the dedicated server.
- `tools/` — helper scripts for building and consuming static sync manifests.
- `tenpack-specs/` — reference/import specs and override files.
- `archived/` — removed or parked mods/configs kept for reference.

Server-side mods that clients need should be mirrored into both `server/mods/` and `client/mods/`.

Going forward, prefer maintaining pack files in `shared/` and publishing generated manifests with:

```bash
./tools/tenpack-build-public.py --out public
```

Then host `public/` over HTTP/HTTPS. Clients can sync before launch with `tools/tenpack-sync.py` and the hosted `client-manifest.json`.

## License

This repository is MIT licensed for the original pack metadata, scripts, notes, and configuration authored here. Third-party Minecraft mods, resource packs, shader packs, and other bundled assets remain under their respective upstream licenses.
