package com.weraisedsirenhead.entity.ai.goal;

import com.weraisedsirenhead.entity.SirenHeadEntity;
import com.weraisedsirenhead.entity.SirenState;
import com.weraisedsirenhead.entity.ai.SirenBrain;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/** ATTACKING: golpe con aviso (anticipacion de 12 ticks) y recuperacion. Despues se retira. */
public class AttackStateGoal extends SirenStateGoal {
    private int ticks;

    public AttackStateGoal(SirenHeadEntity siren) {
        super(siren, EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK), SirenState.ATTACKING);
    }

    @Override
    public void start() {
        ticks = 0;
        siren.getNavigation().stop();
        siren.setAttackCooldown(80);
    }

    @Override
    public void tick() {
        ticks++;
        Player owner = siren.getOwnerPlayer();
        if (owner != null) {
            siren.getLookControl().setLookAt(owner.getX(), owner.getEyeY(), owner.getZ());
            siren.faceTowards(owner.position(), 25.0F);
            if (ticks == 12 && siren.distanceTo(owner) <= siren.attackReach() + 1.2D) {
                siren.strike(owner);
            }
        }
        if (ticks >= 26) {
            SirenBrain.afterAttack(siren);
        }
    }
}
