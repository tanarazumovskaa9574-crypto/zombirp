package com.reputationmod.stamina;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class StaminaConfig {
    public static final ModConfigSpec COMMON_SPEC;
    public static final StaminaConfig COMMON;

    static {
        Pair<StaminaConfig, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(StaminaConfig::new);
        COMMON = pair.getLeft();
        COMMON_SPEC = pair.getRight();
    }

    public final ModConfigSpec.IntValue MAX_STAMINA;
    public final ModConfigSpec.IntValue SPRINT_COST_PER_TICK;
    public final ModConfigSpec.IntValue JUMP_COST;
    public final ModConfigSpec.IntValue ATTACK_COST;
    public final ModConfigSpec.IntValue FISHING_COST;
    public final ModConfigSpec.IntValue BLOCK_BREAK_COST;
    public final ModConfigSpec.IntValue PLACE_BLOCK_COST;
    public final ModConfigSpec.IntValue REGEN_RATE;
    public final ModConfigSpec.IntValue REGEN_DELAY;
    public final ModConfigSpec.IntValue MIN_STAMINA_FOR_ACTION;

    private StaminaConfig(ModConfigSpec.Builder builder) {
        builder.push("stamina");

        MAX_STAMINA = builder
                .comment("Maximum stamina points")
                .defineInRange("maxStamina", 100, 1, 1000);

        SPRINT_COST_PER_TICK = builder
                .comment("Stamina cost per tick while sprinting (20 ticks = 1 second)")
                .defineInRange("sprintCostPerTick", 1, 0, 100);

        JUMP_COST = builder
                .comment("Stamina cost for jumping")
                .defineInRange("jumpCost", 10, 0, 100);

        ATTACK_COST = builder
                .comment("Stamina cost for attacking")
                .defineInRange("attackCost", 10, 0, 100);

        FISHING_COST = builder
                .comment("Stamina cost for fishing (per cast)")
                .defineInRange("fishingCost", 10, 0, 100);

        BLOCK_BREAK_COST = builder
                .comment("Stamina cost for breaking a block")
                .defineInRange("blockBreakCost", 10, 0, 100);

        PLACE_BLOCK_COST = builder
                .comment("Stamina cost for placing a block")
                .defineInRange("placeBlockCost", 10, 0, 100);

        REGEN_RATE = builder
                .comment("Stamina regenerated per tick after delay")
                .defineInRange("regenRate", 1, 1, 100);

        REGEN_DELAY = builder
                .comment("Delay in seconds before stamina starts regenerating after last action or movement")
                .defineInRange("regenDelay", 2, 0, 60);

        MIN_STAMINA_FOR_ACTION = builder
                .comment("Minimum stamina required to perform actions (attack, use items). Must be at least 10 to avoid bugs.")
                .defineInRange("minStaminaForAction", 11, 10, 1000); // Минимум 10

        builder.pop();
    }
}