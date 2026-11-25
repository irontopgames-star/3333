package com.weraisedsirenhead.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Comprobaciones de vision (lineas de vista y hacia donde mira el jugador). */
public final class Sight {
    /** true si no hay bloques entre los ojos del jugador y el punto. */
    public static boolean canSee(Player viewer, Vec3 target) {
        ClipContext ctx = new ClipContext(viewer.getEyePosition(), target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, viewer);
        return viewer.level().clip(ctx).getType() == HitResult.Type.MISS;
    }

    /** true si el punto esta dentro del cono de vision del jugador (dot ~0.8 = ~37 grados). */
    public static boolean isLookingAt(Player viewer, Vec3 target, double minDot) {
        Vec3 look = viewer.getLookAngle().normalize();
        Vec3 to = target.subtract(viewer.getEyePosition()).normalize();
        return look.dot(to) >= minDot;
    }

    private Sight() {
    }
}
