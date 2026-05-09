package dev.yeyito.tenpacktravel;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;
import java.util.stream.Collectors;

public class HitchingPostBlock extends FenceBlock {
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
            player.sendSystemMessage(Component.literal("Hitched nearby led animals to the post."));
            return bindResult;
        }

        LeashFenceKnotEntity knot = findKnot(level, pos);
        if (knot == null) {
            player.sendSystemMessage(Component.literal("No animals are hitched here."));
            return InteractionResult.SUCCESS;
        }

        List<Entity> animals = LeadItem.leashableInArea(level, pos, leashable -> leashable.getLeashHolder() == knot)
                .stream()
                .filter(Entity.class::isInstance)
                .map(Entity.class::cast)
                .toList();
        if (animals.isEmpty()) {
            player.sendSystemMessage(Component.literal("No animals are hitched here."));
            return InteractionResult.SUCCESS;
        }

        String names = animals.stream()
                .limit(5)
                .map(entity -> entity.getDisplayName().getString())
                .collect(Collectors.joining(", "));
        int extra = animals.size() - Math.min(animals.size(), 5);
        player.sendSystemMessage(Component.literal("Hitched here: " + names + (extra > 0 ? " and " + extra + " more" : "") + "."));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("A camp/stable post for lead-tethering animals.").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.literal("Physical parking: no recall, no teleport, no GPS.").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static LeashFenceKnotEntity findKnot(Level level, BlockPos pos) {
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
