package net.sirplop.aetherworks.item;
import net.minecraft.world.item.Item;
import net.sirplop.aetherworks.util.Compat121;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.rekindled.embers.Embers;
import com.rekindled.embers.EmbersClientEvents;
import com.rekindled.embers.util.Misc;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.sirplop.aetherworks.AWConfig;
import net.sirplop.aetherworks.Aetherworks;
import net.sirplop.aetherworks.api.item.IToggleItem;
import net.sirplop.aetherworks.model.AetherCrownModel;
import net.sirplop.aetherworks.util.MoonlightRepair;
import net.sirplop.aetherworks.util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class AetherCrownItem extends ArmorItem implements IToggleItem {
    public AetherCrownItem(net.minecraft.core.Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public byte getToggleMax() { return 2; }

    @Override
    public void clientModeChanged(ItemStack stack, Player player, byte oldValue, byte newValue, byte stateFlag) {
        player.getCommandSenderWorld().playSound(player, player.getOnPos(),
                SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS, 0.6f, 0.9F);
        MutableComponent message = null;
        if (newValue == 0) { //self
            message = Component.translatable(Aetherworks.MODID + ".tooltip.crown_target.self");
        } else if (newValue == 1) {
            message = Component.translatable(Aetherworks.MODID + ".tooltip.crown_target.friends");
        } else {
            message = Component.translatable(Aetherworks.MODID + ".tooltip.crown_target.hostiles");
        }
        if (!Utils.isFakePlayer(player))
            Minecraft.getInstance().gui.setOverlayMessage(message, false);
    }

    private int ticks = 0;
    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        if (world.isClientSide())
            return;
        MoonlightRepair.tryRepair(stack, world, entity, AWConfig.AETHERIC_STRENGTH.get());
        if (entity instanceof ArmorStand)
            return;
        //there's a bug in the slot index for inventoryTick - it resets every "compartment", so multiple slots end up sharing indices.
        boolean inRightSlot = false;
        for (ItemStack armor : entity instanceof LivingEntity living ? living.getArmorSlots() : List.<ItemStack>of()) {
            if (armor == stack) {
                inRightSlot = true;
                break;
            }
        }
        if (!inRightSlot)
            return;
        //try to apply potions once a second.
        if (++ticks < 20)
            return;
        ticks = 0;

        if (!hasAttachedGem(stack))
            return;

        int toggle = getToggled(stack);
        List<LivingEntity> targets;
        switch (toggle) {
            default -> { //self
                targets = new ArrayList<>();
                if (entity instanceof LivingEntity liv)
                    targets.add(liv);
            }
            case 1 -> { //self and friends
                targets = world.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(AWConfig.AETHER_CROWN_EFFECT_RADIUS.get()),
                        ent -> ent instanceof Animal || ent instanceof Player || ent instanceof Npc);
            }
            case 2 -> { //hostiles
                targets = world.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(AWConfig.AETHER_CROWN_EFFECT_RADIUS.get()),
                        ent -> ent instanceof Enemy);
            }
        }
        List<MobEffectInstance> effects = PotionGemItem.getEffects(getAttachedGem(stack));
        for (LivingEntity ent : targets) {
            if (!ent.isAffectedByPotions())
                continue;
            for (MobEffectInstance effect : effects) {
                ent.addEffect(new MobEffectInstance(effect), entity);
            }
        }
    }
    public static boolean hasAttachedGem(ItemStack holder) {
        return !getAttachedGem(holder).isEmpty();
    }

    public static void attachGem(ItemStack holder, ItemStack gem) {
        Compat121.edit(holder, tag -> tag.put("gem", Compat121.save(gem)));
    }

    public static ItemStack detachGem(ItemStack holder) {
        if (Compat121.tag(holder).contains("gem")) {
            ItemStack gem = Compat121.stackOf(Compat121.tag(holder).getCompound("gem"));
            Compat121.edit(holder, tag -> tag.remove("gem"));
            return gem;
        }
        return ItemStack.EMPTY;
    }

    public static ItemStack getAttachedGem(ItemStack holder) {
        if (Compat121.tag(holder).contains("gem")) {
            return Compat121.stackOf(Compat121.tag(holder).getCompound("gem"));
        }
        return ItemStack.EMPTY;
    }

    @Override
    public net.minecraft.resources.ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Aetherworks.MODID,
                layer.dyeable() ? "textures/models/armor/aether_crown_colorable.png" : "textures/models/armor/aether_crown.png");
    }

    @Override
    public net.minecraft.world.item.component.ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        if (isBroken(stack)) {
            return net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY;
        }
        return super.getDefaultAttributeModifiers(stack);
    }

    @Override
    public void setDamage(ItemStack stack, int damage) {
        super.setDamage(stack, Math.min(damage, getMaxDamage(stack) - 1));
    }

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<Item> onBroken) {
        return isBroken(stack) ? 0 : amount;
    }

    public boolean isBroken(ItemStack armor) {
        return armor.getDamageValue() >= armor.getMaxDamage() - 1;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext level, List<Component> tooltip, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltip, isAdvanced);
        tooltip.add(Component.translatable("aetherworks.tooltip.cycle_mode", Component.keybind("key.aetherworks.mode_change")).withStyle(ChatFormatting.GOLD));

        byte target = getToggled(stack);
        if (target == 0) { //self
            tooltip.add(Component.translatable(Aetherworks.MODID + ".tooltip.crown_target.self").withStyle(ChatFormatting.GRAY));
        } else if (target == 1) {
            tooltip.add(Component.translatable(Aetherworks.MODID + ".tooltip.crown_target.friends").withStyle(ChatFormatting.GRAY));
        } else if (target == 2) {
            tooltip.add(Component.translatable(Aetherworks.MODID + ".tooltip.crown_target.hostiles").withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable(Aetherworks.MODID + ".tooltip.crown_gem").withStyle(ChatFormatting.GRAY));
        if (Compat121.tag(stack).contains("gem")) {
            ItemStack gem = Compat121.stackOf(Compat121.tag(stack).getCompound("gem"));
            PotionGemItem.addTooltip(PotionGemItem.getEffects(gem), tooltip);
        } else
            tooltip.add(PotionGemItem.NO_EFFECT);
        if (isBroken(stack)) {
            tooltip.add(Component.translatable(Embers.MODID + ".tooltip.broken").withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(AetherCrownModel.ARMOR_MODEL_GETTER);
    }

    @OnlyIn(Dist.CLIENT)
    public static class ColorHandler implements ItemColor {
        @Override
        public int getColor(ItemStack itemStack, int i) {
            if (i == 1 && AetherCrownItem.hasAttachedGem(itemStack)) {
                return Compat121.tag(AetherCrownItem.getAttachedGem(itemStack)).getInt(PotionGemItem.POTION_COLOR);
            }
            else if (i == 0)
                return 0xFFFFFFFF;
            return 0x0021b2ff;
        }
    }
}