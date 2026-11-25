package com.weraisedsirenhead.entity;

/** Los estados de la IA de Siren Head. */
public enum SirenState {
    IDLE,
    FOLLOWING_PLAYER,
    EATING,
    SLEEPING,
    WANDERING,
    OBSERVING_PLAYER,
    HIDING,
    STALKING,
    SEARCHING,
    CHASING,
    ATTACKING;

    public static SirenState byId(int id) {
        SirenState[] all = values();
        return all[Math.max(0, Math.min(all.length - 1, id))];
    }
}
