package com.weraisedsirenhead.story;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Datos guardados en el mundo (Overworld): una historia por jugador.
 * Se escribe en data/weraisedsirenhead_story.dat, asi sobrevive a cerrar el mundo.
 */
public class StoryData extends SavedData {
    private static final String NAME = "weraisedsirenhead_story";

    private final Map<UUID, PlayerStory> stories = new HashMap<>();

    public static StoryData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(StoryData::load, StoryData::new, NAME);
    }

    public static StoryData load(CompoundTag tag) {
        StoryData data = new StoryData();
        ListTag list = tag.getList("stories", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (entry.hasUUID("player")) {
                data.stories.put(entry.getUUID("player"), PlayerStory.load(entry.getCompound("story")));
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, PlayerStory> e : stories.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("player", e.getKey());
            entry.put("story", e.getValue().save());
            list.add(entry);
        }
        tag.put("stories", list);
        return tag;
    }

    public PlayerStory get(UUID player) {
        return stories.get(player);
    }

    public PlayerStory getOrCreate(UUID player) {
        return stories.computeIfAbsent(player, id -> new PlayerStory());
    }
}
