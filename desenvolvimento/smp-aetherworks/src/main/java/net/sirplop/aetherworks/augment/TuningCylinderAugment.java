package net.sirplop.aetherworks.augment;

import com.rekindled.embers.api.EmbersAPI;
import com.rekindled.embers.api.augment.AugmentUtil;
import com.rekindled.embers.augment.AugmentBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.sirplop.aetherworks.AWRegistry;
import net.sirplop.aetherworks.AWConfig;
import net.sirplop.aetherworks.datagen.AWBiomeTags;
import net.sirplop.aetherworks.datagen.AWBlockTags;
import net.sirplop.aetherworks.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class TuningCylinderAugment extends AugmentBase {
    public TuningCylinderAugment(ResourceLocation name) {
        super(name, 5.0);
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player.isCreative())
            return; //creative players don't drop anything!

        ItemStack heldStack = player.getMainHandItem();
        if (AugmentUtil.hasHeat(heldStack)) {
            Level world = (Level)event.getLevel();
            BlockPos pos = event.getPos();
            int level = AugmentUtil.getAugmentLevel(heldStack, this);
            if (!world.isClientSide() && level > 0 && Utils.blockHasTag(world.getBlockState(pos), AWBlockTags.DROPS_GEODES)) {
                double resonance = EmbersAPI.getEmberResonance(heldStack);
                if (world.random.nextInt(getChance(level, resonance)) == 0)
                    spawnGeode(world, pos);
            }
        }
    }

    private int getChance(int level, double resonance) {
        double bonus = Math.max(1, 1 + (resonance - 1) * 0.5);
        return Math.max(1, (int)Math.floor(AWConfig.AUGMENT_TUNING_CYLINDER_CHANCE.get() / (level * bonus)));
    }

    private void spawnGeode(Level level, BlockPos pos) {
        Holder<Biome> currentBiome = level.getBiome(pos);
        boolean isEnd = currentBiome.is(AWBiomeTags.TC_END_GEODES);
        boolean isNether = currentBiome.is(AWBiomeTags.TC_NETHER_GEODES);
        boolean isOcean = currentBiome.is(AWBiomeTags.TC_OCEAN_GEODES);
        boolean isHot = currentBiome.is(AWBiomeTags.TC_HOT_GEODES);
        boolean isCold = currentBiome.is(AWBiomeTags.TC_COLD_GEODES);
        boolean isMagic = currentBiome.is(AWBiomeTags.TC_MAGIC_GEODES);
        boolean isDeep = AWConfig.isDeepGeodeDimension(level.dimensionTypeRegistration().unwrapKey().orElseThrow()) &&
                pos.getY() < AWConfig.AUGMENT_TUNING_CYLINDER_BIOME_DEEP_DEPTH.get();

        ItemStack geode;
        List<Item> options = new ArrayList<>();
        if (isEnd) options.add(AWRegistry.GEODE_END.get());
        if (isNether) options.add(AWRegistry.GEODE_NETHER.get());
        if (isHot) options.add(AWRegistry.GEODE_HOT.get());
        if (isCold) options.add(AWRegistry.GEODE_COLD.get());
        if (isOcean) options.add(AWRegistry.GEODE_OCEAN.get());
        if (isMagic) options.add(AWRegistry.GEODE_MAGIC.get());

        if (!options.isEmpty()) {
            geode = new ItemStack(isDeep && level.random.nextFloat() < 0.5f ?
                    AWRegistry.GEODE_DEEP.get() : options.get(level.random.nextInt(options.size())), 1);
        } else if (isDeep) {
            geode = new ItemStack(AWRegistry.GEODE_DEEP.get(), 1);
        } else {
            geode = new ItemStack(AWRegistry.GEODE_BASIC.get(), 1);
        }

        Utils.dropItemIntoWorld(level, pos, geode);
    }
}
