package net.sirplop.aetherworks.compat.jei;

import com.rekindled.embers.compat.jei.IngotTooltipCallback;
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
import net.minecraft.world.phys.Vec3;
import net.sirplop.aetherworks.AWRegistry;
import net.sirplop.aetherworks.Aetherworks;
import net.sirplop.aetherworks.recipe.IMetalFormerRecipe;

public class MetalFormingCategory implements IRecipeCategory<IMetalFormerRecipe> {

    private final IDrawable background;
    private final IDrawable icon;
    public static Component title = Component.translatable(Aetherworks.MODID + ".jei.recipe.metal_forming");
    public static ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(Aetherworks.MODID, "textures/gui/jei_metal_former.png");

    public MetalFormingCategory(IGuiHelper helper) {
        background = helper.createDrawable(texture, 0, 0, 87, 75);
        icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(AWRegistry.FORGE_METAL_FORMER.get().asItem()));
    }

    @Override
    public RecipeType<IMetalFormerRecipe> getRecipeType() {
        return JEIPlugin.METAL_FORMING;
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
    public void setRecipe(IRecipeLayoutBuilder builder, IMetalFormerRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 5, 5).addIngredients(recipe.getDisplayInput());

        builder.addSlot(RecipeIngredientRole.OUTPUT, 61, 21).addItemStack(recipe.getResultItem());

        builder.addSlot(RecipeIngredientRole.INPUT, 5, 39)
                .addTooltipCallback(IngotTooltipCallback.INSTANCE)
                .setFluidRenderer(1000, false, 16, 34)
                .addIngredients(NeoForgeTypes.FLUID_STACK, recipe.getDisplayInputFluid().getFluids());
    }

    @SuppressWarnings("resource")
    @Override
    public void draw(IMetalFormerRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Font fontRenderer = Minecraft.getInstance().font;

        Vec3 color = new Vec3(255, 181, 77).lerp(new Vec3(255, 77, 77), (double) (recipe.getTemperature() - 2000) / 900);
        int colorVal = (int)color.x << 16 | (int)color.y << 8 | (int)color.z;

        //Misc.drawComponents(fontRenderer, guiGraphics, 38, 50, Component.translatable(Aetherworks.MODID + ".jei.recipe.metal_forming.temp_title"));
        guiGraphics.drawCenteredString(fontRenderer, Component.translatable(Aetherworks.MODID + ".jei.recipe.metal_forming.temp_value", recipe.getTemperature()), 51, 53, colorVal);
        //Misc.drawComponents(fontRenderer, guiGraphics, 32, 53, );
    }
}
