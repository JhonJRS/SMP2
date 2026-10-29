package net.sirplop.aetherworks.blockentity.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.sirplop.aetherworks.Aetherworks;
import net.sirplop.aetherworks.blockentity.PrismBlockEntity;
import net.sirplop.aetherworks.util.FaceRendererUtil;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.Random;

public class RenderPrism implements BlockEntityRenderer<PrismBlockEntity>
{
    public static final ResourceLocation LOCATION_PRISM_OVERLAY = ResourceLocation.fromNamespaceAndPath(Aetherworks.MODID, "textures/block/prism_active_overlay.png");
    public static final ResourceLocation LOCATION_RUNES = ResourceLocation.fromNamespaceAndPath(Aetherworks.MODID, "textures/block/prism_runes.png");
    public static final ResourceLocation LOCATION_RUNE_BACKS = ResourceLocation.fromNamespaceAndPath(Aetherworks.MODID, "textures/block/prism_runes_backs.png");

    public static final Random RANDOM = new Random();
    public static final float[] COLOR = {1, 1, 1, 1};
    public static final int[] LIGHTMAP = {240, 240};
    public static final int[] LIGHTMAP_NOLIGHT = {0, 0};

    private static final int runeCount = 5;
    private static final float runeUV = 1f / runeCount;
    private static final int frameTime = 3;
    private static final int frameCount = 32;
    private static final float frameUVShift = 1f / frameCount;

    public RenderPrism(BlockEntityRendererProvider.Context context)  { }

