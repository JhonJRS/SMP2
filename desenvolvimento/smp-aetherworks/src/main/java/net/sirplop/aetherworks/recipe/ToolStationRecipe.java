package net.sirplop.aetherworks.recipe;
import com.rekindled.embers.recipe.LegacyRecipeSerializer;
import net.sirplop.aetherworks.util.Compat121;
import net.minecraft.core.HolderLookup;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.sirplop.aetherworks.recipe.RecipeWrapper;
import net.sirplop.aetherworks.Aetherworks;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ToolStationRecipe implements IToolStationRecipe{
    public static final Serializer SERIALIZER = new Serializer();

    public final ResourceLocation id;

    public final List<Ingredient> inputs;
    public final int temperature;
    public final double temperatureRate;

    public final ItemStack output;

    public ToolStationRecipe(ResourceLocation id, List<Ingredient> inputs,  int temperature, double temperatureRate, ItemStack output) {
        this.id = id;
        this.inputs = inputs;
        this.temperature = temperature;
        this.temperatureRate = temperatureRate;
        this.output = output;
    }

    @Override
    public boolean matches(RecipeWrapper context, Level level) {
        for (int i = 0; i < inputs.size(); i++) {
            if (!inputs.get(i).test(context.getItem(i)))
                return false;
        }
        return true;
    }

    @Override
    public ItemStack getOutput(RecipeWrapper context) {
        return getResultItem();
    }

    @Override
    public ItemStack assemble(RecipeWrapper context, HolderLookup.Provider registry) {
        for (int i = 0; i < inputs.size(); i++) {
            if (inputs.get(i).test(context.getItem(i))) {
                context.removeItemNoUpdate(i);
            }
        }
        return this.getOutput(context);
    }

    
    public ResourceLocation getId() {
        return id;
    }
    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }
    @Override
    public ItemStack getResultItem() {
        return output.copy();
    }

    @Override
    public List<Ingredient> getDisplayInputs() {
        return inputs;
    }

    @Override
    public int getTemperature() {
        return temperature;
    }
    @Override
    public double getTemperatureRate() {
        return temperatureRate;
    }

    public static class Serializer extends LegacyRecipeSerializer<ToolStationRecipe> {

        @Override
        public ToolStationRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            List<Ingredient> inputs = new ArrayList<>();
            if (json.has("inputs")) {
                JsonArray arr = json.getAsJsonArray("inputs");
                for (int i = 0; i < arr.size(); i++) {
                    inputs.add(Compat121.ingredientFromJson(arr.get(i)));
                }
            }

            int temperature = json.get("temperature").getAsInt();
            double temperatureRate = json.get("temperature_rate").getAsInt();

            JsonObject outputJson = GsonHelper.getAsJsonObject(json, "output");
            ItemStack output = Compat121.itemFromJson(outputJson);
            return new ToolStationRecipe(recipeId, inputs, temperature, temperatureRate, output);
        }

        @Override
        public @Nullable ToolStationRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            int size = buffer.readInt();
            List<Ingredient> inputs = new ArrayList<>(size);
            for (int i = 0; i < size; i++)
                inputs.add(Compat121.readIngredient(buffer));
            int temperature = buffer.readInt();
            double temperatureRate = buffer.readDouble();

            ItemStack output = Compat121.readItem(buffer);
            return new ToolStationRecipe(recipeId, inputs, temperature, temperatureRate, output);
        }

        @Override
        public void toNetwork(@Nonnull FriendlyByteBuf buffer, ToolStationRecipe recipe) {
            buffer.writeInt(recipe.inputs.size());
            for (int i = 0; i < recipe.inputs.size(); i++)
                Compat121.writeIngredient(buffer, recipe.inputs.get(i));
            buffer.writeInt(recipe.temperature);
            buffer.writeDouble(recipe.temperatureRate);
            Compat121.writeItem(buffer, recipe.output);
        }
    }
}
