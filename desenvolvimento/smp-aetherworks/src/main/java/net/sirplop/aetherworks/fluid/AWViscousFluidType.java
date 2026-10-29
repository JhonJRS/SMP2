package net.sirplop.aetherworks.fluid;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.rekindled.embers.fluidtypes.EmbersFluidType;
import com.rekindled.embers.fluidtypes.ViscousFluidType;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.sirplop.aetherworks.AWRegistry;
import net.sirplop.aetherworks.Aetherworks;
import org.joml.Vector3f;

import java.util.function.Consumer;

public class AWViscousFluidType extends ViscousFluidType {
    public AWViscousFluidType(Properties properties, FluidInfo info) {
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

    @Override
    public boolean move(FluidState state, LivingEntity entity, Vec3 movementVector, double gravity) {
        double d8 = entity.getY();
        boolean flag = entity.getDeltaMovement().y <= 0.0;
        entity.moveRelative(0.02F, movementVector);
        entity.move(MoverType.SELF, entity.getDeltaMovement());
        Vec3 vec34;
        if (entity.getFluidTypeHeight(this) <= entity.getFluidJumpThreshold()) {
            entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.75, 0.800000011920929, 0.75));
            vec34 = entity.getFluidFallingAdjustedMovement(gravity, flag, entity.getDeltaMovement());
            entity.setDeltaMovement(vec34);
        } else {
            entity.setDeltaMovement(entity.getDeltaMovement().scale(0.5));
        }

        if (!entity.isNoGravity()) {
            entity.setDeltaMovement(entity.getDeltaMovement().add(0.0, -gravity / 4.0, 0.0));
        }

        vec34 = entity.getDeltaMovement();
        if (entity.horizontalCollision && entity.isFree(vec34.x, vec34.y + 0.6000000238418579 - entity.getY() + d8, vec34.z)) {
            entity.setDeltaMovement(vec34.x, 0.60000001192092896, vec34.z);
        }

        if (entity.isAffectedByPotions()) {
            if (!entity.hasEffect(AWRegistry.EFFECT_MOONFIRE)) {
                entity.addEffect(new MobEffectInstance(AWRegistry.EFFECT_MOONFIRE, 80, 1, true, false));
            }
        }

        return true;
    }

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
    @Override
    public void setItemMovement(ItemEntity entity) {
        entity.lavaHurt();
        Vec3 vec3 = entity.getDeltaMovement();
        entity.setDeltaMovement(vec3.x * (double)0.95F, vec3.y + (double)(vec3.y < (double)0.06F ? 5.0E-4F : 0.0F), vec3.z * (double)0.95F);
    }
}
