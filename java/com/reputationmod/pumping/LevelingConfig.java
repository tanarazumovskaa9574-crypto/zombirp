package com.reputationmod.stamina;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class LevelingConfig {
    public static final ModConfigSpec COMMON_SPEC;
    public static final LevelingConfig COMMON;

    static {
        Pair<LevelingConfig, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(LevelingConfig::new);
        COMMON = pair.getLeft();
        COMMON_SPEC = pair.getRight();
    }

    public final ModConfigSpec.IntValue BASE_MAX_STAMINA;
    public final ModConfigSpec.IntValue STAMINA_PER_LEVEL;
    public final ModConfigSpec.IntValue EXP_PER_LEVEL;
    public final ModConfigSpec.IntValue EXPERIENCE_COOLDOWN_MS;
    public final ModConfigSpec.ConfigValue<List<? extends Double>> EXP_MULTIPLIERS;
    public final ModConfigSpec.IntValue MAX_LEVEL;

    private LevelingConfig(ModConfigSpec.Builder builder) {
        builder.push("leveling");

        BASE_MAX_STAMINA = builder
                .comment("Base maximum stamina at level 0")
                .defineInRange("baseMaxStamina", 100, 1, 1000);

        STAMINA_PER_LEVEL = builder
                .comment("Stamina increase per level")
                .defineInRange("staminaPerLevel", 10, 1, 100);

        EXP_PER_LEVEL = builder
                .comment("Experience required for level 1 (next levels: level * this value)")
                .defineInRange("expPerLevel", 1000, 1, 10000);

        EXPERIENCE_COOLDOWN_MS = builder
                .comment("Cooldown between experience gains in milliseconds (to prevent farming)")
                .defineInRange("experienceCooldownMs", 200, 0, 5000);

        EXP_MULTIPLIERS = builder
                .comment("Experience multipliers for each level (list of values, one per level)")
                .defineList("expMultipliers",
                        List.of(2.0, 1.5, 1.0, 0.75, 0.75, 0.5),
                        obj -> obj instanceof Double);

        MAX_LEVEL = builder
                .comment("Maximum level (must match the number of multipliers minus one? Actually it's a cap, but ensure consistency)")
                .defineInRange("maxLevel", 5, 1, 100);

        builder.pop();
    }
}