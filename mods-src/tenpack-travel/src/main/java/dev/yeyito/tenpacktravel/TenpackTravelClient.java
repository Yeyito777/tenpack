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
    private static final int WHISTLE_HOLD_TICKS = 10;
    private static final KeyMapping WHISTLE = new KeyMapping(
            "key.tenpack_travel.whistle",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "key.categories.tenpack_travel"
    );
    private static boolean whistleWasDown;
    private static boolean whistleMenuOpened;
    private static int whistleHeldTicks;

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
            resetWhistleState();
            return;
        }
        boolean down = WHISTLE.isDown();
        if (down) {
            if (!whistleWasDown) {
                whistleHeldTicks = 0;
                whistleMenuOpened = false;
            }
            whistleHeldTicks++;
            if (!whistleMenuOpened && whistleHeldTicks >= WHISTLE_HOLD_TICKS && minecraft.screen == null) {
                openWhistleCommands();
                whistleMenuOpened = true;
            }
        } else if (whistleWasDown) {
            if (!whistleMenuOpened && whistleHeldTicks > 0 && whistleHeldTicks < WHISTLE_HOLD_TICKS) {
                PacketDistributor.sendToServer(WhistlePayload.INSTANCE);
            }
            whistleHeldTicks = 0;
            whistleMenuOpened = false;
        }
        whistleWasDown = down;
    }

    public static void openAnimalInspection(AnimalInspectionPayload payload) {
        Minecraft.getInstance().setScreen(new AnimalInspectionScreen(payload));
    }

    public static void openHitchingPost(HitchingPostPayload payload) {
        Minecraft.getInstance().setScreen(new HitchingPostScreen(payload));
    }

    public static void openWhistleCommands() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen == null) {
            minecraft.setScreen(new WhistleCommandScreen());
        }
    }

    private static void resetWhistleState() {
        whistleWasDown = false;
        whistleMenuOpened = false;
        whistleHeldTicks = 0;
    }
}
