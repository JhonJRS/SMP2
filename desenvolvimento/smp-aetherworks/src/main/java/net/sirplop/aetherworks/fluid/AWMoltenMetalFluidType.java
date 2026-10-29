package net.sirplop.aetherworks.fluid;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.rekindled.embers.fluidtypes.MoltenMetalFluidType;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.sirplop.aetherworks.Aetherworks;
import org.joml.Vector3f;

import java.util.function.Consumer;

public class AWMoltenMetalFluidType extends MoltenMetalFluidType {
    public AWMoltenMetalFluidType(Properties properties, FluidInfo info) {
        super(properties, info);
        this.RENDER_OVERLAY = ResourceLocation.fromNamespaceAndPath(Aetherworks.MODID, "textures/overlay/" + info.name + ".png");
        this.TEXTURE_STILL = ResourceLocation.fromNamespaceAndPath(Aetherworks.MODID, "block/fluid/" + info.name + "_still");
        this.TEXTURE_FLOW = ResourceLocation.fromNamespaceAndPath(Aetherworks.MODID, "block/fluid/" + info.name + "_flow");
        this.TEXTURE_OVERLAY = ResourceLocation.fromNamespaceAndPath(Aetherworks.MODID, "block/fluid/" + info.name + "_overlay");
    }

    public final ResourceLocation RENDER_OVERLAY;
    public final ResourceLocation TEXTURE_STILL;
    public final ResourceLocation TEXTURE_FLOW;
    public final ResourceLocation TEXTURE_OVERLAY;

    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
        consumer.accept(new IClientFluidTypeExtensions() {
            public ResourceLocation getStillTexture() {
                return TEXTURE_STILL;
            }

            public ResourceLocation getFlowingTexture() {
                return TEXTURE_FLOW;
            }

            public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount, Vector3f fluidFogColor) {
                return FOG_COLOR;
            }

            public void modifyFogRender(Camera camera, FogRenderer.FogMode mode, float renderDistance, float partialTick, float nearDistance, float farDistance, FogShape shape) {
                RenderSystem.setShaderFogStart(fogStart);
                RenderSystem.setShaderFogEnd(fogEnd);
            }
        });
    }
}
