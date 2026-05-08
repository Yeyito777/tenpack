package net.Gabou.projectatmosphere.compat;

import dev.nonamecrackers2.simpleclouds.api.common.cloud.spawning.SpawnInfo;
import dev.nonamecrackers2.simpleclouds.common.cloud.SimpleCloudsConstants;
import dev.nonamecrackers2.simpleclouds.common.cloud.region.CloudRegion;
import dev.nonamecrackers2.simpleclouds.common.cloud.spawning.CloudGenerator;
import dev.nonamecrackers2.simpleclouds.common.cloud.spawning.CloudSpawningConfig;
import dev.nonamecrackers2.simpleclouds.common.world.CloudManager;
import dev.nonamecrackers2.simpleclouds.common.world.ServerCloudManager;
import dev.nonamecrackers2.simpleclouds.common.world.SpawnRegion;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.Gabou.projectatmosphere.ProjectAtmosphere;
import net.Gabou.projectatmosphere.manager.ForecastOrchestrator;
import net.Gabou.projectatmosphere.manager.SimpleCloudSpawner;
import net.Gabou.projectatmosphere.modules.core.CloudLibrary;
import net.Gabou.projectatmosphere.modules.core.WindVector;
import net.Gabou.projectatmosphere.util.BiomeInstanceKey;
import net.Gabou.projectatmosphere.util.WeatherSampler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.BiasedToBottomInt;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import org.joml.Vector2i;

public class SimpleCloudsCompat {
    public static ServerCloudManager cloudManager;
    public static CloudGenerator generator;
    private static final float MIN_PER_TICK = 0.001f;
    private static final float MAX_PER_TICK = 0.02f;
    private static final float BASE_ACCEL = 0.002f;
    private static final float ACCEL_PER_WIND = 5.0E-4f;
    public static RandomSource random;
    public static CloudSpawningConfig spawnConfig;
    public static final int SCALE = 8;
    public static final int MIN_RADIUS;
    public static final int MAX_RADIUS;
    private static boolean isInit;

    public static void init(ServerLevel level) {
        cloudManager = (ServerCloudManager)CloudManager.get(level);
        generator = cloudManager.getCloudGenerator();
        spawnConfig = generator.getSpawnConfig().get();
    }

    public static void setIsInit(boolean b) {
        isInit = b;
    }

    public static boolean getIsInit() {
        return isInit;
    }

    public static CloudRegion spawnCloudInBiome(String cloudId, BiomeInstanceKey key, ServerLevel level, CloudRegion dummy, WindVector windVector) {
        if (!isInit) {
            ProjectAtmosphere.LOGGER.warn("[Atmosphere] SimpleClouds is not ready yet, cannot spawn cloud: {}", cloudId);
            return null;
        }
        ResourceLocation rl = ResourceLocation.fromNamespaceAndPath("simpleclouds", cloudId);
        CloudSpawningConfig.Info info = spawnConfig.getWeightInfo(rl);
        if (info == null) {
            ProjectAtmosphere.LOGGER.warn("[Atmosphere] Unknown cloud type: {}", cloudId);
            return null;
        }
        ProjectAtmosphere.LOGGER.info("[Atmosphere] Spawning cloud: " + cloudId);
        List<SpawnRegion> regions = generator.getSpawnRegions();
        float x;
        float z;
        Optional<CloudRegion> region;
        if (regions.isEmpty()) {
            x = key.samplePos().getX() + 0.5f;
            z = key.samplePos().getZ() + 0.5f;
            if (dummy != null) {
                boolean added = generator.addCloud(dummy, CloudGenerator.Order.USE_WEIGHT);
                region = added ? Optional.of(dummy) : Optional.empty();
            } else {
                Optional<CloudRegion> created = SimpleCloudsCompat.createRegion(info, key, level, random, windVector, generator);
                if (created.isPresent() && generator.addCloud(created.get(), CloudGenerator.Order.USE_WEIGHT)) {
                    region = created;
                } else {
                    region = Optional.empty();
                }
            }
        } else {
            SpawnRegion targetRegion = regions.iterator().next();
            x = targetRegion.x() + 0.5f;
            z = targetRegion.z() + 0.5f;
            region = dummy != null
                ? generator.spawnCloud(() -> info, spawnConfig.getSpawnInterval().sample(random), spawnConfig.getMaxRegions(), level, (spawnInfo, playerX, playerZ, realX, realZ, rand, grow) -> SimpleCloudsCompat.regionDummy(dummy))
                : generator.spawnCloud(() -> info, spawnConfig.getSpawnInterval().sample(random), spawnConfig.getMaxRegions(), level, (spawnInfo, playerX, playerZ, realX, realZ, rand, grow) -> SimpleCloudsCompat.createRegion(spawnInfo, key, level, rand, windVector, generator));
        }
        region.ifPresentOrElse(r -> ProjectAtmosphere.LOGGER.info("[Atmosphere] Spawned {} at {}, {} in {}", cloudId, Float.valueOf(x), Float.valueOf(z), key.biomeType()), () -> ProjectAtmosphere.LOGGER.warn("[Atmosphere] Failed to spawn {} in {}", cloudId, key.biomeType()));
        return region.orElse(null);
    }

