package com.weraisedsirenhead.quest;

import com.weraisedsirenhead.network.ModNetwork;
import com.weraisedsirenhead.story.PlayerStory;
import com.weraisedsirenhead.story.StoryData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Misiones: 3 por dia (indices 0, 1 y 2). Cada script de dia comprueba sus condiciones
 * y llama a complete(); aca se guarda el progreso y se avisa al HUD del cliente.
 */
public final class QuestManager {

    public static void complete(ServerPlayer player, PlayerStory story, int n) {
        if (n < 0 || n > 2 || story.isDone(n)) {
            return;
        }
        story.setDone(story.day, n);
        StoryData.get(player.server).setDirty();
        player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.MASTER, 0.8F, 1.0F);
        ModNetwork.sync(player, story, n);
    }

    private QuestManager() {
    }
}
