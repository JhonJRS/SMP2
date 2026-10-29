package net.sirplop.aetherworks.blockentity;
import net.minecraft.core.HolderLookup;

import com.rekindled.embers.Embers;
import com.rekindled.embers.api.tile.IExtraCapabilityInformation;
import com.rekindled.embers.particle.GlowParticleOptions;
import com.rekindled.embers.particle.SmokeParticleOptions;
import com.rekindled.embers.particle.SparkParticleOptions;
import com.rekindled.embers.util.EmbersColors;
import com.rekindled.embers.util.Misc;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.rekindled.embers.compat.legacy.capabilities.Capability;
import com.rekindled.embers.compat.legacy.capabilities.ForgeCapabilities;
import com.rekindled.embers.compat.legacy.LazyOptional;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.sirplop.aetherworks.recipe.RecipeWrapper;
import net.sirplop.aetherworks.AWConfig;
import net.sirplop.aetherworks.AWRegistry;
import net.sirplop.aetherworks.recipe.IToolStationRecipe;
import net.sirplop.aetherworks.util.Utils;
import org.joml.Random;

import java.util.List;

public class ToolStationBlockEntity extends BlockEntity implements IForgePart, IExtraCapabilityInformation, com.rekindled.embers.compat.legacy.capabilities.ICapabilityProvider {

