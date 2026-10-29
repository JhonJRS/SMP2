package net.sirplop.aetherworks.recipe;
import com.rekindled.embers.recipe.LegacyRecipeSerializer;
import net.sirplop.aetherworks.util.Compat121;
import net.minecraft.core.HolderLookup;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.rekindled.embers.util.Misc;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.sirplop.aetherworks.recipe.RecipeWrapper;
import net.sirplop.aetherworks.util.WeightedList;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class AetheriumAnvilRecipe implements IAetheriumAnvilRecipe {
    public static final Serializer SERIALIZER = new Serializer();

    public final ResourceLocation id;

    public final Ingredient input;
    public final int temperatureMin;
    public final int temperatureMax;
    public final int difficulty;
    public final int emberPerHit;
    public final int numberOfHits;

    public final WeightedList<Either<ItemStack, TagKey<Item>>> output;

    public AetheriumAnvilRecipe(ResourceLocation id, Ingredient input, int temperatureMin, int temperatureMax,
                                int difficulty, int emberPerHit, int numberOfHits, WeightedList<Either<ItemStack, TagKey<Item>>> list) {
        this.id = id;
        this.input = input;
        this.output = list;

        this.temperatureMin = temperatureMin;
        this.temperatureMax = temperatureMax;
        this.difficulty = difficulty;
        this.emberPerHit = emberPerHit;
        this.numberOfHits = numberOfHits;
    }

    @Override
    public ItemStack getOutput(RecipeWrapper context) {
        return getResultItem();
    }

    @Override
    public ItemStack getResultItem() {
        var result = output.choose();
        if (result.left().isPresent())
            return result.left().get().copy();
        return new ItemStack(Misc.getTaggedItem(result.right().get()), 1);
    }

    @Override
    public List<ItemStack> getAllResults() {
        List<ItemStack> ret = new ArrayList<>();
        for (var pair : output.internalList) {
            if (pair.getFirst().right().isPresent()) {
                ret.add(new ItemStack(Misc.getTaggedItem(pair.getFirst().right().get())));
            } else {
                ret.add(pair.getFirst().left().get());
            }
        }
        return ret;
    }

    @Override
    public Ingredient getDisplayInput() {
        return input;
    }

    @Override
    public int getTemperatureMin() {
        return temperatureMin;
    }

    @Override
    public int getTemperatureMax() {
        return temperatureMax;
    }

    @Override
    public int getDifficulty() {
        return difficulty;
    }

    @Override
    public int getEmberPerHit() {
        return emberPerHit;
    }

    @Override
    public int getNumberOfHits() {
        return numberOfHits;
    }

    @Override
    public boolean matches(AetheriumAnvilContext context, Level level) {
        for (int i = 0; i < context.getContainerSize(); i++) {
            if (input.test(context.getItem(i))) {
                if (context.temperature >= this.temperatureMin &&
                    context.temperature <= this.temperatureMax) {
                        return true;
                }
            }
        }
        return false;
    }

    @Override
    public ItemStack assemble(AetheriumAnvilContext context, HolderLookup.Provider registryAccess) {
        for (int i = 0; i < context.getContainerSize(); i++) {
            if (input.test(context.getItem(i))) {
                context.removeItem(i, 1);
                break;
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
    public static class Serializer extends LegacyRecipeSerializer<AetheriumAnvilRecipe> {

        @Override
        public AetheriumAnvilRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            Ingredient input = Ingredient.EMPTY;
            if (json.has("input"))
                input = Compat121.ingredientFromJson(json.get("input"));

            int temperatureMin = json.get("temperatureMin").getAsInt();
            int temperatureMax = json.get("temperatureMax").getAsInt();
            int difficulty = json.get("difficulty").getAsInt();
            int emberPerHit = json.get("emberPerHit").getAsInt();
            int numberOfHits =  json.get("numberOfHits").getAsInt();

            WeightedList<Either<ItemStack, TagKey<Item>>> result = new WeightedList<>();
            JsonArray outputJson = GsonHelper.getAsJsonArray(json, "result");
            for (JsonElement element : outputJson) {
                JsonObject stackObj = element.getAsJsonObject();
                if (stackObj.has("tag")) {
                    TagKey<Item> output = ItemTags.create(ResourceLocation.parse(GsonHelper.getAsString(stackObj, "tag")));
                    double chance = stackObj.getAsJsonPrimitive("chance").getAsDouble();
                    result.add(Either.right(output), chance);
                } else {
                    ItemStack output = Compat121.itemFromJson(stackObj.getAsJsonObject());
                    double chance = stackObj.getAsJsonPrimitive("chance").getAsDouble();
                    result.add(Either.left(output), chance);
                }
            }
            return new AetheriumAnvilRecipe(recipeId, input, temperatureMin, temperatureMax, difficulty, emberPerHit, numberOfHits, result);
        }

        @Override
        public @Nullable AetheriumAnvilRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            Ingredient input = Compat121.readIngredient(buffer);
            int temperatureMin = buffer.readInt();
            int temperatureMax = buffer.readInt();
            int difficulty = buffer.readInt();
            int emberPerHit = buffer.readInt();
            int numberOfHits = buffer.readInt();

            int size = buffer.readInt();
            WeightedList<Either<ItemStack, TagKey<Item>>> result = new WeightedList<>();
            for (int i = 0; i < size; i++) {
                if (buffer.readBoolean()) {
                    TagKey<Item> output = ItemTags.create(buffer.readResourceLocation());
                    result.add(Either.right(output), buffer.readDouble());
                } else {
                    ItemStack output = Compat121.readItem(buffer);
                    result.add(Either.left(output), buffer.readDouble());
                }
            }
            return new AetheriumAnvilRecipe(recipeId, input, temperatureMin, temperatureMax, difficulty, emberPerHit, numberOfHits, result);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, AetheriumAnvilRecipe recipe) {
            Compat121.writeIngredient(buffer, recipe.input);
            buffer.writeInt(recipe.temperatureMin);
            buffer.writeInt(recipe.temperatureMax);
            buffer.writeInt(recipe.difficulty);
            buffer.writeInt(recipe.emberPerHit);
            buffer.writeInt(recipe.numberOfHits);
            buffer.writeInt(recipe.output.internalList.size());

            for (Pair<Either<ItemStack, TagKey<Item>>, Double> stack : recipe.output.internalList) {
                if (stack.getFirst().right().isPresent()) {
                    buffer.writeBoolean(true);
                    buffer.writeResourceLocation(stack.getFirst().right().get().location());
                    buffer.writeDouble(stack.getSecond());
                } else {
                    buffer.writeBoolean(false);
                    Compat121.writeItem(buffer, stack.getFirst().left().get());
                    buffer.writeDouble(stack.getSecond());
                }
            }
        }
    }
}