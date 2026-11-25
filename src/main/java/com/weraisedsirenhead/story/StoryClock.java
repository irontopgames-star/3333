package com.weraisedsirenhead.story;

import net.minecraft.world.level.Level;

/** Utilidades de tiempo de Minecraft. Un dia = 24000 ticks; la noche va de 13000 a 23000. */
public final class StoryClock {
    public static final long DAY_LENGTH = 24000L;

    public static long timeOfDay(Level level) {
        return level.getDayTime() % DAY_LENGTH;
    }

    public static long gameDay(Level level) {
        return level.getDayTime() / DAY_LENGTH;
    }

    public static boolean isNight(Level level) {
        long t = timeOfDay(level);
        return t >= 13000L && t < 23000L;
    }

    private StoryClock() {
    }
}
