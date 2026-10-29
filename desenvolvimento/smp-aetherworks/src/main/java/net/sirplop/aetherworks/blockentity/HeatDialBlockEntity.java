package net.sirplop.aetherworks.blockentity;
import com.rekindled.embers.util.CapabilityCompat;
import net.minecraft.core.HolderLookup;

import com.rekindled.embers.api.tile.IDialEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.sirplop.aetherworks.AWRegistry;
import net.sirplop.aetherworks.capabilities.AWCapabilities;
import net.sirplop.aetherworks.api.capabilities.IHeatCapability;

public class HeatDialBlockEntity extends BlockEntity  implements IDialEntity {

    public double heat = 0;
    public double capacity = 0;
    public boolean display = false;

    public HeatDialBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(AWRegistry.HEAT_DIAL_ENTITY.get(), pPos, pBlockState);
    }

    @Override
    public void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        if (nbt.contains(IHeatCapability.HEAT))
            heat = nbt.getDouble(IHeatCapability.HEAT);
        if (nbt.contains(IHeatCapability.HEAT_CAPACITY))
            capacity = nbt.getDouble(IHeatCapability.HEAT_CAPACITY);
        if (nbt.contains("display"))
            display = nbt.getBoolean("display");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag nbt = super.getUpdateTag(registries);
        BlockState state = level.getBlockState(worldPosition);
        boolean display = false;
        if (state.hasProperty(BlockStateProperties.FACING)) {
            Direction facing = state.getValue(BlockStateProperties.FACING);
            BlockEntity blockEntity = level.getBlockEntity(worldPosition.relative(facing, -1));
            if (blockEntity != null) {
                IHeatCapability cap = CapabilityCompat.getCapability(blockEntity, AWCapabilities.HEAT_CAPABILITY, facing.getOpposite()).orElse(CapabilityCompat.getCapability(blockEntity, AWCapabilities.HEAT_CAPABILITY, null).orElse(null));
                if (cap != null) {
                    nbt.putDouble(IHeatCapability.HEAT, cap.getHeat());
                    nbt.putDouble(IHeatCapability.HEAT_CAPACITY, cap.getHeatCapacity());
                    display = true;
                }
            }
        }
        nbt.putBoolean("display", display);
        return nbt;
    }

    //@Override
    public Packet<ClientGamePacketListener> getUpdatePacket(int maxLines) {
        return ClientboundBlockEntityDataPacket.create(this, (be, registries) -> this.getUpdateTag(registries));
    }
}