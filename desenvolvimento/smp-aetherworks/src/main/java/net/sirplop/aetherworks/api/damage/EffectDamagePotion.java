package net.sirplop.aetherworks.api.damage;

import com.rekindled.embers.api.projectile.IProjectileEffect;
import com.rekindled.embers.api.projectile.IProjectilePreset;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;

public class EffectDamagePotion implements IProjectileEffect {
    float damage;
    float knockback;
    Function<Entity, DamageSource> source;
    int fire;
    double invinciblityMultiplier = 1.0;
    List<MobEffectInstance> effectInstances;

    public EffectDamagePotion(float damage, float knockback, Function<Entity, DamageSource> source, int fire, double invinciblityMultiplier, List<MobEffectInstance> effectInstances) {
        this.damage = damage;
        this.knockback = knockback;
        this.source = source;
        this.fire = fire;
        this.invinciblityMultiplier = invinciblityMultiplier;
        this.effectInstances = effectInstances;
    }

    public float getDamage() {
        return this.damage;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public float getKnockback() {
        return this.knockback;
    }

    public void setKnockback(float knockback) {
        this.knockback = knockback;
    }

    public Function<Entity, DamageSource> getSource() {
        return this.source;
    }

    public void setSource(Function<Entity, DamageSource> source) {
        this.source = source;
    }

    public int getFire() {
        return this.fire;
    }

    public void setFire(int seconds) {
        this.fire = seconds;
    }

    public double getInvinciblityMultiplier() {
        return this.invinciblityMultiplier;
    }

    public void setInvinciblityMultiplier(double multiplier) {
        this.invinciblityMultiplier = multiplier;
    }

    public void onEntityImpact(Entity entity, @Nullable IProjectilePreset projectile) {
        Entity shooter = projectile != null ? projectile.getShooter() : null;
        Entity projectileEntity = projectile != null ? projectile.getEntity() : null;
        float damage = this.damage;
        if (entity.hurt(this.source.apply(projectileEntity), damage)) {
            entity.igniteForSeconds(this.fire);
        }

        if (entity instanceof LivingEntity livingTarget) {
            livingTarget.setLastHurtMob(shooter);
            livingTarget.hurtDuration = (int)((double)livingTarget.hurtDuration * this.invinciblityMultiplier);

            if (this.knockback > 0 && projectileEntity != null) {
                double d0 = Math.max(0.0D, 1.0D - livingTarget.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
                Vec3 vec3 = projectileEntity.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D).normalize().scale((double)this.knockback * 0.6D * d0);
                if (vec3.lengthSqr() > 0.0D) {
                    livingTarget.push(vec3.x, 0.1D, vec3.z);
                }
            }

            if (projectileEntity != null && !projectileEntity.level().isClientSide && shooter instanceof LivingEntity livingShooter) {
                if (livingTarget.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)
                    EnchantmentHelper.doPostAttackEffects(serverLevel, livingTarget, this.source.apply(projectileEntity));
            }

            if (livingTarget.isAffectedByPotions()) {
                for(MobEffectInstance mobeffectinstance : effectInstances) {
                    net.minecraft.core.Holder<MobEffect> mobeffect = mobeffectinstance.getEffect();
                    if (mobeffect.value().isInstantenous()) {
                        mobeffect.value().applyInstantenousEffect(projectileEntity, shooter, livingTarget, mobeffectinstance.getAmplifier(), 1);
                    } else {
                        MobEffectInstance mobeffectinstance1 = new MobEffectInstance(mobeffect, mobeffectinstance.getDuration(), mobeffectinstance.getAmplifier(), mobeffectinstance.isAmbient(), mobeffectinstance.isVisible());
                        livingTarget.addEffect(mobeffectinstance1, entity);
                    }
                }
            }
        }
    }
}
