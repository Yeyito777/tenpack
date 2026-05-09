package dev.yeyito.tenpacktravel;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class TenpackTravelClient {
    private static final KeyMapping WHISTLE = new KeyMapping(
            "key.tenpack_travel.whistle",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "key.categories.tenpack_travel"
    );

    private TenpackTravelClient() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(TenpackTravelClient::registerKeys);
        NeoForge.EVENT_BUS.addListener(TenpackTravelClient::onClientTick);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(WHISTLE);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        while (WHISTLE.consumeClick()) {
            PacketDistributor.sendToServer(WhistlePayload.INSTANCE);
        }
    }
}
