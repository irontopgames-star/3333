package com.weraisedsirenhead.story.script;

import com.weraisedsirenhead.director.events.RadioEvent;
import com.weraisedsirenhead.entity.SirenHeadEntity;
import com.weraisedsirenhead.entity.SirenState;
import com.weraisedsirenhead.registry.ModSounds;
import com.weraisedsirenhead.sound.SirenSounds;
import com.weraisedsirenhead.story.PlayerStory;
import com.weraisedsirenhead.story.StoryContext;
import com.weraisedsirenhead.util.Shelter;

/**
 * DIA 1 - Encontrar y criar.
 * M1 Encontrar a Siren Head (acercarte; mientras tanto emite chirridos para guiarte).
 * M2 Alimentarlo (clic derecho con comida, ver SirenHeadEntity#feed).
 * M3 Construirle un refugio (la criatura esta dentro de un recinto cerrado).
 */
public class Day1Script implements DayScript {

    @Override
    public void tick(StoryContext c) {
        SirenHeadEntity creature = c.creature;
        PlayerStory story = c.story;
        if (creature == null) {
            return;
        }
        if (!story.isDone(0)) {
            if (c.player.distanceToSqr(creature) < 36.0D) {
                c.complete(0);
            } else if (c.runtime.beat % 16 == 0) {
                SirenSounds.at(c.level, creature.position(), ModSounds.BABY_CHIRP.get(), 2.5F, 1.0F);
            }
        }
        // Primera noche: mientras duerme a tu lado, sus parlantes susurran tu nombre.
        if (!story.whisperDone && c.timeOfDay >= 17500L && c.timeOfDay < 22000L
                && creature.getSirenState() == SirenState.SLEEPING && c.player.distanceTo(creature) < 20.0F) {
            story.whisperDone = true;
            RadioEvent.send(c, 1, 1 + c.random.nextInt(2));
        }
        if (!story.isDone(2) && story.isDone(0) && c.runtime.beat % 2 == 0
                && Shelter.isEnclosed(c.level, creature.blockPosition())) {
            c.complete(2);
        }
    }
}
