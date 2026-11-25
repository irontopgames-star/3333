package com.weraisedsirenhead.director.events;

import com.weraisedsirenhead.director.HorrorEvent;
import com.weraisedsirenhead.entity.CreatureActions;
import com.weraisedsirenhead.entity.SirenHeadEntity;
import com.weraisedsirenhead.entity.SirenState;
import com.weraisedsirenhead.entity.ai.SirenBrain;
import com.weraisedsirenhead.registry.ModSounds;
import com.weraisedsirenhead.sound.SirenSounds;
import com.weraisedsirenhead.story.StoryContext;
import com.weraisedsirenhead.util.Sight;
import com.weraisedsirenhead.world.SpawnSpots;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Dia 4+: engano. Una voz humana pide ayuda desde un punto fuera de la vista; ahi esta escondida la criatura.
 * Si te acercas a menos de 14 bloques se revela y te persigue 18 segundos. Si te quedas lejos, se va sin hacer nada.
 */
public class LureEvent extends HorrorEvent {
    private Vec3 source;
    private boolean failed;
    private boolean revealed;

    @Override
    public int weight(StoryContext c) {
        SirenHeadEntity creature = c.creature;
        if (creature == null || c.day() < 4 || c.story.transformed || creature.getOverrideTicks() > 100
                || c.player.getHealth() <= 8.0F || c.player.isCreative() || c.player.isSpectator()) {
            return 0;
        }
        return 1;
    }

    @Override
    public void start(StoryContext c) {
        BlockPos spot = SpawnSpots.find(c.level, c.player.position(), 22.0D, 34.0D, 60, c.random,
                p -> !Sight.canSee(c.player, Vec3.atBottomCenterOf(p).add(0.0D, 1.0D, 0.0D)));
        if (spot == null) {
            spot = SpawnSpots.find(c.level, c.player.position(), 22.0D, 34.0D, 40, c.random, null);
        }
        if (c.creature == null || spot == null) {
            failed = true;
            return;
        }
        source = Vec3.atBottomCenterOf(spot);
        CreatureActions.teleport(c.creature, source, c.player);
        c.creature.forceState(SirenState.OBSERVING_PLAYER, 400);
    }

    @Override
    public boolean tick(StoryContext c) {
        SirenHeadEntity creature = c.creature;
        if (failed || creature == null) {
            return true;
        }
        if (age % 6 == 1 && age < 20) {
            SirenSounds.at(c.level, source.add(0.0D, 1.5D, 0.0D), ModSounds.VOICE.get(), 3.0F, 0.95F + c.random.nextFloat() * 0.15F);
        }
        if (c.player.distanceToSqr(source) < 14.0D * 14.0D) {
            revealed = true;
            SirenBrain.startChase(creature, c.level, 360);
            return true;
        }
        return age >= 24;
    }

    @Override
    public void end(StoryContext c) {
        if (!failed && !revealed) {
            releaseCreature(c);
        }
    }
}
