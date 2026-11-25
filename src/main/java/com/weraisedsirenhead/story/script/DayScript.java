package com.weraisedsirenhead.story.script;

import com.weraisedsirenhead.story.StoryContext;

/** Guion de un dia de la historia: eventos programados y condiciones de las 3 misiones. */
public interface DayScript {

    /** Se llama una vez cuando empieza el dia. */
    default void onDayStart(StoryContext c) {
    }

    /** Se llama cada 10 ticks mientras el dia esta activo. */
    void tick(StoryContext c);
}
