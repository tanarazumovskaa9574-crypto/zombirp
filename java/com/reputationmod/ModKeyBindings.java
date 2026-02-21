package com.reputationmod.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.reputationmod.ReputationMod;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(value = Dist.CLIENT, modid = ReputationMod.MOD_ID)
public class ModKeyBindings {
    public static final String KEY_CATEGORY = "key.category.reputationmod";
    public static final String KEY_OPEN_LEVELS = "key.reputationmod.open_levels";

    public static final KeyMapping OPEN_LEVELS_KEY = new KeyMapping(
            KEY_OPEN_LEVELS,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_GRAVE_ACCENT,
            KEY_CATEGORY
    );

    @SubscribeEvent
    public static void registerKeyBindings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_LEVELS_KEY);
    }
}