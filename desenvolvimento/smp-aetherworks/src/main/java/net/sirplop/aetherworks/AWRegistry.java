package net.sirplop.aetherworks;
import com.rekindled.embers.util.CapabilityCompat;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.minecraft.core.registries.BuiltInRegistries;


import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.rekindled.embers.RegistryManager;
import com.rekindled.embers.api.EmbersAPI;
import com.rekindled.embers.api.augment.AugmentUtil;
import com.rekindled.embers.api.augment.IAugment;
import com.rekindled.embers.datagen.EmbersSounds;
import com.rekindled.embers.fluidtypes.EmbersFluidType;
import com.rekindled.embers.item.EmberStorageItem;
import com.rekindled.embers.util.AshenArmorMaterial;
import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.material.*;
import net.minecraft.world.level.pathfinder.PathType;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidInteractionRegistry;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.sirplop.aetherworks.augment.AetherPlatingAugment;
import net.sirplop.aetherworks.augment.AgrarianLinersAugment;
import net.sirplop.aetherworks.augment.VolantCalcifierAugment;
import net.sirplop.aetherworks.augment.TuningCylinderAugment;
import net.sirplop.aetherworks.block.*;
import net.sirplop.aetherworks.block.forge.*;
import net.sirplop.aetherworks.blockentity.*;
import net.sirplop.aetherworks.effect.MoonfireEffect;
import net.sirplop.aetherworks.effect.PullDownEffect;
import net.sirplop.aetherworks.entity.DummyArmorLoaderEntity;
import net.sirplop.aetherworks.fluid.AWPainFluidType;
import net.sirplop.aetherworks.fluid.AWViscousFluidType;
import net.sirplop.aetherworks.fluid.AWMoltenMetalFluidType;
import net.sirplop.aetherworks.item.*;
import net.sirplop.aetherworks.item.tool.*;
import net.sirplop.aetherworks.recipe.*;
import net.sirplop.aetherworks.util.AetheriumTiers;
import net.sirplop.aetherworks.util.Utils;
import net.sirplop.aetherworks.worldgen.MeteorStructure;
import net.sirplop.aetherworks.worldgen.MeteorStructurePiece;
import javax.annotation.Nonnull;

