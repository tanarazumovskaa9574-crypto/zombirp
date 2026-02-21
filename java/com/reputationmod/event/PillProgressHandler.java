package com.reputationmod.event;

import com.reputationmod.ReputationMod;
import com.reputationmod.item.ThymodepressinItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

import java.util.WeakHashMap;

@EventBusSubscriber(modid = ReputationMod.MOD_ID)
public class PillProgressHandler {

    private static final WeakHashMap<Player, Integer> lastPercent = new WeakHashMap<>();

    @SubscribeEvent
    public static void onItemUseStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack stack = event.getItem();
        if (!(stack.getItem() instanceof ThymodepressinItem)) return;
        lastPercent.put(player, -1);
    }

    @SubscribeEvent
    public static void onItemUseTick(LivingEntityUseItemEvent.Tick event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack stack = event.getItem();
        if (!(stack.getItem() instanceof ThymodepressinItem)) return;

        int duration = stack.getUseDuration(player);
        int ticksUsed = duration - event.getDuration();
        int percentStep = (int) ((ticksUsed / (float) duration) * 10); // 0..10
        if (percentStep < 0) percentStep = 0;
        if (percentStep > 10) percentStep = 10;

        Integer last = lastPercent.get(player);
        if (last == null || last != percentStep) {
            if (percentStep < 10) {
                player.displayClientMessage(Component.literal("§a" + (percentStep * 10) + "% "), true);
            }
            lastPercent.put(player, percentStep);
        }
    }

    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack stack = event.getItem();
        if (!(stack.getItem() instanceof ThymodepressinItem)) return;

        player.displayClientMessage(Component.literal("§a100% §eУспешно!!!"), true);
        lastPercent.remove(player);
    }

    @SubscribeEvent
    public static void onItemUseStop(LivingEntityUseItemEvent.Stop event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack stack = event.getItem();
        if (!(stack.getItem() instanceof ThymodepressinItem)) return;

        if (lastPercent.containsKey(player)) {
            player.displayClientMessage(Component.literal("§cНеудачно..."), true);
            lastPercent.remove(player);
        }
    }
}