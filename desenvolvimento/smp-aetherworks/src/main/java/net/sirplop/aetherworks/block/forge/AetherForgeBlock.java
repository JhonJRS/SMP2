package net.sirplop.aetherworks.block.forge;

import com.rekindled.embers.block.DoubleTallMachineBlock;
import com.rekindled.embers.block.MechEdgeBlockBase;
import com.rekindled.embers.block.MechEdgeBlockBase.MechEdge;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.sirplop.aetherworks.AWRegistry;
import net.sirplop.aetherworks.blockentity.AetherForgeBlockEntity;
import javax.annotation.Nullable;

public class AetherForgeBlock extends DoubleTallMachineBlock implements SimpleWaterloggedBlock {

    public AetherForgeBlock(Properties properties, SoundType topSound) {
        super(properties, topSound);
    }

    protected static final VoxelShape TOP_AABB = Shapes.or(
            Block.box(1,0,1,15,1,15),
            Block.box(2,1,2,14,2,14),
            Block.box(3,2,3,13,16,13));
    protected static final VoxelShape BOTTOM_AABB = Shapes.box(0, 0, 0, 1, 1, 1);

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER)
            return BOTTOM_AABB;
        return TOP_AABB;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        //place the entity in the block above, aka where the core is
        if (pState.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER)
            return AWRegistry.AETHER_FORGE_BLOCK_ENTITY.get().create(pPos, pState);
        else
            return AWRegistry.AETHER_FORGE_TOP_BLOCK_ENTITY.get().create(pPos, pState);
    }
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        if (pState.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER)
            return pLevel.isClientSide ? createTickerHelper(pBlockEntityType, AWRegistry.AETHER_FORGE_BLOCK_ENTITY.get(), AetherForgeBlockEntity::clientTick)
                : createTickerHelper(pBlockEntityType, AWRegistry.AETHER_FORGE_BLOCK_ENTITY.get(), AetherForgeBlockEntity::serverTick);
        return null;
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            boolean lower = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER;
            if (lower) {
                BlockPos topPos = pos.above();
                if (level.getBlockState(topPos).getBlock() instanceof DoubleTallMachineBlock) {
                    level.destroyBlock(topPos, false);
                }
                for (MechEdge edge : MechEdge.values()) {
                    BlockPos cornerPos = pos.subtract(edge.centerPos);
                    if (level.getBlockState(cornerPos).getBlock() instanceof MechEdgeBlockBase) {
                        level.destroyBlock(cornerPos, false);
                    }
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER)
            return;
        for (MechEdge edge : MechEdge.values()) {
            BlockState edgeState = AWRegistry.AETHER_FORGE_EDGE.get().defaultBlockState()
                    .setValue(BlockStateProperties.WATERLOGGED, Boolean.valueOf(level.getFluidState(pos.subtract(edge.centerPos)).getType() == Fluids.WATER))
                    .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
            level.setBlock(pos.subtract(edge.centerPos), edgeState.setValue(MechEdgeBlockBase.EDGE, edge), UPDATE_ALL_IMMEDIATE);
        }
        BlockState topState = AWRegistry.AETHER_FORGE.get().defaultBlockState()
                .setValue(BlockStateProperties.WATERLOGGED, Boolean.valueOf(level.getFluidState(pos.above()).getType() == Fluids.WATER))
                .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
        level.setBlock(pos.above(), topState, UPDATE_ALL_IMMEDIATE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        BlockState state = super.getStateForPlacement(pContext);
        if (state == null)
            return null;

        boolean lower = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER;

        if (!lower && !state.canBeReplaced(pContext)) {
            return null;
        }
        else if (lower) {
            for (MechEdge edge : MechEdge.values()) {
                if (!pContext.getLevel().getBlockState(pContext.getClickedPos().subtract(edge.centerPos)).canBeReplaced(pContext)) {
                    return null;
                }
            }
        }
        return super.getStateForPlacement(pContext).setValue(BlockStateProperties.HORIZONTAL_AXIS, pContext.getHorizontalDirection().getAxis()).setValue(BlockStateProperties.WATERLOGGED, Boolean.valueOf(pContext.getLevel().getFluidState(pContext.getClickedPos()).getType() == Fluids.WATER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(BlockStateProperties.HORIZONTAL_AXIS);
    }
}
