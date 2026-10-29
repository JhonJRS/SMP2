package net.sirplop.aetherworks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.sirplop.aetherworks.api.block.WaterloggableBlock;

public class PrismSupportBlock extends WaterloggableBlock {
    public PrismSupportBlock(Properties pProperties) {
        super(pProperties);
    }
    public static final VoxelShape SHAPE_AABB = Shapes.or(
            Block.box(0,0,0,4,16,4),
            Block.box(12, 0, 0, 16, 16, 4),
            Block.box(0, 0, 12, 4, 16, 16),
            Block.box(12, 0, 12, 16, 16, 16));

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE_AABB;
    }
}
