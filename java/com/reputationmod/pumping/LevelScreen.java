package com.reputationmod.client;

import com.reputationmod.ReputationMod;
import com.reputationmod.stamina.ClientStaminaLevelData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class LevelScreen extends Screen {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(ReputationMod.MOD_ID, "textures/gui/level_screen.png");
    private static final int WIDTH = 176;
    private static final int HEIGHT = 166;

    public LevelScreen() {
        super(Component.translatable("screen.reputationmod.levels"));
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Полностью убираем стандартный фон
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Не вызываем renderBackground, он пустой
        int x = (this.width - WIDTH) / 2;
        int y = (this.height - HEIGHT) / 2;

        graphics.blit(BACKGROUND, x, y, 0, 0, WIDTH, HEIGHT);

        int level = ClientStaminaLevelData.getLevel();
        int exp = ClientStaminaLevelData.getExperience();
        int maxStamina = ClientStaminaLevelData.getMaxStamina();
        int nextExp = (level + 1) * 1000;
        int prevExp = level * 1000;
        float progress = (level >= 5) ? 1.0f : (float)(exp - prevExp) / (nextExp - prevExp);

        graphics.drawString(this.font, Component.literal("§lУровень стамины: §e" + level), x + 10, y + 20, 0xFFFFFF);
        graphics.drawString(this.font, Component.literal("Текущая стамина: " + maxStamina), x + 10, y + 35, 0xFFFFFF);
        graphics.drawString(this.font, Component.literal("Опыт: " + exp + " / " + nextExp), x + 10, y + 50, 0xFFFFFF);

        int barX = x + 10;
        int barY = y + 65;
        int barWidth = 156;
        int barHeight = 10;
        graphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF333333);
        if (progress > 0) {
            graphics.fill(barX, barY, barX + (int)(barWidth * progress), barY + barHeight, 0xFF00AA00);
        }

        // Не вызываем super.render, чтобы избежать лишнего фона
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}