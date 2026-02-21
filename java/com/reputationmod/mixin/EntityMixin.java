package com.reputationmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import com.reputationmod.crawl.CrawlShared;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow(remap = false) public abstract Pose getPose();

    @ModifyReturnValue(method = "getBlockJumpFactor", at = @At("RETURN"), remap = false)
    float getBlockJumpFactor(float jumpFactor) {
        if (this.getPose() == CrawlShared.CRAWLING) {
            jumpFactor /= 2.0F;
        }
        return jumpFactor;
    }

    @ModifyReturnValue(method = "isVisuallyCrawling", at = @At("RETURN"), remap = false)
    private boolean isVisuallyCrawling(boolean original) {
        return original || this.getPose() == CrawlShared.CRAWLING;
    }

    @ModifyReturnValue(method = "isSteppingCarefully", at = @At("RETURN"), remap = false)
    private boolean isSteppingCarefully(boolean original) {
        return original || this.getPose() == CrawlShared.CRAWLING;
    }

    @ModifyReturnValue(method = "isDiscrete", at = @At("RETURN"), remap = false)
    private boolean isDiscrete(boolean original) {
        return original || this.getPose() == CrawlShared.CRAWLING;
    }
}