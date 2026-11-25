package com.weraisedsirenhead.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * Detecta si un punto esta dentro de un recinto cerrado (una casita): recorre el aire conectado y,
 * si se escapa por cualquier lado (supera el limite de celdas, sale del mundo o toca chunks sin cargar),
 * no es un refugio.
 */
public final class Shelter {
    public static final int MAX_CELLS = 500;
    public static final int MIN_CELLS = 6;

    public static boolean isEnclosed(Level level, BlockPos start) {
        BlockPos begin = start;
        if (!passable(level, begin)) {
            begin = begin.above();
            if (!passable(level, begin)) {
                return false;
            }
        }
        Set<Long> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(begin);
        visited.add(begin.asLong());
        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos next = current.relative(dir);
                if (visited.contains(next.asLong())) {
                    continue;
                }
                if (level.isOutsideBuildHeight(next) || !level.hasChunkAt(next)) {
                    return false;
                }
                if (!passable(level, next)) {
                    continue;
                }
                visited.add(next.asLong());
                if (visited.size() > MAX_CELLS) {
                    return false;
                }
                queue.add(next);
            }
        }
        return visited.size() >= MIN_CELLS;
    }

    private static boolean passable(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getCollisionShape(level, pos).isEmpty();
    }

    private Shelter() {
    }
}
