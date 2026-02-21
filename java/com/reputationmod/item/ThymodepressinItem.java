package com.reputationmod.item;

import com.reputationmod.infection.ModEffects;
import com.reputationmod.sound.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class ThymodepressinItem extends Item {
    private static final int USE_DURATION = 100; // 5 секунд = 100 тиков
    private static final int REGEN_DURATION = 60; // 3 секунды регенерации
    private static final int EXTRA_INFECTION_TIME = 20 * 60 * 10; // 10 минут в тиках

    public ThymodepressinItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof Player player) {
            MobEffectInstance infection = player.getEffect(ModEffects.INFECTION);
            if (infection != null) {
                int newDuration = infection.getDuration() + EXTRA_INFECTION_TIME;
                player.addEffect(new MobEffectInstance(ModEffects.INFECTION, newDuration, infection.getAmplifier()));
                player.sendSystemMessage(Component.literal("§aЗаражение замедлено! +10 минут."));
            } else {
                player.sendSystemMessage(Component.literal("§aОрганизм укреплён, но заражения нет."));
            }
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REGEN_DURATION, 0));
        }

        if (!level.isClientSide) {
            stack.shrink(1);
        }
        return stack;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Проверка кулдауна
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide) {
            // Ставим кулдаун
            player.getCooldowns().addCooldown(this, USE_DURATION + 5);
            // Звук
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.PILLS_SOUND.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE; // без анимации
    }
}