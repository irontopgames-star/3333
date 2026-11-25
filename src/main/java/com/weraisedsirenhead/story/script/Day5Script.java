package com.weraisedsirenhead.story.script;

import com.weraisedsirenhead.director.events.RadioEvent;
import com.weraisedsirenhead.entity.CreatureActions;
import com.weraisedsirenhead.entity.SirenHeadEntity;
import com.weraisedsirenhead.entity.SirenState;
import com.weraisedsirenhead.registry.ModSounds;
import com.weraisedsirenhead.sound.SirenSounds;
import com.weraisedsirenhead.story.PlayerStory;
import com.weraisedsirenhead.story.StoryContext;
import com.weraisedsirenhead.story.StoryManager;
import com.weraisedsirenhead.util.Sight;
import com.weraisedsirenhead.world.Gore;
import com.weraisedsirenhead.world.SpawnSpots;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;

/**
 * DIA 5 - Transformacion final.
 * Al caer la tarde la criatura aparece delante tuyo y se transforma (7 segundos de convulsiones,
 * estatica y rayos). Despues, adulta, te busca. Aparece el cartel "Sobrevive hasta el amanecer" y
 * la noche pasa mas rapido (config finalNightSpeed). No se puede dormir.
 * M1 Sobrevivir a la transformacion.  M2 Aguantar hasta la madrugada.
 * M3 Escapar de Siren Head: llegar vivo al amanecer (lo completa StoryManager#victory).
 */
public class Day5Script implements DayScript {
    private static final int TRANSFORM_TICKS = 140;

    @Override
    public void tick(StoryContext c) {
        PlayerStory story = c.story;
        SirenHeadEntity creature = c.creature;

        // Manana del dia 5: ya no es una mascota. Ofrendas enormes alrededor de la casa y un altar a la distancia.
        if (!story.offeringDone && c.timeOfDay >= 500L && c.timeOfDay < 9000L
                && c.player.position().distanceTo(c.home()) < 30.0D) {
            story.offeringDone = true;
            Vec3 home = c.home();
            Gore.ring(c.level, home, 12, 8, c.random);
            double angle = c.random.nextDouble() * Math.PI * 2.0D;
            Gore.altar(c.level, BlockPos.containing(home.x + Math.cos(angle) * 20.0D, home.y, home.z + Math.sin(angle) * 20.0D), c.random);
            RadioEvent.send(c, 5, 1 + c.random.nextInt(3));
        }

        if (!story.transformStarted) {
            if (creature != null && c.timeOfDay >= 11000L && c.timeOfDay < 22000L) {
                begin(c, creature);
            }
            return;
        }
        if (!story.transformed) {
            tickTransformation(c, creature);
            return;
        }
        if (c.timeOfDay >= 18000L) {
            c.complete(1);
        }
    }

    private void begin(StoryContext c, SirenHeadEntity creature) {
        BlockPos spot = SpawnSpots.find(c.level, c.player.position(), 16.0D, 24.0D, 60, c.random,
                p -> Sight.canSee(c.player, Vec3.atBottomCenterOf(p).add(0.0D, 3.0D, 0.0D)));
        if (spot == null) {
            spot = SpawnSpots.find(c.level, c.player.position(), 16.0D, 24.0D, 60, c.random, null);
        }
        if (spot == null) {
            return;
        }
        c.story.transformStarted = true;
        CreatureActions.teleport(creature, Vec3.atBottomCenterOf(spot), c.player);
        creature.beginTransformation(TRANSFORM_TICKS);
        SirenSounds.at(c.level, creature.position(), ModSounds.TRANSFORM.get(), 6.0F, 1.0F);
        StoryManager.showTitle(c.player, Component.translatable("title.weraisedsirenhead.transform"),
                Component.translatable("subtitle.weraisedsirenhead.transform"));
    }

    private void tickTransformation(StoryContext c, SirenHeadEntity creature) {
        int left = creature == null ? 0 : creature.getTransformTicks();
        if (creature != null && left > 0) {
            Vec3 pos = creature.position();
            c.level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y + 3.0D, pos.z, 14, 1.5D, 3.0D, 1.5D, 0.4D);
            if (c.runtime.beat % 2 == 0) {
                SirenSounds.at(c.level, pos, c.random.nextBoolean() ? ModSounds.STATIC.get() : ModSounds.INTERFERENCE.get(), 3.0F, 0.7F);
            }
            if (left <= 90 && !c.runtime.bolt1) {
                c.runtime.bolt1 = true;
                lightning(c, pos);
            }
            if (left <= 40 && !c.runtime.bolt2) {
                c.runtime.bolt2 = true;
                lightning(c, pos);
            }
            return;
        }
        finish(c, creature);
    }

    private void finish(StoryContext c, SirenHeadEntity creature) {
        c.story.transformed = true;
        if (creature != null) {
            creature.setStage(5);
            creature.setChaseCooldown(300);
            creature.forceState(SirenState.STALKING, 200);
            Vec3 pos = creature.position();
            c.level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y + 4.0D, pos.z, 3, 1.5D, 2.0D, 1.5D, 0.0D);
            SirenSounds.at(c.level, pos, ModSounds.AIR_RAID.get(), 6.0F, 0.9F);
            lightning(c, pos);
        }
        StoryManager.showTitle(c.player, Component.translatable("title.weraisedsirenhead.dawn"),
                Component.translatable("subtitle.weraisedsirenhead.dawn"));
        c.complete(0);
    }

    private static void lightning(StoryContext c, Vec3 pos) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(c.level);
        if (bolt != null) {
            bolt.moveTo(pos);
            bolt.setVisualOnly(true);
            c.level.addFreshEntity(bolt);
        }
    }
}
