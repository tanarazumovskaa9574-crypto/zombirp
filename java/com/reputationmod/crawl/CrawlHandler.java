package com.reputationmod.crawl;

import com.reputationmod.ReputationMod;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ReputationMod.MOD_ID)
public class CrawlHandler {
    private static final String CRAWLING_KEY = "isCrawling";
    private static final String LAST_CRAWL_TIME_KEY = "lastCrawlTime";

    public static void toggleCrawl(ServerPlayer player) {
        long now = System.currentTimeMillis();
        long last = player.getPersistentData().getLong(LAST_CRAWL_TIME_KEY);
        int cooldown = CrawlConfig.INSTANCE.crawlCooldown.get() * 1000;

        if (now - last < cooldown) {
            player.displayClientMessage(Component.literal("Вы не можете ползти так часто!"), true);
            return;
        }

        boolean isCrawling = player.getPersistentData().getBoolean(CRAWLING_KEY);
        boolean newState = !isCrawling;

        player.getPersistentData().putBoolean(CRAWLING_KEY, newState);
        player.getPersistentData().putLong(LAST_CRAWL_TIME_KEY, now);

        if (newState) {
            startCrawling(player);
        } else {
            stopCrawling(player);
        }
    }

    private static void startCrawling(ServerPlayer player) {
        player.setPose(Pose.SWIMMING);
        AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            if (!player.getPersistentData().contains("baseSpeed")) {
                player.getPersistentData().putDouble("baseSpeed", movementSpeed.getBaseValue());
            }
            movementSpeed.setBaseValue(0.05);
        }
    }

    private static void stopCrawling(ServerPlayer player) {
        if (player.getPose() == Pose.SWIMMING) {
            player.setPose(Pose.STANDING);
        }
        AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null && player.getPersistentData().contains("baseSpeed")) {
            double baseSpeed = player.getPersistentData().getDouble("baseSpeed");
            movementSpeed.setBaseValue(baseSpeed);
            player.getPersistentData().remove("baseSpeed");
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        boolean isCrawling = player.getPersistentData().getBoolean(CRAWLING_KEY);

        if (isCrawling) {
            // Принудительно удерживаем позу ползания
            if (player.getPose() != Pose.SWIMMING) {
                player.setPose(Pose.SWIMMING);
            }
            // Блокировка прыжка временно отключена из-за изменений в API
            // TODO: реализовать блокировку прыжка через миксин или событие
        } else {
            // Если игрок не в режиме ползания, но по какой-то причине остался в позе SWIMMING,
            // возвращаем его в STANDING (если он не в воде и не под низким потолком)
            if (player.getPose() == Pose.SWIMMING && !player.isInWater() && player.level().getBlockState(player.blockPosition().above(2)).isAir()) {
                player.setPose(Pose.STANDING);
            }
        }
    }
}