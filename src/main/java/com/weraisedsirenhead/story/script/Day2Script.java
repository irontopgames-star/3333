package com.weraisedsirenhead.story.script;

import com.weraisedsirenhead.director.HorrorDirector;
import com.weraisedsirenhead.director.events.FeastEvent;
import com.weraisedsirenhead.director.events.GiftEvent;
import com.weraisedsirenhead.entity.CreatureActions;
import com.weraisedsirenhead.entity.SirenHeadEntity;
import com.weraisedsirenhead.entity.SirenState;
import com.weraisedsirenhead.registry.ModSounds;
import com.weraisedsirenhead.sound.SirenSounds;
import com.weraisedsirenhead.story.PlayerStory;
import com.weraisedsirenhead.story.StoryContext;
import com.weraisedsirenhead.world.SpawnSpots;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;

/**
 * DIA 2 - Primeras senales extranas.
 * M1 Alimentarlo de nuevo (ver SirenHeadEntity#feed).
 * M2 Conseguir materiales para su refugio (32 bloques entre madera, troncos, piedra o cobblestone).
 * M3 Investigar los sonidos extranos de la noche: a la noche aparece estatica a unos 30 bloques y
 *    la criatura esta parada ahi, mirando.
 */
public class Day2Script implements DayScript {
    private static final int MATERIALS_NEEDED = 32;

    @Override
    public void tick(StoryContext c) {
        PlayerStory story = c.story;
        SirenHeadEntity creature = c.creature;

        if (!story.isDone(1) && countMaterials(c.player) >= MATERIALS_NEEDED) {
            c.complete(1);
        }

        // De dia: la cria devora una vaca (o lo que haya) delante tuyo. Al anochecer deja un regalo en tu puerta.
        if (!story.feastDone && creature != null && c.timeOfDay >= 4500L && c.timeOfDay < 11000L
                && c.runtime.active == null && c.player.distanceTo(creature) < 40.0F
                && HorrorDirector.forceStart(c, new FeastEvent(false))) {
            story.feastDone = true;
        }
        if (!story.giftDone && creature != null && c.timeOfDay >= 12000L && c.timeOfDay < 13300L
                && c.runtime.active == null && HorrorDirector.forceStart(c, new GiftEvent())) {
            story.giftDone = true;
        }

        if (!story.nightSignalDone && creature != null && c.runtime.active == null
                && c.timeOfDay >= 13500L && c.timeOfDay < 22000L) {
            BlockPos spot = SpawnSpots.find(c.level, c.player.position(), 24.0D, 34.0D, 40, c.random, null);
            if (spot != null) {
                story.nightSignalDone = true;
                story.investigatePos = spot;
                CreatureActions.teleport(creature, Vec3.atBottomCenterOf(spot), c.player);
                creature.forceState(SirenState.OBSERVING_PLAYER, 12000);
            }
        }

        if (story.investigatePos != null) {
            Vec3 source = Vec3.atBottomCenterOf(story.investigatePos);
            if (c.player.distanceToSqr(source) < 64.0D) {
                c.complete(2);
                story.investigatePos = null;
                if (creature != null) {
                    creature.clearOverride();
                    creature.setSirenState(SirenState.FOLLOWING_PLAYER);
                }
            } else if (c.runtime.beat % 10 == 0) {
                SirenSounds.at(c.level, source.add(0.0D, 1.5D, 0.0D), ModSounds.STATIC.get(), 2.5F, 0.8F);
            }
        }
    }

    private static int countMaterials(Player player) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && (stack.is(ItemTags.PLANKS) || stack.is(ItemTags.LOGS)
                    || stack.is(Tags.Items.COBBLESTONE) || stack.is(Tags.Items.STONE))) {
                total += stack.getCount();
            }
        }
        return total;
    }
}
