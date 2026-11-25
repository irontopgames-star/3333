package com.weraisedsirenhead.director.events;

import com.weraisedsirenhead.director.HorrorEvent;
import com.weraisedsirenhead.sound.SirenSounds;
import com.weraisedsirenhead.story.StoryContext;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

/**
 * Dia 2+: sonidos de animales distorsionados (pitch bajo) desde un punto lejano.
 * Desde el dia 3 tambien imita a los aldeanos.
 */
public class AnimalMimicEvent extends HorrorEvent {
    private Vec3 source;
    private int plays;

    @Override
    public int weight(StoryContext c) {
        return c.day() >= 2 ? 2 : 0;
    }

    @Override
    public void start(StoryContext c) {
        source = SirenSounds.randomPoint(c.player, 18.0D, 38.0D);
    }

    @Override
    public boolean tick(StoryContext c) {
        if (age % 4 == 1 && plays < 3) {
            SoundEvent[] pool = c.day() >= 3 && c.random.nextBoolean()
                    ? new SoundEvent[]{SoundEvents.VILLAGER_AMBIENT, SoundEvents.VILLAGER_NO, SoundEvents.VILLAGER_HURT, SoundEvents.VILLAGER_YES}
                    : new SoundEvent[]{SoundEvents.COW_AMBIENT, SoundEvents.PIG_AMBIENT, SoundEvents.SHEEP_AMBIENT, SoundEvents.CHICKEN_AMBIENT};
            SoundEvent sound = pool[c.random.nextInt(pool.length)];
            SirenSounds.at(c.level, source, sound, 2.0F, 0.5F + c.random.nextFloat() * 0.25F);
            plays++;
        }
        return plays >= 3 && age >= 10;
    }
}
