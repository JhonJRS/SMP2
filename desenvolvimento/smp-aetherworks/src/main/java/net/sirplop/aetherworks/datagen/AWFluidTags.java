package net.sirplop.aetherworks.datagen;

import com.rekindled.embers.datagen.EmbersFluidTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.FluidTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.sirplop.aetherworks.AWRegistry;
import net.sirplop.aetherworks.Aetherworks;
import java.util.concurrent.CompletableFuture;

// SMP 2 port: constants only (the data provider ran at build time; its output ships in the jar)
public final class AWFluidTags {
    private AWFluidTags() {}

    public static final TagKey<Fluid> AETHERIUM = FluidTags.create(ResourceLocation.fromNamespaceAndPath(Aetherworks.MODID, "aetherium"));
    public static final TagKey<Fluid> FORGE_HEATER_BELOW = FluidTags.create(ResourceLocation.fromNamespaceAndPath(Aetherworks.MODID, "forge_heater_below"));
    public static final TagKey<Fluid> FORGE_COOLER_BELOW = FluidTags.create(ResourceLocation.fromNamespaceAndPath(Aetherworks.MODID, "forge_cooler_below"));
}
