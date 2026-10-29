package net.sirplop.aetherworks;
import net.minecraft.core.registries.BuiltInRegistries;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.dimension.DimensionType;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import javax.annotation.Nullable;

import java.util.*;

public class AWConfig {

    public static ConfigValue<Integer> MOONSNARE_STRENGTH;
    public static ConfigValue<Integer> AETHERIC_STRENGTH;
    public static ConfigValue<Double> MOONGAZE_STRENGTH;
    private static ConfigValue<List<? extends String>> SAME_BLOCKS;
    public static ConfigValue<List<? extends String>> MOONLIT_DIMENSIONS;
    public static ConfigValue<Double> TOOL_EMBER_USE;
    public static ConfigValue<Integer> AETHER_PICKAXE_RANGE;
    public static ConfigValue<Integer> EMBER_PICKAXE_RANGE;
    public static ConfigValue<Integer> ENDER_AXE_RANGE;
    public static ConfigValue<Integer> SKULK_AXE_MINE_RANGE;
    public static ConfigValue<Integer> SKULK_AXE_GROW_RANGE;
    public static ConfigValue<Integer> SLIME_SHOVEL_RANGE;
    public static ConfigValue<Integer> PRISMARINE_SHOVEL_CAPACITY;
    public static ConfigValue<Integer> AMETHYST_HOE_TILL_RANGE;
    public static ConfigValue<Integer> AMETHYST_HOE_HARVEST_RANGE;
    public static ConfigValue<Double> CROSSBOW_EMBER_USE;
    public static ConfigValue<Integer> CROSSBOW_MAGMA_CHAIN_LIMIT;
    public static ConfigValue<Double> CROSSBOW_MAGMA_CHAIN_RANGE;

    public static ConfigValue<Integer> FORGE_TOOL_STATION_MAX_HITS;

    public static ConfigValue<Integer> AUGMENT_TUNING_CYLINDER_CHANCE;
    public static ConfigValue<Integer> AUGMENT_TUNING_CYLINDER_BIOME_DEEP_DEPTH;
    private static ConfigValue<List<? extends String>> AUGMENT_TUNING_CYLINDER_BIOME_DEEP_DIMENSIONS;
    public static ConfigValue<Double> AETHER_CROWN_EFFECT_RADIUS;
    private static ConfigValue<List<? extends  String>> POTION_GEM_BANNED;
    private static Dictionary<Block, Set<Block>> SAME_BLOCK_SET = null;

    private static Set<ResourceKey<DimensionType> > DEEP_GEODE_DIMENSIONS_SET;
    private static Set<ResourceKey<DimensionType> > MOONLIT_DIMENSIONS_SET;

    public static boolean isMoonlitDimension(ResourceKey<DimensionType> type) {
        if (MOONLIT_DIMENSIONS_SET == null) {
            MOONLIT_DIMENSIONS_SET = new HashSet<>();
            for (String key : AWConfig.MOONLIT_DIMENSIONS.get()) {
                ResourceLocation location = ResourceLocation.parse(key);
                ResourceKey<DimensionType>  dim = ResourceKey.create(Registries.DIMENSION_TYPE, location);
                MOONLIT_DIMENSIONS_SET.add(dim);
            }
        }
        return MOONLIT_DIMENSIONS_SET.contains(type);
    }
    public static boolean isDeepGeodeDimension(ResourceKey<DimensionType> type) {
        if (DEEP_GEODE_DIMENSIONS_SET == null) {
            DEEP_GEODE_DIMENSIONS_SET = new HashSet<>();
            for (String key : AWConfig.AUGMENT_TUNING_CYLINDER_BIOME_DEEP_DIMENSIONS.get()) {
                ResourceLocation location = ResourceLocation.parse(key);
                ResourceKey<DimensionType>  dim = ResourceKey.create(Registries.DIMENSION_TYPE, location);
                DEEP_GEODE_DIMENSIONS_SET.add(dim);
            }
        }
        return DEEP_GEODE_DIMENSIONS_SET.contains(type);
    }

    public static Set<Block> mixedListToBlocks(ConfigValue<List<? extends String>> list) {
        Set<Block> ret = new HashSet<>();

        for (String val : list.get()) {
            if (val.charAt(0) == '#') { //it's a tag, get it!
                ResourceLocation location = ResourceLocation.parse(val.substring(1));
                BuiltInRegistries.BLOCK.getTag(BlockTags.create(location))
                        .ifPresent(tag -> tag.forEach(holder -> ret.add(holder.value())));
            } else {
                ResourceLocation location = ResourceLocation.parse(val);
                if (BuiltInRegistries.BLOCK.containsKey(location)) {
                    ret.add(BuiltInRegistries.BLOCK.get(location));
                }
            }
        }
        return ret;
    }

