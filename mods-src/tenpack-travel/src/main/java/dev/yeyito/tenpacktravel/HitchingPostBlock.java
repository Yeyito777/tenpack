package dev.yeyito.tenpacktravel;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.LeadItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class HitchingPostBlock extends FenceBlock implements EntityBlock {
    public HitchingPostBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        InteractionResult bindResult = LeadItem.bindPlayerMobs(player, level, pos);
        if (bindResult != InteractionResult.PASS) {
            openStableBoard(level, pos, player, true);
            return bindResult;
        }

        if (player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof HitchingPostBlockEntity post) {
            AnimalCommand.Mode nextMode = post.postMode() == AnimalCommand.Mode.STAY ? AnimalCommand.Mode.ROAM : AnimalCommand.Mode.STAY;
            HitchingPostBlockEntity.ModeApplyResult result = post.setPostMode(player, nextMode);
            HitchingPostCommandPayload.sendPostModeResult(player, nextMode, result);
        }

        openStableBoard(level, pos, player, false);
        return InteractionResult.SUCCESS;
    }

    private static void openStableBoard(Level level, BlockPos pos, Player player, boolean justHitched) {
        LeashFenceKnotEntity knot = findKnot(level, pos);
        HitchingPostBlockEntity post = level.getBlockEntity(pos) instanceof HitchingPostBlockEntity hitchingPost ? hitchingPost : null;
        if (knot == null) {
            sendStableBoard(pos, player, post, List.of());
            return;
        }

        List<Entity> animals = hitchedAnimals(level, pos, knot);
        if (post != null) {
            animals = justHitched ? post.rememberAndApply(player, animals) : post.rememberCurrentHitchedAnimals();
        }
        sendStableBoard(pos, player, post, animals);
    }

    static List<Entity> hitchedAnimals(Level level, BlockPos pos, LeashFenceKnotEntity knot) {
        return LeadItem.leashableInArea(level, pos, leashable -> leashable.getLeashHolder() == knot)
                .stream()
                .filter(Entity.class::isInstance)
                .map(Entity.class::cast)
                .toList();
    }

    private static void sendStableBoard(BlockPos pos, Player player, HitchingPostBlockEntity post, List<Entity> animals) {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, HitchingPostPayload.from(pos, player, post, animals));
        } else {
            player.sendSystemMessage(animals.isEmpty()
                    ? Component.translatable("message.tenpack_travel.hitching_post.none_hitched")
                    : Component.translatable("message.tenpack_travel.hitching_post.hitched_count", animals.size()));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("block.tenpack_travel.hitching_post.tooltip.infrastructure").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("block.tenpack_travel.hitching_post.tooltip.mode").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("block.tenpack_travel.hitching_post.tooltip.no_gps").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HitchingPostBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide) {
            return null;
        }
        return (tickLevel, tickPos, tickState, blockEntity) -> {
            if (blockEntity instanceof HitchingPostBlockEntity post) {
                HitchingPostBlockEntity.serverTick(tickLevel, tickPos, tickState, post);
            }
        };
    }

    static LeashFenceKnotEntity findKnot(Level level, BlockPos pos) {
        AABB searchBox = new AABB(
                pos.getX() - 1.0,
                pos.getY() - 1.0,
                pos.getZ() - 1.0,
                pos.getX() + 1.0,
                pos.getY() + 1.0,
                pos.getZ() + 1.0
        );
        for (LeashFenceKnotEntity knot : level.getEntitiesOfClass(LeashFenceKnotEntity.class, searchBox)) {
            if (knot.getPos().equals(pos)) {
                return knot;
            }
        }
        return null;
    }
}
