package dev.yeyito.tenpacktravel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class FeedTroughBlockEntity extends BlockEntity {
    static final int MAX_FEED_UNITS = 12;
    private static final String FEED_ITEMS_KEY = "feed_items";
    private static final String FEED_UNITS_KEY = "feed_units"; // legacy v0.1 abstract counter, migrated to wheat on load
    private static final int SERVER_FEED_INTERVAL_TICKS = 20;
    private static final int EAT_READY_TICKS = 20 * 3;
    private static final int RADIUS = 7;
    private static final double EAT_DISTANCE_SQR = 2.75D * 2.75D;
    private static final double APPROACH_SPEED = 1.0D;
    private static final float HEAL_AMOUNT = 4.0F;

    private final List<ItemStack> feedItems = new ArrayList<>();
    private final Map<UUID, Long> eatingStartedAt = new HashMap<>();

    public FeedTroughBlockEntity(BlockPos pos, BlockState blockState) {
        super(TenpackTravel.FEED_TROUGH_BE.get(), pos, blockState);
    }

    static void serverTick(Level level, BlockPos pos, BlockState state, FeedTroughBlockEntity trough) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (trough.feedUnits() <= 0) {
            trough.eatingStartedAt.clear();
            return;
        }
        if ((serverLevel.getGameTime() + pos.asLong()) % SERVER_FEED_INTERVAL_TICKS != 0L) {
            return;
        }
        trough.tryServeNearby(serverLevel, pos, null, 1);
    }

    int feedUnits() {
        int units = 0;
        for (ItemStack stack : feedItems) {
            units += stack.getCount();
        }
        return units;
    }

    boolean isFull() {
        return feedUnits() >= MAX_FEED_UNITS;
    }

    int addFeed(ItemStack stack, int amount) {
        int inserted = storeFeedCopy(stack, amount);
        if (inserted <= 0) {
            return 0;
        }
        changedFill();
        return inserted;
    }

    ItemStack removeOneFeed() {
        ItemStack removed = takeOneFeed();
        if (!removed.isEmpty()) {
            changedFill();
        }
        return removed;
    }

    void dropContents(Level level, BlockPos pos) {
        if (level.isClientSide || feedItems.isEmpty()) {
            return;
        }
        for (ItemStack stack : feedItems) {
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack.copy());
            }
        }
        feedItems.clear();
        setChanged();
    }

    int tryServeNearby(ServerLevel level, BlockPos pos, Player feeder, int maxAnimals) {
        if (feedUnits() <= 0 || maxAnimals <= 0) {
            return 0;
        }
        List<LivingEntity> hungryAnimals = level.getEntitiesOfClass(LivingEntity.class, troughArea(pos), AnimalEligibility::canUseTrough)
                .stream()
                .filter(AnimalCare::shouldTakeTroughFeed)
                .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(pos.getCenter())))
                .toList();
        pruneEatingTimers(hungryAnimals);

        int served = 0;
        long now = level.getGameTime();
        for (LivingEntity animal : hungryAnimals) {
            if (feedUnits() <= 0 || served >= maxAnimals) {
                break;
            }
            double distance = animal.distanceToSqr(pos.getCenter());
            if (distance > EAT_DISTANCE_SQR) {
                eatingStartedAt.remove(animal.getUUID());
                callAnimalTowardTrough(animal, pos);
                continue;
            }
            if (!waitAtTrough(animal, pos, now)) {
                continue;
            }
            if (serveAnimal(level, animal, feeder)) {
                eatingStartedAt.remove(animal.getUUID());
                served++;
            }
        }
        return served;
    }

    private void pruneEatingTimers(List<LivingEntity> hungryAnimals) {
        Set<UUID> hungryIds = new HashSet<>();
        for (LivingEntity animal : hungryAnimals) {
            hungryIds.add(animal.getUUID());
        }
        eatingStartedAt.keySet().removeIf(uuid -> !hungryIds.contains(uuid));
    }

    private boolean waitAtTrough(LivingEntity animal, BlockPos pos, long now) {
        UUID uuid = animal.getUUID();
        long startedAt = eatingStartedAt.computeIfAbsent(uuid, ignored -> now);
        settleAnimalAtTrough(animal, pos);
        return now - startedAt >= EAT_READY_TICKS;
    }

    private boolean serveAnimal(ServerLevel level, LivingEntity animal, Player feeder) {
        ItemStack meal = takeOneFeed();
        if (meal.isEmpty()) {
            return false;
        }
        if (animal.getHealth() < animal.getMaxHealth()) {
            animal.heal(HEAL_AMOUNT);
        }
        AnimalCare.CareUpdate care = feeder == null ? AnimalCare.feedFromTrough(animal) : AnimalCare.feed(feeder, animal);
        if (feeder != null) {
            AnimalBond.feed(feeder, animal);
        }
        AnimalCare.playFeedingReaction(level, animal, care.firstToday());
        changedFill();
        return true;
    }

    private void callAnimalTowardTrough(LivingEntity animal, BlockPos pos) {
        if (animal instanceof Mob mob) {
            mob.getNavigation().moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, APPROACH_SPEED);
            mob.getLookControl().setLookAt(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 20.0F, 20.0F);
        }
    }

    private void settleAnimalAtTrough(LivingEntity animal, BlockPos pos) {
        if (animal instanceof Mob mob) {
            mob.getNavigation().stop();
            mob.getLookControl().setLookAt(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 20.0F, 20.0F);
        }
    }

    Component statusText() {
        int units = feedUnits();
        if (units <= 0) {
            return Component.translatable("message.tenpack_travel.feed_trough.status_empty");
        }
        return Component.translatable("message.tenpack_travel.feed_trough.status_filled", units, MAX_FEED_UNITS, feedSummary());
    }

    private int storeFeedCopy(ItemStack stack, int amount) {
        if (stack.isEmpty() || amount <= 0) {
            return 0;
        }
        int toInsert = Math.max(0, Math.min(Math.min(amount, stack.getCount()), MAX_FEED_UNITS - feedUnits()));
        int remaining = toInsert;
        for (ItemStack stored : feedItems) {
            if (remaining <= 0) {
                break;
            }
            if (!ItemStack.isSameItemSameComponents(stored, stack)) {
                continue;
            }
            int room = Math.min(stored.getMaxStackSize(), MAX_FEED_UNITS) - stored.getCount();
            if (room <= 0) {
                continue;
            }
            int moved = Math.min(room, remaining);
            stored.grow(moved);
            remaining -= moved;
        }
        while (remaining > 0) {
            int moved = Math.min(remaining, Math.min(stack.getMaxStackSize(), MAX_FEED_UNITS));
            feedItems.add(stack.copyWithCount(moved));
            remaining -= moved;
        }
        return toInsert;
    }

    private ItemStack takeOneFeed() {
        for (int i = feedItems.size() - 1; i >= 0; i--) {
            ItemStack stored = feedItems.get(i);
            if (stored.isEmpty()) {
                feedItems.remove(i);
                continue;
            }
            ItemStack meal = stored.split(1);
            if (stored.isEmpty()) {
                feedItems.remove(i);
            }
            return meal;
        }
        return ItemStack.EMPTY;
    }

    private String feedSummary() {
        if (feedItems.isEmpty()) {
            return "empty";
        }
        StringBuilder summary = new StringBuilder();
        int shown = 0;
        for (ItemStack stack : feedItems) {
            if (stack.isEmpty()) {
                continue;
            }
            if (shown > 0) {
                summary.append(", ");
            }
            summary.append(stack.getHoverName().getString());
            if (stack.getCount() > 1) {
                summary.append(" x").append(stack.getCount());
            }
            shown++;
            if (shown >= 3) {
                break;
            }
        }
        int hiddenStacks = Math.max(0, feedItems.size() - shown);
        if (hiddenStacks > 0) {
            summary.append(", …");
        }
        return summary.toString();
    }

    private void changedFill() {
        if (level == null) {
            return;
        }
        setChanged();
        BlockState state = getBlockState();
        if (state.hasProperty(FeedTroughBlock.FILL)) {
            int fillLevel = fillLevel();
            if (state.getValue(FeedTroughBlock.FILL) != fillLevel) {
                level.setBlock(worldPosition, state.setValue(FeedTroughBlock.FILL, fillLevel), 3);
            }
        }
    }

    private int fillLevel() {
        int units = feedUnits();
        if (units <= 0) {
            return 0;
        }
        if (units <= MAX_FEED_UNITS / 3) {
            return 1;
        }
        if (units <= (MAX_FEED_UNITS * 2) / 3) {
            return 2;
        }
        return 3;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        feedItems.clear();
        if (tag.contains(FEED_ITEMS_KEY, Tag.TAG_LIST)) {
            ListTag items = tag.getList(FEED_ITEMS_KEY, Tag.TAG_COMPOUND);
            for (int i = 0; i < items.size(); i++) {
                ItemStack stack = ItemStack.parseOptional(registries, items.getCompound(i));
                storeFeedCopy(stack, stack.getCount());
            }
        } else if (tag.contains(FEED_UNITS_KEY, Tag.TAG_ANY_NUMERIC)) {
            int legacyUnits = Math.max(0, Math.min(MAX_FEED_UNITS, tag.getInt(FEED_UNITS_KEY)));
            storeFeedCopy(new ItemStack(Items.WHEAT, legacyUnits), legacyUnits);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag items = new ListTag();
        for (ItemStack stack : feedItems) {
            if (!stack.isEmpty()) {
                items.add(stack.save(registries));
            }
        }
        tag.put(FEED_ITEMS_KEY, items);
        tag.putInt(FEED_UNITS_KEY, feedUnits());
    }

    private static AABB troughArea(BlockPos pos) {
        return new AABB(pos).inflate(RADIUS);
    }
}