    public static Map<net.minecraft.core.Holder<MobEffect>, net.minecraft.core.Holder<MobEffect>> getPotionGemReplacements() {
        Map<net.minecraft.core.Holder<MobEffect>, net.minecraft.core.Holder<MobEffect>> ret = new HashMap<>();
        for (String val : POTION_GEM_BANNED.get()) {
            String[] split = val.split("\\|");
            ResourceLocation left = ResourceLocation.parse(split[0]);
            ResourceLocation right = ResourceLocation.parse(split[1]);
            if (BuiltInRegistries.MOB_EFFECT.containsKey(left)) {
                if (BuiltInRegistries.MOB_EFFECT.containsKey(right)) {
                    ret.put(BuiltInRegistries.MOB_EFFECT.getHolder(left).get(), BuiltInRegistries.MOB_EFFECT.getHolder(right).get());
                }
                else
                    Aetherworks.LOGGER.atError().log("Malformed output potion effect: "+split[1]);
            } else
                Aetherworks.LOGGER.atError().log("Malformed input potion effect: "+split[0]);
        }
        return ret;
    }

    public static Set<Block> getSameBlocks(Block target) {
        if (SAME_BLOCK_SET == null) {
            SAME_BLOCK_SET = new Hashtable<>();
            for (String val : SAME_BLOCKS.get()) {
                String[] split = val.split("\\|");
                Set<Block> blocks = new HashSet<>();
                for (String b : split) {
                    ResourceLocation loc = ResourceLocation.parse(b);
                    if (BuiltInRegistries.BLOCK.containsKey(loc))
                        blocks.add(BuiltInRegistries.BLOCK.get(loc));
                }
                for (Block block : blocks) {
                    if (SAME_BLOCK_SET.get(block) == null) {
                        SAME_BLOCK_SET.put(block, blocks);
                    }
                    else {
                        Set<Block> set = SAME_BLOCK_SET.get(block);
                        set.addAll(blocks);
                        SAME_BLOCK_SET.put(block, set);
                    }
                }
            }
        }
        Set<Block> set = SAME_BLOCK_SET.get(target);
        return Objects.requireNonNullElseGet(set, HashSet::new);
    }

    public static void register(ModContainer container) {
        //registerClientConfigs();
        registerCommonConfigs(container);
        //registerServerConfigs();
    }

