package com.weraisedsirenhead.director;

import com.weraisedsirenhead.SirenHeadMod;
import com.weraisedsirenhead.config.SirenHeadConfig;
import com.weraisedsirenhead.story.StoryContext;
import com.weraisedsirenhead.story.StoryRuntime;

import java.util.List;

/**
 * Director de terror: elige que evento puede pasar y cuando. Entre evento y evento hay silencios largos
 * (el silencio tambien es parte del miedo) que se acortan con los dias. Los eventos se sortean con pesos
 * y nunca se repite el mismo dos veces seguidas con facilidad, asi que cada partida es distinta.
 */
public final class HorrorDirector {

    public static void tick(StoryContext c) {
        if (c.day() < 2 || c.player.isSleeping()) {
            return;
        }
        StoryRuntime.State runtime = c.runtime;
        if (runtime.active != null) {
            HorrorEvent event = runtime.active;
            event.age++;
            if (event.tick(c)) {
                event.end(c);
                runtime.lastEvent = event.getClass();
                runtime.active = null;
                runtime.nextEvent = c.gameTime + silence(c);
            }
            return;
        }
        // Mientras corre una escena guionada (transformacion del dia 5, primera persecucion del dia 4) no hay eventos al azar.
        if ((c.story.transformStarted && !c.story.transformed) || (c.story.chaseStarted && !c.story.chaseDone)) {
            return;
        }
        if (runtime.nextEvent == 0L) {
            runtime.nextEvent = c.gameTime + silence(c) / 2L;
            return;
        }
        if (c.gameTime < runtime.nextEvent) {
            return;
        }
        HorrorEvent chosen = choose(c);
        if (chosen == null) {
            runtime.nextEvent = c.gameTime + 600L;
            return;
        }
        forceStart(c, chosen);
    }

    /** Arranca un evento puntual (lo usan los guiones de dia). Devuelve false si ya habia otro en curso. */
    public static boolean forceStart(StoryContext c, HorrorEvent event) {
        if (c.runtime.active != null) {
            return false;
        }
        event.age = 0;
        c.runtime.active = event;
        event.start(c);
        SirenHeadMod.debug("Evento de terror: {}", event.getClass().getSimpleName());
        return true;
    }

    private static HorrorEvent choose(StoryContext c) {
        List<HorrorEvent> all = HorrorEvents.createAll();
        int[] weights = new int[all.size()];
        int total = 0;
        for (int i = 0; i < all.size(); i++) {
            int w = all.get(i).weight(c);
            if (w > 0 && all.get(i).getClass() == c.runtime.lastEvent) {
                w = Math.max(1, w / 3);
            }
            weights[i] = w;
            total += w;
        }
        if (total <= 0) {
            return null;
        }
        int roll = c.random.nextInt(total);
        for (int i = 0; i < all.size(); i++) {
            roll -= weights[i];
            if (roll < 0) {
                return all.get(i);
            }
        }
        return null;
    }

    /** Ticks de silencio entre eventos segun el dia. */
    private static long silence(StoryContext c) {
        int min;
        int max;
        switch (c.day()) {
            case 2:
                min = 1200;
                max = 2400;
                break;
            case 3:
                min = 900;
                max = 1800;
                break;
            case 4:
                min = 600;
                max = 1200;
                break;
            default:
                if (c.story.transformed) {
                    min = 300;
                    max = 700;
                } else {
                    min = 600;
                    max = 1200;
                }
                break;
        }
        double base = min + c.random.nextInt(max - min + 1);
        return (long) (base / SirenHeadConfig.EVENT_FREQUENCY.get());
    }

    private HorrorDirector() {
    }
}
