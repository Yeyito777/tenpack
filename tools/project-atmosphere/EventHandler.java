package net.Gabou.projectatmosphere.event;

import dev.nonamecrackers2.simpleclouds.common.cloud.region.CloudRegion;
import dev.nonamecrackers2.simpleclouds.common.cloud.spawning.CloudGenerator;
import dev.nonamecrackers2.simpleclouds.common.cloud.spawning.ServerCloudGenerator;
import dev.nonamecrackers2.simpleclouds.common.world.CloudManager;
import dev.nonamecrackers2.simpleclouds.common.world.ServerCloudManager;
import net.Gabou.projectatmosphere.blocks.BlockManager;
import net.Gabou.projectatmosphere.compat.CompatHandler;
import net.Gabou.projectatmosphere.compat.rainbows.RainbowRainBridge;
import net.Gabou.projectatmosphere.config.AtmoCommonConfig;
import net.Gabou.projectatmosphere.manager.AtmosphereManager;
import net.Gabou.projectatmosphere.manager.AtmosphereWorldEffectsManager;
import net.Gabou.projectatmosphere.manager.ForecastOrchestrator;
import net.Gabou.projectatmosphere.manager.SimpleCloudSpawner;
import net.Gabou.projectatmosphere.modules.core.CloudLibrary;
import net.Gabou.projectatmosphere.modules.wind.WindForces;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class EventHandler {
    private static final int MIN_TICKS_BETWEEN_TEMPESTA = 2000;
    private static final int EMPTY_SPAWN_REGION_RETRY_TICKS = 200;
    private static int tickCounter = 0;
    private static boolean finishedRegenerating = true;
    private static boolean wasRegenerating = false;
    private static int cloudBoosterTicks = 0;
    private static int emptySpawnRegionRetryTicks = 0;

    private EventHandler() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        Level level;
        if (event.getLevel().isClientSide || !((level = event.getLevel()) instanceof ServerLevel)) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel)level;
        if (!AtmosphereManager.isInitialGenerationDone) {
            return;
        }
        if (serverLevel.players().isEmpty()) {
            return;
        }
        if (!serverLevel.dimension().equals(Level.OVERWORLD)) {
            return;
        }
        long t = serverLevel.getDayTime() % 24000L;
        if (t == 21000L) {
            AtmosphereManager.onSwapProfiles(serverLevel);
        }
        ServerCloudManager cloudManager = (ServerCloudManager)CloudManager.get(serverLevel);
        ServerCloudGenerator generator = cloudManager.getCloudGenerator();
        AtmosphereManager.tick(serverLevel);
        AtmosphereWorldEffectsManager.tick(serverLevel);
        if (ForecastOrchestrator.isRegenerating()) {
            finishedRegenerating = false;
        } else if (!finishedRegenerating) {
            wasRegenerating = true;
        }
        if (emptySpawnRegionRetryTicks > 0) {
            --emptySpawnRegionRetryTicks;
        }
        boolean wantsSpawn = generator.getTicksTillNextGen() - cloudBoosterTicks <= 0 || wasRegenerating;
        if (wantsSpawn) {
            if (generator.getSpawnRegions().isEmpty() && !wasRegenerating && emptySpawnRegionRetryTicks > 0) {
                // SimpleClouds has no player spawn-region cache yet. Do not drive the
                // Atmosphere spawn path every tick; retry its player-region recovery soon.
                cloudBoosterTicks = 0;
            } else {
                SimpleCloudSpawner.trySpawnClouds(serverLevel, (CloudGenerator)generator);
                emptySpawnRegionRetryTicks = generator.getSpawnRegions().isEmpty() ? EMPTY_SPAWN_REGION_RETRY_TICKS : 0;
                wasRegenerating = false;
                finishedRegenerating = true;
                cloudBoosterTicks = 0;
            }
        }
        if (CompatHandler.isRainbowsLoaded()) {
            RainbowRainBridge.sync(serverLevel, (CloudGenerator)generator);
        }
        if (((Boolean)AtmoCommonConfig.ENABLE_STORM_DEBRIS.get()).booleanValue() && tickCounter % 2000 == 0) {
            int cloudY = cloudManager.getCloudHeight();
            for (CloudRegion region : generator.getClouds()) {
                int severity = CloudLibrary.getSeverityFromRessourceLocation(region.getCloudTypeId());
                if (severity <= 5) continue;
                BlockPos pos = new BlockPos((int)region.getWorldX(), cloudY, (int)region.getWorldZ());
                BlockManager.simulateTempesta(serverLevel, pos, (int)region.getRadius());
            }
        }
        if (generator.getClouds().size() <= 3 && !generator.getSpawnRegions().isEmpty()) {
            cloudBoosterTicks += 5;
        } else if (generator.getSpawnRegions().isEmpty()) {
            cloudBoosterTicks = 0;
        }
        ++tickCounter;
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer)) {
            return;
        }
        ServerPlayer player2 = (ServerPlayer)player;
        ServerLevel level = player2.serverLevel();
        if (CompatHandler.isRainbowsLoaded() && level.dimension().equals(event.getTo())) {
            ServerCloudManager cloudManager = (ServerCloudManager)CloudManager.get(level);
            RainbowRainBridge.sendSnapshot(player2, level, (CloudGenerator)cloudManager.getCloudGenerator());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer)) {
            return;
        }
        ServerPlayer player2 = (ServerPlayer)player;
        if (player2.level().isClientSide) {
            return;
        }
        ServerLevel level = player2.serverLevel();
        if (!AtmosphereManager.isInitialGenerationDone || ForecastOrchestrator.isRegenerating()) {
            return;
        }
        if (!level.dimension().equals(Level.OVERWORLD)) {
            return;
        }
        WindForces.applyToPlayer(level, player2, 1.0f);
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity)) {
            return;
        }
        LivingEntity entity2 = (LivingEntity)entity;
        if (entity2.level().isClientSide) {
            return;
        }
        Level level = entity2.level();
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ServerLevel level2 = (ServerLevel)level;
        if (!AtmosphereManager.isInitialGenerationDone || ForecastOrchestrator.isRegenerating()) {
            return;
        }
        if (!level2.dimension().equals(Level.OVERWORLD)) {
            return;
        }
        AtmosphereWorldEffectsManager.applyCloudCoverEffects(level2, entity2);
        if (entity2 instanceof ServerPlayer) {
            return;
        }
        WindForces.applyToEntity(level2, entity2, 1.0f);
    }

    public static void onRegenerate() {
        tickCounter = 0;
        finishedRegenerating = true;
        wasRegenerating = false;
        cloudBoosterTicks = 0;
        emptySpawnRegionRetryTicks = 0;
    }
}
