package dev.yeyito.tenpacktravel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class HitchingPostBlockEntity extends BlockEntity {
    private static final String MODE_KEY = "post_mode";
    private static final String ANIMALS_KEY = "animals";
    private static final String UUID_KEY = "uuid";
    private static final String NAME_KEY = "name";
    private static final String SPECIES_KEY = "species";
    private static final String LAST_SEEN_KEY = "last_seen";
    private static final int SERVER_REFRESH_INTERVAL_TICKS = 100;
    static final int STAY_RADIUS = 2;
    static final int ROAM_RADIUS = 8;

    private final LinkedHashMap<UUID, RememberedAnimal> rememberedAnimals = new LinkedHashMap<>();
    private AnimalCommand.Mode postMode = AnimalCommand.Mode.STAY;

    public HitchingPostBlockEntity(BlockPos pos, BlockState blockState) {
        super(TenpackTravel.HITCHING_POST_BE.get(), pos, blockState);
    }

    static void serverTick(Level level, BlockPos pos, BlockState state, HitchingPostBlockEntity post) {
        if (level.isClientSide || (level.getGameTime() + pos.asLong()) % SERVER_REFRESH_INTERVAL_TICKS != 0L) {
            return;
        }
        post.rememberCurrentHitchedAnimals();
    }

    AnimalCommand.Mode postMode() {
        return postMode;
    }

    int postRadius() {
        return postMode == AnimalCommand.Mode.ROAM ? ROAM_RADIUS : STAY_RADIUS;
    }

    List<RememberedAnimal> rememberedAnimals() {
        return List.copyOf(rememberedAnimals.values());
    }

    ModeApplyResult setPostMode(Player player, AnimalCommand.Mode mode) {
        if (mode != AnimalCommand.Mode.STAY && mode != AnimalCommand.Mode.ROAM) {
            return new ModeApplyResult(0, 0, false);
        }
        List<Entity> hitched = rememberCurrentHitchedAnimals();
        ModeApplyResult result = countCommandable(player, hitched);
        if (!hitched.isEmpty() && result.applied() == 0 && result.skipped() > 0) {
            return result;
        }
        postMode = mode;
        result = applyModeToHitchedAnimals(player, hitched);
        setChanged();
        return result.withPostModeChanged(true);
    }

    List<Entity> rememberAndApply(Player player, List<Entity> hitched) {
        remember(hitched);
        applyModeToHitchedAnimals(player, hitched);
        setChanged();
        return hitched;
    }

    List<Entity> rememberCurrentHitchedAnimals() {
        if (level == null) {
            return List.of();
        }
        LeashFenceKnotEntity knot = HitchingPostBlock.findKnot(level, worldPosition);
        if (knot == null) {
            return List.of();
        }
        List<Entity> hitched = HitchingPostBlock.hitchedAnimals(level, worldPosition, knot);
        remember(hitched);
        return hitched;
    }

    int forgetUnhitchedMemories() {
        List<Entity> hitched = rememberCurrentHitchedAnimals();
        Set<UUID> hitchedIds = new HashSet<>();
        for (Entity entity : hitched) {
            hitchedIds.add(entity.getUUID());
        }
        int before = rememberedAnimals.size();
        rememberedAnimals.keySet().removeIf(uuid -> !hitchedIds.contains(uuid));
        int removed = before - rememberedAnimals.size();
        if (removed > 0) {
            setChanged();
        }
        return removed;
    }

    private void remember(List<Entity> hitched) {
        if (level == null || hitched.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        for (Entity entity : hitched) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            rememberedAnimals.put(entity.getUUID(), new RememberedAnimal(
                    entity.getUUID(),
                    entity.getDisplayName().getString(),
                    id.toString(),
                    now,
                    AnimalCare.mood(living),
                    AnimalCare.careSummary(living),
                    AnimalCommand.snapshot(living).label()
            ));
        }
        trimMemories();
        setChanged();
    }

    private ModeApplyResult countCommandable(Player player, List<Entity> hitched) {
        int commandable = 0;
        int skipped = 0;
        for (Entity entity : hitched) {
            if (entity instanceof LivingEntity living && AnimalEligibility.isBondable(living)) {
                if (AnimalCommand.canCommand(player, living)) {
                    commandable++;
                } else {
                    skipped++;
                }
            }
        }
        return new ModeApplyResult(commandable, skipped, false);
    }

    private ModeApplyResult applyModeToHitchedAnimals(Player player, List<Entity> hitched) {
        int applied = 0;
        int skipped = 0;
        for (Entity entity : hitched) {
            if (entity instanceof LivingEntity living && AnimalEligibility.isBondable(living)) {
                if (AnimalCommand.canCommand(player, living)) {
                    AnimalCommand.setModeAtAnchor(player, living, postMode, worldPosition);
                    applied++;
                } else {
                    skipped++;
                }
            }
        }
        return new ModeApplyResult(applied, skipped, true);
    }

    private void trimMemories() {
        while (rememberedAnimals.size() > 16) {
            UUID first = rememberedAnimals.keySet().iterator().next();
            rememberedAnimals.remove(first);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        postMode = AnimalCommand.Mode.fromId(tag.getString(MODE_KEY));
        if (postMode != AnimalCommand.Mode.STAY && postMode != AnimalCommand.Mode.ROAM) {
            postMode = AnimalCommand.Mode.STAY;
        }
        rememberedAnimals.clear();
        ListTag animals = tag.getList(ANIMALS_KEY, Tag.TAG_COMPOUND);
        for (Tag entry : animals) {
            if (!(entry instanceof CompoundTag animalTag) || !animalTag.hasUUID(UUID_KEY)) {
                continue;
            }
            RememberedAnimal memory = new RememberedAnimal(
                    animalTag.getUUID(UUID_KEY),
                    animalTag.getString(NAME_KEY),
                    animalTag.getString(SPECIES_KEY),
                    animalTag.getLong(LAST_SEEN_KEY),
                    animalTag.getString("mood"),
                    animalTag.getString("care"),
                    animalTag.getString("command")
            );
            rememberedAnimals.put(memory.uuid(), memory);
        }
        trimMemories();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString(MODE_KEY, postMode.id());
        ListTag animals = new ListTag();
        for (RememberedAnimal memory : rememberedAnimals.values()) {
            CompoundTag animalTag = new CompoundTag();
            animalTag.putUUID(UUID_KEY, memory.uuid());
            animalTag.putString(NAME_KEY, memory.name());
            animalTag.putString(SPECIES_KEY, memory.species());
            animalTag.putLong(LAST_SEEN_KEY, memory.lastSeenTick());
            animalTag.putString("mood", memory.lastMood());
            animalTag.putString("care", memory.lastCare());
            animalTag.putString("command", memory.lastCommand());
            animals.add(animalTag);
        }
        tag.put(ANIMALS_KEY, animals);
    }

    record RememberedAnimal(UUID uuid, String name, String species, long lastSeenTick, String lastMood, String lastCare, String lastCommand) {
        String ageText(Level level) {
            if (level == null || lastSeenTick <= 0L) {
                return translated("message.tenpack_travel.hitching_post.memory.last_seen_earlier");
            }
            long age = Math.max(0L, level.getGameTime() - lastSeenTick);
            if (age < 20L * 60L) {
                return translated("message.tenpack_travel.hitching_post.memory.seen_just_now");
            }
            long minutes = age / (20L * 60L);
            return translated("message.tenpack_travel.hitching_post.memory.last_seen_minutes", minutes);
        }

        private static String translated(String key, Object... args) {
            return Component.translatable(key, args).getString();
        }
    }

    record ModeApplyResult(int applied, int skipped, boolean postModeChanged) {
        ModeApplyResult withPostModeChanged(boolean changed) {
            return new ModeApplyResult(applied, skipped, changed);
        }
    }
}
