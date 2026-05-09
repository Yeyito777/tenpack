package dev.yeyito.tenpacktravel;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.neoforged.api.distmarker.Dist;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(TenpackTravel.MODID)
public class TenpackTravel {
    public static final String MODID = "tenpack_travel";
    private static final double RIDDEN_TRAVEL_XZ_SQR_THRESHOLD = 0.0025D;
    private static final TagKey<Item> FEED_TROUGH_FOOD = TagKey.create(BuiltInRegistries.ITEM.key(), ResourceLocation.fromNamespaceAndPath(MODID, "feed_trough_food"));

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    private static final DeferredBlock<Block> HITCHING_POST = BLOCKS.registerBlock(
            "hitching_post",
            HitchingPostBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE)
    );

    private static final DeferredBlock<Block> FEED_TROUGH = BLOCKS.registerBlock(
            "feed_trough",
            properties -> new FeedTroughBlock(properties, FEED_TROUGH_FOOD),
            BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL).noOcclusion()
    );

    private static final DeferredItem<BlockItem> HITCHING_POST_ITEM = ITEMS.registerSimpleBlockItem(HITCHING_POST);
    private static final DeferredItem<BlockItem> FEED_TROUGH_ITEM = ITEMS.registerSimpleBlockItem(FEED_TROUGH);

    private static final DeferredItem<Item> GROOMING_BRUSH = ITEMS.registerItem(
            "grooming_brush",
            GroomingBrushItem::new,
            new Item.Properties().durability(128)
    );

    public TenpackTravel(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        modEventBus.addListener(this::addCreativeTabItems);
        modEventBus.addListener(TenpackTravelNetwork::register);
        registerClientOnly(modEventBus);
        NeoForge.EVENT_BUS.register(this);
    }

    private static void registerClientOnly(IEventBus modEventBus) {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }
        try {
            Class.forName("dev.yeyito.tenpacktravel.TenpackTravelClient")
                    .getMethod("register", IEventBus.class)
                    .invoke(null, modEventBus);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to register Tenpack Travel client hooks", exception);
        }
    }

    private void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(GROOMING_BRUSH.get());
        }
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(HITCHING_POST_ITEM.get());
            event.accept(FEED_TROUGH_ITEM.get());
        }
    }

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (!stack.is(GROOMING_BRUSH.get())) {
            return;
        }

        AnimalInspectionReport report = AnimalInspectionReport.from(event.getTarget(), player);
        if (report == null) {
            if (!player.level().isClientSide) {
                player.sendSystemMessage(Component.literal("The brush finds no useful travel notes on this creature."));
            }
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        Level level = player.level();
        if (!level.isClientSide) {
            if (event.getTarget() instanceof LivingEntity living) {
                AnimalBond.brush(player, living);
                report = AnimalInspectionReport.from(living, player);
            }
            boolean exact = player.isCreative() || player.isSpectator();
            for (Component line : report.lines(exact)) {
                player.sendSystemMessage(line);
            }
            if (player instanceof ServerPlayer && !player.hasInfiniteMaterials()) {
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(event.getHand()));
            }
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onEntityMount(EntityMountEvent event) {
        if (!event.isMounting() || event.getLevel().isClientSide) {
            return;
        }
        if (event.getEntityMounting() instanceof Player player && event.getEntityBeingMounted() instanceof LivingEntity animal && AnimalBond.isBondable(animal)) {
            AnimalBond.ride(player, animal);
        }
    }

    @SubscribeEvent
    public void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof LivingEntity animal) || !AnimalBond.isBondable(animal)) {
            return;
        }
        if (riddenHorizontalTravelSqr(animal) < RIDDEN_TRAVEL_XZ_SQR_THRESHOLD) {
            return;
        }
        if (animal.getControllingPassenger() instanceof Player player) {
            AnimalBond.ride(player, animal);
            return;
        }
        if (animal.getFirstPassenger() instanceof Player player) {
            AnimalBond.ride(player, animal);
        }
    }

    private static double riddenHorizontalTravelSqr(LivingEntity animal) {
        double dx = animal.getX() - animal.xOld;
        double dz = animal.getZ() - animal.zOld;
        return dx * dx + dz * dz;
    }
}
