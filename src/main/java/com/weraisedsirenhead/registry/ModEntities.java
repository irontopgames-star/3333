package com.weraisedsirenhead.registry;

import com.weraisedsirenhead.SirenHeadMod;
import com.weraisedsirenhead.entity.SirenHeadEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Tipos de entidad. El tamano real depende de la etapa (ver SirenHeadEntity#getDimensions). */
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, SirenHeadMod.MOD_ID);

    public static final RegistryObject<EntityType<SirenHeadEntity>> SIREN_HEAD = ENTITIES.register("siren_head",
            () -> EntityType.Builder.<SirenHeadEntity>of(SirenHeadEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.4F)
                    .fireImmune()
                    .clientTrackingRange(16)
                    .updateInterval(2)
                    .build(SirenHeadMod.MOD_ID + ":siren_head"));

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
    }

    private ModEntities() {
    }
}
