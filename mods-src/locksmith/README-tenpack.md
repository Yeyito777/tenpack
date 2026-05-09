# Tenpack patched Lucky's Locksmith

This project was reconstructed from `locksmith-1.0.3.jar` with Vineflower so Tenpack can carry a small server-behavior patch.

Tenpack changes:

- Key-locked containers still require a matching key carried by the player to open.
- Locked containers can be broken by anyone; the mod no longer blocks break events for locked containers.
- New locks no longer store/use an owner UUID for access control.

Upstream metadata lists Lucky's Locksmith as All Rights Reserved; keep this patch scoped to Tenpack unless upstream provides permission/a license.
