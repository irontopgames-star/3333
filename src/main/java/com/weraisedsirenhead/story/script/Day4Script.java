package com.weraisedsirenhead.story.script;

import com.weraisedsirenhead.director.HorrorDirector;
import com.weraisedsirenhead.director.events.KnockEvent;
import com.weraisedsirenhead.director.events.RadioEvent;
import com.weraisedsirenhead.entity.CreatureActions;
import com.weraisedsirenhead.entity.SirenHeadEntity;
import com.weraisedsirenhead.entity.SirenState;
import com.weraisedsirenhead.entity.ai.SirenBrain;
import com.weraisedsirenhead.registry.ModSounds;
import com.weraisedsirenhead.sound.SirenSounds;
import com.weraisedsirenhead.story.PlayerStory;
import com.weraisedsirenhead.story.StoryContext;
import com.weraisedsirenhead.util.Shelter;
import com.weraisedsirenhead.world.Gore;
import com.weraisedsirenhead.world.SpawnSpots;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * DIA 4 - Crecimiento y acecho.
 * Al anochecer aparece a unos 35 bloques y te persigue 45 segundos (con aviso sonoro y sin matarte).
 * Despues golpea puertas y ventanas alrededor de tu casa.
 * M1 Sobrevivir a la primera persecucion.  M2 Investigar los sonidos alrededor de la casa.
 * M3 Preparar un refugio seguro (estar dentro de un recinto cerrado).
 */
public class Day4Script implements DayScript {
    private static final int CHASE_TICKS = 900;

    @Override
    public void tick(StoryContext c) {
        PlayerStory story = c.story;
        SirenHeadEntity creature = c.creature;

        // Amanecer del dia 4: la casa esta rodeada de ofrendas (seis cadaveres con huesos y antorchas de almas).
        if (!story.offeringDone && c.timeOfDay >= 1500L && c.timeOfDay < 9000L
                && c.player.position().distanceTo(c.home()) < 30.0D) {
            story.offeringDone = true;
            Gore.ring(c.level, c.home(), 9, 6, c.random);
            SirenSounds.around(c.player, ModSounds.STATIC.get(), 4.0D, 8.0D, 2.0F, 0.7F);
            RadioEvent.send(c, 4, 1 + c.random.nextInt(4));
        }

        if (!story.chaseStarted && creature != null && c.timeOfDay >= 13500L && c.timeOfDay < 22500L
                && !c.player.isCreative() && !c.player.isSpectator() && !c.player.isSleeping()) {
            BlockPos spot = SpawnSpots.find(c.level, c.player.position(), 32.0D, 44.0D, 50, c.random, null);
            if (spot != null) {
                story.chaseStarted = true;
                story.chaseEnd = c.gameTime + CHASE_TICKS;
                CreatureActions.teleport(creature, Vec3.atBottomCenterOf(spot), c.player);
                SirenBrain.startChase(creature, c.level, CHASE_TICKS);
            }
        }

        if (story.chaseStarted && !story.chaseDone && c.gameTime >= story.chaseEnd) {
            story.chaseDone = true;
            c.complete(0);
            if (creature != null) {
                creature.forceState(SirenState.HIDING, 300);
            }
        }

        if (story.chaseDone && !story.knocksStarted && c.gameTime >= story.chaseEnd + 1200L && c.isNight()
                && c.runtime.active == null && creature != null) {
            story.knocksStarted = true;
            HorrorDirector.forceStart(c, new KnockEvent());
        }

        if (story.lastKnockPos != null && !story.isDone(1) && c.gameTime - story.lastKnockTick < 1200L
                && c.player.distanceToSqr(Vec3.atCenterOf(story.lastKnockPos)) < 64.0D) {
            c.complete(1);
        }

        if (!story.isDone(2) && c.runtime.beat % 2 == 0 && Shelter.isEnclosed(c.level, c.player.blockPosition())) {
            c.complete(2);
        }
    }
}
