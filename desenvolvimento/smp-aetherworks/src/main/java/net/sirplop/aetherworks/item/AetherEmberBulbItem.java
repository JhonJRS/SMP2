package net.sirplop.aetherworks.item;
import com.rekindled.embers.util.CapabilityCompat;


import com.rekindled.embers.api.capabilities.EmbersCapabilities;
import com.rekindled.embers.item.EmberStorageItem;
import com.rekindled.embers.item.IEmbersCurioItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import com.rekindled.embers.compat.legacy.capabilities.ForgeCapabilities;
import net.sirplop.aetherworks.AWConfig;
import net.sirplop.aetherworks.Aetherworks;
import net.sirplop.aetherworks.util.MoonlightRepair;
import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import com.rekindled.embers.compat.legacy.capabilities.ICapabilityProvider;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

public class AetherEmberBulbItem extends EmberStorageItem implements IEmbersCurioItem {

    public static final double CAPACITY = 750.0;

    public AetherEmberBulbItem(Properties properties) {
        super(properties);
    }

    @Override
    public double getCapacity() {
        return CAPACITY;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        var cap = CapabilityCompat.getCapability(stack, EmbersCapabilities.EMBER_CAPABILITY, null);
        if (cap.isPresent())
            MoonlightRepair.tryFillWithEmber(cap.resolveOptional().get(), world, entity, AWConfig.MOONSNARE_STRENGTH.get());
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new AetherEmberJarItem.EmberJarCapability(stack, getCapacity());
    }

    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
        if (prevStack.getItem() != stack.getItem())
            this.playEquipSound(slotContext, false);
    }
    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        if (newStack.getItem() != stack.getItem())
            this.playEquipSound(slotContext, true);
    }
}