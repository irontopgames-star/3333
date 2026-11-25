package com.weraisedsirenhead.client;

import com.weraisedsirenhead.SirenHeadMod;
import com.weraisedsirenhead.registry.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Registros exclusivos del cliente (renderer y HUD). */
@Mod.EventBusSubscriber(modid = SirenHeadMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SIREN_HEAD.get(), SirenHeadRenderer::new);
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("story", new StoryHud());
    }

    private ClientModEvents() {
    }
}
