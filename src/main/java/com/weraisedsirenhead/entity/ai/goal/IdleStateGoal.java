package com.weraisedsirenhead.entity.ai.goal;

import com.weraisedsirenhead.entity.SirenHeadEntity;
import com.weraisedsirenhead.entity.SirenState;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/** IDLE y EATING: se queda quieta, mirando a su dueno si esta cerca. */
public class IdleStateGoal extends SirenStateGoal {
    public IdleStateGoal(SirenHeadEntity siren) {
        super(siren, EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK), SirenState.IDLE, SirenState.EATING);
    }

    @Override
    public void start() {
        siren.getNavigation().stop();
    }

    @Override
    public void tick() {
        Player owner = siren.getOwnerPlayer();
        if (owner != null && !siren.isFeasting() && siren.distanceTo(owner) < 16.0F) {
            siren.getLookControl().setLookAt(owner, 30.0F, 30.0F);
        }
    }
}
