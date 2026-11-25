package com.weraisedsirenhead.entity.ai.goal;

import com.weraisedsirenhead.entity.SirenHeadEntity;
import com.weraisedsirenhead.entity.SirenState;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** HIDING: se aleja del jugador hasta ponerse a distancia y se queda quieta mirando. */
public class HideStateGoal extends SirenStateGoal {
    private int repath;

    public HideStateGoal(SirenHeadEntity siren) {
        super(siren, EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK), SirenState.HIDING);
    }

    @Override
    public void tick() {
        Player owner = siren.getOwnerPlayer();
        if (owner == null) {
            return;
        }
        if (siren.distanceTo(owner) < 24.0D) {
            if (--repath <= 0) {
                repath = 20;
                Vec3 away = LandRandomPos.getPosAway(siren, 16, 7, owner.position());
                if (away != null) {
                    siren.getNavigation().moveTo(away.x, away.y, away.z, 1.0D);
                }
            }
        } else {
            siren.getNavigation().stop();
            siren.getLookControl().setLookAt(owner.getX(), owner.getEyeY(), owner.getZ());
            siren.faceTowards(owner.position(), 5.0F);
        }
    }
}
