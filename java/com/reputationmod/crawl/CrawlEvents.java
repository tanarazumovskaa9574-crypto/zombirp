package com.reputationmod.crawl;

import com.reputationmod.ReputationMod;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = ReputationMod.MOD_ID, value = Dist.CLIENT)
public class CrawlEvents {
    private static boolean wasKeyDown = false;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        boolean isKeyDown = CrawlKeys.CRAWL_KEY.isDown();
        if (isKeyDown && !wasKeyDown) {
            PacketDistributor.sendToServer(new CrawlPacket());
        }
        wasKeyDown = isKeyDown;
    }
}