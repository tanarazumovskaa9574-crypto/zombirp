package com.reputationmod.sound;

import com.reputationmod.ReputationMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, ReputationMod.MOD_ID);

    // Звук для таблеток
    public static final DeferredHolder<SoundEvent, SoundEvent> PILLS_SOUND = SOUND_EVENTS.register("pillssound",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ReputationMod.MOD_ID, "pillssound")));

    // Звук для сигареты Marlboro
    public static final DeferredHolder<SoundEvent, SoundEvent> MARLBORO_SOUND = SOUND_EVENTS.register("marlboro",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ReputationMod.MOD_ID, "marlboro")));

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}