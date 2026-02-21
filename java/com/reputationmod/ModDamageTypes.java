package com.reputationmod.damage;

import com.reputationmod.ReputationMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModDamageTypes {
    public static final DeferredRegister<DamageType> DAMAGE_TYPES =
            DeferredRegister.create(Registries.DAMAGE_TYPE, ReputationMod.MOD_ID);

    public static final ResourceKey<DamageType> INFECTION = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(ReputationMod.MOD_ID, "infection")
    );

    public static final Supplier<DamageType> INFECTION_TYPE = DAMAGE_TYPES.register("infection",
            () -> new DamageType("infection", 0.1F));

    public static DamageSource infection(Entity source) {
        return new DamageSource(
                source.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(INFECTION),
                source
        );
    }

    public static void register(IEventBus eventBus) {
        DAMAGE_TYPES.register(eventBus);
    }
}