package com.reputationmod.item;

import com.reputationmod.infection.ModEffects;
import com.reputationmod.sound.ModSounds;
import com.reputationmod.stamina.StaminaAttachment;
import com.reputationmod.stamina.StaminaData;
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

public class MarlborocigaretteItem extends Item {
    private static final int USE_DURATION = 120; // 6 секунд
    private static final int COOLDOWN_TICKS = USE_DURATION + 10;

    // Дебаффы
    private static final int NAUSEA_DURATION = 15 * 20; // 15 секунд
    private static final int POISON_DURATION = 3 * 20;   // 3 секунды
    private static final int POISON_AMPLIFIER = 0;       // I уровень
    private static final int STAMINA_COST = 30;           // -30 стамины

    // Баффы
    private static final int NIGHT_VISION_DURATION = 90 * 20; // 90 секунд (1 минута 30)
    private static final int INFECTION_SLOW_DURATION = 60 * 20; // 1 минута

    public MarlborocigaretteItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ItemStack offhand = player.getOffhandItem();

        // Проверка наличия зажигалки в левой руке
        if (!(offhand.getItem() instanceof ZippoItem)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("Вам нужна зажигалка Zippo в левой руке!"), true);
            }
            return InteractionResultHolder.fail(stack);
        }

        // Проверка, не сломана ли зажигалка
        if (offhand.getDamageValue() >= offhand.getMaxDamage()) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("Зажигалка сломана!"), true);
            }
            return InteractionResultHolder.fail(stack);
        }

        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide) {
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.MARLBORO_SOUND.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof Player player) {
            ItemStack offhand = player.getOffhandItem();

            // Повторная проверка (на случай, если зажигалка исчезла во время использования)
            if (!(offhand.getItem() instanceof ZippoItem) || offhand.getDamageValue() >= offhand.getMaxDamage()) {
                player.displayClientMessage(Component.literal("Не удалось закурить: зажигалка отсутствует или сломана!"), true);
                return stack;
            }

            // Тратим прочность зажигалки
            offhand.hurtAndBreak(1, player,
                    LivingEntity.getSlotForHand(InteractionHand.OFF_HAND));

            // Дебаффы
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, NAUSEA_DURATION, 0));
            player.addEffect(new MobEffectInstance(MobEffects.POISON, POISON_DURATION, POISON_AMPLIFIER));

            // Трата стамины
            StaminaData staminaData = player.getData(StaminaAttachment.STAMINA_DATA);
            int currentStamina = staminaData.getStamina();
            int newStamina = Math.max(0, currentStamina - STAMINA_COST);
            staminaData.setStamina(newStamina);

            // Бафф: ночное зрение (теперь 90 секунд)
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, NIGHT_VISION_DURATION, 0));

            // Замедление заражения
            MobEffectInstance infection = player.getEffect(ModEffects.INFECTION);
            if (infection != null) {
                int newDuration = infection.getDuration() + INFECTION_SLOW_DURATION;
                player.addEffect(new MobEffectInstance(ModEffects.INFECTION, newDuration, infection.getAmplifier()));
            }

            // Тратим сигарету
            stack.shrink(1);
        }
        return stack;
    }
}