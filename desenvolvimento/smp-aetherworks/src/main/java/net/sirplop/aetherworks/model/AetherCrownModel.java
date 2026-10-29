package net.sirplop.aetherworks.model;
import net.sirplop.aetherworks.util.Compat121;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.sirplop.aetherworks.Aetherworks;
import javax.annotation.Nonnull;

import java.util.EnumMap;
import java.util.Map;

public class AetherCrownModel extends HumanoidModel<LivingEntity> {
    public static final ModelLayerLocation CROWN_HEAD = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Aetherworks.MODID, "aether_crown"), "head");

    public static final Map<EquipmentSlot, AetherCrownModel> ARMOR_MODELS = new EnumMap<>(EquipmentSlot.class);
    public static final IClientItemExtensions ARMOR_MODEL_GETTER = new IClientItemExtensions() {
        @Override
        public @Nonnull HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
            AetherCrownModel model = AetherCrownModel.ARMOR_MODELS.get(equipmentSlot);
            model.setup(livingEntity, itemStack);
            return model;
        }
    };

    public ModelPart gem = null;

    public AetherCrownModel(ModelPart root, EquipmentSlot slot) {
        super(root);
        if (this.head.hasChild("gem")) {
            gem = this.head.getChild("gem");
        }
    }
    public void setup(LivingEntity entity, ItemStack itemStack) {
        if (gem != null) {
            gem.visible = Compat121.tag(itemStack).contains("gem");
        }
    }

    public static void init(EntityRendererProvider.Context context) {
        ModelPart mesh = context.bakeLayer(CROWN_HEAD);
        ARMOR_MODELS.put(EquipmentSlot.HEAD, new AetherCrownModel(mesh, EquipmentSlot.HEAD));
    }

    public static MeshDefinition createHeadMesh() {
        CubeDeformation scale = new CubeDeformation(0.01F);
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.ZERO);

        head.addOrReplaceChild("left_side_1", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-1.0F, 0, 0, 0.5f, 1, 1, scale), PartPose.offset(-3.52F, -5.0F, 2.5F));

        head.addOrReplaceChild("left_side_2", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-1.0F, 0, 0, 0.5f, 1, 4, scale), PartPose.offset(-3.52F, -6.0F, -0.5F));

        head.addOrReplaceChild("left_side_3", CubeListBuilder.create().texOffs(4, 0)
                .addBox(-1.0f, 0, 0, 0.5f, 1, 5, scale), PartPose.offset(-3.52F, -7.0F, -3.5F));
        head.addOrReplaceChild("left_side_4", CubeListBuilder.create().texOffs(10, 0)
                .addBox(-1.0f, 0, 0, 0.50f, 1, 4, scale), PartPose.offset(-3.52F, -8.0F, -4.5F));
        head.addOrReplaceChild("front_left_side", CubeListBuilder.create().texOffs(15, 0)
                .addBox(-1.0f, 0, 0f, 2f, 1, 0.5f, scale), PartPose.offset(-3.0F, -8.0F, -4.5F));

        head.addOrReplaceChild("spike_left_1", CubeListBuilder.create().texOffs(5, 0)
                .addBox(-1.0f, 0, 0, 0.50f, 1, 1, scale), PartPose.offset(-3.52F, -9.0F, -2.0F));
        head.addOrReplaceChild("spike_left_2", CubeListBuilder.create().texOffs(5, 2)
                .addBox(-1.0f, 0, 0, 0.50f, 1, 1, scale), PartPose.offset(-3.52F, -9.0F, -3.5F));

        head.addOrReplaceChild("spike_front_1", CubeListBuilder.create().texOffs(10, 0)
                .addBox(-1.0f, 0, 0f, 1f, 1, 0.5f, scale), PartPose.offset(-3.52F, -9.0F, -4.5F));
        head.addOrReplaceChild("spike_front_2", CubeListBuilder.create().texOffs(10, 2)
                .addBox(-1.0f, 0, 0f, 1f, 1, 0.5f, scale), PartPose.offset(-2.0F, -9.0F, -4.5F));


        head.addOrReplaceChild("right_side_1", CubeListBuilder.create().texOffs(0, 0)
                .addBox(0.0F, 0, 0, 0.5f, 1, 1, scale), PartPose.offset(4.02F, -5.0F, 2.5F));

        head.addOrReplaceChild("right_side_2", CubeListBuilder.create().texOffs(0, 0)
                .addBox(0.0F, 0, 0, 0.5f, 1, 4, scale), PartPose.offset(4.02F, -6.0F, -0.5F));

        head.addOrReplaceChild("right_side_3", CubeListBuilder.create().texOffs(4, 0)
                .addBox(0.0f, 0, 0, 0.5f, 1, 5, scale), PartPose.offset(4.02F, -7.0F, -3.5F));
        head.addOrReplaceChild("right_side_4", CubeListBuilder.create().texOffs(10, 0)
                .addBox(0.0f, 0, 0, 0.50f, 1, 4, scale), PartPose.offset(4.02F, -8.0F, -4.5F));
        head.addOrReplaceChild("front_right_side", CubeListBuilder.create().texOffs(15, 0)
                .addBox(0.0f, 0, 0f, 2f, 1, 0.5f, scale), PartPose.offset(2F, -8.0F, -4.5F));

        head.addOrReplaceChild("spike_right_1", CubeListBuilder.create().texOffs(5, 0)
                .addBox(0.0f, 0, 0, 0.50f, 1, 1, scale), PartPose.offset(4.02F, -9.0F, -2.0F));
        head.addOrReplaceChild("spike_right_2", CubeListBuilder.create().texOffs(5, 2)
                .addBox(0.0f, 0, 0, 0.50f, 1, 1, scale), PartPose.offset(4.02F, -9.0F, -3.5F));

        head.addOrReplaceChild("spike_front_3", CubeListBuilder.create().texOffs(10, 0)
                .addBox(0.0f, 0, 0f, 1f, 1, 0.5f, scale), PartPose.offset(3.52f, -9.0F, -4.5F));
        head.addOrReplaceChild("spike_front_4", CubeListBuilder.create().texOffs(10, 2)
                .addBox(0.0f, 0, 0f, 1f, 1, 0.5f, scale), PartPose.offset(2F, -9.0F, -4.5F));

        head.addOrReplaceChild("front_center_left", CubeListBuilder.create().texOffs(0, 6)
                .addBox(0.0f, 0, 0f, 1f, 2f, 1f, scale), PartPose.offset(-2F, -8.5F, -5F));
        head.addOrReplaceChild("front_center_right", CubeListBuilder.create().texOffs(4, 6)
                .addBox(0.0f, 0, 0f, 1f, 2f, 1f, scale), PartPose.offset(1F, -8.5F, -5F));

        head.addOrReplaceChild("front_center_top", CubeListBuilder.create().texOffs(0, 9)
                .addBox(0.0f, 0, 0f, 2f, 1f, 1f, scale), PartPose.offset(-1, -9.5F, -5F));
        head.addOrReplaceChild("front_center_bottom", CubeListBuilder.create().texOffs(6, 9)
                .addBox(0.0f, 0, 0f, 2f, 1f, 1f, scale), PartPose.offset(-1, -6.5F, -5F));

        head.addOrReplaceChild("gem", CubeListBuilder.create().texOffs(8, 6)
                .addBox(0.0f, 0, 0f, 2f, 2f, 1f, scale), PartPose.offset(-1, -8.5F, -5.5F));
        head.addOrReplaceChild("gem_back", CubeListBuilder.create().texOffs(12, 9)
                .addBox(0.0f, 0, 0f, 2f, 2f, 0f, scale), PartPose.offset(-1, -8.5F, -4F));


        return mesh;
    }
}
