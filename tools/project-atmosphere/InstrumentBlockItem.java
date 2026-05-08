package net.Gabou.projectatmosphere.items;

import net.Gabou.projectatmosphere.blocks.InstrumentReader;
import net.Gabou.projectatmosphere.util.InstrumentUtils;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public abstract class InstrumentBlockItem extends BlockItem implements InstrumentReader {
    public InstrumentBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && !player.isShiftKeyDown()) {
            if (!context.getLevel().isClientSide) {
                tenpackDisplay(context.getLevel(), player);
            }
            return InteractionResult.SUCCESS;
        }
        return super.useOn(context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                tenpackDisplay(level, player);
            }
            return InteractionResultHolder.success(player.getItemInHand(hand));
        }
        return super.use(level, player, hand);
    }

    private void tenpackDisplay(Level level, Player player) {
        // Project Atmosphere 0.8.1.0 can throw AbstractMethodError here on NeoForge
        // when the subclass display method is invoked virtually. Dispatch by class name
        // to preserve the intended instrument behavior while avoiding a server crash.
        switch (this.getClass().getSimpleName()) {
            case "Anemometer" -> InstrumentUtils.displayWind(level, player);
            case "Barometre" -> InstrumentUtils.displayPressure(level, player);
            case "Humidimeter" -> InstrumentUtils.displayHumidity(level, player);
            case "Thermometre" -> InstrumentUtils.displayTemperature(level, player);
            default -> {
                try {
                    this.display(level, player);
                } catch (AbstractMethodError ignored) {
                    // Unknown instrument subclass; fail closed instead of crashing the server.
                }
            }
        }
    }
}
