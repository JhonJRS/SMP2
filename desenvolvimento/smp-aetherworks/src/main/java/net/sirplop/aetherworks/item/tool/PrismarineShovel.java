package net.sirplop.aetherworks.item.tool;
import net.sirplop.aetherworks.AWRegistry;

import com.rekindled.embers.particle.GlowParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import com.rekindled.embers.compat.legacy.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.common.util.Lazy;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStack;
import net.sirplop.aetherworks.AWConfig;
import net.sirplop.aetherworks.api.item.IHudFocus;
import net.sirplop.aetherworks.lib.AWFluidNode;
import net.sirplop.aetherworks.lib.AWHarvestHelper;
import net.sirplop.aetherworks.util.AetheriumTiers;
import net.sirplop.aetherworks.util.Utils;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3f;

public class PrismarineShovel extends AOEEmberDiggerItem implements IHudFocus {
    protected final Lazy<Integer> capacity = Lazy.of(() -> AWConfig.PRISMARINE_SHOVEL_CAPACITY.get() * 1000);

    private final GlowParticleOptions particle = new GlowParticleOptions(getParticleColor(), 1, 15);

    public PrismarineShovel(Properties properties) {
        super(2f, -3f, AetheriumTiers.AETHERIUM, BlockTags.MINEABLE_WITH_SHOVEL, properties);
    }

    @Override
    public Vector3f getParticleColor() {
        return new Vector3f(56/255f, 195/255f, 250/225f);
    }

    @Override
    public ItemStack getFocus(ItemStack stack) {
        FluidHandlerItemStack fStack = new FluidHandlerItemStack(AWRegistry.FLUID_CONTENTS, stack, capacity.get());
        if (fStack.getFluid().isEmpty())
            return null;
        else {
            FluidStack fluid = fStack.getFluid();
            return new ItemStack(fluid.getFluid().getBucket(), fluid.getAmount() / 1000);
        }
    }
    @Override
    public boolean showAmount() { return true; }

    // SMP 2 port: the item fluid handler is registered in AWRegistry#registerCapabilities

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand handIn) {
        ItemStack stack = Utils.getPlayerInteractionHandItem(player, handIn);
        if (!(level instanceof ServerLevel))
            return InteractionResultHolder.pass(stack);;
        HitResult pick = player.pick(player.blockInteractionRange(), 0.0F, true);

        BlockHitResult blockHitResult = (BlockHitResult) pick;
        if (!level.getBlockState(blockHitResult.getBlockPos()).isAir()) {
            if (level.isClientSide())
                return InteractionResultHolder.pass(stack);

            FluidHandlerItemStack fStack = new FluidHandlerItemStack(AWRegistry.FLUID_CONTENTS, stack, capacity.get());

            BlockPos pos = blockHitResult.getBlockPos();
            BlockPos posRel = blockHitResult.getBlockPos().relative(blockHitResult.getDirection());

            FluidStack fluid = fStack.getFluid();

            if ((fluid.isEmpty() && !level.getFluidState(pos).isEmpty()) ||
                    (!fluid.isEmpty() && level.getFluidState(pos).is(fluid.getFluid()))) { //the tank is empty and there's fluid, or there's fluid that shares at ype with the tank
                if (suckFluids(fStack, level, pos, player, handIn))
                    return InteractionResultHolder.success(stack);
            } else if (!fluid.isEmpty() && level.getBlockState(posRel).isAir()) {
                if (placeFluids(fStack, level, posRel, player, handIn))
                    return InteractionResultHolder.success(stack);
            }
        }
        return super.use(level, player, handIn);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        HitResult pick = context.getPlayer().pick(context.getPlayer().blockInteractionRange(), 0.0F, true);

        BlockHitResult blockHitResult = (BlockHitResult) pick;
        if (!context.getLevel().getBlockState(blockHitResult.getBlockPos()).isAir()) {
            return InteractionResult.PASS;
        }
        return super.onItemUseFirst(stack, context);
    }

    public boolean suckFluids(FluidHandlerItemStack stack, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (!level.mayInteract(player, pos))
            return false;
        FluidState fluid = level.getFluidState(pos);
        if (!stack.getFluid().isEmpty() && !fluid.is(stack.getFluid().getFluid()))
            return false;
        if (AWHarvestHelper.addNode(player,
                new AWFluidNode(player, level, pos,
                        AWConfig.PRISMARINE_SHOVEL_CAPACITY.get(), p -> p.getMainHandItem().getItem() == this, particle, 0.75f, true)))
        {
             player.swing(hand, true);
            return true;
        }
        return false;
    }
    public boolean placeFluids(FluidHandlerItemStack stack, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (!level.mayInteract(player, pos) || stack.getFluid().isEmpty())
            return false;
        else if (level.dimensionType().ultraWarm() && stack.getFluid().getFluid().is(FluidTags.WATER)) {
            int i = pos.getX();
            int j = pos.getY();
            int k = pos.getZ();
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F + (level.random.nextFloat() - level.random.nextFloat()) * 0.8F);

            ((ServerLevel) level).sendParticles(ParticleTypes.LARGE_SMOKE,
                    i + 0.5,
                    j + 0.5,
                    k + 0.5, 8, 0.375f, 0.375f, 0.375f, 0.0f);
            player.swing(hand, true);
            return false;
        }
        if (AWHarvestHelper.addNode(player,
                new AWFluidNode(player, level, pos,
                        AWConfig.PRISMARINE_SHOVEL_CAPACITY.get(), p -> p.getMainHandItem().getItem() == this, particle, 0.75f, false)))
        {
           player.swing(hand, true);
            return true;
        }
        return false;
    }

    @Override
    public boolean canPerformAction(ItemStack stack, net.neoforged.neoforge.common.ItemAbility toolAction) {
        return net.neoforged.neoforge.common.ItemAbilities.DEFAULT_SHOVEL_ACTIONS.contains(toolAction);
    }
}
