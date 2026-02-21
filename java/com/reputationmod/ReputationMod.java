package com.reputationmod;

import com.reputationmod.block.ModBlocks;
import com.reputationmod.block.entity.ModBlockEntities;
import com.reputationmod.block.entity.ProtectionChestBlockEntity;
import com.reputationmod.creative.ModCreativeTab;
import com.reputationmod.damage.ModDamageTypes;
import com.reputationmod.infection.InfectionConfig;
import com.reputationmod.infection.ModEffects;
import com.reputationmod.item.ModItems;
import com.reputationmod.sound.ModSounds;
import com.reputationmod.stamina.LevelingConfig;
import com.reputationmod.stamina.StaminaAttachment;
import com.reputationmod.stamina.StaminaConfig;
import com.reputationmod.stamina.StaminaNetwork;
import com.reputationmod.crawl.CrawlCommand;
import com.reputationmod.crawl.CrawlConfig;
import com.reputationmod.crawl.CrawlPacket;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(ReputationMod.MOD_ID)
public class ReputationMod {
    public static final String MOD_ID = "reputationmod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ReputationMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Reputation Mod загружен!");

        // Регистрация блоков и предметов
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);

        // Регистрация BlockEntity
        ModBlockEntities.register(modEventBus);

        // Регистрация творческой вкладки
        ModCreativeTab.register(modEventBus);

        // Регистрация системы выносливости
        StaminaAttachment.ATTACHMENT_TYPES.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, StaminaConfig.COMMON_SPEC, "reputationmod-stamina.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, LevelingConfig.COMMON_SPEC, "reputationmod-leveling.toml");

        // Регистрация системы заражения
        ModEffects.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, InfectionConfig.COMMON_SPEC, "reputationmod-infection.toml");

        // Регистрация звуков
        ModSounds.register(modEventBus);

        // Регистрация кастомных типов урона
        ModDamageTypes.register(modEventBus);

        // Регистрация сетевых пакетов
        modEventBus.addListener(this::registerPackets);

        // Конфиг для ползания
        modContainer.registerConfig(ModConfig.Type.COMMON, CrawlConfig.SPEC, "reputationmod-crawl.toml");
    }

    private void registerPackets(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        StaminaNetwork.register(registrar);
        // Регистрация пакета ползания
        registrar.playToServer(CrawlPacket.TYPE, CrawlPacket.STREAM_CODEC, (packet, context) -> packet.handle(context));
    }

    @EventBusSubscriber(modid = MOD_ID)
    public static class ModCommands {

        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
            CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

            dispatcher.register(Commands.literal("rep")
                    // /rep - показать свою репутацию
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        int rep = ReputationEvents.getPlayerReputation(player.getUUID());
                        String color = ReputationEvents.getReputationColor(rep);
                        context.getSource().sendSuccess(() ->
                                Component.literal("§6[Репутация] §fВаша репутация: " + color + rep), false);
                        return 1;
                    })

                    // /rep get <игрок> - посмотреть репутацию игрока
                    .then(Commands.literal("get")
                            .then(Commands.argument("player", EntityArgument.player())
                                    .executes(context -> {
                                        ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                        int rep = ReputationEvents.getPlayerReputation(target.getUUID());
                                        String color = ReputationEvents.getReputationColor(rep);
                                        context.getSource().sendSuccess(() ->
                                                Component.literal("§6[Репутация] §fРепутация игрока §e" +
                                                        target.getName().getString() + "§f: " + color + rep), false);
                                        return 1;
                                    })
                            )
                    )

                    // /rep add <игрок> <количество> - добавить или забрать репутацию
                    .then(Commands.literal("add")
                            .requires(source -> source.hasPermission(2))
                            .then(Commands.argument("player", EntityArgument.player())
                                    .then(Commands.argument("amount", IntegerArgumentType.integer())
                                            .executes(context -> {
                                                ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                                int amount = IntegerArgumentType.getInteger(context, "amount");

                                                ReputationEvents.addPlayerReputation(target.getUUID(), amount);
                                                ReputationEvents.updateReputationDisplay(target);
                                                ReputationEvents.updatePlayerNameColor(target);

                                                int newRep = ReputationEvents.getPlayerReputation(target.getUUID());
                                                String color = amount > 0 ? "§a" : "§c";
                                                String sign = amount > 0 ? "+" : "";

                                                context.getSource().sendSuccess(() ->
                                                        Component.literal("§6[Репутация] §f" +
                                                                (amount > 0 ? "Добавлено" : "Снято") + " §e" +
                                                                target.getName().getString() + "§f: " + color + sign + amount +
                                                                " §7(Теперь: " + newRep + ")"), true);

                                                String msgColor = amount > 0 ? "§a" : "§c";
                                                target.sendSystemMessage(
                                                        Component.literal("§6[Репутация] §fВаша репутация " +
                                                                (amount > 0 ? "увеличена" : "уменьшена") + " на: " +
                                                                msgColor + sign + amount)
                                                );
                                                return 1;
                                            })
                                    )
                            )
                    )

                    // /rep top - таблица лидеров (самые отрицательные)
                    .then(Commands.literal("top")
                            .executes(context -> {
                                ServerPlayer player = context.getSource().getPlayerOrException();

                                context.getSource().sendSuccess(() ->
                                        Component.literal("§6╔══════════════════════════════════════╗"), false);
                                context.getSource().sendSuccess(() ->
                                        Component.literal("§6║     §lАНТИ-ТОП (САМЫЕ ПЛОХИЕ)§r      §6║"), false);
                                context.getSource().sendSuccess(() ->
                                        Component.literal("§6╠══════════════════════════════════════╣"), false);

                                var topPlayers = ReputationEvents.getTopPlayers(10);

                                if (topPlayers.isEmpty()) {
                                    context.getSource().sendSuccess(() ->
                                            Component.literal("§6║     §cПока нет игроков с репутацией    §6║"), false);
                                } else {
                                    int position = 1;
                                    for (var entry : topPlayers.entrySet()) {
                                        final int pos = position;
                                        String playerName = entry.getKey();
                                        int rep = entry.getValue();
                                        String repColor = ReputationEvents.getReputationColor(rep);

                                        String medal = pos == 1 ? "§c💀" : (pos == 2 ? "§6👿" : (pos == 3 ? "§8👹" : "  "));

                                        context.getSource().sendSuccess(() ->
                                                Component.literal(String.format("§6║ §f%d. %s %s§f: " + repColor + "%d",
                                                        pos, medal, playerName, rep)), false);
                                        position++;
                                    }
                                }

                                context.getSource().sendSuccess(() ->
                                        Component.literal("§6╚══════════════════════════════════════╝"), false);

                                int playerPos = ReputationEvents.getPlayerPosition(player.getUUID());
                                if (playerPos > 0) {
                                    int playerRep = ReputationEvents.getPlayerReputation(player.getUUID());
                                    String playerColor = ReputationEvents.getReputationColor(playerRep);
                                    context.getSource().sendSuccess(() ->
                                            Component.literal("§7Ваше место в анти-топе: §f" + playerPos + " §7(" + playerColor + playerRep + "§7)"), false);
                                }

                                return 1;
                            })
                    )

                    // /rep top positive - показать топ положительных
                    .then(Commands.literal("top")
                            .then(Commands.literal("positive")
                                    .executes(context -> {
                                        context.getSource().sendSuccess(() ->
                                                Component.literal("§6╔══════════════════════════════════════╗"), false);
                                        context.getSource().sendSuccess(() ->
                                                Component.literal("§6║       §lТОП ПОЛОЖИТЕЛЬНЫХ§r          §6║"), false);
                                        context.getSource().sendSuccess(() ->
                                                Component.literal("§6╠══════════════════════════════════════╣"), false);

                                        var topPlayers = ReputationEvents.getTopPlayersPositive(10);

                                        if (topPlayers.isEmpty()) {
                                            context.getSource().sendSuccess(() ->
                                                    Component.literal("§6║     §cНет игроков с положительной репой    §6║"), false);
                                        } else {
                                            int position = 1;
                                            for (var entry : topPlayers.entrySet()) {
                                                final int pos = position;
                                                String playerName = entry.getKey();
                                                int rep = entry.getValue();
                                                String repColor = ReputationEvents.getReputationColor(rep);

                                                String medal = pos == 1 ? "§6👑" : (pos == 2 ? "§7🥈" : (pos == 3 ? "§6🥉" : "  "));

                                                context.getSource().sendSuccess(() ->
                                                        Component.literal(String.format("§6║ §f%d. %s %s§f: " + repColor + "%d",
                                                                pos, medal, playerName, rep)), false);
                                                position++;
                                            }
                                        }

                                        context.getSource().sendSuccess(() ->
                                                Component.literal("§6╚══════════════════════════════════════╝"), false);
                                        return 1;
                                    })
                            )
                    )

                    // /rep worst - показать самых плохих
                    .then(Commands.literal("worst")
                            .executes(context -> {
                                context.getSource().sendSuccess(() ->
                                        Component.literal("§6╔══════════════════════════════════════╗"), false);
                                context.getSource().sendSuccess(() ->
                                        Component.literal("§6║       §lСАМЫЕ ОТРИЦАТЕЛЬНЫЕ§r         §6║"), false);
                                context.getSource().sendSuccess(() ->
                                        Component.literal("§6╠══════════════════════════════════════╣"), false);

                                var worstPlayers = ReputationEvents.getWorstPlayers(10);

                                if (worstPlayers.isEmpty()) {
                                    context.getSource().sendSuccess(() ->
                                            Component.literal("§6║     §cНет игроков с отрицательной репой    §6║"), false);
                                } else {
                                    int position = 1;
                                    for (var entry : worstPlayers.entrySet()) {
                                        final int pos = position;
                                        String playerName = entry.getKey();
                                        int rep = entry.getValue();
                                        String repColor = ReputationEvents.getReputationColor(rep);

                                        context.getSource().sendSuccess(() ->
                                                Component.literal(String.format("§6║ §f%d. %s§f: " + repColor + "%d",
                                                        pos, playerName, rep)), false);
                                        position++;
                                    }
                                }

                                context.getSource().sendSuccess(() ->
                                        Component.literal("§6╚══════════════════════════════════════╝"), false);
                                return 1;
                            })
                    )

                    // /rep save - принудительное сохранение
                    .then(Commands.literal("save")
                            .requires(source -> source.hasPermission(2))
                            .executes(context -> {
                                ReputationEvents.saveData();
                                context.getSource().sendSuccess(() ->
                                        Component.literal("§a[Репутация] Данные сохранены"), true);
                                return 1;
                            })
                    )

                    // /rep reload - перезагрузить данные
                    .then(Commands.literal("reload")
                            .requires(source -> source.hasPermission(2))
                            .executes(context -> {
                                ReputationEvents.loadData();
                                context.getSource().sendSuccess(() ->
                                        Component.literal("§a[Репутация] Данные перезагружены"), true);
                                return 1;
                            })
                    )

                    // ============ КОМАНДЫ ДЛЯ ЗОН ============

                    // /zone pos1 - установить первую точку
                    .then(Commands.literal("zone")
                            .then(Commands.literal("pos1")
                                    .executes(context -> {
                                        ServerPlayer player = context.getSource().getPlayerOrException();
                                        ReputationEvents.setTempPos1(player);
                                        return 1;
                                    })
                            )
                    )

                    // /zone pos2 - установить вторую точку
                    .then(Commands.literal("zone")
                            .then(Commands.literal("pos2")
                                    .executes(context -> {
                                        ServerPlayer player = context.getSource().getPlayerOrException();
                                        ReputationEvents.setTempPos2(player);
                                        return 1;
                                    })
                            )
                    )

                    // /zone create <порог> - создать зону
                    .then(Commands.literal("zone")
                            .then(Commands.literal("create")
                                    .then(Commands.argument("minRep", IntegerArgumentType.integer(-1000, 1000))
                                            .executes(context -> {
                                                int minRep = IntegerArgumentType.getInteger(context, "minRep");
                                                ReputationEvents.createZone(minRep, context.getSource());
                                                return 1;
                                            })
                                    )
                            )
                    )

                    // /zone list - показать все зоны
                    .then(Commands.literal("zone")
                            .then(Commands.literal("list")
                                    .executes(context -> {
                                        ReputationEvents.listZones(context.getSource());
                                        return 1;
                                    })
                            )
                    )

                    // /zone remove <индекс> - удалить зону
                    .then(Commands.literal("zone")
                            .then(Commands.literal("remove")
                                    .then(Commands.argument("index", IntegerArgumentType.integer(0))
                                            .executes(context -> {
                                                int index = IntegerArgumentType.getInteger(context, "index");
                                                ReputationEvents.removeZone(index);
                                                context.getSource().sendSuccess(() ->
                                                        Component.literal("§aЗона удалена!"), true);
                                                return 1;
                                            })
                                    )
                            )
                    )

                    // /zone highlight <индекс> - подсветить границы зоны
                    .then(Commands.literal("zone")
                            .then(Commands.literal("highlight")
                                    .then(Commands.argument("index", IntegerArgumentType.integer(0))
                                            .executes(context -> {
                                                int index = IntegerArgumentType.getInteger(context, "index");
                                                ServerPlayer player = context.getSource().getPlayerOrException();
                                                ReputationEvents.highlightZone(index, player);
                                                return 1;
                                            })
                                    )
                            )
                    )

                    // ============ РАДИАЦИОННЫЕ ЗОНЫ ============

                    // /rad pos1 - первая точка
                    .then(Commands.literal("rad")
                            .then(Commands.literal("pos1")
                                    .executes(context -> {
                                        ServerPlayer player = context.getSource().getPlayerOrException();
                                        RadiationHandler.setRadPos1(player);
                                        return 1;
                                    })
                            )
                    )

                    // /rad pos2 - вторая точка
                    .then(Commands.literal("rad")
                            .then(Commands.literal("pos2")
                                    .executes(context -> {
                                        ServerPlayer player = context.getSource().getPlayerOrException();
                                        RadiationHandler.setRadPos2(player);
                                        return 1;
                                    })
                            )
                    )

                    // /rad create <уровень> - создать зону
                    .then(Commands.literal("rad")
                            .then(Commands.literal("create")
                                    .then(Commands.argument("level", IntegerArgumentType.integer(1, 5))
                                            .executes(context -> {
                                                int level = IntegerArgumentType.getInteger(context, "level");
                                                RadiationHandler.createRadiationZone(level, context.getSource());
                                                return 1;
                                            })
                                    )
                            )
                    )

                    // /rad list - список зон
                    .then(Commands.literal("rad")
                            .then(Commands.literal("list")
                                    .executes(context -> {
                                        RadiationHandler.listRadiationZones(context.getSource());
                                        return 1;
                                    })
                            )
                    )

                    // /rad remove <индекс> - удалить зону
                    .then(Commands.literal("rad")
                            .then(Commands.literal("remove")
                                    .then(Commands.argument("index", IntegerArgumentType.integer(0))
                                            .executes(context -> {
                                                int index = IntegerArgumentType.getInteger(context, "index");
                                                RadiationHandler.removeRadiationZone(index);
                                                context.getSource().sendSuccess(() ->
                                                        Component.literal("§cЗона удалена!"), true);
                                                return 1;
                                            })
                                    )
                            )
                    )

                    // /rad highlight <индекс> - подсветить зону
                    .then(Commands.literal("rad")
                            .then(Commands.literal("highlight")
                                    .then(Commands.argument("index", IntegerArgumentType.integer(0))
                                            .executes(context -> {
                                                int index = IntegerArgumentType.getInteger(context, "index");
                                                ServerPlayer player = context.getSource().getPlayerOrException();
                                                RadiationHandler.highlightRadiationZone(index, player);
                                                return 1;
                                            })
                                    )
                            )
                    )

                    // /gasmask - показать информацию о зачаровании
                    .then(Commands.literal("gasmask")
                            .executes(context -> {
                                ServerPlayer player = context.getSource().getPlayerOrException();
                                RadiationHandler.makeGasMask(player);
                                return 1;
                            })
                    )
            );

            // ============ КОМАНДЫ ДЛЯ ЗАЩИТЫ (по взгляду) ============

            dispatcher.register(Commands.literal("protection")
                    .then(Commands.literal("info")
                            .executes(context -> {
                                ServerPlayer player = context.getSource().getPlayerOrException();
                                BlockPos targetPos = getTargetBlockPos(player);
                                if (targetPos == null) {
                                    context.getSource().sendFailure(Component.literal("§cВы не смотрите на блок!"));
                                    return 0;
                                }
                                BlockState state = player.level().getBlockState(targetPos);
                                if (state.getBlock() == ModBlocks.PROTECTION_CHEST.get()) {
                                    BlockEntity be = player.level().getBlockEntity(targetPos);
                                    if (be instanceof ProtectionChestBlockEntity chest) {
                                        int count = chest.getProtectedBlocks().size();
                                        boolean active = chest.isActive();
                                        int minutesLeft = chest.getMinutesLeft();
                                        context.getSource().sendSuccess(() ->
                                                Component.literal("§6[Защита] Активен: " + active +
                                                        ", Осталось минут: " + minutesLeft +
                                                        ", Блоков в доме: " + count), false);
                                    } else {
                                        context.getSource().sendSuccess(() -> Component.literal("§cУ сундука нет данных!"), false);
                                    }
                                } else {
                                    context.getSource().sendSuccess(() -> Component.literal("§cВы смотрите не на сундук защиты!"), false);
                                }
                                return 1;
                            })
                    )
                    .then(Commands.literal("highlight")
                            .executes(context -> {
                                ServerPlayer player = context.getSource().getPlayerOrException();
                                BlockPos targetPos = getTargetBlockPos(player);
                                if (targetPos == null) {
                                    context.getSource().sendFailure(Component.literal("§cВы не смотрите на блок!"));
                                    return 0;
                                }
                                BlockState state = player.level().getBlockState(targetPos);
                                if (state.getBlock() == ModBlocks.PROTECTION_CHEST.get()) {
                                    BlockEntity be = player.level().getBlockEntity(targetPos);
                                    if (be instanceof ProtectionChestBlockEntity chest) {
                                        int radius = 10;
                                        BlockPos center = chest.getBlockPos();
                                        for (int x = -radius; x <= radius; x++) {
                                            for (int y = -radius; y <= radius; y++) {
                                                for (int z = -radius; z <= radius; z++) {
                                                    if (Math.abs(x) == radius || Math.abs(y) == radius || Math.abs(z) == radius) {
                                                        player.serverLevel().sendParticles(
                                                                ParticleTypes.END_ROD,
                                                                center.getX() + x + 0.5,
                                                                center.getY() + y + 0.5,
                                                                center.getZ() + z + 0.5,
                                                                1, 0, 0, 0, 0
                                                        );
                                                    }
                                                }
                                            }
                                        }
                                        context.getSource().sendSuccess(() ->
                                                Component.literal("§aГраницы зоны защиты подсвечены!"), false);
                                    } else {
                                        context.getSource().sendSuccess(() -> Component.literal("§cУ сундука нет данных!"), false);
                                    }
                                } else {
                                    context.getSource().sendSuccess(() -> Component.literal("§cВы смотрите не на сундук защиты!"), false);
                                }
                                return 1;
                            })
                    )
            );

            // Добавляем команду для ползания
            CrawlCommand.register(dispatcher);
        }

        private static BlockPos getTargetBlockPos(ServerPlayer player) {
            Vec3 eyePos = player.getEyePosition(1.0F);
            Vec3 lookVec = player.getLookAngle();
            Vec3 endPos = eyePos.add(lookVec.x * 20, lookVec.y * 20, lookVec.z * 20);
            ClipContext context = new ClipContext(eyePos, endPos, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player);
            BlockHitResult result = player.level().clip(context);
            if (result.getType() == HitResult.Type.BLOCK) {
                return result.getBlockPos();
            }
            return null;
        }
    }
}