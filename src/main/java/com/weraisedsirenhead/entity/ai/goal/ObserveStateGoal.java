package com.weraisedsirenhead.entity.ai.goal;

import com.weraisedsirenhead.entity.SirenHeadEntity;
import com.weraisedsirenhead.entity.SirenState;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/** OBSERVING_PLAYER: inmovil, con la mirada clavada en el jugador. */
public class ObserveStateGoal extends SirenStateGoal {
    public ObserveStateGoal(SirenHeadEntity siren) {
        super(siren, EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK), SirenState.OBSERVING_PLAYER);
    }

    @Override
    public void start() {
        siren.getNavigation().stop();
    }

    @Override
    public void tick() {
        Player owner = siren.getOwnerPlayer();
        if (owner == null) {
            return;
        }
        siren.getLookControl().setLookAt(owner.getX(), owner.getEyeY(), owner.getZ());
        siren.faceTowards(owner.position(), 6.0F);
    }
}
