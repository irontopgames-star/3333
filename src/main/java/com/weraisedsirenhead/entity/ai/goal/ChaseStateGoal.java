package com.weraisedsirenhead.entity.ai.goal;

import com.weraisedsirenhead.entity.SirenHeadEntity;
import com.weraisedsirenhead.entity.SirenState;
import com.weraisedsirenhead.entity.ai.BlockBreaker;
import com.weraisedsirenhead.entity.ai.SirenBrain;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * CHASING: persigue al jugador. Antes de arrancar espera un instante (aviso con la sirena) para que
 * la persecucion sea justa. El adulto rompe puertas, vidrios y madera que le cierran el paso.
 */
public class ChaseStateGoal extends SirenStateGoal {
    private int repath;
    private int breakTimer;

    public ChaseStateGoal(SirenHeadEntity siren) {
        super(siren, EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK), SirenState.CHASING);
    }

    @Override
    public void tick() {
        Player owner = siren.getOwnerPlayer();
        if (owner == null) {
            return;
        }
        siren.getLookControl().setLookAt(owner.getX(), owner.getEyeY(), owner.getZ());
        if (siren.getChaseDelay() > 0) {
            siren.getNavigation().stop();
            siren.faceTowards(owner.position(), 10.0F);
            return;
        }
        if (siren.getStage() < 5 && owner.getHealth() <= 6.0F) {
            SirenBrain.afterAttack(siren);
            return;
        }
        double distance = siren.distanceTo(owner);
        if (distance <= siren.attackReach() && siren.getAttackCooldown() <= 0 && siren.hasLineOfSight(owner)) {
            siren.setSirenState(SirenState.ATTACKING);
            return;
        }
        if (--repath <= 0) {
            repath = 4;
            siren.getNavigation().moveTo(owner, siren.chaseSpeed());
        }
        if (siren.getStage() >= 5 && --breakTimer <= 0) {
            breakTimer = 10;
            if (distance > 2.0D) {
                BlockBreaker.tryBreak(siren, owner);
            }
        }
    }
}
