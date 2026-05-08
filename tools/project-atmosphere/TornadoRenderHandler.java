package net.Gabou.projectatmosphere.client;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import dev.nonamecrackers2.simpleclouds.common.config.SimpleCloudsConfig;
import net.Gabou.projectatmosphere.modules.tornado.TornadoInstance;
import net.Gabou.projectatmosphere.modules.tornado.TornadoLevel;
import net.Gabou.projectatmosphere.modules.tornado.TornadoManager;
import net.Gabou.projectatmosphere.particles.DebrisParticleData;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class TornadoRenderHandler {
   private static final ResourceLocation NOISE_TEXTURE = ResourceLocation.fromNamespaceAndPath("projectatmosphere", "textures/effects/noise.png");
   private static final ResourceLocation TORNADO_TEXTURE = ResourceLocation.fromNamespaceAndPath("projectatmosphere", "textures/effects/base.png");
   private static final ResourceLocation FLOWMAP_TEXTURE = ResourceLocation.fromNamespaceAndPath("projectatmosphere", "textures/effects/flowmap.png");
   private static final ResourceLocation NORMALMAP_TEXTURE = ResourceLocation.fromNamespaceAndPath("projectatmosphere", "textures/effects/tornado_normal.png");
   private static final float SPAWN_DESCENT_DURATION = 10.0F;

   public static void renderTornado(
      PoseStack stack,
      double tornadoX,
      double tornadoY,
      double tornadoZ,
      float twistSpeed,
      ClientLevel level,
      Camera camera,
      Minecraft minecraft,
      TornadoInstance tornado
   ) {
      ShaderInstance shader = MyShaders.TORNADO;
      if (shader != null) {
         RenderSystem.setShaderTexture(0, TORNADO_TEXTURE);
         RenderSystem.setShaderTexture(1, FLOWMAP_TEXTURE);
         RenderSystem.setShaderTexture(2, NORMALMAP_TEXTURE);
         RenderSystem.setShaderTexture(3, NOISE_TEXTURE);
         RenderSystem.setShader(() -> shader);
         shader.apply();
         int segments = 64;
         int rings = 128;
         float baseRadius = 20.0F;
         float topRadius = 5.0F;
         float height = 356.0F;
         float coneStart = 0.5F;
         float coneFactor = 3.5F;
         stack.pushPose();
         try {
            stack.translate(tornadoX, tornadoY, tornadoZ);
            Matrix4f matrix = stack.last().pose();
            Uniform modelView = shader.getUniform("ModelViewMat");
            if (modelView != null) {
               modelView.set(matrix);
            }

            Uniform projMat = shader.getUniform("ProjMat");
            if (projMat != null) {
               projMat.set(RenderSystem.getProjectionMatrix());
            }

            Uniform timeUniform = shader.getUniform("Time");
            if (timeUniform != null) {
               timeUniform.set(TornadoManager.getShaderTime());
            }

            Uniform twistUniform = shader.getUniform("TwistSpeed");
            if (twistUniform != null) {
               twistUniform.set(twistSpeed);
            }

            Uniform baseRadiusUniform = shader.getUniform("BaseRadius");
            if (baseRadiusUniform != null) {
               baseRadiusUniform.set(baseRadius);
            }

            Uniform topRadiusUniform = shader.getUniform("TopRadius");
            if (topRadiusUniform != null) {
               topRadiusUniform.set(topRadius);
            }

            Uniform heightUniform = shader.getUniform("Height");
            if (heightUniform != null) {
               heightUniform.set(height);
            }

            Uniform dustUniform = shader.getUniform("DustIntensity");
            if (dustUniform != null) {
               dustUniform.set(0.5F);
            }

            Uniform coreUniform = shader.getUniform("CoreTightness");
            if (coreUniform != null) {
               coreUniform.set(0.2F);
            }

            Uniform flowIntensity = shader.getUniform("FlowIntensity");
            if (flowIntensity != null) {
               flowIntensity.set(0.1F);
            }

            Uniform scaleUniform = shader.getUniform("Scale");
            if (scaleUniform != null) {
               float scale = (float)(tornado.getLevel().getBaseDamage() / TornadoLevel.F1.getBaseDamage());
               scaleUniform.set(scale);
            }

            float partialTicks = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
            float sunAngle = level.getTimeOfDay(partialTicks);
            float angle = sunAngle * (float) (Math.PI * 2);
            float xLight = Mth.cos(angle);
            float yLight = Mth.sin(angle);
            float zLight = 0.2F;
            float length = Mth.sqrt(xLight * xLight + yLight * yLight + zLight * zLight);
            xLight /= length;
            yLight /= length;
            zLight /= length;
            Uniform lightX = shader.getUniform("LightDirX");
            Uniform lightY = shader.getUniform("LightDirY");
            Uniform lightZ = shader.getUniform("LightDirZ");
            if (lightX != null) {
               lightX.set(xLight);
            }

            if (lightY != null) {
               lightY.set(yLight);
            }

            if (lightZ != null) {
               lightZ.set(zLight);
            }

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            Tesselator tess = Tesselator.getInstance();
            BufferBuilder buffer = tess.begin(Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX);
            VertexConsumer consumer = buffer;
            float windSpeed = tornado.wind.gustSpeed();
            float windAngleDeg = tornado.wind.angleRadians();
            float windAngleRad = (float)Math.toRadians((double)windAngleDeg);
            double windX = Math.cos((double)windAngleRad) * (double)windSpeed;
            double windZ = Math.sin((double)windAngleRad) * (double)windSpeed;
            Vec3 horizontalWind = new Vec3(windX, 5.0, windZ);
            float spawnProgress = Mth.clamp(tornado.getLifetimeSeconds() / 10.0F, 0.0F, 1.0F);
            float cutoffY = height * (1.0F - spawnProgress);
            float time = TornadoManager.getShaderTime();

            for (int i = rings - 1; i >= 0; i--) {
               float y0 = (float)i * (height / (float)rings);
               float y1 = (float)(i + 1) * (height / (float)rings);
               if (y1 < cutoffY) {
                  break;
               }

               if (y0 < cutoffY) {
                  y0 = cutoffY;
               }

               float t0 = y0 / height;
               float t1 = y1 / height;

               for (int j = 0; j < segments; j++) {
                  float u0 = (float)j / (float)segments;
                  float u1 = ((float)j + 1.0F) / (float)segments;
                  float U_EPS = 1.0E-6F;
                  float u0s = j == 0 ? u0 + 1.0E-6F : u0;
                  float u1s = j == segments - 1 ? 0.999999F : u1;
                  float twist = (float) (Math.PI * 7.0 / 2.0);
                  float angleOffset0 = twist * (1.0F - t0);
                  float angleOffset1 = twist * (1.0F - t1);
                  float angle0_0 = (float)((Math.PI * 2) * (double)u0 + (double)angleOffset0);
                  float angle0_1 = (float)((Math.PI * 2) * (double)u1 + (double)angleOffset0);
                  float angle1_0 = (float)((Math.PI * 2) * (double)u0 + (double)angleOffset1);
                  float angle1_1 = (float)((Math.PI * 2) * (double)u1 + (double)angleOffset1);
                  float x00 = tornadoShapeRadius(y0, angle0_0, time) * (float)Math.cos((double)angle0_0);
                  float z00 = tornadoShapeRadius(y0, angle0_0, time) * (float)Math.sin((double)angle0_0);
                  float x01 = tornadoShapeRadius(y0, angle0_1, time) * (float)Math.cos((double)angle0_1);
                  float z01 = tornadoShapeRadius(y0, angle0_1, time) * (float)Math.sin((double)angle0_1);
                  float x10 = tornadoShapeRadius(y1, angle1_0, time) * (float)Math.cos((double)angle1_0);
                  float z10 = tornadoShapeRadius(y1, angle1_0, time) * (float)Math.sin((double)angle1_0);
                  float x11 = tornadoShapeRadius(y1, angle1_1, time) * (float)Math.cos((double)angle1_1);
                  float z11 = tornadoShapeRadius(y1, angle1_1, time) * (float)Math.sin((double)angle1_1);
                  float wiggleFreq = 5.0F;
                  float wiggleAmp = 0.5F;
                  x00 = (float)((double)x00 + Math.sin((double)(y0 * 0.1F + angle0_0 * wiggleFreq)) * (double)wiggleAmp);
                  z00 = (float)((double)z00 + Math.cos((double)(y0 * 0.1F + angle0_0 * wiggleFreq)) * (double)wiggleAmp);
                  x01 = (float)((double)x01 + Math.sin((double)(y0 * 0.1F + angle0_1 * wiggleFreq)) * (double)wiggleAmp);
                  z01 = (float)((double)z01 + Math.cos((double)(y0 * 0.1F + angle0_1 * wiggleFreq)) * (double)wiggleAmp);
                  x10 = (float)((double)x10 + Math.sin((double)(y1 * 0.1F + angle1_0 * wiggleFreq)) * (double)wiggleAmp);
                  z10 = (float)((double)z10 + Math.cos((double)(y1 * 0.1F + angle1_0 * wiggleFreq)) * (double)wiggleAmp);
                  x11 = (float)((double)x11 + Math.sin((double)(y1 * 0.1F + angle1_1 * wiggleFreq)) * (double)wiggleAmp);
                  z11 = (float)((double)z11 + Math.cos((double)(y1 * 0.1F + angle1_1 * wiggleFreq)) * (double)wiggleAmp);
                  float epsilon = 1.0E-4F;
                  float v0 = (y0 + epsilon) / height;
                  float v1 = (y1 - epsilon) / height;
                  float bendScale = 1.5F;
                  float bendFactor0 = y0 / height * bendScale * windSpeed;
                  float bendFactor1 = y1 / height * bendScale * windSpeed;
                  float offsetX0 = (float)horizontalWind.x * bendFactor0;
                  float offsetZ0 = (float)horizontalWind.z * bendFactor0;
                  float offsetX1 = (float)horizontalWind.x * bendFactor1;
                  float offsetZ1 = (float)horizontalWind.z * bendFactor1;
                  x00 += offsetX0;
                  z00 += offsetZ0;
                  x01 += offsetX0;
                  z01 += offsetZ0;
                  x10 += offsetX1;
                  z10 += offsetZ1;
                  x11 += offsetX1;
                  z11 += offsetZ1;
                  consumer.addVertex(matrix, x00, y0, z00).setUv(u0s, v0);
                  consumer.addVertex(matrix, x10, y1, z10).setUv(u0s, v1);
                  consumer.addVertex(matrix, x11, y1, z11).setUv(u1s, v1);
                  consumer.addVertex(matrix, x00, y0, z00).setUv(u0s, v0);
                  consumer.addVertex(matrix, x11, y1, z11).setUv(u1s, v1);
                  consumer.addVertex(matrix, x01, y0, z01).setUv(u1s, v0);
               }
            }

            int bowlRings = 24;
            float bowlHeight = 12.0F;
            topRadius -= 3.0F;
            float angleDeg = 18.0F;
            float targetTopR = topRadius * 1.6F;
            float factor = 1.6F;
            float p = 1.0F;
            float flareSlope = 0.3F;
            float maxBowlRadius = topRadius + flareSlope * bowlHeight;

            for (int i = 0; i < bowlRings; i++) {
               float t0 = (float)i / (float)bowlRings;
               float t1 = ((float)i + 1.0F) / (float)bowlRings;
               float y0x = height + t0 * bowlHeight;
               float y1x = height + t1 * bowlHeight;
               float r0 = coneRadiusByAngle(y0x, height, topRadius, bowlHeight, angleDeg, p);
               float r1 = coneRadiusByAngle(y1x, height, topRadius, bowlHeight, angleDeg, p);
               float twist = (float) (Math.PI * 7.0 / 2.0);
               float aOff0 = twist * (1.0F - Math.min(1.0F, y0x / height));
               float aOff1 = twist * (1.0F - Math.min(1.0F, y1x / height));

               for (int j = 0; j < segments; j++) {
                  float u0 = (float)j / (float)segments;
                  float u1 = ((float)j + 1.0F) / (float)segments;
                  float a00 = (float)((Math.PI * 2) * (double)u0 + (double)aOff0);
                  float a01 = (float)((Math.PI * 2) * (double)u1 + (double)aOff0);
                  float a10 = (float)((Math.PI * 2) * (double)u0 + (double)aOff1);
                  float a11 = (float)((Math.PI * 2) * (double)u1 + (double)aOff1);
                  float x00 = r0 * (float)Math.cos((double)a00);
                  float z00 = r0 * (float)Math.sin((double)a00);
                  float x01 = r0 * (float)Math.cos((double)a01);
                  float z01 = r0 * (float)Math.sin((double)a01);
                  float x10 = r1 * (float)Math.cos((double)a10);
                  float z10 = r1 * (float)Math.sin((double)a10);
                  float x11 = r1 * (float)Math.cos((double)a11);
                  float z11 = r1 * (float)Math.sin((double)a11);
                  float b0 = y0x / (height + bowlHeight) * 1.5F * windSpeed;
                  float b1 = y1x / (height + bowlHeight) * 1.5F * windSpeed;
                  x00 += (float)horizontalWind.x * b0;
                  z00 += (float)horizontalWind.z * b0;
                  x01 += (float)horizontalWind.x * b0;
                  z01 += (float)horizontalWind.z * b0;
                  x10 += (float)horizontalWind.x * b1;
                  z10 += (float)horizontalWind.z * b1;
                  x11 += (float)horizontalWind.x * b1;
                  z11 += (float)horizontalWind.z * b1;
                  float epsilon = 1.0E-4F;
                  float v0 = Math.min(1.0F - epsilon, y0x / height);
                  float v1 = Math.min(1.0F - epsilon, y1x / height);
                  consumer.addVertex(matrix, x00, y0x, z00).setUv(u0, v0);
                  consumer.addVertex(matrix, x10, y1x, z10).setUv(u0, v1);
                  consumer.addVertex(matrix, x11, y1x, z11).setUv(u1, v1);
                  consumer.addVertex(matrix, x00, y0x, z00).setUv(u0, v0);
                  consumer.addVertex(matrix, x11, y1x, z11).setUv(u1, v1);
                  consumer.addVertex(matrix, x01, y0x, z01).setUv(u1, v0);
               }
            }

            MeshData mesh = buffer.build();
            BufferUploader.drawWithShader(mesh);
         } finally {
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            stack.popPose();
         }
      }
   }

   private static float tornadoShapeRadius(float y, float angle, float time) {
      float yAdj = y + 45.0F;
      float zcurve = (float)Math.pow((double)yAdj, 1.5) * 0.03F;
      float base = zcurve + 5.5F;
      float scale = Mth.clamp(zcurve * 0.2F, 0.1F, 1.0F);
      float radius = base + scale * Mth.sin(time - Mth.sqrt(yAdj) + angle) * 5.0F;
      float ridgedNoise = 1.0F - 2.0F * Math.abs(Mth.sin(time * 1.5F + 0.1F * yAdj + angle));
      return radius - ridgedNoise * 1.2F;
   }

   public static void spawnDebrisParticles(TornadoInstance tornado, ClientLevel level) {
      for (int i = 0; i < 10; i++) {
         double maxRadius = 16.0;
         double radius = Math.sqrt(level.random.nextDouble()) * maxRadius;
         double height = level.random.nextDouble() * (double)((Integer)SimpleCloudsConfig.SERVER.cloudHeight.get()).intValue();
         float angularSpeed = 4.0F;
         level.addParticle(
            new DebrisParticleData(tornado, radius, height, angularSpeed), tornado.position.x, tornado.position.y, tornado.position.z, 0.0, 0.01, 0.0
         );
      }
   }

   static float coneRadiusByAngle(float y, float seamY, float topRadius, float bowlHeight, float angleDeg, float p) {
      float t = Mth.clamp((y - seamY) / bowlHeight, 0.0F, 1.0F);
      float slope = (float)Math.tan(Math.toRadians((double)angleDeg));
      float targetR = topRadius + slope * bowlHeight;
      float linear = topRadius + (targetR - topRadius) * t;
      return p == 1.0F ? linear : topRadius + (targetR - topRadius) * (float)Math.pow((double)t, (double)p);
   }
}
