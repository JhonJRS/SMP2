package net.sirplop.aetherworks.item.tool;

import com.rekindled.embers.particle.GlowParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.sirplop.aetherworks.AWConfig;
import net.sirplop.aetherworks.datagen.AWBlockTags;
import net.sirplop.aetherworks.lib.AWHarvestHelper;
import net.sirplop.aetherworks.lib.AWHarvestNode;
import net.sirplop.aetherworks.lib.OctDirection;
import net.sirplop.aetherworks.util.AetheriumTiers;
import net.sirplop.aetherworks.util.Utils;
import javax.annotation.Nonnull;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class SculkAxe extends AOEEmberDiggerItem{

    private static final Vector3f color = new Vector3f(57f / 255f, 214 / 255f, 224 / 255f);

    public SculkAxe(Properties properties) {
        super(5f, -3f, AetheriumTiers.AETHERIUM, AWBlockTags.SCULK_AXE_MINEABLE, properties);
    }

    private final GlowParticleOptions particle = new GlowParticleOptions(getParticleColor(), 1, 15);
    public static final GlowParticleOptions USE_PARTICLE = new GlowParticleOptions(color, 1f, 25);

    @Override
    public Vector3f getParticleColor() {
        return color;
    }

    @Override
    public @Nonnull InteractionResult useOn(@Nonnull UseOnContext context) {
        InteractionResult result = super.useOn(context);
        if (!(context.getLevel() instanceof ServerLevel) || context.getLevel().isClientSide)
            return result;

        if (context.getPlayer() == null
                || !Utils.blockHasTag(context.getLevel().getBlockState(context.getClickedPos()), AWBlockTags.AOSA_ALLOWED)
        )
            return result;
        if (result == InteractionResult.PASS && context.getHand() == InteractionHand.MAIN_HAND
                && context.getLevel().getBlockState(context.getClickedPos()).canHarvestBlock(context.getLevel(), context.getClickedPos(), context.getPlayer()))
        {
            if (AWHarvestHelper.addNode(context.getPlayer(),
                    new AWHarvestNode(context.getPlayer(), context.getLevel(), context.getClickedPos(),
                            AWConfig.SKULK_AXE_MINE_RANGE.get(), p -> p.getMainHandItem().getItem() == this, particle, 0.25)))
            {
                context.getPlayer().swing(context.getHand(), true);
                return InteractionResult.SUCCESS;
            }
        }
        return result;
    }

    @Override
    public @Nonnull UseAnim getUseAnimation(@Nonnull ItemStack pStack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack pStack, LivingEntity entity) {
        return 72000;
    }
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !ItemStack.matches(oldStack, newStack);
    }

    private int ticksUsed = 0;
    @Override
    public void onUseTick(@Nonnull Level level, @Nonnull LivingEntity entity, @Nonnull ItemStack pStack, int pRemainingUseDuration) {
        if (++ticksUsed < 40 || level.isClientSide() || !(entity instanceof Player player))
            return;
        if (!Utils.hasEnoughDurability(player.getUseItem(), 2)) {
            player.stopUsingItem();
            return;
        }

        //spawn particles
        Vec3 playerPos = player.position();
        ((ServerLevel)level).sendParticles(USE_PARTICLE,
                playerPos.x,
                playerPos.y + 0.5f,
                playerPos.z,
                25, 1, 0f, 1, 0.15f);
        if (ticksUsed % 10 != 0)
            return;
        //get sapling in inventory
        Inventory inventory = player.getInventory();
        int slot = -1;
        SaplingBlock saplingInfo = null;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).getItem() instanceof BlockItem bi && bi.getBlock() instanceof SaplingBlock sap) {
                slot = i;
                saplingInfo = sap;
                break;
            }
        }
        if (slot == -1) {
            entity.stopUsingItem();
            return; //no more saplings.
        }
        //get uniformly random location in a circle
        float minRadiusSqr = 9; //3^2
        float maxRadiusSqr = AWConfig.SKULK_AXE_GROW_RANGE.get() * AWConfig.SKULK_AXE_GROW_RANGE.get();
        double theta = Math.toRadians(level.random.nextFloat() * 360);
        double r = Math.sqrt(level.random.nextFloat() * (maxRadiusSqr - minRadiusSqr) + minRadiusSqr);
        //convert polar coordinates to cartesian coordinates
        double nx = r * Math.cos(theta);
        double ny = r * Math.sin(theta);
        BlockPos pos = new BlockPos(
                player.getBlockX() + (int)Math.floor(nx),
                player.getBlockY(),
                player.getBlockZ() + (int)Math.floor(ny)
        );
        BlockPos placePos = null;
        OctDirection[] directions = Utils.fisherYatesShuffle(OctDirection.values(), level.random);
        for (OctDirection dir : directions) {
            BlockPos start = dir.offsetBlock(pos).above(5);
            for (int i = 0; i < 10; i++) {
                BlockPos check = start.below(i);
                if (level.getBlockState(check).isAir() && saplingInfo.defaultBlockState().canSurvive(level, check)) {
                    placePos = check;
                    break;
                }
            }
        }
        if (placePos == null)
            return; //no valid location found. aw dangit!
        //place sapling and remove from inventory
        level.setBlock(placePos, saplingInfo.defaultBlockState(), 1 | 2);

        int usedSaplings = 0;

        //force-grow sapling
        if (level.getBlockState(placePos).getBlock() instanceof SaplingBlock sapling) {
            usedSaplings = 1;
            sapling.advanceTree((ServerLevel)level, placePos, level.getBlockState(placePos), level.random);
            sapling.advanceTree((ServerLevel)level, placePos, level.getBlockState(placePos), level.random);
            if (level.getBlockState(placePos).is(sapling)) {
                //if we failed to grow the sapling, try a 2x2 configuration!
                if (inventory.countItem(sapling.asItem()) >= 4) {//if they have enough saplings...
                    for(int i = 0; i >= -1; --i) {
                        for (int j = 0; j >= -1; --j) {
                            List<BlockPos> checkPos = tryTwoByTwoSapling(saplingInfo, level, placePos, i, j);
                            if (!checkPos.isEmpty()) {
                                //we can place the saplings! Grow them.
                                level.setBlock(checkPos.get(0), saplingInfo.defaultBlockState(), 1 | 2);
                                level.setBlock(checkPos.get(1), saplingInfo.defaultBlockState(), 1 | 2);
                                level.setBlock(checkPos.get(2), saplingInfo.defaultBlockState(), 1 | 2);
                                usedSaplings = 4;
                                sapling.advanceTree((ServerLevel)level, placePos, level.getBlockState(placePos), level.random);
                                if (level.getBlockState(placePos).is(sapling)) {
                                    //this tree isn't 2x2, so return the saplings and give up.
                                    level.setBlock(checkPos.get(0), Blocks.AIR.defaultBlockState(), 1 | 2);
                                    level.setBlock(checkPos.get(1), Blocks.AIR.defaultBlockState(), 1 | 2);
                                    level.setBlock(checkPos.get(2), Blocks.AIR.defaultBlockState(), 1 | 2);
                                    level.setBlock(placePos, Blocks.AIR.defaultBlockState(), 1 | 2);
                                    usedSaplings = 0;
                                    player.stopUsingItem();
                                }
                                break;
                            }
                        }
                    }
                }
                if (usedSaplings == 1) {
                    level.setBlock(placePos, Blocks.AIR.defaultBlockState(), 1 | 2);
                    usedSaplings = 0;
                }
            }
        }
        Utils.tryRemoveAmount(saplingInfo.asItem(), inventory, usedSaplings);
        if (usedSaplings > 0) {
            ((ServerLevel)level).sendParticles(USE_PARTICLE,
                    placePos.getX() + 0.5,
                    placePos.getY() + 0.5f,
                    placePos.getZ() + 0.5,
                    50, 0.5, 0.2, 0.5, 1f);
            if (!player.isCreative())
                player.getMainHandItem().hurtAndBreak(2, (net.minecraft.server.level.ServerLevel) ((ServerPlayer) player).level(), (ServerPlayer) player, item -> {});
        }
    }
    private List<BlockPos> tryTwoByTwoSapling(SaplingBlock sapling, Level level, BlockPos pos, int pXOffset, int pZOffset) {
        List<BlockPos> ret = new ArrayList<>();
        //check the 3 available positions to see if it's viable to place all 3 saplings.
        BlockPos work = pos.offset(pXOffset + 1, 0, pZOffset);
        if (level.getBlockState(work).isAir() && sapling.defaultBlockState().canSurvive(level, work)) {
            ret.add(work);
        } else {
            return ret;
        }
        work = pos.offset(pXOffset, 0, pZOffset + 1);
        if (level.getBlockState(work).isAir() && sapling.defaultBlockState().canSurvive(level, work)) {
            ret.add(work);
        } else {
            ret.clear();
            return ret;
        }
        work = pos.offset(pXOffset + 1, 0, pZOffset + 1);
        if (level.getBlockState(work).isAir() && sapling.defaultBlockState().canSurvive(level, work)) {
            ret.add(work);
        } else {
            ret.clear();
            return ret;
        }
        return ret;
    }

    @Override
    public void onStopUsing(ItemStack stack, LivingEntity entity, int count) {
        ticksUsed = 0;
        super.onStopUsing(stack, entity, count);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
        if (playerIn.isUsingItem())
            return InteractionResultHolder.consume(Utils.getPlayerInteractionHandItem(playerIn, handIn));
        InteractionResultHolder<ItemStack> result = super.use(worldIn, playerIn, handIn);

        if (!Utils.hasEnoughDurability(Utils.getPlayerInteractionHandItem(playerIn, handIn), 2)) {
            return result;
        }

        HitResult pick = playerIn.pick(playerIn.blockInteractionRange(), 0.0F, false);
        // Hit something that wasn't a block.
        if (pick instanceof BlockHitResult blockHitResult && !worldIn.getBlockState(blockHitResult.getBlockPos()).isAir()) {
            return result;
        }
        playerIn.startUsingItem(handIn);
        return InteractionResultHolder.consume(Utils.getPlayerInteractionHandItem(playerIn, handIn));
    }

    @Override
    public boolean canPerformAction(ItemStack stack, net.neoforged.neoforge.common.ItemAbility toolAction) {
        return net.neoforged.neoforge.common.ItemAbilities.DEFAULT_AXE_ACTIONS.contains(toolAction);
    }
}
