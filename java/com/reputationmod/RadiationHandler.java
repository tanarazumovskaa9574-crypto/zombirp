package com.reputationmod;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Holder;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.*;
import java.nio.file.*;
import com.google.gson.*;
import java.io.*;

@EventBusSubscriber(modid = ReputationMod.MOD_ID)
public class RadiationHandler {

    // ============ РАДИАЦИОННЫЕ ЗОНЫ ============
    private static class RadiationZone {
        BlockPos pos1;
        BlockPos pos2;
        int radiationLevel; // 1-5

        RadiationZone(BlockPos pos1, BlockPos pos2, int radiationLevel) {
            this.pos1 = pos1;
            this.pos2 = pos2;
            this.radiationLevel = radiationLevel;
        }

        public boolean isInside(BlockPos playerPos) {
            int minX = Math.min(pos1.getX(), pos2.getX());
            int maxX = Math.max(pos1.getX(), pos2.getX());
            int minY = Math.min(pos1.getY(), pos2.getY());
            int maxY = Math.max(pos1.getY(), pos2.getY());
            int minZ = Math.min(pos1.getZ(), pos2.getZ());
            int maxZ = Math.max(pos1.getZ(), pos2.getZ());

            return playerPos.getX() >= minX && playerPos.getX() <= maxX &&
                    playerPos.getY() >= minY && playerPos.getY() <= maxY &&
                    playerPos.getZ() >= minZ && playerPos.getZ() <= maxZ;
        }
    }

    private static final List<RadiationZone> RADIATION_ZONES = new ArrayList<>();
    private static BlockPos radTempPos1 = null;
    private static BlockPos radTempPos2 = null;

