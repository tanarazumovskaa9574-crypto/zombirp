package com.reputationmod;

import com.reputationmod.stamina.StaminaAttachment;
import com.reputationmod.stamina.StaminaConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = ReputationMod.MOD_ID)
public class SwingCancelHandler {

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null || player.isCreative() || player.isSpectator()) return;

        var staminaData = player.getData(StaminaAttachment.STAMINA_DATA);
        // БЕРЁМ ЗНАЧЕНИЕ ИЗ КОНФИГА
        int requiredStamina = StaminaConfig.COMMON.MIN_STAMINA_FOR_ACTION.get();

        if (staminaData.getStamina() < requiredStamina) {
            if (event.isAttack()) {
                event.setCanceled(true);
                event.setSwingHand(false);
            }
            if (event.isUseItem()) {
                event.setCanceled(true);
                event.setSwingHand(false);
            }
        }
    }
}