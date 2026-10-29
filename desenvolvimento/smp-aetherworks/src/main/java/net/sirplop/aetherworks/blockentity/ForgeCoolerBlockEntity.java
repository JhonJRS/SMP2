package net.sirplop.aetherworks.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.sirplop.aetherworks.AWRegistry;
import net.sirplop.aetherworks.datagen.AWBlockTags;
import net.sirplop.aetherworks.datagen.AWFluidTags;

public class ForgeCoolerBlockEntity  extends HeaterBaseBlockEntity{
    public ForgeCoolerBlockEntity(BlockPos pos, BlockState state) {
        super(AWRegistry.FORGE_COOLER_BLOCK_ENTITY.get(), pos, state, -500, 1000,
                Fluids.WATER, AWBlockTags.FORGE_COOLER_BELOW, AWFluidTags.FORGE_COOLER_BELOW,
                true, 1000, true, 1200, SoundEvents.LAVA_EXTINGUISH);
    }
}
