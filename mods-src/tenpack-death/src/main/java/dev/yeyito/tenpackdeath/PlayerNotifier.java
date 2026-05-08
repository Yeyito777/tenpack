package dev.yeyito.tenpackdeath;

import de.maxhenkel.corpse.entities.CorpseEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.UUID;

final class PlayerNotifier {
    private PlayerNotifier() {
    }

    static void notifyOwner(MinecraftServer server, CorpseEntity corpse, String message) {
        Optional<UUID> ownerId = corpse.getCorpseUUID();
        if (ownerId.isEmpty()) {
            return;
        }
        ServerPlayer owner = server.getPlayerList().getPlayer(ownerId.get());
        if (owner == null) {
            return;
        }
        owner.sendSystemMessage(Component.literal("[Tenpack Death] " + message));
    }
}
