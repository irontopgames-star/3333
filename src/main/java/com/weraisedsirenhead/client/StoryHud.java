package com.weraisedsirenhead.client;

import com.weraisedsirenhead.SirenHeadMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/** HUD de misiones (esquina superior izquierda), aviso de mision completada y cartel del amanecer. */
public class StoryHud implements IGuiOverlay {
    private static final int[][] TICK = {{2, 4}, {3, 5}, {4, 6}, {5, 5}, {6, 4}, {7, 3}};

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || !ClientStoryState.active) {
            return;
        }
        Font font = mc.font;
        drawQuestPanel(g, font);
        drawCompletedBanner(g, font, width);
        if (ClientStoryState.surviveDawn && !ClientStoryState.completed) {
            drawDawnSign(g, font, width);
        }
        if (ClientStoryState.completed) {
            Component text = Component.translatable("hud.weraisedsirenhead.completed");
            g.drawCenteredString(font, text, width / 2, 14, 0xFFD700);
        }
    }

    private void drawQuestPanel(GuiGraphics g, Font font) {
        int day = ClientStoryState.day;
        Component title = Component.translatable("hud.weraisedsirenhead.day", day);
        Component[] quests = new Component[3];
        int textWidth = font.width(title);
        for (int i = 0; i < 3; i++) {
            quests[i] = Component.translatable("quest." + SirenHeadMod.MOD_ID + ".d" + day + ".q" + (i + 1));
            textWidth = Math.max(textWidth, font.width(quests[i]) + 15);
        }
        int x = 8;
        int y = 8;
        int panelHeight = 12 + 3 * 13 + 2;
        g.fill(x - 4, y - 4, x + textWidth + 6, y - 4 + panelHeight + 4, 0x99000000);
        g.drawString(font, title, x, y, 0xFFD24A, true);
        for (int i = 0; i < 3; i++) {
            int rowY = y + 14 + i * 13;
            boolean done = ClientStoryState.isDone(i);
            drawCheckbox(g, x, rowY - 1, done);
            g.drawString(font, quests[i], x + 14, rowY, done ? 0x55FF55 : 0xFFFFFF, true);
        }
    }

    private void drawCheckbox(GuiGraphics g, int x, int y, boolean done) {
        g.fill(x, y, x + 10, y + 10, 0xFF8A8A8A);
        g.fill(x + 1, y + 1, x + 9, y + 9, done ? 0xFF1F5E1F : 0xFF151515);
        if (done) {
            for (int[] p : TICK) {
                g.fill(x + p[0], y + p[1], x + p[0] + 1, y + p[1] + 2, 0xFF55FF55);
            }
        }
    }

    private void drawCompletedBanner(GuiGraphics g, Font font, int width) {
        if (ClientStoryState.completedIndex < 0) {
            return;
        }
        long age = System.currentTimeMillis() - ClientStoryState.completedAtMs;
        if (age < 0 || age > 4500L) {
            return;
        }
        int alpha = age > 3500L ? (int) (255 * (4500L - age) / 1000.0D) : 255;
        if (alpha < 8) {
            return;
        }
        Component line1 = Component.translatable("hud.weraisedsirenhead.mission_complete");
        Component line2 = Component.translatable("quest." + SirenHeadMod.MOD_ID + ".d" + ClientStoryState.day
                + ".q" + (ClientStoryState.completedIndex + 1));
        g.pose().pushPose();
        g.pose().translate(width / 2.0F, 44.0F, 0.0F);
        g.pose().scale(1.4F, 1.4F, 1.0F);
        g.drawCenteredString(font, line1, 0, 0, (alpha << 24) | 0x55FF55);
        g.pose().popPose();
        g.drawCenteredString(font, line2, width / 2, 62, (alpha << 24) | 0xFFFFFF);
    }

    private void drawDawnSign(GuiGraphics g, Font font, int width) {
        boolean blink = (System.currentTimeMillis() / 600L) % 2L == 0L;
        Component text = Component.translatable("hud.weraisedsirenhead.survive_dawn");
        int textWidth = (int) (font.width(text) * 1.3F);
        int x = width / 2;
        g.fill(x - textWidth / 2 - 8, 6, x + textWidth / 2 + 8, 26, 0xAA200000);
        g.pose().pushPose();
        g.pose().translate(x, 12.0F, 0.0F);
        g.pose().scale(1.3F, 1.3F, 1.0F);
        g.drawCenteredString(font, text, 0, 0, blink ? 0xFF3030 : 0xFFFFFF);
        g.pose().popPose();
    }
}
