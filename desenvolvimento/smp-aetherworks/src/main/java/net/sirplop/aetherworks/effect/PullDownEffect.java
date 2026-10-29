package net.sirplop.aetherworks.effect;

import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.awt.*;

public class PullDownEffect extends MobEffect {

    public static final Color COLOR = new Color(157, 98, 51);

    public PullDownEffect(MobEffectCategory pCategory, int pColor) {
        super(pCategory, pColor);
    }

    @Override
    public boolean applyEffectTick(LivingEntity pLivingEntity, int pAmplifier) {
        if (!pLivingEntity.level().isClientSide() || isClientPlayer(pLivingEntity)) {
            //pLivingEntity.push(0, -0.25 * (1 + pAmplifier), 0);
            pLivingEntity.push(0, -0.1f * (1f + pAmplifier), 0);
        }

        return super.applyEffectTick(pLivingEntity, pAmplifier);
    }

    @OnlyIn(Dist.CLIENT)
    private boolean isClientPlayer(LivingEntity livingEntity) {
        return livingEntity instanceof Player player && Minecraft.getInstance().player == player;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int pDuration, int pAmplifier) {
        return true;
    }
}
