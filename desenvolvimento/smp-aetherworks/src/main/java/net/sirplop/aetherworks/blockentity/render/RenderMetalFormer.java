package net.sirplop.aetherworks.blockentity.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rekindled.embers.render.FluidCuboid;
import com.rekindled.embers.render.FluidRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.sirplop.aetherworks.api.block.HorizontalWaterloggableEntityBlock;
import net.sirplop.aetherworks.blockentity.MetalFormerBlockEntity;
import org.joml.Vector3f;

public class RenderMetalFormer implements BlockEntityRenderer<MetalFormerBlockEntity> {
    private final ItemRenderer itemRenderer;

    FluidCuboid cube = new FluidCuboid(new Vector3f(2, 1, 2), new Vector3f(14, 2, 14), FluidCuboid.DEFAULT_FACES);

    public RenderMetalFormer(BlockEntityRendererProvider.Context pContext) {
        this.itemRenderer = pContext.getItemRenderer();
    }

    @Override
    public void render(MetalFormerBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        //render item
        if (!blockEntity.inventory.getStackInSlot(0).isEmpty()) {
            poseStack.pushPose();
            ItemStack stack = blockEntity.inventory.getStackInSlot(0);
            int seed = Item.getId(stack.getItem()) + stack.getDamageValue();
            BakedModel bakedmodel = this.itemRenderer.getModel(stack, blockEntity.getLevel(), null, seed);
            float f2 = bakedmodel.getTransforms().getTransform(ItemDisplayContext.FIXED).scale.y();
            poseStack.translate(0.5D, (double)(0.1F * f2), 0.5D);
            poseStack.scale(0.7f, 1, 0.7f);
            poseStack.mulPose(Axis.XP.rotationDegrees(90));
            poseStack.mulPose(Axis.ZP.rotationDegrees(blockEntity.getBlockState().getValue(HorizontalWaterloggableEntityBlock.FACING).getOpposite().toYRot()));
            this.itemRenderer.render(stack, ItemDisplayContext.FIXED, false, poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY, bakedmodel);
            //poseStack.scale(0.7f, 1, 0.7f);
            poseStack.popPose();
        }

        //render fluid
        FluidStack fluidStack = blockEntity.getFluidStack();
        int capacity = blockEntity.getCapacity();
        if (!fluidStack.isEmpty() && capacity > 0) {
            float offset = blockEntity.renderOffset;
            if (offset > 1.2f || offset < -1.2f) {
                offset = offset - ((offset / 12f + 0.1f) * partialTick);
                blockEntity.renderOffset = offset;
            } else {
                blockEntity.renderOffset = 0;
            }
            FluidRenderer.renderScaledCuboid(poseStack, bufferSource, cube, fluidStack, offset, capacity, packedLight, packedOverlay, false);
        } else {
            blockEntity.renderOffset = 0;
        }
    }
}
