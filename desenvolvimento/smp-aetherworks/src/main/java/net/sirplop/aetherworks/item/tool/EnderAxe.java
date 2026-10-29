package net.sirplop.aetherworks.item.tool;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.sirplop.aetherworks.AWConfig;
import net.sirplop.aetherworks.datagen.AWBlockTags;
import net.sirplop.aetherworks.lib.OctDirection;
import net.sirplop.aetherworks.util.AetheriumTiers;
import net.sirplop.aetherworks.util.Utils;
import javax.annotation.Nonnull;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;

public class EnderAxe extends AOEEmberDiggerItem{

    private final Vector3f color = new Vector3f(205f / 255f, 0, 1f);

    public EnderAxe(Properties properties) {
        super(5f, -3f, AetheriumTiers.AETHERIUM, BlockTags.MINEABLE_WITH_AXE, properties);
    }

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
                || !Utils.blockHasTag(context.getLevel().getBlockState(context.getClickedPos()), AWBlockTags.AOTR_ALLOWED))
            return result;
        if (result == InteractionResult.PASS && context.getHand() == InteractionHand.MAIN_HAND
                && context.getLevel().getBlockState(context.getClickedPos()).canHarvestBlock(context.getLevel(), context.getClickedPos(), context.getPlayer())
                && Utils.hasEnoughDurability(context.getPlayer().getMainHandItem(), 1))
        {
            context.getPlayer().swing(context.getHand(), true);
            BlockPos target = findFurthestLogRecursive(context.getLevel(), context.getClickedPos(), context.getLevel().getBlockState(context.getClickedPos()).getBlock(), 0, new HashSet<>(), AWConfig.getSameBlocks(context.getLevel().getBlockState(context.getClickedPos()).getBlock()));
            Utils.breakAndHarvestBlock((ServerLevel)context.getLevel(), target, (ServerPlayer)context.getPlayer(), context.getPlayer().getMainHandItem(),
                    Direction.getRandom(context.getLevel().random), (state) -> true, false, true);
            context.getLevel().playSound(null, context.getClickedPos(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.15f, 2 - context.getLevel().random.nextFloat());

            ((ServerLevel)context.getLevel()).sendParticles(getParticle(),
                    target.getX() + 0.5f,
                    target.getY() + 0.5f,
                    target.getZ() + 0.5f,
                    10, 0.25f, 0.25f, 0.25f, 0.1f);

            if (!context.getPlayer().isCreative())
                context.getPlayer().getMainHandItem().hurtAndBreak(1, (net.minecraft.server.level.ServerLevel) ((ServerPlayer) context.getPlayer()).level(), (ServerPlayer) context.getPlayer(), item -> {});

            return InteractionResult.SUCCESS;
        }

        return result;
    }

    private BlockPos findFurthestLogRecursive(final Level level, BlockPos at, final Block compare, int iter, Set<BlockPos> known, final Set<Block> sameBlocks) {
        if (++iter >= 512)
        {
            return at;
        }
        if (isSame(level.getBlockState(at.above()).getBlock(), compare, sameBlocks)) {
            known.add(at);
            return findFurthestLogRecursive(level, at.above(), compare, iter, known, sameBlocks);
        }
        for (OctDirection dir : OctDirection.values()) {
            BlockPos check = dir.offsetBlock(at);
            if (known.contains(check)) {
                continue;
            }
            if (isSame(level.getBlockState(check).getBlock(), compare, sameBlocks)) {
                known.add(at);
                return findFurthestLogRecursive(level, check, compare, iter, known, sameBlocks);
            }
            if (isSame(level.getBlockState(check.above()).getBlock(), compare, sameBlocks)) {
                known.add(at);
                return findFurthestLogRecursive(level, check.above(), compare, iter, known, sameBlocks);
            }
        }
        return at;
    }
    private boolean isSame(Block block, Block compare, Set<Block> sameBlocks) {
        return block.equals(compare) || sameBlocks.contains(block);
    }

    @Override
    public boolean canPerformAction(ItemStack stack, net.neoforged.neoforge.common.ItemAbility toolAction) {
        return net.neoforged.neoforge.common.ItemAbilities.DEFAULT_AXE_ACTIONS.contains(toolAction);
    }
}
