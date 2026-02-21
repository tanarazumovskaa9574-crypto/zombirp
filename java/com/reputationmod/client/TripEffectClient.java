package com.reputationmod.client;

import com.reputationmod.ReputationMod;
import com.reputationmod.infection.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.io.IOException;

@EventBusSubscriber(modid = ReputationMod.MOD_ID, value = Dist.CLIENT)
public class TripEffectClient {
    private static PostChain invertShader;
    private static boolean wasEffectActive = false;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        boolean hasEffect = player.hasEffect(ModEffects.TRIP);
        ReputationMod.LOGGER.debug("[TripEffect] hasEffect: {}, wasEffectActive: {}", hasEffect, wasEffectActive);

        if (hasEffect && !wasEffectActive) {
            ReputationMod.LOGGER.info("[TripEffect] Effect just appeared – trying to load shader");
            try {
                if (invertShader != null) {
                    invertShader.close();
                    ReputationMod.LOGGER.debug("[TripEffect] Closed previous shader");
                }
                ResourceLocation location = ResourceLocation.fromNamespaceAndPath(ReputationMod.MOD_ID, "shaders/post/invert.json");
                ReputationMod.LOGGER.debug("[TripEffect] Loading shader from: {}", location);
                invertShader = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), location);
                invertShader.resize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
                wasEffectActive = true;
                ReputationMod.LOGGER.info("[TripEffect] Shader loaded successfully");
            } catch (IOException e) {
                ReputationMod.LOGGER.error("[TripEffect] Failed to load shader", e);
            }
        } else if (!hasEffect && wasEffectActive) {
            ReputationMod.LOGGER.info("[TripEffect] Effect ended – closing shader");
            if (invertShader != null) {
                invertShader.close();
                invertShader = null;
            }
            wasEffectActive = false;
        }

        if (hasEffect && invertShader != null) {
            // Исправлено: используем getTimer() для получения времени кадра
            float partialTick = mc.getTimer().getGameTimeDeltaPartialTick(false);
            invertShader.process(partialTick);
        }
    }
}