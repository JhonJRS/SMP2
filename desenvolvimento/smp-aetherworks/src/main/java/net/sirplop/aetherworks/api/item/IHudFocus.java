package net.sirplop.aetherworks.api.item;

import net.minecraft.world.item.ItemStack;

public interface IHudFocus {
    boolean showAmount();
    ItemStack getFocus(ItemStack stack);
}
