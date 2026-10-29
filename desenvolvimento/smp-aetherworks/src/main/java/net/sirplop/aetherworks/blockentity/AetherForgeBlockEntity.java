package net.sirplop.aetherworks.blockentity;
import com.rekindled.embers.util.CapabilityCompat;
import net.minecraft.core.HolderLookup;

import com.rekindled.embers.api.capabilities.EmbersCapabilities;
import com.rekindled.embers.api.event.DialInformationEvent;
import com.rekindled.embers.api.power.IEmberCapability;
import com.rekindled.embers.api.tile.IExtraDialInformation;
import com.rekindled.embers.api.upgrades.UpgradeUtil;
import com.rekindled.embers.block.MechEdgeBlockBase;
import com.rekindled.embers.blockentity.FluidVesselBlockEntity;
import com.rekindled.embers.particle.GlowParticleOptions;
import com.rekindled.embers.power.DefaultEmberCapability;
import com.rekindled.embers.util.EmbersColors;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import com.rekindled.embers.compat.legacy.capabilities.Capability;
import com.rekindled.embers.compat.legacy.capabilities.ForgeCapabilities;
import com.rekindled.embers.compat.legacy.LazyOptional;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.sirplop.aetherworks.AWRegistry;
import net.sirplop.aetherworks.api.capabilities.IHeatCapability;
import net.sirplop.aetherworks.capabilities.AWCapabilities;
import net.sirplop.aetherworks.datagen.AWSounds;
import net.sirplop.aetherworks.power.DefaultHeatCapability;
import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

