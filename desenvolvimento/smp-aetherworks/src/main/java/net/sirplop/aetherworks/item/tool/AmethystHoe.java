package net.sirplop.aetherworks.item.tool;

import com.rekindled.embers.particle.GlowParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.sirplop.aetherworks.AWConfig;
import net.sirplop.aetherworks.lib.AWCropNode;
import net.sirplop.aetherworks.lib.AWHarvestHelper;
import net.sirplop.aetherworks.lib.AWTillNode;
import net.sirplop.aetherworks.util.AetheriumTiers;
import org.joml.Vector3f;

public class AmethystHoe extends AOEEmberHoeItem {
    public AmethystHoe(Properties pProperties) {
        super(-2f, 0f, AetheriumTiers.AETHERIUM, BlockTags.MINEABLE_WITH_HOE, pProperties);
    }

    private final GlowParticleOptions particle = new GlowParticleOptions(getParticleColor(), 1, 15);

    @Override
    public Vector3f getParticleColor() { return new Vector3f(1f, 0.55F, 0.74F); }

    public boolean useOnBlock(PlayerInteractEvent.RightClickBlock context) {
        if (!(context.getLevel() instanceof ServerLevel) || context.getLevel().isClientSide
            || context.getEntity() == null || AWHarvestHelper.alreadyHasPlayer(context.getEntity()))
            return false;
        
        if (context.getHand() == InteractionHand.MAIN_HAND)
        {
            BlockState selectedBlock = context.getLevel().getBlockState(context.getPos());
            //determine if block is a plant - if it is, do plant harvest, otherwise till.
            if (AWCropNode.isCrop(selectedBlock.getBlock())) {
                if (AWHarvestHelper.addNode(context.getEntity(),
                        new AWCropNode(context.getEntity(), context.getLevel(), context.getPos(),
                                AWConfig.AMETHYST_HOE_HARVEST_RANGE.get(), p -> p.getMainHandItem().getItem() == this,
                                particle, 1f)))
                {
                    context.getEntity().swing(context.getHand(), true);
                    return true;
                }
            } else {

                if (AWHarvestHelper.addNode(context.getEntity(),
                        new AWTillNode(context.getEntity(), context.getLevel(), context.getPos(),
                                AWConfig.AMETHYST_HOE_TILL_RANGE.get(), p -> p.getMainHandItem().getItem() == this,
                                particle, 1f, context.getEntity().getDirection(), (state) -> state.getBlock() == selectedBlock.getBlock())))
                {
                    context.getEntity().swing(context.getHand(), true);
                    return true;
                }
            }
        }

        return false;
    }
}
