package com.reputationmod.stamina;

import com.reputationmod.ReputationMod;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = ReputationMod.MOD_ID)
public class StaminaOverlay {

    // Для плавной анимации полоски
    private static float animatedStamina = -1; // -1 означает, что нужно инициализировать
    private static long lastRenderTime = System.currentTimeMillis();
    private static final float ANIMATION_SPEED = 200f; // единиц в секунду

    @SubscribeEvent
    public static void registerOverlay(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(ReputationMod.MOD_ID, "stamina_overlay"),
                StaminaOverlay::renderStaminaBar
        );
    }

    private static void renderStaminaBar(GuiGraphics graphics, DeltaTracker deltaTracker) {
        // Если HUD отключён – не рисуем
        if (!StaminaClientSettings.isHudEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.isCreative() || mc.player.isSpectator()) return;

        var staminaData = mc.player.getData(StaminaAttachment.STAMINA_DATA);
        int realStamina = staminaData.getStamina();
        int maxStamina = staminaData.getMaxStamina();

        // Инициализация анимированного значения при первом рендере или смене игрока
        if (animatedStamina < 0) {
            animatedStamina = realStamina;
            lastRenderTime = System.currentTimeMillis();
        }

        // Плавное движение к реальному значению
        long now = System.currentTimeMillis();
        float deltaSec = Math.min((now - lastRenderTime) / 1000f, 0.05f); // максимум 0.05 сек за кадр (20 FPS) для избежания рывков
        lastRenderTime = now;

        if (Math.abs(animatedStamina - realStamina) > 0.1f) {
            if (animatedStamina < realStamina) {
                animatedStamina = Math.min(animatedStamina + ANIMATION_SPEED * deltaSec, realStamina);
            } else {
                animatedStamina = Math.max(animatedStamina - ANIMATION_SPEED * deltaSec, realStamina);
            }
        } else {
            animatedStamina = realStamina; // синхронизация при малой разнице
        }

        // Позиционирование (код из твоего файла, оставляем как есть)
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int healthBarX = 10;
        int healthBarY = screenHeight - 39;

        int guiScale = mc.options.guiScale().get();
        int barX, barY, barWidth, barHeight;

        switch (guiScale) {
            case 3:
                barX = healthBarX + 210;
                barY = healthBarY - 2;
                barWidth = 4;
                barHeight = 40;
                break;
            case 4:
                barX = healthBarX - -132;
                barY = healthBarY - 3;
                barWidth = 4;
                barHeight = 40;
                break;
            default:
                barX = healthBarX + 80;
                barY = healthBarY - 3;
                barWidth = 4;
                barHeight = 40;
                break;
        }

        // Вычисляем высоту заполнения на основе анимированной стамины
        int fillHeight = (int) (animatedStamina / maxStamina * barHeight);

        // Фон
        graphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0x80000000);

        // Заполнение
        if (fillHeight > 0) {
            graphics.fill(
                    barX, barY + (barHeight - fillHeight),
                    barX + barWidth, barY + barHeight,
                    0xFFDAA520
            );
        }

        // Обводка
        graphics.fill(barX - 1, barY - 1, barX + barWidth + 1, barY, 0xFFFFFFFF);
        graphics.fill(barX - 1, barY + barHeight, barX + barWidth + 1, barY + barHeight + 1, 0xFFFFFFFF);
        graphics.fill(barX - 1, barY, barX, barY + barHeight, 0xFFFFFFFF);
        graphics.fill(barX + barWidth, barY, barX + barWidth + 1, barY + barHeight, 0xFFFFFFFF);
    }
}