package com.weraisedsirenhead.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.function.Predicate;

/** Busca lugares de la superficie, ya cargados, donde puede aparecer la criatura. */
public final class SpawnSpots {

    public static boolean standable(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos) || !level.isPositionEntityTicking(pos)) {
            return false;
        }
        BlockPos below = pos.below();
        BlockState ground = level.getBlockState(below);
        if (ground.getCollisionShape(level, below).isEmpty() || ground.is(BlockTags.LEAVES)) {
            return false;
        }
        if (!level.getFluidState(below).isEmpty() || !level.getFluidState(pos).isEmpty()) {
            return false;
        }
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
    }

    /**
     * Elige un punto al azar entre min y max bloques del centro.
     *
     * @param filter condicion extra (puede ser null)
     * @return la posicion (pies de la criatura) o null si no hubo suerte
     */
    public static BlockPos find(ServerLevel level, Vec3 center, double min, double max, int tries,
                                RandomSource random, Predicate<BlockPos> filter) {
        for (int i = 0; i < tries; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double dist = min + random.nextDouble() * (max - min);
            int x = Mth.floor(center.x + Math.cos(angle) * dist);
            int z = Mth.floor(center.z + Math.sin(angle) * dist);
            if (!level.hasChunkAt(new BlockPos(x, level.getMinBuildHeight(), z))) {
                continue;
            }
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (!standable(level, pos)) {
                continue;
            }
            if (filter == null || filter.test(pos)) {
                return pos;
            }
        }
        return null;
    }

    /** Igual que find, pero prefiere un bosque; si no hay ninguno cerca, acepta cualquier terreno. */
    public static BlockPos findForest(ServerLevel level, Vec3 center, double min, double max, RandomSource random) {
        BlockPos forest = find(level, center, min, max, 80, random, p -> level.getBiome(p).is(BiomeTags.IS_FOREST));
        if (forest != null) {
            return forest;
        }
        return find(level, center, min, Math.max(min + 10.0D, max * 0.7D), 80, random, null);
    }

    private SpawnSpots() {
    }
}
