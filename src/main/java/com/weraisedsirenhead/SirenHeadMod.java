package com.weraisedsirenhead;

import com.mojang.logging.LogUtils;
import com.weraisedsirenhead.config.SirenHeadConfig;
import com.weraisedsirenhead.network.ModNetwork;
import com.weraisedsirenhead.registry.ModEntities;
import com.weraisedsirenhead.registry.ModSounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Punto de entrada del mod. Solo conecta registros, configuracion y red;
 * la logica de juego vive en sus propios paquetes (story, quest, entity, director, sound...).
 */
@Mod(SirenHeadMod.MOD_ID)
public class SirenHeadMod {
    public static final String MOD_ID = "weraisedsirenhead";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SirenHeadMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModEntities.register(modBus);
        ModSounds.register(modBus);

        modBus.addListener(this::commonSetup);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SirenHeadConfig.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
        LOGGER.info("[{}] Inicializado (Minecraft 1.20.1 / Forge 47.x)", MOD_ID);
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    public static void debug(String message, Object... args) {
        if (SirenHeadConfig.DEBUG_LOGGING.get()) {
            LOGGER.info("[" + MOD_ID + "] " + message, args);
        }
    }
}
