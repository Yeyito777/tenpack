package dev.yeyito.tenpacktravel;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AnimalInspectionPayload(
        int entityId,
        String name,
        String species,
        String health,
        String speed,
        String jump,
        String bond,
        int bondXp,
        String temperament,
        String mood,
        String care,
        String command,
        String role,
        String notes,
        String debug
) implements CustomPacketPayload {
    public static final Type<AnimalInspectionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TenpackTravel.MODID, "animal_inspection"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AnimalInspectionPayload> STREAM_CODEC = StreamCodec.ofMember(
            AnimalInspectionPayload::encode,
            AnimalInspectionPayload::decode
    );

    static AnimalInspectionPayload from(LivingEntity animal, AnimalInspectionReport report, boolean exact) {
        String speed = report.speed() == null ? noData() : AnimalStatBands.speed(report.speed());
        String jump = report.jump() == null || report.jump() <= 0.0D ? noData() : AnimalStatBands.jump(report.jump());
        String debug = exact
                ? translated("screen.tenpack_travel.animal_care.debug_values",
                AnimalStatBands.rounded(report.health()),
                AnimalStatBands.rounded(report.maxHealth()),
                report.speed() == null ? "" : translated("screen.tenpack_travel.animal_care.debug_speed", AnimalStatBands.rounded(report.speed())),
                report.jump() == null ? "" : translated("screen.tenpack_travel.animal_care.debug_jump", AnimalStatBands.rounded(report.jump())),
                report.bond().xp())
                : "";
        return new AnimalInspectionPayload(
                animal.getId(),
                report.name(),
                report.species(),
                translated("screen.tenpack_travel.animal_care.health_summary", AnimalStatBands.maxHealth(report.maxHealth()), AnimalStatBands.currentHealth(report.health(), report.maxHealth())),
                speed,
                jump,
                report.bond().label(),
                report.bond().xp(),
                report.temperament(),
                report.mood(),
                report.care(),
                AnimalCommand.snapshot(animal).label(),
                report.role(),
                report.notes() == null ? "" : report.notes(),
                debug
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(entityId);
        write(buffer, name, 128);
        write(buffer, species, 128);
        write(buffer, health, 96);
        write(buffer, speed, 64);
        write(buffer, jump, 64);
        write(buffer, bond, 64);
        buffer.writeVarInt(bondXp);
        write(buffer, temperament, 96);
        write(buffer, mood, 64);
        write(buffer, care, 160);
        write(buffer, command, 64);
        write(buffer, role, 160);
        write(buffer, notes, 384);
        write(buffer, debug, 192);
    }

    private static AnimalInspectionPayload decode(RegistryFriendlyByteBuf buffer) {
        return new AnimalInspectionPayload(
                buffer.readVarInt(),
                buffer.readUtf(128),
                buffer.readUtf(128),
                buffer.readUtf(96),
                buffer.readUtf(64),
                buffer.readUtf(64),
                buffer.readUtf(64),
                buffer.readVarInt(),
                buffer.readUtf(96),
                buffer.readUtf(64),
                buffer.readUtf(160),
                buffer.readUtf(64),
                buffer.readUtf(160),
                buffer.readUtf(384),
                buffer.readUtf(192)
        );
    }

    static void handle(AnimalInspectionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist != Dist.CLIENT) {
                return;
            }
            try {
                Class.forName("dev.yeyito.tenpacktravel.TenpackTravelClient")
                        .getMethod("openAnimalInspection", AnimalInspectionPayload.class)
                        .invoke(null, payload);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Failed to open Tenpack Travel animal inspection screen", exception);
            }
        });
    }

    private static void write(RegistryFriendlyByteBuf buffer, String value, int maxLength) {
        buffer.writeUtf(value == null ? "" : value, maxLength);
    }

    private static String noData() {
        return translated("screen.tenpack_travel.not_available");
    }

    private static String translated(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }
}
