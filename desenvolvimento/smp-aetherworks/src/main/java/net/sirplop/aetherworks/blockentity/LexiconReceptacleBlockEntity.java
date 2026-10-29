package net.sirplop.aetherworks.blockentity;
import net.minecraft.core.HolderLookup;

import com.rekindled.embers.Embers;
import com.rekindled.embers.api.tile.IExtraCapabilityInformation;
import com.rekindled.embers.particle.GlowParticleOptions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import com.rekindled.embers.compat.legacy.capabilities.Capability;
import com.rekindled.embers.compat.legacy.capabilities.ForgeCapabilities;
import com.rekindled.embers.compat.legacy.LazyOptional;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.sirplop.aetherworks.AWRegistry;
import net.sirplop.aetherworks.Aetherworks;
import net.sirplop.aetherworks.item.Lexicon;
import net.sirplop.aetherworks.util.Utils;
import javax.annotation.Nonnull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class LexiconReceptacleBlockEntity extends BlockEntity implements IExtraCapabilityInformation {

    public LexiconHandler inventory;
    public final LazyOptional<IItemHandler> lazyStorage;
    public final LazyOptional<IItemHandler> lazyLexicon;

    public LexiconReceptacleBlockEntity(BlockPos pos, BlockState state) {
        super(AWRegistry.LEXICON_RECEPTACLE_BLOCK_ENTITY.get(), pos, state);

        this.inventory = new LexiconHandler(Integer.MAX_VALUE);
        this.lazyStorage = LazyOptional.of(() -> inventory);
        this.lazyLexicon = LazyOptional.of(() -> lexiconInventory);
    }

    public static final GlowParticleOptions EMBER = new GlowParticleOptions(Utils.AETHERIUM_COLOR, 0.5f, 30);
    private byte particleCycle = 0;
    @OnlyIn(Dist.CLIENT)
    public static void clientTick(Level level, BlockPos pos, BlockState state, LexiconReceptacleBlockEntity blockEntity) {
        if (!level.isClientSide() || blockEntity.lexiconInventory.getStackInSlot(0).isEmpty())
            return;
        blockEntity.particleCycle += 1;
        if (blockEntity.particleCycle % 3 != 0)
            return;

        float sX = pos.getX() + 0.3F + level.random.nextFloat() * 0.4F;
        float sY = pos.getY() + 0.6F + level.random.nextFloat() * 0.2F;
        float sZ = pos.getZ() + 0.3F + level.random.nextFloat() * 0.4F;
        float speedUp = level.random.nextFloat() * 0.2F;
        float speedX = level.random.nextFloat() * 0.05F - 0.025f;
        float speedZ = level.random.nextFloat() * 0.05F - 0.025f;
        level.addParticle(EMBER, sX, sY, sZ, speedX, speedUp, speedZ);
    }

    @Override
    public void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);
        lexiconInventory.deserializeNBT(registries, nbt.getCompound("inventory"));
    }
    @Override
    public void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
        nbt.put("inventory", lexiconInventory.serializeNBT(registries));
    }

    @Override
    public @Nonnull CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag nbt = super.getUpdateTag(registries);
        nbt.put("inventory", lexiconInventory.serializeNBT(registries));
        return nbt;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Nonnull
    
    public <U> LazyOptional<U> getCapability(@Nonnull Capability<U> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            if (side == Direction.UP)
                return lazyLexicon.cast();
            return lazyStorage.cast();
        }
        return LazyOptional.empty();
    }
    @Override
    public boolean hasCapabilityDescription(Capability<?> capability) {
        return capability == ForgeCapabilities.ITEM_HANDLER;
    }

    @Override
    public void addCapabilityDescription(List<Component> strings, Capability<?> capability, Direction facing) {
        if (capability == ForgeCapabilities.ITEM_HANDLER)
        {
            if (facing == Direction.UP)
                strings.add(IExtraCapabilityInformation.formatCapability(EnumIOType.BOTH, Aetherworks.MODID + ".tooltip.goggles.item.lexicon", null));
            else
                strings.add(IExtraCapabilityInformation.formatCapability(EnumIOType.BOTH, Embers.MODID + ".tooltip.goggles.item", null));
        }
    }

    @Override
    public void addOtherDescription(List<Component> strings, Direction facing) {
        if (!lexiconInventory.getStackInSlot(0).isEmpty()) {
            ItemStack lexiconStack = lexiconInventory.getStackInSlot(0);
            strings.add(Lexicon.getStoredItem(lexiconStack).getDisplayName().copy().withStyle(ChatFormatting.GOLD));
        }
    }

    
    public void invalidateCaps() {
        lazyLexicon.invalidate();
        lazyStorage.invalidate();
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level instanceof ServerLevel)
            ((ServerLevel) level).getChunkSource().blockChanged(worldPosition);
    }

    public ItemStackHandler lexiconInventory = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            LexiconReceptacleBlockEntity.this.setChanged();
        }

        @Override
        public int getSlotLimit(int slot)
        {
            return 1;
        }
    };

    //mostly an edited version of Industrial Foregoing's Black Hole Unit.
    //performance might be bad, investigate lazy stored count getters, maybe?
    public class LexiconHandler extends ItemStackHandler implements IItemHandler {

        private final int amount;

        public LexiconHandler(int amount) {
            this.amount = amount;
        }

        @Override
        public int getSlots() {
            return 1;
        }

        @Nonnull
        @Override
        public ItemStack getStackInSlot(int slot) {
            ItemStack lexiconStack = lexiconInventory.getStackInSlot(0);
            if (lexiconStack.isEmpty())
                return ItemStack.EMPTY;
            ItemStack copied = Lexicon.getStoredItem(lexiconStack);
            copied.setCount(Lexicon.getStoredItemCount(lexiconStack));
            return copied;
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            if (isItemValid(slot, stack)) {
                ItemStack lexiconStack = lexiconInventory.getStackInSlot(0);
                int inserted = Math.min(this.amount - Lexicon.getStoredItemCount(lexiconStack), stack.getCount());
                if (!simulate) {
                    Lexicon.setStoredAmount(lexiconStack, Math.min(Lexicon.getStoredItemCount(lexiconStack) + inserted, amount));
                }
                if (inserted == stack.getCount()) return ItemStack.EMPTY;
                return stack.copyWithCount(stack.getCount() - inserted);
            }
            return stack;
        }

        @Nonnull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (amount == 0) return ItemStack.EMPTY;

            ItemStack lexiconStack = lexiconInventory.getStackInSlot(0);
            if (lexiconStack.isEmpty()) return ItemStack.EMPTY;
            int stored = Lexicon.getStoredItemCount(lexiconStack);
            ItemStack out = Lexicon.getStoredItem(lexiconStack);
            if (stored <= amount) {
                if (!simulate) {
                    Lexicon.setStoredAmount(lexiconStack, 0);
                }
                out.setCount(stored);
                return out;
            } else {
                if (!simulate) {
                    Lexicon.setStoredAmount(lexiconStack, stored - amount);
                }
                return out.copyWithCount(amount);
            }
        }

        @Override
        public int getSlotLimit(int slot) {
            return amount;
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            if (slot == 0) {
                ItemStack lexiconStack = lexiconInventory.getStackInSlot(0);
                return ItemStack.isSameItemSameComponents(Lexicon.getStoredItem(lexiconStack), stack);
            }
            return false;
        }
    }
}