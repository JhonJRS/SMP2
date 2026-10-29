package net.sirplop.aetherworks.recipe;
import net.minecraft.core.HolderLookup;
import com.rekindled.embers.recipe.LegacyRecipeSerializer;

import javax.annotation.Nullable;

import net.minecraft.world.item.PotionItem;
import javax.annotation.Nonnull;
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
import net.sirplop.aetherworks.item.AetherCrownItem;
import net.sirplop.aetherworks.item.PotionGemItem;


public class PotionGemSocketRecipe implements CraftingRecipe {

    public static final Serializer SERIALIZER = new Serializer();

    public final ResourceLocation id;

    public PotionGemSocketRecipe(ResourceLocation id) {
        this.id = id;
    }

    @Override
    public boolean matches(CraftingInput container, @Nonnull Level level) {
        ItemStack crown = ItemStack.EMPTY;
        ItemStack gem = ItemStack.EMPTY;
        for (int i = 0; i < container.size(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                if (stack.getItem() instanceof AetherCrownItem) {
                    if (!crown.isEmpty())
                        return false; //too many gems
                    crown = stack;
                } else if (stack.getItem() instanceof PotionGemItem) {
                    if (!gem.isEmpty())
                        return false; //too many potions
                    gem = stack;
                } else {
                    return false;
                }
            }
        }
        return !crown.isEmpty() && !gem.isEmpty();

        /*ItemStack crown = ItemStack.EMPTY;
        int crowns = 0;
        int gems = 0;
        for (int i = 0; i < container.size(); i ++) {
            ItemStack stack = container.getItem(i);
            if (stack.getItem() instanceof AetherCrownItem) {
                crown = stack;
            }
        }
        for (int i = 0; i < container.size(); i ++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                if (stack.getItem() instanceof AetherCrownItem) {
                    crowns++;
                } else if (!crown.isEmpty() && ((AetherCrownItem) crown.getItem()).canAttachGem(stack)) {
                    gems++;
                } else {
                    return false;
                }
            }
        }
        return !crown.isEmpty() && crowns == 1 && gems == 1;*/
    }

    @Override
    public @Nonnull ItemStack assemble(CraftingInput container, HolderLookup.Provider registryAccess) {
        ItemStack crown = ItemStack.EMPTY;

        for (int i = 0; i < container.size(); i++) {
            if (!container.getItem(i).isEmpty() && container.getItem(i).getItem() instanceof AetherCrownItem) {
                crown = container.getItem(i).copy();
            }
        }
        if (!crown.isEmpty()) {
            for (int i = 0; i < container.size(); i++) {
                ItemStack stack = container.getItem(i);
                if (!stack.isEmpty() && stack.getItem() instanceof PotionGemItem) {
                    AetherCrownItem.attachGem(crown, stack.copyWithCount(1));
                }
            }
            return crown;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public @Nonnull NonNullList<ItemStack> getRemainingItems(CraftingInput container) {
        NonNullList<ItemStack> gems = NonNullList.withSize(container.size(), ItemStack.EMPTY);
        for (int i = 0; i < container.size(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                if (stack.getItem() instanceof AetherCrownItem crown) {
                    if (crown.hasAttachedGem(stack)) {
                        ItemStack gem = crown.getAttachedGem(stack);
                        for (int x = 0; x < container.size(); x++) {
                            if (true) {
                                gems.set(x, gem);
                                return gems;
                            }
                        }
                        gems.set(0, gem);
                        return gems;
                    }
                }
            }
        }
        return gems;
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

    public static class Serializer extends LegacyRecipeSerializer<PotionGemSocketRecipe> {

        @Override
        public @Nonnull PotionGemSocketRecipe fromJson(@Nonnull ResourceLocation recipeId, @Nonnull JsonObject json) {
            return new PotionGemSocketRecipe(recipeId);
        }

        @Override
        public @Nullable PotionGemSocketRecipe fromNetwork(@Nonnull ResourceLocation recipeId, @Nonnull FriendlyByteBuf buffer) {
            return new PotionGemSocketRecipe(recipeId);
        }

        @Override
        public void toNetwork(@Nonnull FriendlyByteBuf buffer, @Nonnull PotionGemSocketRecipe recipe) { }
    }
}
