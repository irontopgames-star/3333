package com.weraisedsirenhead.entity.ai;

import com.weraisedsirenhead.entity.CreatureActions;
import com.weraisedsirenhead.entity.SirenHeadEntity;
import com.weraisedsirenhead.entity.SirenState;
import com.weraisedsirenhead.registry.ModSounds;
import com.weraisedsirenhead.sound.SirenSounds;
import com.weraisedsirenhead.story.PlayerStory;
import com.weraisedsirenhead.story.StoryClock;
import com.weraisedsirenhead.story.StoryData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Decide en que estado esta la criatura, una vez por segundo, segun el dia y el contexto.
 * Mientras haya un estado forzado (por un evento o por el propio cerebro) no cambia nada.
 * Es impredecible (probabilidades y duraciones al azar) pero justa: los ataques tienen aviso,
 * enfriamiento y, antes del dia 5, nunca matan.
 */
public final class SirenBrain {

    public static void think(SirenHeadEntity siren, ServerLevel level) {
        if (siren.isTransforming() || siren.getOverrideTicks() > 0) {
            return;
        }
        Player owner = siren.getOwnerPlayer();
        if (owner == null) {
            siren.setSirenState(SirenState.IDLE);
            return;
        }
        PlayerStory story = StoryData.get(level.getServer()).get(owner.getUUID());
        if (story == null || !story.active || story.completed) {
            siren.setSirenState(SirenState.IDLE);
            return;
        }
        if (story.day == 1 && !story.isDone(0)) {
            // Todavia no la encontraste: se queda esperando donde esta (el dia 1 el bebe esta perdido en el bosque).
            siren.setSirenState(SirenState.IDLE);
            return;
        }
        RandomSource random = siren.getRandom();
        double distance = siren.distanceTo(owner);
        boolean night = StoryClock.isNight(level);
        switch (story.day) {
            case 1:
            case 2:
                earlyDays(siren, owner, random, distance, night, story.day);
                break;
            case 3:
                dayThree(siren, owner, random, distance);
                break;
            case 4:
                stalkCycle(siren, owner, random, distance, false);
                break;
            default:
                if (story.transformed) {
                    hunt(siren, owner, level, random, distance);
                } else {
                    stalkCycle(siren, owner, random, distance, true);
                }
                break;
        }
    }

    /** Dias 1 y 2: mascota. Sigue, se sienta, observa, duerme de noche (dia 1). */
    private static void earlyDays(SirenHeadEntity siren, Player owner, RandomSource random, double distance,
                                  boolean night, int day) {
        if (siren.isStay()) {
            siren.setSirenState(SirenState.IDLE);
            return;
        }
        if (day == 1 && night && distance < 40.0D) {
            siren.setSirenState(SirenState.SLEEPING);
            return;
        }
        double follow = day == 1 ? 5.0D : 6.5D;
        if (distance > follow) {
            siren.setSirenState(SirenState.FOLLOWING_PLAYER);
            if (distance > 14.0D) {
                siren.chirp();
            }
            return;
        }
        int roll = random.nextInt(100);
        int observe = day == 1 ? 20 : 30;
        if (roll < observe) {
            siren.forceState(SirenState.OBSERVING_PLAYER, 60 + random.nextInt(100));
        } else if (roll < observe + 12) {
            siren.setAnchor(owner.position());
            siren.setWanderRadius(day == 1 ? 5 : 9);
            siren.forceState(SirenState.WANDERING, 100 + random.nextInt(100));
        } else {
            siren.setSirenState(SirenState.IDLE);
        }
    }

