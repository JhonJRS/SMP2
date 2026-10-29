package net.sirplop.aetherworks.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.sirplop.aetherworks.AWRegistry;
import net.sirplop.aetherworks.datagen.AWBlockTags;
import net.sirplop.aetherworks.datagen.AWFluidTags;

import java.util.ArrayList;

public class ForgeHeaterBlockEntity extends HeaterBaseBlockEntity {

    public ForgeHeaterBlockEntity(BlockPos pos, BlockState state) {
        super(AWRegistry.FORGE_HEATER_BLOCK_ENTITY.get(), pos, state, 1, 1,
                Fluids.WATER, AWBlockTags.FORGE_HEATER_BELOW, AWFluidTags.FORGE_HEATER_BELOW,
                false, 1, false, 0, null);
    }
}
