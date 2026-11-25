package com.weraisedsirenhead.entity.ai.goal;

import com.weraisedsirenhead.entity.SirenHeadEntity;
import com.weraisedsirenhead.entity.SirenState;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** Base de los comportamientos: cada goal se activa solo cuando la criatura esta en alguno de sus estados. */
public abstract class SirenStateGoal extends Goal {
    protected final SirenHeadEntity siren;
    private final EnumSet<SirenState> states;

    protected SirenStateGoal(SirenHeadEntity siren, EnumSet<Goal.Flag> flags, SirenState first, SirenState... rest) {
        this.siren = siren;
        this.states = EnumSet.of(first, rest);
        this.setFlags(flags);
    }

    @Override
    public boolean canUse() {
        return !siren.isTransforming() && states.contains(siren.getSirenState());
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void stop() {
        siren.getNavigation().stop();
    }
}
