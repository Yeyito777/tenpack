# Create addon selection pass

Purpose: record the first curated addon batch after the core Create/Aeronautics/oil/food baseline.

## Philosophy

Tenpack should not become a random Create kitchen sink. Addons should do at least one of these:

1. make faction infrastructure more visible;
2. create useful logistics pressure;
3. make farms/kitchens/rails/factories feel social and architectural;
4. support JEI/Ponder-readable learning;
5. avoid bypassing precision, flight, oil, and heavy-industry gates.

## Added in this local pass

### Rails and navigation

- Create: Steam 'n' Rails `0.2.0-beta.2+neoforge-mc1.21.1`
- Create Railways Navigator `1.21.1-beta-0.9.0-C6`
- DragonLib `1.21.1-beta-3.0.26`, required by Railways Navigator
- Architectury API `13.0.8+neoforge`, required by DragonLib

Reason: rail networks are an obvious faction pressure point. They make oilfields, refineries, farms, airfields, cannon foundries, and markets physically connected and contestable.

### Architecture / faction identity

- Create: Copycats+ `3.0.4+mc.1.21.1-neoforge`
- Create Deco `2.1.3`
- Create: Bells & Whistles `0.4.7` for 1.21.1
- Rechiseled `1.2.4-neoforge-mc1.21`
- Rechiseled: Create `1.1.0-neoforge-mc1.21`
- SuperMartijn642's Core Lib `1.1.21-neoforge-mc1.21`, required by Rechiseled
- SuperMartijn642's Config Lib `1.1.8-neoforge-mc1.21`, required by Rechiseled
- Fusion `1.2.12-neoforge-mc1.21.1`, client-side connected texture dependency required by Rechiseled

Reason: faction infrastructure should look like it belongs to someone. Refineries, stations, bridges, hangars, kitchens, and cannon works need more than bare andesite casing.

### Food / farm industry flavor

- Create Slice & Dice `4.2.4`
- Kotlin for Forge `5.11.0`, required by Slice & Dice
- Create Confectionery `1.1.2` for 1.21.1
- Create: Winery `2.0.2-neoforge-1.21.1`
- Create: Bitterballen `1.0.2C` for 1.21.1

Reason: Farmer's Delight and Central Kitchen need actual Create-adjacent food automation/flavor. These support visible farm districts, kitchens, taverns, trade goods, and funny faction culture without becoming a hard hunger tax.

## Chosen vs set aside

For the broader addon decision log, see `notes/create-addon-disposition.md`.
For the tiny/specialized addon sweep, see `notes/create-small-addon-audit.md`.

### Create Deco vs Create: Design n' Decor

Decision for now: **choose Create Deco**, set aside **Create: Design n' Decor**.

Reason: both fill the same broad industrial-decoration niche. Create Deco is a well-known, direct fit for train stations, factories, and rail/industrial architecture. Add Design n' Decor later only if Create Deco + Bells & Whistles + Copycats + Rechiseled still leave a clear aesthetic gap.

## Deferred on purpose

### Create: Enchantment Industry / Enchantable Machinery

Defer to an XP/equipment economy pass. XP automation can be very strong in factions and can affect combat, death mechanics, trading, and enchanted gear availability.

### AE2 / Create: Applied Kinetics

Defer. AE2 was recommended, but Tenpack has not decided whether it wants AE2-style storage/autocrafting at all.

### Create Crafts & Additions

Not added in this pass. It is probably a good fit **if gated and configured**, but it deserves a deliberate electricity bridge decision. See `notes/create-crafts-additions-audit.md`.

### Create Ore Excavation

Very promising as another territorial resource-pressure system, but it should get an oilfield-style design/config pass before adding. The interesting direction is to slow ordinary infinite ore-generation machines and make discovered infinite veins clearly better so ore outposts become real faction pressure points.

### Create: Molten Vents / Create: Trading Floor / Create: Hypertubes

Defer to pressure-point meetings. Vents and ore-like systems affect territory/resources; Trading Floor affects economy; Hypertubes affect travel infrastructure.

### Jetpacks / Stuff 'N Additions / Waystone-like travel

Avoid for now. Tenpack is deliberately protecting animal travel, rails, roads, vehicles, Aeronautics, and fuel logistics. Cheap personal mobility can bypass too much of that.

Waystones: Sable is explicitly out: teleportation is not aligned with the pack, even if the compatibility layer works with Sable/Aeronautics.

### Backpacks

Out. Tenpack is not adding backpacks or Create integrations for backpacks.

### Create Mechanical Extruder / Create: Connected / Configurable Machine Outputs

Out for now. Extruder risks trivial renewable material generation; Connected feels too convenience-heavy; Configurable Machine Outputs is not needed without a specific problem.

### Create: Escalated

Out for now. It is escalators/stairs, not combat escalation, and it does not solve a current Tenpack need.

### Create: New Age / Create: Pattern Schematics

No immediate action. New Age overlaps with the future electricity decision. Pattern Schematics is a soft maybe for builders, but not urgent.

## Runtime caveat

This pass was statically checked and added to manifests, but still needs an actual NeoForge launch test. Railways Navigator and Steam 'n Rails 1.21.1 builds are beta-versioned; if launch stability is poor, remove them before deployment rather than forcing the issue.
