package com.weraisedsirenhead.director.events;

import com.weraisedsirenhead.director.HorrorEvent;
import com.weraisedsirenhead.entity.CreatureActions;
import com.weraisedsirenhead.entity.SirenHeadEntity;
import com.weraisedsirenhead.entity.SirenState;
import com.weraisedsirenhead.story.StoryContext;
import com.weraisedsirenhead.util.Sight;
import com.weraisedsirenhead.world.SpawnSpots;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.phys.Vec3;

import java.util.function.Predicate;

/**
 * La criatura aparece y te mira inmovil, y se esfuma si te acercas o pasa un rato.
 * DISTANT: desde lejos (dia 2+). TREES: entre los arboles (dia 3+). WINDOW: afuera de la casa, de noche (dia 2+).
 */
public class WatchEvent extends HorrorEvent {
    public enum Mode { DISTANT, TREES, WINDOW }

    private final Mode mode;
    private boolean failed;
    private int duration;

    public WatchEvent(Mode mode) {
        this.mode = mode;
    }

    @Override
    public int weight(StoryContext c) {
        SirenHeadEntity creature = c.creature;
        if (creature == null || c.story.transformed || creature.getOverrideTicks() > 200
                || (c.story.vanishStarted && !c.story.vanishFound)) {
            return 0;
        }
        switch (mode) {
            case DISTANT:
                return c.day() >= 2 ? 3 : 0;
            case TREES:
                return c.day() >= 3 ? 2 : 0;
            default:
                return c.day() >= 2 && c.isNight() && c.player.position().distanceTo(c.home()) < 20.0D ? 3 : 0;
        }
    }

    @Override
    public void start(StoryContext c) {
        SirenHeadEntity creature = c.creature;
        if (creature == null) {
            failed = true;
            return;
        }
        double top = creature.getGrowthStage().visualHeight * 0.8D;
        ServerLevel level = c.level;
        Predicate<BlockPos> visible = p -> Sight.canSee(c.player, Vec3.atBottomCenterOf(p).add(0.0D, top, 0.0D));
        BlockPos spot;
        switch (mode) {
            case TREES:
                spot = SpawnSpots.find(level, c.player.position(), 24.0D, 42.0D, 70, c.random,
                        p -> nearLog(level, p) && visible.test(p));
                break;
            case WINDOW:
                spot = SpawnSpots.find(level, c.home(), 7.0D, 12.0D, 70, c.random, visible);
                break;
            default:
                spot = SpawnSpots.find(level, c.player.position(), 28.0D, 44.0D, 60, c.random, visible);
                break;
        }
        if (spot == null) {
            failed = true;
            return;
        }
        duration = 160 + c.random.nextInt(140);
        CreatureActions.teleport(creature, Vec3.atBottomCenterOf(spot), c.player);
        creature.forceState(SirenState.OBSERVING_PLAYER, duration + 40);
    }

    private static boolean nearLog(ServerLevel level, BlockPos pos) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 0; dy <= 2; dy++) {
                    if (level.getBlockState(pos.offset(dx, dy, dz)).is(BlockTags.LOGS)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean tick(StoryContext c) {
        SirenHeadEntity creature = c.creature;
        if (failed || creature == null) {
            return true;
        }
        double tooClose = mode == Mode.WINDOW ? 5.0D : 11.0D;
        return c.player.distanceTo(creature) < tooClose || age * 10 >= duration;
    }

    @Override
    public void end(StoryContext c) {
        if (!failed) {
            releaseCreature(c);
        }
    }
}
