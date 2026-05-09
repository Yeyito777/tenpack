package dev.yeyito.tenpacktravel;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Comparator;
import java.util.List;

public class FeedTroughBlock extends Block {
    static final int RADIUS = 5;
    static final float HEAL_AMOUNT = 4.0F;
    static final int MAX_ANIMALS_PER_ITEM = 3;

    private final TagKey<Item> feedTag;

    public FeedTroughBlock(BlockBehaviour.Properties properties, TagKey<Item> feedTag) {
        super(properties);
        this.feedTag = feedTag;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.is(feedTag)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        List<LivingEntity> woundedAnimals = level.getEntitiesOfClass(LivingEntity.class, troughArea(pos), FeedTroughBlock::canUseTrough)
                .stream()
                .filter(entity -> entity.getHealth() < entity.getMaxHealth())
                .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(pos.getCenter())))
                .toList();

        if (woundedAnimals.isEmpty()) {
            player.sendSystemMessage(Component.literal("The trough is ready, but no nearby working animals need feed."));
            return ItemInteractionResult.CONSUME;
        }

        int healed = 0;
        int bonded = 0;
        for (LivingEntity animal : woundedAnimals) {
            animal.heal(HEAL_AMOUNT);
            if (AnimalBond.feed(player, animal)) {
                bonded++;
            }
            healed++;
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HEART, animal.getX(), animal.getY() + animal.getBbHeight() + 0.15, animal.getZ(), 2, 0.25, 0.15, 0.25, 0.02);
            }
            if (healed >= MAX_ANIMALS_PER_ITEM) {
                break;
            }
        }

        stack.consume(1, player);
        level.playSound(null, pos, SoundEvents.GENERIC_EAT, SoundSource.BLOCKS, 0.8F, 0.9F + level.random.nextFloat() * 0.2F);
        player.sendSystemMessage(Component.literal("The trough feeds " + healed + " nearby working animal" + (healed == 1 ? "." : "s.")
                + (bonded > 0 ? " Familiar trust grows." : "")));
        return ItemInteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("Feed nearby tamed/working animals at camp.").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.literal("Consumes real feed; no automation, no teleport safety.").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static AABB troughArea(BlockPos pos) {
        return new AABB(pos).inflate(RADIUS);
    }

    private static boolean canUseTrough(LivingEntity entity) {
        if (!entity.isAlive() || entity.isBaby()) {
            return false;
        }
        if (entity instanceof AbstractHorse horse) {
            return horse.isTamed();
        }
        if (entity instanceof TamableAnimal tameable) {
            return tameable.isTame();
        }
        if (entity instanceof Camel) {
            return true;
        }
        if (entity instanceof Animal) {
            Entity leashHolder = entity instanceof net.minecraft.world.entity.Leashable leashable ? leashable.getLeashHolder() : null;
            return leashHolder != null;
        }
        return false;
    }
}
