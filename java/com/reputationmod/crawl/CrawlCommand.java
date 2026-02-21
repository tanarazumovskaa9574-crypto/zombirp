package com.reputationmod.crawl;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class CrawlCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("crawl")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("cooldown")
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(0, 60))
                                .executes(context -> setCooldown(context.getSource(), IntegerArgumentType.getInteger(context, "seconds"))))));
    }

    private static int setCooldown(CommandSourceStack source, int seconds) {
        CrawlConfig.INSTANCE.crawlCooldown.set(seconds);
        CrawlConfig.SPEC.save();
        source.sendSuccess(() -> Component.literal("Кулдаун ползания установлен на " + seconds + " секунд."), true);
        return 1;
    }
}