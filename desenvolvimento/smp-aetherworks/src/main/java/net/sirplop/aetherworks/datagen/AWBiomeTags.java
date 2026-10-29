package net.sirplop.aetherworks.datagen;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.sirplop.aetherworks.Aetherworks;

// SMP 2 port: constants only (the data provider ran at build time; its output ships in the jar)
public final class AWBiomeTags {
    private AWBiomeTags() {}

    public static final TagKey<Biome> TC_END_GEODES = tag("tc_end_geodes");
    public static final TagKey<Biome> TC_NETHER_GEODES = tag("tc_nether_geodes");
    public static final TagKey<Biome> TC_OCEAN_GEODES = tag("tc_ocean_geodes");
    public static final TagKey<Biome> TC_HOT_GEODES = tag("tc_hot_geodes");
    public static final TagKey<Biome> TC_COLD_GEODES = tag("tc_cold_geodes");
    public static final TagKey<Biome> TC_MAGIC_GEODES = tag("tc_magic_geodes");

    private static TagKey<Biome> tag(String name) {
        return TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(Aetherworks.MODID, name));
    }
}
