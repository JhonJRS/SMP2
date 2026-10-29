package net.sirplop.aetherworks.recipe;
import net.minecraft.core.HolderLookup;
import com.rekindled.embers.recipe.LegacyRecipeSerializer;

import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.sirplop.aetherworks.AWRegistry;
import net.sirplop.aetherworks.item.PotionGemItem;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;

public class PotionGemImbueRecipe implements CraftingRecipe {

    public static final Serializer SERIALIZER = new Serializer();

    public final ResourceLocation id;

    public PotionGemImbueRecipe(ResourceLocation id) {
        this.id = id;
    }

    @Override
    public boolean matches(CraftingInput container, @Nonnull Level level) {
        ItemStack gem = ItemStack.EMPTY;
        ItemStack potion = ItemStack.EMPTY;
        for (int i = 0; i < container.size(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                if (stack.getItem() instanceof PotionGemItem) {
                    if (!gem.isEmpty())
                        return false; //too many gems
                    gem = stack;
                } else if (stack.getItem() instanceof PotionItem) {
                    if (!potion.isEmpty())
                        return false; //too many potions
                    potion = stack;
                } else {
                    return false;
                }
            }
        }
        return !gem.isEmpty() && !potion.isEmpty();
    }

    @Override
    public @Nonnull ItemStack assemble(CraftingInput container, HolderLookup.Provider registryAccess) {
        ItemStack gem = ItemStack.EMPTY;

        for (int i = 0; i < container.size(); i++) {
            if (!container.getItem(i).isEmpty() && container.getItem(i).getItem() instanceof PotionGemItem) {
                gem = container.getItem(i).copyWithCount(1);
            }
        }
        if (!gem.isEmpty()) {
            for (int i = 0; i < container.size(); i++) {
                ItemStack stack = container.getItem(i);
                if (!stack.isEmpty() && stack.getItem() instanceof PotionItem) {
                    ((PotionGemItem)gem.getItem()).setEffects(stack, gem);
                }
            }
            return gem;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public @Nonnull NonNullList<ItemStack> getRemainingItems(CraftingInput container) {
        NonNullList<ItemStack> bottles = NonNullList.withSize(container.size(), ItemStack.EMPTY);
        for (int i = 0; i < container.size(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof PotionItem potion) {
                bottles.set(i, new ItemStack(Items.GLASS_BOTTLE.asItem(), 1));
                break;
            }
        }
        return bottles;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public @Nonnull ItemStack getResultItem(HolderLookup.Provider registryAccess) {
        return new ItemStack(AWRegistry.AETHER_CROWN.get());
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
    public @Nonnull RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public @Nonnull CraftingBookCategory category() {
        return CraftingBookCategory.EQUIPMENT;
    }

    public static class Serializer extends LegacyRecipeSerializer<PotionGemImbueRecipe> {

        @Override
        public @Nonnull PotionGemImbueRecipe fromJson(@Nonnull ResourceLocation recipeId, @Nonnull JsonObject json) {
            return new PotionGemImbueRecipe(recipeId);
        }

        @Override
        public @Nullable PotionGemImbueRecipe fromNetwork(@Nonnull ResourceLocation recipeId, @Nonnull FriendlyByteBuf buffer) {
            return new PotionGemImbueRecipe(recipeId);
        }

        @Override
        public void toNetwork(@Nonnull FriendlyByteBuf buffer, @Nonnull PotionGemImbueRecipe recipe) { }
    }
}
