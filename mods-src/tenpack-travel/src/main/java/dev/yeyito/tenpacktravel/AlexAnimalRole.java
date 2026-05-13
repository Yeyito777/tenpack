package dev.yeyito.tenpacktravel;

import net.minecraft.network.chat.Component;

record AlexAnimalRole(String role, String temperament, String notes) {
    static AlexAnimalRole forPath(String path) {
        return switch (path) {
            case "bald_eagle" -> localized(path);
            case "bison" -> localized(path);
            case "caiman" -> localized(path);
            case "capuchin_monkey" -> localized(path);
            case "catfish" -> localized(path);
            case "cosmaw" -> localized(path);
            case "crocodile" -> localized(path);
            case "crow" -> localized(path);
            case "elephant" -> localized(path);
            case "emu" -> localized(path);
            case "kangaroo" -> localized(path);
            case "grizzly_bear" -> localized(path);
            case "gorilla" -> localized(path);
            case "komodo_dragon" -> localized(path);
            case "endergrade" -> localized(path);
            case "laviathan" -> localized(path);
            case "mantis_shrimp" -> localized(path);
            case "mimic_octopus" -> localized(path);
            case "mudskipper" -> localized(path);
            case "raccoon" -> localized(path);
            case "rhinoceros" -> localized(path);
            case "seagull" -> localized(path);
            case "seal" -> localized(path);
            case "spectre" -> localized(path);
            case "sugar_glider" -> localized(path);
            case "tarantula_hawk" -> localized(path);
            case "tusklin" -> localized(path);
            case "warped_toad" -> localized(path);
            default -> null;
        };
    }

    private static AlexAnimalRole localized(String path) {
        String prefix = "message.tenpack_travel.alex_animal_role." + path;
        return new AlexAnimalRole(
                translated(prefix + ".role"),
                translated(prefix + ".temperament"),
                translated(prefix + ".notes")
        );
    }

    private static String translated(String key) {
        return Component.translatable(key).getString();
    }
}
