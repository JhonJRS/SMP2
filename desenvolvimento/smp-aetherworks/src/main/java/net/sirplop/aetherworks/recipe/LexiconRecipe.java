package net.sirplop.aetherworks.recipe;
import com.rekindled.embers.recipe.LegacyRecipeSerializer;
import net.minecraft.core.HolderLookup;

import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.sirplop.aetherworks.AWRegistry;
import net.sirplop.aetherworks.item.Lexicon;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;

public class LexiconRecipe implements CraftingRecipe {

    public static final LexiconRecipe.Serializer SERIALIZER = new LexiconRecipe.Serializer();

    public final ResourceLocation id;

    public LexiconRecipe(ResourceLocation id) {
        this.id = id;
    }

    @Override
    public @Nonnull CraftingBookCategory category() {
        return CraftingBookCategory.EQUIPMENT;
    }

    @Override
    public boolean matches(CraftingInput container, Level level) {
        ItemStack lexicon = ItemStack.EMPTY;
        ItemStack lexiconInsert = ItemStack.EMPTY;
        for (int i = 0; i < container.size(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                if (stack.getItem() instanceof Lexicon) {
                    if (!lexicon.isEmpty())
                        return false; //too many gems
                    lexicon = stack;
                } else {
                    if (!lexiconInsert.isEmpty())
                        return false;
                    lexiconInsert = stack;
                }
            }
        }
        return !lexicon.isEmpty() && !lexiconInsert.isEmpty();
    }

    @Override
    public @Nonnull ItemStack assemble(CraftingInput container, HolderLookup.Provider registryAccess) {
        ItemStack lexicon = ItemStack.EMPTY;
        ItemStack insert = ItemStack.EMPTY;

        for (int i = 0; i < container.size(); i++) {
            if (!container.getItem(i).isEmpty()) {
                if (container.getItem(i).getItem() instanceof Lexicon) {
                    lexicon = container.getItem(i).copyWithCount(1);
                } else {
                    insert = container.getItem(i).copyWithCount(1);
                }

                if (!insert.isEmpty() && !lexicon.isEmpty())
                    break;
            }
        }
        if (!insert.isEmpty() && !lexicon.isEmpty()) {
            Lexicon.setStoredItem(lexicon, insert, 0);
            return lexicon;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public @Nonnull NonNullList<ItemStack> getRemainingItems(CraftingInput container) {
        NonNullList<ItemStack> remains = NonNullList.withSize(container.size(), ItemStack.EMPTY);
        for (int i = 0; i < container.size(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                if (!(stack.getItem() instanceof Lexicon)) {
                    remains.set(i, stack.copyWithCount(1));
                }
            }
        }
        return remains;
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return true;
    }

    @Override
    public @Nonnull ItemStack getResultItem(HolderLookup.Provider pRegistryAccess) {
        return new ItemStack(AWRegistry.LEXICON.get());
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    
    public @Nonnull ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    public static class Serializer extends LegacyRecipeSerializer<LexiconRecipe> {

        @Override
        public @Nonnull LexiconRecipe fromJson(@Nonnull ResourceLocation recipeId, @Nonnull JsonObject json) {
            return new LexiconRecipe(recipeId);
        }

        @Override
        public @Nullable LexiconRecipe fromNetwork(@Nonnull ResourceLocation recipeId, @Nonnull FriendlyByteBuf buffer) {
            return new LexiconRecipe(recipeId);
        }

        @Override
        public void toNetwork(@Nonnull FriendlyByteBuf buffer, @Nonnull LexiconRecipe recipe) { }
    }
}
