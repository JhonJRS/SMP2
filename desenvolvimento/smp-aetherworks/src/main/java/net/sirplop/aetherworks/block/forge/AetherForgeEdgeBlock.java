package net.sirplop.aetherworks.block.forge;

import com.rekindled.embers.block.MechEdgeBlockBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.sirplop.aetherworks.AWRegistry;

import javax.annotation.Nullable;

public class AetherForgeEdgeBlock extends MechEdgeBlockBase implements EntityBlock {
    public AetherForgeEdgeBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(BlockStateProperties.WATERLOGGED, false)
                .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER)
                .setValue(BlockStateProperties.NORTH, false)
                .setValue(BlockStateProperties.SOUTH, false)
                .setValue(BlockStateProperties.EAST, false)
                .setValue(BlockStateProperties.WEST, false));
    }

    public static final VoxelShape NORTH_AABB =     Shapes.or(Block.box(0,0,1,16,4,16), Block.box(0,4,2,16,6,16), Block.box(0,6,4,16,10,16), Block.box(0,10,2,16,12,16), Block.box(0,12,0,16,16,16));
    public static final VoxelShape NORTHEAST_AABB = Shapes.or(Block.box(0,0,1,15,4,16), Block.box(0,4,2,14,6,16), Block.box(0,6,4,12,10,16), Block.box(0,10,2,14,12,16), Block.box(0,12,0,16,16,16));
    public static final VoxelShape EAST_AABB =      Shapes.or(Block.box(0,0,0,15,4,16), Block.box(0,4,0,14,6,16), Block.box(0,6,0,12,10,16), Block.box(0,10,0,14,12,16), Block.box(0,12,0,16,16,16));
    public static final VoxelShape SOUTHEAST_AABB = Shapes.or(Block.box(0,0,0,15,4,15), Block.box(0,4,0,14,6,14), Block.box(0,6,0,12,10,12), Block.box(0,10,0,14,12,14), Block.box(0,12,0,16,16,16));
    public static final VoxelShape SOUTH_AABB =     Shapes.or(Block.box(0,0,0,16,4,15), Block.box(0,4,0,16,6,14), Block.box(0,6,0,16,10,12), Block.box(0,10,0,16,12,14), Block.box(0,12,0,16,16,16));
    public static final VoxelShape SOUTHWEST_AABB = Shapes.or(Block.box(1,0,0,16,4,15), Block.box(2,4,0,16,6,14), Block.box(4,6,0,16,10,12), Block.box(2,10,0,16,12,14), Block.box(0,12,0,16,16,16));
    public static final VoxelShape WEST_AABB =      Shapes.or(Block.box(1,0,0,16,4,16), Block.box(2,4,0,16,6,16), Block.box(4,6,0,16,10,16), Block.box(2,10,0,16,12,16), Block.box(0,12,0,16,16,16));
    public static final VoxelShape NORTHWEST_AABB = Shapes.or(Block.box(1,0,1,16,4,16), Block.box(2,4,2,16,6,16), Block.box(4,6,4,16,10,16), Block.box(2,10,2,16,12,16), Block.box(0,12,0,16,16,16));
    public static final VoxelShape[] SHAPES_BOTTOM = new VoxelShape[] { NORTH_AABB, NORTHEAST_AABB, EAST_AABB, SOUTHEAST_AABB, SOUTH_AABB, SOUTHWEST_AABB, WEST_AABB, NORTHWEST_AABB };

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES_BOTTOM[state.getValue(MechEdgeBlockBase.EDGE).index];
    }

    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    @Override
    public Block getCenterBlock() {
        return AWRegistry.AETHER_FORGE.get();
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockPos centerPos;
            if (state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER)
                centerPos = pos.below();
            else {
                centerPos = pos.offset(state.getValue(MechEdgeBlockBase.EDGE).centerPos);
            }
            if (level.getBlockState(centerPos).getBlock() == this.getCenterBlock()) {
                level.destroyBlock(centerPos, false);
            }

            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return super.getStateForPlacement(pContext)
                .setValue(BlockStateProperties.WATERLOGGED, pContext.getLevel().getFluidState(pContext.getClickedPos()).getType() == Fluids.WATER)
                .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER)
                .setValue(BlockStateProperties.NORTH, false)
                .setValue(BlockStateProperties.SOUTH, false)
                .setValue(BlockStateProperties.EAST, false)
                .setValue(BlockStateProperties.WEST, false);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(BlockStateProperties.DOUBLE_BLOCK_HALF);
        pBuilder.add(BlockStateProperties.NORTH);
        pBuilder.add(BlockStateProperties.SOUTH);
        pBuilder.add(BlockStateProperties.EAST);
        pBuilder.add(BlockStateProperties.WEST);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return AWRegistry.TOP_HAMMERABLE_BLOCK_ENTITY.get().create(blockPos, blockState);
    }
}
