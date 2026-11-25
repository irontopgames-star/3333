package com.weraisedsirenhead.director.events;

import com.weraisedsirenhead.director.HorrorEvent;
import com.weraisedsirenhead.entity.CreatureActions;
import com.weraisedsirenhead.entity.SirenHeadEntity;
import com.weraisedsirenhead.entity.SirenState;
import com.weraisedsirenhead.story.StoryContext;
import com.weraisedsirenhead.world.SpawnSpots;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Dia 3+, de noche: aparece a ~55 bloques de la casa y se acerca de a poco, congelandose cuando la miras.
 * Al llegar a unos 15 bloques se queda observando un momento y se esfuma.
 */
public class NightApproachEvent extends HorrorEvent {
    private boolean failed;
    private boolean arrived;

    @Override
    public int weight(StoryContext c) {
        SirenHeadEntity creature = c.creature;
        if (creature == null || c.story.transformed || creature.getOverrideTicks() > 200 || !c.isNight()
                || (c.story.vanishStarted && !c.story.vanishFound)) {
            return 0;
        }
        return c.day() == 3 ? 3 : c.day() >= 4 ? 1 : 0;
    }

    @Override
    public void start(StoryContext c) {
        BlockPos spot = SpawnSpots.find(c.level, c.home(), 50.0D, 62.0D, 60, c.random, null);
        if (c.creature == null || spot == null) {
            failed = true;
            return;
        }
        CreatureActions.teleport(c.creature, Vec3.atBottomCenterOf(spot), c.player);
        c.creature.forceState(SirenState.STALKING, 900);
    }

    @Override
    public boolean tick(StoryContext c) {
        SirenHeadEntity creature = c.creature;
        if (failed || creature == null) {
            return true;
        }
        if (!arrived && c.player.distanceTo(creature) < 16.0D) {
            arrived = true;
            creature.forceState(SirenState.OBSERVING_PLAYER, 100);
            return false;
        }
        if (arrived) {
            return creature.getOverrideTicks() == 0;
        }
        return age >= 95;
    }

    @Override
    public void end(StoryContext c) {
        if (!failed) {
            releaseCreature(c);
        }
    }
}
