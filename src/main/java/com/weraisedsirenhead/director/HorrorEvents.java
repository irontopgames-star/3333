package com.weraisedsirenhead.director;

import com.weraisedsirenhead.director.events.AlarmEvent;
import com.weraisedsirenhead.director.events.AnimalMimicEvent;
import com.weraisedsirenhead.director.events.BreathEvent;
import com.weraisedsirenhead.director.events.ChaseEvent;
import com.weraisedsirenhead.director.events.FeastEvent;
import com.weraisedsirenhead.director.events.FootstepsEvent;
import com.weraisedsirenhead.director.events.GiftEvent;
import com.weraisedsirenhead.director.events.HumanVoiceEvent;
import com.weraisedsirenhead.director.events.KnockEvent;
import com.weraisedsirenhead.director.events.LureEvent;
import com.weraisedsirenhead.director.events.NightApproachEvent;
import com.weraisedsirenhead.director.events.RadioEvent;
import com.weraisedsirenhead.director.events.StaticBurstEvent;
import com.weraisedsirenhead.director.events.WatchEvent;

import java.util.List;

/** Catalogo de eventos de terror aleatorios. */
public final class HorrorEvents {

    /** Instancias nuevas de todos los eventos (para evaluar sus pesos y sortear uno). */
    public static List<HorrorEvent> createAll() {
        return List.of(
                new StaticBurstEvent(),
                new AnimalMimicEvent(),
                new AlarmEvent(),
                new HumanVoiceEvent(),
                new WatchEvent(WatchEvent.Mode.DISTANT),
                new WatchEvent(WatchEvent.Mode.TREES),
                new WatchEvent(WatchEvent.Mode.WINDOW),
                new NightApproachEvent(),
                new KnockEvent(),
                new ChaseEvent(),
                new LureEvent(),
                new BreathEvent(),
                new FootstepsEvent(),
                new FeastEvent(false),
                new FeastEvent(true),
                new GiftEvent(),
                new RadioEvent());
    }

    private HorrorEvents() {
    }
}
