package dev.yeyito.tenpacktravel.mixin;

import de.mrjulsen.crn.network.packets.pain.TeleportPlayerPacket;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TeleportPlayerPacket.class, remap = false)
public abstract class CrnTeleportPlayerPacketMixin {
    @Inject(method = "handle", at = @At("HEAD"), cancellable = true, remap = false)
    private static void tenpackTravel$blockCrnTeleportPacket(TeleportPlayerPacket packet, NetworkPacketContext context, CallbackInfo callback) {
        callback.cancel();
    }
}
