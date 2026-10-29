package net.sirplop.aetherworks.block;

import com.rekindled.embers.datagen.EmbersSounds;
import com.rekindled.embers.util.Misc;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.sirplop.aetherworks.AWRegistry;
import net.sirplop.aetherworks.blockentity.LexiconReceptacleBlockEntity;
import net.sirplop.aetherworks.item.Lexicon;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class LexiconReceptacleBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    // 1.21: block types need a codec (never serialized by type here)
    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return simpleCodec(LexiconReceptacleBlock::new);
    }

    public LexiconReceptacleBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.stateDefinition.any().setValue(BlockStateProperties.WATERLOGGED, false));
    }

    protected static final VoxelShape AABB = Shapes.or(
            Block.box(0,0,0,16,14,16),
            Block.box(0, 14, 0, 4, 16, 4),
            Block.box(12, 14, 0, 16, 16, 4),
            Block.box(0, 14, 12, 4, 16, 16),
            Block.box(12, 14, 12, 16, 16, 16));

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return AWRegistry.LEXICON_RECEPTACLE_BLOCK_ENTITY.get().create(pPos, pState);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        return pLevel.isClientSide ? createTickerHelper(pBlockEntityType, AWRegistry.LEXICON_RECEPTACLE_BLOCK_ENTITY.get(), LexiconReceptacleBlockEntity::clientTick)
                : null;
    }

    
    public InteractionResult useLegacy(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof LexiconReceptacleBlockEntity receptacle) {
            ItemStack heldItem = player.getItemInHand(hand);
            if (hit.getDirection() == Direction.UP) {
                // no item in main hand, lexicon in slot, give player lexicon
                if (player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
                    if (!receptacle.lexiconInventory.getStackInSlot(0).isEmpty()) {
                        level.addFreshEntity(new ItemEntity(level, player.position().x, player.position().y, player.position().z,
                                receptacle.lexiconInventory.getStackInSlot(0)));
                        receptacle.lexiconInventory.setStackInSlot(0, ItemStack.EMPTY);
                        if (level.isClientSide)
                            player.playSound(EmbersSounds.BAUBLE_UNEQUIP.get(), 1.0f, (level.random.nextFloat() * 0.2f) + 0.8f);
                        return InteractionResult.SUCCESS;
                    }
                }
                else {
                    //item in hand, is lexicon, and lexicon slot is empty, insert lexicon
                    if (heldItem.getItem() == AWRegistry.LEXICON.get()
                            && receptacle.lexiconInventory.getStackInSlot(0).isEmpty()
                            && Lexicon.getStoredItem(heldItem) != ItemStack.EMPTY) {
                        ItemStack leftover = receptacle.lexiconInventory.insertItem(0, heldItem, false);
                        if (!leftover.equals(heldItem)) {
                            player.setItemInHand(hand, leftover);
                            if (level.isClientSide)
                                player.playSound(EmbersSounds.BAUBLE_UNEQUIP.get(), 1.0f, (level.random.nextFloat() * 0.2f) + 0.8f);
                            return InteractionResult.SUCCESS;
                        }
                    }
                }
            }
/*
            //not a lexicon and the slot isn't empty, insert into lexicon.
            if (!receptacle.inventory.getStackInSlot(0).isEmpty()) {
                ItemStack leftover = receptacle.inventory.insertItem(0, heldItem, false);
                if (!leftover.equals(heldItem)) {
                    player.setItemInHand(hand, leftover);
                    return InteractionResult.SUCCESS;
                }
            }

 */
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof LexiconReceptacleBlockEntity receptacle) {
                Misc.spawnInventoryInWorld(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, receptacle.lexiconInventory);
                level.updateNeighbourForOutputSignal(pos, this);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public @Nonnull RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nonnull VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return AABB;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        BlockState state = super.getStateForPlacement(pContext);
        return state == null ? null : state.setValue(BlockStateProperties.WATERLOGGED,
                pContext.getLevel().getFluidState(pContext.getClickedPos()).getType() == Fluids.WATER);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(BlockStateProperties.WATERLOGGED);
    }

    @Override
    public FluidState getFluidState(BlockState pState) {
        return pState.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(pState);
    }

    @Override
    public BlockState updateShape(BlockState pState, Direction pDirection, BlockState pNeighborState, LevelAccessor pLevel, BlockPos pPos, BlockPos pNeighborPos) {
        if (pState.getValue(BlockStateProperties.WATERLOGGED)) {
            pLevel.scheduleTick(pPos, Fluids.WATER, Fluids.WATER.getTickDelay(pLevel));
        }
        return super.updateShape(pState, pDirection, pNeighborState, pLevel, pPos, pNeighborPos);
    }

    // SMP 2 port: 1.21 split Block#use into useItemOn / useWithoutItem; both call the original logic.
    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        InteractionResult r = useLegacy(state, level, pos, player, hand, hit);
        if (r == InteractionResult.PASS) return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (r == InteractionResult.FAIL) return net.minecraft.world.ItemInteractionResult.FAIL;
        return r == InteractionResult.SUCCESS ? net.minecraft.world.ItemInteractionResult.SUCCESS : net.minecraft.world.ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return useLegacy(state, level, pos, player, InteractionHand.MAIN_HAND, hit);
    }
}
