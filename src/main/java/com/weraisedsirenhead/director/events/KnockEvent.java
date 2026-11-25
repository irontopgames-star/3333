package com.weraisedsirenhead.director.events;

import com.weraisedsirenhead.director.HorrorEvent;
import com.weraisedsirenhead.entity.CreatureActions;
import com.weraisedsirenhead.entity.SirenHeadEntity;
import com.weraisedsirenhead.entity.SirenState;
import com.weraisedsirenhead.registry.ModSounds;
import com.weraisedsirenhead.sound.SirenSounds;
import com.weraisedsirenhead.story.StoryContext;
import com.weraisedsirenhead.world.SpawnSpots;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;

import java.util.ArrayList;
import java.util.List;

/**
 * Dia 4+, de noche: golpea tres veces una puerta, trampilla o vidrio cerca de donde estas.
 * Guarda donde golpeo (mision "investigar los sonidos alrededor de la casa").
 */
public class KnockEvent extends HorrorEvent {
    private BlockPos target;
    private boolean glass;
    private boolean moved;
    private int knocks;

    @Override
    public int weight(StoryContext c) {
        if (c.creature == null || c.day() < 4 || !c.isNight() || (c.story.chaseStarted && !c.story.chaseDone)) {
            return 0;
        }
        return c.story.transformed ? 1 : 2;
    }

    @Override
    public void start(StoryContext c) {
        target = findOpening(c);
        BlockState state = c.level.getBlockState(target);
        glass = state.is(Tags.Blocks.GLASS) || state.is(Tags.Blocks.GLASS_PANES);
        SirenHeadEntity creature = c.creature;
        if (creature != null) {
            // Se pone del lado de afuera, 3.5 bloques mas alla del punto golpeado.
            Vec3 center = Vec3.atCenterOf(target);
            Vec3 dir = new Vec3(center.x - c.player.getX(), 0.0D, center.z - c.player.getZ());
            dir = dir.lengthSqr() < 1.0E-4D ? new Vec3(1.0D, 0.0D, 0.0D) : dir.normalize();
            Vec3 outside = center.add(dir.scale(3.5D));
            BlockPos spot = new BlockPos((int) Math.floor(outside.x), (int) Math.floor(c.player.getY()), (int) Math.floor(outside.z));
            for (int dy = -3; dy <= 3; dy++) {
                BlockPos candidate = spot.above(dy);
                if (SpawnSpots.standable(c.level, candidate)) {
                    CreatureActions.teleport(creature, Vec3.atBottomCenterOf(candidate), c.player);
                    creature.forceState(SirenState.OBSERVING_PLAYER, 260);
                    moved = true;
                    break;
                }
            }
        }
    }

    public static BlockPos findOpening(StoryContext c) {
        List<BlockPos> found = new ArrayList<>();
        BlockPos origin = c.player.blockPosition();
        for (int dx = -9; dx <= 9; dx++) {
            for (int dz = -9; dz <= 9; dz++) {
                for (int dy = -2; dy <= 3; dy++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    BlockState state = c.level.getBlockState(pos);
                    if (state.is(BlockTags.DOORS) || state.is(BlockTags.TRAPDOORS)
                            || state.is(Tags.Blocks.GLASS) || state.is(Tags.Blocks.GLASS_PANES)) {
                        found.add(pos);
                    }
                }
            }
        }
        if (!found.isEmpty()) {
            return found.get(c.random.nextInt(found.size()));
        }
        Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(c.random);
        return origin.relative(dir, 4);
    }

    @Override
    public boolean tick(StoryContext c) {
        if (age == 1 || age == 4 || age == 7) {
            SirenSounds.at(c.level, target, glass ? ModSounds.WINDOW_BANG.get() : ModSounds.KNOCK.get(), 3.0F,
                    0.7F + c.random.nextFloat() * 0.2F);
            c.story.lastKnockPos = target;
            c.story.lastKnockTick = c.gameTime;
            knocks++;
        }
        return age >= 9;
    }

    @Override
    public void end(StoryContext c) {
        if (moved && knocks > 0) {
            releaseCreature(c);
        }
    }
}
