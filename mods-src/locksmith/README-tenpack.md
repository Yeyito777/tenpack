# Tenpack patched Lucky's Locksmith

This project was reconstructed from `locksmith-1.0.3.jar` with Vineflower so Tenpack can carry a small server-behavior patch.

Tenpack changes:

- Key-locked containers require a matching key in the player's normal inventory/hand to open.
- Keys no longer auto-equip into the Curios key slot on right-click, and the bundled Curios key slot/tag data is removed so keys do not disappear into a hidden slot.
- Locked containers can be broken by anyone; the mod no longer blocks break events for locked containers.
- New locks no longer store/use an owner UUID for access control.

Upstream metadata lists Lucky's Locksmith as All Rights Reserved; keep this patch scoped to Tenpack unless upstream provides permission/a license.
