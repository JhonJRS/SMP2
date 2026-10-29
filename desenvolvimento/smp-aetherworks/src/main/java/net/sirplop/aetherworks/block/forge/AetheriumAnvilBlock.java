package net.sirplop.aetherworks.block.forge;
import com.rekindled.embers.util.CapabilityCompat;

import com.rekindled.embers.RegistryManager;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.rekindled.embers.compat.legacy.capabilities.ForgeCapabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.sirplop.aetherworks.AWRegistry;
import net.sirplop.aetherworks.api.block.HorizontalWaterloggableEntityBlock;
import net.sirplop.aetherworks.blockentity.AetheriumAnvilBlockEntity;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class AetheriumAnvilBlock extends HorizontalWaterloggableEntityBlock {
    // 1.21: block types need a codec (never serialized by type here)
    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return simpleCodec(AetheriumAnvilBlock::new);
    }

    public AetheriumAnvilBlock(Properties pProperties) {
        super(pProperties);
    }
    public static final VoxelShape EAST_WEST_AABB = Block.box(4, 0, 2, 12, 3, 14);
    public static final VoxelShape NORTH_SOUTH_AABB = Block.box(3, 0, 4, 14, 3, 12);

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return switch (pState.getValue(HorizontalDirectionalBlock.FACING)) {
            case EAST, WEST -> EAST_WEST_AABB;
            default -> NORTH_SOUTH_AABB;
        };
    }
    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return AWRegistry.AETHERIUM_ANVIL_BLOCK_ENTITY.get().create(blockPos, blockState);
    }
    @Nullable
    @Override
    public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        Direction dir = context.getHorizontalDirection().getOpposite();
        return state == null ? null : state.setValue(FACING, dir).setValue(BlockStateProperties.WATERLOGGED,
                context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
    }

    
    public InteractionResult useLegacy(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof AetheriumAnvilBlockEntity anvil) {
            ItemStack heldItem = player.getItemInHand(hand);
            if (!heldItem.isEmpty()) {
                if (heldItem.getItem() == RegistryManager.TINKER_HAMMER.get()) {
                    anvil.onHit();
                    return InteractionResult.SUCCESS;
                }
                ItemStack leftover = anvil.inventory.insertItem(0, heldItem, false);
                if (!leftover.equals(heldItem)) {
                    player.setItemInHand(hand, leftover);
                    return InteractionResult.SUCCESS;
                }
            }
            if (!anvil.inventory.getStackInSlot(0).isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, player.position().x, player.position().y, player.position().z, anvil.inventory.getStackInSlot(0)));
                anvil.inventory.setStackInSlot(0, ItemStack.EMPTY);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null) {
                IItemHandler handler = CapabilityCompat.getCapability(blockEntity, ForgeCapabilities.ITEM_HANDLER, null).orElse(null);
                if (handler != null) {
                    Misc.spawnInventoryInWorld(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, handler);
                    level.updateNeighbourForOutputSignal(pos, this);
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
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
