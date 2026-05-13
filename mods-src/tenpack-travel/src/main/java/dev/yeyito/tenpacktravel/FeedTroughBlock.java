package dev.yeyito.tenpacktravel;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

public class FeedTroughBlock extends Block implements EntityBlock {
    static final IntegerProperty FILL = IntegerProperty.create("fill", 0, 3);
    private static final int MAX_INSERT_PER_CLICK = FeedTroughBlockEntity.MAX_FEED_UNITS;
    private static final int IMMEDIATE_SERVE_LIMIT = 3;

    private final TagKey<Item> feedTag;

    public FeedTroughBlock(BlockBehaviour.Properties properties, TagKey<Item> feedTag) {
        super(properties);
        this.feedTag = feedTag;
        registerDefaultState(stateDefinition.any().setValue(FILL, 0));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.is(feedTag)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        if (!(level.getBlockEntity(pos) instanceof FeedTroughBlockEntity trough)) {
            return ItemInteractionResult.CONSUME;
        }

        int requested = player.isShiftKeyDown() ? Math.min(stack.getCount(), MAX_INSERT_PER_CLICK) : 1;
        int inserted = trough.addFeed(stack, requested);
        if (inserted <= 0) {
            player.displayClientMessage(Component.translatable("message.tenpack_travel.feed_trough.full"), true);
            return ItemInteractionResult.CONSUME;
        }

        stack.consume(inserted, player);
        level.playSound(null, pos, SoundEvents.COMPOSTER_FILL, SoundSource.BLOCKS, 0.75F, 0.9F + level.random.nextFloat() * 0.2F);
        if (level instanceof ServerLevel serverLevel) {
            int served = trough.tryServeNearby(serverLevel, pos, player, IMMEDIATE_SERVE_LIMIT);
            player.displayClientMessage(served > 0
                    ? Component.translatable("message.tenpack_travel.feed_trough.added_served", inserted, trough.feedUnits(), FeedTroughBlockEntity.MAX_FEED_UNITS, served)
                    : Component.translatable("message.tenpack_travel.feed_trough.added", inserted, trough.feedUnits(), FeedTroughBlockEntity.MAX_FEED_UNITS), true);
        }
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof FeedTroughBlockEntity trough) {
            if (player.isShiftKeyDown()) {
                ItemStack removed = trough.removeOneFeed();
                if (removed.isEmpty()) {
                    player.displayClientMessage(Component.translatable("message.tenpack_travel.feed_trough.empty"), true);
                } else {
                    if (!player.getInventory().add(removed)) {
                        player.drop(removed, false);
                    }
                    level.playSound(null, pos, SoundEvents.BARREL_OPEN, SoundSource.BLOCKS, 0.6F, 1.25F);
                    player.displayClientMessage(Component.translatable("message.tenpack_travel.feed_trough.removed", trough.feedUnits(), FeedTroughBlockEntity.MAX_FEED_UNITS), true);
                }
                return InteractionResult.SUCCESS;
            }
            player.sendSystemMessage(trough.statusText());
        } else {
            player.sendSystemMessage(Component.translatable("message.tenpack_travel.feed_trough.no_state"));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FeedTroughBlockEntity(pos, state);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.getBlock() != newState.getBlock() && level.getBlockEntity(pos) instanceof FeedTroughBlockEntity trough) {
            trough.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide) {
            return null;
        }
        return (tickLevel, tickPos, tickState, blockEntity) -> {
            if (blockEntity instanceof FeedTroughBlockEntity trough) {
                FeedTroughBlockEntity.serverTick(tickLevel, tickPos, tickState, trough);
            }
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FILL);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("block.tenpack_travel.feed_trough.tooltip.infrastructure").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("block.tenpack_travel.feed_trough.tooltip.no_gps").withStyle(ChatFormatting.DARK_GRAY));
    }
}
