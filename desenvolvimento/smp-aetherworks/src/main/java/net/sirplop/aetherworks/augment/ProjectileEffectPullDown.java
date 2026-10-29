package net.sirplop.aetherworks.augment;

import com.rekindled.embers.api.projectile.IProjectileEffect;
import com.rekindled.embers.api.projectile.IProjectilePreset;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.sirplop.aetherworks.AWRegistry;

public class ProjectileEffectPullDown implements IProjectileEffect {
    protected int ticks;
    protected int amplifier;

    public ProjectileEffectPullDown(int duration, int level) {
        this.amplifier = level;
        this.ticks = duration;
    }

    @Override
    public void onEntityImpact(Entity entity, IProjectilePreset projectile) {
        if (entity instanceof LivingEntity livingTarget) {
            if (livingTarget.isAffectedByPotions()) {
                MobEffectInstance mobeffectinstance = new MobEffectInstance(AWRegistry.EFFECT_PULLDOWN, ticks, amplifier, false, true);
                livingTarget.addEffect(mobeffectinstance, entity);
            }
        }
    }
}
