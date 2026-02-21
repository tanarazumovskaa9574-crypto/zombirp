package com.reputationmod.creative;

import com.reputationmod.ReputationMod;
import com.reputationmod.block.ModBlocks;
import com.reputationmod.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTab {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ReputationMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> REPUTATION_TAB = CREATIVE_MODE_TABS.register("reputation_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creativetab.reputationmod.reputation_tab"))
                    .icon(() -> new ItemStack(ModBlocks.TROPICAL_WOOD.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModBlocks.TROPICAL_WOOD.get());
                        output.accept(ModBlocks.REINFORCED_BRICKS.get());
                        output.accept(ModBlocks.REINFORCED_STONE.get());
                        output.accept(ModBlocks.DARK_METAL.get());
                        output.accept(ModBlocks.PROTECTION_CHEST.get());
                    })
                    .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FOOD_TAB = CREATIVE_MODE_TABS.register("food_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creativetab.reputationmod.food_tab"))
                    .icon(() -> new ItemStack(ModItems.APTECHKA.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.APTECHKA.get());
                        output.accept(ModItems.BANDAGE.get());
                        output.accept(ModItems.BEER.get());
                        output.accept(ModItems.CIGARETTES.get());
                        output.accept(ModItems.CANNABIS_CIGARETTE.get());
                        output.accept(ModItems.MARLBORO_CIGARETTE.get());
                        output.accept(ModItems.COKE.get());
                        output.accept(ModItems.FIRST_AID_KIT.get());
                        output.accept(ModItems.KILKA.get());
                        output.accept(ModItems.KILKA2.get());
                        output.accept(ModItems.KILKA3.get());
                        output.accept(ModItems.MONSTER.get());
                        output.accept(ModItems.SYRINGE.get());
                        output.accept(ModItems.THYMODEPRESSIN.get());
                        // Зажигалка
                        output.accept(ModItems.ZIPPO.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}