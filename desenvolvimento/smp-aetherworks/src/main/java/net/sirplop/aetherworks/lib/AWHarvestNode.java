package net.sirplop.aetherworks.lib;

import com.rekindled.embers.particle.GlowParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.sirplop.aetherworks.AWConfig;
import net.sirplop.aetherworks.util.Utils;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.function.Predicate;

public class AWHarvestNode {
    public final Player harvester;
    public final Level level;
    public final BlockPos beginning;
    public final Predicate<Player> canHarvest;
    public final int range;
    public final Stack<BlockPos> toHarvest = new Stack<>();
    protected BlockState baseState;
    protected final GlowParticleOptions particle;
    protected final ItemStack usedItem;
    protected double damageChance;

    public boolean allowSimilarBlocks = true;

    public boolean isInvalid()
    {
        return invalid;
    }

    protected boolean invalid;

    public boolean isLoaded(BlockPos pos)
    {
        return level.isLoaded(pos);
    }
    public double distanceToSqr(BlockPos pos)
    {
        return beginning.distToCenterSqr(pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f);
    }


    public AWHarvestNode(Player harvester, Level level, BlockPos beginning,
                         int range, Predicate<Player> canHarvest, @Nullable GlowParticleOptions particle, double damageChance)
    {
        this.harvester = harvester;
        this.level = level;
        this.beginning = beginning;
        this.canHarvest = canHarvest;
        this.range = range;
        this.particle = particle;
        this.usedItem = harvester.getMainHandItem();
        this.damageChance = damageChance;
    }
    public void initNode()
    {
        if (!isLoaded(this.beginning))
        {
            this.invalid = true;
            return;
        }

        this.toHarvest.add(this.beginning);
        this.baseState = this.level.getBlockState(this.beginning);
        this.traverse(this.beginning);

        this.toHarvest.sort((BlockPos l, BlockPos r) -> (int) (distanceToSqr(r) - distanceToSqr(l)));
        if (this.toHarvest.isEmpty())
        {
            this.invalid = true;
        }
    }

    public void traverse(BlockPos from) {
        if (from.distToCenterSqr(this.beginning.getX() + 0.5, this.beginning.getY() + 0.5, this.beginning.getZ() + 0.5) >= (range * range)-1)
            return;
        Stack<BlockPos> check = new Stack<>();
        check.add(from);
        this.toHarvest.add(0, from);
        Set<Block> sameBlocks = AWConfig.getSameBlocks(level.getBlockState(from).getBlock());
        while (!check.isEmpty()) {
            BlockPos pos = check.pop();
            if (pos.distToCenterSqr(this.beginning.getX() + 0.5, this.beginning.getY() + 0.5, this.beginning.getZ() + 0.5) >= (range * range)-1)
                continue;

            for (Direction facing : Direction.values()) {
                BlockPos offset = pos.relative(facing);
                if (this.toHarvest.contains(offset)) {
                    continue;
                }

                if (!this.isLoaded(offset)) {
                    continue;
                }

                Block block = this.level.getBlockState(offset).getBlock();
                if (block.equals(this.baseState.getBlock()) || (allowSimilarBlocks && sameBlocks.contains(block))) {
                    this.toHarvest.add(0, offset);
                    check.add(offset);
                }
            }
        }
    }

    public void tick()
    {
        if (!(level instanceof ServerLevel)){
            this.invalid = true;
            return;
        }
        if (!canHarvest.test(harvester) || !Utils.hasEnoughDurability(harvester.getMainHandItem(), 1)) {
            this.invalid = true;
            return;
        }
        BlockPos pos = this.toHarvest.pop();
        boolean val = Utils.breakAndHarvestBlock((ServerLevel)level, pos, (ServerPlayer)harvester, harvester.getMainHandItem(),
                Direction.getRandom(level.random), (state) -> state.getBlock().equals(baseState.getBlock()) || (allowSimilarBlocks && AWConfig.getSameBlocks(baseState.getBlock()).contains(state.getBlock())), false, true).isEmpty();
        if (val) {
            if (!harvester.isCreative() && level.random.nextFloat() <= damageChance)
                harvester.getMainHandItem().hurtAndBreak(1, (net.minecraft.server.level.ServerLevel) ((ServerPlayer) harvester).level(), (ServerPlayer) harvester, item -> {});
            if (particle != null) {
                ((ServerLevel)level).sendParticles(particle,
                        pos.getX() + 0.5f,
                        pos.getY() + 0.5f,
                        pos.getZ() + 0.5f,
                        10, 0.25f, 0.25f, 0.25f, 0.25f);
            }
        }
        if (this.toHarvest.isEmpty())
            this.invalid = true;
    }
}
