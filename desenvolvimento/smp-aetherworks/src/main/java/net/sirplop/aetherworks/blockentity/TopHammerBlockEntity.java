package net.sirplop.aetherworks.blockentity;

import com.rekindled.embers.api.tile.IHammerable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.sirplop.aetherworks.AWRegistry;
import net.sirplop.aetherworks.api.tile.ITopHammerable;

public class TopHammerBlockEntity extends BlockEntity implements IHammerable {
    public TopHammerBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(AWRegistry.TOP_HAMMERABLE_BLOCK_ENTITY.get(), pPos, pBlockState);
    }

    @Override
    public void onHit(BlockEntity hammer) {
        if (this.getLevel().getBlockEntity(this.getBlockPos().above()) instanceof ITopHammerable ham)
            ham.onHit(hammer);
    }

    public boolean isValid() {
        if (this.getLevel().getBlockEntity(this.getBlockPos().above()) instanceof ITopHammerable target)
            return target.isValid();
        return false;
    }
}
