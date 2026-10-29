package net.sirplop.aetherworks.entity.render;

import com.rekindled.embers.Embers;
import com.rekindled.embers.entity.AncientGolemEntity;
import com.rekindled.embers.model.AncientGolemModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.sirplop.aetherworks.Aetherworks;
import net.sirplop.aetherworks.entity.DummyArmorLoaderEntity;
import net.sirplop.aetherworks.model.AetherCrownModel;
import net.sirplop.aetherworks.model.DummyLoaderModel;

@OnlyIn(Dist.CLIENT)
public class DummyAetherCrownRender  extends MobRenderer<DummyArmorLoaderEntity, DummyLoaderModel<DummyArmorLoaderEntity>> {

    public DummyAetherCrownRender(EntityRendererProvider.Context context) {
        super(context, new DummyLoaderModel<>(),0f);
        //hooking into a dummy renderer so I can initialize the crown because this mod has no entities yet.
        AetherCrownModel.init(context);
    }

    @Override
    public ResourceLocation getTextureLocation(DummyArmorLoaderEntity dummyArmorLoaderEntity) {
        return null;
    }
}

