package com.weraisedsirenhead.director.events;

import com.weraisedsirenhead.director.HorrorEvent;
import com.weraisedsirenhead.entity.CreatureActions;
import com.weraisedsirenhead.entity.SirenHeadEntity;
import com.weraisedsirenhead.entity.SirenState;
import com.weraisedsirenhead.entity.ai.SirenBrain;
import com.weraisedsirenhead.story.StoryContext;
import com.weraisedsirenhead.util.Sight;
import com.weraisedsirenhead.world.SpawnSpots;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/** Dia 4 (despues de la primera persecucion): una persecucion corta, solo si estas sano. Siempre con aviso de sirena. */
public class ChaseEvent extends HorrorEvent {
    private boolean failed;

    @Override
    public int weight(StoryContext c) {
        SirenHeadEntity creature = c.creature;
        if (creature == null || c.day() != 4 || !c.story.chaseDone || creature.getOverrideTicks() > 100
                || c.player.getHealth() <= 8.0F || c.player.isCreative() || c.player.isSpectator()) {
            return 0;
        }
        return 1;
    }

    @Override
    public void start(StoryContext c) {
        SirenHeadEntity creature = c.creature;
        double top = creature == null ? 3.0D : creature.getGrowthStage().visualHeight * 0.8D;
        BlockPos spot = SpawnSpots.find(c.level, c.player.position(), 30.0D, 42.0D, 60, c.random,
                p -> Sight.canSee(c.player, Vec3.atBottomCenterOf(p).add(0.0D, top, 0.0D)));
        if (creature == null || spot == null) {
            failed = true;
            return;
        }
        CreatureActions.teleport(creature, Vec3.atBottomCenterOf(spot), c.player);
        SirenBrain.startChase(creature, c.level, 450);
    }

    @Override
    public boolean tick(StoryContext c) {
        SirenHeadEntity creature = c.creature;
        if (failed || creature == null) {
            return true;
        }
        return age >= 2 && creature.getSirenState() != SirenState.CHASING && creature.getSirenState() != SirenState.ATTACKING;
    }
}
