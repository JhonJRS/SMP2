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
import net.sirplop.aetherworks.item.AetherCrownItem;

import javax.annotation.Nullable;

public class PotionGemUnsocketRecipe implements CraftingRecipe {

    public static final Serializer SERIALIZER = new Serializer();

    public final ResourceLocation id;

    public PotionGemUnsocketRecipe(ResourceLocation id) {
        this.id = id;
    }

    @Override
    public boolean matches(CraftingInput container, Level level) {
        ItemStack crown = ItemStack.EMPTY;
        for (int i = 0; i < container.size(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.getItem() instanceof AetherCrownItem) {
                if (!crown.isEmpty() || !AetherCrownItem.hasAttachedGem(stack))
                    return false;
                crown = stack;
                continue;
            }
            if (!stack.isEmpty())
                return false;
        }
        return !crown.isEmpty() && container.size() >= 1;
    }

    @Override
    public ItemStack assemble(CraftingInput container, HolderLookup.Provider registryAccess) {
        ItemStack crownStack = ItemStack.EMPTY;
        for (int i = 0; i < container.size(); i++) {
            if (!container.getItem(i).isEmpty() && container.getItem(i).getItem() instanceof AetherCrownItem) {
                crownStack = container.getItem(i).copy();
            }
        }
        if (!crownStack.isEmpty()) {
            AetherCrownItem.detachGem(crownStack);
        }
        return crownStack;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput container) {
        NonNullList<ItemStack> gems = NonNullList.withSize(container.size(), ItemStack.EMPTY);
        int index = 0;
        for (int i = 0; i < container.size(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                if (stack.getItem() instanceof AetherCrownItem) {
                    if (!AetherCrownItem.getAttachedGem(stack).isEmpty()) {
                        gems.set(index, AetherCrownItem.getAttachedGem(stack));
                        index++;
                    }
                }
            }
        }
        return gems;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registryAccess) {
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

    
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.EQUIPMENT;
    }

    public static class Serializer extends LegacyRecipeSerializer<PotionGemUnsocketRecipe> {

        @Override
        public PotionGemUnsocketRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            return new PotionGemUnsocketRecipe(recipeId);
        }

        @Override
        public @Nullable PotionGemUnsocketRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            return new PotionGemUnsocketRecipe(recipeId);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, PotionGemUnsocketRecipe recipe) {
        }
    }
}
