package dev.yeyito.tenpackfoodui;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(TenpackFoodUi.MODID)
public final class TenpackFoodUi {
    public static final String MODID = "tenpack_food_ui";
    static final Logger LOGGER = LogUtils.getLogger();

    public TenpackFoodUi(IEventBus modEventBus) {
        modEventBus.addListener(TenpackFoodUiNetwork::register);
        NeoForge.EVENT_BUS.register(new FoodBalanceServer());
        registerClientOnly();
    }

    private static void registerClientOnly() {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }
        try {
            Class.forName("dev.yeyito.tenpackfoodui.TenpackFoodUiClient")
                    .getMethod("register")
                    .invoke(null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to register Tenpack Food UI client hooks", exception);
        }
    }
}
