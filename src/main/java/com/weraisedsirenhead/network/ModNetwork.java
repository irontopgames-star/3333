package com.weraisedsirenhead.network;

import com.weraisedsirenhead.SirenHeadMod;
import com.weraisedsirenhead.story.PlayerStory;
import com.weraisedsirenhead.story.StoryData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Canal de red del mod. */
public final class ModNetwork {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            SirenHeadMod.id("main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void register() {
        CHANNEL.registerMessage(0, SyncStoryPacket.class, SyncStoryPacket::encode, SyncStoryPacket::decode, SyncStoryPacket::handle);
    }

    /** Envia al jugador el estado de su historia. justCompleted = indice de la mision recien completada (o -1). */
    public static void sync(ServerPlayer player, PlayerStory story, int justCompleted) {
        SyncStoryPacket packet;
        if (story == null || !story.active) {
            packet = new SyncStoryPacket(false, false, false, 1, 0, -1);
        } else {
            boolean surviveDawn = story.day == 5 && story.transformed && !story.completed;
            packet = new SyncStoryPacket(true, story.completed, surviveDawn, story.day, story.questMask, justCompleted);
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sync(ServerPlayer player) {
        sync(player, StoryData.get(player.server).get(player.getUUID()), -1);
    }

    private ModNetwork() {
    }
}
