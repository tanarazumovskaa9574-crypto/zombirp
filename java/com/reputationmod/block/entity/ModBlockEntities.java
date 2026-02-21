package com.reputationmod.block.entity;

import com.reputationmod.ReputationMod;
import com.reputationmod.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ReputationMod.MOD_ID);

    public static final Supplier<BlockEntityType<ProtectionChestBlockEntity>> PROTECTION_CHEST =
            BLOCK_ENTITIES.register("protection_chest",
                    () -> BlockEntityType.Builder.of(ProtectionChestBlockEntity::new, ModBlocks.PROTECTION_CHEST.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}