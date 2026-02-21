package com.reputationmod.block.entity;

import com.reputationmod.ReputationMod;
import com.reputationmod.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ProtectionChestBlockEntity extends BlockEntity {
    private List<BlockPos> protectedBlocks = new ArrayList<>();
    private boolean active = true;
    private int minutesLeft = 0;

    // Радиус защиты (изменён на 10)
    private static final int PROTECTION_RADIUS = 10;

    public ProtectionChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PROTECTION_CHEST.get(), pos, state);
    }

    public List<BlockPos> getProtectedBlocks() {
        return protectedBlocks;
    }

    public void addBlock(BlockPos pos) {
        if (!protectedBlocks.contains(pos)) {
            protectedBlocks.add(pos);
            System.out.println("[Protection] Block added to chest at " + worldPosition + " from " + pos);
            setChanged();
            syncToClient();
        }
    }

    public void removeBlock(BlockPos pos) {
        protectedBlocks.remove(pos);
        setChanged();
        syncToClient();
    }

    public void clearBlocks() {
        protectedBlocks.clear();
        setChanged();
        syncToClient();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        setChanged();
        syncToClient();
    }

    public int getMinutesLeft() {
        return minutesLeft;
    }

    public void setMinutesLeft(int minutes) {
        this.minutesLeft = minutes;
        setChanged();
        syncToClient();
    }

    // Сканирование области при установке сундука
    public void scanArea() {
        if (level == null) return;
        BlockPos center = this.worldPosition;
        BlockPos.betweenClosedStream(center.offset(-PROTECTION_RADIUS, -PROTECTION_RADIUS, -PROTECTION_RADIUS),
                        center.offset(PROTECTION_RADIUS, PROTECTION_RADIUS, PROTECTION_RADIUS))
                .forEach(pos -> {
                    BlockState state = level.getBlockState(pos);
                    if (state.is(TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(ReputationMod.MOD_ID, "protection_blocks")))) {
                        addBlock(pos.immutable());
                    }
                });
    }

    // Статический метод для добавления блока к ближайшему сундуку (вызывается из событий)
    public static void tryAddBlockToNearbyChest(ServerLevel level, BlockPos blockPos) {
        BlockPos.betweenClosedStream(blockPos.offset(-PROTECTION_RADIUS, -PROTECTION_RADIUS, -PROTECTION_RADIUS),
                        blockPos.offset(PROTECTION_RADIUS, PROTECTION_RADIUS, PROTECTION_RADIUS))
                .forEach(chestPos -> {
                    BlockState chestState = level.getBlockState(chestPos);
                    if (chestState.is(ModBlocks.PROTECTION_CHEST.get())) {
                        BlockEntity be = level.getBlockEntity(chestPos);
                        if (be instanceof ProtectionChestBlockEntity chest) {
                            chest.addBlock(blockPos.immutable());
                        }
                    }
                });
    }

    // Метод тика, вызываемый из блока
    public static void tick(Level level, BlockPos pos, BlockState state, ProtectionChestBlockEntity blockEntity) {
        if (level.isClientSide) return;
        // каждую минуту (1200 тиков) проверяем ресурсы
        if (level.getGameTime() % 1200 == 0) {
            blockEntity.checkResources();
        }
    }

    private void checkResources() {
        // TODO: реализация проверки ресурсов и запуска таймера разрушения
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (BlockPos pos : protectedBlocks) {
            CompoundTag posTag = new CompoundTag();
            posTag.putInt("x", pos.getX());
            posTag.putInt("y", pos.getY());
            posTag.putInt("z", pos.getZ());
            list.add(posTag);
        }
        tag.put("ProtectedBlocks", list);
        tag.putBoolean("Active", active);
        tag.putInt("MinutesLeft", minutesLeft);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        protectedBlocks.clear();
        ListTag list = tag.getList("ProtectedBlocks", Tag.TAG_COMPOUND);
        for (Tag t : list) {
            CompoundTag posTag = (CompoundTag) t;
            int x = posTag.getInt("x");
            int y = posTag.getInt("y");
            int z = posTag.getInt("z");
            protectedBlocks.add(new BlockPos(x, y, z));
        }
        active = tag.getBoolean("Active");
        minutesLeft = tag.getInt("MinutesLeft");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}