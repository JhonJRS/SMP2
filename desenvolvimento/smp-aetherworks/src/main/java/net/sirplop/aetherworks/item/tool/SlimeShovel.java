package net.sirplop.aetherworks.item.tool;
import net.sirplop.aetherworks.util.Compat121;

import com.rekindled.embers.particle.GlowParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.sirplop.aetherworks.network.PacketDistributor;
import net.sirplop.aetherworks.AWConfig;
import net.sirplop.aetherworks.api.item.IHudFocus;
import net.sirplop.aetherworks.datagen.AWBlockTags;
import net.sirplop.aetherworks.lib.AWExchangeNode;
import net.sirplop.aetherworks.lib.AWHarvestHelper;
import net.sirplop.aetherworks.network.MessageFocusedStack;
import net.sirplop.aetherworks.network.PacketHandler;
import net.sirplop.aetherworks.util.AetheriumTiers;
import net.sirplop.aetherworks.util.Utils;
import javax.annotation.Nonnull;
import org.joml.Vector3f;

public class SlimeShovel extends AOEEmberDiggerItem implements IHudFocus {
    public SlimeShovel(Properties properties) {
        super(2f, -3f, AetheriumTiers.AETHERIUM, BlockTags.MINEABLE_WITH_SHOVEL, properties);
    }

    public void setFocus(ItemStack held, ItemStack focus, Player player) {
        MessageFocusedStack.setFocus(held, focus);
        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), new MessageFocusedStack(held, focus));
    }

    public ItemStack getFocus(ItemStack held) {
        return Compat121.stackOf(Compat121.tag(held).getCompound(MessageFocusedStack.FOCUS_TAG));
    }

    @Override
    public boolean showAmount() { return false; }

    private final GlowParticleOptions particle = new GlowParticleOptions(getParticleColor(), 1, 15);

    @Override
    public @Nonnull InteractionResultHolder<ItemStack> use(@Nonnull Level worldIn, Player playerIn, @Nonnull InteractionHand handIn) {
        HitResult pick = playerIn.pick(playerIn.blockInteractionRange(), 0.0F, false);
        // Hit something that wasn't a block.
        if (pick instanceof BlockHitResult blockHitResult && !worldIn.getBlockState(blockHitResult.getBlockPos()).isAir()) {
            return InteractionResultHolder.pass(Utils.getPlayerInteractionHandItem(playerIn, handIn));
        }
        return super.use(worldIn, playerIn, handIn);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        HitResult pick = context.getPlayer().pick(context.getPlayer().blockInteractionRange(), 0.0F, false);
        // Hit something that wasn't a block.
        if (pick instanceof BlockHitResult blockHitResult && !context.getLevel().getBlockState(blockHitResult.getBlockPos()).isAir()) {
            return InteractionResult.PASS;
        }
        return super.onItemUseFirst(stack, context);
    }

    @Override
    public Vector3f getParticleColor() {
        return new Vector3f(131/255f, 200/255f, 115/225f);
    }

    @Override
    public @Nonnull InteractionResult useOn(@Nonnull UseOnContext context) {
        InteractionResult result = super.useOn(context);
        Player player = context.getPlayer();
        if (!(context.getLevel() instanceof ServerLevel) || player == null || Utils.isFakePlayer(player) || context.getLevel().isClientSide()) {
            return result;
        }
        ItemStack usedItem = context.getItemInHand();
        //if sneaking, select block
        if (player.isShiftKeyDown()) {
            Block target = context.getLevel().getBlockState(context.getClickedPos()).getBlock();
            if (context.getLevel().getBlockEntity(context.getClickedPos()) != null) {
                return InteractionResult.PASS; //I forbid block entity exchanging. That can cause lotsa problems.
            }
            ItemStack focused = getFocus(usedItem);
            if (target.asItem().equals(focused.getItem())) {
                setFocus(usedItem, ItemStack.EMPTY, player);
            } else {
                setFocus(usedItem, new ItemStack(target.asItem()), player);
            }
          return InteractionResult.SUCCESS;
        }

        //else, put it in exchange node and let it run
        if (Utils.blockHasTag(context.getLevel().getBlockState(context.getClickedPos()), AWBlockTags.SOIC_BANNED))
            return result;

        if (result == InteractionResult.PASS && context.getHand() == InteractionHand.MAIN_HAND)
        {
            ItemStack focused = Compat121.stackOf(Compat121.tag(usedItem).getCompound(MessageFocusedStack.FOCUS_TAG));
            if (focused.getItem().equals(context.getLevel().getBlockState(context.getClickedPos()).getBlock().asItem()))
                return InteractionResult.PASS;
            AWExchangeNode node = new AWExchangeNode(context.getPlayer(), context.getLevel(), context.getClickedPos(),
                    AWConfig.SLIME_SHOVEL_RANGE.get(), p -> p.getMainHandItem().getItem() == this, particle, focused);
            node.allowSimilarBlocks = false;
            if (AWHarvestHelper.addNode(context.getPlayer(), node))
            {
                context.getPlayer().swing(context.getHand(), true);
                return InteractionResult.SUCCESS;
            }
        }

        return result;
    }

    @Override
    public boolean canPerformAction(ItemStack stack, net.neoforged.neoforge.common.ItemAbility toolAction) {
        return net.neoforged.neoforge.common.ItemAbilities.DEFAULT_SHOVEL_ACTIONS.contains(toolAction);
    }
}
