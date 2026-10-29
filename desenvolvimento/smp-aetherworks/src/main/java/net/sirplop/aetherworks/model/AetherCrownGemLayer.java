package net.sirplop.aetherworks.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.rekindled.embers.EmbersClientEvents;
import com.rekindled.embers.util.Misc;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.sirplop.aetherworks.item.AetherCrownItem;
import net.sirplop.aetherworks.item.PotionGemItem;
import javax.annotation.Nonnull;

@OnlyIn(Dist.CLIENT)
public class AetherCrownGemLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
    public final AetherCrownModel model;

    public AetherCrownGemLayer(RenderLayerParent<T, M> pRenderer, EntityModelSet pModelSet) {
        super(pRenderer);
        model = new AetherCrownModel(pModelSet.bakeLayer(AetherCrownModel.CROWN_HEAD), EquipmentSlot.HEAD);
    }

    @Override
    public void render(@Nonnull PoseStack poseStack, @Nonnull MultiBufferSource multiBufferSource, int packedLight, LivingEntity livingEntity, float pLimbSwing,
                       float pLimbSwingAmount, float pPartialTicks, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
        //gotta be honest, don't entirely understand the armor renderer, so some of this is probably unnecessary
        ItemStack itemstack = livingEntity.getItemBySlot(EquipmentSlot.HEAD);
        Item item = itemstack.getItem();
        if (item instanceof AetherCrownItem crown) {
            this.getParentModel().copyPropertiesTo((HumanoidModel<T>) model);
            model.setAllVisible(false);
            model.head.visible = true;
            model.hat.visible = true;

            //render gem
            if (AetherCrownItem.hasAttachedGem(itemstack)) {
                int color = PotionGemItem.getColor(AetherCrownItem.getAttachedGem(itemstack));
                float timerSine = ((float)Math.sin(2.5 * Math.toRadians(EmbersClientEvents.ticks % 360)) + 1.0F) / 2.0F;
                float coeff = 1.125f - (timerSine * 0.1625f);

                float r = Math.min(1f, coeff * (float)(color >> 16 & 255) / 255.0F);
                float g = Math.min(1f, coeff * (float)(color >> 8 & 255) / 255.0F);
                float b = Math.min(1f, coeff * (float)(color & 255) / 255.0F);
                ResourceLocation textureLoc = ResourceLocation.fromNamespaceAndPath("aetherworks", "textures/models/armor/aether_crown_colorable.png");
                this.renderModel(poseStack, multiBufferSource, packedLight, crown, model, r, g, b, textureLoc);
            }
            //render crown
            float timerSine = ((float)Math.sin(5 * Math.toRadians(EmbersClientEvents.ticks % 360)) + 1.0F) / 2.0F;

            float r = 1 - ((144 * timerSine) / 255);
            float g = 1 - ((72 * timerSine) / 255);
            float b = 1 - ((13 * timerSine) / 255);
            ResourceLocation textureLoc = ResourceLocation.fromNamespaceAndPath("aetherworks", "textures/models/armor/aether_crown.png");
            this.renderModel(poseStack, multiBufferSource, packedLight, crown, model,r, g, b, textureLoc);
        }
    }
    //amazing copy+paste, m'lord!
    private void renderModel(PoseStack p_289664_, MultiBufferSource p_289689_, int p_289681_, ArmorItem p_289650_, Model p_289658_, float p_289678_, float p_289674_, float p_289693_, ResourceLocation armorResource) {
        VertexConsumer vertexconsumer = p_289689_.getBuffer(RenderType.armorCutoutNoCull(armorResource));
        p_289658_.renderToBuffer(p_289664_, vertexconsumer, p_289681_, OverlayTexture.NO_OVERLAY, net.minecraft.util.FastColor.ARGB32.colorFromFloat(1.0F, p_289678_, p_289674_, p_289693_));
    }
}