    public static Optional<CloudRegion> regionDummy(CloudRegion region) {
        return Optional.of(region);
    }

    public static Optional<CloudRegion> createRegion(SpawnInfo info, BiomeInstanceKey biomeKey, ServerLevel level, RandomSource random, WindVector wind, CloudGenerator generator) {
        float x = biomeKey.samplePos().getX();
        float z = biomeKey.samplePos().getZ();
        float windAngleRad = wind.angleRadians();
        float dx = (float)Math.sin(windAngleRad);
        float dz = (float)Math.cos(windAngleRad);
        Vec2 direction = new Vec2(dx, dz).normalized();
        float rotation = windAngleRad + (float)Math.PI;
        Optional<CloudRegion> region = generator.createRegion(info, 10.0f, 10.0f, x, z, random, true);
        if (region.isEmpty()) {
            return Optional.empty();
        }
        CloudRegion cloudRegion = region.get();
        cloudRegion.setMovementDirection(direction);
        cloudRegion.setRotation(rotation);
        float targetPerTick = wind.baseSpeed() / 20.0f;
        targetPerTick = Mth.clamp(targetPerTick, 0.001f, 0.02f);
        cloudRegion.setMaxSpeed(targetPerTick);
        float acc = cloudRegion.getAccelerationFactor();
        float accel = acc + 5.0E-4f * wind.baseSpeed();
        accel = Mth.clamp(accel, 0.001f, 0.01f);
        cloudRegion.setAccelerationFactor(accel);
        cloudRegion.setRadius(700.0f);
        return Optional.of(cloudRegion);
    }

    public static void doInitialGenWithWeather(int x, int z, ServerLevel level) {
        List<SpawnRegion> regions = generator.getSpawnRegions();
        SpawnRegion region = regions.stream().filter(r -> r.includesPoint(x, z)).findFirst().orElseGet(() -> new SpawnRegion(x, z, SimpleCloudsConstants.SPAWN_RADIUS));
        CloudSpawningConfig config = spawnConfig;
        if (generator.getCloudsInRegion(region).size() > config.getMaxInitialRegions()) {
            return;
        }
        block0: for (int i = 0; i < config.getMaxInitialRegions(); ++i) {
            int sharedRadius = BiasedToBottomInt.of(MIN_RADIUS, MAX_RADIUS).sample(random);
            for (int j = 0; j < 10; ++j) {
                Vector2i pos;
                if (generator.getClouds().isEmpty()) {
                    sharedRadius = 200;
                    pos = new Vector2i(x, z);
                } else {
                    pos = SpawnRegion.getRandomPointInRegion(region, random);
                }
                if (generator.getCloudsInRegion(region).size() >= config.getMaxInitialRegions()) {
                    return;
                }
                boolean intersectsOther = regions.stream().filter(r -> r != region).anyMatch(r -> r.includesPoint(pos.x, pos.y));
                Set<BiomeInstanceKey> keys;
                WeatherSampler.WeatherStats stats;
                if (intersectsOther || (stats = WeatherSampler.computeWeatherStats(keys = WeatherSampler.sampleBiomesInArea(pos.x, pos.y, sharedRadius, level), level, level.getGameTime())) == null) continue;
                String cloudId = CloudLibrary.getCloudIdFromSeverity(SimpleCloudSpawner.determineCloudSeverity(stats.temperature(), stats.humidity(), stats.pressure(), SimpleCloudSpawner.calculateDewPoint(stats.temperature(), stats.humidity()), stats.stormChance(), level));
                ResourceLocation rl = ResourceLocation.fromNamespaceAndPath("simpleclouds", cloudId);
                CloudSpawningConfig.Info selected = config.getWeightInfo(rl);
                if (selected == null) {
                    return;
                }
                Optional<CloudRegion> cloudFormation = SimpleCloudsCompat.createRegion(selected, new BiomeInstanceKey(stats.dominantBiome(), stats.pos()), level, random, stats.windVector(), generator);
                int finalSharedRadius = sharedRadius;
                cloudFormation.ifPresent(cf -> {
                    cf.setRadius((float)finalSharedRadius);
                    generator.addCloud(cf, CloudGenerator.Order.USE_WEIGHT);
                    SimpleCloudsCompat.setIsInit(true);
                });
                continue block0;
            }
        }
    }

    public static double getCloudScale() {
        return 8.0;
    }

    static {
        random = RandomSource.create();
        MIN_RADIUS = Math.round(625.0f);
        MAX_RADIUS = Math.round(1178.625f);
        isInit = false;
    }
}
