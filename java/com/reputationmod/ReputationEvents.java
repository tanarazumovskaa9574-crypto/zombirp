package com.reputationmod;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.particles.ParticleTypes;

import java.util.*;
import java.util.stream.Collectors;
import java.nio.file.*;
import com.google.gson.*;
import java.io.*;

@EventBusSubscriber(modid = ReputationMod.MOD_ID)
public class ReputationEvents {
    private static final Map<UUID, Integer> playerReputation = new HashMap<>();
    private static final Map<UUID, String> playerNames = new HashMap<>();
    private static final int KILL_REPUTATION_CHANGE = -25;

    // Цвета для разных уровней репутации
    private static final ChatFormatting COLOR_YELLOW = ChatFormatting.YELLOW;      // Жёлтый (0)
    private static final ChatFormatting COLOR_GOLD = ChatFormatting.GOLD;          // Тёмно-жёлтый (-1 до -250)
    private static final ChatFormatting COLOR_RED = ChatFormatting.RED;            // Красный (-251 и ниже)
    private static final ChatFormatting COLOR_GREEN = ChatFormatting.GREEN;        // Зелёный (+1 и выше)

    // Файл для сохранения данных
    private static final String DATA_FILE = "config/reputationmod_data.json";
    private static final String ZONES_FILE = "config/reputationmod_zones.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // ============ ЗАПРЕТНЫЕ ЗОНЫ ============
    private static class RestrictedZone {
        BlockPos pos1;
        BlockPos pos2;
        int minReputation;

