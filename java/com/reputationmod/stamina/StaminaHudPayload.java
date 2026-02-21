package com.reputationmod.stamina;

import com.reputationmod.ReputationMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record StaminaHudPayload(boolean enabled) implements CustomPacketPayload {
    public static final Type<StaminaHudPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ReputationMod.MOD_ID, "stamina_hud")
    );

    public static final StreamCodec<FriendlyByteBuf, StaminaHudPayload> STREAM_CODEC =
            StreamCodec.ofMember(StaminaHudPayload::write, StaminaHudPayload::new);

    public StaminaHudPayload(FriendlyByteBuf buf) {
        this(buf.readBoolean());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(enabled);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleData(final StaminaHudPayload data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            StaminaClientSettings.setHudEnabled(data.enabled());
        });
    }
}