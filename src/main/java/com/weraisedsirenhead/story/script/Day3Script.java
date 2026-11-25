package com.weraisedsirenhead.story.script;

import com.weraisedsirenhead.director.HorrorDirector;
import com.weraisedsirenhead.director.events.FeastEvent;
import com.weraisedsirenhead.director.events.GiftEvent;
import com.weraisedsirenhead.entity.CreatureActions;
import com.weraisedsirenhead.entity.SirenHeadEntity;
import com.weraisedsirenhead.entity.SirenState;
import com.weraisedsirenhead.registry.ModSounds;
import com.weraisedsirenhead.sound.SirenSounds;
import com.weraisedsirenhead.story.PlayerStory;
import com.weraisedsirenhead.story.StoryContext;
import com.weraisedsirenhead.world.Gore;
import com.weraisedsirenhead.world.SpawnSpots;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * DIA 3 - Comportamiento paranormal.
 * Por la manana la criatura desaparece y deja un rastro de huellas (particulas) hasta donde se esconde.
 * A media tarde empiezan voces humanas pidiendo ayuda desde un punto del bosque.
 * M1 Encontrarla despues de que desaparezca.  M2 Investigar las voces.  M3 Seguir sus huellas.
 */
public class Day3Script implements DayScript {

    @Override
    public void tick(StoryContext c) {
        PlayerStory story = c.story;
        SirenHeadEntity creature = c.creature;

        // --- desaparicion
        if (!story.vanishStarted && creature != null && c.timeOfDay >= 3000L && c.timeOfDay < 11000L) {
            BlockPos hide = SpawnSpots.find(c.level, c.player.position(), 48.0D, 64.0D, 60, c.random, null);
            if (hide != null) {
                story.vanishStarted = true;
                story.trailStart = creature.blockPosition();
                story.trailEnd = hide;
                SirenSounds.at(c.level, creature.position(), ModSounds.STATIC.get(), 2.0F, 0.8F);
                CreatureActions.teleport(creature, Vec3.atBottomCenterOf(hide), c.player);
                // En su escondite arma un altar de huesos con cadaveres y se queda devorando.
                Gore.altar(c.level, hide, c.random);
                Gore.bloodTrail(c.level, story.trailStart, hide, c.random);
                creature.setFeasting(true);
                creature.forceState(SirenState.EATING, 24000);
            }
        }

        // --- voces
        if (!story.voicesStarted && c.timeOfDay >= 5000L && c.timeOfDay < 22000L) {
            BlockPos source = SpawnSpots.find(c.level, c.player.position(), 30.0D, 45.0D, 40, c.random, null);
            if (source != null) {
                story.voicesStarted = true;
                story.voicePos = source;
            }
        }
        if (story.voicePos != null) {
            Vec3 source = Vec3.atBottomCenterOf(story.voicePos);
            if (c.player.distanceToSqr(source) < 49.0D) {
                c.complete(1);
                SirenSounds.at(c.level, source, ModSounds.STATIC.get(), 2.0F, 1.2F);
                SirenSounds.behind(c.player, ModSounds.BREATH.get(), 2.0D, 3.0D, 1.0F, 0.9F);
                story.voicePos = null;
            } else if (c.runtime.beat % 24 == 0) {
                SirenSounds.at(c.level, source.add(0.0D, 1.5D, 0.0D), ModSounds.VOICE.get(), 3.0F, 0.85F + c.random.nextFloat() * 0.25F);
            }
        }

        nightScenes(c);

        // --- rastro y reencuentro
        if (story.vanishStarted && !story.vanishFound && story.trailStart != null && story.trailEnd != null) {
            showTrail(c);
            if (creature != null && creature.isFeasting() && c.player.distanceToSqr(creature) < 24.0D * 24.0D) {
                // Te acercaste: deja de comer y gira la cabeza hacia vos.
                creature.setFeasting(false);
                creature.forceState(SirenState.OBSERVING_PLAYER, 24000);
                SirenSounds.at(c.level, creature.position(), ModSounds.BREATH.get(), 2.0F, 0.7F);
            }
            if (creature != null && c.player.distanceToSqr(creature) < 49.0D) {
                story.vanishFound = true;
                c.complete(0);
                creature.clearOverride();
                creature.setAnchor(c.player.position());
                creature.setSirenState(SirenState.FOLLOWING_PLAYER);
            }
        }
    }

    private static void nightScenes(StoryContext c) {
        PlayerStory story = c.story;
        if (!story.vanishFound || c.creature == null || c.runtime.active != null) {
            return;
        }
        if (!story.feastDone && c.timeOfDay >= 12500L && HorrorDirector.forceStart(c, new FeastEvent(true))) {
            story.feastDone = true;
        } else if (story.feastDone && !story.giftDone && c.timeOfDay >= 15000L && c.timeOfDay < 22000L
                && HorrorDirector.forceStart(c, new GiftEvent())) {
            story.giftDone = true;
        }
    }

    /** Huellas: columnitas de almas cada 3 bloques a lo largo del camino; pasar cerca de la 5ta en adelante completa M3. */
    private static void showTrail(StoryContext c) {
        Vec3 a = Vec3.atBottomCenterOf(c.story.trailStart);
        Vec3 b = Vec3.atBottomCenterOf(c.story.trailEnd);
        int steps = Math.max(5, (int) (a.distanceTo(b) / 3.0D));
        for (int i = 1; i < steps; i++) {
            Vec3 p = a.lerp(b, i / (double) steps);
            double dx = c.player.getX() - p.x;
            double dz = c.player.getZ() - p.z;
            double horizontal = dx * dx + dz * dz;
            if (horizontal > 26.0D * 26.0D) {
                continue;
            }
            int x = Mth.floor(p.x);
            int z = Mth.floor(p.z);
            if (!c.level.hasChunkAt(new BlockPos(x, c.level.getMinBuildHeight(), z))) {
                continue;
            }
            int y = c.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            c.level.sendParticles(ParticleTypes.SOUL, p.x, y + 0.2D, p.z, 2, 0.15D, 0.05D, 0.15D, 0.01D);
            if (i >= 4 && !c.story.isDone(2) && horizontal < 25.0D && Math.abs(c.player.getY() - y) < 4.0D) {
                c.complete(2);
            }
        }
    }
}
