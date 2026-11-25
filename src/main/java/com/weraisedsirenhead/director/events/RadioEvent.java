package com.weraisedsirenhead.director.events;

import com.weraisedsirenhead.SirenHeadMod;
import com.weraisedsirenhead.director.HorrorEvent;
import com.weraisedsirenhead.registry.ModSounds;
import com.weraisedsirenhead.sound.SirenSounds;
import com.weraisedsirenhead.story.StoryContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * Transmisiones de radio: estatica y un mensaje en el chat, a veces con tu nombre.
 * Cuanto mas avanza la historia, mas directo y amenazante.
 */
public class RadioEvent extends HorrorEvent {
    /** Cantidad de mensajes por dia (indice = dia). Ver radio.weraisedsirenhead.dN.K en los archivos de idioma. */
    private static final int[] COUNTS = {0, 2, 3, 4, 4, 3};

    @Override
    public int weight(StoryContext c) {
        return c.day() >= 4 ? 3 : c.day() >= 2 ? 2 : 0;
    }

    @Override
    public void start(StoryContext c) {
        SirenSounds.around(c.player, ModSounds.STATIC.get(), 2.0D, 5.0D, 1.0F, 1.0F);
    }

    @Override
    public boolean tick(StoryContext c) {
        if (age == 2) {
            send(c, c.day(), 1 + c.random.nextInt(COUNTS[Math.max(1, Math.min(5, c.day()))]));
        }
        return age >= 3;
    }

    /** Envia al jugador el mensaje de radio numero index del dia indicado. */
    public static void send(StoryContext c, int day, int index) {
        String key = "radio." + SirenHeadMod.MOD_ID + ".d" + day + "." + index;
        Component garble = Component.literal("\u2592\u2592 ").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.OBFUSCATED);
        Component tag = Component.literal("[Radio] ").withStyle(ChatFormatting.DARK_GRAY);
        Component text = Component.translatable(key, c.player.getName()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
        c.player.sendSystemMessage(Component.empty().append(tag).append(garble).append(text));
    }
}
