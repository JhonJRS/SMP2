package net.sirplop.aetherworks.datagen;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.sirplop.aetherworks.AWRegistry;
import net.sirplop.aetherworks.Aetherworks;
import net.sirplop.aetherworks.item.Lexicon;
import net.sirplop.aetherworks.item.PotionGemItem;

public class AWItemProperties {

    public static void register() {
        //copied from vanilla to mimic shield behaviour
        ItemProperties.register(AWRegistry.AETHER_SHIELD.get(), ResourceLocation.fromNamespaceAndPath(Aetherworks.MODID, "blocking"), (p_174575_, p_174576_, p_174577_, p_174578_) -> {
            return p_174577_ != null && p_174577_.isUsingItem() && p_174577_.getUseItem() == p_174575_ ? 1.0F : 0.0F;
        });

        ItemProperties.register(AWRegistry.POTION_GEM.get(), ResourceLocation.fromNamespaceAndPath(Aetherworks.MODID, "has_gem"), (stack, world, living, intIn) -> {
            if (stack.isEmpty())
                return 0;
            return PotionGemItem.getColor(stack) == PotionGemItem.DEFAULT_COLOR ? 0 : 1;
        });

        ItemProperties.register(AWRegistry.LEXICON.get(), ResourceLocation.fromNamespaceAndPath(Aetherworks.MODID, "lexicon_has_item"), (stack, world, living, intIn) -> {
            if (stack.isEmpty())
                return 0;
            return Lexicon.getStoredItem(stack) != ItemStack.EMPTY ? 1 : 0;
        });

        //Copied from vanilla to mimic normal crossbow
        ItemProperties.register(AWRegistry.CROSSBOW_QUARTZ.get(), ResourceLocation.fromNamespaceAndPath(Aetherworks.MODID, "pull"), (p_239427_0_, p_239427_1_, p_239427_2_, intIn) -> {
            if (p_239427_2_ == null) {
                return 0.0F;
            } else {
                return CrossbowItem.isCharged(p_239427_0_) ? 0.0F : (float)(p_239427_0_.getUseDuration(p_239427_2_) - p_239427_2_.getUseItemRemainingTicks()) / (float)CrossbowItem.getChargeDuration(p_239427_0_, p_239427_2_);
            }
        });
        ItemProperties.register(AWRegistry.CROSSBOW_QUARTZ.get(), ResourceLocation.fromNamespaceAndPath(Aetherworks.MODID, "pulling"), (p_239426_0_, p_239426_1_, p_239426_2_, intIn) -> {
            return p_239426_2_ != null && p_239426_2_.isUsingItem() && p_239426_2_.getUseItem() == p_239426_0_ && !CrossbowItem.isCharged(p_239426_0_) ? 1.0F : 0.0F;
        });
        ItemProperties.register(AWRegistry.CROSSBOW_QUARTZ.get(), ResourceLocation.fromNamespaceAndPath(Aetherworks.MODID, "charged"), (p_239425_0_, p_239425_1_, p_239425_2_, intIn) -> {
            return p_239425_2_ != null && CrossbowItem.isCharged(p_239425_0_) ? 1.0F : 0.0F;
        });
        //Copied from vanilla to mimic normal crossbow
        ItemProperties.register(AWRegistry.CROSSBOW_MAGMA.get(), ResourceLocation.fromNamespaceAndPath(Aetherworks.MODID, "pull"), (p_239427_0_, p_239427_1_, p_239427_2_, intIn) -> {
            if (p_239427_2_ == null) {
                return 0.0F;
            } else {
                return CrossbowItem.isCharged(p_239427_0_) ? 0.0F : (float)(p_239427_0_.getUseDuration(p_239427_2_) - p_239427_2_.getUseItemRemainingTicks()) / (float)CrossbowItem.getChargeDuration(p_239427_0_, p_239427_2_);
            }
        });
        ItemProperties.register(AWRegistry.CROSSBOW_MAGMA.get(), ResourceLocation.fromNamespaceAndPath(Aetherworks.MODID, "pulling"), (p_239426_0_, p_239426_1_, p_239426_2_, intIn) -> {
            return p_239426_2_ != null && p_239426_2_.isUsingItem() && p_239426_2_.getUseItem() == p_239426_0_ && !CrossbowItem.isCharged(p_239426_0_) ? 1.0F : 0.0F;
        });
        ItemProperties.register(AWRegistry.CROSSBOW_MAGMA.get(), ResourceLocation.fromNamespaceAndPath(Aetherworks.MODID, "charged"), (p_239425_0_, p_239425_1_, p_239425_2_, intIn) -> {
            return p_239425_2_ != null && CrossbowItem.isCharged(p_239425_0_) ? 1.0F : 0.0F;
        });
    }
}
