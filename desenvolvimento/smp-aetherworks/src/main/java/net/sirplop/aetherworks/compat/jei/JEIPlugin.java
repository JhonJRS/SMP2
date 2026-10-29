package net.sirplop.aetherworks.compat.jei;

import com.rekindled.embers.recipe.IVisuallySplitRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;

import net.sirplop.aetherworks.AWRegistry;
import net.sirplop.aetherworks.Aetherworks;
import net.sirplop.aetherworks.recipe.IAetheriumAnvilRecipe;
import net.sirplop.aetherworks.recipe.IMetalFormerRecipe;
import net.sirplop.aetherworks.recipe.IToolStationRecipe;
import net.sirplop.aetherworks.recipe.PotionGemImbueRecipe;
import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class JEIPlugin  implements IModPlugin {

    public static ResourceLocation pluginID = ResourceLocation.fromNamespaceAndPath(Aetherworks.MODID, "jei_plugin");

    public static final RecipeType<IMetalFormerRecipe> METAL_FORMING = RecipeType.create(Aetherworks.MODID, "metal_forming", IMetalFormerRecipe.class);
    public static final RecipeType<IAetheriumAnvilRecipe> AETHERIUM_ANVIL = RecipeType.create(Aetherworks.MODID, "aetherium_anvil", IAetheriumAnvilRecipe.class);
    public static final RecipeType<IToolStationRecipe> TOOL_STATION = RecipeType.create(Aetherworks.MODID, "tool_station", IToolStationRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
        return pluginID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registry) {
        IGuiHelper guiHelper = registry.getJeiHelpers().getGuiHelper();

        registry.addRecipeCategories(new MetalFormingCategory(guiHelper));
        registry.addRecipeCategories(new AetheriumAnvilCategory(guiHelper));
        registry.addRecipeCategories(new ToolStationCategory(guiHelper));
    }
    @SuppressWarnings("unchecked")
    public static <C extends net.minecraft.world.item.crafting.RecipeInput, T extends Recipe<C>> void addRecipes(IRecipeRegistration register, RecipeManager manager, RecipeType<T> jeiType, net.minecraft.world.item.crafting.RecipeType<T> type) {
        List<T> recipes = manager.getAllRecipesFor(type).stream().map(net.minecraft.world.item.crafting.RecipeHolder::value).toList();
        List<T> visualRecipes = new ArrayList<T>();
        for (T recipe : recipes) {
            if (recipe instanceof IVisuallySplitRecipe) {
                visualRecipes.addAll(((IVisuallySplitRecipe<T>) recipe).getVisualRecipes());
            } else {
                visualRecipes.add(recipe);
            }
        }
        register.addRecipes(jeiType, visualRecipes);
    }
    @Override
    public void registerRecipes(@Nonnull IRecipeRegistration register) {
        assert Minecraft.getInstance().level != null;
        RecipeManager manager = Minecraft.getInstance().level.getRecipeManager();

        addRecipes(register, manager, METAL_FORMING, AWRegistry.METAL_FORMING.get());
        addRecipes(register, manager, AETHERIUM_ANVIL, AWRegistry.AETHERIUM_ANVIL.get());
        addRecipes(register, manager, TOOL_STATION, AWRegistry.TOOL_STATION_RECIPE.get());
    }
    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registry) {
        registry.addRecipeCatalyst(new ItemStack(AWRegistry.FORGE_METAL_FORMER.get()), METAL_FORMING);
        registry.addRecipeCatalyst(new ItemStack(AWRegistry.FORGE_ANVIL.get()), AETHERIUM_ANVIL);
        registry.addRecipeCatalyst(new ItemStack(AWRegistry.FORGE_TOOL_STATION.get()), TOOL_STATION);
    }
}
