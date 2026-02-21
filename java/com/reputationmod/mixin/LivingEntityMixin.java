package com.reputationmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.world.entity.LivingEntity;
import com.reputationmod.crawl.CrawlShared;

@Mixin(LivingEntity.class)
abstract class LivingEntityMixin extends net.minecraft.world.entity.Entity {

    LivingEntityMixin() { super(null, null); }

    @ModifyExpressionValue(
            method = "updateSwimAmount",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;isVisuallySwimming()Z",
                    remap = false
            ),
            remap = false
    )
    boolean updateSwimAmount(boolean isInSwimmingPose) {
        return isInSwimmingPose || this.getPose() == CrawlShared.CRAWLING;
    }
}