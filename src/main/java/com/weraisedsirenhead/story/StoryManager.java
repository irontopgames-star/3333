package com.weraisedsirenhead.story;

import com.weraisedsirenhead.SirenHeadMod;
import com.weraisedsirenhead.director.HorrorDirector;
import com.weraisedsirenhead.entity.CreatureActions;
import com.weraisedsirenhead.entity.SirenHeadEntity;
import com.weraisedsirenhead.network.ModNetwork;
import com.weraisedsirenhead.quest.QuestManager;
import com.weraisedsirenhead.registry.ModEntities;
import com.weraisedsirenhead.registry.ModSounds;
import com.weraisedsirenhead.sound.SirenSounds;
import com.weraisedsirenhead.story.script.DayScripts;
import com.weraisedsirenhead.world.SpawnSpots;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Logica central de la historia: iniciar, reiniciar, avanzar de dia automaticamente,
 * mantener viva a la criatura y llamar a los scripts de cada dia y al director de terror.
 */
public final class StoryManager {

    public enum StartResult { STARTED, ALREADY_ACTIVE, WRONG_DIMENSION, NO_SPACE }

    // ------------------------------------------------------------------ inicio y reinicio

    public static StartResult start(ServerPlayer player) {
        MinecraftServer server = player.server;
        ServerLevel overworld = server.overworld();
        if (player.level() != overworld) {
            return StartResult.WRONG_DIMENSION;
        }
        StoryData data = StoryData.get(server);
        PlayerStory story = data.getOrCreate(player.getUUID());
        if (story.active && !story.completed) {
            return StartResult.ALREADY_ACTIVE;
        }
        story.reset();
        SirenHeadEntity baby = spawnCreature(overworld, player, story, 1, true);
        if (baby == null) {
            return StartResult.NO_SPACE;
        }
        story.active = true;
        long gameDay = StoryClock.gameDay(overworld);
        // Si empezas pasada la mitad del dia, el dia 1 dura hasta la manana siguiente a la proxima.
        story.nextDayAt = gameDay + (StoryClock.timeOfDay(overworld) > 6000L ? 2L : 1L);
        StoryRuntime.clear(player.getUUID());
        data.setDirty();
        showTitle(player, Component.translatable("title.weraisedsirenhead.day.1"),
                Component.translatable("subtitle.weraisedsirenhead.day.1"));
        ModNetwork.sync(player, story, -1);
        SirenHeadMod.debug("Historia iniciada para {}", player.getGameProfile().getName());
        return StartResult.STARTED;
    }

    public static void reset(ServerPlayer player) {
        MinecraftServer server = player.server;
        StoryData data = StoryData.get(server);
        PlayerStory story = data.get(player.getUUID());
        if (story != null) {
            SirenHeadEntity creature = lookup(server.overworld(), story);
            if (creature != null) {
                creature.discard();
            }
            story.reset();
        }
        StoryRuntime.clear(player.getUUID());
        data.setDirty();
        ModNetwork.sync(player, story, -1);
    }

    /** Solo para pruebas (/sirenhead day N): salta a un dia. */
    public static void setDay(ServerPlayer player, PlayerStory story, int day) {
        ServerLevel overworld = player.server.overworld();
        story.day = Math.max(1, Math.min(5, day));
        story.resetDaily();
        story.nextDayAt = StoryClock.gameDay(overworld) + 1L;
        StoryRuntime.of(player.getUUID()).resetDay();
        StoryData.get(player.server).setDirty();
        showTitle(player, Component.translatable("title.weraisedsirenhead.day." + story.day),
                Component.translatable("subtitle.weraisedsirenhead.day." + story.day));
        ModNetwork.sync(player, story, -1);
    }

    // ------------------------------------------------------------------ tick

    public static void tickPlayer(ServerPlayer player, PlayerStory story, ServerLevel overworld) {
        long gameDay = StoryClock.gameDay(overworld);
        if (story.nextDayAt > gameDay + 2L) {
            story.nextDayAt = gameDay + 2L;
        }
        if (gameDay >= story.nextDayAt) {
            story.nextDayAt = gameDay + 1L;
            advanceDay(player, story, overworld);
            if (story.completed) {
                return;
            }
        }
        if (player.level() != overworld || player.tickCount % 10 != 0) {
            return;
        }
        StoryRuntime.State runtime = StoryRuntime.of(player.getUUID());
        runtime.beat++;
        SirenHeadEntity creature = findCreature(overworld, player, story, runtime);
        StoryContext ctx = new StoryContext(overworld, player, story, creature, runtime);
        if (creature != null && creature.getStage() != desiredStage(story)) {
            creature.setStage(desiredStage(story));
        }
        DayScripts.get(story.day).tick(ctx);
        HorrorDirector.tick(ctx);
        if (runtime.beat % 10 == 0) {
            StoryData.get(player.server).setDirty();
        }
    }

