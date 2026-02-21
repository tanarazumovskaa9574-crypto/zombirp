package com.reputationmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.List;

public class KilkaItem extends Item {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int EAT_DURATION_TICKS = 60; // 3 секунды

    public KilkaItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return EAT_DURATION_TICKS;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        ItemStack result = super.finishUsingItem(stack, level, user);

        if (!level.isClientSide && user instanceof ServerPlayer player) {
            // Отладка: проверяем, есть ли эффект регенерации после еды (для kilka2)
            if (stack.getItem() == ModItems.KILKA2.get()) {
                boolean hasRegen = player.hasEffect(MobEffects.REGENERATION);
                LOGGER.info("After eating kilka2 (овощная), regeneration effect present: " + hasRegen);
                if (!hasRegen) {
                    LOGGER.warn("Regeneration effect was not applied! Food properties: " + stack.getFoodProperties(player));
                }
            }
        }

        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        // Информация о регенерации для kilka2
        if (stack.getItem() == ModItems.KILKA2.get()) {
            tooltipComponents.add(Component.literal("§aДаёт регенерацию I (3 сек)"));
        }
    }
}