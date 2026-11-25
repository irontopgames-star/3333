package com.weraisedsirenhead.story;

import com.weraisedsirenhead.director.HorrorEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Estado temporal (no se guarda) de cada jugador con historia activa: temporizadores y evento en curso. */
public final class StoryRuntime {
    public static final class State {
        public int beat;
        public int missingCreature;
        public long nextEvent;
        public HorrorEvent active;
        public Class<?> lastEvent;
        public boolean bolt1;
        public boolean bolt2;

        public void resetDay() {
            nextEvent = 0L;
            active = null;
            bolt1 = false;
            bolt2 = false;
        }
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    public static State of(UUID player) {
        return STATES.computeIfAbsent(player, id -> new State());
    }

    public static void clear(UUID player) {
        STATES.remove(player);
    }

    public static void clearAll() {
        STATES.clear();
    }

    private StoryRuntime() {
    }
}
