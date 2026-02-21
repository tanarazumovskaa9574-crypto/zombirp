package com.reputationmod.infection;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class InfectionConfig {
    public static final ModConfigSpec COMMON_SPEC;
    public static final InfectionConfig COMMON;

    static {
        Pair<InfectionConfig, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(InfectionConfig::new);
        COMMON = pair.getLeft();
        COMMON_SPEC = pair.getRight();
    }

    public final ModConfigSpec.DoubleValue INFECTION_CHANCE;
    public final ModConfigSpec.IntValue INFECTION_DURATION; // в секундах

    private InfectionConfig(ModConfigSpec.Builder builder) {
        builder.push("infection");

        INFECTION_CHANCE = builder
                .comment("Chance (0.0-1.0) for a zombie to infect a player on hit")
                .defineInRange("infectionChance", 0.05, 0.0, 1.0);

        INFECTION_DURATION = builder
                .comment("Duration of infection in seconds (converted to ticks internally)")
                .defineInRange("infectionDuration", 30, 1, 86400); // 30 секунд для теста

        builder.pop();
    }
}