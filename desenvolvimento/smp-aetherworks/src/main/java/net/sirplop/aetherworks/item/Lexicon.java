package net.sirplop.aetherworks.item;
import net.sirplop.aetherworks.util.Compat121;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class Lexicon extends Item {

    public Lexicon(Properties pProperties) {
        super(pProperties);
    }
    public static final String LEXICON_ITEM = "lexicon_item";
    public static final String LEXICON_AMOUNT = "lexicon_amount";

    public static ItemStack getStoredItem(ItemStack stack) {
        if (Compat121.tag(stack).contains(Lexicon.LEXICON_ITEM))
            return Compat121.stackOf(Compat121.tag(stack).getCompound(LEXICON_ITEM));
        return ItemStack.EMPTY;
    }
    public static int getStoredItemCount(ItemStack stack) {
        if (Compat121.tag(stack).contains(Lexicon.LEXICON_AMOUNT))
            return Compat121.tag(stack).getInt(LEXICON_AMOUNT);
        return 0;
    }
    public static void setStoredItem(ItemStack lexicon, ItemStack toSet, int count) {
        Compat121.edit(lexicon, tag -> tag.put(LEXICON_ITEM, Compat121.save(toSet)));
        Compat121.edit(lexicon, tag -> tag.putInt(LEXICON_AMOUNT, count));
    }
    public static int setStoredAmount(ItemStack lexicon, int count) {
        int val = Math.max(0, count);
        Compat121.edit(lexicon, tag -> tag.putInt(LEXICON_AMOUNT, val));
        return val;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext level, List<Component> tooltip, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltip, isAdvanced);
        ItemStack stored = getStoredItem(stack);
        if (stored.isEmpty())
            return;
        tooltip.add(stored.getDisplayName().copy().withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  x" + getStoredItemCount(stack)).withStyle(ChatFormatting.GRAY));
    }
}
