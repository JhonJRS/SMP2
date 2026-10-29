package net.sirplop.aetherworks.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import javax.annotation.Nullable;

import java.util.List;

public class TooltipItem extends Item {
    protected final String tooltipLocation;
    protected final ChatFormatting format;
    protected final boolean foil;
    public TooltipItem(Properties pProperties, String tooltipLocation, ChatFormatting format, boolean foil) {
        super(pProperties);
        this.tooltipLocation = tooltipLocation;
        this.format = format;
        this.foil = foil;
    }
    public TooltipItem(Properties pProperties, String tooltipLocation, boolean foil) {
        super(pProperties);
        this.tooltipLocation = tooltipLocation;
        this.format = ChatFormatting.GRAY;
        this.foil = foil;
    }
    @Override
    public void appendHoverText(ItemStack pStack, Item.TooltipContext pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        pTooltipComponents.add(Component.translatable(tooltipLocation).withStyle(format));
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
    }
    @Override
    public boolean isFoil(ItemStack pStack) {
        return foil;
    }
}
