package com.weraisedsirenhead.entity;

import com.weraisedsirenhead.SirenHeadMod;
import com.weraisedsirenhead.registry.ModEntities;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Registro de atributos de la entidad (bus del mod). */
@Mod.EventBusSubscriber(modid = SirenHeadMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModEntityEvents {

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.SIREN_HEAD.get(), SirenHeadEntity.createAttributes().build());
    }

    private ModEntityEvents() {
    }
}
