package dev.yeyito.tenpackperf;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.ClientCommandHandler;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Mod(TenpackPerfHarness.MODID)
public final class TenpackPerfHarness {
    public static final String MODID = "tenpack_perf_harness";
    private static final Logger LOGGER = LogManager.getLogger();

    private enum State {
        DISABLED,
        WAITING_FOR_WORLD,
        WARMUP,
        PROFILING,
        STOPPING_SPARK,
        COMPLETE,
        FAILED
    }

    private final boolean enabled;
    private final String runId;
    private final String scenario;
    private final int warmupSeconds;
    private final int durationSeconds;
    private final int shutdownDelaySeconds;
    private final boolean useSpark;
    private final String sparkStartCommand;
    private final String sparkStopCommand;
    private final boolean quitWhenDone;
    private final Path outputDir;

    private State state = State.DISABLED;
    private long stateStartedNs = 0L;
    private long runStartedNs = 0L;
    private long lastFrameNs = 0L;
    private long lastFlushNs = 0L;
    private long frameCount = 0L;
    private double totalFrameMs = 0.0;
    private double minFrameMs = Double.POSITIVE_INFINITY;
    private double maxFrameMs = 0.0;
    private long over16 = 0L;
    private long over25 = 0L;
    private long over33 = 0L;
    private long over50 = 0L;
    private boolean sparkStarted = false;
    private boolean sparkStopIssued = false;
    private BufferedWriter frames;
    private BufferedWriter events;

    public TenpackPerfHarness(IEventBus ignoredModBus) {
        this.enabled = Boolean.parseBoolean(System.getProperty("tenpack.perfharness", "false"));
        this.runId = System.getProperty("tenpack.perfharness.runId", defaultRunId());
        this.scenario = System.getProperty("tenpack.perfharness.scenario", "cloud_pan");
        this.warmupSeconds = intProperty("tenpack.perfharness.warmupSeconds", 30);
        this.durationSeconds = intProperty("tenpack.perfharness.durationSeconds", 120);
        this.shutdownDelaySeconds = intProperty("tenpack.perfharness.shutdownDelaySeconds", 6);
        this.useSpark = Boolean.parseBoolean(System.getProperty("tenpack.perfharness.spark", "true"));
        this.sparkStartCommand = System.getProperty("tenpack.perfharness.sparkStart", "sparkc profiler start");
        this.sparkStopCommand = System.getProperty("tenpack.perfharness.sparkStop", "sparkc profiler stop --save-to-file");
        this.quitWhenDone = Boolean.parseBoolean(System.getProperty("tenpack.perfharness.quit", "true"));
        this.outputDir = Path.of(System.getProperty("tenpack.perfharness.outputDir", "perf-runs/" + this.runId)).toAbsolutePath();

        if (FMLEnvironment.dist != Dist.CLIENT) {
            LOGGER.warn("Tenpack Perf Harness loaded on non-client dist; disabled");
            return;
        }
        if (!this.enabled) {
            LOGGER.info("Tenpack Perf Harness disabled; set -Dtenpack.perfharness=true to enable");
            return;
        }

        this.state = State.WAITING_FOR_WORLD;
        this.runStartedNs = System.nanoTime();
        this.stateStartedNs = this.runStartedNs;
        try {
            Files.createDirectories(this.outputDir);
            this.frames = Files.newBufferedWriter(this.outputDir.resolve("frametimes.csv"), StandardCharsets.UTF_8);
            this.frames.write("frame,timestamp_ns,dt_ms,state,scenario,x,y,z,yaw,pitch\n");
            this.events = Files.newBufferedWriter(this.outputDir.resolve("events.log"), StandardCharsets.UTF_8);
            logEvent("enabled runId=" + this.runId + " scenario=" + this.scenario + " warmupSeconds=" + this.warmupSeconds + " durationSeconds=" + this.durationSeconds + " outputDir=" + this.outputDir);
        } catch (IOException e) {
            this.state = State.FAILED;
            LOGGER.error("Could not initialize Tenpack Perf Harness output directory {}", this.outputDir, e);
            return;
        }

        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(this::onRenderFramePost);
        LOGGER.info("Tenpack Perf Harness enabled; outputDir={}", this.outputDir);
    }

