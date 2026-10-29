package net.sirplop.aetherworks.api.damage;
import net.sirplop.aetherworks.util.Compat121;

import com.rekindled.embers.api.projectile.IProjectilePreset;
import com.rekindled.embers.entity.EmberProjectileEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import net.sirplop.aetherworks.Aetherworks;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;

public class EffectDamageCrossbowQuartz extends EffectDamagePotion{
    public EffectDamageCrossbowQuartz(float damage, float knockback, Function<Entity, DamageSource> source, int fire, double invinciblityMultiplier, List<MobEffectInstance> effectInstances, ItemStack sourceItem) {
        super(damage, knockback, source, fire, invinciblityMultiplier, effectInstances);
        this.sourceItem = sourceItem;
        this.knockback = knockback;
    }

    ItemStack sourceItem;

    public float getDamage() {
        return this.damage;
    }

    public void setDamage(float damage) {
        this.damage = damage;
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
        if (entity == projectileEntity || entity == null)
            return;
        float damage = this.damage;
        if (sourceItem != null && !sourceItem.isEmpty()) {
            CompoundTag tag = Compat121.tag(sourceItem);
            CompoundTag exp;
            if (tag.contains("exponential_damage")) {
                exp = tag.getCompound("exponential_damage");
            } else {
                exp = new CompoundTag();
            }
            long time = System.currentTimeMillis();
            if (exp.contains(entity.getStringUUID()))
            { //entity has been struck before - but was it recent enough?
                CompoundTag multTag = exp.getCompound(entity.getUUID().toString());
                if (multTag.contains("time") && time - multTag.getLong("time") < 60000) {
                    int mult = multTag.getInt("damage");
                    damage *= mult;
                    multTag.putLong("time", time);
                    multTag.putInt("damage", mult + 1);
                } else { //this should probably never happen, but better safe than sorry.
                    multTag.putLong("time", time);
                    multTag.putInt("damage", 1);
                }
                exp.put(entity.getUUID().toString(), multTag);
            } else {
                CompoundTag multTag = new CompoundTag();
                multTag.putLong("time", time);
                multTag.putInt("damage", 1);
                exp.put(entity.getStringUUID(), multTag);
            }
            tag.put("exponential_damage", exp);
            Compat121.setTag(sourceItem, tag);
            if (shooter instanceof Player player)
                player.getInventory().setChanged();
        }
        boolean hurt = entity.hurt(this.source.apply(projectileEntity), damage);
        if (hurt) {
            entity.igniteForSeconds(this.fire);
        }

        if (hurt && entity instanceof LivingEntity livingTarget) {
            livingTarget.setLastHurtMob(shooter);
            livingTarget.hurtDuration = (int)((double)livingTarget.hurtDuration * this.invinciblityMultiplier);

            if (this.knockback > 0 && projectileEntity != null) {
                double d0 = Math.max(0.0D, 1.0D - livingTarget.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
                //because deltaMovement is always (0,0,0), we need to create a vector to send our victim
                Vec3 delta = livingTarget.position().subtract(new Vec3(projectileEntity.xOld, projectileEntity.yOld, projectileEntity.zOld));
                Vec3 vec3 = delta.multiply(1.0D, 0.0D, 1.0D).normalize().scale((double)this.knockback * 0.6D * d0);
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
