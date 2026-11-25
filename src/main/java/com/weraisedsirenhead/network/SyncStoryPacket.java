package com.weraisedsirenhead.network;

import com.weraisedsirenhead.client.ClientStoryState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> cliente: estado de la historia para el HUD de misiones. */
public record SyncStoryPacket(boolean active, boolean completed, boolean surviveDawn, int day, int mask, int justCompleted) {

    public static void encode(SyncStoryPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.active);
        buf.writeBoolean(msg.completed);
        buf.writeBoolean(msg.surviveDawn);
        buf.writeVarInt(msg.day);
        buf.writeVarInt(msg.mask);
        buf.writeVarInt(msg.justCompleted + 1);
    }

    public static SyncStoryPacket decode(FriendlyByteBuf buf) {
        boolean active = buf.readBoolean();
        boolean completed = buf.readBoolean();
        boolean surviveDawn = buf.readBoolean();
        int day = buf.readVarInt();
        int mask = buf.readVarInt();
        int justCompleted = buf.readVarInt() - 1;
        return new SyncStoryPacket(active, completed, surviveDawn, day, mask, justCompleted);
    }

    public static void handle(SyncStoryPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientStoryState.update(msg));
        ctx.get().setPacketHandled(true);
    }
}
