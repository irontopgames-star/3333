package com.weraisedsirenhead.entity.ai.goal;

import com.weraisedsirenhead.entity.SirenHeadEntity;
import com.weraisedsirenhead.entity.SirenState;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** SEARCHING: va hacia el ultimo lugar donde creyo oir o ver al jugador y rastrea la zona. */
public class SearchStateGoal extends SirenStateGoal {
    private int timer;

    public SearchStateGoal(SirenHeadEntity siren) {
        super(siren, EnumSet.of(Goal.Flag.MOVE), SirenState.SEARCHING);
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
        timer = 60;
        Vec3 target = siren.getSearchPos();
        if (target == null) {
            Player owner = siren.getOwnerPlayer();
            if (owner == null) {
                return;
            }
            target = owner.position();
            siren.setSearchPos(target);
        }
        if (siren.position().distanceTo(target) < 4.0D) {
            Vec3 around = LandRandomPos.getPosTowards(siren, 12, 4, target);
            if (around != null) {
                siren.setSearchPos(around);
                siren.getNavigation().moveTo(around.x, around.y, around.z, 0.9D);
            }
        } else {
            siren.getNavigation().moveTo(target.x, target.y, target.z, 0.95D);
        }
    }
}
