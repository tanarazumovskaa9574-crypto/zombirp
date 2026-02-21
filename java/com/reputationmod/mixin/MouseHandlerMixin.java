package com.reputationmod.mixin;

import com.reputationmod.ReputationMod;
import com.reputationmod.infection.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    /**
     * Инвертирует движение мыши по горизонтали (Yaw), если у игрока есть эффект TRIP.
     * Перехватываем первый аргумент (double) при вызове LocalPlayer.turn(double, double).
     */
    @ModifyArg(
            method = "turnPlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V",
                    remap = false   // важно: ищем точное имя, без маппингов
            ),
            index = 0,          // первый параметр — горизонтальное вращение (yRot)
            remap = false
    )
    private double invertMouseX(double yRot) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.hasEffect(ModEffects.TRIP)) {
            ReputationMod.LOGGER.debug("[MouseMixin] Inverting X: {} -> {}", yRot, -yRot);
            return -yRot;
        }
        return yRot;
    }

    /**
     * Инвертирует движение мыши по вертикали (Pitch), если у игрока есть эффект TRIP.
     * Перехватываем второй аргумент (double) при вызове LocalPlayer.turn(double, double).
     */
    @ModifyArg(
            method = "turnPlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V",
                    remap = false
            ),
            index = 1,          // второй параметр — вертикальное вращение (xRot)
            remap = false
    )
    private double invertMouseY(double xRot) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.hasEffect(ModEffects.TRIP)) {
            ReputationMod.LOGGER.debug("[MouseMixin] Inverting Y: {} -> {}", xRot, -xRot);
            return -xRot;
        }
        return xRot;
    }
}