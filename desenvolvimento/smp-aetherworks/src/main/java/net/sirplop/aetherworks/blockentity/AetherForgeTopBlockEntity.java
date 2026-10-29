package net.sirplop.aetherworks.blockentity;
import com.rekindled.embers.util.CapabilityCompat;

import java.util.List;

import com.rekindled.embers.RegistryManager;
import com.rekindled.embers.api.tile.IExtraCapabilityInformation;
import com.rekindled.embers.api.tile.IExtraDialInformation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import com.rekindled.embers.compat.legacy.capabilities.Capability;
import com.rekindled.embers.compat.legacy.LazyOptional;
import net.sirplop.aetherworks.AWRegistry;

public class AetherForgeTopBlockEntity extends BlockEntity implements IExtraDialInformation, IExtraCapabilityInformation, com.rekindled.embers.compat.legacy.capabilities.ICapabilityProvider{

    public AetherForgeTopBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(AWRegistry.AETHER_FORGE_TOP_BLOCK_ENTITY.get(), pPos, pBlockState);
    }

    public BlockEntityDirection getAttachedMultiblock() {
        return new BlockEntityDirection(level.getBlockEntity(worldPosition.below()), Direction.DOWN);
    }

    public BlockEntity getAttachedBlockEntity() {
        return level.getBlockEntity(worldPosition.below());
    }

    public Direction getAttachedSide() {
        return Direction.DOWN;
    }

    
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        BlockEntityDirection multiblock = getAttachedMultiblock();
        if (multiblock != null && multiblock.blockEntity != null)
            return CapabilityCompat.getCapability(multiblock.blockEntity, cap, multiblock.direction);
        return LazyOptional.empty();
    }

    @Override
    public void addDialInformation(Direction facing, List<Component> information, String dialType) {
        BlockEntityDirection multiblock = getAttachedMultiblock();
        if(multiblock != null && multiblock.blockEntity instanceof IExtraDialInformation)
            ((IExtraDialInformation) multiblock.blockEntity).addDialInformation(multiblock.direction, information, dialType);
    }

    @Override
    public boolean hasCapabilityDescription(Capability<?> capability) {
        BlockEntity multiblock = getAttachedBlockEntity();
        if (multiblock instanceof IExtraCapabilityInformation)
            return ((IExtraCapabilityInformation) multiblock).hasCapabilityDescription(capability);
        return false;
    }

    @Override
    public void addCapabilityDescription(List<Component> strings, Capability<?> capability, Direction facing) {
        BlockEntityDirection multiblock = getAttachedMultiblock();
        if (multiblock != null && multiblock.blockEntity instanceof IExtraCapabilityInformation)
            ((IExtraCapabilityInformation) multiblock.blockEntity).addCapabilityDescription(strings, capability, multiblock.direction);
    }

    @Override
    public void addOtherDescription(List<Component> strings, Direction facing) {
        BlockEntityDirection multiblock = getAttachedMultiblock();
        if (multiblock != null && multiblock.blockEntity instanceof IExtraCapabilityInformation)
            ((IExtraCapabilityInformation) multiblock.blockEntity).addOtherDescription(strings, multiblock.direction);
    }

    public static class BlockEntityDirection {

        public BlockEntity blockEntity;
        public Direction direction;

        public BlockEntityDirection(BlockEntity blockEntity, Direction direction) {
            this.blockEntity = blockEntity;
            this.direction = direction;
        }
    }
}