package com.reputationmod.stamina;

import com.reputationmod.ReputationMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record StaminaSyncPayload(int stamina, int maxStamina) implements CustomPacketPayload {
    public static final Type<StaminaSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ReputationMod.MOD_ID, "stamina_sync")
    );

    public static final StreamCodec<FriendlyByteBuf, StaminaSyncPayload> STREAM_CODEC =
            StreamCodec.ofMember(StaminaSyncPayload::write, StaminaSyncPayload::new);

    public StaminaSyncPayload(FriendlyByteBuf buf) {
        this(buf.readInt(), buf.readInt());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(stamina);
        buf.writeInt(maxStamina);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleData(final StaminaSyncPayload data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            if (player != null) {
                var staminaData = player.getData(StaminaAttachment.STAMINA_DATA);
                staminaData.setStamina(data.stamina());
                staminaData.setMaxStamina(data.maxStamina());
            }
        });
    }
}