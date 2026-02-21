package com.reputationmod.event;

import com.reputationmod.ReputationMod;
import com.reputationmod.item.MarlborocigaretteItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

@EventBusSubscriber(modid = ReputationMod.MOD_ID)
public class CigaretteParticleHandler {

    @SubscribeEvent
    public static void onItemUseTick(LivingEntityUseItemEvent.Tick event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getItem();
        if (!(stack.getItem() instanceof MarlborocigaretteItem)) return;

        // Каждые 4 тика (0.2 секунды) спавним по 2 частицы дыма
        if (event.getDuration() % 4 == 0) {
            double x = player.getX();
            double y = player.getY() + 1.0;
            double z = player.getZ();

            player.serverLevel().sendParticles(
                    ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    x, y, z,
                    2,                 // 2 частицы за раз
                    0.15, 0.15, 0.15,  // небольшой разброс
                    0.02               // скорость
            );
        }
    }
}