    private static void advanceDay(ServerPlayer player, PlayerStory story, ServerLevel overworld) {
        StoryRuntime.State runtime = StoryRuntime.of(player.getUUID());
        SirenHeadEntity creature = lookup(overworld, story);
        if (story.day >= 5) {
            victory(player, story, overworld, creature);
            return;
        }
        story.day++;
        story.resetDaily();
        runtime.resetDay();
        if (creature != null) {
            creature.clearOverride();
            creature.setStay(false);
            creature.setChaseCooldown(0);
            creature.setStage(desiredStage(story));
            if (player.level() == overworld && creature.distanceTo(player) > 40.0F) {
                CreatureActions.teleportNear(creature, player, 10.0D, 16.0D);
            }
        }
        showTitle(player, Component.translatable("title.weraisedsirenhead.day." + story.day),
                Component.translatable("subtitle.weraisedsirenhead.day." + story.day));
        StoryContext ctx = new StoryContext(overworld, player, story, creature, runtime);
        DayScripts.get(story.day).onDayStart(ctx);
        StoryData.get(player.server).setDirty();
        ModNetwork.sync(player, story, -1);
        SirenHeadMod.debug("{} pasa al dia {}", player.getGameProfile().getName(), story.day);
    }

    /** Amanecer del dia 5: sobreviviste. */
    private static void victory(ServerPlayer player, PlayerStory story, ServerLevel overworld, SirenHeadEntity creature) {
        story.completed = true;
        if (creature != null) {
            SirenSounds.at(overworld, creature.position(), ModSounds.TRANSFORM.get(), 3.0F, 1.4F);
            overworld.sendParticles(ParticleTypes.POOF, creature.getX(), creature.getY() + 4.0D, creature.getZ(),
                    30, 1.0D, 3.0D, 1.0D, 0.05D);
            creature.discard();
        }
        story.creature = null;
        QuestManager.complete(player, story, 2);
        showTitle(player, Component.translatable("title.weraisedsirenhead.victory"),
                Component.translatable("subtitle.weraisedsirenhead.victory"));
        StoryData.get(player.server).setDirty();
        ModNetwork.sync(player, story, 2);
    }

    // ------------------------------------------------------------------ criatura

    public static int desiredStage(PlayerStory story) {
        if (story.day == 5) {
            return story.transformed ? 5 : 4;
        }
        return story.day;
    }

    private static SirenHeadEntity lookup(ServerLevel level, PlayerStory story) {
        if (story.creature == null) {
            return null;
        }
        Entity entity = level.getEntity(story.creature);
        if (entity instanceof SirenHeadEntity siren && siren.isAlive()) {
            return siren;
        }
        return null;
    }

    private static SirenHeadEntity findCreature(ServerLevel level, ServerPlayer player, PlayerStory story, StoryRuntime.State runtime) {
        SirenHeadEntity creature = lookup(level, story);
        if (creature != null) {
            runtime.missingCreature = 0;
            return creature;
        }
        // Si pasan ~30 segundos sin encontrarla (murio o se perdio), reaparece cerca del jugador.
        if (++runtime.missingCreature >= 60) {
            runtime.missingCreature = 0;
            return spawnCreature(level, player, story, desiredStage(story), false);
        }
        return null;
    }

    private static SirenHeadEntity spawnCreature(ServerLevel level, ServerPlayer player, PlayerStory story, int stage, boolean forest) {
        BlockPos spot = forest
                ? SpawnSpots.findForest(level, player.position(), 28.0D, 60.0D, level.getRandom())
                : SpawnSpots.find(level, player.position(), 14.0D, 22.0D, 60, level.getRandom(), null);
        if (spot == null) {
            return null;
        }
        SirenHeadEntity creature = ModEntities.SIREN_HEAD.get().create(level);
        if (creature == null) {
            return null;
        }
        Vec3 pos = Vec3.atBottomCenterOf(spot);
        creature.moveTo(pos.x, pos.y, pos.z, level.getRandom().nextFloat() * 360.0F, 0.0F);
        creature.setOwner(player.getUUID());
        creature.setStage(stage);
        story.creature = creature.getUUID();
        level.addFreshEntity(creature);
        return creature;
    }

    // ------------------------------------------------------------------ eventos del jugador

    /** El jugador alimento a la criatura: completa la mision de comida del dia 1 o del dia 2. */
    public static void onFed(ServerPlayer player) {
        PlayerStory story = StoryData.get(player.server).get(player.getUUID());
        if (story == null || !story.active || story.completed) {
            return;
        }
        if (story.day == 1) {
            QuestManager.complete(player, story, 1);
        } else if (story.day == 2) {
            QuestManager.complete(player, story, 0);
        }
    }

    public static void showTitle(ServerPlayer player, Component title, Component subtitle) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 80, 25));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    public static PlayerStory storyOf(ServerPlayer player) {
        return StoryData.get(player.server).get(player.getUUID());
    }

    public static UUID idOf(ServerPlayer player) {
        return player.getUUID();
    }

    private StoryManager() {
    }
}
