package com.reputationmod.crawl;

import com.mojang.blaze3d.platform.InputConstants;
import com.reputationmod.ReputationMod;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = ReputationMod.MOD_ID, value = Dist.CLIENT)
public class CrawlKeys {
    public static final String KEY_CATEGORY = "key.category.reputationmod.crawl";
    public static final String KEY_CRAWL = "key.reputationmod.crawl";

    public static final KeyMapping CRAWL_KEY = new KeyMapping(
            KEY_CRAWL,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Z,
            KEY_CATEGORY
    );

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(CRAWL_KEY);
    }
}