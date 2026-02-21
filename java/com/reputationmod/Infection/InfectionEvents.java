package com.reputationmod.infection;

import com.reputationmod.ReputationMod;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = ReputationMod.MOD_ID)
public class InfectionEvents {
    private static final Random RANDOM = new Random();
    private static final Set<UUID> pendingInfectionDeaths = new HashSet<>();

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (event.getSource().getDirectEntity() instanceof Zombie && event.getEntity() instanceof Player player) {
            if (player.isCreative() || player.isSpectator()) return;

            double chance = InfectionConfig.COMMON.INFECTION_CHANCE.get();
            if (RANDOM.nextDouble() >= chance) return;

            if (player.hasEffect(ModEffects.INFECTION)) return;

            int durationTicks = InfectionConfig.COMMON.INFECTION_DURATION.get() * 20;
            player.addEffect(new MobEffectInstance(ModEffects.INFECTION, durationTicks, 0, false, true, true));
        }
    }

    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        MobEffectInstance effectInstance = event.getEffectInstance();
        if (effectInstance == null || !effectInstance.getEffect().is(ModEffects.INFECTION)) return;

        pendingInfectionDeaths.add(player.getUUID());

        // Просто убиваем игрока без кастомного урона
        player.kill();
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (pendingInfectionDeaths.remove(player.getUUID())) {
                // Спавним зомби
                ServerLevel level = player.serverLevel();
                Zombie zombie = new Zombie(level);
                zombie.setPos(player.getX(), player.getY(), player.getZ());
                level.addFreshEntity(zombie);

                player.sendSystemMessage(Component.literal("§c§l☠ ВЫ ПРЕВРАТИЛИСЬ В ЗОМБИ..."));
            }
        }
    }
}