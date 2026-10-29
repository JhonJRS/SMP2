package net.sirplop.aetherworks.capabilities;

import com.rekindled.embers.compat.legacy.capabilities.Capability;
import com.rekindled.embers.compat.legacy.capabilities.CapabilityManager;
import com.rekindled.embers.compat.legacy.capabilities.CapabilityToken;
import net.sirplop.aetherworks.api.capabilities.IAetheriometerCap;
import net.sirplop.aetherworks.api.capabilities.IHeatCapability;

public class AWCapabilities {

    public static final Capability<IHeatCapability> HEAT_CAPABILITY = CapabilityManager.get(new CapabilityToken<>(){});
    public static final Capability<IAetheriometerCap> AETHERIOMETER_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});
}
