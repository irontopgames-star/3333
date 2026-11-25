package com.weraisedsirenhead.entity.ai;

import com.weraisedsirenhead.config.SirenHeadConfig;
import com.weraisedsirenhead.entity.SirenHeadEntity;
import com.weraisedsirenhead.registry.ModSounds;
import com.weraisedsirenhead.sound.SirenSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;

/**
 * Siren Head adulto destruye solo ciertos bloques de las construcciones: puertas y trampillas de madera,
 * tablones, escaleras/losas/vallas de madera, lana, vidrio y hojas. La piedra, el ladrillo, la tierra
 * y el metal lo detienen (golpea la pared pero no puede pasar).
 */
public final class BlockBreaker {
    public static final int NOTHING = 0;
    public static final int BROKE = 1;
    public static final int BLOCKED = 2;

    public static boolean breakable(BlockState state) {
        return state.is(BlockTags.WOODEN_DOORS) || state.is(BlockTags.WOODEN_TRAPDOORS)
                || state.is(BlockTags.PLANKS) || state.is(BlockTags.WOODEN_STAIRS)
                || state.is(BlockTags.WOODEN_SLABS) || state.is(BlockTags.WOODEN_FENCES)
                || state.is(BlockTags.FENCE_GATES) || state.is(BlockTags.WOOL)
                || state.is(BlockTags.LEAVES) || state.is(Tags.Blocks.GLASS) || state.is(Tags.Blocks.GLASS_PANES);
    }

    /** Recorre la linea hacia el objetivo; rompe el primer obstaculo permitido o avisa que esta bloqueado. */
    public static int tryBreak(SirenHeadEntity siren, Player target) {
        if (!SirenHeadConfig.ALLOW_BLOCK_DESTRUCTION.get() || !(siren.level() instanceof ServerLevel level)) {
            return NOTHING;
        }
        if (!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return NOTHING;
        }
        Vec3 from = siren.position().add(0.0D, 1.0D, 0.0D);
        Vec3 to = target.position().add(0.0D, 1.0D, 0.0D);
        Vec3 direction = to.subtract(from);
        double length = Math.min(direction.length(), siren.attackReach() + 2.0D);
        if (length < 0.5D) {
            return NOTHING;
        }
        direction = direction.normalize();
        for (double d = 0.9D; d <= length; d += 0.5D) {
            BlockPos pos = BlockPos.containing(from.add(direction.scale(d)));
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.getCollisionShape(level, pos).isEmpty()) {
                continue;
            }
            if (!breakable(state)) {
                if (siren.tickCount % 40 < 10) {
                    SirenSounds.at(level, pos, ModSounds.KNOCK.get(), 3.0F, 0.6F);
                }
                return BLOCKED;
            }
            breakColumn(level, siren, pos);
            return BROKE;
        }
        return NOTHING;
    }

    private static void breakColumn(Level level, SirenHeadEntity siren, BlockPos pos) {
        for (int dy = 0; dy <= 2; dy++) {
            BlockPos p = pos.above(dy);
            if (breakable(level.getBlockState(p))) {
                level.destroyBlock(p, true, siren);
            }
        }
        BlockPos below = pos.below();
        if (below.getY() >= siren.blockPosition().getY() && breakable(level.getBlockState(below))) {
            level.destroyBlock(below, true, siren);
        }
    }

    private BlockBreaker() {
    }
}
