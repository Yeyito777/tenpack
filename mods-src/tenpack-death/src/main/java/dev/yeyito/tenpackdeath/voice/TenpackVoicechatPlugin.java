package dev.yeyito.tenpackdeath.voice;

import de.maxhenkel.corpse.entities.CorpseEntity;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.NameTagIconRenderEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
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
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof CorpseEntity && entity.getUUID().equals(event.getEntityId())) {
                event.cancel();
                return;
            }
        }
    }
}
