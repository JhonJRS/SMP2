package net.sirplop.aetherworks.lib;
import net.sirplop.aetherworks.AWRegistry;

import com.rekindled.embers.particle.GlowParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStack;
import net.sirplop.aetherworks.network.PacketDistributor;
import net.sirplop.aetherworks.network.MessageFluidSync;
import net.sirplop.aetherworks.network.PacketHandler;
import net.sirplop.aetherworks.util.Utils;
import javax.annotation.Nullable;

import java.util.Stack;
import java.util.function.Predicate;

import static net.minecraft.world.level.block.Block.dropResources;

public class AWFluidNode extends AWHarvestNode {
    public final boolean drain;
    public final Fluid targetfluid;
    public final FluidHandlerItemStack fluidHandler;

    public AWFluidNode(Player harvester, Level level, BlockPos beginning, int range, Predicate<Player> canHarvest,
                       @Nullable GlowParticleOptions particle, double damageChance, boolean pickup) {
        super(harvester, level, beginning, range * 1000, canHarvest, particle, damageChance);
        this.drain = pickup;
        this.fluidHandler = new FluidHandlerItemStack(AWRegistry.FLUID_CONTENTS, usedItem, this.range);
        if (drain)
            this.targetfluid = level.getFluidState(beginning).getType();
        else
            this.targetfluid = fluidHandler.getFluid().getFluid();
    }

    @Override
    public void initNode() {
        if (!isLoaded(this.beginning))
        {
            this.invalid = true;
            return;
        }

        this.toHarvest.add(this.beginning);
        this.baseState = this.level.getBlockState(this.beginning);

        if (this.toHarvest.isEmpty())
        {
            this.invalid = true;
        }
    }

    @Override
    public void traverse(BlockPos from) { }

    public void traverseRepl(BlockPos from, Stack<BlockPos> nodes) {
        if ((drain && nodes.size() >= (fluidHandler.getTankCapacity(1) / 1000) - 1)
                || (!drain && nodes.size() >= (fluidHandler.getFluidInTank(1).getAmount() / 1000) - 1))
        {
            return;
        }

        if (drain) {
            for (Direction facing : Direction.values())
            {
                BlockPos offset = from.relative(facing);
                if (nodes.contains(offset) || !this.isLoaded(offset))
                {
                    continue;
                }

                FluidState state = level.getFluidState(offset);
                if (state.is(targetfluid))
                {
                    nodes.add(0, offset);
                }
            }
        } else {

            for (Direction facing : Direction.values())
            {
                if (facing == Direction.UP)
                    continue; //only allow placing on or below the selected level.
                BlockPos offset = from.relative(facing);
                if (nodes.contains(offset) || !this.isLoaded(offset))
                {
                    continue;
                }
                BlockState state = level.getBlockState(offset);
                if (state.isAir() || state.canBeReplaced())
                {
                    nodes.add(0, offset);
                }
            }
        }
    }

    public void tick()
    {
        if (!canHarvest.test(harvester)) {
            this.invalid = true;
            return;
        }
        Stack<BlockPos> repl = new Stack<>();
        boolean val;
        int change = 0;
        while (!toHarvest.isEmpty()) {
            if (!Utils.hasEnoughDurability(harvester.getMainHandItem(), 1)
                    || (!drain && fluidHandler.getFluidInTank(1).getAmount() - change < 1000)
                    || (drain && fluidHandler.getFluidInTank(1).getAmount() + change > fluidHandler.getTankCapacity(1) - 1000)) {
                this.invalid = true;
                break;
            }
            BlockPos pos = toHarvest.pop();
            val = false;

            BlockState blockstate = level.getBlockState(pos);
            Block block = blockstate.getBlock();
            if (drain) { //drain
                if (block instanceof BucketPickup bucketpickup) {
                    val = !bucketpickup.pickupBlock(null, level, pos, blockstate).isEmpty();
                } else if (blockstate.getBlock() instanceof LiquidBlock) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 1 | 2);
                    val = true;
                } else if (blockstate.is(Blocks.KELP) || blockstate.is(Blocks.KELP_PLANT) || blockstate.is(Blocks.SEAGRASS) || blockstate.is(Blocks.TALL_SEAGRASS)) {
                    BlockEntity blockentity = blockstate.hasBlockEntity() ? level.getBlockEntity(pos) : null;
                    dropResources(blockstate, level, pos, blockentity);
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 1 | 2);
                    val = true;
                }
            } else { //place
                if (blockstate.isAir() || blockstate.canBeReplaced(targetfluid)) {
                    if (blockstate.canBeReplaced(targetfluid))
                        level.destroyBlock(pos, true);
                    val = level.setBlock(pos, targetfluid.defaultFluidState().createLegacyBlock(), 1 | 2 | 8);
                }
            }
            //Aetherworks.LOGGER.atDebug().log("val: "+val);
            if (val) {
                change += 1000;
                if (!harvester.isCreative() && level.random.nextFloat() <= damageChance)
                    harvester.getMainHandItem().hurtAndBreak(1, (net.minecraft.server.level.ServerLevel) ((ServerPlayer) harvester).level(), (ServerPlayer) harvester, item -> {});
                if (particle != null) {
                    ((ServerLevel)level).sendParticles(particle,
                            pos.getX() + 0.5f,
                            pos.getY() + 0.5f,
                            pos.getZ() + 0.5f,
                            10, 0.25f, 0.25f, 0.25f, 0.25f);
                }
                this.level.playSound(null, pos, SoundEvents.SLIME_HURT_SMALL, SoundSource.BLOCKS, 0.2f, 0.75f + level.random.nextFloat() * 0.5f);

                traverseRepl(pos, repl);
            }
        }

        if (change > 0) {
            if (drain) {
                int amount = Math.min(fluidHandler.getTankCapacity(1), fluidHandler.getFluidInTank(1).getAmount() + change);
                MessageFluidSync.setFluid(fluidHandler, new FluidStack(targetfluid, amount));
                PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) harvester),
                        new MessageFluidSync(usedItem, new FluidStack(targetfluid, amount), range));
            }
            else {
                int amount = Math.max(0, fluidHandler.getFluidInTank(1).getAmount() - change);
                MessageFluidSync.setFluid(fluidHandler, new FluidStack(targetfluid, amount));
                PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) harvester),
                        new MessageFluidSync(usedItem, new FluidStack(targetfluid, amount), range));
            }
        }

        if (this.invalid || repl.isEmpty())
            this.invalid = true;
        else
            toHarvest.addAll(repl);
    }
}
