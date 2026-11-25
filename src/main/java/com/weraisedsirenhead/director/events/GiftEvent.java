package com.weraisedsirenhead.director.events;

import com.weraisedsirenhead.director.HorrorEvent;
import com.weraisedsirenhead.entity.CreatureActions;
import com.weraisedsirenhead.entity.SirenHeadEntity;
import com.weraisedsirenhead.entity.SirenState;
import com.weraisedsirenhead.registry.ModSounds;
import com.weraisedsirenhead.sound.SirenSounds;
import com.weraisedsirenhead.story.StoryContext;
import com.weraisedsirenhead.util.Sight;
import com.weraisedsirenhead.world.Gore;
import com.weraisedsirenhead.world.SpawnSpots;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * El "regalo": pasos pesados que se acercan, un golpe sordo y, frente a tu puerta, un cadaver (o varios) con un rastro
 * de sangre que viene del bosque. Despues Siren Head aparece a lo lejos, mirandote, para ver si te gusto.
 * Dia 2: animales. Dia 3: un aldeano. Dia 4: tres cuerpos. Dia 5: cuatro.
 */
public class GiftEvent extends HorrorEvent {
    private boolean failed;
    private Vec3 spotVec;
    private BlockPos spot;
    private Vec3 dir = new Vec3(1.0D, 0.0D, 0.0D);

    @Override
    public int weight(StoryContext c) {
        SirenHeadEntity creature = c.creature;
        if (creature == null || c.day() < 2 || c.story.transformed || creature.getOverrideTicks() > 200
                || (c.story.vanishStarted && !c.story.vanishFound)
                || !(c.isNight() || c.timeOfDay >= 11500L)) {
            return 0;
        }
        return c.day() >= 3 ? 3 : 2;
    }

    @Override
    public void start(StoryContext c) {
        BlockPos door = KnockEvent.findOpening(c);
        Vec3 center = Vec3.atCenterOf(door);
        Vec3 horizontal = new Vec3(center.x - c.player.getX(), 0.0D, center.z - c.player.getZ());
        dir = horizontal.lengthSqr() < 1.0E-4D ? new Vec3(1.0D, 0.0D, 0.0D) : horizontal.normalize();
        Vec3 target = center.add(dir.scale(4.0D));
        spot = Gore.surface(c.level, (int) Math.floor(target.x), (int) Math.floor(target.z));
        if (spot == null) {
            failed = true;
            return;
        }
        spotVec = Vec3.atBottomCenterOf(spot);
    }

    @Override
    public boolean tick(StoryContext c) {
        if (failed) {
            return true;
        }
        if (age == 1) {
            SirenSounds.at(c.level, spotVec.add(dir.scale(26.0D)), ModSounds.STEP.get(), 3.0F, 0.8F);
        } else if (age == 2) {
            SirenSounds.at(c.level, spotVec.add(dir.scale(15.0D)), ModSounds.STEP.get(), 3.0F, 0.8F);
        } else if (age == 3) {
            SirenSounds.at(c.level, spotVec.add(dir.scale(8.0D)), ModSounds.STEP.get(), 3.5F, 0.75F);
        } else if (age == 4) {
            leaveGift(c);
        } else if (age == 6) {
            appear(c);
        }
        return age >= 22;
    }

    private void leaveGift(StoryContext c) {
        int day = c.day();
        int count = day == 2 ? 1 : day == 3 ? 2 : day == 4 ? 3 : 4;
        for (int i = 0; i < count; i++) {
            Vec3 offset = i == 0 ? Vec3.ZERO : new Vec3((c.random.nextDouble() - 0.5D) * 6.0D, 0.0D, (c.random.nextDouble() - 0.5D) * 6.0D);
            BlockPos p = Gore.surface(c.level, (int) Math.floor(spotVec.x + offset.x), (int) Math.floor(spotVec.z + offset.z));
            if (p == null) {
                continue;
            }
            Gore.Kind kind = (i == 0 && day >= 3) ? Gore.Kind.VILLAGER : Gore.randomKind(c.random, day >= 3);
            Gore.corpse(c.level, p, kind, c.random);
        }
        BlockPos farBack = Gore.surface(c.level, (int) Math.floor(spotVec.x + dir.x * 26.0D), (int) Math.floor(spotVec.z + dir.z * 26.0D));
        if (farBack != null) {
            Gore.bloodTrail(c.level, farBack, spot, c.random);
        }
        c.level.addFreshEntity(new ItemEntity(c.level, spotVec.x, spotVec.y + 0.5D, spotVec.z, new ItemStack(Items.ROTTEN_FLESH, 2)));
        c.level.addFreshEntity(new ItemEntity(c.level, spotVec.x + 0.5D, spotVec.y + 0.5D, spotVec.z, new ItemStack(Items.BONE, 2)));
        SirenSounds.at(c.level, spotVec, ModSounds.STEP.get(), 4.0F, 0.5F);
        Gore.bloodBurst(c.level, spotVec.x, spotVec.y + 0.6D, spotVec.z, 30);
    }

    private void appear(StoryContext c) {
        SirenHeadEntity creature = c.creature;
        if (creature == null) {
            return;
        }
        double top = creature.getGrowthStage().visualHeight * 0.8D;
        BlockPos watch = SpawnSpots.find(c.level, spotVec, 28.0D, 40.0D, 50, c.random,
                p -> Sight.canSee(c.player, Vec3.atBottomCenterOf(p).add(0.0D, top, 0.0D)));
        if (watch != null) {
            CreatureActions.teleport(creature, Vec3.atBottomCenterOf(watch), c.player);
            creature.forceState(SirenState.OBSERVING_PLAYER, 160);
        }
    }

    @Override
    public void end(StoryContext c) {
        if (!failed && c.creature != null) {
            releaseCreature(c);
        }
    }
}
