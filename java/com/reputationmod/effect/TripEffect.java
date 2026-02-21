package com.reputationmod.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class TripEffect extends MobEffect {
    public TripEffect() {
        super(MobEffectCategory.NEUTRAL, 0x00FF00); // зелёный цвет иконки
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        // Здесь можно добавить дополнительную логику, например, случайные движения
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // Обновляем каждый тик
        return true;
    }
}