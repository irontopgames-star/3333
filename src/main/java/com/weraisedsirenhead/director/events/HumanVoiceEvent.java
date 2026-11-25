package com.weraisedsirenhead.director.events;

import com.weraisedsirenhead.director.HorrorEvent;
import com.weraisedsirenhead.registry.ModSounds;
import com.weraisedsirenhead.sound.SirenSounds;
import com.weraisedsirenhead.story.StoryContext;
import net.minecraft.world.phys.Vec3;

/** Dia 3+: voz humana distorsionada pidiendo ayuda. Desde el dia 4 suena a tu espalda. */
public class HumanVoiceEvent extends HorrorEvent {
    private Vec3 source;
    private int total;
    private int played;

    @Override
    public int weight(StoryContext c) {
        return c.day() >= 4 ? 3 : c.day() >= 3 ? 2 : 0;
    }

    @Override
    public void start(StoryContext c) {
        source = c.day() >= 4 ? SirenSounds.behindPoint(c.player, 14.0D, 24.0D) : SirenSounds.randomPoint(c.player, 16.0D, 32.0D);
        total = 1 + c.random.nextInt(3);
    }

    @Override
    public boolean tick(StoryContext c) {
        if (age % 6 == 1 && played < total) {
            SirenSounds.at(c.level, source, ModSounds.VOICE.get(), 2.5F, 0.85F + c.random.nextFloat() * 0.3F);
            played++;
        }
        return played >= total && age >= 4;
    }
}
