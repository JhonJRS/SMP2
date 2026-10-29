package net.sirplop.aetherworks.api.tile;

import net.minecraft.world.level.block.entity.BlockEntity;

//duplicated IHammerable to make aether anvil stuff function good
public interface ITopHammerable {

    void onHit(BlockEntity var1);

    default boolean isValid() {
        return true;
    }
}
