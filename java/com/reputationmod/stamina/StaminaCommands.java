package com.reputationmod.stamina;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import com.reputationmod.ReputationMod;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = ReputationMod.MOD_ID)
public class StaminaCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("stamina")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("maxstamina")
                        .then(Commands.argument("value", IntegerArgumentType.integer(1, 1000))
                                .executes(context -> setMaxStamina(context, IntegerArgumentType.getInteger(context, "value")))))
                .then(Commands.literal("sprintcost")
                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                                .executes(context -> setSprintCost(context, IntegerArgumentType.getInteger(context, "value")))))
                .then(Commands.literal("jumpcost")
                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                                .executes(context -> setJumpCost(context, IntegerArgumentType.getInteger(context, "value")))))
                .then(Commands.literal("attackcost")
                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                                .executes(context -> setAttackCost(context, IntegerArgumentType.getInteger(context, "value")))))
                .then(Commands.literal("fishingcost")
                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                                .executes(context -> setFishingCost(context, IntegerArgumentType.getInteger(context, "value")))))
                .then(Commands.literal("blockbreakcost")
                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                                .executes(context -> setBlockBreakCost(context, IntegerArgumentType.getInteger(context, "value")))))
                .then(Commands.literal("placeblockcost")
                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                                .executes(context -> setPlaceBlockCost(context, IntegerArgumentType.getInteger(context, "value")))))
                .then(Commands.literal("regenrate")
                        .then(Commands.argument("value", IntegerArgumentType.integer(1, 100))
                                .executes(context -> setRegenRate(context, IntegerArgumentType.getInteger(context, "value")))))
                .then(Commands.literal("regendelay")
                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 60))
                                .executes(context -> setRegenDelay(context, IntegerArgumentType.getInteger(context, "value")))))
                .then(Commands.literal("actionthreshold")
                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 1000))
                                .executes(context -> setActionThreshold(context, IntegerArgumentType.getInteger(context, "value")))))
                .then(Commands.literal("hud")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> setHudEnabled(context, BoolArgumentType.getBool(context, "enabled")))))
                // ========== КОМАНДЫ ПРОКАЧКИ ==========
                .then(Commands.literal("level")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            var data = player.getData(StaminaAttachment.STAMINA_DATA);
                            int level = data.getLevel();
                            int exp = data.getExperience();
                            int required = data.getRequiredExpForNextLevel();
                            int stamina = data.getStamina();
                            int maxStamina = data.getMaxStamina();
                            context.getSource().sendSuccess(() ->
                                    Component.literal(String.format("§6Уровень: %d, Опыт: %d/%d, Стамина: %d/%d",
                                            level, exp, required, stamina, maxStamina)), false);
                            return 1;
                        })
                )
                .then(Commands.literal("setlevel")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("level", IntegerArgumentType.integer(0, 100))
                                .executes(context -> {
                                    int level = IntegerArgumentType.getInteger(context, "level");
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    var data = player.getData(StaminaAttachment.STAMINA_DATA);
                                    data.setLevel(level);
                                    StaminaNetwork.syncToClient(player);
                                    context.getSource().sendSuccess(() ->
                                            Component.literal("§aУровень стамины установлен на " + level), true);
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("resetlevel")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            var data = player.getData(StaminaAttachment.STAMINA_DATA);
                            // Сбрасываем на начальные значения (без удаления, просто устанавливаем)
                            data.setLevel(0);
                            data.setExperience(0);
                            data.setMaxStamina(LevelingConfig.COMMON.BASE_MAX_STAMINA.get());
                            data.setStamina(LevelingConfig.COMMON.BASE_MAX_STAMINA.get());
                            StaminaNetwork.syncToClient(player);
                            context.getSource().sendSuccess(() ->
                                    Component.literal("§aПрогресс прокачки сброшен!"), true);
                            return 1;
                        })
                )
                // ========== КОМАНДЫ КОНФИГА ПРОКАЧКИ ==========
                .then(Commands.literal("config")
                        .then(Commands.literal("baseMaxStamina")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("value", IntegerArgumentType.integer(1, 1000))
                                        .executes(context -> {
                                            int value = IntegerArgumentType.getInteger(context, "value");
                                            LevelingConfig.COMMON.BASE_MAX_STAMINA.set(value);
                                            LevelingConfig.COMMON.BASE_MAX_STAMINA.save();
                                            context.getSource().sendSuccess(() ->
                                                    Component.literal("§aБазовая стамина установлена: " + value), true);
                                            return 1;
                                        })))
                        .then(Commands.literal("staminaPerLevel")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("value", IntegerArgumentType.integer(1, 100))
                                        .executes(context -> {
                                            int value = IntegerArgumentType.getInteger(context, "value");
                                            LevelingConfig.COMMON.STAMINA_PER_LEVEL.set(value);
                                            LevelingConfig.COMMON.STAMINA_PER_LEVEL.save();
                                            context.getSource().sendSuccess(() ->
                                                    Component.literal("§aСтамина за уровень: " + value), true);
                                            return 1;
                                        })))
                        .then(Commands.literal("expPerLevel")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("value", IntegerArgumentType.integer(1, 10000))
                                        .executes(context -> {
                                            int value = IntegerArgumentType.getInteger(context, "value");
                                            LevelingConfig.COMMON.EXP_PER_LEVEL.set(value);
                                            LevelingConfig.COMMON.EXP_PER_LEVEL.save();
                                            context.getSource().sendSuccess(() ->
                                                    Component.literal("§aОпыта на уровень: " + value), true);
                                            return 1;
                                        })))
                        .then(Commands.literal("expCooldown")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("value", IntegerArgumentType.integer(0, 5000))
                                        .executes(context -> {
                                            int value = IntegerArgumentType.getInteger(context, "value");
                                            LevelingConfig.COMMON.EXPERIENCE_COOLDOWN_MS.set(value);
                                            LevelingConfig.COMMON.EXPERIENCE_COOLDOWN_MS.save();
                                            context.getSource().sendSuccess(() ->
                                                    Component.literal("§aЗадержка опыта: " + value + " мс"), true);
                                            return 1;
                                        })))
                        .then(Commands.literal("expMultipliers")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("values", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            String valuesStr = StringArgumentType.getString(context, "values");
                                            String[] parts = valuesStr.split(" ");
                                            List<Double> list = new ArrayList<>();
                                            try {
                                                for (String p : parts) {
                                                    list.add(Double.parseDouble(p));
                                                }
                                                LevelingConfig.COMMON.EXP_MULTIPLIERS.set(list);
                                                LevelingConfig.COMMON.EXP_MULTIPLIERS.save();
                                                context.getSource().sendSuccess(() ->
                                                        Component.literal("§aМножители обновлены: " + list), true);
                                            } catch (NumberFormatException e) {
                                                context.getSource().sendFailure(Component.literal("§cНеверный формат чисел"));
                                            }
                                            return 1;
                                        })))
                        .then(Commands.literal("maxLevel")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("value", IntegerArgumentType.integer(1, 100))
                                        .executes(context -> {
                                            int value = IntegerArgumentType.getInteger(context, "value");
                                            LevelingConfig.COMMON.MAX_LEVEL.set(value);
                                            LevelingConfig.COMMON.MAX_LEVEL.save();
                                            context.getSource().sendSuccess(() ->
                                                    Component.literal("§aМаксимальный уровень: " + value), true);
                                            return 1;
                                        })))
                        .then(Commands.literal("show")
                                .executes(context -> {
                                    context.getSource().sendSuccess(() ->
                                            Component.literal("§6=== Конфиг прокачки ==="), false);
                                    context.getSource().sendSuccess(() ->
                                            Component.literal("§7baseMaxStamina: §f" + LevelingConfig.COMMON.BASE_MAX_STAMINA.get()), false);
                                    context.getSource().sendSuccess(() ->
                                            Component.literal("§7staminaPerLevel: §f" + LevelingConfig.COMMON.STAMINA_PER_LEVEL.get()), false);
                                    context.getSource().sendSuccess(() ->
                                            Component.literal("§7expPerLevel: §f" + LevelingConfig.COMMON.EXP_PER_LEVEL.get()), false);
                                    context.getSource().sendSuccess(() ->
                                            Component.literal("§7expCooldown: §f" + LevelingConfig.COMMON.EXPERIENCE_COOLDOWN_MS.get() + " мс"), false);
                                    context.getSource().sendSuccess(() ->
                                            Component.literal("§7expMultipliers: §f" + LevelingConfig.COMMON.EXP_MULTIPLIERS.get()), false);
                                    context.getSource().sendSuccess(() ->
                                            Component.literal("§7maxLevel: §f" + LevelingConfig.COMMON.MAX_LEVEL.get()), false);
                                    return 1;
                                }))
                )
                .then(Commands.literal("get")
                        .executes(context -> showAll(context)))
        );
    }

    private static int setMaxStamina(CommandContext<CommandSourceStack> context, int value) {
        StaminaConfig.COMMON.MAX_STAMINA.set(value);
        StaminaConfig.COMMON.MAX_STAMINA.save();
        context.getSource().sendSuccess(() -> Component.literal("§aМаксимум выносливости установлен: " + value), true);
        return 1;
    }

    private static int setSprintCost(CommandContext<CommandSourceStack> context, int value) {
        StaminaConfig.COMMON.SPRINT_COST_PER_TICK.set(value);
        StaminaConfig.COMMON.SPRINT_COST_PER_TICK.save();
        context.getSource().sendSuccess(() -> Component.literal("§aСтоимость бега за тик: " + value), true);
        return 1;
    }

    private static int setJumpCost(CommandContext<CommandSourceStack> context, int value) {
        StaminaConfig.COMMON.JUMP_COST.set(value);
        StaminaConfig.COMMON.JUMP_COST.save();
        context.getSource().sendSuccess(() -> Component.literal("§aСтоимость прыжка: " + value), true);
        return 1;
    }

    private static int setAttackCost(CommandContext<CommandSourceStack> context, int value) {
        StaminaConfig.COMMON.ATTACK_COST.set(value);
        StaminaConfig.COMMON.ATTACK_COST.save();
        context.getSource().sendSuccess(() -> Component.literal("§aСтоимость атаки: " + value), true);
        return 1;
    }

    private static int setFishingCost(CommandContext<CommandSourceStack> context, int value) {
        StaminaConfig.COMMON.FISHING_COST.set(value);
        StaminaConfig.COMMON.FISHING_COST.save();
        context.getSource().sendSuccess(() -> Component.literal("§aСтоимость рыбалки: " + value), true);
        return 1;
    }

    private static int setBlockBreakCost(CommandContext<CommandSourceStack> context, int value) {
        StaminaConfig.COMMON.BLOCK_BREAK_COST.set(value);
        StaminaConfig.COMMON.BLOCK_BREAK_COST.save();
        context.getSource().sendSuccess(() -> Component.literal("§aСтоимость ломания блока: " + value), true);
        return 1;
    }

    private static int setPlaceBlockCost(CommandContext<CommandSourceStack> context, int value) {
        StaminaConfig.COMMON.PLACE_BLOCK_COST.set(value);
        StaminaConfig.COMMON.PLACE_BLOCK_COST.save();
        context.getSource().sendSuccess(() -> Component.literal("§aСтоимость установки блока: " + value), true);
        return 1;
    }

    private static int setRegenRate(CommandContext<CommandSourceStack> context, int value) {
        StaminaConfig.COMMON.REGEN_RATE.set(value);
        StaminaConfig.COMMON.REGEN_RATE.save();
        context.getSource().sendSuccess(() -> Component.literal("§aСкорость регенерации: " + value + " за тик"), true);
        return 1;
    }

    private static int setRegenDelay(CommandContext<CommandSourceStack> context, int value) {
        StaminaConfig.COMMON.REGEN_DELAY.set(value);
        StaminaConfig.COMMON.REGEN_DELAY.save();
        context.getSource().sendSuccess(() -> Component.literal("§aЗадержка регенерации: " + value + " сек"), true);
        return 1;
    }

    private static int setActionThreshold(CommandContext<CommandSourceStack> context, int value) {
        StaminaConfig.COMMON.MIN_STAMINA_FOR_ACTION.set(value);
        StaminaConfig.COMMON.MIN_STAMINA_FOR_ACTION.save();
        context.getSource().sendSuccess(() -> Component.literal("§aПорог для действий: " + value), true);
        return 1;
    }

    private static int setHudEnabled(CommandContext<CommandSourceStack> context, boolean enabled) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            player.connection.send(new ClientboundCustomPayloadPacket(
                    new StaminaHudPayload(enabled)
            ));
            context.getSource().sendSuccess(() ->
                    Component.literal("§aHUD выносливости " + (enabled ? "включён" : "отключён")), false);
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cОшибка: " + e.getMessage()));
        }
        return 1;
    }

    private static int showAll(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("§6=== Текущие настройки выносливости ==="), false);
        context.getSource().sendSuccess(() -> Component.literal("§7Максимум: §f" + StaminaConfig.COMMON.MAX_STAMINA.get()), false);
        context.getSource().sendSuccess(() -> Component.literal("§7Бег за тик: §f" + StaminaConfig.COMMON.SPRINT_COST_PER_TICK.get()), false);
        context.getSource().sendSuccess(() -> Component.literal("§7Прыжок: §f" + StaminaConfig.COMMON.JUMP_COST.get()), false);
        context.getSource().sendSuccess(() -> Component.literal("§7Атака: §f" + StaminaConfig.COMMON.ATTACK_COST.get()), false);
        context.getSource().sendSuccess(() -> Component.literal("§7Рыбалка: §f" + StaminaConfig.COMMON.FISHING_COST.get()), false);
        context.getSource().sendSuccess(() -> Component.literal("§7Ломание блока: §f" + StaminaConfig.COMMON.BLOCK_BREAK_COST.get()), false);
        context.getSource().sendSuccess(() -> Component.literal("§7Установка блока: §f" + StaminaConfig.COMMON.PLACE_BLOCK_COST.get()), false);
        context.getSource().sendSuccess(() -> Component.literal("§7Скорость регенерации: §f" + StaminaConfig.COMMON.REGEN_RATE.get() + " за тик"), false);
        context.getSource().sendSuccess(() -> Component.literal("§7Задержка регенерации: §f" + StaminaConfig.COMMON.REGEN_DELAY.get() + " сек"), false);
        context.getSource().sendSuccess(() -> Component.literal("§7Порог для действий: §f" + StaminaConfig.COMMON.MIN_STAMINA_FOR_ACTION.get()), false);
        return 1;
    }
}