    private static void registerCommonConfigs(ModContainer container) {
        ModConfigSpec.Builder COMMON = new ModConfigSpec.Builder();

        COMMON.comment("Settings for tool parameters").push("tool");

        MOONSNARE_STRENGTH = COMMON.comment("How quickly do the Moonsnare jars generate ember? Set to -1 to disable. [default: 3]").defineInRange("moonsnare_strength", 3, -1, 40);
        AETHERIC_STRENGTH = COMMON.comment("How strong is the self-repair effect of aetherium tools? Set to -1 to disable. [default: 3]").defineInRange("aetheric_strength", 3, -1, 40);
        MOONGAZE_STRENGTH = COMMON.comment("How strong should each tick of Moongaze damage be? [default: 4.0]").defineInRange("moongaze_strength", 4.0, 0, 20);
        MOONLIT_DIMENSIONS = COMMON.comment("List of dimensions that are always considered moonlit for Moonsnare containers and Aetheriuc tools.").defineListAllowEmpty("aetheric_dimensions", List.of(
                "minecraft:the_end"
            ), (e) -> true);
        TOOL_EMBER_USE = COMMON.comment("Ember used when using the AOE mode on aetheric tools. [default: 4.0]").define("ember_use", 4.0);
        SAME_BLOCKS = COMMON.comment("Syntax is ( block|block|...|block ). List of sets of blocks that are considered identical for certain aetherium tool effects.")
                .defineListAllowEmpty("same_blocks", List.of(
                    "minecraft:oak_log|minecraft:oak_wood",
                    "minecraft:birch_log|minecraft:birch_wood",
                    "minecraft:spruce_log|minecraft:spruce_wood",
                    "minecraft:jungle_log|minecraft:jungle_wood",
                    "minecraft:dark_oak_log|minecraft:dark_oak_wood",
                    "minecraft:acacia_log|minecraft:acacia_wood",
                    "minecraft:mangrove_log|minecraft:mangrove_wood",
                    "minecraft:cherry_log|minecraft:cherry_wood",
                    "minecraft:crimson_stem|minecraft:crimson_hyphae",
                    "minecraft:warped_stem|minecraft:warped_hyphae",
                    "minecraft:azalea_leaves|minecraft:flowering_azalea_leaves"
                ), a -> true);

        AETHER_PICKAXE_RANGE = COMMON.comment("Maximum vein mine radius of the Pickaxe of the Boundless Sky. [default: 8]").defineInRange("pobs.range", 8, 0, 128);

        EMBER_PICKAXE_RANGE = COMMON.comment("Maximum number of boreholes the Pickaxe of the Molten Depths makes when tunneling. [default: 8]").defineInRange("pomd.range", 8, 0, 128);

        ENDER_AXE_RANGE = COMMON.comment("Maximum recursions the Axe of the Twisted Realm can do while chopping. [default: 512]").defineInRange("aotr.range", 512, 0, 2048);

        SKULK_AXE_MINE_RANGE = COMMON.comment("Maximum vein mine radius of the Axe of the Sonorous Archives. [default: 12]").defineInRange("aosa.range", 12, 0, 128);
        SKULK_AXE_GROW_RANGE = COMMON.comment("Maximum growing radius of the Axe of the Sonorous Archives. [default: 24]").defineInRange("aosa.range", 24, 0, 128);

        SLIME_SHOVEL_RANGE = COMMON.comment("Maximum replacement radius of the Shovel of the Ineluctable Changes. [default: 16]").defineInRange("soic.range", 16, 0, 128);

        PRISMARINE_SHOVEL_CAPACITY = COMMON.comment("Fluid capacity of the Shovel of the Timeless Cascades. [default: 4096]").defineInRange("sotc.capacity", 4096, 0, 1048576);
        AMETHYST_HOE_TILL_RANGE = COMMON.comment("Maximum row length that can be tilled by the Hoe of the Villatic Harvest. [default: 32]").defineInRange("hovh.till_range", 32, 1, 64);
        AMETHYST_HOE_HARVEST_RANGE = COMMON.comment("Maximum harvest radius of the Hoe of the Villatic Harvest. [default: 12]").defineInRange("hovh.harvest_range", 12, 1, 64);

        CROSSBOW_EMBER_USE = COMMON.comment("Ember cost of loading an aetherium crossbow. [default 10]").define("crossbow.ember_use", 10.0);
        CROSSBOW_MAGMA_CHAIN_LIMIT = COMMON.comment("How many entities the Crossbow of the Shattered Reflection is allowed to chain to. [default 16]").define("crossbow.magma.chain_limit", 16);
        CROSSBOW_MAGMA_CHAIN_RANGE = COMMON.comment("How far can an entity be for the Crossbow of the Shattered Reflection to chain to them. [default 16.0]").define("crossbow.magma.chain_range", 16.0);

        AETHER_CROWN_EFFECT_RADIUS = COMMON.comment("Cube radius of the area of effect the Aetherium Crown applies to. [default: 8.0]")
                .define("crown.radius", 8.0);

        POTION_GEM_BANNED = COMMON.comment("Syntax is ( original|replacement ). Effects that should be replaced with another when put into a Vessel Gem.")
                .defineListAllowEmpty("crown.potion_banned", List.of(
                        "minecraft:instant_health|minecraft:regeneration",
                        "minecraft:instant_damage|minecraft:wither"
                ), AWConfig::validatePotions);
        AUGMENT_TUNING_CYLINDER_CHANCE = COMMON.comment("Base chance (1/X) per level to drop a geode when using the Tuning Cylinder augment. [default: 32]").define("tuning_cylinder.chance", 32);
        AUGMENT_TUNING_CYLINDER_BIOME_DEEP_DEPTH = COMMON.comment("At what depth should deep geodes start dropping?").define("tuning_cylinder.deep.depth", 0);
        AUGMENT_TUNING_CYLINDER_BIOME_DEEP_DIMENSIONS = COMMON.comment("What dimensions should be able to drop this geode?")
                .defineListAllowEmpty("tuning_cylinder.deep.dimensions", List.of(
                        "minecraft:overworld"
                ), a -> true);

        COMMON.pop();

        COMMON.comment("Settings for block parameters").push("block");

        FORGE_TOOL_STATION_MAX_HITS = COMMON.comment("Number of hits required to work a tool in the Forge Tool Station. [default: 30]").define("forge.tool_station.hits", 30);

        COMMON.pop();

        container.registerConfig(ModConfig.Type.COMMON, COMMON.build());
    }

    private static boolean validatePotions(final Object obj)
    {
        if (obj instanceof final String potions) {
            String[] split = potions.split("\\|");
            ResourceLocation left = ResourceLocation.parse(split[0]);
            ResourceLocation right = ResourceLocation.parse(split[1]);
            return BuiltInRegistries.MOB_EFFECT.containsKey(left) && BuiltInRegistries.MOB_EFFECT.containsKey(right);
        }
        return false;
    }
}
