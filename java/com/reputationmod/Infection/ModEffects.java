package com.reputationmod.infection;

import com.reputationmod.ReputationMod;
import com.reputationmod.effect.TripEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, ReputationMod.MOD_ID);

    // Эффект заражения (класс InfectionMobEffect должен существовать в этом пакете)
    public static final DeferredHolder<MobEffect, MobEffect> INFECTION =
            EFFECTS.register("infection", () -> new InfectionMobEffect(MobEffectCategory.HARMFUL, 0x4CBB17));

    // Новый эффект "Приход"
    public static final DeferredHolder<MobEffect, MobEffect> TRIP =
            EFFECTS.register("trip", TripEffect::new);

    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
    }
}