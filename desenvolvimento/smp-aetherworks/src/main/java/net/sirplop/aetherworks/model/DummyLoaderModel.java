package net.sirplop.aetherworks.model;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;

public class DummyLoaderModel <T extends Entity> extends HierarchicalModel<T> {
    @Override
    public ModelPart root() {
        return null;
    }

    @Override
    public void setupAnim(T t, float v, float v1, float v2, float v3, float v4) {
        return;
    }
}
