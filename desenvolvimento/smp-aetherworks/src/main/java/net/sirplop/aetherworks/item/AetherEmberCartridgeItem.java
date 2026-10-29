package net.sirplop.aetherworks.item;
import com.rekindled.embers.util.CapabilityCompat;

import com.rekindled.embers.api.capabilities.EmbersCapabilities;
import com.rekindled.embers.api.item.IHeldEmberCell;
import com.rekindled.embers.item.EmberStorageItem;
import com.rekindled.embers.power.DefaultEmberItemCapability;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import com.rekindled.embers.compat.legacy.capabilities.ICapabilityProvider;
import net.sirplop.aetherworks.AWConfig;
import net.sirplop.aetherworks.util.MoonlightRepair;

import javax.annotation.Nullable;

public class AetherEmberCartridgeItem extends EmberStorageItem {

    public static final double CAPACITY = 4500.0;

    public AetherEmberCartridgeItem(Properties properties) {
        super(properties);
    }

    @Override
    public double getCapacity() {
        return CAPACITY;
    }

    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        var cap = CapabilityCompat.getCapability(stack, EmbersCapabilities.EMBER_CAPABILITY, null);
        if (cap.isPresent())
            MoonlightRepair.tryFillWithEmber(cap.resolveOptional().get(), world, entity, AWConfig.MOONSNARE_STRENGTH.get());
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new EmberCartridgeCapability(stack, getCapacity());
    }

    public static class EmberCartridgeCapability extends DefaultEmberItemCapability implements IHeldEmberCell {
        public EmberCartridgeCapability(ItemStack stack, double capacity) {
            super(stack, capacity);
        }
    }
}