package dev.yeyito.tenpackfoodui;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

final class TenpackFoodUiNetwork {
    private static final String VERSION = "1";

    private TenpackFoodUiNetwork() {
    }

    static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(TenpackFoodUi.MODID).versioned(VERSION);
        registrar.playToClient(FoodBalancePayload.TYPE, FoodBalancePayload.STREAM_CODEC, FoodBalancePayload::handle);
    }
}
