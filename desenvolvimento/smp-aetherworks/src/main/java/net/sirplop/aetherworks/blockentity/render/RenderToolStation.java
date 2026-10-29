package net.sirplop.aetherworks.blockentity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.sirplop.aetherworks.AWConfig;
import net.sirplop.aetherworks.api.block.HorizontalWaterloggableEntityBlock;
import net.sirplop.aetherworks.blockentity.ToolStationBlockEntity;
import net.sirplop.aetherworks.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class RenderToolStation implements BlockEntityRenderer<ToolStationBlockEntity> {
    private final ItemRenderer itemRenderer;
    public RenderToolStation(BlockEntityRendererProvider.Context pContext) {
        this.itemRenderer = pContext.getItemRenderer();
    }

    public final List<Vec3> positions = List.of(
            new Vec3(4 / 16f, 2.101 / 16f, 4 / 16f),
            new Vec3(12 / 16f, 2.104 / 16f, 4 / 16f),
            new Vec3(8 / 16f, 2.1 / 16f, 8 / 16f),
            new Vec3(4 / 16f, 2.103 / 16f, 12 / 16f),
            new Vec3(12 / 16f, 2.102 / 16f, 12 / 16f)
    );

    @Override
    public void render(ToolStationBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (blockEntity.isInvalid())
            return;

        float progress = (float)blockEntity.getProgress() / AWConfig.FORGE_TOOL_STATION_MAX_HITS.get();
        renderItemAt(blockEntity.inventory.getStackInSlot(0), Utils.multiLerp(progress, positions.get(0), positions.get(2)), Utils.mix(0.3125, 0.75, progress), blockEntity, poseStack, buffer, packedLight);
        renderItemAt(blockEntity.inventory.getStackInSlot(1), Utils.multiLerp(progress, positions.get(1), positions.get(2)), Utils.mix(0.3125, 0.75, progress), blockEntity, poseStack, buffer, packedLight);
        renderItemAt(blockEntity.inventory.getStackInSlot(2), positions.get(2), Utils.mix(0.3125, 0.75, progress), blockEntity, poseStack, buffer, packedLight);
        renderItemAt(blockEntity.inventory.getStackInSlot(3), Utils.multiLerp(progress, positions.get(3), positions.get(2)), Utils.mix(0.3125, 0.75, progress), blockEntity, poseStack, buffer, packedLight);
        renderItemAt(blockEntity.inventory.getStackInSlot(4), Utils.multiLerp(progress, positions.get(4), positions.get(2)), Utils.mix(0.3125, 0.75, progress), blockEntity, poseStack, buffer, packedLight);

        ItemStack stack = blockEntity.inventory.getStackInSlot(5);
        if (stack.isEmpty())
            return;
        Vec3 pos = positions.get(2);
        poseStack.pushPose();
        int seed = Item.getId(stack.getItem()) + stack.getDamageValue();
        BakedModel bakedmodel = this.itemRenderer.getModel(stack, blockEntity.getLevel(), null, seed);
        float f2 = bakedmodel.getTransforms().getTransform(ItemDisplayContext.FIXED).scale.z();

        pos = rotatePosition(blockEntity, pos);
        poseStack.translate(pos.x, (pos.y + 0.02) * f2, pos.z);
        poseStack.scale(0.75f, 0.75f, 0.75f);
        poseStack.mulPose(Axis.XP.rotationDegrees(90));
        poseStack.mulPose(Axis.ZP.rotationDegrees(blockEntity.getBlockState().getValue(HorizontalWaterloggableEntityBlock.FACING).getOpposite().toYRot()));
        this.itemRenderer.render(stack, ItemDisplayContext.FIXED, false, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, bakedmodel);
        poseStack.popPose();
    }

    public void renderItemAt(ItemStack stack, Vec3 pos, double scale,
                             ToolStationBlockEntity blockEntity, PoseStack poseStack,
                             MultiBufferSource buffer, int packedLight) {
        if (stack.isEmpty())
            return;
        poseStack.pushPose();
        int seed = Item.getId(stack.getItem()) + stack.getDamageValue();
        BakedModel bakedmodel = this.itemRenderer.getModel(stack, blockEntity.getLevel(), null, seed);
        float f2 = bakedmodel.getTransforms().getTransform(ItemDisplayContext.FIXED).scale.z();

        double y = pos.y;
        pos = rotatePosition(blockEntity, pos);
        poseStack.translate(pos.x, y * f2, pos.z);
        poseStack.scale((float)scale, (float)scale, (float)scale);
        poseStack.mulPose(Axis.XP.rotationDegrees(90));
        poseStack.mulPose(Axis.ZP.rotationDegrees(blockEntity.getBlockState().getValue(HorizontalWaterloggableEntityBlock.FACING).getOpposite().toYRot()));
        this.itemRenderer.render(stack, ItemDisplayContext.FIXED, false, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, bakedmodel);
        poseStack.popPose();
    }

    public Vec3 rotatePosition(ToolStationBlockEntity entity, Vec3 pos) {
        double x = pos.x;
        double y = pos.y;
        double z = pos.z;

        switch (entity.getLevel().getBlockState(entity.getBlockPos()).getValue(HorizontalWaterloggableEntityBlock.FACING)) {
            case EAST -> {
                double tX = x;
                x = z;
                z = 1 - tX;
            }
            case NORTH -> {
                x = 1 - x;
                z = 1 - z;
            }
            case WEST -> {
                double tX = x;
                x = 1 - z;
                z = tX;
            }
            //south is the default direction
        }
        return new Vec3(x, y, z);
    }
}
