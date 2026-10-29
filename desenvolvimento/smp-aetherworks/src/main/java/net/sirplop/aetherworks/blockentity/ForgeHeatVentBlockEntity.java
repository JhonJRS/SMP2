package net.sirplop.aetherworks.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.sirplop.aetherworks.AWRegistry;

public class ForgeHeatVentBlockEntity extends BlockEntity implements IForgePart {
    public ForgeHeatVentBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(AWRegistry.FORGE_VENT_BLOCK_ENTITY.get(), pPos, pBlockState);
    }

    @Override
    public boolean isInvalid() {
        return (level != null ? level.getBlockEntity(getBlockPos()) : null) == null;
    }

    @Override
    public void onForgeTick(IForge forge) {
        if (forge.getHeatCapability().getHeat() > 100 && level.hasNeighborSignal(getBlockPos())) {
            if (level.isClientSide()) {
                BlockState state = level.getBlockState(getBlockPos());
                Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
                BlockPos pos = getBlockPos();
                double x, y, z, sx, sy, sz;
                switch (facing) {
                    case SOUTH -> {
                        x = pos.getX() + level.random.nextFloat();
                        y = pos.getY() + level.random.nextFloat();
                        z = pos.getZ() + 0.8f;
                        sx = 0;
                        sy = 0;
                        sz = -0.1f;
                    }
                    case NORTH -> {
                        x = pos.getX() + level.random.nextFloat();
                        y = pos.getY() + level.random.nextFloat();
                        z = pos.getZ() + 0.2f;
                        sx = 0;
                        sy = 0;
                        sz = 0.1f;
                    }
                    case EAST -> {
                        x = pos.getX() + 0.8f;
                        y = pos.getY() + level.random.nextFloat();
                        z = pos.getZ() + level.random.nextFloat();
                        sx = -0.1f;
                        sy = 0;
                        sz = 0;
                    }
                    default -> {
                        x = pos.getX() + 0.2f;
                        y = pos.getY() + level.random.nextFloat();
                        z = pos.getZ() + level.random.nextFloat();
                        sx = 0.1f;
                        sy = 0;
                        sz = 0;
                    }
                }
                level.addParticle(ParticleTypes.POOF, x, y, z, sx, sy, sz);
            } else {
                forge.getHeatCapability().removeAmount(0.5, true);
            }
        }
    }

    @Override
    public boolean isTopPart() {
        return false;
    }
}
