package net.sirplop.aetherworks.augment;

import com.rekindled.embers.api.augment.AugmentUtil;
import com.rekindled.embers.augment.AugmentBase;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.UUID;

public class AetherPlatingAugment extends AugmentBase {
    // 1.21: attribute modifiers are identified by ResourceLocation
    private static final ResourceLocation ARMOR = ResourceLocation.fromNamespaceAndPath("aetherworks", "aetherplate.armor");
    private static final ResourceLocation TOUGHNESS = ResourceLocation.fromNamespaceAndPath("aetherworks", "aetherplate.toughness");

    public AetherPlatingAugment(ResourceLocation name) {
        super(name, 0.0);
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void armorChangedEvent(LivingEquipmentChangeEvent event) {
        EquipmentSlot slotChanged = event.getSlot();
        if (slotChanged.getFilterFlag() < 1 || slotChanged.getFilterFlag() > 4) // Check out the class EquipmentSlot
            return; //limits this to the 4 armor slots.

        ItemStack previousArmorPiece = event.getFrom();
        ItemStack newArmorPiece = event.getTo();
        LivingEntity ent = event.getEntity();

        int oldLevel = AugmentUtil.getAugmentLevel(previousArmorPiece, this);
        int newLevel = AugmentUtil.getAugmentLevel(newArmorPiece, this);

        if (oldLevel == newLevel)
            return; //no point in updating if nothing has changed.

        int currentTotal = AugmentUtil.getArmorAugmentLevel(ent, this);
        AttributeInstance armor = ent.getAttribute(Attributes.ARMOR);
        if (armor != null) {
            armor.removeModifier(ARMOR);
            armor.addPermanentModifier(new AttributeModifier(ARMOR, currentTotal * 0.1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        AttributeInstance toughness = ent.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (toughness != null) {
            toughness.removeModifier(TOUGHNESS);
            toughness.addPermanentModifier(new AttributeModifier(TOUGHNESS, currentTotal * 0.1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }
}
