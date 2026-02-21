package com.reputationmod.crawl;

import com.reputationmod.ReputationMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CrawlPacket() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CrawlPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(ReputationMod.MOD_ID, "crawl")
    );
    public static final StreamCodec<FriendlyByteBuf, CrawlPacket> STREAM_CODEC = StreamCodec.unit(new CrawlPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                CrawlHandler.toggleCrawl(serverPlayer);
            }
        });
    }
}