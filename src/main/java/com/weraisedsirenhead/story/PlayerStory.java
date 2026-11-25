package com.weraisedsirenhead.story;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/** Progreso de la historia de UN jugador. Se guarda en StoryData. */
public class PlayerStory {
    public boolean active;
    public boolean completed;
    public int day = 1;
    /** Dia de Minecraft (dayTime / 24000) a partir del cual la historia pasa al dia siguiente. */
    public long nextDayAt;
    public UUID creature;
    /** Bit (dia-1)*3 + n = mision n de ese dia completada. */
    public int questMask;

    // ---- estado diario (se reinicia en cada dia nuevo)
    public boolean nightSignalDone;
    public boolean vanishStarted;
    public boolean vanishFound;
    public boolean voicesStarted;
    public boolean chaseStarted;
    public boolean chaseDone;
    public boolean knocksStarted;
    public boolean transformStarted;
    public boolean transformed;
    public boolean giftDone;
    public boolean feastDone;
    public boolean offeringDone;
    public boolean whisperDone;
    public long chaseEnd;
    public long lastKnockTick;
    public BlockPos investigatePos;
    public BlockPos voicePos;
    public BlockPos trailStart;
    public BlockPos trailEnd;
    public BlockPos lastKnockPos;

    public boolean isDone(int day, int n) {
        return (questMask & (1 << ((day - 1) * 3 + n))) != 0;
    }

    public boolean isDone(int n) {
        return isDone(day, n);
    }

    public void setDone(int day, int n) {
        questMask |= 1 << ((day - 1) * 3 + n);
    }

    public void resetDaily() {
        nightSignalDone = false;
        vanishStarted = false;
        vanishFound = false;
        voicesStarted = false;
        chaseStarted = false;
        chaseDone = false;
        knocksStarted = false;
        transformStarted = false;
        transformed = false;
        giftDone = false;
        feastDone = false;
        offeringDone = false;
        whisperDone = false;
        chaseEnd = 0L;
        lastKnockTick = 0L;
        investigatePos = null;
        voicePos = null;
        trailStart = null;
        trailEnd = null;
        lastKnockPos = null;
    }

    public void reset() {
        active = false;
        completed = false;
        day = 1;
        nextDayAt = 0L;
        creature = null;
        questMask = 0;
        resetDaily();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("active", active);
        tag.putBoolean("completed", completed);
        tag.putInt("day", day);
        tag.putLong("nextDayAt", nextDayAt);
        if (creature != null) {
            tag.putUUID("creature", creature);
        }
        tag.putInt("questMask", questMask);
        tag.putBoolean("nightSignalDone", nightSignalDone);
        tag.putBoolean("vanishStarted", vanishStarted);
        tag.putBoolean("vanishFound", vanishFound);
        tag.putBoolean("voicesStarted", voicesStarted);
        tag.putBoolean("chaseStarted", chaseStarted);
        tag.putBoolean("chaseDone", chaseDone);
        tag.putBoolean("knocksStarted", knocksStarted);
        tag.putBoolean("transformStarted", transformStarted);
        tag.putBoolean("transformed", transformed);
        tag.putBoolean("giftDone", giftDone);
        tag.putBoolean("feastDone", feastDone);
        tag.putBoolean("offeringDone", offeringDone);
        tag.putBoolean("whisperDone", whisperDone);
        tag.putLong("chaseEnd", chaseEnd);
        tag.putLong("lastKnockTick", lastKnockTick);
        putPos(tag, "investigatePos", investigatePos);
        putPos(tag, "voicePos", voicePos);
        putPos(tag, "trailStart", trailStart);
        putPos(tag, "trailEnd", trailEnd);
        putPos(tag, "lastKnockPos", lastKnockPos);
        return tag;
    }

    public static PlayerStory load(CompoundTag tag) {
        PlayerStory s = new PlayerStory();
        s.active = tag.getBoolean("active");
        s.completed = tag.getBoolean("completed");
        s.day = Math.max(1, Math.min(5, tag.getInt("day")));
        s.nextDayAt = tag.getLong("nextDayAt");
        if (tag.hasUUID("creature")) {
            s.creature = tag.getUUID("creature");
        }
        s.questMask = tag.getInt("questMask");
        s.nightSignalDone = tag.getBoolean("nightSignalDone");
        s.vanishStarted = tag.getBoolean("vanishStarted");
        s.vanishFound = tag.getBoolean("vanishFound");
        s.voicesStarted = tag.getBoolean("voicesStarted");
        s.chaseStarted = tag.getBoolean("chaseStarted");
        s.chaseDone = tag.getBoolean("chaseDone");
        s.knocksStarted = tag.getBoolean("knocksStarted");
        s.transformStarted = tag.getBoolean("transformStarted");
        s.transformed = tag.getBoolean("transformed");
        s.giftDone = tag.getBoolean("giftDone");
        s.feastDone = tag.getBoolean("feastDone");
        s.offeringDone = tag.getBoolean("offeringDone");
        s.whisperDone = tag.getBoolean("whisperDone");
        s.chaseEnd = tag.getLong("chaseEnd");
        s.lastKnockTick = tag.getLong("lastKnockTick");
        s.investigatePos = getPos(tag, "investigatePos");
        s.voicePos = getPos(tag, "voicePos");
        s.trailStart = getPos(tag, "trailStart");
        s.trailEnd = getPos(tag, "trailEnd");
        s.lastKnockPos = getPos(tag, "lastKnockPos");
        return s;
    }

    private static void putPos(CompoundTag tag, String key, BlockPos pos) {
        if (pos != null) {
            tag.putLong(key, pos.asLong());
        }
    }

    private static BlockPos getPos(CompoundTag tag, String key) {
        return tag.contains(key) ? BlockPos.of(tag.getLong(key)) : null;
    }
}
