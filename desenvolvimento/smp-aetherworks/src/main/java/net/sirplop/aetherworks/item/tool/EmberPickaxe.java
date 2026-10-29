package net.sirplop.aetherworks.item.tool;

import com.rekindled.embers.particle.GlowParticleOptions;
import com.rekindled.embers.util.EmbersColors;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.sirplop.aetherworks.datagen.AWBlockTags;
import net.sirplop.aetherworks.lib.AWHarvestHelper;
import net.sirplop.aetherworks.lib.AWTunnelNode;
import net.sirplop.aetherworks.AWConfig;
import net.sirplop.aetherworks.util.AetheriumTiers;
import net.sirplop.aetherworks.util.Utils;
import javax.annotation.Nonnull;
import org.joml.Vector3f;

public class EmberPickaxe  extends AOEEmberDiggerItem{
    public EmberPickaxe(Properties properties) {
        super(1.5f, -2.8f, AetheriumTiers.AETHERIUM, BlockTags.MINEABLE_WITH_PICKAXE, properties);
    }

    private final GlowParticleOptions particle = new GlowParticleOptions(getParticleColor(), 1, 15);

    @Override
    public Vector3f getParticleColor() {
        return EmbersColors.EMBER;
    }

    @Override
    public @Nonnull InteractionResult useOn(UseOnContext context) {
        InteractionResult result = super.useOn(context);
        if (!(context.getLevel() instanceof ServerLevel) || context.getLevel().isClientSide)
            return result;

        if (context.getPlayer() == null
                || !Utils.blockHasTag(context.getLevel().getBlockState(context.getClickedPos()), AWBlockTags.POMD_ALLOWED))
            return result;
        if (result == InteractionResult.PASS && context.getHand() == InteractionHand.MAIN_HAND
                && context.getLevel().getBlockState(context.getClickedPos()).canHarvestBlock(context.getLevel(), context.getClickedPos(), context.getPlayer()))
        {
            HitResult pick = context.getPlayer().pick(20D, 0.0F, false);

            // Hit something that wasn't a block.
            if (!(pick instanceof BlockHitResult blockHitResult)) {
                return result;
            }
            if (AWHarvestHelper.addNode(context.getPlayer(),
                    new AWTunnelNode(context.getPlayer(), context.getLevel(), context.getClickedPos(),
                            AWConfig.EMBER_PICKAXE_RANGE.get() * 3, p -> p.getMainHandItem().getItem() == this,
                            particle, 0.75, blockHitResult.getDirection().getOpposite(), (state) -> Utils.blockHasTag(state, AWBlockTags.POMD_ALLOWED))))
            {
                context.getPlayer().swing(context.getHand(), true);
                return InteractionResult.SUCCESS;
            }
        }

        return result;
    }

    @Override
    public boolean canPerformAction(ItemStack stack, net.neoforged.neoforge.common.ItemAbility toolAction) {
        return net.neoforged.neoforge.common.ItemAbilities.DEFAULT_PICKAXE_ACTIONS.contains(toolAction);
    }
}