    private void onClientTick(ClientTickEvent.Post event) {
        if (!this.enabled || this.state == State.DISABLED || this.state == State.FAILED || this.state == State.COMPLETE) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        long now = System.nanoTime();
        try {
            switch (this.state) {
                case WAITING_FOR_WORLD -> tickWaitingForWorld(minecraft, now);
                case WARMUP -> tickWarmup(minecraft, now);
                case PROFILING -> tickProfiling(minecraft, now);
                case STOPPING_SPARK -> tickStopping(minecraft, now);
                default -> {
                }
            }
        } catch (Throwable t) {
            fail("harness exception: " + t, t);
        }
    }

    private void onRenderFramePost(RenderFrameEvent.Post event) {
        if (!this.enabled || this.state != State.PROFILING || this.frames == null) {
            this.lastFrameNs = System.nanoTime();
            return;
        }
        long now = System.nanoTime();
        if (this.lastFrameNs == 0L) {
            this.lastFrameNs = now;
            return;
        }
        long dtNs = now - this.lastFrameNs;
        this.lastFrameNs = now;
        if (dtNs <= 0L || dtNs > 5_000_000_000L) {
            return;
        }

        double dtMs = dtNs / 1_000_000.0;
        this.frameCount++;
        this.totalFrameMs += dtMs;
        this.minFrameMs = Math.min(this.minFrameMs, dtMs);
        this.maxFrameMs = Math.max(this.maxFrameMs, dtMs);
        if (dtMs > 16.6667) this.over16++;
        if (dtMs > 25.0) this.over25++;
        if (dtMs > 33.3333) this.over33++;
        if (dtMs > 50.0) this.over50++;

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        try {
            if (player != null) {
                this.frames.write(String.format(Locale.ROOT,
                        "%d,%d,%.6f,%s,%s,%.3f,%.3f,%.3f,%.3f,%.3f%n",
                        this.frameCount, now, dtMs, this.state, this.scenario,
                        player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot()));
            } else {
                this.frames.write(String.format(Locale.ROOT,
                        "%d,%d,%.6f,%s,%s,,,,,%n",
                        this.frameCount, now, dtMs, this.state, this.scenario));
            }
            if (now - this.lastFlushNs > 1_000_000_000L) {
                this.frames.flush();
                this.lastFlushNs = now;
            }
        } catch (IOException e) {
            fail("could not write frame data: " + e, e);
        }
    }

    private void tickWaitingForWorld(Minecraft minecraft, long now) throws IOException {
        if (minecraft.player != null && minecraft.level != null && ClientCommandHandler.getDispatcher() != null) {
            minecraft.setScreen(null);
            transition(State.WARMUP, now, "world ready; starting warmup");
            return;
        }
        if (secondsSince(this.runStartedNs, now) > intProperty("tenpack.perfharness.maxStartupSeconds", 240)) {
            throw new IllegalStateException("timed out waiting for world/player/client command dispatcher");
        }
    }

    private void tickWarmup(Minecraft minecraft, long now) throws IOException {
        driveScenario(minecraft, now, false);
        if (secondsSince(this.stateStartedNs, now) >= this.warmupSeconds) {
            resetFrameStats(now);
            if (this.useSpark) {
                runClientCommand(this.sparkStartCommand);
                this.sparkStarted = true;
                logEvent("spark start command issued: " + this.sparkStartCommand);
            } else {
                logEvent("spark disabled for this run");
            }
            transition(State.PROFILING, now, "warmup complete; profiling started");
        }
    }

    private void tickProfiling(Minecraft minecraft, long now) throws IOException {
        driveScenario(minecraft, now, true);
        if (secondsSince(this.stateStartedNs, now) >= this.durationSeconds) {
            clearInputs(minecraft);
            if (this.useSpark && this.sparkStarted && !this.sparkStopIssued) {
                runClientCommand(this.sparkStopCommand);
                this.sparkStopIssued = true;
                logEvent("spark stop command issued: " + this.sparkStopCommand);
            }
            writeSummary(false, "profiling duration complete");
            transition(State.STOPPING_SPARK, now, "profiling complete; waiting before shutdown");
        }
    }

