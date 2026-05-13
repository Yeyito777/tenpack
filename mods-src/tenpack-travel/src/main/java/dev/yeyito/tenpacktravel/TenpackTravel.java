package dev.yeyito.tenpacktravel;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.neoforged.api.distmarker.Dist;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

@Mod(TenpackTravel.MODID)
public class TenpackTravel {
    public static final String MODID = "tenpack_travel";
    private static final TagKey<Item> FEED_TROUGH_FOOD = TagKey.create(BuiltInRegistries.ITEM.key(), ResourceLocation.fromNamespaceAndPath(MODID, "feed_trough_food"));
    private static final int ROUTE_JOURNAL_MODEL_DATA = 1778601;

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MODID);
    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

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

    private static final DeferredBlock<Block> TRAIL_MARKER = BLOCKS.registerBlock(
            "trail_marker",
            TrailMarkerBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE).noOcclusion().lightLevel(state -> 3)
    );

    private static final DeferredBlock<Block> MOORING_POST = BLOCKS.registerBlock(
            "mooring_post",
            MooringPostBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE)
    );

    private static final DeferredBlock<Block> CHANNEL_MARKER = BLOCKS.registerBlock(
            "channel_marker",
            ChannelMarkerBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE).noOcclusion().lightLevel(state -> 4)
    );

    private static final DeferredBlock<Block> CHART_TABLE = BLOCKS.registerBlock(
            "chart_table",
            ChartTableBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.CARTOGRAPHY_TABLE)
    );

    private static final DeferredItem<BlockItem> HITCHING_POST_ITEM = ITEMS.registerSimpleBlockItem(HITCHING_POST);
    private static final DeferredItem<BlockItem> FEED_TROUGH_ITEM = ITEMS.registerSimpleBlockItem(FEED_TROUGH);
    private static final DeferredItem<BlockItem> TRAIL_MARKER_ITEM = ITEMS.registerSimpleBlockItem(TRAIL_MARKER);
    private static final DeferredItem<BlockItem> MOORING_POST_ITEM = ITEMS.registerSimpleBlockItem(MOORING_POST);
    private static final DeferredItem<BlockItem> CHANNEL_MARKER_ITEM = ITEMS.registerSimpleBlockItem(CHANNEL_MARKER);
    private static final DeferredItem<BlockItem> CHART_TABLE_ITEM = ITEMS.registerSimpleBlockItem(CHART_TABLE);

    static final java.util.function.Supplier<BlockEntityType<FeedTroughBlockEntity>> FEED_TROUGH_BE = BLOCK_ENTITIES.register(
            "feed_trough",
            () -> BlockEntityType.Builder.of(FeedTroughBlockEntity::new, FEED_TROUGH.get()).build(null)
    );

    static final java.util.function.Supplier<BlockEntityType<HitchingPostBlockEntity>> HITCHING_POST_BE = BLOCK_ENTITIES.register(
            "hitching_post",
            () -> BlockEntityType.Builder.of(HitchingPostBlockEntity::new, HITCHING_POST.get()).build(null)
    );

    private static final DeferredItem<Item> GROOMING_BRUSH = ITEMS.registerItem(
            "grooming_brush",
            GroomingBrushItem::new,
            new Item.Properties().durability(128)
    );

    private static final DeferredItem<Item> WHISTLE = ITEMS.registerItem(
            "whistle",
            WhistleItem::new,
            new Item.Properties().stacksTo(1)
    );

    private static final java.util.function.Supplier<CreativeModeTab> TENPACK_TRAVEL_TAB = CREATIVE_TABS.register(
            "tenpack_travel",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(WHISTLE.get()))
                    .title(Component.translatable("itemGroup.tenpack_travel"))
                    .displayItems((parameters, output) -> {
                        output.accept(GROOMING_BRUSH.get());
                        output.accept(WHISTLE.get());
                        output.accept(routeJournalStack());
                        output.accept(HITCHING_POST_ITEM.get());
                        output.accept(FEED_TROUGH_ITEM.get());
                        output.accept(TRAIL_MARKER_ITEM.get());
                        output.accept(MOORING_POST_ITEM.get());
                        output.accept(CHANNEL_MARKER_ITEM.get());
                        output.accept(CHART_TABLE_ITEM.get());
                    })
                    .build()
    );

    public TenpackTravel(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
        modEventBus.addListener(this::addCreativeTabItems);
        modEventBus.addListener(TenpackTravelNetwork::register);
        registerClientOnly(modEventBus);
        NeoForge.EVENT_BUS.register(new GroomingBrushHandler(GROOMING_BRUSH));
        NeoForge.EVENT_BUS.register(new MountedTravelBondHandler());
        NeoForge.EVENT_BUS.register(new AnimalCommandHandler());
        NeoForge.EVENT_BUS.register(new AstikorDraftWorkHandler());
        NeoForge.EVENT_BUS.register(new LodestoneCompassPolicyHandler());
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
            event.accept(WHISTLE.get());
            event.accept(routeJournalStack());
        }
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(HITCHING_POST_ITEM.get());
            event.accept(FEED_TROUGH_ITEM.get());
            event.accept(TRAIL_MARKER_ITEM.get());
            event.accept(MOORING_POST_ITEM.get());
            event.accept(CHANNEL_MARKER_ITEM.get());
            event.accept(CHART_TABLE_ITEM.get());
        }
    }

    static boolean isWhistle(ItemStack stack) {
        return stack.is(WHISTLE.get());
    }

    static boolean canUseWhistle(Player player) {
        if (player.isCreative() || player.isSpectator()) {
            return true;
        }
        return player.getInventory().items.stream().anyMatch(TenpackTravel::isWhistle)
                || player.getInventory().offhand.stream().anyMatch(TenpackTravel::isWhistle);
    }

    private static ItemStack routeJournalStack() {
        ItemStack stack = new ItemStack(Items.WRITABLE_BOOK);
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(ROUTE_JOURNAL_MODEL_DATA));
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable("item.tenpack_travel.route_journal").withStyle(style -> style.withItalic(false)));
        stack.set(DataComponents.LORE, new ItemLore(List.of(
                Component.translatable("item.tenpack_travel.route_journal.tooltip.write").withStyle(ChatFormatting.GRAY).withStyle(style -> style.withItalic(false)),
                Component.translatable("item.tenpack_travel.route_journal.tooltip.no_gps").withStyle(ChatFormatting.DARK_GRAY).withStyle(style -> style.withItalic(false))
        )));
        return stack;
    }

}