    public ToolStationBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(AWRegistry.TOOL_STATION_BLOCK_ENTITY.get(), pPos, pBlockState);
    }
    int progress = 0;
    int prevProgress = -1;
    public IToolStationRecipe cachedRecipe;
    public boolean hasEmber = false;
    public boolean hasHeat = false;

    public void setHasEmber(boolean set) {
        if (set != hasEmber) {
            hasEmber = set;
            setChanged();
        }
    }
    public void setHasHeat(boolean set) {
        if (set != hasHeat) {
            hasHeat= set;
            setChanged();
        }
    }

    static Random random = new Random();

    public int getProgress() { return progress; }

    public ItemStackHandler inventory = new ItemStackHandler(6) {
        @Override
        protected void onContentsChanged(int slot) {
            RecipeWrapper context = new RecipeWrapper(inventory);
            cachedRecipe = Misc.getRecipe(cachedRecipe, AWRegistry.TOOL_STATION_RECIPE.get(), context, level);
            progress = 0;
            ToolStationBlockEntity.this.setChanged();
        }

        @Override
        protected int getStackLimit(int slot, ItemStack stack) {
            return 1;
        }
    };
    @Override
    public void setChanged() {
        super.setChanged();
        if (level instanceof ServerLevel)
            ((ServerLevel) level).getChunkSource().blockChanged(worldPosition);
    }
    public LazyOptional<IItemHandler> holder = LazyOptional.of(() -> inventory);
    @Override
    public void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);
        inventory.deserializeNBT(registries, nbt.getCompound("inventory"));
        progress = nbt.getInt("progress");
        hasEmber = nbt.getBoolean("has_ember");
        hasHeat = nbt.getBoolean("has_heat");
    }

    @Override
    public void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
        nbt.put("inventory", inventory.serializeNBT(registries));
        nbt.putInt("progress", progress);
        nbt.putBoolean("has_ember", hasEmber);
        nbt.putBoolean("has_heat", hasHeat);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag nbt = super.getUpdateTag(registries);
        nbt.put("inventory", inventory.serializeNBT(registries));
        nbt.putInt("progress", progress);
        nbt.putBoolean("has_ember", hasEmber);
        nbt.putBoolean("has_heat", hasHeat);
        return nbt;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (!this.remove && cap == ForgeCapabilities.ITEM_HANDLER) {
            return ForgeCapabilities.ITEM_HANDLER.orEmpty(cap, holder);
        }
        return LazyOptional.empty();
    }

    @Override
    public void addCapabilityDescription(List<Component> strings, Capability<?> capability, Direction facing) {
        if (capability == ForgeCapabilities.ITEM_HANDLER)
            strings.add(IExtraCapabilityInformation.formatCapability(EnumIOType.BOTH, Embers.MODID + ".tooltip.goggles.item", null));
    }

    @Override
    public boolean hasCapabilityDescription(Capability<?> capability) {
        return capability == ForgeCapabilities.ITEM_HANDLER;
    }

    
    public void invalidateCaps() {
        holder.invalidate();
    }

    @Override
    public void onForgeTick(IForge forge) {
        if (!level.isClientSide) {
            if (cachedRecipe == null) {
                progress = 0;
                return;
            }
            if (progress > prevProgress) {
                forge.getEmberCapability().removeAmount(cachedRecipe.getTemperatureRate() * 10 * (progress - prevProgress), true);
                forge.getHeatCapability().removeAmount(cachedRecipe.getTemperatureRate() * (progress - prevProgress), true);
            }
            prevProgress = progress;

            setHasEmber(forge.getEmberCapability().getEmber() > cachedRecipe.getTemperatureRate() * 10);
            setHasHeat(forge.getHeatCapability().getHeat() > cachedRecipe.getTemperature());
        }
        else if (cachedRecipe != null && hasEmber && hasHeat) { //client particles & checks
            if (!inventory.getStackInSlot(5).isEmpty()) {
                cachedRecipe = null;
                return;
            }
            final GlowParticleOptions glow = new GlowParticleOptions(Utils.multiLerp( progress / (float)AWConfig.FORGE_TOOL_STATION_MAX_HITS.get(),
                    EmbersColors.EMBER, Utils.AETHERIUM_COLOR), 1f, 30);
            BlockPos pos = getBlockPos();
            final float speed = 0.1f;
            level.addParticle(glow,
                    pos.getX() + level.random.nextFloat(),
                    pos.getY() + 0.2f,
                    pos.getZ() + level.random.nextFloat(),
                    (level.random.nextFloat() * speed) - (speed * 0.5f),
                    speed,
                    (level.random.nextFloat() * speed) - (speed * 0.5f));
        }
    }

    @Override
    public boolean isTopPart() {
        return true;
    }

    @Override
    public boolean isInvalid() {
        return (level != null ? level.getBlockEntity(getBlockPos()) : null) == null;
    }

    public boolean onHit() {
        RecipeWrapper context = new RecipeWrapper(inventory);
        cachedRecipe = Misc.getRecipe(cachedRecipe, AWRegistry.TOOL_STATION_RECIPE.get(), context, level);
        if (level.isClientSide())
            return false;
        if (cachedRecipe != null && hasEmber && hasHeat) {
            ++progress;
            level.playSound(null, worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.25f, 2.0f+random.nextFloat());
            if (progress > AWConfig.FORGE_TOOL_STATION_MAX_HITS.get())
            {
                progress = 0;

                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(new SparkParticleOptions(EmbersColors.EMBER, 1.0f), worldPosition.getX() + 0.5f, worldPosition.getY() + 1.0625f, worldPosition.getZ() + 0.5f, 10, 0.1, 0.0, 0.1, 1.0);
                    serverLevel.sendParticles(new SmokeParticleOptions(EmbersColors.SMOKE, 3.0f), worldPosition.getX() + 0.5f, worldPosition.getY() + 1.0625f, worldPosition.getZ() + 0.5f, 10, 0.1, 0.0, 0.1, 1.0);
                }
                level.playSound(null, worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.0f, 0.95f+random.nextFloat()*0.1f);
                inventory.setStackInSlot(5, cachedRecipe.assemble(context, level.registryAccess()));
                setChanged();
            } else if (level instanceof ServerLevel serverLevel) {
                setChanged();
                serverLevel.sendParticles(new SparkParticleOptions(EmbersColors.EMBER, 1.0f),
                        worldPosition.getX() + 0.3f + level.random.nextFloat() * 0.4f,
                        worldPosition.getY() + 0.125f,
                        worldPosition.getZ() + 0.3f + level.random.nextFloat() * 0.4f,
                        10, 0.1, 0.0, 0.1, 1.0);
            }
            return true;
        }
        return false;
    }
}
