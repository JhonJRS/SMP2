package net.sirplop.aetherworks.datagen;

import com.rekindled.embers.datagen.EmbersBlockTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.sirplop.aetherworks.AWRegistry;
import net.sirplop.aetherworks.Aetherworks;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

// SMP 2 port: constants only (the data provider ran at build time; its output ships in the jar)
public final class AWBlockTags {
    private AWBlockTags() {}

    public static final TagKey<Block> NEEDS_AETHERIUM_TOOL = BlockTags.create(ResourceLocation.fromNamespaceAndPath(Aetherworks.MODID, "needs_aetherium_tool"));
    public static final TagKey<Block> SCULK_AXE_MINEABLE = BlockTags.create(ResourceLocation.fromNamespaceAndPath(Aetherworks.MODID, "sculk_axe_mineable"));
    public static final TagKey<Block> FORGE_HEATER_BELOW = BlockTags.create(ResourceLocation.fromNamespaceAndPath(Aetherworks.MODID, "forge_heater_below"));
    public static final TagKey<Block> FORGE_COOLER_BELOW = BlockTags.create(ResourceLocation.fromNamespaceAndPath(Aetherworks.MODID, "forge_cooler_below"));
    public static final TagKey<Block> BLOCK_AETHERIUM = BlockTags.create(ResourceLocation.fromNamespaceAndPath("forge", "storage_blocks/aetherium"));
    public static final TagKey<Block> BLOCK_SHARDS = BlockTags.create(ResourceLocation.fromNamespaceAndPath("forge", "storage_blocks/raw_aetherium"));
    public static final TagKey<Block> DROPS_GEODES = BlockTags.create(ResourceLocation.fromNamespaceAndPath(Aetherworks.MODID, "drops_geodes"));
    public static final TagKey<Block> HOVH_RIGHTCLICK = BlockTags.create(ResourceLocation.fromNamespaceAndPath(Aetherworks.MODID, "hovh_rightclick"));
    public static final TagKey<Block> SOIC_BANNED = BlockTags.create(ResourceLocation.fromNamespaceAndPath(Aetherworks.MODID, "soic_banned"));
    public static final TagKey<Block> AOSA_ALLOWED = BlockTags.create(ResourceLocation.fromNamespaceAndPath(Aetherworks.MODID, "aosa_allowed"));
    public static final TagKey<Block> AOTR_ALLOWED = BlockTags.create(ResourceLocation.fromNamespaceAndPath(Aetherworks.MODID, "aotr_allowed"));
    public static final TagKey<Block> POMD_ALLOWED = BlockTags.create(ResourceLocation.fromNamespaceAndPath(Aetherworks.MODID, "pomd_allowed"));
    public static final TagKey<Block> POBS_BANNED = BlockTags.create(ResourceLocation.fromNamespaceAndPath(Aetherworks.MODID, "pobs_banned"));
}
