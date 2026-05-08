package dev.yeyito.tenpackdeath.voice;

import de.maxhenkel.corpse.entities.CorpseEntity;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.NameTagIconRenderEvent;
import net.minecraft.world.entity.Entity;

@ForgeVoicechatPlugin
public class TenpackVoicechatPlugin implements VoicechatPlugin {
    @Override
    public String getPluginId() {
        return "tenpackdeath";
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(NameTagIconRenderEvent.class, this::onNameTagIconRender);
    }

    private void onNameTagIconRender(NameTagIconRenderEvent event) {
        Iterable<?> renderEntities = getClientRenderEntities();
        if (renderEntities == null) {
            return;
        }
        for (Object object : renderEntities) {
            if (!(object instanceof Entity entity)) {
                continue;
            }
            if (!(entity instanceof CorpseEntity corpse)) {
                continue;
            }
            if (corpse.getUUID().equals(event.getEntityId())
                    || corpse.getCorpseUUID().map(event.getEntityId()::equals).orElse(false)) {
                event.cancel();
                return;
            }
        }
    }

    /**
     * Simple Voice Chat loads plugins on the common/server side too. Use reflection
     * here so this plugin class does not hard-link dedicated servers to client-only
     * Minecraft classes just to fix a client render icon.
     */
    private static Iterable<?> getClientRenderEntities() {
        try {
            Class<?> minecraftClass = Class.forName("net.minecraft.client.Minecraft");
            Object minecraft = minecraftClass.getMethod("getInstance").invoke(null);
            Object level = minecraftClass.getField("level").get(minecraft);
            if (level == null) {
                return null;
            }
            Object entities = level.getClass().getMethod("entitiesForRendering").invoke(level);
            if (entities instanceof Iterable<?> iterable) {
                return iterable;
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }
        return null;
    }
}