    private void tickStopping(Minecraft minecraft, long now) throws IOException {
        clearInputs(minecraft);
        if (secondsSince(this.stateStartedNs, now) >= this.shutdownDelaySeconds) {
            writeSummary(true, "complete");
            logEvent("done");
            closeWriters();
            this.state = State.COMPLETE;
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.literal("Tenpack perf harness complete: " + this.outputDir), false);
            }
            if (this.quitWhenDone) {
                minecraft.stop();
            }
        }
    }

    private void driveScenario(Minecraft minecraft, long now, boolean profiling) {
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        double elapsed = (now - this.runStartedNs) / 1_000_000_000.0;
        float yaw;
        float pitch;
        switch (this.scenario) {
            case "fixed" -> {
                yaw = floatProperty("tenpack.perfharness.yaw", player.getYRot());
                pitch = floatProperty("tenpack.perfharness.pitch", 8.0F);
                clearInputs(minecraft);
            }
            case "walk_pan" -> {
                yaw = (float) (floatProperty("tenpack.perfharness.baseYaw", player.getYRot()) + Math.sin(elapsed * 0.20) * 35.0);
                pitch = (float) (floatProperty("tenpack.perfharness.pitch", 5.0F) + Math.sin(elapsed * 0.13) * 5.0);
                minecraft.options.keyUp.setDown(profiling);
                minecraft.options.keySprint.setDown(profiling);
            }
            case "cloud_pan", "static_pan" -> {
                yaw = (float) (floatProperty("tenpack.perfharness.baseYaw", player.getYRot()) + elapsed * floatProperty("tenpack.perfharness.yawDegreesPerSecond", 9.0F));
                pitch = (float) (floatProperty("tenpack.perfharness.pitch", 8.0F) + Math.sin(elapsed * 0.25) * 4.0);
                clearInputs(minecraft);
            }
            default -> {
                yaw = (float) (player.getYRot() + 1.5F);
                pitch = floatProperty("tenpack.perfharness.pitch", 8.0F);
                clearInputs(minecraft);
            }
        }
        pitch = Mth.clamp(pitch, -89.0F, 89.0F);
        player.setYRot(yaw);
        player.setYHeadRot(yaw);
        player.setXRot(pitch);
    }

    private void clearInputs(Minecraft minecraft) {
        minecraft.options.keyUp.setDown(false);
        minecraft.options.keySprint.setDown(false);
        minecraft.options.keyJump.setDown(false);
        minecraft.options.keyLeft.setDown(false);
        minecraft.options.keyRight.setDown(false);
        minecraft.options.keyDown.setDown(false);
    }

    private void runClientCommand(String command) throws IOException {
        String trimmed = command.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        boolean handled = ClientCommandHandler.runCommand(trimmed);
        logEvent("client command handled=" + handled + ": " + trimmed);
    }

    private void transition(State next, long now, String reason) throws IOException {
        this.state = next;
        this.stateStartedNs = now;
        logEvent("state=" + next + " reason=" + reason);
    }

    private void resetFrameStats(long now) {
        this.lastFrameNs = 0L;
        this.lastFlushNs = now;
        this.frameCount = 0L;
        this.totalFrameMs = 0.0;
        this.minFrameMs = Double.POSITIVE_INFINITY;
        this.maxFrameMs = 0.0;
        this.over16 = 0L;
        this.over25 = 0L;
        this.over33 = 0L;
        this.over50 = 0L;
    }

    private void writeSummary(boolean done, String reason) throws IOException {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        double mean = this.frameCount > 0L ? this.totalFrameMs / this.frameCount : 0.0;
        double min = Double.isFinite(this.minFrameMs) ? this.minFrameMs : 0.0;
        String json = "{\n" +
                "  \"schema\": 1,\n" +
                "  \"done\": " + done + ",\n" +
                "  \"failed\": false,\n" +
                "  \"reason\": \"" + json(reason) + "\",\n" +
                "  \"runId\": \"" + json(this.runId) + "\",\n" +
                "  \"scenario\": \"" + json(this.scenario) + "\",\n" +
                "  \"warmupSeconds\": " + this.warmupSeconds + ",\n" +
                "  \"durationSeconds\": " + this.durationSeconds + ",\n" +
                "  \"sparkEnabled\": " + this.useSpark + ",\n" +
                "  \"sparkStartCommand\": \"" + json(this.sparkStartCommand) + "\",\n" +
                "  \"sparkStopCommand\": \"" + json(this.sparkStopCommand) + "\",\n" +
                "  \"frames\": " + this.frameCount + ",\n" +
                "  \"meanFrameMs\": " + fmt(mean) + ",\n" +
                "  \"minFrameMs\": " + fmt(min) + ",\n" +
                "  \"maxFrameMs\": " + fmt(this.maxFrameMs) + ",\n" +
                "  \"over16_7ms\": " + this.over16 + ",\n" +
                "  \"over25ms\": " + this.over25 + ",\n" +
                "  \"over33_3ms\": " + this.over33 + ",\n" +
                "  \"over50ms\": " + this.over50 + ",\n" +
                "  \"player\": " + playerJson(player) + ",\n" +
                "  \"outputDir\": \"" + json(this.outputDir.toString()) + "\",\n" +
                "  \"writtenAt\": \"" + Instant.now() + "\"\n" +
                "}\n";
        Files.writeString(this.outputDir.resolve(done ? "done.json" : "summary.json"), json, StandardCharsets.UTF_8);
        Files.writeString(this.outputDir.resolve("summary.json"), json, StandardCharsets.UTF_8);
        if (this.frames != null) this.frames.flush();
        if (this.events != null) this.events.flush();
    }

    private String playerJson(LocalPlayer player) {
        if (player == null) return "null";
        return String.format(Locale.ROOT,
                "{\"x\":%.3f,\"y\":%.3f,\"z\":%.3f,\"yaw\":%.3f,\"pitch\":%.3f,\"dimension\":\"%s\"}",
                player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot(), json(player.level().dimension().location().toString()));
    }

    private void fail(String reason, Throwable throwable) {
        this.state = State.FAILED;
        LOGGER.error("Tenpack Perf Harness failed: {}", reason, throwable);
        try {
            logEvent("failed: " + reason);
            String json = "{\n" +
                    "  \"schema\": 1,\n" +
                    "  \"done\": true,\n" +
                    "  \"failed\": true,\n" +
                    "  \"reason\": \"" + json(reason) + "\",\n" +
                    "  \"runId\": \"" + json(this.runId) + "\",\n" +
                    "  \"scenario\": \"" + json(this.scenario) + "\",\n" +
                    "  \"outputDir\": \"" + json(this.outputDir.toString()) + "\",\n" +
                    "  \"writtenAt\": \"" + Instant.now() + "\"\n" +
                    "}\n";
            Files.writeString(this.outputDir.resolve("done.json"), json, StandardCharsets.UTF_8);
            Files.writeString(this.outputDir.resolve("summary.json"), json, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        } finally {
            closeWriters();
            if (this.quitWhenDone) {
                Minecraft.getInstance().stop();
            }
        }
    }

    private void logEvent(String message) throws IOException {
        String line = Instant.now() + " " + message;
        LOGGER.info("[TenpackPerf] {}", message);
        if (this.events != null) {
            this.events.write(line);
            this.events.newLine();
            this.events.flush();
        }
    }

    private void closeWriters() {
        try {
            if (this.frames != null) this.frames.close();
        } catch (IOException ignored) {
        }
        try {
            if (this.events != null) this.events.close();
        } catch (IOException ignored) {
        }
    }

    private static int intProperty(String name, int fallback) {
        try {
            return Integer.parseInt(System.getProperty(name, Integer.toString(fallback)));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static float floatProperty(String name, float fallback) {
        try {
            return Float.parseFloat(System.getProperty(name, Float.toString(fallback)));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static double secondsSince(long startNs, long nowNs) {
        return (nowNs - startNs) / 1_000_000_000.0;
    }

    private static String defaultRunId() {
        return DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC).format(Instant.now());
    }

    private static String json(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.6f", value);
    }
}
