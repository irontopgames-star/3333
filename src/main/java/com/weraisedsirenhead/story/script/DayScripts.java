package com.weraisedsirenhead.story.script;

/** Los cinco guiones, uno por dia. */
public final class DayScripts {
    private static final DayScript[] SCRIPTS = {
            new Day1Script(), new Day2Script(), new Day3Script(), new Day4Script(), new Day5Script()
    };

    public static DayScript get(int day) {
        return SCRIPTS[Math.max(1, Math.min(5, day)) - 1];
    }

    private DayScripts() {
    }
}
