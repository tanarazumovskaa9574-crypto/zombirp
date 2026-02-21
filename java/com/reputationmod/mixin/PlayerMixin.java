package com.reputationmod.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.google.common.collect.ImmutableMap;

import com.reputationmod.stamina.StaminaAttachment;
import com.reputationmod.stamina.StaminaConfig;
import com.reputationmod.crawl.CrawlShared;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity implements PrevPoseState {

    // === Твоя оригинальная логика (стамина) ===
    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true, remap = false)
    private void onJumpFromGround(CallbackInfo ci) {
        Player player = (Player) (Object) this;

        if (player.isCreative() || player.isSpectator()) return;

        var data = player.getData(StaminaAttachment.STAMINA_DATA);
        int requiredStamina = StaminaConfig.COMMON.MIN_STAMINA_FOR_ACTION.get();

        if (data.getStamina() < requiredStamina) {
            ci.cancel();
        }
    }

    // === Логика Crawl (ползание) ===
    @Shadow(remap = false) @Final private Abilities abilities;
    @Shadow(remap = false) @Final @Mutable private static Map<Pose, EntityDimensions> POSES;

    @Unique private Pose prevPose;
    @Unique private Pose prevTickPose;

    @Inject(method = "defineSynchedData", at = @At("TAIL"), remap = false)
    private void onDefineSynchedData(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(CrawlShared.CRAWL_REQUEST, false);
    }

    @ModifyArg(
            method = "updatePlayerPose",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;setPose(Lnet/minecraft/world/entity/Pose;)V",
                    remap = false
            ),
            remap = false
    )
    private Pose onPreUpdatePlayerPose(Pose pose) {
        Player player = (Player) (Object) this;
        if (!player.isSpectator() && !player.isPassenger() && !this.abilities.flying) {
            boolean requested = player.getEntityData().get(CrawlShared.CRAWL_REQUEST);
            boolean swimming = player.isSwimming() || player.isInWater();

            if (requested) {
                pose = swimming ? Pose.SWIMMING : CrawlShared.CRAWLING;
            }
            else if (pose == Pose.SWIMMING && !swimming) {
                pose = CrawlShared.CRAWLING;
            }
        }
        return pose;
    }

    @Inject(method = "<clinit>", at = @At("TAIL"), remap = false)
    private static void onPoseMapCreation(CallbackInfo ci) {
        POSES = ImmutableMap.<Pose, EntityDimensions>builder()
                .putAll(POSES)
                .put(CrawlShared.CRAWLING, CrawlShared.CRAWLING_DIMENSIONS)
                .build();
    }

    @Inject(method = "tick", at = @At("TAIL"), remap = false)
    public void onTickEnd(CallbackInfo ci) {
        if (this.getPose() != prevTickPose) {
            prevPose = prevTickPose;
        }
        prevTickPose = this.getPose();
    }

    @Override
    public Pose getPrevPose() {
        return prevPose;
    }

    PlayerMixin() { super(null, null); }
}