package net.sirplop.aetherworks.compat.jei;

import com.rekindled.embers.RegistryManager;
import com.rekindled.embers.compat.jei.IngotTooltipCallback;
import com.rekindled.embers.gui.GuiCodex;
import com.rekindled.embers.util.Misc;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.phys.Vec3;
import net.sirplop.aetherworks.AWConfig;
import net.sirplop.aetherworks.AWRegistry;
import net.sirplop.aetherworks.Aetherworks;
import net.sirplop.aetherworks.recipe.IMetalFormerRecipe;
import net.sirplop.aetherworks.recipe.IToolStationRecipe;

import java.util.List;

public class ToolStationCategory implements IRecipeCategory<IToolStationRecipe> {

    private final IDrawable background;
    private final IDrawable icon;
    public static Component title = Component.translatable(Aetherworks.MODID + ".jei.recipe.tool_station");
    public static ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(Aetherworks.MODID, "textures/gui/jei_tool_station.png");

    public ToolStationCategory(IGuiHelper helper) {
        background = helper.createDrawable(texture, 0, 0, 117, 60);
        icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(AWRegistry.FORGE_TOOL_STATION.get().asItem()));
    }

    @Override
    public RecipeType<IToolStationRecipe> getRecipeType() {
        return JEIPlugin.TOOL_STATION;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, IToolStationRecipe recipe, IFocusGroup focuses) {
        List<Ingredient> inputs = recipe.getDisplayInputs();

        builder.addSlot(RecipeIngredientRole.CATALYST, 64, 4).addItemStack(new ItemStack(RegistryManager.TINKER_HAMMER.get().asItem()));

        if (!inputs.get(0).isEmpty())
            builder.addSlot(RecipeIngredientRole.INPUT, 10, 4).addIngredients(inputs.get(0));
        if (!inputs.get(1).isEmpty())
            builder.addSlot(RecipeIngredientRole.INPUT, 46, 4).addIngredients(inputs.get(1));
        if (!inputs.get(3).isEmpty())
            builder.addSlot(RecipeIngredientRole.INPUT, 10, 40).addIngredients(inputs.get(3));
        if (!inputs.get(4).isEmpty())
            builder.addSlot(RecipeIngredientRole.INPUT, 46, 40).addIngredients(inputs.get(4));

        builder.addSlot(RecipeIngredientRole.INPUT, 28, 22).addIngredients(inputs.get(2));

        builder.addSlot(RecipeIngredientRole.OUTPUT, 92, 22).addItemStack(recipe.getResultItem());
    }

    @SuppressWarnings("resource")
    @Override
    public void draw(IToolStationRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Font fontRenderer = Minecraft.getInstance().font;
        //number of hits
        GuiCodex.drawTextGlowing(fontRenderer, guiGraphics,
                Component.translatable(Aetherworks.MODID + ".jei.recipe.tool_station.hits", AWConfig.FORGE_TOOL_STATION_MAX_HITS.get()).getVisualOrderText(),
                78, 12);

        Vec3 color = new Vec3(255, 181, 77).lerp(new Vec3(255, 77, 77), (double) (recipe.getTemperature() - 2000) / 900);
        int colorVal = (int)color.x << 16 | (int)color.y << 8 | (int)color.z;

        //Misc.drawComponents(fontRenderer, guiGraphics, 15, 62, Component.translatable(Aetherworks.MODID + ".jei.recipe.tool_station.temp_title"));
        Misc.drawComponents(fontRenderer, guiGraphics, 68, 45, Component.translatable(Aetherworks.MODID + ".jei.recipe.tool_station.temp_value", recipe.getTemperature()).withStyle(style -> style.withColor(colorVal)));
    }
}
