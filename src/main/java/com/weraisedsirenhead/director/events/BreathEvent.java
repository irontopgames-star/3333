package com.weraisedsirenhead.director.events;

import com.weraisedsirenhead.director.HorrorEvent;
import com.weraisedsirenhead.registry.ModSounds;
import com.weraisedsirenhead.sound.SirenSounds;
import com.weraisedsirenhead.story.StoryContext;

/** Dia 3+: respiracion pesada pegada a tu espalda. No pasa nada mas. */
public class BreathEvent extends HorrorEvent {

    @Override
    public int weight(StoryContext c) {
        return c.day() >= 3 ? 2 : 0;
    }

    @Override
    public void start(StoryContext c) {
        SirenSounds.behind(c.player, ModSounds.BREATH.get(), 2.0D, 4.0D, 1.2F, 0.9F);
    }

    @Override
    public boolean tick(StoryContext c) {
        if (age == 6 && c.random.nextBoolean()) {
            SirenSounds.behind(c.player, ModSounds.BREATH.get(), 2.0D, 4.0D, 1.0F, 0.8F);
        }
        return age >= 8;
    }
}
