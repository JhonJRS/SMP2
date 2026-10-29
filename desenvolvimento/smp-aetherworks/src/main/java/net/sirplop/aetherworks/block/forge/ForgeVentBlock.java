package net.sirplop.aetherworks.block.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.sirplop.aetherworks.AWRegistry;
import net.sirplop.aetherworks.api.block.HorizontalWaterloggableEntityBlock;
import javax.annotation.Nullable;

public class ForgeVentBlock extends HorizontalWaterloggableEntityBlock {
    // 1.21: block types need a codec (never serialized by type here)
    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return simpleCodec(ForgeVentBlock::new);
    }

    public ForgeVentBlock(Properties pProperties) { super(pProperties); }

    public static final VoxelShape NORTH_AABB = Block.box(0,0,0,16,16,2);
    public static final VoxelShape EAST_AABB = Block.box(14,0,0,16,16,16);;
    public static final VoxelShape SOUTH_AABB = Block.box(0,0,14,16,16,16);
    public static final VoxelShape WEST_AABB = Block.box(0,0,0,2,16,16);

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return switch (pState.getValue(HorizontalDirectionalBlock.FACING)) {
            case EAST -> EAST_AABB;
            case WEST -> WEST_AABB;
            case SOUTH -> SOUTH_AABB;
            default -> NORTH_AABB;
        };
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return AWRegistry.FORGE_VENT_BLOCK_ENTITY.get().create(blockPos, blockState);
    }
}
