package com.weraisedsirenhead.entity;

import com.weraisedsirenhead.world.SpawnSpots;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Movimientos "magicos" de la criatura: aparecer, desaparecer, reaparecer cerca del jugador. */
public final class CreatureActions {

    /** Mueve a la criatura a pos (pies) mirando hacia face (puede ser null). */
    public static void teleport(SirenHeadEntity creature, Vec3 pos, Player face) {
        ServerLevel level = (ServerLevel) creature.level();
        poof(level, creature.position(), creature);
        creature.getNavigation().stop();
        float yaw = creature.getYRot();
        if (face != null) {
            yaw = (float) (Mth.atan2(face.getZ() - pos.z, face.getX() - pos.x) * (180.0D / Math.PI)) - 90.0F;
        }
        creature.moveTo(pos.x, pos.y, pos.z, yaw, 0.0F);
        creature.setYHeadRot(yaw);
        creature.yBodyRot = yaw;
        creature.setDeltaMovement(Vec3.ZERO);
        poof(level, pos, creature);
    }

    /** Reaparece en un lugar libre entre min y max bloques del jugador. */
    public static boolean teleportNear(SirenHeadEntity creature, Player player, double min, double max) {
        ServerLevel level = (ServerLevel) creature.level();
        BlockPos spot = SpawnSpots.find(level, player.position(), min, max, 50, creature.getRandom(), null);
        if (spot == null) {
            return false;
        }
        teleport(creature, Vec3.atBottomCenterOf(spot), player);
        return true;
    }

    private static void poof(ServerLevel level, Vec3 pos, SirenHeadEntity creature) {
        double h = Math.min(6.0D, creature.getGrowthStage().visualHeight * 0.5D);
        level.sendParticles(ParticleTypes.POOF, pos.x, pos.y + h, pos.z, 10, 0.3D, h * 0.6D, 0.3D, 0.03D);
    }

    private CreatureActions() {
    }
}
