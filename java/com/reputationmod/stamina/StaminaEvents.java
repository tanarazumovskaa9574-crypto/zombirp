package com.reputationmod.stamina;

import com.reputationmod.ReputationMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ReputationMod.MOD_ID)
public class StaminaEvents {

    private static final int SLOW_THRESHOLD = 5;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        var data = player.getData(StaminaAttachment.STAMINA_DATA);

        if (!player.isCreative() && !player.isSpectator() && player.swinging) {
            data.setLastExertionTime();
        }

        if (!player.isCreative() && !player.isSpectator()) {
            if (Math.abs(player.getDeltaMovement().x) > 1.0E-5 || Math.abs(player.getDeltaMovement().z) > 1.0E-5) {
                data.setLastExertionTime();
            }
        }

        int stamina = data.getStamina();

        if (player.isSprinting() && !player.isCreative() && !player.isSpectator()) {
            int cost = StaminaConfig.COMMON.SPRINT_COST_PER_TICK.get();
            System.out.println("[StaminaDebug] Sprint tick: cost=" + cost + ", current stamina=" + stamina);
            if (data.hasStamina(cost)) {
                data.trySpend(cost);
            } else {
                player.setSprinting(false);
                System.out.println("[StaminaDebug] Sprint stopped due to low stamina");
            }
        }

        if (!player.isCreative() && !player.isSpectator()) {
            if (stamina <= 0) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2, false, false, true));
            } else if (stamina <= SLOW_THRESHOLD) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 0, false, false, true));
            }
        }

        data.tickRegen();

        if (!player.level().isClientSide && data.isDirty()) {
            StaminaNetwork.syncToClient((ServerPlayer) player);
            data.setDirty(false);
        }

        // Проверка повышения уровня
        if (!player.level().isClientSide && data.consumeLevelIncreased()) {
            player.sendSystemMessage(Component.literal("§aВы повысили уровень стамины до " + data.getLevel() + "!"));
            player.playSound(SoundEvents.PLAYER_LEVELUP, 1.0F, 1.0F);
        }

        // Отладка каждые 100 тиков
        if (player.tickCount % 100 == 0) {
            System.out.println("[StaminaDebug] Tick " + player.tickCount + ": stamina=" + data.getStamina() + "/" + data.getMaxStamina() + ", level=" + data.getLevel() + ", exp=" + data.getExperience());
        }
    }

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (player.isCreative() || player.isSpectator()) return;
            int cost = StaminaConfig.COMMON.JUMP_COST.get();
            var data = player.getData(StaminaAttachment.STAMINA_DATA);
            System.out.println("[StaminaDebug] Jump event: cost=" + cost + ", before stamina=" + data.getStamina());
            data.trySpend(cost);
            data.setLastExertionTime();
        }
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.isCreative() || player.isSpectator()) return;

        int cost = StaminaConfig.COMMON.ATTACK_COST.get();
        var data = player.getData(StaminaAttachment.STAMINA_DATA);
        System.out.println("[StaminaDebug] Attack event: cost=" + cost + ", current stamina=" + data.getStamina());
        if (data.getStamina() < cost) {
            event.setCanceled(true);
            player.displayClientMessage(Component.literal("§cНедостаточно выносливости для атаки!"), true);
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (!(event.getSource().getDirectEntity() instanceof Player player)) return;
        if (player.isCreative() || player.isSpectator()) return;

        int cost = StaminaConfig.COMMON.ATTACK_COST.get();
        var data = player.getData(StaminaAttachment.STAMINA_DATA);
        System.out.println("[StaminaDebug] LivingDamage (attack hit): cost=" + cost + ", before stamina=" + data.getStamina());
        data.trySpend(cost);
        data.setLastExertionTime();
    }

    @SubscribeEvent
    public static void onBreakBlock(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player.isCreative() || player.isSpectator()) return;

        int cost = StaminaConfig.COMMON.BLOCK_BREAK_COST.get();
        var data = player.getData(StaminaAttachment.STAMINA_DATA);
        System.out.println("[StaminaDebug] BreakBlock event: cost=" + cost + ", current stamina=" + data.getStamina());
        if (data.getStamina() >= cost) {
            data.trySpend(cost);
            data.setLastExertionTime();
        } else {
            event.setCanceled(true);
            player.displayClientMessage(Component.literal("§cНедостаточно выносливости для ломания!"), true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (player.isCreative() || player.isSpectator()) return;

        if (event.getItemStack().isEmpty() || !(event.getItemStack().getItem() instanceof BlockItem)) {
            return;
        }

        int cost = StaminaConfig.COMMON.PLACE_BLOCK_COST.get();
        var data = player.getData(StaminaAttachment.STAMINA_DATA);
        int staminaBefore = data.getStamina();
        System.out.println("[StaminaDebug] RightClickBlock (place block): cost=" + cost + ", staminaBefore=" + staminaBefore);

        if (staminaBefore < cost) {
            event.setCanceled(true);
            player.displayClientMessage(Component.literal("§cНедостаточно выносливости для установки блока!"), true);
            return;
        }

        data.trySpend(cost);
        data.setLastExertionTime();
    }

    @SubscribeEvent
    public static void onFishing(ItemFishedEvent event) {
        Player player = event.getEntity();
        if (player.isCreative() || player.isSpectator()) return;
        int cost = StaminaConfig.COMMON.FISHING_COST.get();
        var data = player.getData(StaminaAttachment.STAMINA_DATA);
        System.out.println("[StaminaDebug] Fishing event: cost=" + cost + ", current stamina=" + data.getStamina());
        if (data.getStamina() >= cost) {
            data.trySpend(cost);
            data.setLastExertionTime();
        } else {
            event.setCanceled(true);
            player.displayClientMessage(Component.literal("§cНедостаточно выносливости для рыбалки!"), true);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        Player original = event.getOriginal();
        Player newPlayer = event.getEntity();

        var oldData = original.getData(StaminaAttachment.STAMINA_DATA);
        var newData = newPlayer.getData(StaminaAttachment.STAMINA_DATA);
        var provider = original.registryAccess();
        newData.deserializeNBT(provider, oldData.serializeNBT(provider));
        System.out.println("[StaminaDebug] Player cloned after death, data copied.");
    }
}