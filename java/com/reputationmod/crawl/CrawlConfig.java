package com.reputationmod.crawl;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class CrawlConfig {
    public static final ModConfigSpec SPEC;
    public static final CrawlConfig INSTANCE;

    static {
        final Pair<CrawlConfig, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(CrawlConfig::new);
        INSTANCE = pair.getLeft();
        SPEC = pair.getRight();
    }

    public final ModConfigSpec.IntValue crawlCooldown;

    private CrawlConfig(ModConfigSpec.Builder builder) {
        builder.comment("Crawl settings").push("crawl");
        crawlCooldown = builder
                .comment("Cooldown between crawl toggles in seconds (default 2)")
                .defineInRange("cooldown", 2, 0, 60);
        builder.pop();
    }
}