public class AWRegistry {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, Aetherworks.MODID);
    public static final DeferredRegister<Block> BLOCKS =  DeferredRegister.create(Registries.BLOCK, Aetherworks.MODID);
    public static final DeferredRegister<FluidType> FLUIDTYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Aetherworks.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, Aetherworks.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Aetherworks.MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, Aetherworks.MODID);
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Aetherworks.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Aetherworks.MODID);
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, Aetherworks.MODID);
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, Aetherworks.MODID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, Aetherworks.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Aetherworks.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Aetherworks.MODID);
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Aetherworks.MODID);
    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, Aetherworks.MODID);

    // SMP 2 port: item fluid handlers (Prismarine Shovel) keep their fluid in a data component
    public static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Aetherworks.MODID);
    public static final DeferredHolder<net.minecraft.core.component.DataComponentType<?>, net.minecraft.core.component.DataComponentType<net.neoforged.neoforge.fluids.SimpleFluidContent>> FLUID_CONTENTS =
            DATA_COMPONENTS.registerComponentType("fluid_contents", b -> b.persistent(net.neoforged.neoforge.fluids.SimpleFluidContent.CODEC)
                    .networkSynchronized(net.neoforged.neoforge.fluids.SimpleFluidContent.STREAM_CODEC));

    // SMP 2 port: per-chunk aether amount (was a chunk capability attached through AttachCapabilitiesEvent)
    public static final DeferredRegister<net.neoforged.neoforge.attachment.AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Aetherworks.MODID);
    public static final java.util.function.Supplier<net.neoforged.neoforge.attachment.AttachmentType<net.sirplop.aetherworks.capabilities.AetheriometerChunk>> AETHERIOMETER_DATA =
            ATTACHMENT_TYPES.register("aether", () -> net.neoforged.neoforge.attachment.AttachmentType
                    .serializable(net.sirplop.aetherworks.capabilities.AetheriometerChunk::forHolder).build());

    public static List<FluidStuff> fluidList = new ArrayList<>();

    public static FluidStuff addFluid(EmbersFluidType.FluidInfo info,
                                      BiFunction<FluidType.Properties, EmbersFluidType.FluidInfo, FluidType> type,
                                      BiFunction<FlowingFluid, Properties, LiquidBlock> block,
                                      Function<BaseFlowingFluid.Properties, BaseFlowingFluid.Source> source,
                                      Function<BaseFlowingFluid.Properties, BaseFlowingFluid.Flowing> flowing,
                                      @Nullable Consumer<BaseFlowingFluid.Properties> fluidProperties, FluidType.Properties prop) {
        FluidStuff fluid = new FluidStuff(info.name, info.color, type.apply(prop, info), block, fluidProperties, source, flowing);
        fluidList.add(fluid);
        return fluid;
    }

    public static FluidStuff addFluid(EmbersFluidType.FluidInfo info,
                                      BiFunction<FluidType.Properties, EmbersFluidType.FluidInfo, FluidType> type,
                                      BiFunction<FlowingFluid, Properties, LiquidBlock> block,
                                      @Nullable Consumer<BaseFlowingFluid.Properties> fluidProperties, FluidType.Properties prop) {
        return addFluid(info, type, block, BaseFlowingFluid.Source::new, BaseFlowingFluid.Flowing::new, fluidProperties, prop);
    }

    //Items & Blocks
    public static final DeferredHolder<Item, Item> PICKAXE_EMBER = ITEMS.register("pickaxe_ember", () -> new EmberPickaxe(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, Item> PICKAXE_AETHER = ITEMS.register("pickaxe_aether", () -> new AetherPickaxe(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, Item> AXE_ENDER = ITEMS.register("axe_ender", () -> new EnderAxe(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, Item> AXE_SCULK = ITEMS.register("axe_sculk", () -> new SculkAxe(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, Item> SHOVEL_SLIME = ITEMS.register("shovel_slime", () -> new SlimeShovel(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, Item> SHOVEL_PRISMARINE = ITEMS.register("shovel_prismarine", () -> new PrismarineShovel(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, Item> HOE_HONEY = ITEMS.register("hoe_honey", () -> new HoneyHoe(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, Item> HOE_AMETHYST = ITEMS.register("hoe_amethyst", () -> new AmethystHoe(new Item.Properties().rarity(Rarity.RARE)));

    public static final DeferredHolder<Item, Item> CROSSBOW_QUARTZ = ITEMS.register("crossbow_quartz", () -> new AetherCrossbowQuartz(new Item.Properties().rarity(Rarity.RARE).stacksTo(1).durability(AetheriumTiers.AETHERIUM.getUses())));
    public static final DeferredHolder<Item, Item> CROSSBOW_MAGMA = ITEMS.register("crossbow_magma", () -> new AetherCrossbowMagma(new Item.Properties().rarity(Rarity.RARE).stacksTo(1).durability(AetheriumTiers.AETHERIUM.getUses())));

    public static final DeferredHolder<Item, Item> AETHER_SHIELD = ITEMS.register("aether_shield", () -> new AetherShield(new Item.Properties().rarity(Rarity.RARE).stacksTo(1).durability(AetheriumTiers.AETHERIUM.getUses())));
    public static final DeferredHolder<Item, Item> AETHER_CROWN = ITEMS.register("aether_crown", () -> new AetherCrownItem(AshenArmorMaterial.INSTANCE, ArmorItem.Type.HELMET, new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, Item> POTION_GEM = ITEMS.register("potion_gem", () -> new PotionGemItem(new Item.Properties()));

    public static final DeferredHolder<Item, Item> AETHER_EMBER_JAR = ITEMS.register("aether_ember_jar", () -> new AetherEmberJarItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> AETHER_EMBER_CARTRIDGE = ITEMS.register("aether_ember_cartridge", () -> new AetherEmberCartridgeItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> AETHERIOMETER = ITEMS.register("aetheriometer", () -> new Item(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Block, Block> HEAT_DIAL = registerBlock("heat_dial", () -> new HeatDialBlock(Properties.ofFullCopy(RegistryManager.EMBER_DIAL.get())));
    public static final DeferredHolder<Block, Block> PRISM = registerBlock("prism", () -> new PrismBlock(Properties.ofFullCopy(RegistryManager.FLUID_VESSEL.get()).forceSolidOn().strength(4, 8).requiresCorrectToolForDrops()));
    public static final DeferredHolder<Block, Block> PRISM_SUPPORT = registerBlock("prism_support", () -> new PrismSupportBlock(Properties.ofFullCopy(RegistryManager.CAMINITE_BRICKS.get()).forceSolidOn().requiresCorrectToolForDrops()));
    public static final DeferredHolder<Block, Block> MOONLIGHT_AMPLIFIER = registerBlock("moonlight_amplifier", () -> new MoonlightAmplifierBlock(Properties.ofFullCopy(RegistryManager.FLUID_VESSEL.get()).forceSolidOn().requiresCorrectToolForDrops().strength(3, 6).sound(SoundType.GLASS)));
    public static final DeferredHolder<Block, Block> CONTROL_MATRIX = registerBlock("aether_prism_controller_matrix", () -> new ControlMatrixBlock(Properties.ofFullCopy(RegistryManager.FLUID_VESSEL.get()).forceSolidOn().strength(3, 6).requiresCorrectToolForDrops()));

    public static final DeferredHolder<Block, Block> AETHER_FORGE = registerBlock("aether_forge", () -> new AetherForgeBlock(Properties.of().mapColor(MapColor.WOOD).pushReaction(PushReaction.BLOCK).sound(EmbersSounds.MULTIBLOCK_CENTER).requiresCorrectToolForDrops().strength(1.6f).noOcclusion(), EmbersSounds.MULTIBLOCK_EXTRA));
    public static final DeferredHolder<Block, Block> AETHER_FORGE_EDGE = BLOCKS.register("aether_forge_edge", () -> new AetherForgeEdgeBlock(Properties.of().mapColor(MapColor.WOOD).sound(EmbersSounds.MULTIBLOCK_EXTRA).requiresCorrectToolForDrops().strength(1.6f)));
    public static final DeferredHolder<Block, Block> FORGE_VENT = registerBlock("forge_vent", () -> new ForgeVentBlock(Properties.ofFullCopy(RegistryManager.FLUID_VESSEL.get()).forceSolidOn().strength(3, 10).requiresCorrectToolForDrops()));
    public static final DeferredHolder<Block, Block> FORGE_HEATER = registerBlock("forge_heater", () -> new ForgeHeaterBlock(Properties.ofFullCopy(RegistryManager.FLUID_VESSEL.get()).forceSolidOn().strength(3, 10).requiresCorrectToolForDrops()));
    public static final DeferredHolder<Block, Block> FORGE_COOLER = registerBlock("forge_cooler", () -> new ForgeCoolerBlock(Properties.ofFullCopy(RegistryManager.FLUID_VESSEL.get()).forceSolidOn().strength(3, 10).requiresCorrectToolForDrops()));
    public static final DeferredHolder<Block, Block> FORGE_ANVIL = registerBlock("forge_anvil", () -> new AetheriumAnvilBlock(Properties.ofFullCopy(RegistryManager.DAWNSTONE_ANVIL.get()).forceSolidOn().strength(3, 10).requiresCorrectToolForDrops()));
    public static final DeferredHolder<Block, Block> FORGE_METAL_FORMER = registerBlock("forge_metal_former", () -> new MetalFormerBlock(Properties.ofFullCopy(RegistryManager.FLUID_VESSEL.get()).forceSolidOn().strength(3, 10).requiresCorrectToolForDrops()));
    public static final DeferredHolder<Block, Block> FORGE_TOOL_STATION = registerBlock("forge_tool_station", () -> new ForgeToolStation(Properties.ofFullCopy(RegistryManager.FLUID_VESSEL.get()).forceSolidOn().requiresCorrectToolForDrops().strength(3, 6)));

    public static final DeferredHolder<Block, Block> LEXICON_RECEPTACLE = registerBlock("lexicon_receptacle", () -> new LexiconReceptacleBlock(Properties.ofFullCopy(RegistryManager.FLUID_VESSEL.get()).forceSolidOn().requiresCorrectToolForDrops().strength(3, 12)));

    public static final DeferredHolder<Block, Block> SUEVITE = registerBlock("suevite", () -> new Block(Properties.of().mapColor(MapColor.TERRACOTTA_LIGHT_GRAY).sound(SoundType.ANCIENT_DEBRIS).requiresCorrectToolForDrops().strength(2.5f)));
    public static final DeferredHolder<Block, Block> SUEVITE_COBBLE = registerBlock("suevite_cobble", () -> new Block(Properties.of().mapColor(MapColor.TERRACOTTA_LIGHT_GRAY).sound(SoundType.ANCIENT_DEBRIS).requiresCorrectToolForDrops().strength(1.6f)));
    public static final StoneDecoBlocks SUEVITE_COBBLE_DECO = new StoneDecoBlocks("suevite_cobble", SUEVITE_COBBLE, Properties.of().mapColor(MapColor.TERRACOTTA_LIGHT_GRAY).sound(SoundType.ANCIENT_DEBRIS).requiresCorrectToolForDrops().strength(1.6f));
    public static final DeferredHolder<Block, Block> SUEVITE_BRICKS = registerBlock("suevite_bricks", () -> new Block(Properties.of().mapColor(MapColor.TERRACOTTA_GRAY).sound(SoundType.ANCIENT_DEBRIS).requiresCorrectToolForDrops().strength(1.6f)));
    public static final StoneDecoBlocks SUEVITE_BRICKS_DECO = new StoneDecoBlocks("suevite_bricks", SUEVITE_BRICKS, Properties.of().mapColor(MapColor.TERRACOTTA_GRAY).sound(SoundType.ANCIENT_DEBRIS).requiresCorrectToolForDrops().strength(1.6f));
    public static final DeferredHolder<Block, Block> SUEVITE_SMALL_BRICKS = registerBlock("suevite_small_bricks", () -> new Block(Properties.of().mapColor(MapColor.TERRACOTTA_GRAY).sound(SoundType.ANCIENT_DEBRIS).requiresCorrectToolForDrops().strength(1.6f)));
    public static final StoneDecoBlocks SUEVITE_SMALL_BRICKS_DECO = new StoneDecoBlocks("suevite_small_bricks", SUEVITE_SMALL_BRICKS, Properties.of().mapColor(MapColor.TERRACOTTA_GRAY).sound(SoundType.ANCIENT_DEBRIS).requiresCorrectToolForDrops().strength(1.6f));
    public static final DeferredHolder<Block, Block> SUEVITE_BIG_TILE = registerBlock("suevite_big_tile", () -> new Block(Properties.of().mapColor(MapColor.TERRACOTTA_GRAY).sound(SoundType.ANCIENT_DEBRIS).requiresCorrectToolForDrops().strength(1.6f)));
    public static final StoneDecoBlocks SUEVITE_BIG_TILE_DECO = new StoneDecoBlocks("suevite_big_tile", SUEVITE_BIG_TILE, Properties.of().mapColor(MapColor.TERRACOTTA_GRAY).sound(SoundType.ANCIENT_DEBRIS).requiresCorrectToolForDrops().strength(1.6f));
    public static final DeferredHolder<Block, Block> SUEVITE_SMALL_TILE = registerBlock("suevite_small_tile", () -> new Block(Properties.of().mapColor(MapColor.TERRACOTTA_GRAY).sound(SoundType.ANCIENT_DEBRIS).requiresCorrectToolForDrops().strength(1.6f)));
    public static final StoneDecoBlocks SUEVITE_SMALL_TILE_DECO = new StoneDecoBlocks("suevite_small_tile", SUEVITE_SMALL_TILE, Properties.of().mapColor(MapColor.TERRACOTTA_GRAY).sound(SoundType.ANCIENT_DEBRIS).requiresCorrectToolForDrops().strength(1.6f));

    public static final DeferredHolder<Block, Block> GLASS_AETHERIUM = registerBlock("glass_aetherium", () -> new StainedGlassBlock(DyeColor.LIGHT_BLUE, Properties.ofFullCopy(Blocks.LIGHT_BLUE_STAINED_GLASS).explosionResistance(1800000).requiresCorrectToolForDrops()));
    public static final DeferredHolder<Block, Block> GLASS_AETHERIUM_BORDERLESS = registerBlock("glass_aetherium_borderless", () -> new StainedGlassBlock(DyeColor.LIGHT_BLUE, Properties.ofFullCopy(GLASS_AETHERIUM.get())));

    public static final DeferredHolder<Block, Block> AETHERIUM_ORE = registerBlock("ore_aether", () -> new AetherOreBlock(Properties.ofFullCopy(Blocks.STONE).sound(SoundType.ANCIENT_DEBRIS).requiresCorrectToolForDrops().strength(5, 12), UniformInt.of(4, 8)));
    public static final DeferredHolder<Block, Block> AETHERIUM_SHARD_BLOCK = registerBlock("block_shards_raw", () -> new AetherBlock(Properties.ofFullCopy(Blocks.STONE).sound(SoundType.ANCIENT_DEBRIS).requiresCorrectToolForDrops().strength(5, 12)));
    public static final DeferredHolder<Block, Block> AETHERIUM_BLOCK = registerBlock("block_aether", () -> new AetherBlock(Properties.ofFullCopy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().strength(10, 40)));

    public static final DeferredHolder<Item, Item> AETHER_SHARD = ITEMS.register("aether_shard", () -> new TooltipItem(new Item.Properties(), "aetherworks.tooltip.shard", false));
    public static final DeferredHolder<Item, Item> AETHER_AMALGAM = ITEMS.register("aether_amalgam", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> AETHER_PEARL = ITEMS.register("aether_pearl", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredHolder<Item, Item> FOCUS_CRYSTAL = ITEMS.register("focus_crystal", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> AETHERIUM_LENS = ITEMS.register("aetherium_lens", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> PLATE_AETHER = ITEMS.register("plate_aether", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> INGOT_AETHER = ITEMS.register("ingot_aether", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> GEM_AETHER = ITEMS.register("gem_aether", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> TOOL_ROD_CRUDE = ITEMS.register("tool_rod_crude", () -> new TooltipItem(new Item.Properties(), "aetherworks.tooltip.must_work_crude", false));
    public static final DeferredHolder<Item, Item> TOOL_ROD = ITEMS.register("tool_rod", () -> new TooltipItem(new Item.Properties(), "aetherworks.tooltip.must_work_uninfused", false));
    public static final DeferredHolder<Item, Item> TOOL_ROD_INFUSED = ITEMS.register("tool_rod_infused", () -> new Item(new Item.Properties().component(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));
    public static final DeferredHolder<Item, Item> PICKAXE_HEAD_CRUDE = ITEMS.register("pickaxe_head_crude", () -> new TooltipItem(new Item.Properties(), "aetherworks.tooltip.must_work_crude", false));
    public static final DeferredHolder<Item, Item> PICKAXE_HEAD = ITEMS.register("pickaxe_head", () -> new TooltipItem(new Item.Properties(), "aetherworks.tooltip.must_work_uninfused", false));
    public static final DeferredHolder<Item, Item> PICKAXE_HEAD_AETHER = ITEMS.register("pickaxe_head_aether", () -> new TooltipItem(new Item.Properties(), "aetherworks.tooltip.pickaxe_head_aether", false));
    public static final DeferredHolder<Item, Item> PICKAXE_HEAD_EMBER = ITEMS.register("pickaxe_head_ember", () -> new TooltipItem(new Item.Properties(), "aetherworks.tooltip.pickaxe_head_ember", false));
    public static final DeferredHolder<Item, Item> AXE_HEAD_CRUDE = ITEMS.register("axe_head_crude", () -> new TooltipItem(new Item.Properties(), "aetherworks.tooltip.must_work_crude", false));
    public static final DeferredHolder<Item, Item> AXE_HEAD = ITEMS.register("axe_head", () -> new TooltipItem(new Item.Properties(), "aetherworks.tooltip.must_work_uninfused", false));
    public static final DeferredHolder<Item, Item> AXE_HEAD_SCULK = ITEMS.register("axe_head_sculk", () -> new TooltipItem(new Item.Properties(), "aetherworks.tooltip.axe_head_sculk", false));
    public static final DeferredHolder<Item, Item> AXE_HEAD_ENDER = ITEMS.register("axe_head_ender", () -> new TooltipItem(new Item.Properties(), "aetherworks.tooltip.axe_head_ender", false));
    public static final DeferredHolder<Item, Item> SHOVEL_HEAD_CRUDE = ITEMS.register("shovel_head_crude", () -> new TooltipItem(new Item.Properties(), "aetherworks.tooltip.must_work_crude", false));
    public static final DeferredHolder<Item, Item> SHOVEL_HEAD = ITEMS.register("shovel_head", () -> new TooltipItem(new Item.Properties(), "aetherworks.tooltip.must_work_uninfused", false));
    public static final DeferredHolder<Item, Item> SHOVEL_HEAD_PRISMARINE = ITEMS.register("shovel_head_prismarine", () -> new TooltipItem(new Item.Properties(), "aetherworks.tooltip.shovel_head_prismarine", false));
    public static final DeferredHolder<Item, Item> SHOVEL_HEAD_SLIME = ITEMS.register("shovel_head_slime", () -> new TooltipItem(new Item.Properties(), "aetherworks.tooltip.shovel_head_slime", false));
    public static final DeferredHolder<Item, Item> HOE_HEAD_CRUDE = ITEMS.register("hoe_head_crude", () -> new TooltipItem(new Item.Properties(), "aetherworks.tooltip.must_work_crude", false));
    public static final DeferredHolder<Item, Item> HOE_HEAD = ITEMS.register("hoe_head", () -> new TooltipItem(new Item.Properties(), "aetherworks.tooltip.must_work_uninfused", false));
    public static final DeferredHolder<Item, Item> HOE_HEAD_HONEY = ITEMS.register("hoe_head_honey", () -> new TooltipItem(new Item.Properties(), "aetherworks.tooltip.hoe_head_honey", false));
    public static final DeferredHolder<Item, Item> HOE_HEAD_AMETHYST = ITEMS.register("hoe_head_amethyst", () -> new TooltipItem(new Item.Properties(), "aetherworks.tooltip.hoe_head_amethyst", false));
    public static final DeferredHolder<Item, Item> AETHER_CROWN_CRUDE = ITEMS.register("aether_crown_crude", () -> new TooltipItem(new Item.Properties(), "aetherworks.tooltip.must_work_crude", false));
    public static final DeferredHolder<Item, Item> AETHER_CROWN_MUNDANE = ITEMS.register("aether_crown_mundane", () -> new TooltipItem(new Item.Properties(), "aetherworks.tooltip.must_work_uninfused", false));
    public static final DeferredHolder<Item, Item> CROSSBOW_FRAME_CRUDE = ITEMS.register("crossbow_frame_crude", () -> new TooltipItem(new Item.Properties(), "aetherworks.tooltip.must_work_crude", false));
    public static final DeferredHolder<Item, Item> CROSSBOW_FRAME = ITEMS.register("crossbow_frame", () -> new TooltipItem(new Item.Properties(), "aetherworks.tooltip.must_work_uninfused", false));
    public static final DeferredHolder<Item, Item> CROSSBOW_FRAME_INFUSED = ITEMS.register("crossbow_frame_infused", () -> new Item(new Item.Properties().component(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));
    public static final DeferredHolder<Item, Item> CROSSBOW_LIMBS_CRUDE = ITEMS.register("crossbow_limbs_crude", () -> new TooltipItem(new Item.Properties(), "aetherworks.tooltip.must_work_crude", false));
    public static final DeferredHolder<Item, Item> CROSSBOW_LIMBS = ITEMS.register("crossbow_limbs", () -> new TooltipItem(new Item.Properties(), "aetherworks.tooltip.must_work_uninfused", false));
    public static final DeferredHolder<Item, Item> CROSSBOW_LIMBS_QUARTZ = ITEMS.register("crossbow_limbs_quartz", () -> new TooltipItem(new Item.Properties(), "aetherworks.tooltip.crossbow_limbs_quartz", false));
    public static final DeferredHolder<Item, Item> CROSSBOW_LIMBS_MAGMA = ITEMS.register("crossbow_limbs_magma", () -> new TooltipItem(new Item.Properties(), "aetherworks.tooltip.crossbow_limbs_magma", false));
    public static final DeferredHolder<Item, Item> SHIELD_CORE_CRUDE = ITEMS.register("shield_core_crude", () -> new TooltipItem(new Item.Properties(), "aetherworks.tooltip.must_work_crude", false));
    public static final DeferredHolder<Item, Item> SHIELD_CORE = ITEMS.register("shield_core", () -> new TooltipItem(new Item.Properties(), "aetherworks.tooltip.must_work_uninfused", false));
    public static final DeferredHolder<Item, Item> SHIELD_CORE_INFUSED = ITEMS.register("shield_core_infused", () -> new Item(new Item.Properties().component(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));

    public static final DeferredHolder<Item, Item> GEODE_END = ITEMS.register("geode_end", () -> new TooltipItem(new Item.Properties(), "aetherworks.tooltip.geode", false));
    public static final DeferredHolder<Item, Item> GEODE_NETHER = ITEMS.register("geode_nether", () -> new TooltipItem(new Item.Properties(), "aetherworks.tooltip.geode", false));
    public static final DeferredHolder<Item, Item> GEODE_HOT = ITEMS.register("geode_hot", () -> new TooltipItem(new Item.Properties(), "aetherworks.tooltip.geode", false));
    public static final DeferredHolder<Item, Item> GEODE_COLD = ITEMS.register("geode_cold", () -> new TooltipItem(new Item.Properties(), "aetherworks.tooltip.geode", false));
    public static final DeferredHolder<Item, Item> GEODE_MAGIC = ITEMS.register("geode_magical", () -> new TooltipItem(new Item.Properties(), "aetherworks.tooltip.geode", false));
    public static final DeferredHolder<Item, Item> GEODE_OCEAN = ITEMS.register("geode_ocean", () -> new TooltipItem(new Item.Properties(), "aetherworks.tooltip.geode", false));
    public static final DeferredHolder<Item, Item> GEODE_DEEP = ITEMS.register("geode_deep", () -> new TooltipItem(new Item.Properties(), "aetherworks.tooltip.geode", false));
    public static final DeferredHolder<Item, Item> GEODE_BASIC = ITEMS.register("geode_basic", () -> new TooltipItem(new Item.Properties(), "aetherworks.tooltip.geode", false));

    public static final DeferredHolder<Item, Item> AETHER_ASPECTUS = ITEMS.register("aspectus_aetherium", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> TUNING_CYLINDER = ITEMS.register("tuning_cylinder", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> VOLANT_CALCIFIER = ITEMS.register("volant_calcifier", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> AGRARIAN_LINERS = ITEMS.register("agrarian_liners", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> AETHERIAL_PLATING = ITEMS.register("aetherial_plating", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> LEXICON = ITEMS.register("lexicon", () -> new Lexicon(new Item.Properties().stacksTo(1)));

    //Augments
    public static final IAugment TUNING_CYLINDER_AUGMENT = AugmentUtil.registerAugment(new TuningCylinderAugment(ResourceLocation.fromNamespaceAndPath(Aetherworks.MODID, "tuning_cylinder")));
    public static final IAugment VOLANT_CALCIFIER_AUGMENT = AugmentUtil.registerAugment(new VolantCalcifierAugment(ResourceLocation.fromNamespaceAndPath(Aetherworks.MODID, "volant_calcifier")));
    public static final IAugment AGRARIAN_LINERS_AUGMENT = AugmentUtil.registerAugment(new AgrarianLinersAugment(ResourceLocation.fromNamespaceAndPath(Aetherworks.MODID, "agrarian_liners")));
    public static final IAugment AETHERIAL_PLATING_AUGMENT = AugmentUtil.registerAugment(new AetherPlatingAugment(ResourceLocation.fromNamespaceAndPath(Aetherworks.MODID, "aetherial_plating")));

    //Fluids
    public static final FluidStuff AETHERIUM_GAS_IMPURE = addFluid(new EmbersFluidType.FluidInfo("aether_gas_impure", 0xff6c829f, 0.1F, 1.5F),
            AWViscousFluidType::new, LiquidBlock::new,
            prop -> prop.explosionResistance(1000F).tickRate(3),
            FluidType.Properties.create()
                    .canSwim(false)
                    .canDrown(true)
                    .pathType(PathType.WATER)
                    .adjacentPathType(null)
                    .motionScale(0.05D)
                    .canPushEntity(false)
                    .canHydrate(false)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .density(800)
                    .viscosity(20)
                    .temperature(1020)
                    .lightLevel(6));
    public static final FluidStuff AETHERIUM_GAS = addFluid(new EmbersFluidType.FluidInfo("aether_gas", 0xff00b8ff, 0.1F, 1.5F),
            AWViscousFluidType::new, LiquidBlock::new,
            prop -> prop.explosionResistance(1000F).tickRate(3),
            FluidType.Properties.create()
                    .canSwim(false)
                    .canDrown(true)
                    .pathType(PathType.WATER)
                    .adjacentPathType(null)
                    .motionScale(0.0005D)
                    .canPushEntity(false)
                    .canHydrate(false)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .density(1)
                    .viscosity(10)
                    .temperature(1020)
                    .lightLevel(10));
    public static final FluidStuff SEETHING_AETHERIUM = addFluid(new EmbersFluidType.FluidInfo("aether_gas_painful", 0xff00b8ff, 0.1F, 1.5F),
            AWPainFluidType::new, LiquidBlock::new,
            prop -> prop.explosionResistance(1000F).tickRate(1).slopeFindDistance(2).levelDecreasePerBlock(2),
            FluidType.Properties.create()
                    .canSwim(false)
                    .canDrown(true)
                    .pathType(PathType.LAVA)
                    .adjacentPathType(null)
                    .motionScale(0.0023333333333333335D)
                    .canPushEntity(true)
                    .canHydrate(false)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_LAVA)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL_LAVA)
                    .density(100)
                    .viscosity(10)
                    .temperature(6840)
                    .lightLevel(5));
    public static final FluidStuff ALCHEMIC_PRECURSOR = addFluid(new EmbersFluidType.FluidInfo("alchemic_precursor", 0x9a5e45, 0.1F, 1.5F),
            AWMoltenMetalFluidType::new, LiquidBlock::new,
            prop -> prop.explosionResistance(1000F).tickRate(30).slopeFindDistance(2).levelDecreasePerBlock(2),
            FluidType.Properties.create()
                    .canSwim(true)
                    .canDrown(true)
                    .pathType(PathType.LAVA)
                    .adjacentPathType(null)
                    .motionScale(0.00233)
                    .canPushEntity(false)
                    .canHydrate(false)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_LAVA)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL_LAVA)
                    .density(2000)
                    .viscosity(6000)
                    .temperature(1100)
                    .lightLevel(12));

    //Block Entities
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PrismBlockEntity>> PRISM_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("prism_block_entity", () -> BlockEntityType.Builder.of(PrismBlockEntity::new, PRISM.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ForgeHeatVentBlockEntity>> FORGE_VENT_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("forge_vent_block_entity", () -> BlockEntityType.Builder.of(ForgeHeatVentBlockEntity::new, FORGE_VENT.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ForgeHeaterBlockEntity>> FORGE_HEATER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("forge_heater_block_entity", () -> BlockEntityType.Builder.of(ForgeHeaterBlockEntity::new, FORGE_HEATER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ForgeCoolerBlockEntity>> FORGE_COOLER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("forge_cooler_block_entity", () -> BlockEntityType.Builder.of(ForgeCoolerBlockEntity::new, FORGE_COOLER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HeatDialBlockEntity>> HEAT_DIAL_ENTITY = BLOCK_ENTITY_TYPES.register("heat_dial_block_entity", () -> BlockEntityType.Builder.of(HeatDialBlockEntity::new, HEAT_DIAL.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MetalFormerBlockEntity>> METAL_FORMER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("metal_former_block_entity", () -> BlockEntityType.Builder.of(MetalFormerBlockEntity::new, FORGE_METAL_FORMER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AetheriumAnvilBlockEntity>> AETHERIUM_ANVIL_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("aetherium_anvil_block_entity", () -> BlockEntityType.Builder.of(AetheriumAnvilBlockEntity::new, FORGE_ANVIL.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ToolStationBlockEntity>> TOOL_STATION_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("tool_station_block_entity", () -> BlockEntityType.Builder.of(ToolStationBlockEntity::new, FORGE_TOOL_STATION.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TopHammerBlockEntity>> TOP_HAMMERABLE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("top_hammerable_block_entity", () -> BlockEntityType.Builder.of(TopHammerBlockEntity::new, AETHER_FORGE_EDGE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AetherForgeBlockEntity>> AETHER_FORGE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("aether_forge_block_entity", () -> BlockEntityType.Builder.of(AetherForgeBlockEntity::new, AETHER_FORGE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AetherForgeTopBlockEntity>> AETHER_FORGE_TOP_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("aether_forge_top_block_entity", () -> BlockEntityType.Builder.of(AetherForgeTopBlockEntity::new, AETHER_FORGE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LexiconReceptacleBlockEntity>> LEXICON_RECEPTACLE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("lexicon_receptacle_block_entity", () -> BlockEntityType.Builder.of(LexiconReceptacleBlockEntity::new, LEXICON_RECEPTACLE.get()).build(null));

    //Creative Tabs
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> AW_TAB = CREATIVE_MODE_TAB.register("aetherworks_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(AWRegistry.AETHER_SHARD.get()))
                    .title(Component.translatable("itemgroup." + Aetherworks.MODID))
                    .displayItems((params, output) -> {
                        for (DeferredHolder<Item, ? extends Item> item : ITEMS.getEntries()) {
                            output.accept(item.get());

                            if (item.get() instanceof EmberStorageItem)
                                output.accept(EmberStorageItem.withFill(item.get(), ((EmberStorageItem) item.get()).getCapacity()));
                        }
                        PotionGemItem.getAllPotionGems(output);
                    })
                    .build());

    //Entities
    public static final DeferredHolder<EntityType<?>, EntityType<DummyArmorLoaderEntity>> DUMMY_LOADER = registerEntity("dummy_loader", EntityType.Builder.<DummyArmorLoaderEntity>of(DummyArmorLoaderEntity::new, MobCategory.MISC).sized(0.0F, 0F));

    //Attributes

    //Spawn Eggs

    //Particle Types

    //Mob Effects & Potions
    public static final DeferredHolder<MobEffect, MobEffect> EFFECT_MOONFIRE = MOB_EFFECTS.register("moonfire", () -> new MoonfireEffect(MobEffectCategory.HARMFUL, Utils.AETHERIUM_PROJECTILE_COLOR.getRGB()));
    public static final DeferredHolder<MobEffect, MobEffect> EFFECT_PULLDOWN = MOB_EFFECTS.register("pulldown", () -> new PullDownEffect(MobEffectCategory.NEUTRAL, PullDownEffect.COLOR.getRGB()));

    //Recipe Types
    public static final DeferredHolder<RecipeType<?>, RecipeType<IMetalFormerRecipe>> METAL_FORMING = registerRecipeType("metal_forming");
    public static final DeferredHolder<RecipeType<?>, RecipeType<IAetheriumAnvilRecipe>> AETHERIUM_ANVIL = registerRecipeType("aetherium_anvil");
    public static final DeferredHolder<RecipeType<?>, RecipeType<IToolStationRecipe>> TOOL_STATION_RECIPE = registerRecipeType("tool_station");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PotionGemSocketRecipe>> GEM_SOCKET_SERIALIZER = RECIPE_SERIALIZERS.register("potion_gem_socket", () -> PotionGemSocketRecipe.SERIALIZER);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PotionGemUnsocketRecipe>> GEM_UNSOCKET_SERIALIZER = RECIPE_SERIALIZERS.register("potion_gem_unsocket", () -> PotionGemUnsocketRecipe.SERIALIZER);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PotionGemImbueRecipe>> GEM_IMBUE_SERIALIZER = RECIPE_SERIALIZERS.register("potion_gem_imbue", () -> PotionGemImbueRecipe.SERIALIZER);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<DrainRecipe>> DRAIN_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register("drain_shovel", () -> DrainRecipe.SERIALIZER);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<LexiconRecipe>> FILL_LEXICON_SERIALIZER = RECIPE_SERIALIZERS.register("fill_lexicon", () -> LexiconRecipe.SERIALIZER);

    //Recipe Serializers
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MetalFormerRecipe>> METAL_FORMING_SERIALIZER = RECIPE_SERIALIZERS.register("metal_forming", () -> MetalFormerRecipe.SERIALIZER);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AetheriumAnvilRecipe>> AETHERIUM_ANVIL_SERIALIZER = RECIPE_SERIALIZERS.register("aetherium_anvil", () -> AetheriumAnvilRecipe.SERIALIZER);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ToolStationRecipe>> TOOL_STATION_SERIALIZER = RECIPE_SERIALIZERS.register("tool_station", () -> ToolStationRecipe.SERIALIZER);

    //Structures
    public static final DeferredHolder<StructureType<?>, StructureType<MeteorStructure>> METEOR_STRUCTURE = STRUCTURE_TYPES.register("meteor", () -> MeteorStructure.TYPE);

    //Structure Pieces
    public static final DeferredHolder<StructurePieceType, StructurePieceType> METEOR_STRUCTURE_PIECE = STRUCTURE_PIECES.register("meteor", () -> MeteorStructurePiece.TYPE);

    public static void init(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            DispenseItemBehavior dispenseBucket = new DefaultDispenseItemBehavior() {
                private final DefaultDispenseItemBehavior defaultDispenseItemBehavior = new DefaultDispenseItemBehavior();

                @Override
                public @Nonnull ItemStack execute(BlockSource source, ItemStack stack) {
                    DispensibleContainerItem container = (DispensibleContainerItem)stack.getItem();
                    BlockPos blockpos = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
                    Level level = source.level();
                    if (container.emptyContents(null, level, blockpos, null)) {
                        container.checkExtraContent(null, level, stack, blockpos);
                        return new ItemStack(Items.BUCKET);
                    } else {
                        return this.defaultDispenseItemBehavior.dispense(source, stack);
                    }
                }
            };
            for (FluidStuff fluid : fluidList) {
                DispenserBlock.registerBehavior(fluid.FLUID_BUCKET.get(), dispenseBucket);
            }

            EmbersAPI.registerEmberResonance(Ingredient.of(PICKAXE_EMBER.get(), PICKAXE_AETHER.get(), AXE_ENDER.get(), AXE_SCULK.get(), SHOVEL_PRISMARINE.get(), SHOVEL_SLIME.get(), CROSSBOW_MAGMA.get(), CROSSBOW_QUARTZ.get(), HOE_AMETHYST.get(), HOE_HONEY.get()), 2.5);
            EmbersAPI.registerEmberResonance(Ingredient.of(AETHER_CROWN.get()), 2.5);
            EmbersAPI.registerWearableLens(Ingredient.of(AETHER_CROWN.get())); //of course it's a lens!

            FluidInteractionRegistry.addInteraction(AETHERIUM_GAS.FLUID.get().getFluidType(), new FluidInteractionRegistry.InteractionInformation(NeoForgeMod.WATER_TYPE.value(),
                    fluidState -> fluidState.isSource() ? SUEVITE.get().defaultBlockState() : SUEVITE_COBBLE.get().defaultBlockState()));
        });
    }

    /**
     * SMP 2 port: the block entities still answer the old getCapability(Capability, Direction) through Embers'
     * legacy layer; this exposes those answers as NeoForge capabilities (pipes, hoppers, ember network), the same
     * bridge Embers Re-Ignited registers for its own block entities.
     */
    @SuppressWarnings("unchecked")
    public static void registerCapabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
        for (DeferredHolder<BlockEntityType<?>, ? extends BlockEntityType<?>> entry : BLOCK_ENTITY_TYPES.getEntries()) {
            BlockEntityType<net.minecraft.world.level.block.entity.BlockEntity> type = (BlockEntityType<net.minecraft.world.level.block.entity.BlockEntity>) entry.get();
            event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, type, (be, side) ->
                    CapabilityCompat.getCapability(be, com.rekindled.embers.compat.legacy.capabilities.ForgeCapabilities.ITEM_HANDLER, side).orElse(null));
            event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, type, (be, side) ->
                    CapabilityCompat.getCapability(be, com.rekindled.embers.compat.legacy.capabilities.ForgeCapabilities.FLUID_HANDLER, side).orElse(null));
            event.registerBlockEntity(com.rekindled.embers.api.capabilities.EmbersCapabilities.EMBER_BLOCK_CAPABILITY, type, (be, side) ->
                    CapabilityCompat.getCapability(be, com.rekindled.embers.api.capabilities.EmbersCapabilities.EMBER_CAPABILITY, side).orElse(null));
        }
        // Prismarine Shovel: fluid tank in the item
        event.registerItem(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM, (stack, ctx) ->
                new net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStack(FLUID_CONTENTS, stack,
                        AWConfig.PRISMARINE_SHOVEL_CAPACITY.get() * 1000), SHOVEL_PRISMARINE.get());
    }

    public static <T extends Block> DeferredHolder<Block, T> registerBlock(String name, Supplier<T> block)
    {
        DeferredHolder<Block, T> ret = BLOCKS.register(name, block);
        registerBlockItem(name, ret);
        return ret;
    }

    private static DeferredHolder<Item, Item> registerBlockItem(String name, DeferredHolder<Block, ? extends Block> block)
    {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void RegisterEnabledGeodes()
    {

    }

    public static class FluidStuff {

        public final BaseFlowingFluid.Properties PROPERTIES;

        public final DeferredHolder<Fluid, BaseFlowingFluid.Source> FLUID;
        public final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLUID_FLOW;
        public final DeferredHolder<FluidType, FluidType> TYPE;

        public final DeferredHolder<Block, LiquidBlock> FLUID_BLOCK;

        public final DeferredHolder<Item, BucketItem> FLUID_BUCKET;

        public final String name;
        public final int color;

        public FluidStuff(String name, int color, FluidType type,
                          BiFunction<FlowingFluid, Properties, LiquidBlock> block,
                          @Nullable Consumer<BaseFlowingFluid.Properties> fluidProperties,
                          Function<BaseFlowingFluid.Properties, BaseFlowingFluid.Source> source,
                          Function<BaseFlowingFluid.Properties, BaseFlowingFluid.Flowing> flowing) {
            this.name = name;
            this.color = color;

            FLUID = FLUIDS.register(name, () -> source.apply(getFluidProperties()));
            FLUID_FLOW = FLUIDS.register("flowing_" + name, () -> flowing.apply(getFluidProperties()));
            TYPE = FLUIDTYPES.register(name, () -> type);

            PROPERTIES = new BaseFlowingFluid.Properties(TYPE, FLUID, FLUID_FLOW);
            if (fluidProperties != null)
                fluidProperties.accept(PROPERTIES);

            FLUID_BLOCK = BLOCKS.register(name + "_block", () -> block.apply(FLUID.get(), Block.Properties.of().liquid().pushReaction(PushReaction.DESTROY).lightLevel((state) -> { return type.getLightLevel(); }).randomTicks().replaceable().strength(100.0F).noLootTable()));
            FLUID_BUCKET = ITEMS.register(name + "_bucket", () -> new BucketItem(FLUID.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

            PROPERTIES.bucket(FLUID_BUCKET).block(FLUID_BLOCK);
        }

        public BaseFlowingFluid.Properties getFluidProperties() {
            return PROPERTIES;
        }
    }

    public static class StoneDecoBlocks {
        public String name;
        public DeferredHolder<Block, Block> block;
        public DeferredHolder<Block, StairBlock> stairs;
        public DeferredHolder<Item, Item> stairsItem;
        public DeferredHolder<Block, SlabBlock> slab;
        public DeferredHolder<Item, Item> slabItem;
        public DeferredHolder<Block, WallBlock> wall;
        public DeferredHolder<Item, Item> wallItem;

        public StoneDecoBlocks(String name, DeferredHolder<Block, Block> block, Properties properties, boolean stairs, boolean slab, boolean wall) {
            this.stairs = null;
            this.stairsItem = null;
            this.slab = null;
            this.slabItem = null;
            this.wall = null;
            this.wallItem = null;
            this.name = name;
            this.block = block;
            if (stairs) {
                this.stairs = BLOCKS.register(name + "_stairs", () ->
                        new StairBlock(block.get().defaultBlockState(), properties) {});
                this.stairsItem = registerBlockItem(name + "_stairs", this.stairs);
            }

            if (slab) {
                this.slab = BLOCKS.register(name + "_slab", () -> new SlabBlock(properties));
                this.slabItem = registerBlockItem(name + "_slab", this.slab);
            }

            if (wall) {
                this.wall = BLOCKS.register(name + "_wall", () -> new WallBlock(properties));
                this.slabItem = registerBlockItem(name + "_wall", this.wall);
            }

        }

        public StoneDecoBlocks(String name, DeferredHolder<Block, Block> block, Properties properties) {
            this(name, block, properties, true, true, true);
        }
    }

    public static <T extends Recipe<?>> DeferredHolder<RecipeType<?>, RecipeType<T>> registerRecipeType(final String identifier) {
        return RECIPE_TYPES.register(identifier, () -> new RecipeType<T>() {
            public String toString() {
                return Aetherworks.MODID + ":" + identifier;
            }
        });
    }
    public static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> registerEntity(String name, EntityType.Builder<T> builder) {
        return ENTITY_TYPES.register(name, () -> builder.build(Aetherworks.MODID + ":" + name));
    }
}
