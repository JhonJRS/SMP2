package net.sirplop.aetherworks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.sirplop.aetherworks.api.block.WaterloggableBlock;

public class ControlMatrixBlock extends WaterloggableBlock {
    public ControlMatrixBlock(Properties pProperties) {
        super(pProperties);
    }
    public static final VoxelShape SHAPE_AABB = Shapes.or(
            Block.box(0,0,0,16,2,16),
            Block.box(2, 2, 2, 14, 18, 14),
            Block.box(5, 18, 5, 11, 25, 11));

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE_AABB;
    }
}
