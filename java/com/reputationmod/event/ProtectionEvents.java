package com.reputationmod.event;

import com.reputationmod.ReputationMod;
import com.reputationmod.block.ModBlocks;
import com.reputationmod.block.entity.ProtectionChestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = ReputationMod.MOD_ID)
public class ProtectionEvents {

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof Level level)) return;
        if (level.isClientSide) return;

        BlockPos pos = event.getPos();
        BlockState state = event.getPlacedBlock();
        // Временная проверка на наши блоки (позже заменим на тег)
        if (state.is(ModBlocks.TROPICAL_WOOD.get()) ||
                state.is(ModBlocks.REINFORCED_BRICKS.get()) ||
                state.is(ModBlocks.REINFORCED_STONE.get()) ||
                state.is(ModBlocks.DARK_METAL.get())) {

            if (level instanceof ServerLevel serverLevel) {
                ProtectionChestBlockEntity.tryAddBlockToNearbyChest(serverLevel, pos);
            }
        }
    }
}