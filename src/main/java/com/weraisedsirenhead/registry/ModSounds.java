package com.weraisedsirenhead.registry;

import com.weraisedsirenhead.SirenHeadMod;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Eventos de sonido. Los .ogg y sounds.json estan en assets/weraisedsirenhead. */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, SirenHeadMod.MOD_ID);

    public static final RegistryObject<SoundEvent> BABY_CHIRP = register("baby_chirp");
    public static final RegistryObject<SoundEvent> STATIC = register("static");
    public static final RegistryObject<SoundEvent> INTERFERENCE = register("interference");
    public static final RegistryObject<SoundEvent> ALARM = register("alarm");
    public static final RegistryObject<SoundEvent> AIR_RAID = register("air_raid");
    public static final RegistryObject<SoundEvent> VOICE = register("voice");
    public static final RegistryObject<SoundEvent> STEP = register("step");
    public static final RegistryObject<SoundEvent> KNOCK = register("knock");
    public static final RegistryObject<SoundEvent> WINDOW_BANG = register("window_bang");
    public static final RegistryObject<SoundEvent> BREATH = register("breath");
    public static final RegistryObject<SoundEvent> TRANSFORM = register("transform");
    public static final RegistryObject<SoundEvent> FEAST = register("feast");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(SirenHeadMod.id(name)));
    }

    public static void register(IEventBus modBus) {
        SOUND_EVENTS.register(modBus);
    }

    private ModSounds() {
    }
}
