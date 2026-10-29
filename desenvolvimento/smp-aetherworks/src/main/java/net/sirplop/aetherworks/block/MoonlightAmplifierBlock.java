package net.sirplop.aetherworks.block;

import com.rekindled.embers.particle.GlowParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.sirplop.aetherworks.AWRegistry;
import net.sirplop.aetherworks.util.Utils;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3f;

public class MoonlightAmplifierBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock {
    // 1.21: block types need a codec (never serialized by type here)
    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.HorizontalDirectionalBlock> codec() {
        return simpleCodec(MoonlightAmplifierBlock::new);
    }

    public MoonlightAmplifierBlock(Properties pProperties) {
        super(pProperties);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(BlockStateProperties.WATERLOGGED, false));;
    }

    public static final VoxelShape SHAPE_AABB = Shapes.or(
            Block.box(0,0,0,16,2,16),
            Block.box(2, 2, 2, 14, 2, 14),
            Block.box(3, 3, 3, 13, 11, 13));

    public static final GlowParticleOptions GLOW_SKY = new GlowParticleOptions(Utils.AETHERIUM_COLOR, 1f, 80);
    public static final GlowParticleOptions GLOW_BEAM = new GlowParticleOptions(Utils.AETHERIUM_COLOR, new Vec3(0, -0.1f, 0),2.5f, 30);

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
        builder.add(BlockStateProperties.WATERLOGGED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        Direction dir = context.getHorizontalDirection().getOpposite();
        return state == null ? null : state.setValue(FACING, dir).setValue(BlockStateProperties.WATERLOGGED,
                context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!level.canSeeSky(pos.above()) || level.getDayTime() < 15000 || level.getDayTime() > 21000)
            return;
        Direction dir = state.getValue(FACING);
        if (!level.getBlockState(pos.relative(dir, 3)).is(AWRegistry.PRISM.get()))
            return;
        float rX = pos.getX() + 0.5F;
        float rZ = pos.getZ() + 0.5F;

        for (float i = 0; i < 8; ++i)
        {
            float sX = (random.nextFloat() - random.nextFloat()) * 4;
            float sZ = (random.nextFloat() - random.nextFloat()) * 4;
            float sY = random.nextFloat() * 3;
            level.addParticle(GLOW_SKY,
                    rX + sX,
                    pos.getY() + 1 + random.nextFloat() * 3,
                    rZ + sZ,
                    -sX / 5,
                    -sY / 5,
                    -sZ / 5);

        }
        for (float i = 0; i < 25; ++i)
        {
            level.addParticle(GLOW_BEAM,
                    rX + dir.getStepX() * (i / 8),
                    pos.getY() + 0.5F,
                    rZ + dir.getStepZ() * (i / 8),
                    0, -0.1f, 0);
        }

        super.animateTick(state, level, pos, random);
    }

    @Override
    public FluidState getFluidState(BlockState pState) {
        return pState.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(pState);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE_AABB;
    }
}
