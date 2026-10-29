package net.sirplop.aetherworks.item.tool;
import net.sirplop.aetherworks.util.Compat121;

import com.rekindled.embers.api.event.EmberProjectileEvent;
import com.rekindled.embers.api.projectile.EffectArea;
import com.rekindled.embers.api.projectile.IProjectilePreset;
import com.rekindled.embers.api.projectile.ProjectileFireball;
import com.rekindled.embers.damage.DamageEmber;
import com.rekindled.embers.datagen.EmbersDamageTypes;
import com.rekindled.embers.datagen.EmbersSounds;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.sirplop.aetherworks.AWRegistry;
import net.sirplop.aetherworks.api.damage.EffectDamageCrossbowQuartz;
import net.sirplop.aetherworks.util.Utils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class AetherCrossbowQuartz extends AetherCrossbow {
    public AetherCrossbowQuartz(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        CompoundTag tag = Compat121.tag(stack);
        boolean changed = false;
        if (tag.contains("exponential_damage")) {
            CompoundTag exp = tag.getCompound("exponential_damage");
            long time = System.currentTimeMillis();
            Set<String> rem = new HashSet<>();
            for (String key : exp.getAllKeys()) {
                CompoundTag multTag = exp.getCompound(key);
                if (multTag.contains("time")) {
                    if (time - multTag.getLong("time") >= 60000) {
                        rem.add(key);
                        changed = true;
                    }
                } else { //malformed?
                    rem.add(key);
                    changed = true;
                }
            }
            for (String key : rem) {
                exp.remove(key);
            }
            tag.put("exponential_damage", exp);
            if (changed)
                Compat121.setTag(stack, tag);
        }
        if (changed && entity instanceof Player player)
            player.getInventory().setChanged();
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void shootProjectile(Level level, LivingEntity entity, ItemStack stack, InteractionHand hand, float pSoundPitch, float rotation) {
        if (level.isClientSide)
            return;

        float spawnDistance = 1.0f;//Math.max(1.0f, (float)charge/5.0f);
        Vec3 eyesPos = entity.getEyePosition();
        HitResult traceResult;
        if (entity instanceof Player player)
            traceResult = getPlayerPOVHitResult(entity.level(), player, ClipContext.Fluid.NONE);
        else
            traceResult = Utils.getEntityPOVHitResult(entity.level(), entity, spawnDistance, ClipContext.Fluid.NONE);
        if (traceResult.getType() == HitResult.Type.BLOCK)
            spawnDistance = (float) Math.min(spawnDistance, traceResult.getLocation().distanceTo(eyesPos));

        Vec3 launchPos = eyesPos.add(entity.getLookAngle().scale(spawnDistance));
        int powerLevel = EnchantmentHelper.getEnchantmentLevel(Compat121.enchantment(Enchantments.POWER), entity);
        int knockback = EnchantmentHelper.getEnchantmentLevel(Compat121.enchantment(Enchantments.PUNCH), entity);
        int fire = EnchantmentHelper.getEnchantmentLevel(Compat121.enchantment(Enchantments.FLAME), entity) > 0 ? 100 : 0;
        float damage = 9f + (powerLevel * 0.5f);
        float size = 6f;
        float aoeSize = 1f;
        int lifetime = 100;

        double delta = Math.toRadians(rotation); //handle multishot
        Vec3 direction = entity.getLookAngle().scale(1.5f);
        direction = new Vec3(
                direction.x * Math.cos(delta) - direction.z * Math.sin(delta),
                direction.y,
                direction.x * Math.sin(delta) + direction.z * Math.cos(delta)
        );

        Function<Entity, DamageSource> damageSource = e ->  new DamageEmber(level.registryAccess().registry(Registries.DAMAGE_TYPE).get().getHolderOrThrow(EmbersDamageTypes.EMBER_KEY), e, true);
        EffectDamageCrossbowQuartz dam = new EffectDamageCrossbowQuartz(damage, knockback, damageSource, fire, 1.0,
                List.of(new MobEffectInstance(AWRegistry.EFFECT_MOONFIRE, 200, 1, false, true, true)), stack);
        EffectArea effect = new EffectArea(dam, aoeSize, false);
        ProjectileFireball fireball = new ProjectileFireball(entity, launchPos, direction, size, lifetime, effect);
        fireball.setColor(Utils.AETHERIUM_PROJECTILE_COLOR);
        EmberProjectileEvent event = new EmberProjectileEvent(entity, stack, 1, fireball);
        NeoForge.EVENT_BUS.post(event);
        if (!event.isCanceled()) {
            for (IProjectilePreset projectile : event.getProjectiles()) {
                projectile.shoot(level);
            }
        }

        SoundEvent sound = EmbersSounds.FIREBALL_BIG.get();
        if (rotation == 0) {
            level.playSound(null, launchPos.x, launchPos.y, launchPos.z, sound, SoundSource.PLAYERS, 1.0f, 1.0f);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0F, pSoundPitch);
        }
        if (entity instanceof Player player && player.getAbilities().instabuild)
            return;
        stack.hurtAndBreak(1, entity, net.minecraft.world.entity.LivingEntity.getSlotForHand(hand));
    }
}
