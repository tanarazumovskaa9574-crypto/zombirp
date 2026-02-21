package com.reputationmod.stamina;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class StaminaNetwork {
    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(
                StaminaHudPayload.TYPE,
                StaminaHudPayload.STREAM_CODEC,
                StaminaHudPayload::handleData
        );
        registrar.playToClient(
                StaminaSyncPayload.TYPE,
                StaminaSyncPayload.STREAM_CODEC,
                StaminaSyncPayload::handleData
        );
        registrar.playToClient(
                StaminaLevelSyncPayload.TYPE,
                StaminaLevelSyncPayload.STREAM_CODEC,
                StaminaLevelSyncPayload::handleData
        );
    }

    public static void syncToClient(ServerPlayer player) {
        var staminaData = player.getData(StaminaAttachment.STAMINA_DATA);
        PacketDistributor.sendToPlayer(player, new StaminaSyncPayload(staminaData.getStamina(), staminaData.getMaxStamina()));
        PacketDistributor.sendToPlayer(player, new StaminaLevelSyncPayload(staminaData.getLevel(), staminaData.getExperience(), staminaData.getMaxStamina()));
    }
}