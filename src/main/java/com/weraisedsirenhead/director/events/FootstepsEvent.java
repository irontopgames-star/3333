package com.weraisedsirenhead.director.events;

import com.weraisedsirenhead.director.HorrorEvent;
import com.weraisedsirenhead.registry.ModSounds;
import com.weraisedsirenhead.sound.SirenSounds;
import com.weraisedsirenhead.story.StoryContext;
import net.minecraft.world.phys.Vec3;

/** Dia 3+: pisadas pesadas e invisibles que se acercan desde ~34 hasta ~12 bloques y se detienen de golpe. */
public class FootstepsEvent extends HorrorEvent {
    private static final int STEPS = 9;
    private double angle;

    @Override
    public int weight(StoryContext c) {
        return c.day() >= 3 ? 2 : 0;
    }

    @Override
    public void start(StoryContext c) {
        angle = c.random.nextDouble() * Math.PI * 2.0D;
    }

    @Override
    public boolean tick(StoryContext c) {
        if (age <= STEPS) {
            double progress = (age - 1) / (double) (STEPS - 1);
            double distance = 34.0D + (12.0D - 34.0D) * progress;
            Vec3 pos = new Vec3(c.player.getX() + Math.cos(angle) * distance, c.player.getY(), c.player.getZ() + Math.sin(angle) * distance);
            SirenSounds.at(c.level, pos, ModSounds.STEP.get(), 3.0F, 0.8F + c.random.nextFloat() * 0.15F);
        }
        return age > STEPS;
    }
}
