package net.sirplop.aetherworks.blockentity.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.sirplop.aetherworks.Aetherworks;
import net.sirplop.aetherworks.blockentity.AetherForgeBlockEntity;
import net.sirplop.aetherworks.util.FaceRendererUtil;

public class RenderAetherForge implements BlockEntityRenderer<AetherForgeBlockEntity> {
    public RenderAetherForge(BlockEntityRendererProvider.Context context)  { }

    public static final ResourceLocation LOCATION_FORGE_OVERLAY = ResourceLocation.fromNamespaceAndPath(Aetherworks.MODID, "textures/block/forge/forge_overlay.png");
    //public static final float[] COLOR = {1, 1, 1, 1};
    public static final int[] LIGHTMAP = {240, 240};

    private static final int frameTime = 3;
    private static final int frameCount = 20;
    private static final float frameUVShift = 1f / frameCount;
    private static final float posShift = 12.01f / 16f;
    private static final float posShiftY = -0.5f / 16f;
    private float animTime = 0;
    private boolean flip = false;

    @Override
    public void render(AetherForgeBlockEntity blockEntity, float pPartialTick, PoseStack poseStack,
                       MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay) {
        blockEntity.renderTime += pPartialTick;
        animTime += pPartialTick;
        if (animTime >= frameTime) {
            animTime = 0;
            if (flip) {
                if (--blockEntity.renderFrame <= -1) {
                    flip = false;
                    blockEntity.renderFrame += 2;
                }
            } else {
                if (++blockEntity.renderFrame >= frameCount) {
                    flip = true;
                    blockEntity.renderFrame -= 2;
                }
            }
        }

        VertexConsumer overlayBuffer = pBuffer.getBuffer(RenderType.text(LOCATION_FORGE_OVERLAY));
        RenderSystem.enableDepthTest();

        float uv0 = frameUVShift * blockEntity.renderFrame;
        float uv1 = frameUVShift * (blockEntity.renderFrame + 1);

        float sin = 0.5F + (float) Math.abs(Math.sin(Math.toRadians(blockEntity.renderTime * 3))) * 0.5F;
        float[] color = {1, sin, sin, 1};

        //render all 4 sides using a 3x1 texture.
        poseStack.translate(0, posShiftY, -posShift);
        FaceRendererUtil.renderFace(Direction.NORTH, overlayBuffer, poseStack, color, LIGHTMAP, 0, 1, uv0, uv1, 3, 1, 1);
        poseStack.translate(posShift, 0, posShift);
        FaceRendererUtil.renderFace(Direction.EAST, overlayBuffer, poseStack, color, LIGHTMAP, 0, 1, uv0, uv1, 1, 1, 3);
        poseStack.translate(-posShift, 0, posShift);
        FaceRendererUtil.renderFace(Direction.SOUTH, overlayBuffer, poseStack, color, LIGHTMAP, 0, 1, uv0, uv1, 3, 1, 1);
        poseStack.translate(-posShift, 0, -posShift);
        FaceRendererUtil.renderFace(Direction.WEST, overlayBuffer, poseStack, color, LIGHTMAP, 0, 1, uv0, uv1, 1, 1, 3);
    }

    // SMP 2 port: 1.21 asks the renderer for the render box
    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(AetherForgeBlockEntity blockEntity) {
        return blockEntity.getRenderBoundingBox();
    }
}
