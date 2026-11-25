package com.weraisedsirenhead.director.events;

import com.weraisedsirenhead.director.HorrorEvent;
import com.weraisedsirenhead.registry.ModSounds;
import com.weraisedsirenhead.sound.SirenSounds;
import com.weraisedsirenhead.story.StoryContext;

/** Dia 2+: alarma de emergencia lejana; desde el dia 4, sirena de ataque aereo (se oye a ~96 bloques). */
public class AlarmEvent extends HorrorEvent {

    @Override
    public int weight(StoryContext c) {
        return c.day() >= 4 ? 2 : c.day() >= 2 ? 1 : 0;
    }

    @Override
    public void start(StoryContext c) {
        if (c.day() >= 4 && c.random.nextBoolean()) {
            SirenSounds.around(c.player, ModSounds.AIR_RAID.get(), 30.0D, 50.0D, 6.0F, 1.0F);
        } else {
            SirenSounds.around(c.player, ModSounds.ALARM.get(), 30.0D, 50.0D, 4.0F, 0.95F);
        }
    }

    @Override
    public boolean tick(StoryContext c) {
        return age >= 12;
    }
}
