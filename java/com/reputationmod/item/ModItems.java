package com.reputationmod.item;

import com.reputationmod.ReputationMod;
import com.reputationmod.block.ModBlocks;
import com.reputationmod.food.ModFoods;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(ReputationMod.MOD_ID);

    // Предметы для блоков
    public static final DeferredItem<BlockItem> TROPICAL_WOOD_ITEM = ITEMS.registerSimpleBlockItem(
            "tropical_wood", ModBlocks.TROPICAL_WOOD);
    public static final DeferredItem<BlockItem> REINFORCED_BRICKS_ITEM = ITEMS.registerSimpleBlockItem(
            "reinforced_bricks", ModBlocks.REINFORCED_BRICKS);
    public static final DeferredItem<BlockItem> REINFORCED_STONE_ITEM = ITEMS.registerSimpleBlockItem(
            "reinforced_stone", ModBlocks.REINFORCED_STONE);
    public static final DeferredItem<BlockItem> DARK_METAL_ITEM = ITEMS.registerSimpleBlockItem(
            "dark_metal", ModBlocks.DARK_METAL);
    public static final DeferredItem<BlockItem> PROTECTION_CHEST_ITEM = ITEMS.registerSimpleBlockItem(
            "protection_chest", ModBlocks.PROTECTION_CHEST);

    // Медицинские предметы
    public static final DeferredItem<Item> BANDAGE = ITEMS.registerSimpleItem("bandage");
    public static final DeferredItem<Item> FIRST_AID_KIT = ITEMS.registerSimpleItem("first_aid_kit");
    public static final DeferredItem<Item> APTECHKA = ITEMS.registerSimpleItem("aptechka");
    public static final DeferredItem<Item> SYRINGE = ITEMS.registerSimpleItem("syringe");

    // Тимодепрессин (свойства еды + стак 16)
    public static final DeferredItem<Item> THYMODEPRESSIN = ITEMS.registerItem("thymodepressin",
            properties -> new ThymodepressinItem(properties.food(
                    new FoodProperties.Builder()
                            .nutrition(0)
                            .saturationModifier(0)
                            .alwaysEdible()
                            .build()
            ).stacksTo(16)));

    // Еда
    public static final DeferredItem<Item> BEER = ITEMS.registerItem("beer",
            properties -> new Item(properties.food(ModFoods.BEER)));
    public static final DeferredItem<Item> COKE = ITEMS.registerItem("coke",
            properties -> new Item(properties.food(ModFoods.COKE)));
    public static final DeferredItem<Item> MONSTER = ITEMS.registerItem("monster",
            properties -> new Item(properties.food(ModFoods.MONSTER)));

    // Килька
    public static final DeferredItem<Item> KILKA = ITEMS.registerItem("kilka",
            properties -> new KilkaItem(properties.food(ModFoods.KILKA_TOMATO)));
    public static final DeferredItem<Item> KILKA2 = ITEMS.registerItem("kilka2",
            properties -> new KilkaItem(properties.food(ModFoods.KILKA_VEGETABLE)));
    public static final DeferredItem<Item> KILKA3 = ITEMS.registerItem("kilka3",
            properties -> new KilkaItem(properties.food(ModFoods.KILKA_CLASSIC)));

    // Пачка сигарет (старый предмет)
    public static final DeferredItem<Item> CIGARETTES = ITEMS.registerSimpleItem("cigarettes");

    // Сигарета Marlboro
    public static final DeferredItem<Item> MARLBORO_CIGARETTE = ITEMS.registerItem("marlborocigarette",
            properties -> new MarlborocigaretteItem(properties.stacksTo(16)));

    // Сигарета Cannabis (если нужна)
    public static final DeferredItem<Item> CANNABIS_CIGARETTE = ITEMS.registerSimpleItem("cannabiscigarette");

    public static final DeferredItem<Item> ZIPPO = ITEMS.registerItem("zippo",
            properties -> new ZippoItem(properties.stacksTo(1).durability(300)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);

    }
}