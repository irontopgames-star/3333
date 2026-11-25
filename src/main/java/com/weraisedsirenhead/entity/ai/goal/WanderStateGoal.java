package com.weraisedsirenhead.entity.ai.goal;

import com.weraisedsirenhead.entity.SirenHeadEntity;
import com.weraisedsirenhead.entity.SirenState;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** WANDERING: camina sin rumbo fijo alrededor de un punto de anclaje. */
public class WanderStateGoal extends SirenStateGoal {
    private int timer;

    public WanderStateGoal(SirenHeadEntity siren) {
        super(siren, EnumSet.of(Goal.Flag.MOVE), SirenState.WANDERING);
    }

    @Override
    public void start() {
        timer = 0;
    }

    @Override
    public void tick() {
        if (--timer > 0) {
            return;
        }
        timer = 80 + siren.getRandom().nextInt(80);
        Vec3 anchor = siren.getAnchor();
        if (anchor == null) {
            Player owner = siren.getOwnerPlayer();
            if (owner == null) {
                return;
            }
            anchor = owner.position();
        }
        Vec3 target = LandRandomPos.getPosTowards(siren, siren.getWanderRadius(), 4, anchor);
        if (target != null) {
            siren.getNavigation().moveTo(target.x, target.y, target.z, 0.8D);
        }
    }
}
