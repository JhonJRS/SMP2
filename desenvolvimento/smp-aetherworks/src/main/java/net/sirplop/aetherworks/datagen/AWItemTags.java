package net.sirplop.aetherworks.datagen;

import com.rekindled.embers.Embers;
import com.rekindled.embers.datagen.EmbersItemTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.sirplop.aetherworks.AWRegistry;
import net.sirplop.aetherworks.Aetherworks;
import net.sirplop.aetherworks.compat.curios.CuriosCompat;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

// SMP 2 port: constants only (the data provider ran at build time; its output ships in the jar)
public final class AWItemTags {
    private AWItemTags() {}

    public static final TagKey<Item> RAW_AETHERIUM = ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", "raw_materials/aetherium"));
    public static final TagKey<Item> BLOCK_AETHERIUM = ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", "storage_blocks/aetherium"));
    public static final TagKey<Item> BLOCK_SHARDS = ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", "storage_blocks/raw_aetherium"));
    public static final TagKey<Item> AETHERIUM_INGOT = ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", "ingots/aetherium"));
    public static final TagKey<Item> AETHERIUM_PLATE = ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", "plates/aetherium"));
    public static final TagKey<Item> AETHERIUM_ASPECTUS = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "aspectus/aetherium"));
}
