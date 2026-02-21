package com.reputationmod.stamina;

import com.reputationmod.ReputationMod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class StaminaAttachment {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ReputationMod.MOD_ID);

    public static final Supplier<AttachmentType<StaminaData>> STAMINA_DATA = ATTACHMENT_TYPES.register(
            "stamina_data",
            () -> AttachmentType.serializable(StaminaData::new).build()
    );
}