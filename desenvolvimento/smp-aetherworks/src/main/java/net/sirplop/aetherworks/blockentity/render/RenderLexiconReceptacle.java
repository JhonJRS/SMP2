package net.sirplop.aetherworks.blockentity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.sirplop.aetherworks.blockentity.LexiconReceptacleBlockEntity;

public class RenderLexiconReceptacle implements BlockEntityRenderer<LexiconReceptacleBlockEntity> {

    public static BakedModel lexicon;


    public RenderLexiconReceptacle(BlockEntityRendererProvider.Context pContext) { }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void render(LexiconReceptacleBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {

        if (!blockEntity.lexiconInventory.getStackInSlot(0).isEmpty() && lexicon != null) {

            BlockRenderDispatcher blockrendererdispatcher = Minecraft.getInstance().getBlockRenderer();
            Level level = blockEntity.getLevel();
            poseStack.pushPose();

            float time = level.getGameTime() + partialTick;
            poseStack.translate(0.5f, (0.05f * Math.sin(time * 0.025f)) + 0.85f, 0.5f);
            poseStack.mulPose(Axis.YP.rotationDegrees(time * 0.6f));
            poseStack.mulPose(Axis.XP.rotationDegrees((float)(10f * Math.sin(time * 0.05f) - 5)));
            poseStack.mulPose(Axis.ZP.rotationDegrees((float)(10f * Math.sin(time * 0.05f + 1.56f) - 5)));

            blockrendererdispatcher.getModelRenderer().renderModel(poseStack.last(), buffer.getBuffer(Sheets.solidBlockSheet()),
                    blockEntity.getBlockState(), lexicon, 0.0f, 0.0f, 0.0f,
                    packedLight, packedOverlay, ModelData.EMPTY, Sheets.solidBlockSheet());
            poseStack.popPose();
        }
    }
}
