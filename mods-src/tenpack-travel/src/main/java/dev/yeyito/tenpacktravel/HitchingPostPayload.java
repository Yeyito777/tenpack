package dev.yeyito.tenpacktravel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record HitchingPostPayload(BlockPos pos, String postMode, int postRadius, List<Row> animals) implements CustomPacketPayload {
    private static final int MAX_ROWS = 12;
    public static final Type<HitchingPostPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TenpackTravel.MODID, "hitching_post"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HitchingPostPayload> STREAM_CODEC = StreamCodec.ofMember(
            HitchingPostPayload::encode,
            HitchingPostPayload::decode
    );

    static HitchingPostPayload from(BlockPos pos, Player viewer, HitchingPostBlockEntity post, List<Entity> hitchedAnimals) {
        List<Row> rows = new ArrayList<>();
        Set<UUID> visibleIds = new HashSet<>();
        for (Entity entity : hitchedAnimals.stream().limit(MAX_ROWS).toList()) {
            rows.add(Row.from(pos, viewer, entity));
            visibleIds.add(entity.getUUID());
        }
        if (post != null && rows.size() < MAX_ROWS) {
            for (HitchingPostBlockEntity.RememberedAnimal memory : post.rememberedAnimals()) {
                if (rows.size() >= MAX_ROWS || visibleIds.contains(memory.uuid())) {
                    continue;
                }
                rows.add(Row.fromMemory(memory, post.getLevel()));
            }
        }
        String mode = post == null ? AnimalCommand.Mode.STAY.label() : post.postMode().label();
        int radius = post == null ? HitchingPostBlockEntity.STAY_RADIUS : post.postRadius();
        return new HitchingPostPayload(pos, mode, radius, List.copyOf(rows));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        write(buffer, postMode, 32);
        buffer.writeVarInt(postRadius);
        buffer.writeVarInt(Math.min(MAX_ROWS, animals.size()));
        for (Row row : animals.stream().limit(MAX_ROWS).toList()) {
            row.encode(buffer);
        }
    }

    private static HitchingPostPayload decode(RegistryFriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        String postMode = buffer.readUtf(32);
        int postRadius = buffer.readVarInt();
        int count = Math.min(MAX_ROWS, buffer.readVarInt());
        List<Row> rows = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            rows.add(Row.decode(buffer));
        }
        return new HitchingPostPayload(pos, postMode, postRadius, List.copyOf(rows));
    }

    static void handle(HitchingPostPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist != Dist.CLIENT) {
                return;
            }
            try {
                Class.forName("dev.yeyito.tenpacktravel.TenpackTravelClient")
                        .getMethod("openHitchingPost", HitchingPostPayload.class)
                        .invoke(null, payload);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Failed to open Tenpack Travel hitching post screen", exception);
            }
        });
    }

    public record Row(String name, String species, String health, String mood, String care, String command, String bond, String role, String proximity) {
        static Row from(BlockPos pos, Player viewer, Entity entity) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            String species = id.toString();
            String name = entity.getDisplayName().getString();
            String proximity = proximityToPost(pos, entity);

            if (entity instanceof LivingEntity living) {
                AnimalInspectionReport report = AnimalInspectionReport.from(living, viewer);
                if (report != null) {
                    return new Row(
                            name,
                            species,
                            AnimalStatBands.currentHealth(report.health(), report.maxHealth()),
                            report.mood(),
                            report.care(),
                            AnimalCommand.snapshot(living).label(),
                            report.bond().label(),
                            report.role(),
                            proximity
                    );
                }
                AnimalBond.Snapshot bond = AnimalBond.snapshot(living, viewer);
                String bondLabel = AnimalEligibility.isBondable(living) ? bond.label() : noData();
                return new Row(
                        name,
                        species,
                        AnimalStatBands.currentHealth(living.getHealth(), living.getMaxHealth()),
                        AnimalCare.mood(living),
                        AnimalCare.careSummary(living),
                        AnimalCommand.snapshot(living).label(),
                        bondLabel,
                        translated("message.tenpack_travel.hitching_post.row_state.hitched"),
                        proximity
                );
            }

            return new Row(name, species, noData(), noData(), noData(), noData(), noData(), translated("message.tenpack_travel.hitching_post.row_state.hitched"), proximity);
        }

        static Row fromMemory(HitchingPostBlockEntity.RememberedAnimal memory, net.minecraft.world.level.Level level) {
            String mood = memory.lastMood() == null || memory.lastMood().isBlank() ? noData() : memory.lastMood();
            String care = memory.lastCare() == null || memory.lastCare().isBlank() ? translated("message.tenpack_travel.hitching_post.row_state.unknown_care") : memory.lastCare();
            String command = memory.lastCommand() == null || memory.lastCommand().isBlank() ? noData() : memory.lastCommand();
            return new Row(memory.name(), memory.species(), noData(), mood, care, command, noData(), translated("message.tenpack_travel.hitching_post.row_state.remembered"), memory.ageText(level));
        }

        private void encode(RegistryFriendlyByteBuf buffer) {
            write(buffer, name, 128);
            write(buffer, species, 128);
            write(buffer, health, 64);
            write(buffer, mood, 64);
            write(buffer, care, 160);
            write(buffer, command, 64);
            write(buffer, bond, 64);
            write(buffer, role, 160);
            write(buffer, proximity, 64);
        }

        private static Row decode(RegistryFriendlyByteBuf buffer) {
            return new Row(
                    buffer.readUtf(128),
                    buffer.readUtf(128),
                    buffer.readUtf(64),
                    buffer.readUtf(64),
                    buffer.readUtf(160),
                    buffer.readUtf(64),
                    buffer.readUtf(64),
                    buffer.readUtf(160),
                    buffer.readUtf(64)
            );
        }
    }

    private static void write(RegistryFriendlyByteBuf buffer, String value, int maxLength) {
        buffer.writeUtf(value == null ? "" : value, maxLength);
    }

    private static String proximityToPost(BlockPos pos, Entity entity) {
        double dx = entity.getX() - (pos.getX() + 0.5D);
        double dy = entity.getY() - (pos.getY() + 0.5D);
        double dz = entity.getZ() - (pos.getZ() + 0.5D);
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance <= 3.0D) {
            return translated("message.tenpack_travel.hitching_post.proximity.at_post");
        }
        if (distance <= 8.0D) {
            return translated("message.tenpack_travel.hitching_post.proximity.near_post");
        }
        if (distance <= 16.0D) {
            return translated("message.tenpack_travel.hitching_post.proximity.nearby");
        }
        return translated("message.tenpack_travel.hitching_post.proximity.away");
    }

    private static String translated(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }

    private static String noData() {
        return translated("screen.tenpack_travel.not_available");
    }
}
