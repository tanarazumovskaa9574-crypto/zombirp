package com.reputationmod.client;

import com.reputationmod.ReputationMod;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = ReputationMod.MOD_ID)
public class ClientModBusEvents {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (ModKeyBindings.OPEN_LEVELS_KEY.consumeClick()) {
            Minecraft.getInstance().setScreen(new LevelScreen());
        }
    }
}