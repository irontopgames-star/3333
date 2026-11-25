package com.weraisedsirenhead.story;

import com.weraisedsirenhead.entity.SirenHeadEntity;
import com.weraisedsirenhead.quest.QuestManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Todo lo que necesita un script de dia o un evento de terror en un momento dado. */
public final class StoryContext {
    public final ServerLevel level;
    public final ServerPlayer player;
    public final PlayerStory story;
    /** Puede ser null si la criatura no esta cargada o murio. */
    public final SirenHeadEntity creature;
    public final StoryRuntime.State runtime;
    public final RandomSource random;
    public final long timeOfDay;
    public final long gameTime;

    public StoryContext(ServerLevel level, ServerPlayer player, PlayerStory story, SirenHeadEntity creature, StoryRuntime.State runtime) {
        this.level = level;
        this.player = player;
        this.story = story;
        this.creature = creature;
        this.runtime = runtime;
        this.random = level.getRandom();
        this.timeOfDay = StoryClock.timeOfDay(level);
        this.gameTime = level.getGameTime();
    }

    public int day() {
        return story.day;
    }

    public boolean isNight() {
        return StoryClock.isNight(level);
    }

    public void complete(int quest) {
        QuestManager.complete(player, story, quest);
    }

    /** "Casa" del jugador: su cama de reaparicion si esta cerca; si no, donde esta parado. */
    public Vec3 home() {
        BlockPos respawn = player.getRespawnPosition();
        if (respawn != null && player.getRespawnDimension() == Level.OVERWORLD
                && respawn.distSqr(player.blockPosition()) < 64.0D * 64.0D) {
            return Vec3.atBottomCenterOf(respawn);
        }
        return player.position();
    }
}
