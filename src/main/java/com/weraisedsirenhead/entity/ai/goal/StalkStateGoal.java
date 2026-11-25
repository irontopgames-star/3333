package com.weraisedsirenhead.entity.ai.goal;

import com.weraisedsirenhead.entity.SirenHeadEntity;
import com.weraisedsirenhead.entity.SirenState;
import com.weraisedsirenhead.util.Sight;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * STALKING: se acerca de a poco, pero se congela cuando el jugador la mira.
 * Se detiene a unos metros y queda observando.
 */
public class StalkStateGoal extends SirenStateGoal {
    private int repath;

    public StalkStateGoal(SirenHeadEntity siren) {
        super(siren, EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK), SirenState.STALKING);
    }

    @Override
    public void tick() {
        Player owner = siren.getOwnerPlayer();
        if (owner == null) {
            return;
        }
        siren.getLookControl().setLookAt(owner.getX(), owner.getEyeY(), owner.getZ());
        double distance = siren.distanceTo(owner);
        if (distance <= siren.stalkDistance()) {
            siren.getNavigation().stop();
            siren.faceTowards(owner.position(), 6.0F);
            return;
        }
        Vec3 center = siren.position().add(0.0D, siren.getGrowthStage().visualHeight * 0.6D, 0.0D);
        boolean watched = distance < 60.0D && Sight.isLookingAt(owner, center, 0.82D) && siren.hasLineOfSight(owner);
        if (watched) {
            siren.getNavigation().stop();
            return;
        }
        if (--repath <= 0) {
            repath = 10;
            siren.getNavigation().moveTo(owner, siren.stalkSpeed());
        }
    }
}
