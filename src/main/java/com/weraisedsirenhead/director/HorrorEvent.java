package com.weraisedsirenhead.director;

import com.weraisedsirenhead.entity.CreatureActions;
import com.weraisedsirenhead.entity.SirenHeadEntity;
import com.weraisedsirenhead.entity.SirenState;
import com.weraisedsirenhead.registry.ModSounds;
import com.weraisedsirenhead.sound.SirenSounds;
import com.weraisedsirenhead.story.StoryContext;

/**
 * Un evento de terror. El director crea una instancia nueva cada vez, asi que puede guardar estado propio.
 * El director llama a tick() cada 10 ticks (age cuenta esas llamadas) hasta que devuelve true.
 */
public abstract class HorrorEvent {
    /** Cantidad de tick() ya ejecutados (lo incrementa el director). */
    public int age;

    /** Peso para el sorteo (0 = no puede ocurrir ahora). */
    public abstract int weight(StoryContext c);

    public abstract void start(StoryContext c);

    /** @return true cuando el evento termino. */
    public abstract boolean tick(StoryContext c);

    /** Se llama al terminar (por defecto no hace nada). */
    public void end(StoryContext c) {
    }

    /** Despues de un evento con la criatura: se esfuma y vuelve a su rutina (cerca el dia 1-2, lejos desde el 3). */
    protected static void releaseCreature(StoryContext c) {
        SirenHeadEntity creature = c.creature;
        if (creature == null) {
            return;
        }
        SirenSounds.at(c.level, creature.position(), ModSounds.STATIC.get(), 2.0F, 1.3F);
        creature.clearOverride();
        if (c.day() <= 2) {
            CreatureActions.teleportNear(creature, c.player, 7.0D, 10.0D);
            creature.setSirenState(SirenState.FOLLOWING_PLAYER);
        } else {
            CreatureActions.teleportNear(creature, c.player, 26.0D, 36.0D);
            creature.setSirenState(SirenState.IDLE);
        }
    }
}
