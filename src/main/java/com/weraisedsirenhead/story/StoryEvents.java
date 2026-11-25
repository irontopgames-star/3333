package com.weraisedsirenhead.story;

import com.weraisedsirenhead.SirenHeadMod;
import com.weraisedsirenhead.command.StoryCommands;
import com.weraisedsirenhead.config.SirenHeadConfig;
import com.weraisedsirenhead.network.ModNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Engancha la historia a los eventos de Forge (bus del juego). */
@Mod.EventBusSubscriber(modid = SirenHeadMod.MOD_ID)
public final class StoryEvents {

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)
                || level.dimension() != Level.OVERWORLD) {
            return;
        }
        MinecraftServer server = level.getServer();
        StoryData data = StoryData.get(server);
        boolean finalNight = false;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerStory story = data.get(player.getUUID());
            if (story == null || !story.active || story.completed) {
                continue;
            }
            StoryManager.tickPlayer(player, story, level);
            if (story.day == 5 && story.transformed && !story.completed
                    && StoryClock.isNight(level) && StoryClock.timeOfDay(level) < 23500L) {
                finalNight = true;
            }
        }
        int speed = SirenHeadConfig.FINAL_NIGHT_SPEED.get();
        if (finalNight && speed > 1) {
            // La noche del ultimo dia pasa mas rapido: se le suman ticks extra al reloj del mundo.
            level.setDayTime(level.getDayTime() + (speed - 1));
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ModNetwork.sync(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ModNetwork.sync(player);
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ModNetwork.sync(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        StoryRuntime.clear(event.getEntity().getUUID());
    }

    /** El ultimo dia no se puede dormir: la noche hay que sobrevivirla. */
    @SubscribeEvent
    public static void onSleep(PlayerSleepInBedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        PlayerStory story = StoryData.get(player.server).get(player.getUUID());
        if (story != null && story.active && !story.completed && story.day == 5) {
            event.setResult(Player.BedSleepingProblem.NOT_POSSIBLE_NOW);
            player.displayClientMessage(Component.translatable("message.weraisedsirenhead.no_sleep"), true);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        StoryRuntime.clearAll();
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        StoryCommands.register(event.getDispatcher());
    }

    private StoryEvents() {
    }
}
