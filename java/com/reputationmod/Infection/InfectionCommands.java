package com.reputationmod.infection;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.reputationmod.ReputationMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = ReputationMod.MOD_ID)
public class InfectionCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("infection")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("chance")
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0, 1.0))
                                .executes(context -> {
                                    double chance = DoubleArgumentType.getDouble(context, "value");
                                    InfectionConfig.COMMON.INFECTION_CHANCE.set(chance);
                                    InfectionConfig.COMMON.INFECTION_CHANCE.save();
                                    context.getSource().sendSuccess(() ->
                                            Component.literal("§aШанс заражения установлен: " + chance), true);
                                    return 1;
                                })))
                .then(Commands.literal("duration")
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 86400))
                                .executes(context -> {
                                    int seconds = IntegerArgumentType.getInteger(context, "seconds");
                                    InfectionConfig.COMMON.INFECTION_DURATION.set(seconds);
                                    InfectionConfig.COMMON.INFECTION_DURATION.save();
                                    context.getSource().sendSuccess(() ->
                                            Component.literal("§aДлительность заражения: " + seconds + " сек"), true);
                                    return 1;
                                })))
                .then(Commands.literal("get")
                        .executes(context -> {
                            double chance = InfectionConfig.COMMON.INFECTION_CHANCE.get();
                            int duration = InfectionConfig.COMMON.INFECTION_DURATION.get();
                            context.getSource().sendSuccess(() ->
                                    Component.literal("§6=== Настройки заражения ==="), false);
                            context.getSource().sendSuccess(() ->
                                    Component.literal("§7Шанс: §f" + chance), false);
                            context.getSource().sendSuccess(() ->
                                    Component.literal("§7Длительность: §f" + duration + " сек"), false);
                            return 1;
                        }))
        );
    }
}