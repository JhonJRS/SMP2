package net.sirplop.aetherworks.util;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;
import net.sirplop.aetherworks.datagen.AWItemTags;

// SMP 2 port: 1.21 has no TierSortingRegistry. Aetherium mines everything (its "incorrect" tag is the netherite one);
// lower tools are kept off aetherium blocks by the data tags incorrect_for_*_tool, which include
// #aetherworks:needs_aetherium_tool (see src/main/resources/data/minecraft/tags/block).
public class AetheriumTiers {
    public static final Tier AETHERIUM = new SimpleTier(
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 3841, 9.5f, 2f, 18,
            () -> Ingredient.of(AWItemTags.AETHERIUM_INGOT));
}
