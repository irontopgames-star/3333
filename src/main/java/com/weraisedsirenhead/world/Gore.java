package com.weraisedsirenhead.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Utileria de escenas macabras hechas de bloques: cadaveres (lana roja y de color, costillas de bloque de hueso,
 * calavera), charcos y rastros de sangre (alfombra roja), altares de huesos con antorchas de almas y circulos de ofrendas.
 * Solo se colocan sobre aire o bloques reemplazables (pasto alto, nieve...), nunca rompen construcciones.
 */
public final class Gore {

    public enum Kind { COW, SHEEP, PIG, CHICKEN, VILLAGER }

    /** Primer bloque libre sobre el suelo en esa columna, o null si el chunk no esta cargado. */
    public static BlockPos surface(ServerLevel level, int x, int z) {
        if (!level.hasChunkAt(new BlockPos(x, level.getMinBuildHeight(), z))) {
            return null;
        }
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return new BlockPos(x, y, z);
    }

    private static boolean place(ServerLevel level, BlockPos pos, BlockState state) {
        BlockState current = level.getBlockState(pos);
        if (!(current.isAir() || current.canBeReplaced()) || !level.getFluidState(pos).isEmpty()) {
            return false;
        }
        BlockPos below = pos.below();
        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
            return false;
        }
        return level.setBlock(pos, state, 3);
    }

    private static BlockState hideOf(Kind kind) {
        switch (kind) {
            case SHEEP:
            case CHICKEN:
                return Blocks.WHITE_WOOL.defaultBlockState();
            case PIG:
                return Blocks.PINK_WOOL.defaultBlockState();
            default:
                return Blocks.BROWN_WOOL.defaultBlockState();
        }
    }

    public static Kind randomKind(RandomSource random, boolean allowVillager) {
        int roll = random.nextInt(allowVillager ? 5 : 4);
        return Kind.values()[roll];
    }

    /** Un cadaver tendido: cuerpo abierto, pellejo, costillas asomando y una calavera en la punta. */
    public static void corpse(ServerLevel level, BlockPos start, Kind kind, RandomSource random) {
        Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        int length = kind == Kind.COW ? 3 : kind == Kind.CHICKEN ? 1 : 2;
        BlockState hide = hideOf(kind);
        BlockState flesh = Blocks.RED_WOOL.defaultBlockState();
        for (int i = 0; i < length; i++) {
            BlockPos p = surface(level, start.getX() + dir.getStepX() * i, start.getZ() + dir.getStepZ() * i);
            if (p == null) {
                continue;
            }
            place(level, p, i == 0 ? flesh : hide);
            if (random.nextInt(3) != 0) {
                place(level, p.above(), Blocks.BONE_BLOCK.defaultBlockState());
            }
        }
        BlockPos end = surface(level, start.getX() + dir.getStepX() * length, start.getZ() + dir.getStepZ() * length);
        if (end != null) {
            place(level, end, kind == Kind.VILLAGER ? Blocks.ZOMBIE_HEAD.defaultBlockState() : Blocks.SKELETON_SKULL.defaultBlockState());
        }
        blood(level, start, 2, random);
    }

    /** Charco de sangre irregular (alfombra roja). */
    public static void blood(ServerLevel level, BlockPos center, int radius, RandomSource random) {
        BlockState carpet = Blocks.RED_CARPET.defaultBlockState();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz <= radius * radius && random.nextInt(100) < 55) {
                    BlockPos p = surface(level, center.getX() + dx, center.getZ() + dz);
                    if (p != null) {
                        place(level, p, carpet);
                    }
                }
            }
        }
    }

    /** Rastro de sangre entre dos puntos (como si arrastraran algo). */
    public static void bloodTrail(ServerLevel level, BlockPos from, BlockPos to, RandomSource random) {
        Vec3 a = Vec3.atBottomCenterOf(from);
        Vec3 b = Vec3.atBottomCenterOf(to);
        int steps = Math.max(2, (int) (a.distanceTo(b) / 1.6D));
        BlockState carpet = Blocks.RED_CARPET.defaultBlockState();
        for (int i = 0; i <= steps; i++) {
            Vec3 p = a.lerp(b, i / (double) steps);
            int x = (int) Math.floor(p.x + (random.nextDouble() - 0.5D) * 1.2D);
            int z = (int) Math.floor(p.z + (random.nextDouble() - 0.5D) * 1.2D);
            BlockPos surface = surface(level, x, z);
            if (surface != null) {
                place(level, surface, carpet);
            }
        }
    }

    /** Altar: pilar de huesos con calavera, aro de huesos con antorchas de almas y calaveras, y cadaveres alrededor. */
    public static void altar(ServerLevel level, BlockPos center, RandomSource random) {
        BlockPos c = surface(level, center.getX(), center.getZ());
        if (c == null) {
            return;
        }
        BlockState bone = Blocks.BONE_BLOCK.defaultBlockState();
        for (int i = 0; i < 3; i++) {
            place(level, c.above(i), bone);
        }
        place(level, c.above(3), Blocks.SKELETON_SKULL.defaultBlockState());
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4.0D;
            BlockPos p = surface(level, c.getX() + (int) Math.round(Math.cos(angle) * 4.0D), c.getZ() + (int) Math.round(Math.sin(angle) * 4.0D));
            if (p != null && place(level, p, bone)) {
                place(level, p.above(), i % 2 == 0 ? Blocks.SOUL_TORCH.defaultBlockState() : Blocks.SKELETON_SKULL.defaultBlockState());
            }
        }
        for (int i = 0; i < 4; i++) {
            double angle = i * Math.PI / 2.0D + Math.PI / 4.0D;
            BlockPos p = surface(level, c.getX() + (int) Math.round(Math.cos(angle) * 7.0D), c.getZ() + (int) Math.round(Math.sin(angle) * 7.0D));
            if (p != null) {
                corpse(level, p, randomKind(random, true), random);
            }
        }
        blood(level, c, 5, random);
    }

    /** Circulo de ofrendas alrededor de un punto (tu casa): cadaveres, huesos y antorchas de almas. */
    public static void ring(ServerLevel level, Vec3 center, int radius, int count, RandomSource random) {
        for (int i = 0; i < count; i++) {
            double angle = i * Math.PI * 2.0D / count + random.nextDouble() * 0.3D;
            BlockPos p = surface(level, (int) Math.floor(center.x + Math.cos(angle) * radius), (int) Math.floor(center.z + Math.sin(angle) * radius));
            if (p == null) {
                continue;
            }
            corpse(level, p, randomKind(random, true), random);
            double between = angle + Math.PI / count;
            BlockPos q = surface(level, (int) Math.floor(center.x + Math.cos(between) * radius), (int) Math.floor(center.z + Math.sin(between) * radius));
            if (q != null && place(level, q, Blocks.BONE_BLOCK.defaultBlockState())) {
                place(level, q.above(), Blocks.BONE_BLOCK.defaultBlockState());
                place(level, q.above(2), random.nextBoolean() ? Blocks.SOUL_TORCH.defaultBlockState() : Blocks.SKELETON_SKULL.defaultBlockState());
            }
        }
    }

    /** Salpicadura de sangre (particulas). */
    public static void bloodBurst(ServerLevel level, double x, double y, double z, int count) {
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()),
                x, y, z, count, 0.3D, 0.5D, 0.3D, 0.12D);
    }

    private Gore() {
    }
}