public class AetherForgeBlockEntity extends BlockEntity implements IForge, IExtraDialInformation, com.rekindled.embers.compat.legacy.capabilities.ICapabilityProvider {
    public AetherForgeBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(AWRegistry.AETHER_FORGE_BLOCK_ENTITY.get(), pPos, pBlockState);
        emberCapability.setEmberCapacity(5000);
        emberCapability.setEmber(0);
        heatCapability.setHeat(0);
        heatCapability.setHeatCapacity(3000);
    }

    private final List<IFluidHandler> fluidHandlers = new ArrayList<>();
    private final List<IForgePart> parts = new ArrayList<>();
    private double storedHeat;
    private int ticksInDanger = 0;
    public int ticksExisted = -1;

    public int renderFrame = 0;
    public float renderTime = 0;

    public IEmberCapability emberCapability = new DefaultEmberCapability() {
        @Override
        public void onContentsChanged() {
            super.onContentsChanged();
            AetherForgeBlockEntity.this.setChanged();
            if (level.getBlockEntity(worldPosition.above()) instanceof AetherForgeTopBlockEntity ent)
                ent.setChanged();
        }
    };

    public IHeatCapability heatCapability = new DefaultHeatCapability() {
        @Override
        public void onContentsChanged() {
            super.onContentsChanged();
            AetherForgeBlockEntity.this.setChanged();
            if (level.getBlockEntity(worldPosition.above()) instanceof AetherForgeTopBlockEntity ent)
                ent.setChanged();
        }
    };

    @Override
    public AetherForgeBlockEntity getOwner() {
        return this;
    }

    @Override
    public IHeatCapability getHeatCapability() {
        return heatCapability;
    }

    @Override
    public IEmberCapability getEmberCapability() {
        return emberCapability;
    }

    @Override
    public void transferHeat(float value, boolean immediate) {
        if (!immediate)
        {
            this.storedHeat += value;
        }
        else
        {
            this.heatCapability.setHeat(this.heatCapability.getHeat() + value);
        }
    }

    @Override
    public boolean canFunction() {
        return false;
    }

    @Override
    public List<IFluidHandler> getAttachedFluidHandlers() {
        return fluidHandlers;
    }
    @Override
    public List<IForgePart> getParts() {
        return parts;
    }

    private int groanTick = 0;
    public static final GlowParticleOptions EMBER = new GlowParticleOptions(EmbersColors.EMBER, 2f, 40);
    @OnlyIn(Dist.CLIENT)
    public static void clientTick(Level level, BlockPos pos, BlockState state, AetherForgeBlockEntity blockEntity) {
        if (!level.isClientSide())
            return;
        if (++blockEntity.ticksExisted % 5 == 0)
        { //the client needs to populate the list of parts themselves.
            blockEntity.populateStructure();
        }

        for (IForgePart part : blockEntity.parts) {
            if (part != null && !part.isInvalid())
                part.onForgeTick(blockEntity);
        }

        double current = blockEntity.heatCapability.getHeat();
        double max = blockEntity.heatCapability.getHeatCapacity();
        for (int i = 0; i <= current / max * 10; i++)
        {
            float sX = pos.getX() + 0.4F + level.random.nextFloat() * 0.2F;
            float sY = pos.getY() + 1 + 0.2F + level.random.nextFloat() * 0.2F;
            float sZ = pos.getZ() + 0.4F + level.random.nextFloat() * 0.2F;
            float speedUp = level.random.nextFloat() * 0.2F;
            level.addParticle(EMBER, sX, sY, sZ, 0, speedUp, 0);
        }

        if (current >= 2100)
        {
            float sX = pos.getX() - 1 + level.random.nextFloat() * 3;
            float sY = pos.getY() + 1;
            float sZ = pos.getZ() - 1 + level.random.nextFloat() * 3;
            float speedUp = level.random.nextFloat() * 0.02F;
            level.addParticle(EMBER, sX, sY, sZ, 0, speedUp, 0);
        }

        if (current >= 2800)
        {
            float sX = pos.getX() - 1 + level.random.nextFloat() * 3;
            float sY = pos.getY() + 1;
            float sZ = pos.getZ() - 1 + level.random.nextFloat() * 3;
            float speedUp = level.random.nextFloat() * 0.02F;
            level.addParticle(ParticleTypes.SMOKE, sX, sY, sZ, 0, speedUp, 0);

            if (blockEntity.groanTick++ % 200 == 0) { //every ten seconds
                level.playSound(Minecraft.getInstance().player, blockEntity.getBlockPos(), AWSounds.FORGE_GROAN.get(),
                        SoundSource.BLOCKS, 1.5f, 0.75f + level.random.nextFloat() * 0.5f);
            }
        } else {
            blockEntity.groanTick = 160;
        }

        if (current >= 2900)
        {
            float sX = pos.getX() - 1 + level.random.nextFloat() * 3;
            float sY = pos.getY() + 1;
            float sZ = pos.getZ() - 1 + level.random.nextFloat() * 3;
            float speedUp = level.random.nextFloat() * 0.02F;
            level.addParticle(ParticleTypes.FLAME, sX, sY, sZ, 0, speedUp, 0);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AetherForgeBlockEntity blockEntity) {
        if (level.isClientSide())
            return;
        if (++blockEntity.ticksExisted % 5 == 0)
        {
            blockEntity.populateStructure();
        }

        for (IForgePart part : blockEntity.parts) {
            if (part != null && !part.isInvalid())
                part.onForgeTick(blockEntity);
        }

        if (blockEntity.storedHeat > 0) {
            double added = Math.max(0.1F, blockEntity.storedHeat * 0.35F);
            added *= Math.max(0.01F, blockEntity.getHeatCapability().getHeat() / blockEntity.heatCapability.getHeatCapacity());
            blockEntity.heatCapability.addAmount(added, true);
            blockEntity.storedHeat -= added;
        }
        if (blockEntity.storedHeat <= 0)
        {
            double removed = Math.max(0.1F, blockEntity.getHeatCapability().getHeat() / blockEntity.heatCapability.getHeatCapacity());
            if (blockEntity.getHeatCapability().getHeat() > removed)
            {
                blockEntity.heatCapability.removeAmount(removed, true);
            }
        }

        level.sendBlockUpdated(pos, state, blockEntity.getBlockState(), 1 | 2);

        if (blockEntity.heatCapability.getHeat() >= blockEntity.heatCapability.getHeatCapacity())
        {
            if (++blockEntity.ticksInDanger >= 60)
            {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 1 | 2);
                level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 1 | 2);
                for (MechEdgeBlockBase.MechEdge edge : MechEdgeBlockBase.MechEdge.values())
                {
                    level.setBlock(pos.subtract(edge.centerPos), Blocks.AIR.defaultBlockState(), 1 | 2);
                }

                level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 10, true, Level.ExplosionInteraction.TNT);
            }
        }
        else
        {
            blockEntity.ticksInDanger = Math.max(0, --blockEntity.ticksInDanger);
        }
    }

    private void populateStructure()
    {
        BlockPos pos = this.worldPosition.above();
        this.fluidHandlers.clear();
        this.parts.clear();
        for (MechEdgeBlockBase.MechEdge edge : MechEdgeBlockBase.MechEdge.values())
        { //scan the top of the forge for stuff
            BlockPos offset = pos.subtract(edge.centerPos);
            BlockEntity entity = getLevel().getBlockEntity(offset);
            if (entity instanceof IForgePart part && part.isTopPart()) {
                parts.add(part);
            }
        }

        pos = pos.below();
        for (MechEdgeBlockBase.MechEdge edge : MechEdgeBlockBase.MechEdge.values()) { //scan the sides for upgrades and set connections appropriately
            BlockPos block = pos.subtract(edge.centerPos);
            BlockState forgeState = getLevel().getBlockState(block);
            if (forgeState.isAir())
                break; //we're breaking the forge, kill it!
            if (!forgeState.getBlock().equals(AWRegistry.AETHER_FORGE_EDGE.get()))
                continue; //this isn't a forge block, which means something strange is happening here! (probably placed it on snow or something)
            if (edge.corner) {
                boolean change = false;
                BooleanProperty xState = edge.centerPos.getX() < 0 ? BlockStateProperties.EAST : BlockStateProperties.WEST;
                BooleanProperty zState = edge.centerPos.getZ() < 0 ? BlockStateProperties.SOUTH : BlockStateProperties.NORTH;

                if (forgeState.getValue(xState) != checkAndAdd(block.offset(-edge.centerPos.getX(), 0, 0))) {
                    forgeState = forgeState.setValue(xState, !forgeState.getValue(xState));
                    change = true;
                }
                if (forgeState.getValue(zState) != checkAndAdd(block.offset(0, 0, -edge.centerPos.getZ()))) {
                    forgeState = forgeState.setValue(zState, !forgeState.getValue(zState));
                    change = true;
                }
                if (change)
                    level.setBlock(block, forgeState, 16);
            } else {
                BooleanProperty state;
                if (edge.centerPos.getX() != 0) {
                    state = edge.centerPos.getX() < 0 ? BlockStateProperties.EAST : BlockStateProperties.WEST;
                } else {
                    state = edge.centerPos.getZ() < 0 ? BlockStateProperties.SOUTH : BlockStateProperties.NORTH;
                }
                if (forgeState.getValue(state) != checkAndAdd(block.subtract(edge.centerPos))) {
                    forgeState = forgeState.setValue(state, !forgeState.getValue(state));
                    level.setBlock(block, forgeState, 16);
                }
            }
        }
    }

    private boolean checkAndAdd(BlockPos check) {
        BlockEntity entity = getLevel().getBlockEntity(check);
        if (entity instanceof IForgePart part && !part.isTopPart()) {
            parts.add(part);
            return true;
        }
        else if (entity instanceof FluidVesselBlockEntity vessel) {
            this.fluidHandlers.add(CapabilityCompat.getCapability(vessel, ForgeCapabilities.FLUID_HANDLER).resolveOptional().get());
            return true;
        }

        return false;
    }

    @Override
    public @Nonnull CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag nbt = super.getUpdateTag(registries);
        emberCapability.writeToNBT(nbt);
        heatCapability.writeToNBT(nbt);
        return nbt;
    }
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        // Will get tag from #getUpdateTag
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);
        emberCapability.deserializeNBT(nbt);
        heatCapability.deserializeNBT(nbt);
        if (nbt.contains("storedheat"))
            storedHeat = nbt.getDouble("storedheat");
        if (nbt.contains("danger"))
            ticksInDanger = nbt.getInt("danger");
    }

    @Override
    public void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
        emberCapability.writeToNBT(nbt);
        heatCapability.writeToNBT(nbt);
        nbt.putDouble("storedheat", storedHeat);
        nbt.putInt("danger", ticksInDanger);
    }

    
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (!this.remove) {
            if (cap == EmbersCapabilities.EMBER_CAPABILITY) {
                return CapabilityCompat.getCapability(emberCapability, cap, side);
            }
            else if (cap == AWCapabilities.HEAT_CAPABILITY) {
                return CapabilityCompat.getCapability(heatCapability, cap, side);
            }
        }
        return LazyOptional.empty();
    }

    
    public void invalidateCaps() {
        emberCapability.invalidate();
        heatCapability.invalidate();
    }

    
    public AABB getRenderBoundingBox() {
        return AABB.encapsulatingFullBlocks(this.getBlockPos().above(1).north(2).east(2),
                this.getBlockPos().south(2).west(2));
    }

    @Override
    public void addDialInformation(Direction facing, List<Component> information, String dialType) {
        UpgradeUtil.throwEvent(this, new DialInformationEvent(this, information, dialType), List.of());
    }
}
