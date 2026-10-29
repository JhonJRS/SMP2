package net.sirplop.aetherworks.lib;

import com.mojang.datafixers.util.Pair;
import com.rekindled.embers.particle.GlowParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.sirplop.aetherworks.util.Utils;
import javax.annotation.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class AWTillNode extends AWHarvestNode {
    public AWTillNode(Player harvester, Level level, BlockPos beginning, int range, Predicate<Player> canHarvest,
                      @Nullable GlowParticleOptions particle, double damageChance, Direction direction,
                      Predicate<BlockState> checkStateMatch) {
        super(harvester, level, beginning, range, canHarvest, particle, damageChance);
        initialDirection = direction;
        this.checkStateMatch = checkStateMatch;
    }

    public final Direction initialDirection;
    public final Predicate<BlockState> checkStateMatch;

    @Override
    public void initNode()
    {
        if (!isLoaded(this.beginning) || initialDirection == Direction.DOWN || initialDirection == Direction.UP)
        {
            this.invalid = true;
            return;
        }

        UseOnContext ctx = new UseOnContext(harvester, InteractionHand.MAIN_HAND, new BlockHitResult(this.beginning.getCenter(), Direction.UP, this.beginning, false));
        BlockState toolModifiedState = level.getBlockState(this.beginning).getToolModifiedState(ctx, net.neoforged.neoforge.common.ItemAbilities.HOE_TILL, true);
        if (toolModifiedState == null) {
            this.invalid = true;
            return; //ignore tiles that don't till.
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
        int depth = 0;
        BlockPos check = from;
        while (check != null) {
            if (depth >= range || !level.isLoaded(check))
                break;
            BlockState state = this.level.getBlockState(check);
            if (!state.getFluidState().isEmpty()) { //skip fluids.
                check = check.relative(initialDirection);
                depth++;
                continue;
            }
            if (!checkStateMatch.test(state) || !this.level.getBlockState(check.above()).isAir())
                break;

            this.toHarvest.add(0, check);
            check = check.relative(initialDirection);
            depth++;
        }
    }

    @Override
    public void tick()
    {
        if (!canHarvest.test(harvester) || !Utils.hasEnoughDurability(harvester.getMainHandItem(), 1)) {
            this.invalid = true;
            return;
        }
        if (!this.toHarvest.isEmpty()) {
            till(this.toHarvest.pop());
        }
        if (this.toHarvest.isEmpty())
            this.invalid = true;
    }

    protected void till(BlockPos pos) {
        UseOnContext ctx = new UseOnContext(harvester, InteractionHand.MAIN_HAND, new BlockHitResult(pos.getCenter(), Direction.UP, pos, false));
        BlockState toolModifiedState = level.getBlockState(pos).getToolModifiedState(ctx, net.neoforged.neoforge.common.ItemAbilities.HOE_TILL, false);
        Pair<Predicate<UseOnContext>, Consumer<UseOnContext>> pair = toolModifiedState == null ? null : Pair.of(context -> true, HoeItem.changeIntoState(toolModifiedState));
        if (pair == null || !pair.getFirst().test(ctx))
            return;
        level.playSound(harvester, pos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);
        pair.getSecond().accept(ctx);
        if (!harvester.isCreative())
            harvester.getMainHandItem().hurtAndBreak(1, (net.minecraft.server.level.ServerLevel) ((ServerPlayer) harvester).level(), (ServerPlayer) harvester, item -> {});
        if (particle != null) {
            ((ServerLevel)level).sendParticles(particle,
                    pos.getX() + 0.5f,
                    pos.getY() + 1f,
                    pos.getZ() + 0.5f,
                    10, 0.25f, 0.1f, 0.25f, 0.25f);
        }
    }
}