package com.reputationmod.mixin;

import com.reputationmod.stamina.StaminaAttachment;
import com.reputationmod.stamina.StaminaConfig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public class PlayerAttackMixin {

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true, remap = false)
    private void onAttack(Entity target, CallbackInfo ci) {
        Player player = (Player) (Object) this;

        if (player.isCreative() || player.isSpectator()) return;

        var data = player.getData(StaminaAttachment.STAMINA_DATA);
        // Берём порог из конфига
        int requiredStamina = StaminaConfig.COMMON.MIN_STAMINA_FOR_ACTION.get();

        if (data.getStamina() < requiredStamina) {
            ci.cancel();
        }
    }
}