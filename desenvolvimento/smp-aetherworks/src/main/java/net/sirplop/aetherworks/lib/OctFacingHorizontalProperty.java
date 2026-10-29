package net.sirplop.aetherworks.lib;

import net.minecraft.world.level.block.state.properties.EnumProperty;

import java.util.Arrays;
import java.util.Collection;

public class OctFacingHorizontalProperty  extends EnumProperty<OctDirection> {
    protected OctFacingHorizontalProperty(String pName, Collection<OctDirection> pValues) {
        super(pName, OctDirection.class, pValues);
    }
    public static OctFacingHorizontalProperty create(String pName, Collection<OctDirection> pValues) {
        return new OctFacingHorizontalProperty(pName, pValues);
    }

    public static final OctFacingHorizontalProperty OCT_DIRECTIONS = create("oct_facing", Arrays.stream(OctDirection.values()).toList());
}
