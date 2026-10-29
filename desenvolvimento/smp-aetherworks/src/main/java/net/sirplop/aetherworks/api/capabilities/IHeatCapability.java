package net.sirplop.aetherworks.api.capabilities;

import net.minecraft.nbt.CompoundTag;
import com.rekindled.embers.compat.legacy.capabilities.ICapabilitySerializable;

public interface IHeatCapability  extends ICapabilitySerializable<CompoundTag> {
    String HEAT_CAPACITY = "aetherworks:heat_capacity";
    String HEAT = "aetherworks:heat";

    double getHeat();

    double getHeatCapacity();

    void setHeat(double value);

    void setHeatCapacity(double value);

    double addAmount(double value, boolean doAdd);

    double removeAmount(double value, boolean doRemove);

    void writeToNBT(CompoundTag vnbt);

    void onContentsChanged();

    void invalidate();
}
