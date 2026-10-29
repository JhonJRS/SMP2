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

import java.util.List;

public interface IToolStationRecipe extends Recipe<RecipeWrapper> {
    public ItemStack getOutput(RecipeWrapper context);

    @Override
    public default ItemStack getToastSymbol() {
        return new ItemStack(AWRegistry.FORGE_TOOL_STATION.get());
    }

    @Override
    public default RecipeType<?> getType() {
        return AWRegistry.TOOL_STATION_RECIPE.get();
    }

    @Override
    public default ItemStack getResultItem(HolderLookup.Provider registry) {
        return getResultItem();
    }

    public ItemStack getResultItem();

    public List<Ingredient> getDisplayInputs();

    public int getTemperature();
    public double getTemperatureRate();

    @Override
    @Deprecated
    public default boolean canCraftInDimensions(int width, int height) {
        return true;
    }
}
