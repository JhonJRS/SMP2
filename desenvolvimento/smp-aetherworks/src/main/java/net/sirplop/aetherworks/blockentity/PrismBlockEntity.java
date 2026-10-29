package net.sirplop.aetherworks.blockentity;
import net.minecraft.core.HolderLookup;

import com.rekindled.embers.RegistryManager;
import com.rekindled.embers.blockentity.FluidVesselBlockEntity;
import com.rekindled.embers.particle.GlowParticleOptions;
import com.rekindled.embers.util.EmbersColors;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.sirplop.aetherworks.AWRegistry;
import net.sirplop.aetherworks.block.MoonlightAmplifierBlock;
import net.sirplop.aetherworks.util.Utils;
import javax.annotation.Nonnull;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;

import java.util.ArrayList;
import java.util.List;

public class PrismBlockEntity extends BlockEntity {
    public PrismBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(AWRegistry.PRISM_BLOCK_ENTITY.get(), pPos, pBlockState);
    }
    public int renderFrame = 0;
    public float renderTime = 0;


    public boolean isStructureValid()
    {
        return this.isStructureValid;
    }
    private void setStructureValid(boolean value) {
        if (isStructureValid != value)
        {
            isStructureValid = value;
            level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 1 | 2);
        }
    }

    private boolean canWork()
    {
        return this.canWork;
    }
    private void setWork(boolean value) {
        if (canWork != value)
        {
            canWork = value;
            level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 1 | 2);
        }
    }

    private boolean isStructureValid;
    private boolean canWork;
    private int ticksExisted;

    private BlockPos currentCrystal;
    private int currentCrystalInt = 0;
    private int currentCrystalFXTime = 0;
    private boolean currentCrystalHeat;

    public static final GlowParticleOptions GLOW = new GlowParticleOptions(Utils.AETHERIUM_COLOR, 2f, 80);
    public static final GlowParticleOptions EMBER = new GlowParticleOptions(EmbersColors.EMBER_ID, 2f, 80);
    public static final GlowParticleOptions GLOW_WORK = new GlowParticleOptions(Utils.AETHERIUM_COLOR, 1.5f, 40);


    public static void clientTick(Level level, BlockPos pos, BlockState state, PrismBlockEntity blockEntity){
        if (!level.isClientSide())
            return;
        if (blockEntity.canWork() && blockEntity.isStructureValid())
        {
            blockEntity.mkCrystalFX(level, pos);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PrismBlockEntity blockEntity) {
        if (level.isClientSide())
            return;
        if (++blockEntity.ticksExisted % 60 == 0)
        {
            blockEntity.updateTick((ServerLevel) level, pos);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void mkCrystalFX(Level level, BlockPos pos)
    {
        List<BlockPos> crystals = new ArrayList<>();

        // Matrices
        if (level.getBlockState(pos.below().west(2).north(2)).is(AWRegistry.CONTROL_MATRIX.get()))
            crystals.add(pos.below().west(2).north(2));
        if (level.getBlockState(pos.below().west(2).south(2)).is(AWRegistry.CONTROL_MATRIX.get()))
            crystals.add(pos.below().west(2).south(2));
        if (level.getBlockState(pos.below().east(2).north(2)).is(AWRegistry.CONTROL_MATRIX.get()))
            crystals.add(pos.below().east(2).north(2));
        if (level.getBlockState(pos.below().east(2).south(2)).is(AWRegistry.CONTROL_MATRIX.get()))
            crystals.add(pos.below().east(2).south(2));

        if (currentCrystalFXTime > 50 && !crystals.isEmpty())
            currentCrystalFXTime = 0;
        else if (crystals.isEmpty()) {
            currentCrystalFXTime = 9999; //arbitrary large number
            currentCrystalInt = 5; //out of bounds
            return;
        }

        if (--currentCrystalFXTime <= 0)
        {
            int crystalPtr = 0;
            if (crystals.size() > 1) { //randomization requires 2+ crystals
                crystalPtr = level.random.nextInt(crystals.size() - 1);
                if (crystalPtr >= currentCrystalInt)
                    crystalPtr++;
            }
            currentCrystalInt = crystalPtr;
            currentCrystal = crystals.get(crystalPtr);
            currentCrystalFXTime = 40;
            currentCrystalHeat = level.random.nextBoolean();
            level.playSound(Minecraft.getInstance().player, currentCrystal,
                    SoundEvents.FIRECHARGE_USE,
                    SoundSource.BLOCKS, 0.5f, 0.1F);
        }
        else
        {
            float progress = (float)currentCrystalFXTime / 40;
            float sX = pos.getX() - ((float)(pos.getX() - currentCrystal.getX()) * progress) + 0.5F;
            float sZ = pos.getZ() - ((float)(pos.getZ() - currentCrystal.getZ()) * progress) + 0.5F;
            float sY = pos.getY() + 0.5F + (float)Math.sin(Math.toRadians(progress * 360));
            if (this.currentCrystalHeat)
            {
                level.addParticle(EMBER, sX, sY, sZ, 0, -0.01f, 0);
            }
            else
            {
                level.addParticle(GLOW, sX, sY, sZ, 0, -0.01f, 0);
            }
        }
    }

    public void updateTick(ServerLevel level, BlockPos pos) {
        setStructureValid(checkStructure(level, pos));
        setWork(isStructureValid && checkWorkConditions(level, pos));

        if (canWork)
        {
            work(level, pos);
        }
    }

    private void work(ServerLevel level, BlockPos pos)
    {
        FluidVesselBlockEntity target = (FluidVesselBlockEntity)level.getBlockEntity(pos.below(3));
        if (target != null)
        {
            int amount = 1;
            // Matrices
            if (level.getBlockState(pos.below().west(2).north(2)).is(AWRegistry.CONTROL_MATRIX.get()))
                amount++;
            if (level.getBlockState(pos.below().west(2).south(2)).is(AWRegistry.CONTROL_MATRIX.get()))
                amount++;
            if (level.getBlockState(pos.below().east(2).north(2)).is(AWRegistry.CONTROL_MATRIX.get()))
                amount++;
            if (level.getBlockState(pos.below().east(2).south(2)).is(AWRegistry.CONTROL_MATRIX.get()))
                amount++;

            FluidTank tank = target.getTank();
            FluidStack fs = new FluidStack(AWRegistry.AETHERIUM_GAS_IMPURE.FLUID.get(), amount);
            if (tank.fill(fs, IFluidHandler.FluidAction.SIMULATE) >= fs.getAmount())
            {
                tank.fill(fs, IFluidHandler.FluidAction.EXECUTE);
                target.setChanged();
            }
            level.sendParticles(GLOW_WORK,
                    pos.getX() + 0.5f,
                    pos.getY() - 1f,
                    pos.getZ() + 0.5f,
                    128, 0, 0.625f, 0, -0.001f);
        }
    }

    private boolean checkWorkConditions(Level level, BlockPos pos)
    {
        if (!level.isNight())
            return false;

        return (level.canSeeSky(pos.above())
                && level.canSeeSky(pos.north(3).above())
                && level.canSeeSky(pos.south(3).above())
                && level.canSeeSky(pos.east(3).above())
                && level.canSeeSky(pos.west(3).above()));
    }
    private boolean checkStructure(Level level, BlockPos pos)
    {
        return
                // Supports
                level.getBlockState(pos.below()).is(AWRegistry.PRISM_SUPPORT.get()) &&  level.getBlockState(pos.below(2)).is(AWRegistry.PRISM_SUPPORT.get()) &&

                        // Tank
                        level.getBlockState(pos.below(3)).is(RegistryManager.FLUID_VESSEL.get()) &&

                        // Air around
                        level.getBlockState(pos.west()).isAir() && level.getBlockState(pos.west(2)).isAir()
                        && level.getBlockState(pos.east()).isAir() && level.getBlockState(pos.east(2)).isAir()
                        && level.getBlockState(pos.north()).isAir() && level.getBlockState(pos.north(2)).isAir()
                        && level.getBlockState(pos.south()).isAir() && level.getBlockState(pos.south(2)).isAir() &&

                        // Amplifiers
                        level.getBlockState(pos.west(3)).is(AWRegistry.MOONLIGHT_AMPLIFIER.get()) && level.getBlockState(pos.west(3)).getValue(MoonlightAmplifierBlock.FACING) == Direction.EAST
                        && level.getBlockState(pos.east(3)).is(AWRegistry.MOONLIGHT_AMPLIFIER.get()) && level.getBlockState(pos.east(3)).getValue(MoonlightAmplifierBlock.FACING) == Direction.WEST
                        && level.getBlockState(pos.north(3)).is(AWRegistry.MOONLIGHT_AMPLIFIER.get()) && level.getBlockState(pos.north(3)).getValue(MoonlightAmplifierBlock.FACING) == Direction.SOUTH
                        && level.getBlockState(pos.south(3)).is(AWRegistry.MOONLIGHT_AMPLIFIER.get()) && level.getBlockState(pos.south(3)).getValue(MoonlightAmplifierBlock.FACING) == Direction.NORTH &&

                        this.checkStructureBase(level, pos);
    }

    private boolean checkStructureBase(Level level, BlockPos pos)
    {
        for (Direction dir : Direction.Plane.HORIZONTAL)
        {
            // Pillar
            for (int dy = 0; dy < 3; ++dy)
            {
                BlockPos offset = pos.relative(dir, 3).below(dy + 1);
                Block test = dy == 0 ? RegistryManager.ARCHAIC_EDGE.get() : RegistryManager.ARCHAIC_BRICKS.get();
                if (!level.getBlockState(offset).is(test))
                {
                    return false;
                }
            }
        }

        for (int dy = 0; dy < 2; ++dy)
        {
            Block test = dy == 0 ? RegistryManager.ARCHAIC_EDGE.get() : RegistryManager.ARCHAIC_BRICKS.get();
            for (int i = 0; i < 4; ++i)
            {
                BlockPos offset = pos.relative(i / 2 == 0 ? Direction.WEST : Direction.EAST, 2).relative(i % 2 == 0 ? Direction.NORTH : Direction.SOUTH, 2);
                offset = offset.below(dy + 2);
                if (!level.getBlockState(offset).is(test))
                {
                    return false;
                }
            }
        }

        return true;
    }
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("work", canWork());
        tag.putBoolean("valid", isStructureValid());
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        // Will get tag from #getUpdateTag
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void loadAdditional(@Nonnull CompoundTag pTag, HolderLookup.Provider registries) {
        super.loadAdditional(pTag, registries);
        if (pTag.contains("work"))
            canWork = pTag.getBoolean("work");
        if (pTag.contains("valid"))
            isStructureValid = pTag.getBoolean("valid");
    }

    
    public AABB getRenderBoundingBox() {
        return AABB.encapsulatingFullBlocks(this.getBlockPos().below(3).north(4).east(4),
                this.getBlockPos().south(4).west(4));
    }
}
