package net.sirplop.aetherworks.block.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.sirplop.aetherworks.AWRegistry;
import net.sirplop.aetherworks.api.block.HorizontalWaterloggableEntityBlock;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ForgeHeaterBlock extends HorizontalWaterloggableEntityBlock {
    // 1.21: block types need a codec (never serialized by type here)
    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return simpleCodec(ForgeHeaterBlock::new);
    }


    public ForgeHeaterBlock(Properties pProperties) { super(pProperties); }

    protected static final VoxelShape NORTH_AABB = Shapes.or(
            Block.box(1,0,1,15,3,15), //base
            Block.box(3,3,0,13,13,1), //big panel
            Block.box(5,5,1,11,11,2), //big panel back
            Block.box(6,6,0,10,10,16), //side post
            Block.box(0,6,6,16,10,10), //side post
            Block.box(3,3,3,13,16,13)); //center
    protected static final VoxelShape SOUTH_AABB = Shapes.or(
            Block.box(1,0,1,15,3,15), //base
            Block.box(3,3,15,13,13,16), //big panel
            Block.box(5,5,14,11,11,15), //big panel back
            Block.box(6,6,0,10,10,16), //side post
            Block.box(0,6,6,16,10,10), //side post
            Block.box(3,3,3,13,16,13)); //center
    protected static final VoxelShape WEST_AABB = Shapes.or(
            Block.box(1,0,1,15,3,15), //base
            Block.box(0,3,3,1,13,13), //big panel
            Block.box(1,5,5,2,11,11), //big panel back
            Block.box(6,6,0,10,10,16), //side post
            Block.box(0,6,6,16,10,10), //side post
            Block.box(3,3,3,13,16,13)); //center
    protected static final VoxelShape EAST_AABB = Shapes.or(
            Block.box(1,0,1,15,3,15), //base
            Block.box(15,3,3,16,13,13), //big panel
            Block.box(14,5,5,16,11,11), //big panel back
            Block.box(6,6,0,10,10,16), //side post
            Block.box(0,6,6,16,10,10), //side post
            Block.box(3,3,3,13,16,13)); //center

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return AWRegistry.FORGE_HEATER_BLOCK_ENTITY.get().create(blockPos, blockState);
    }
    @Nullable
    @Override
    public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        Direction dir = context.getHorizontalDirection();
        return state == null ? null : state.setValue(FACING, dir).setValue(BlockStateProperties.WATERLOGGED,
                context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(HorizontalDirectionalBlock.FACING)) {
            case EAST -> EAST_AABB;
            case WEST -> WEST_AABB;
            case SOUTH -> SOUTH_AABB;
            default -> NORTH_AABB;
        };
    }
}
