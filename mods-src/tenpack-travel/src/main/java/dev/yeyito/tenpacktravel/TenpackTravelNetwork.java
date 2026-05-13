package dev.yeyito.tenpacktravel;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class TenpackTravelNetwork {
    private static final String VERSION = "1";

    private TenpackTravelNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(TenpackTravel.MODID).versioned(VERSION);
        registrar.playToServer(WhistlePayload.TYPE, WhistlePayload.STREAM_CODEC, WhistleHandler::handle);
        registrar.playToServer(WhistleCommandPayload.TYPE, WhistleCommandPayload.STREAM_CODEC, WhistleCommandPayload::handle);
        registrar.playToServer(AnimalCommandPayload.TYPE, AnimalCommandPayload.STREAM_CODEC, AnimalCommandPayload::handle);
        registrar.playToServer(HitchingPostCommandPayload.TYPE, HitchingPostCommandPayload.STREAM_CODEC, HitchingPostCommandPayload::handle);
        registrar.playToServer(HitchingPostForgetPayload.TYPE, HitchingPostForgetPayload.STREAM_CODEC, HitchingPostForgetPayload::handle);
        registrar.playToClient(AnimalInspectionPayload.TYPE, AnimalInspectionPayload.STREAM_CODEC, AnimalInspectionPayload::handle);
        registrar.playToClient(HitchingPostPayload.TYPE, HitchingPostPayload.STREAM_CODEC, HitchingPostPayload::handle);
    }
}
