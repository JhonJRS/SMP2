package net.sirplop.aetherworks.recipe;
import net.minecraft.core.HolderLookup;

import com.mojang.datafixers.util.Either;
import com.rekindled.embers.recipe.FluidIngredient;
import net.minecraft.core.RegistryAccess;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.sirplop.aetherworks.recipe.RecipeWrapper;
import net.sirplop.aetherworks.AWRegistry;

import java.util.List;

public interface IAetheriumAnvilRecipe extends Recipe<AetheriumAnvilContext> {

    public ItemStack getOutput(RecipeWrapper context);

    @Override
    public default ItemStack getToastSymbol() {
        return new ItemStack(AWRegistry.FORGE_ANVIL.get());
    }

    @Override
    public default RecipeType<?> getType() {
        return AWRegistry.AETHERIUM_ANVIL.get();
    }

    @Override
    public default ItemStack getResultItem(HolderLookup.Provider registry) {
        return getResultItem();
    }

    public ItemStack getResultItem();

    public List<ItemStack> getAllResults();

    public Ingredient getDisplayInput();

    public int getTemperatureMin();
    public int getTemperatureMax();
    public int getDifficulty();
    public int getEmberPerHit();
    public int getNumberOfHits();

    @Override
    @Deprecated
    public default boolean canCraftInDimensions(int width, int height) {
        return true;
    }
}
