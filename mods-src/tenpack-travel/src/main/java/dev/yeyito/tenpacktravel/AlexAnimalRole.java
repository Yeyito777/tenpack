package dev.yeyito.tenpacktravel;

record AlexAnimalRole(String role, String temperament, String notes) {
    static AlexAnimalRole forPath(String path) {
        return switch (path) {
            case "bald_eagle" -> new AlexAnimalRole("Scout — falconry", "Sharp", "glove/hood scouting is active player skill, not radar");
            case "bison" -> new AlexAnimalRole("Resource — cold travel", "Herd", "fur improves powdered-snow travel gear");
            case "caiman" -> new AlexAnimalRole("Camp defense — grappler", "Watchful", "imprinted hatchlings hold threats near water camps");
            case "capuchin_monkey" -> new AlexAnimalRole("Companion — shoulder fighter", "Clever", "banana-tamed helper with ranged/melee support");
            case "catfish" -> new AlexAnimalRole("Weird cargo — bucket belly", "Sluggish", "small/medium carry 3/9 stacks; bucket preserves belly contents");
            case "cosmaw" -> new AlexAnimalRole("End safety — fall rescue", "Devoted", "prepared End explorers use this instead of GPS/teleport safety");
            case "crocodile" -> new AlexAnimalRole("Water defense/resource", "Territorial", "imprinted young defend birth area; scutes support swim gear");
            case "crow" -> new AlexAnimalRole("Camp logistics — gather/deposit", "Clever", "hay-block home and framed containers turn camps into living logistics");
            case "elephant" -> new AlexAnimalRole("Freight — 54 slots when chested", "Gentle", "wide roads/stables make this animal practical");
            case "emu" -> new AlexAnimalRole("Resource — projectile-dodge gear", "Fast", "not a pack animal; feathers support travel-combat comfort");
            case "kangaroo" -> new AlexAnimalRole("Pouch — 9 slots", "Alert", "can use pouch weapon/armor and eat pouch food to heal");
            case "grizzly_bear" -> new AlexAnimalRole("Combat mount", "Dangerous", "war mount; powerful but not a freight animal");
            case "gorilla" -> new AlexAnimalRole("Companion — jungle defender", "Protective", "banana trust creates defense, not cargo");
            case "komodo_dragon" -> new AlexAnimalRole("Predator mount", "Savage", "saddled predator mount; costly rotten-flesh taming");
            case "endergrade" -> new AlexAnimalRole("End mount — slow vertical travel", "Passive", "slow End mount; controlled with Chorus on a Stick");
            case "laviathan" -> new AlexAnimalRole("Passengers — up to 4", "Passive", "Nether lava ferry; needs Straddlite Saddle and Tack");
            case "mantis_shrimp" -> new AlexAnimalRole("Utility — block breaker", "Punchy", "powerful aquatic helper; griefing/server rules matter");
            case "mimic_octopus" -> new AlexAnimalRole("Aquatic companion — scare defense", "Shy", "can follow onto land but drying out keeps friction");
            case "mudskipper" -> new AlexAnimalRole("Small amphibious pet", "Hardy", "bucketable follower; flavor and light defense, not logistics");
            case "raccoon" -> new AlexAnimalRole("Friction pet/resource", "Mischievous", "cute thief; tail cap gives minor sneak-speed travel comfort");
            case "rhinoceros" -> new AlexAnimalRole("Village/frontier defense", "Skittish", "wheat-pacified defender, not a mount plan");
            case "seagull" -> new AlexAnimalRole("Navigation clue — treasure bird", "Opportunist", "food theft plus physical treasure-map clue behavior");
            case "seal" -> new AlexAnimalRole("Coastal trade support", "Basking", "fish-for-seabed-item trade makes coast camps useful");
            case "spectre" -> new AlexAnimalRole("End travel — void tow", "Distant", "Soul Heart plus lead creates physical End island travel");
            case "sugar_glider" -> new AlexAnimalRole("Safety — head slow-fall", "Gentle", "vertical comfort pet for cliffs, trees, ravines, and mountains");
            case "tarantula_hawk" -> new AlexAnimalRole("Combat pet/gear path", "Erratic", "anti-arthropod defense; wings lead toward upgraded elytra");
            case "tusklin" -> new AlexAnimalRole("Bucking battle mount", "Hostile", "hogshoes are the upgrade path");
            case "warped_toad" -> new AlexAnimalRole("Nether defense — anti-insect", "Warty", "lava/water-capable follower for bug-heavy Nether expeditions");
            default -> null;
        };
    }
}
