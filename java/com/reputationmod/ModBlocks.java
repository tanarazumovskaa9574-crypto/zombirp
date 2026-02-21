package com.reputationmod.block;

import com.reputationmod.ReputationMod;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(ReputationMod.MOD_ID);

    // Тропическая древесина (улучшенное дерево)
    public static final DeferredBlock<Block> TROPICAL_WOOD = BLOCKS.registerSimpleBlock(
            "tropical_wood",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0f, 3.0f)
                    .sound(SoundType.WOOD)
    );

    // Кирпичи
    public static final DeferredBlock<Block> REINFORCED_BRICKS = BLOCKS.registerSimpleBlock(
            "reinforced_bricks",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(2.5f, 6.0f)
                    .sound(SoundType.STONE)
    );

    // Камень
    public static final DeferredBlock<Block> REINFORCED_STONE = BLOCKS.registerSimpleBlock(
            "reinforced_stone",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0f, 9.0f)
                    .sound(SoundType.STONE)
    );

    // Базальт (тёмный металл)
    public static final DeferredBlock<Block> DARK_METAL = BLOCKS.registerSimpleBlock(
            "dark_metal",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(4.0f, 12.0f)
                    .sound(SoundType.BASALT)
    );

    // Сундук защиты (теперь с BlockEntity)
    public static final DeferredBlock<ProtectionChestBlock> PROTECTION_CHEST = BLOCKS.registerBlock(
            "protection_chest",
            ProtectionChestBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.5f, 5.0f)
                    .sound(SoundType.WOOD)
    );

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}