    private static final Map<UUID, Long> lastRadiationMessage = new HashMap<>();
    private static final Map<UUID, Boolean> wasInZone = new HashMap<>(); // Для отслеживания входа/выхода
    private static final String RADIATION_ZONES_FILE = "config/reputationmod_radiation_zones.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    static {
        loadRadiationZones();
    }

    // ============ МЕТОДЫ ДЛЯ РАДИАЦИОННЫХ ЗОН ============

    public static void setRadPos1(ServerPlayer player) {
        radTempPos1 = player.blockPosition();
        player.sendSystemMessage(
                Component.literal("§a[Радиация] Первая точка: " +
                        radTempPos1.getX() + " " + radTempPos1.getY() + " " + radTempPos1.getZ())
        );
    }

    public static void setRadPos2(ServerPlayer player) {
        radTempPos2 = player.blockPosition();
        player.sendSystemMessage(
                Component.literal("§a[Радиация] Вторая точка: " +
                        radTempPos2.getX() + " " + radTempPos2.getY() + " " + radTempPos2.getZ())
        );
    }

    public static void createRadiationZone(int radiationLevel, CommandSourceStack source) {
        if (radTempPos1 == null || radTempPos2 == null) {
            source.sendSuccess(() ->
                    Component.literal("§cСначала установи обе точки! /rad pos1 и /rad pos2"), false);
            return;
        }

        RADIATION_ZONES.add(new RadiationZone(radTempPos1, radTempPos2, radiationLevel));
        saveRadiationZones();

        source.sendSuccess(() ->
                Component.literal("§a✅ Радиационная зона создана! Уровень: " + radiationLevel), true);
        source.sendSuccess(() ->
                Component.literal("§7От " + radTempPos1.getX() + " " + radTempPos1.getY() + " " + radTempPos1.getZ() +
                        " до " + radTempPos2.getX() + " " + radTempPos2.getY() + " " + radTempPos2.getZ()), false);

        radTempPos1 = null;
        radTempPos2 = null;
    }

    public static void listRadiationZones(CommandSourceStack source) {
        if (RADIATION_ZONES.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§cНет радиационных зон"), false);
        } else {
            source.sendSuccess(() -> Component.literal("§a=== РАДИАЦИОННЫЕ ЗОНЫ ==="), false);
            for (int i = 0; i < RADIATION_ZONES.size(); i++) {
                RadiationZone zone = RADIATION_ZONES.get(i);
                final int index = i;
                source.sendSuccess(() -> Component.literal(
                        String.format("§e%d. §f(%d %d %d) - (%d %d %d) §c☢ Уровень %d",
                                index,
                                zone.pos1.getX(), zone.pos1.getY(), zone.pos1.getZ(),
                                zone.pos2.getX(), zone.pos2.getY(), zone.pos2.getZ(),
                                zone.radiationLevel)), false);
            }
        }
    }

    public static void removeRadiationZone(int index) {
        if (index >= 0 && index < RADIATION_ZONES.size()) {
            RADIATION_ZONES.remove(index);
            saveRadiationZones();
        }
    }

    public static void highlightRadiationZone(int index, ServerPlayer player) {
        if (index < 0 || index >= RADIATION_ZONES.size()) {
            player.sendSystemMessage(Component.literal("§cЗона не найдена!"));
            return;
        }

        RadiationZone zone = RADIATION_ZONES.get(index);

        int minX = Math.min(zone.pos1.getX(), zone.pos2.getX());
        int maxX = Math.max(zone.pos1.getX(), zone.pos2.getX());
        int minY = Math.min(zone.pos1.getY(), zone.pos2.getY());
        int maxY = Math.max(zone.pos1.getY(), zone.pos2.getY());
        int minZ = Math.min(zone.pos1.getZ(), zone.pos2.getZ());
        int maxZ = Math.max(zone.pos1.getZ(), zone.pos2.getZ());

        player.sendSystemMessage(Component.literal("§c=== Подсветка радиационной зоны " + index + " ==="));

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ) {
                        player.serverLevel().sendParticles(
                                ParticleTypes.FALLING_NECTAR,
                                x + 0.5, y + 0.5, z + 0.5,
                                2,
                                0, 0, 0,
                                0.2
                        );
                    }
                }
            }
        }

        player.sendSystemMessage(Component.literal("§a✓ Границы подсвечены!"));
    }

    // ============ ПРОВЕРКА РАДИАЦИИ ============

    @SubscribeEvent
    public static void onRadiationTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (player.tickCount % 20 != 0) return; // Проверяем раз в секунду

            BlockPos playerPos = player.blockPosition();
            long currentTime = System.currentTimeMillis();

            boolean inZone = false;
            RadiationZone currentZone = null;

            for (RadiationZone zone : RADIATION_ZONES) {
                if (zone.isInside(playerPos)) {
                    inZone = true;
                    currentZone = zone;
                    break;
                }
            }

            boolean wasInZone = RadiationHandler.wasInZone.getOrDefault(player.getUUID(), false);

            // Проверяем вход в зону
            if (inZone && !wasInZone) {
                onZoneEnter(player, currentZone);
            }

            // Проверяем выход из зоны
            if (!inZone && wasInZone) {
                onZoneExit(player);
            }

            // Обновляем статус
            RadiationHandler.wasInZone.put(player.getUUID(), inZone);

            if (inZone && currentZone != null) {
                boolean hasGasMask = hasGasMask(player);

                if (!hasGasMask) {
                    // Без противогаза - получаем радиацию КАЖДУЮ СЕКУНДУ
                    player.addEffect(new MobEffectInstance(
                            MobEffects.POISON,
                            100,                    // 5 секунд (будет обновляться каждую секунду)
                            currentZone.radiationLevel - 1,
                            false,
                            true,
                            true
                    ));

                    if (!lastRadiationMessage.containsKey(player.getUUID()) ||
                            currentTime - lastRadiationMessage.get(player.getUUID()) > 3000) {

                        player.sendSystemMessage(
                                Component.literal("§c§l☠️ КРИТИЧЕСКИЙ УРОВЕНЬ РАДИАЦИИ!")
                        );
                        player.sendSystemMessage(
                                Component.literal("§cНЕМЕДЛЕННО ПОКИНЬТЕ ЗОНУ!")
                        );

                        // GENERIC_EXPLODE - Holder.Reference<SoundEvent> - для playSeededSound
                        player.level().playSeededSound(
                                null,
                                player.getX(), player.getY(), player.getZ(),
                                SoundEvents.GENERIC_EXPLODE,
                                SoundSource.PLAYERS,
                                1.0F, 1.0F,
                                player.getRandom().nextLong()
                        );

                        lastRadiationMessage.put(player.getUUID(), currentTime);
                    }
                } else {
                    // С противогазом - НЕТ радиации, просто тратится прочность
                    damageGasMask(player, currentZone);

                    // Добавляем атмосферные звуки для игроков с противогазом
                    if (player.getRandom().nextInt(100) < 5) { // 5% шанс каждую секунду
                        // SOUL_ESCAPE - Holder.Reference<SoundEvent> - для player.playSound нужен .value()
                        player.playSound(SoundEvents.SOUL_ESCAPE.value(), 0.3F, 1.0F);
                    }
                }
            }
        }
    }

    private static void onZoneEnter(ServerPlayer player, RadiationZone zone) {
        // Сообщение при входе в зону
        player.sendSystemMessage(Component.literal("§c§l╔══════════════════════════════════════╗"));
        player.sendSystemMessage(Component.literal("§c§l║      ⚠️ ВНИМАНИЕ! РАДИАЦИЯ! ⚠️       §c§l║"));
        player.sendSystemMessage(Component.literal("§c§l╠══════════════════════════════════════╣"));
        player.sendSystemMessage(Component.literal("§c§l║  Вы вошли в заражённую зону!        §c§l║"));

        if (hasGasMask(player)) {
            player.sendSystemMessage(Component.literal("§a§l║  ✓ Противогаз активирован            §c§l║"));
            player.sendSystemMessage(Component.literal("§e§l║  ⚡ Расход ресурса: 1 ед/сек         §c§l║"));

            // ARMOR_EQUIP_LEATHER - Holder.Reference<SoundEvent> - нужен .value()
            player.playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.0F, 0.8F);
        } else {
            player.sendSystemMessage(Component.literal("§c§l║  ✗ НЕТ ПРОТИВОГАЗА!                  §c§l║"));
            player.sendSystemMessage(Component.literal("§c§l║  ☠️ Получен смертельный уровень яда  §c§l║"));

            // WITHER_SPAWN - SoundEvent (обычный звук)
            player.playSound(SoundEvents.WITHER_SPAWN, 0.5F, 1.0F);
        }

        player.sendSystemMessage(Component.literal("§c§l╚══════════════════════════════════════╝"));

        // AMBIENT_SOUL_SAND_VALLEY_MOOD - Holder.Reference<SoundEvent> - для playSeededSound
        player.level().playSeededSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMBIENT_SOUL_SAND_VALLEY_MOOD,
                SoundSource.AMBIENT,
                1.0F, 0.8F,
                player.getRandom().nextLong()
        );
    }

    private static void onZoneExit(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("§a✓ Вы покинули заражённую зону"));
        player.sendSystemMessage(Component.literal("§7Радиационный фон нормализован"));

        // BEACON_DEACTIVATE - SoundEvent (обычный звук)
        player.playSound(SoundEvents.BEACON_DEACTIVATE, 0.5F, 1.0F);
    }

    // ============ СИСТЕМА ПРОТИВОГАЗОВ (ТОЛЬКО ЗАЧАРОВАНИЕ) ============

    private static boolean hasGasMask(ServerPlayer player) {
        ItemStack helmet = player.getInventory().getArmor(3);
        if (helmet.isEmpty()) return false;

        // Проверяем наличие зачарования
        ItemEnchantments enchantments = helmet.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

        for (var entry : enchantments.entrySet()) {
            if (entry.getKey() != null) {
                ResourceLocation id = entry.getKey().unwrapKey()
                        .map(key -> key.location())
                        .orElse(null);
                if (id != null && id.toString().equals("reputationmod:gas_mask")) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void makeGasMask(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("§a✓ Зачарование 'Противогаз' создано!"));
        player.sendSystemMessage(Component.literal("§7Проверь папку data/reputationmod/enchantment/"));
        player.sendSystemMessage(Component.literal("§7Чтобы получить шлем с зачарованием:"));
        player.sendSystemMessage(Component.literal("§7/give @p minecraft:iron_helmet[enchantments={levels:{\"reputationmod:gas_mask\":1}}]"));
    }

    private static boolean isHelmet(ItemStack item) {
        return item.getItem() == Items.LEATHER_HELMET ||
                item.getItem() == Items.IRON_HELMET ||
                item.getItem() == Items.GOLDEN_HELMET ||
                item.getItem() == Items.DIAMOND_HELMET ||
                item.getItem() == Items.NETHERITE_HELMET ||
                item.getItem() == Items.TURTLE_HELMET;
    }

    private static void damageGasMask(ServerPlayer player, RadiationZone zone) {
        ItemStack helmet = player.getInventory().getArmor(3);
        if (helmet.isEmpty()) return;

        // Проверяем наличие зачарования
        if (!hasGasMask(player)) return;

        int currentDamage = helmet.getDamageValue();
        int maxDamage = helmet.getMaxDamage();

        // Увеличиваем повреждение КАЖДУЮ СЕКУНДУ
        helmet.setDamageValue(currentDamage + 1);

        int remaining = maxDamage - (currentDamage + 1);

        // Разные сообщения в зависимости от уровня радиации
        if (remaining <= 0) {
            // Противогаз сломался
            player.getInventory().setItem(3, ItemStack.EMPTY);
            player.sendSystemMessage(Component.literal("§c§l💥 ПРОТИВОГАЗ РАЗРУШЕН!"));
            player.sendSystemMessage(Component.literal("§cФильтры больше не держат радиацию!"));

            // ITEM_BREAK - Holder.Reference<SoundEvent> - для playSeededSound
            player.level().playSeededSound(
                    null,
                    player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ITEM_BREAK,
                    SoundSource.PLAYERS,
                    1.0F, 1.0F,
                    player.getRandom().nextLong()
            );

            // Даем немного яда в качестве "штрафа" за разрушенный противогаз
            player.addEffect(new MobEffectInstance(
                    MobEffects.POISON,
                    60,
                    zone.radiationLevel - 1,
                    false,
                    true,
                    true
            ));

        } else {
            // Сообщения о состоянии противогаза
            if (remaining <= 10 && remaining % 5 == 0) {
                player.sendSystemMessage(Component.literal("§e⚠ КРИТИЧЕСКИ НИЗКИЙ УРОВЕНЬ ФИЛЬТРОВ: " + remaining + "%"));
                // NOTE_BLOCK_HAT - Holder.Reference<SoundEvent> - нужен .value()
                player.playSound(SoundEvents.NOTE_BLOCK_HAT.value(), 0.5F, 0.5F);
            } else if (remaining <= 30 && remaining % 10 == 0) {
                player.sendSystemMessage(Component.literal("§7Фильтры противогаза изнашиваются: " + remaining + "%"));
            }
        }
    }

    // ============ СОХРАНЕНИЕ ============

    private static void saveRadiationZones() {
        try {
            JsonObject root = new JsonObject();
            JsonArray zones = new JsonArray();

            for (RadiationZone zone : RADIATION_ZONES) {
                JsonObject z = new JsonObject();

                JsonObject pos1 = new JsonObject();
                pos1.addProperty("x", zone.pos1.getX());
                pos1.addProperty("y", zone.pos1.getY());
                pos1.addProperty("z", zone.pos1.getZ());

                JsonObject pos2 = new JsonObject();
                pos2.addProperty("x", zone.pos2.getX());
                pos2.addProperty("y", zone.pos2.getY());
                pos2.addProperty("z", zone.pos2.getZ());

                z.add("pos1", pos1);
                z.add("pos2", pos2);
                z.addProperty("radiationLevel", zone.radiationLevel);

                zones.add(z);
            }

            root.add("zones", zones);
            String jsonString = gson.toJson(root);
            Files.write(Paths.get(RADIATION_ZONES_FILE), jsonString.getBytes());

            ReputationMod.LOGGER.info("Радиационные зоны сохранены");
        } catch (IOException e) {
            ReputationMod.LOGGER.error("Ошибка сохранения радиационных зон: " + e.getMessage());
        }
    }

    private static void loadRadiationZones() {
        try {
            Path path = Paths.get(RADIATION_ZONES_FILE);
            if (Files.exists(path)) {
                String jsonString = new String(Files.readAllBytes(path));
                JsonObject root = gson.fromJson(jsonString, JsonObject.class);

                if (root != null && root.has("zones")) {
                    JsonArray zones = root.getAsJsonArray("zones");

                    for (JsonElement element : zones) {
                        JsonObject z = element.getAsJsonObject();

                        JsonObject pos1Obj = z.getAsJsonObject("pos1");
                        JsonObject pos2Obj = z.getAsJsonObject("pos2");

                        BlockPos pos1 = new BlockPos(
                                pos1Obj.get("x").getAsInt(),
                                pos1Obj.get("y").getAsInt(),
                                pos1Obj.get("z").getAsInt()
                        );

                        BlockPos pos2 = new BlockPos(
                                pos2Obj.get("x").getAsInt(),
                                pos2Obj.get("y").getAsInt(),
                                pos2Obj.get("z").getAsInt()
                        );

                        int level = z.get("radiationLevel").getAsInt();
                        RADIATION_ZONES.add(new RadiationZone(pos1, pos2, level));
                    }
                }

                ReputationMod.LOGGER.info("Радиационные зоны загружены: " + RADIATION_ZONES.size());
            }
        } catch (IOException e) {
            ReputationMod.LOGGER.error("Ошибка загрузки радиационных зон: " + e.getMessage());
        }
    }
}