    @Override
    public void render(PrismBlockEntity blockEntity, float pPartialTick, PoseStack poseStack,
                       MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay) {
        if (!blockEntity.isStructureValid()) {
            return;
        }
        blockEntity.renderTime += pPartialTick;
        if (blockEntity.renderTime >= frameTime)
        {
            blockEntity.renderTime = 0;
            if (++blockEntity.renderFrame >= frameCount)
            {
                blockEntity.renderFrame = 0;
            }
        }
        VertexConsumer overlayBuffer = pBuffer.getBuffer(RenderType.text(LOCATION_PRISM_OVERLAY));
        RenderSystem.enableDepthTest();

        float uv0 = frameUVShift * blockEntity.renderFrame;
        float uv1 = frameUVShift * (blockEntity.renderFrame + 1);


        poseStack.translate(3, -1, 0); //3, -1, 0
        FaceRendererUtil.renderHorizontals(overlayBuffer, poseStack, COLOR, LIGHTMAP, 0, 1, uv0, uv1);
        poseStack.translate(-6, 0, 0); //-3, -1, 0
        FaceRendererUtil.renderHorizontals(overlayBuffer, poseStack, COLOR, LIGHTMAP,0, 1, uv0, uv1);
        poseStack.translate(3, 0, 3);  //0, -1, 3
        FaceRendererUtil.renderHorizontals(overlayBuffer, poseStack, COLOR, LIGHTMAP,0, 1, uv0, uv1);
        poseStack.translate(0, 0, -6); //0, -1, -3
        FaceRendererUtil.renderHorizontals(overlayBuffer, poseStack, COLOR, LIGHTMAP,0, 1, uv0, uv1);
        poseStack.translate(2, -1, 5); //2, -2, 2
        FaceRendererUtil.renderHorizontals(overlayBuffer, poseStack, COLOR, LIGHTMAP,0, 1, uv0, uv1);
        poseStack.translate(0, 0, -4); //2, -2, -2
        FaceRendererUtil.renderHorizontals(overlayBuffer, poseStack, COLOR, LIGHTMAP,0, 1, uv0, uv1);
        poseStack.translate(-4, 0, 4); //-2, -2, 2
        FaceRendererUtil.renderHorizontals(overlayBuffer, poseStack, COLOR, LIGHTMAP,0, 1, uv0, uv1);
        poseStack.translate(0, 0, -4); //-2, -2, -2
        FaceRendererUtil.renderHorizontals(overlayBuffer, poseStack, COLOR, LIGHTMAP,0, 1, uv0, uv1);
        poseStack.translate(2, 2, 2);

        VertexConsumer runeBuffer = pBuffer.getBuffer(RenderType.text(LOCATION_RUNES));
        RenderSystem.enableDepthTest();

        BlockPos entPos = blockEntity.getBlockPos();

        renderRune(runeBuffer, poseStack, entPos, new Vector3d(3, -2, 0), true, 1, 1, 1, 0);
        renderRune(runeBuffer, poseStack, entPos, new Vector3d(-3, -2, 0), true, 1,1, 1,  0);
        renderRune(runeBuffer, poseStack, entPos, new Vector3d(0, -2, 3), true, 1, 1, 1, 0);
        renderRune(runeBuffer, poseStack, entPos, new Vector3d(0, -2, -3), true, 1,1, 1,  0);
        renderRune(runeBuffer, poseStack, entPos, new Vector3d(2, -3, 2), true, 1, 1, 1, 0);
        renderRune(runeBuffer, poseStack, entPos, new Vector3d(2, -3, -2), true, 1,1, 1,  0);
        renderRune(runeBuffer, poseStack, entPos, new Vector3d(-2, -3, 2), true, 1, 1, 1, 0);
        renderRune(runeBuffer, poseStack, entPos, new Vector3d(-2, -3, -2), true, 1,1, 1,  0);

        VertexConsumer backBuffer = pBuffer.getBuffer(RenderType.text(LOCATION_RUNE_BACKS));
        RenderSystem.enableDepthTest();

        renderRune(backBuffer, poseStack, entPos, new Vector3d(3, -2, 0), false, 2, 2, 2, 0);
        renderRune(backBuffer, poseStack, entPos, new Vector3d(-3, -2, 0), false, 2, 2, 2, 0);
        renderRune(backBuffer, poseStack, entPos, new Vector3d(0, -2, 3), false, 2, 2, 2, 0);
        renderRune(backBuffer, poseStack, entPos, new Vector3d(0, -2, -3), false, 2, 2, 2, 0);
        renderRune(backBuffer, poseStack, entPos, new Vector3d(2, -2.75, 2), false, 2, 1.5f, 2, -0.05f);
        renderRune(backBuffer, poseStack, entPos, new Vector3d(2, -2.75, -2), false, 2, 1.5f, 2, -0.05f);
        renderRune(backBuffer, poseStack, entPos, new Vector3d(-2, -2.75, 2), false, 2, 1.5f, 2, -0.05f);
        renderRune(backBuffer, poseStack, entPos, new Vector3d(-2, -2.75, -2), false, 2, 1.5f, 2, -0.05f);

    }
    private static final Float[] xUVArr = new Float[] {0f, 1f};
    private void renderRune(VertexConsumer buffer, PoseStack pose, BlockPos entPos, Vector3d at, boolean lit, float scalex, float scaley, float scalez, float uvAdj)
    {
        RANDOM.setSeed(getRuneSeed(entPos, at));
        pose.translate(at.x, at.y, at.z);
        FaceRendererUtil.renderHorizontals(buffer, pose, COLOR, lit ? LIGHTMAP : LIGHTMAP_NOLIGHT, () -> xUVArr, () -> {
            int val = RANDOM.nextInt(runeCount);
            return new Float[] {val * runeUV, (val + 1) * runeUV + uvAdj};
        }, scalex, scaley, scalez);
        pose.translate(-at.x, -at.y, -at.z);
    }

    private long getRuneSeed(BlockPos base, Vector3d adj) {
        long x = (long)(base.getX() + Math.floor(adj.x));
        long y = (long)(base.getY() + Math.floor(adj.y));
        long z = (long)(base.getZ() + Math.floor(adj.z));

        return (x << 16) + (z << 8) + y;
    }

    // SMP 2 port: 1.21 asks the renderer for the render box
    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(PrismBlockEntity blockEntity) {
        return blockEntity.getRenderBoundingBox();
    }
}
