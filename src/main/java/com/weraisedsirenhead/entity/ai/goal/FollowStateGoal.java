package com.weraisedsirenhead.entity.ai.goal;

import com.weraisedsirenhead.entity.CreatureActions;
import com.weraisedsirenhead.entity.SirenHeadEntity;
import com.weraisedsirenhead.entity.SirenState;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/** FOLLOWING_PLAYER: sigue al dueno y se queda cerca. */
public class FollowStateGoal extends SirenStateGoal {
    private int repath;

    public FollowStateGoal(SirenHeadEntity siren) {
        super(siren, EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK), SirenState.FOLLOWING_PLAYER);
    }

    @Override
    public void tick() {
        Player owner = siren.getOwnerPlayer();
        if (owner == null) {
            return;
        }
        double distance = siren.distanceTo(owner);
        siren.getLookControl().setLookAt(owner, 30.0F, 30.0F);
        if (distance > 48.0D) {
            CreatureActions.teleportNear(siren, owner, 8.0D, 12.0D);
            return;
        }
        if (distance > siren.followDistance()) {
            if (--repath <= 0) {
                repath = 8;
                siren.getNavigation().moveTo(owner, 1.0D);
            }
        } else {
            siren.getNavigation().stop();
        }
    }
}
