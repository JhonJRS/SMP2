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
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.rekindled.embers.compat.legacy.capabilities.ForgeCapabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.sirplop.aetherworks.AWRegistry;
import net.sirplop.aetherworks.Aetherworks;
import net.sirplop.aetherworks.api.block.HorizontalWaterloggableEntityBlock;
import net.sirplop.aetherworks.blockentity.ToolStationBlockEntity;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ForgeToolStation extends HorizontalWaterloggableEntityBlock {
    // 1.21: block types need a codec (never serialized by type here)
    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return simpleCodec(ForgeToolStation::new);
    }

    public ForgeToolStation(Properties pProperties) {
        super(pProperties);
    }
    public static final VoxelShape AABB = Shapes.or(
            Block.box(0,0,0,16,1,16),
            Block.box(1,1,1,15,2,15));


    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return AWRegistry.TOOL_STATION_BLOCK_ENTITY.get().create(blockPos, blockState);
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
    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return AABB;
    }

    
    public InteractionResult useLegacy(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof ToolStationBlockEntity toolStation) {
            if (!toolStation.inventory.getStackInSlot(5).isEmpty()) {
                //grab the finished product.
                level.addFreshEntity(new ItemEntity(level, player.position().x, player.position().y, player.position().z, toolStation.inventory.getStackInSlot(5)));
                toolStation.inventory.setStackInSlot(5, ItemStack.EMPTY);
                return InteractionResult.SUCCESS;
            }

            ItemStack heldItem = player.getItemInHand(hand);
            if (!heldItem.isEmpty()) {
                //check if they're whacking it.
                if (heldItem.getItem() == RegistryManager.TINKER_HAMMER.get()) {
                    if (toolStation.onHit())
                        return InteractionResult.SUCCESS;
                    return InteractionResult.CONSUME;
                }
                //not whacking, so try and insert their hand item.
                int slot = getSlotLookingAt(hit, level);
                if (slot == -1)
                    return InteractionResult.PASS;
                ItemStack leftover = toolStation.inventory.insertItem(slot, heldItem, false);
                if (!leftover.equals(heldItem)) {
                    player.setItemInHand(hand, leftover);
                    return InteractionResult.SUCCESS;
                }
            }
            int slot = getSlotLookingAt(hit, level);
            if (slot == -1)
                return InteractionResult.PASS;
            if (!toolStation.inventory.getStackInSlot(slot).isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, player.position().x, player.position().y, player.position().z, toolStation.inventory.getStackInSlot(slot)));
                toolStation.inventory.setStackInSlot(slot, ItemStack.EMPTY);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    private int getSlotLookingAt(BlockHitResult hit, Level level) {
        Vec3 posRaw = hit.getLocation();
        BlockPos pos = hit.getBlockPos();
        //get our hit position in local pixels.
        double x = (posRaw.x - pos.getX()) * 16;
        double y = (posRaw.y - pos.getY()) * 16;
        double z = (posRaw.z - pos.getZ()) * 16;

        if (x < 1 || x > 15 || z < 1 || z > 15 || y < 1)
            return -1; //not looking at a slot.


        //this is done after checking the center because the center is symmetrical in all 4 directions.
        //rotate our pixel coordinates depending on direction so we get the right slot. (a 90 degree turn is [-y, x])
        switch (level.getBlockState(hit.getBlockPos()).getValue(FACING)) {
            case EAST -> {
                double tX = x;
                x = 16 - z;
                z = tX;
            }
            case NORTH -> {
                x = 16 - x;
                z = 16 - z;
            }
            case WEST -> {
                double tX = x;
                x = z;
                z = 16 - tX;
            }
            //south is the default direction
        }

        //check center
        if ((x >= 6 && x <= 10 && z >= 6 && z <= 10) ||
                (x >= 6 && x <= 10 && z >= 5 && z <= 6) ||
                (x >= 5 && x <= 6 && z >= 6 && z <= 10) ||
                (x >= 10 && x <= 11 && z >= 6 && z <= 10) ||
                (x >= 6 && x <= 10 && z >= 10 && z <= 11) )
            return 2;

        if (x >= 8 && z >= 8)
            return 4; //bottom right
        if (z >= 8)
            return 3; //bottom left
        if (x >= 8)
            return 1; //top right
        return 0; //top left
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
