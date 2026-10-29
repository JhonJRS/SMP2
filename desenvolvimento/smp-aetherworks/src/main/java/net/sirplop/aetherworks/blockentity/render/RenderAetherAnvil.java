package net.sirplop.aetherworks.blockentity.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.sirplop.aetherworks.Aetherworks;
import net.sirplop.aetherworks.api.block.HorizontalWaterloggableEntityBlock;
import net.sirplop.aetherworks.blockentity.AetheriumAnvilBlockEntity;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class RenderAetherAnvil implements BlockEntityRenderer<AetheriumAnvilBlockEntity> {

    private final ItemRenderer itemRenderer;
    public RenderAetherAnvil(BlockEntityRendererProvider.Context pContext) {
        this.itemRenderer = pContext.getItemRenderer();
    }

    public static final ResourceLocation LOCATION_MARK = ResourceLocation.fromNamespaceAndPath(Aetherworks.MODID, "textures/gui/forge_marker.png");
    public static final float[] colorOK = new float[] { 46 / 255f, 1f, 71f / 255f, 1f };
    public static final float[] colorWait = new float[] { 1f, 245f / 255f, 46f / 255f, 1f };
    public static final float[] colorNoEmber = new float[] { 1f, 80f / 255f, 46f / 255f, 1f };
    public static final int[] LIGHTMAP = {240, 240};

    @Override
    @OnlyIn(Dist.CLIENT)
    public void render(AetheriumAnvilBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        //render item
        if (!blockEntity.inventory.getStackInSlot(0).isEmpty()) {
            poseStack.pushPose();
            ItemStack stack = blockEntity.inventory.getStackInSlot(0);
            int seed = Item.getId(stack.getItem()) + stack.getDamageValue();
            BakedModel bakedmodel = this.itemRenderer.getModel(stack, blockEntity.getLevel(), null, seed);
            float f2 = bakedmodel.getTransforms().getTransform(ItemDisplayContext.FIXED).scale.z();
            poseStack.translate(0.5D, (double)(0.2F * f2), 0.5D);
            poseStack.scale(0.7f, 1, 0.7f);
            poseStack.mulPose(Axis.XP.rotationDegrees(90));
            poseStack.mulPose(Axis.ZP.rotationDegrees(blockEntity.getBlockState().getValue(HorizontalWaterloggableEntityBlock.FACING).getOpposite().toYRot()));
            this.itemRenderer.render(stack, ItemDisplayContext.FIXED, false, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, bakedmodel);
            poseStack.popPose();
        }

        if (blockEntity.cachedRecipe != null) {
            Player player = Minecraft.getInstance().player;
            float[] c = blockEntity.hasEmber ? blockEntity.hitTimeout > 0 ? colorOK : colorWait : colorNoEmber;

            Vector3f eyePos = player.getEyePosition().toVector3f();
            poseStack.pushPose();
            poseStack.translate(0.5f, 0.9f, 0.5f);
            poseStack.scale(0.75f, 0.75f, 0.75f);

            Vector3f position = blockEntity.getBlockPos().getCenter().toVector3f();
            Vector3f dir = new Vector3f(position.x() - eyePos.x(),
                    position.y() - eyePos.y() + 0.4f,
                    position.z() - eyePos.z()).normalize();

            //billboard effect
            double pitch = Math.asin(-dir.y());
            double yaw = Math.atan2(dir.x(), dir.z());

            poseStack.mulPose(Axis.ZP.rotationDegrees(180)); //rotate it around so it faces upright.
            poseStack.mulPose(Axis.YP.rotation((float)-yaw));
            poseStack.mulPose(Axis.XP.rotation((float)-pitch));

            VertexConsumer overlayBuffer = buffer.getBuffer(RenderType.text(LOCATION_MARK));
            RenderSystem.enableDepthTest();

            Matrix4f matrix4f = poseStack.last().pose();
            overlayBuffer.addVertex(matrix4f, -0.5f, -0.5f, 0).setColor(c[0], c[1], c[2], c[3]).setUv(0, 0).setUv2(LIGHTMAP[0], LIGHTMAP[1]);
            overlayBuffer.addVertex(matrix4f, -0.5f, 0.5f, 0).setColor(c[0], c[1], c[2], c[3]).setUv(0, 1).setUv2(LIGHTMAP[0], LIGHTMAP[1]);
            overlayBuffer.addVertex(matrix4f,  0.5f, 0.5f, 0).setColor(c[0], c[1], c[2], c[3]).setUv(1, 1).setUv2(LIGHTMAP[0], LIGHTMAP[1]);
            overlayBuffer.addVertex(matrix4f, 0.5f, -0.5f, 0).setColor(c[0], c[1], c[2], c[3]).setUv(1, 0).setUv2(LIGHTMAP[0], LIGHTMAP[1]);
            poseStack.popPose();
        }
    }
}