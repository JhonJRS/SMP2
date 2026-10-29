package net.sirplop.aetherworks.blockentity;

import com.rekindled.embers.api.power.IEmberCapability;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.sirplop.aetherworks.api.capabilities.IHeatCapability;

import java.util.List;

public interface IForge {

    BlockEntity getOwner();
    IHeatCapability getHeatCapability();
    IEmberCapability getEmberCapability();
    default void transferHeat(float value)
    {
        this.transferHeat(value, false);
    }

    void transferHeat(float value, boolean immediate);

    boolean canFunction();

    List<IFluidHandler> getAttachedFluidHandlers();
    List<IForgePart> getParts();
}
