package com.weraisedsirenhead.director.events;

import com.weraisedsirenhead.director.HorrorEvent;
import com.weraisedsirenhead.registry.ModSounds;
import com.weraisedsirenhead.sound.SirenSounds;
import com.weraisedsirenhead.story.StoryContext;
import net.minecraft.sounds.SoundEvent;

/** Dia 2+: rafagas de estatica o interferencia de radio desde algun lugar cercano. */
public class StaticBurstEvent extends HorrorEvent {

    @Override
    public int weight(StoryContext c) {
        return c.day() >= 2 ? 3 : 0;
    }

    @Override
    public void start(StoryContext c) {
        play(c);
    }

    private void play(StoryContext c) {
        SoundEvent sound = c.random.nextBoolean() ? ModSounds.STATIC.get() : ModSounds.INTERFERENCE.get();
        float volume = c.day() >= 4 ? 3.0F : 2.0F;
        SirenSounds.around(c.player, sound, 8.0D, 26.0D, volume, 0.85F + c.random.nextFloat() * 0.3F);
    }

    @Override
    public boolean tick(StoryContext c) {
        if (age == 3 && c.random.nextBoolean()) {
            play(c);
        }
        return age >= 4;
    }
}