    /** Dia 3: se aleja de su dueno, lo mira de lejos, se esconde. */
    private static void dayThree(SirenHeadEntity siren, Player owner, RandomSource random, double distance) {
        if (distance > 45.0D) {
            siren.setSirenState(SirenState.FOLLOWING_PLAYER);
            return;
        }
        int roll = random.nextInt(100);
        if (roll < 55) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double reach = 16.0D + random.nextDouble() * 8.0D;
            siren.setAnchor(owner.position().add(Math.cos(angle) * reach, 0.0D, Math.sin(angle) * reach));
            siren.setWanderRadius(8);
            siren.forceState(SirenState.WANDERING, 160 + random.nextInt(160));
        } else if (roll < 85) {
            siren.forceState(SirenState.OBSERVING_PLAYER, 120 + random.nextInt(140));
        } else {
            siren.forceState(SirenState.HIDING, 160 + random.nextInt(80));
        }
    }

    /** Dia 4 (y la tarde del 5): aparece, desaparece, acecha; no ataca constantemente. */
    private static void stalkCycle(SirenHeadEntity siren, Player owner, RandomSource random, double distance, boolean dayFive) {
        if (distance > 90.0D) {
            CreatureActions.teleportNear(siren, owner, 45.0D, 60.0D);
        }
        int roll = random.nextInt(100);
        if (dayFive) {
            if (roll < 40) {
                siren.forceState(SirenState.HIDING, 240 + random.nextInt(200));
            } else if (roll < 80) {
                siren.forceState(SirenState.OBSERVING_PLAYER, 240 + random.nextInt(200));
            } else {
                siren.setAnchor(owner.position().add(random.nextInt(61) - 30, 0.0D, random.nextInt(61) - 30));
                siren.setWanderRadius(10);
                siren.forceState(SirenState.WANDERING, 200 + random.nextInt(100));
            }
            return;
        }
        if (roll < 35) {
            siren.forceState(SirenState.STALKING, 200 + random.nextInt(200));
        } else if (roll < 60) {
            siren.forceState(SirenState.OBSERVING_PLAYER, 200 + random.nextInt(200));
        } else if (roll < 85) {
            siren.forceState(SirenState.HIDING, 200 + random.nextInt(160));
        } else {
            siren.setAnchor(owner.position().add(random.nextInt(41) - 20, 0.0D, random.nextInt(41) - 20));
            siren.setWanderRadius(10);
            siren.forceState(SirenState.WANDERING, 200 + random.nextInt(100));
        }
    }

    /** Dia 5, ya transformada: busca al jugador, acecha y lo persigue en rafagas con descansos. */
    private static void hunt(SirenHeadEntity siren, Player owner, ServerLevel level, RandomSource random, double distance) {
        if (owner.isCreative() || owner.isSpectator()) {
            siren.setSirenState(SirenState.OBSERVING_PLAYER);
            return;
        }
        if (siren.getChaseCooldown() > 0) {
            if (random.nextInt(100) < 30) {
                siren.forceState(SirenState.HIDING, 100 + random.nextInt(80));
            } else {
                siren.setSearchPos(owner.position());
                siren.forceState(SirenState.SEARCHING, 120 + random.nextInt(80));
            }
            return;
        }
        if (distance < 70.0D && (siren.hasLineOfSight(owner) || random.nextInt(3) == 0)) {
            startChase(siren, level, 500 + random.nextInt(300));
            return;
        }
        if (distance > 100.0D) {
            CreatureActions.teleportNear(siren, owner, 50.0D, 62.0D);
            siren.forceState(SirenState.STALKING, 200);
            return;
        }
        if (random.nextBoolean()) {
            siren.forceState(SirenState.STALKING, 160 + random.nextInt(100));
        } else {
            siren.setSearchPos(owner.position());
            siren.forceState(SirenState.SEARCHING, 160 + random.nextInt(100));
        }
    }

    /** Arranca una persecucion avisando con una sirena; durante 2 segundos no se mueve. */
    public static void startChase(SirenHeadEntity siren, ServerLevel level, int ticks) {
        siren.forceState(SirenState.CHASING, ticks);
        siren.setChaseDelay(40);
        Vec3 pos = siren.position();
        if (siren.getRandom().nextInt(100) < 55) {
            SirenSounds.at(level, pos, ModSounds.AIR_RAID.get(), 6.0F, 1.0F);
        } else {
            SirenSounds.at(level, pos, ModSounds.ALARM.get(), 5.0F, 0.9F);
        }
    }

    /** Despues de golpear (o de apiadarse del jugador) se retira un rato. */
    public static void afterAttack(SirenHeadEntity siren) {
        Player owner = siren.getOwnerPlayer();
        if (owner != null) {
            siren.setSearchPos(owner.position());
        }
        if (siren.getStage() >= 5) {
            siren.setChaseCooldown(400);
            siren.forceState(SirenState.SEARCHING, 180);
        } else {
            siren.setChaseCooldown(1200);
            siren.forceState(SirenState.HIDING, 200);
        }
    }

    private SirenBrain() {
    }
}
