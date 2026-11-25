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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Siren Head devora a un animal (vaca, cerdo, oveja, gallina) o a un aldeano delante tuyo: aparece junto a la victima,
 * la deja inmovil, la mata con sonidos humedos y salpicaduras y deja los restos como cadaver de bloques.
 * Usa un bicho que ya este cerca; si no hay ninguno, aparece uno solo (bizarro). Si te acercas a menos de 9 bloques
 * se detiene y te mira.
 */
public class FeastEvent extends HorrorEvent {
    private final boolean villager;
    private Mob victim;
    private BlockPos victimPos;
    private Gore.Kind kind = Gore.Kind.COW;
    private boolean failed;
    private boolean placed;
    private boolean interrupted;

    public FeastEvent(boolean villager) {
        this.villager = villager;
    }

    @Override
    public int weight(StoryContext c) {
        SirenHeadEntity creature = c.creature;
        if (creature == null || c.story.transformed || creature.getOverrideTicks() > 200
                || (c.story.vanishStarted && !c.story.vanishFound)) {
            return 0;
        }
        if (villager) {
            return c.day() >= 3 ? 2 : 0;
        }
        return c.day() >= 2 ? 3 : 0;
    }

    @Override
    public void start(StoryContext c) {
        SirenHeadEntity creature = c.creature;
        victim = creature == null ? null : findVictim(c);
        if (victim == null) {
            failed = true;
            return;
        }
        victim.setNoAi(true);
        victimPos = victim.blockPosition();
        kind = kindOf(victim);
        Vec3 vp = victim.position();
        Vec3 away = new Vec3(vp.x - c.player.getX(), 0.0D, vp.z - c.player.getZ());
        away = away.lengthSqr() < 1.0E-4D ? new Vec3(1.0D, 0.0D, 0.0D) : away.normalize();
        Vec3 target = vp.add(away.scale(2.5D));
        BlockPos stand = null;
        for (int dy = -3; dy <= 3 && stand == null; dy++) {
            BlockPos candidate = BlockPos.containing(target.x, target.y + dy, target.z);
            if (SpawnSpots.standable(c.level, candidate)) {
                stand = candidate;
            }
        }
        if (stand == null) {
            failed = true;
            return;
        }
        CreatureActions.teleport(creature, Vec3.atBottomCenterOf(stand), null);
        creature.faceTowards(vp, 360.0F);
        creature.setFeasting(true);
        creature.forceState(SirenState.EATING, 260);
        SirenSounds.at(c.level, vp, ModSounds.FEAST.get(), 3.0F, 0.9F);
    }

    private Mob findVictim(StoryContext c) {
        AABB box = c.player.getBoundingBox().inflate(48.0D);
        List<Mob> candidates = new ArrayList<>();
        if (villager) {
            candidates.addAll(c.level.getEntitiesOfClass(Villager.class, box));
        } else {
            candidates.addAll(c.level.getEntitiesOfClass(Cow.class, box));
            candidates.addAll(c.level.getEntitiesOfClass(Pig.class, box));
            candidates.addAll(c.level.getEntitiesOfClass(Sheep.class, box));
            candidates.addAll(c.level.getEntitiesOfClass(Chicken.class, box));
        }
        List<Mob> valid = new ArrayList<>();
        for (Mob mob : candidates) {
            double distance = c.player.distanceTo(mob);
            if (mob.isAlive() && !mob.isNoAi() && distance >= 16.0D && distance <= 48.0D && Sight.canSee(c.player, mob.getEyePosition())) {
                valid.add(mob);
            }
        }
        if (!valid.isEmpty()) {
            return valid.get(c.random.nextInt(valid.size()));
        }
        // No hay ninguno a la vista: aparece uno solo, de la nada.
        BlockPos spot = SpawnSpots.find(c.level, c.player.position(), 20.0D, 36.0D, 60, c.random,
                p -> Sight.canSee(c.player, Vec3.atBottomCenterOf(p).add(0.0D, 1.0D, 0.0D)));
        if (spot == null) {
            return null;
        }
        Mob created;
        if (villager) {
            created = EntityType.VILLAGER.create(c.level);
        } else {
            created = EntityType.COW.create(c.level);
        }
        if (created == null) {
            return null;
        }
        created.moveTo(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D, c.random.nextFloat() * 360.0F, 0.0F);
        c.level.addFreshEntity(created);
        return created;
    }

    private static Gore.Kind kindOf(Mob mob) {
        if (mob instanceof Villager) {
            return Gore.Kind.VILLAGER;
        }
        if (mob instanceof Sheep) {
            return Gore.Kind.SHEEP;
        }
        if (mob instanceof Pig) {
            return Gore.Kind.PIG;
        }
        if (mob instanceof Chicken) {
            return Gore.Kind.CHICKEN;
        }
        return Gore.Kind.COW;
    }

    @Override
    public boolean tick(StoryContext c) {
        SirenHeadEntity creature = c.creature;
        if (failed || creature == null || victim == null) {
            return true;
        }
        Vec3 vp = Vec3.atBottomCenterOf(victimPos);
        if (!interrupted && (age == 2 || age == 9 || age == 16)) {
            SirenSounds.at(c.level, vp, ModSounds.FEAST.get(), 3.0F, 0.8F + c.random.nextFloat() * 0.2F);
        }
        if (age == 3 && victim.isAlive()) {
            victim.hurt(c.level.damageSources().mobAttack(creature), 3.0F);
            Gore.bloodBurst(c.level, vp.x, vp.y + 0.8D, vp.z, 20);
        }
        if (age == 5 && victim.isAlive()) {
            victim.kill();
            Gore.bloodBurst(c.level, vp.x, vp.y + 0.8D, vp.z, 40);
        }
        if (age == 8) {
            placeRemains(c);
        }
        if (!interrupted && c.player.distanceTo(creature) < 9.0D) {
            interrupted = true;
            creature.setFeasting(false);
            creature.forceState(SirenState.OBSERVING_PLAYER, 80);
        }
        if (interrupted) {
            return creature.getOverrideTicks() == 0;
        }
        return age >= 22;
    }

    private void placeRemains(StoryContext c) {
        if (!placed) {
            placed = true;
            Gore.corpse(c.level, victimPos, kind, c.random);
        }
    }

    @Override
    public void end(StoryContext c) {
        if (c.creature != null) {
            c.creature.setFeasting(false);
        }
        if (failed) {
            return;
        }
        if (victim != null && victim.isAlive()) {
            victim.kill();
        }
        placeRemains(c);
        releaseCreature(c);
    }
}
