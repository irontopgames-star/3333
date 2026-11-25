package com.weraisedsirenhead.entity.ai.goal;

import com.weraisedsirenhead.entity.SirenHeadEntity;
import com.weraisedsirenhead.entity.SirenState;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/** SLEEPING: se acerca al dueno y se duerme a su lado. */
public class SleepStateGoal extends SirenStateGoal {
    private int repath;

    public SleepStateGoal(SirenHeadEntity siren) {
        super(siren, EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK), SirenState.SLEEPING);
    }

    @Override
    public void tick() {
        Player owner = siren.getOwnerPlayer();
        if (owner == null) {
            return;
        }
        if (siren.distanceTo(owner) > 4.0D) {
            if (--repath <= 0) {
                repath = 10;
                siren.getNavigation().moveTo(owner, 0.9D);
            }
        } else {
            siren.getNavigation().stop();
        }
    }
}
