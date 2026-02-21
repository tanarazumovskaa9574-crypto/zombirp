package com.reputationmod.crawl;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

public class CrawlShared {
    public static final Pose CRAWLING = Pose.valueOf("CRAWLING");
    public static final EntityDimensions CRAWLING_DIMENSIONS = EntityDimensions.scalable(0.6F, 0.6F).withEyeHeight(0.5F);
    public static final EntityDataAccessor<Boolean> CRAWL_REQUEST = SynchedEntityData.defineId(Player.class, EntityDataSerializers.BOOLEAN);
}