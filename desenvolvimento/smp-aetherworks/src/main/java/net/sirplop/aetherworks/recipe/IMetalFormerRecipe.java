package net.sirplop.aetherworks.recipe;
import net.minecraft.core.HolderLookup;

import com.rekindled.embers.recipe.FluidIngredient;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.sirplop.aetherworks.recipe.RecipeWrapper;
import net.sirplop.aetherworks.AWRegistry;

public interface IMetalFormerRecipe extends Recipe<MetalFormerContext> {


    public ItemStack getOutput(RecipeWrapper context);

    @Override
    public default ItemStack getToastSymbol() {
        return new ItemStack(AWRegistry.FORGE_METAL_FORMER.get());
    }

    @Override
    public default RecipeType<?> getType() {
        return AWRegistry.METAL_FORMING.get();
    }

    @Override
    public default ItemStack getResultItem(HolderLookup.Provider registry) {
        return getResultItem();
    }

    public ItemStack getResultItem();

    public FluidIngredient getDisplayInputFluid();

    public Ingredient getDisplayInput();

    public int getTemperature();

    public int getCraftTime();

    @Override
    @Deprecated
    public default boolean canCraftInDimensions(int width, int height) {
        return true;
    }
}
