package com.reputationmod.stamina;

import com.reputationmod.ReputationMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record StaminaLevelSyncPayload(int level, int experience, int maxStamina) implements CustomPacketPayload {
    public static final Type<StaminaLevelSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ReputationMod.MOD_ID, "stamina_level_sync")
    );

    public static final StreamCodec<FriendlyByteBuf, StaminaLevelSyncPayload> STREAM_CODEC =
            StreamCodec.ofMember(StaminaLevelSyncPayload::write, StaminaLevelSyncPayload::new);

    public StaminaLevelSyncPayload(FriendlyByteBuf buf) {
        this(buf.readInt(), buf.readInt(), buf.readInt());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(level);
        buf.writeInt(experience);
        buf.writeInt(maxStamina);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleData(final StaminaLevelSyncPayload data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientStaminaLevelData.set(data.level(), data.experience(), data.maxStamina());
        });
    }
}