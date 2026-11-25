package com.weraisedsirenhead.sound;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Reproduccion de sonidos con distancia y direccion.
 * El alcance depende del volumen (alcance ~ 16 bloques x volumen cuando el volumen es mayor que 1):
 * 1.0 = susurro (16), 2.0 = medio (32), 4.0 = lejano (64), 6.0 = sirena de aviso (96).
 */
public final class SirenSounds {

    public static void at(ServerLevel level, Vec3 pos, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, pos.x, pos.y, pos.z, sound, SoundSource.HOSTILE, volume, pitch);
    }

    public static void at(ServerLevel level, BlockPos pos, SoundEvent sound, float volume, float pitch) {
        at(level, Vec3.atCenterOf(pos), sound, volume, pitch);
    }

    /** Punto al azar alrededor del jugador, entre min y max bloques. */
    public static Vec3 randomPoint(ServerPlayer player, double min, double max) {
        RandomSource r = player.getRandom();
        double angle = r.nextDouble() * Math.PI * 2.0D;
        double dist = min + r.nextDouble() * (max - min);
        return new Vec3(player.getX() + Math.cos(angle) * dist, player.getEyeY(), player.getZ() + Math.sin(angle) * dist);
    }

    /** Punto a la espalda del jugador (opuesto a hacia donde mira), con un poco de dispersion. */
    public static Vec3 behindPoint(ServerPlayer player, double min, double max) {
        RandomSource r = player.getRandom();
        Vec3 look = player.getLookAngle();
        Vec3 back = new Vec3(-look.x, 0.0D, -look.z);
        if (back.lengthSqr() < 1.0E-4D) {
            back = new Vec3(0.0D, 0.0D, 1.0D);
        }
        back = back.normalize().yRot((float) Math.toRadians((r.nextDouble() - 0.5D) * 70.0D));
        double dist = min + r.nextDouble() * (max - min);
        return new Vec3(player.getX() + back.x * dist, player.getEyeY(), player.getZ() + back.z * dist);
    }

    public static void around(ServerPlayer player, SoundEvent sound, double min, double max, float volume, float pitch) {
        at(player.serverLevel(), randomPoint(player, min, max), sound, volume, pitch);
    }

    public static void behind(ServerPlayer player, SoundEvent sound, double min, double max, float volume, float pitch) {
        at(player.serverLevel(), behindPoint(player, min, max), sound, volume, pitch);
    }

    private SirenSounds() {
    }
}