        RestrictedZone(BlockPos pos1, BlockPos pos2, int minReputation) {
            this.pos1 = pos1;
            this.pos2 = pos2;
            this.minReputation = minReputation;
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

    private static final List<RestrictedZone> RESTRICTED_ZONES = new ArrayList<>();
    private static BlockPos tempPos1 = null;
    private static BlockPos tempPos2 = null;

    // ============ МЕТОДЫ ДЛЯ ЗОН ============

    public static void setTempPos1(ServerPlayer player) {
        tempPos1 = player.blockPosition();
        player.sendSystemMessage(
                Component.literal("§aПервая точка установлена: " +
                        tempPos1.getX() + " " + tempPos1.getY() + " " + tempPos1.getZ())
        );
    }

    public static void setTempPos2(ServerPlayer player) {
        tempPos2 = player.blockPosition();
        player.sendSystemMessage(
                Component.literal("§aВторая точка установлена: " +
                        tempPos2.getX() + " " + tempPos2.getY() + " " + tempPos2.getZ())
        );
    }

    public static void createZone(int minReputation, CommandSourceStack source) {
        if (tempPos1 == null || tempPos2 == null) {
            source.sendSuccess(() ->
                    Component.literal("§cСначала установи обе точки! /zone pos1 и /zone pos2"), false);
            return;
        }

        RESTRICTED_ZONES.add(new RestrictedZone(tempPos1, tempPos2, minReputation));
        saveZones();

        source.sendSuccess(() ->
                Component.literal("§a✅ Запретная зона создана!"), true);
        source.sendSuccess(() ->
                Component.literal("§7От " + tempPos1.getX() + " " + tempPos1.getY() + " " + tempPos1.getZ() +
                        " до " + tempPos2.getX() + " " + tempPos2.getY() + " " + tempPos2.getZ()), false);
        source.sendSuccess(() ->
                Component.literal("§7Порог репутации: " + minReputation), false);

        tempPos1 = null;
        tempPos2 = null;
    }

    public static void listZones(CommandSourceStack source) {
        if (RESTRICTED_ZONES.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§cНет запретных зон"), false);
        } else {
            source.sendSuccess(() -> Component.literal("§6=== ЗАПРЕТНЫЕ ЗОНЫ ==="), false);
            for (int i = 0; i < RESTRICTED_ZONES.size(); i++) {
                RestrictedZone zone = RESTRICTED_ZONES.get(i);
                final int index = i;
                source.sendSuccess(() -> Component.literal(
                        String.format("§e%d. §f(%d %d %d) - (%d %d %d) §7(репа < %d)",
                                index,
                                zone.pos1.getX(), zone.pos1.getY(), zone.pos1.getZ(),
                                zone.pos2.getX(), zone.pos2.getY(), zone.pos2.getZ(),
                                zone.minReputation)), false);
            }
        }
    }

    public static void removeZone(int index) {
        if (index >= 0 && index < RESTRICTED_ZONES.size()) {
            RESTRICTED_ZONES.remove(index);
            saveZones();
        }
    }

    public static void highlightZone(int index, ServerPlayer player) {
        if (index < 0 || index >= RESTRICTED_ZONES.size()) {
            player.sendSystemMessage(Component.literal("§cЗона с таким индексом не найдена!"));
            return;
        }

        RestrictedZone zone = RESTRICTED_ZONES.get(index);

        int minX = Math.min(zone.pos1.getX(), zone.pos2.getX());
        int maxX = Math.max(zone.pos1.getX(), zone.pos2.getX());
        int minY = Math.min(zone.pos1.getY(), zone.pos2.getY());
        int maxY = Math.max(zone.pos1.getY(), zone.pos2.getY());
        int minZ = Math.min(zone.pos1.getZ(), zone.pos2.getZ());
        int maxZ = Math.max(zone.pos1.getZ(), zone.pos2.getZ());

        player.sendSystemMessage(Component.literal("§6=== Подсветка зоны " + index + " ==="));

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ) {
                        player.serverLevel().sendParticles(
                                ParticleTypes.FLAME,
                                x + 0.5, y + 0.5, z + 0.5,
                                1,
                                0, 0, 0,
                                0.1
                        );
                    }
                }
            }
        }

        player.sendSystemMessage(Component.literal("§a✓ Границы подсвечены!"));
    }

    private static void saveZones() {
        try {
            JsonObject root = new JsonObject();
            JsonArray zones = new JsonArray();

            for (RestrictedZone zone : RESTRICTED_ZONES) {
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
                z.addProperty("minReputation", zone.minReputation);

                zones.add(z);
            }

            root.add("zones", zones);
            String jsonString = gson.toJson(root);
            Files.write(Paths.get(ZONES_FILE), jsonString.getBytes());

            ReputationMod.LOGGER.info("Зоны сохранены. Всего зон: " + RESTRICTED_ZONES.size());
        } catch (IOException e) {
            ReputationMod.LOGGER.error("Ошибка при сохранении зон: " + e.getMessage());
        }
    }

    private static void loadZones() {
        try {
            Path path = Paths.get(ZONES_FILE);
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

                        int minRep = z.get("minReputation").getAsInt();

                        RESTRICTED_ZONES.add(new RestrictedZone(pos1, pos2, minRep));
                    }
                }

                ReputationMod.LOGGER.info("Зоны загружены. Всего зон: " + RESTRICTED_ZONES.size());
            }
        } catch (IOException e) {
            ReputationMod.LOGGER.error("Ошибка при загрузке зон: " + e.getMessage());
        }
    }

    // ============ ПРОВЕРКА ВХОДА В ЗОНУ ============
    private static final Map<UUID, Long> lastWarningTime = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (player.tickCount % 20 != 0) return;

            int rep = playerReputation.getOrDefault(player.getUUID(), 0);
            BlockPos playerPos = player.blockPosition();
            long currentTime = System.currentTimeMillis();

            for (RestrictedZone zone : RESTRICTED_ZONES) {
                if (zone.isInside(playerPos) && rep < zone.minReputation) {

                    player.addEffect(new MobEffectInstance(
                            MobEffects.POISON,
                            100,
                            2,
                            false,
                            true,
                            true
                    ));

                    if (!lastWarningTime.containsKey(player.getUUID()) ||
                            currentTime - lastWarningTime.get(player.getUUID()) > 3000) {

                        player.sendSystemMessage(
                                Component.literal("§c☠️ ТВОЯ РЕПУТАЦИЯ СЛИШКОМ НИЗКАЯ ДЛЯ ЭТОЙ ЗОНЫ!")
                        );
                        player.sendSystemMessage(
                                Component.literal("§7(Требуется: " + zone.minReputation + " | Твоя: " + rep + ")")
                        );

                        player.playSound(SoundEvents.SPIDER_HURT, 1.0F, 1.0F);

                        lastWarningTime.put(player.getUUID(), currentTime);
                    }

                    break;
                }
            }
        }
    }

    // ============ ОСНОВНЫЕ МЕТОДЫ ============

    static {
        loadData();
        loadZones();
    }

    @SubscribeEvent
    public static void onPlayerKill(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer killer) {
            if (event.getEntity() instanceof Player) {
                int currentRep = playerReputation.getOrDefault(killer.getUUID(), 0);
                playerReputation.put(killer.getUUID(), currentRep + KILL_REPUTATION_CHANGE);

                playerNames.put(killer.getUUID(), killer.getName().getString());
                if (event.getEntity() instanceof ServerPlayer victim) {
                    playerNames.put(victim.getUUID(), victim.getName().getString());
                }

                updateReputationDisplay(killer);
                updatePlayerNameColor(killer);
                saveData();

                killer.sendSystemMessage(
                        Component.literal("§cРепутация уменьшена! " + KILL_REPUTATION_CHANGE +
                                " §7(Текущая: " + getReputationColor(playerReputation.get(killer.getUUID())) +
                                playerReputation.get(killer.getUUID()) + "§7)")
                );
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            playerNames.put(player.getUUID(), player.getName().getString());

            int rep = playerReputation.getOrDefault(player.getUUID(), 0);

            updatePlayerNameColor(player);
            updateReputationDisplay(player);

            String color = getReputationColor(rep);
            player.sendSystemMessage(
                    Component.literal("§6[Репутация] §fВаша текущая репутация: " + color + rep)
            );
        }
    }

    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            saveData();
        }
    }

    public static void saveData() {
        try {
            JsonObject root = new JsonObject();
            JsonObject players = new JsonObject();

            for (Map.Entry<UUID, Integer> entry : playerReputation.entrySet()) {
                JsonObject playerData = new JsonObject();
                playerData.addProperty("name", playerNames.getOrDefault(entry.getKey(), "Unknown"));
                playerData.addProperty("reputation", entry.getValue());
                players.add(entry.getKey().toString(), playerData);
            }

            root.add("players", players);
            root.addProperty("lastSave", System.currentTimeMillis());

            String jsonString = gson.toJson(root);
            Files.write(Paths.get(DATA_FILE), jsonString.getBytes());

            ReputationMod.LOGGER.info("Данные репутации сохранены");
        } catch (IOException e) {
            ReputationMod.LOGGER.error("Ошибка при сохранении данных репутации: " + e.getMessage());
        }
    }

    public static void loadData() {
        try {
            Path path = Paths.get(DATA_FILE);
            if (Files.exists(path)) {
                String jsonString = new String(Files.readAllBytes(path));
                JsonObject root = gson.fromJson(jsonString, JsonObject.class);

                if (root != null && root.has("players")) {
                    JsonObject players = root.getAsJsonObject("players");

                    for (String key : players.keySet()) {
                        try {
                            UUID uuid = UUID.fromString(key);
                            JsonObject playerData = players.getAsJsonObject(key);

                            String name = playerData.get("name").getAsString();
                            int reputation = playerData.get("reputation").getAsInt();

                            playerNames.put(uuid, name);
                            playerReputation.put(uuid, reputation);
                        } catch (Exception e) {
                            ReputationMod.LOGGER.error("Ошибка при загрузке игрока: " + e.getMessage());
                        }
                    }
                }

                ReputationMod.LOGGER.info("Данные репутации загружены. Загружено игроков: " + playerReputation.size());
            } else {
                ReputationMod.LOGGER.info("Файл с данными не найден. Будет создан новый при сохранении.");
            }
        } catch (IOException e) {
            ReputationMod.LOGGER.error("Ошибка при загрузке данных репутации: " + e.getMessage());
        }
    }

    public static String getReputationColor(int reputation) {
        if (reputation == 0) {
            return "§e";
        } else if (reputation < 0) {
            if (reputation >= -250) {
                return "§6";
            } else {
                return "§c";
            }
        } else {
            return "§a";
        }
    }

    public static ChatFormatting getNameColor(int reputation) {
        if (reputation == 0) {
            return COLOR_YELLOW;
        } else if (reputation < 0) {
            if (reputation >= -250) {
                return COLOR_GOLD;
            } else {
                return COLOR_RED;
            }
        } else {
            return COLOR_GREEN;
        }
    }

    public static void updatePlayerNameColor(ServerPlayer player) {
        Scoreboard scoreboard = player.getScoreboard();
        String teamName = "rep_" + player.getUUID().toString().substring(0, 8);

        PlayerTeam team = scoreboard.getPlayerTeam(teamName);
        if (team == null) {
            team = scoreboard.addPlayerTeam(teamName);
        }

        int rep = playerReputation.getOrDefault(player.getUUID(), 0);
        team.setColor(getNameColor(rep));

        if (!team.getPlayers().contains(player.getScoreboardName())) {
            scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
        }
    }

    public static void updateReputationDisplay(ServerPlayer player) {
        Scoreboard scoreboard = player.getScoreboard();
        String repObjectiveName = "reputation";

        Objective objective = scoreboard.getObjective(repObjectiveName);
        if (objective == null) {
            objective = scoreboard.addObjective(
                    repObjectiveName,
                    ObjectiveCriteria.DUMMY,
                    Component.literal("Репутация"),
                    ObjectiveCriteria.RenderType.INTEGER,
                    true,
                    null
            );
        }

        scoreboard.setDisplayObjective(DisplaySlot.LIST, objective);

        int rep = playerReputation.getOrDefault(player.getUUID(), 0);
        scoreboard.getOrCreatePlayerScore(player, objective).set(rep);

        ReputationMod.LOGGER.debug("Репутация игрока {}: {}{}",
                player.getName().getString(),
                getReputationColor(rep),
                rep);
    }

    public static LinkedHashMap<String, Integer> getTopPlayers(int limit) {
        return playerReputation.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue())
                .limit(limit)
                .collect(Collectors.toMap(
                        entry -> playerNames.getOrDefault(entry.getKey(), "Неизвестный"),
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    public static LinkedHashMap<String, Integer> getTopPlayersPositive(int limit) {
        return playerReputation.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(
                        entry -> playerNames.getOrDefault(entry.getKey(), "Неизвестный"),
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    public static LinkedHashMap<String, Integer> getWorstPlayers(int limit) {
        return playerReputation.entrySet().stream()
                .filter(entry -> entry.getValue() < 0)
                .sorted(Map.Entry.<UUID, Integer>comparingByValue())
                .limit(limit)
                .collect(Collectors.toMap(
                        entry -> playerNames.getOrDefault(entry.getKey(), "Неизвестный"),
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    public static LinkedHashMap<String, Integer> getAllPlayersSorted() {
        return playerReputation.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue())
                .collect(Collectors.toMap(
                        entry -> playerNames.getOrDefault(entry.getKey(), "Неизвестный"),
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    public static int getPlayerPosition(UUID playerId) {
        List<UUID> sortedPlayers = playerReputation.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        int position = sortedPlayers.indexOf(playerId) + 1;
        return position > 0 ? position : -1;
    }

    public static void setPlayerReputation(UUID playerId, int newValue) {
        playerReputation.put(playerId, newValue);
        saveData();
    }

    public static int getPlayerReputation(UUID playerId) {
        return playerReputation.getOrDefault(playerId, 0);
    }

    public static void addPlayerReputation(UUID playerId, int amount) {
        int current = playerReputation.getOrDefault(playerId, 0);
        playerReputation.put(playerId, current + amount);
        saveData();
    }
}