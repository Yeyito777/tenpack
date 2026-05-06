# Tenpack shared source-of-truth

`shared/` is the source tree for files that should be synchronized into Tenpack client/server instances.

## Layout

- `common/` — files installed on both client and server.
- `client/` — client-only files, layered on top of `common/`.
- `server/` — server-only files, layered on top of `common/`.

The relative paths under these folders are the paths inside a Minecraft instance/server root. For example:

- `shared/common/mods/example.jar` -> `mods/example.jar`
- `shared/client/resourcepacks/example.zip` -> `resourcepacks/example.zip`
- `shared/server/config/example-server.toml` -> `config/example-server.toml`

## Current import

This directory was initialized from the current `client/` and `server/` trees:

- files with identical path and content on both sides went to `shared/common/`
- client-only files went to `shared/client/`
- server-only files went to `shared/server/`

For now, the existing `client/` and `server/` folders are still present for compatibility. Going forward, prefer editing `shared/` first and then using the tools in `tools/` to publish/sync.

## Build static sync output

From the repo root:

```bash
./tools/tenpack-build-public.py --out public
```

This generates a static directory that can be hosted over HTTP/HTTPS:

```text
public/
  client-manifest.json
  server-manifest.json
  tenpack-sync.py
  files/<sha256-prefix>/<sha256>
```

The generated manifests use content-addressed files and SHA-256 verification.
