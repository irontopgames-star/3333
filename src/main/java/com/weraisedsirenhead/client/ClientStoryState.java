package com.weraisedsirenhead.client;

import com.weraisedsirenhead.network.SyncStoryPacket;

/** Copia en el cliente del estado de la historia (lo lee el HUD). No usa clases exclusivas del cliente. */
public final class ClientStoryState {
    public static boolean active;
    public static boolean completed;
    public static boolean surviveDawn;
    public static int day = 1;
    public static int mask;
    public static int completedIndex = -1;
    public static long completedAtMs;

    public static void update(SyncStoryPacket packet) {
        active = packet.active();
        completed = packet.completed();
        surviveDawn = packet.surviveDawn();
        day = packet.day();
        mask = packet.mask();
        if (packet.justCompleted() >= 0) {
            completedIndex = packet.justCompleted();
            completedAtMs = System.currentTimeMillis();
        }
    }

    public static boolean isDone(int n) {
        return (mask & (1 << ((day - 1) * 3 + n))) != 0;
    }

    private ClientStoryState() {
    }
}
