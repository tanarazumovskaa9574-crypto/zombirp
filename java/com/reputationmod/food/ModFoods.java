package com.reputationmod.food;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;

public class ModFoods {
    // Килька томатная (без эффекта)
    public static final FoodProperties KILKA_TOMATO = new FoodProperties.Builder()
            .nutrition(3)
            .saturationModifier(0.3f)
            .build();

    // Килька овощная (с регенерацией)
    public static final FoodProperties KILKA_VEGETABLE = new FoodProperties.Builder()
            .nutrition(4)
            .saturationModifier(0.4f)
            .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 60, 0), 1.0f)
            .build();

    // Килька классическая (без эффекта)
    public static final FoodProperties KILKA_CLASSIC = new FoodProperties.Builder()
            .nutrition(5)
            .saturationModifier(0.5f)
            .build();

    // Пиво
    public static final FoodProperties BEER = new FoodProperties.Builder()
            .nutrition(2)
            .saturationModifier(0.2f)
            .build();

    // Энергетик Monster
    public static final FoodProperties MONSTER = new FoodProperties.Builder()
            .nutrition(4)
            .saturationModifier(0.3f)
            .build();

    // Кока-кола
    public static final FoodProperties COKE = new FoodProperties.Builder()
            .nutrition(3)
            .saturationModifier(0.2f)
            .build();
}