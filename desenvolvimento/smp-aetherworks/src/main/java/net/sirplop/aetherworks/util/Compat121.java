package net.sirplop.aetherworks.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.util.function.Consumer;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * SMP 2 port (1.20.1 Forge -> 1.21.1 NeoForge): the item NBT tag became the immutable CUSTOM_DATA component, and
 * items/ingredients/fluids on the network need registry-aware codecs. Everything the old code did with
 * getOrCreateTag()/Compat121.stackOf()/writeItem() goes through here.
 */
public final class Compat121 {
    private Compat121() {}

    // ---------- registries for code that has no Level at hand ----------
    public static HolderLookup.Provider registries() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) return server.registryAccess();
        if (FMLEnvironment.dist.isClient()) {
            HolderLookup.Provider client = Client.registries();
            if (client != null) return client;
        }
        return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    private static final class Client {
        static HolderLookup.Provider registries() {
            var level = net.minecraft.client.Minecraft.getInstance().level;
            return level != null ? level.registryAccess() : null;
        }
    }

    // ---------- enchantments are data-driven in 1.21 (keys, not objects) ----------
    public static net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> enchantment(
            net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key) {
        return registries().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(key);
    }

    public static int enchantmentLevel(net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key, ItemStack stack) {
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(enchantment(key), stack);
    }

    // ---------- item custom data (old stack NBT) ----------
    /** A copy of the stack's custom data; changing it does NOT change the stack (use {@link #edit}). */
    public static CompoundTag tag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    public static boolean hasTag(ItemStack stack) {
        return stack.has(DataComponents.CUSTOM_DATA);
    }

    public static void edit(ItemStack stack, Consumer<CompoundTag> change) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, change);
    }

    public static void setTag(ItemStack stack, CompoundTag tag) {
        CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
    }

    public static ItemStack stackOf(CompoundTag tag) {
        return tag == null || tag.isEmpty() ? ItemStack.EMPTY : ItemStack.parseOptional(registries(), tag);
    }

    public static CompoundTag save(ItemStack stack) {
        return stack.isEmpty() ? new CompoundTag() : (CompoundTag) stack.save(registries());
    }

    // ---------- JSON (recipes keep the 1.20 format: {"item": id, "count": n, "nbt": "{...}"}) ----------
    public static ItemStack itemFromJson(JsonObject json) {
        if (!json.has("item"))
            return ItemStack.CODEC.parse(registries().createSerializationContext(JsonOps.INSTANCE), json).getOrThrow();
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(json.get("item").getAsString())),
                json.has("count") ? json.get("count").getAsInt() : 1);
        if (json.has("nbt")) {
            try {
                JsonElement nbt = json.get("nbt");
                setTag(stack, TagParser.parseTag(nbt.isJsonPrimitive() ? nbt.getAsString() : nbt.toString()));
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid nbt in item json: " + e.getMessage());
            }
        }
        return stack;
    }

    public static Ingredient ingredientFromJson(JsonElement json) {
        return Ingredient.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
    }

    // ---------- network (LegacyRecipeSerializer hands us a RegistryFriendlyByteBuf) ----------
    public static void writeItem(FriendlyByteBuf buf, ItemStack stack) {
        ItemStack.OPTIONAL_STREAM_CODEC.encode((RegistryFriendlyByteBuf) buf, stack);
    }

    public static ItemStack readItem(FriendlyByteBuf buf) {
        return ItemStack.OPTIONAL_STREAM_CODEC.decode((RegistryFriendlyByteBuf) buf);
    }

    public static void writeIngredient(FriendlyByteBuf buf, Ingredient ingredient) {
        Ingredient.CONTENTS_STREAM_CODEC.encode((RegistryFriendlyByteBuf) buf, ingredient);
    }

    public static Ingredient readIngredient(FriendlyByteBuf buf) {
        return Ingredient.CONTENTS_STREAM_CODEC.decode((RegistryFriendlyByteBuf) buf);
    }

    public static void writeFluid(FriendlyByteBuf buf, FluidStack fluid) {
        FluidStack.OPTIONAL_STREAM_CODEC.encode((RegistryFriendlyByteBuf) buf, fluid);
    }

    public static FluidStack readFluid(FriendlyByteBuf buf) {
        return FluidStack.OPTIONAL_STREAM_CODEC.decode((RegistryFriendlyByteBuf) buf);
    }
}
