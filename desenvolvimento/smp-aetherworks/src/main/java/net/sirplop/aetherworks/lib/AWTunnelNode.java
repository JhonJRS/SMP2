package net.sirplop.aetherworks.lib;

import com.rekindled.embers.RegistryManager;
import com.rekindled.embers.particle.GlowParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.sirplop.aetherworks.Aetherworks;
import net.sirplop.aetherworks.util.Utils;
import javax.annotation.Nullable;

import java.util.*;
import java.util.function.Predicate;

public class AWTunnelNode extends AWHarvestNode {
    public AWTunnelNode(Player harvester, Level level, BlockPos beginning, int range,
                        Predicate<Player> canHarvest, @Nullable GlowParticleOptions particle, double damageChance, Direction direction,
                        Predicate<BlockState> checkStateMatch) {
        super(harvester, level, beginning, range, canHarvest, particle, damageChance);
        initialDirection = direction;
        this.checkStateMatch = checkStateMatch;
    }

    public final Stack<Stack<BlockPos>> toHarvestLeftTunnel = new Stack<>();
    public final Stack<Stack<BlockPos>> toHarvestRightTunnel = new Stack<>();
    public final Direction initialDirection;
    public final Predicate<BlockState> checkStateMatch;

    @Override
    public void initNode()
    {
        if (!isLoaded(this.beginning))
        {
            this.invalid = true;
            return;
        }

        this.baseState = this.level.getBlockState(this.beginning);
        this.traverse(this.beginning);
        if (this.toHarvest.isEmpty())
        {
            this.invalid = true;
        }
    }

    @Override
    public void traverse(BlockPos from) {
        if (initialDirection == Direction.UP || initialDirection == Direction.DOWN) {
            this.invalid = true; //this can't mine up or down.
            return;
        }
        int depth = 0;
        BlockPos check = from;

        while (check != null) {
            if (depth >= range || !level.isLoaded(check))
                break;
            BlockState state = this.level.getBlockState(check);
            if (!checkStateMatch.test(state) && !state.isAir())
                break;

            BlockPos below = check.below();
            BlockState belowState = this.level.getBlockState(below);
            if (!checkStateMatch.test(belowState) && !belowState.isAir())
                break; //don't continue the tunnel unless the whole space is clear.

            if (belowState.isAir() && state.isAir())
                break; //too much air - another tunnel or just a gap?

            this.toHarvest.add(0, check);
            this.toHarvest.add(0, below);

            if (depth % 3 == 2) {
                Direction left, right;
                switch (initialDirection) {
                    case NORTH -> {
                        left = Direction.WEST;
                        right = Direction.EAST;
                    }
                    case SOUTH -> {
                        left = Direction.EAST;
                        right = Direction.WEST;
                    }
                    case WEST -> {
                        left = Direction.SOUTH;
                        right = Direction.NORTH;
                    }
                    case EAST -> {
                        left = Direction.NORTH;
                        right = Direction.SOUTH;
                    }
                    default -> {
                        left = Direction.UP;
                        right = Direction.DOWN;
                    }
                }
                toHarvestLeftTunnel.add(0, traverseRecursiveTunnel(0, check.relative(left), left, new Stack<>()));
                toHarvestRightTunnel.add(0, traverseRecursiveTunnel(0, check.relative(right), right, new Stack<>()));
            }

            check = check.relative(initialDirection);
            depth++;
        }
    }

    private Stack<BlockPos> traverseRecursiveTunnel(int depth, BlockPos from, Direction direction,  Stack<BlockPos> inStack) {
        if (depth == 4)
            return inStack;

        BlockState state = this.level.getBlockState(from);
        if (!checkStateMatch.test(state) && !state.isAir())
            return inStack; //don't continue the tunnel unless the whole space is clear.

        inStack.add(from);
        traverseRecursiveTunnel(++depth, from.relative(direction), direction, inStack);
        return inStack;
    }

    private int tickCount = 0;
    private Stack<BlockPos> leftTarget;
    private Stack<BlockPos> rightTarget;

    @Override
    public void tick()
    {
        if (!canHarvest.test(harvester) || !Utils.hasEnoughDurability(harvester.getMainHandItem(), 1)) {
            this.invalid = true;
            return;
        }
        if (tickCount % 6 == 5) {
            //do the thing with the left and right tunnels.
            leftTarget = toHarvestLeftTunnel == null ? null : toHarvestLeftTunnel.pop();
            rightTarget = toHarvestRightTunnel == null ? null : toHarvestRightTunnel.pop();
        }
        boolean leftHas = leftTarget != null && !leftTarget.isEmpty();
        boolean rightHas = rightTarget != null && !rightTarget.isEmpty();

        if (!this.toHarvest.isEmpty()) {
            BlockPos pos = this.toHarvest.pop();
            harvest(pos);
            if (tickCount % 12 == 11 && (leftHas || rightHas)) {
                level.setBlock(pos, RegistryManager.GLIMMER.get().defaultBlockState(), 1 | 2);
            }
        }
        if (leftHas)
            harvest(leftTarget.pop());
        if (rightHas)
            harvest((rightTarget.pop()));

        tickCount++;
        if (this.toHarvest.isEmpty() && !leftHas && !rightHas)
            this.invalid = true;
    }

    protected void harvest(BlockPos pos) {
        boolean val = Utils.breakAndHarvestBlock((ServerLevel)level, pos, (ServerPlayer)harvester, harvester.getMainHandItem(),
                Direction.getRandom(level.random), (state) -> true, false, true).isEmpty();
        if (val) {
            if (!harvester.isCreative())
                harvester.getMainHandItem().hurtAndBreak(1, (net.minecraft.server.level.ServerLevel) ((ServerPlayer) harvester).level(), (ServerPlayer) harvester, item -> {});
            if (particle != null) {
                ((ServerLevel)level).sendParticles(particle,
                        pos.getX() + 0.5f,
                        pos.getY() + 0.5f,
                        pos.getZ() + 0.5f,
                        10, 0.25f, 0.25f, 0.25f, 0.25f);
            }
        }
    }
}
