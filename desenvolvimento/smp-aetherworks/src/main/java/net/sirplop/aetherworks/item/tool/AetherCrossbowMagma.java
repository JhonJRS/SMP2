package net.sirplop.aetherworks.item.tool;
import net.sirplop.aetherworks.util.Compat121;

import com.rekindled.embers.ConfigManager;
import com.rekindled.embers.api.event.EmberProjectileEvent;
import com.rekindled.embers.api.projectile.IProjectilePreset;
import com.rekindled.embers.api.projectile.ProjectileRay;
import com.rekindled.embers.damage.DamageEmber;
import com.rekindled.embers.datagen.EmbersDamageTypes;
import com.rekindled.embers.datagen.EmbersSounds;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.sirplop.aetherworks.AWRegistry;
import net.sirplop.aetherworks.api.damage.EffectDamageCrossbowMagma;
import net.sirplop.aetherworks.util.Utils;

import java.util.*;

public class AetherCrossbowMagma extends AetherCrossbow{
    public AetherCrossbowMagma(Properties pProperties) {
        super(pProperties);
    }

    public int shotID = 0;
    public final Map<Integer, Set<UUID>> projectileGroups = new HashMap<>();

    @Override
    public void shootProjectile(Level level, LivingEntity entity, ItemStack stack, InteractionHand hand, float pSoundPitch, float rotation) {
        if (level.isClientSide)
            return;

        double posX = entity.getX() + entity.getLookAngle().x;
        double y = entity.getY() + entity.getEyeHeight() - 0.2 + entity.getLookAngle().y;
        double posZ = entity.getZ() + entity.getLookAngle().z;

        double delta = Math.toRadians(rotation); //handle multishot
        Vec3 direction = entity.getLookAngle().scale(1.5f);
        direction = new Vec3(
                direction.x * Math.cos(delta) - direction.z * Math.sin(delta),
                direction.y,
                direction.x * Math.sin(delta) + direction.z * Math.cos(delta)
        );

        double targX = entity.getX() + direction.x * ConfigManager.BLAZING_RAY_MAX_DISTANCE.get();
        double targY = entity.getY() + 2 + direction.y * ConfigManager.BLAZING_RAY_MAX_DISTANCE.get();
        double targZ = entity.getZ() + direction.z * ConfigManager.BLAZING_RAY_MAX_DISTANCE.get();

        int powerLevel = EnchantmentHelper.getEnchantmentLevel(Compat121.enchantment(Enchantments.POWER), entity);
        int knockback = EnchantmentHelper.getEnchantmentLevel(Compat121.enchantment(Enchantments.PUNCH), entity);
        int fire = EnchantmentHelper.getEnchantmentLevel(Compat121.enchantment(Enchantments.FLAME), entity) > 0 ? 100 : 0;

        float damage = ConfigManager.BLAZING_RAY_DAMAGE.get().floatValue() + (powerLevel * 0.5f);

        createRay(level, damage, knockback, fire, entity, new Vec3(posX, y, posZ), new Vec3(targX, targY, targZ), stack, getNextID());

        if (rotation == 0) { //stop us from going deaf.
            level.playSound(null, entity, EmbersSounds.BLAZING_RAY_FIRE.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0F, pSoundPitch);
        }
        if (entity instanceof Player player && player.getAbilities().instabuild)
            return;
        stack.hurtAndBreak(1, entity, net.minecraft.world.entity.LivingEntity.getSlotForHand(hand));
    }

    public void createRay(Level level, EffectDamageCrossbowMagma effect, LivingEntity shooter, Vec3 start, Vec3 targetPos, ItemStack stack, boolean hitBefore) {
        ProjectileRay ray = new ProjectileRay(shooter, start, targetPos, true, effect);
        ray.setColor(Utils.AETHERIUM_PROJECTILE_COLOR);

        EmberProjectileEvent event = new EmberProjectileEvent(shooter, stack, 1, ray);
        NeoForge.EVENT_BUS.post(event);
        if (!event.isCanceled()) {
            for (IProjectilePreset projectile : event.getProjectiles()) {
                projectile.shoot(level);
                if (hitBefore) //to prevent exponential amounts of diffraction barrel nonsense.
                    break;
            }
        }
    }

    public void createRay(Level level, float damage, float knockback, int fire, LivingEntity shooter, Vec3 start, Vec3 targetPos, ItemStack stack, int id) {
        DamageSource dam = new DamageEmber(level.registryAccess().registry(Registries.DAMAGE_TYPE).get().getHolderOrThrow(EmbersDamageTypes.EMBER_KEY), shooter, true);
        EffectDamageCrossbowMagma effect = new EffectDamageCrossbowMagma(damage, knockback, e -> dam, fire, 1.0f,
                List.of(new MobEffectInstance(AWRegistry.EFFECT_MOONFIRE, 200, 1, false, true, true)),
                this, id, stack);
        createRay(level, effect, shooter, start, targetPos, stack, false);
    }

    public int getNextID() {
        int currentID = shotID;

        if (!projectileGroups.containsKey(currentID))
            projectileGroups.put(currentID, new HashSet<>());
        projectileGroups.get(currentID).clear();

        if (++shotID >= 8)
            shotID = 0; //limit it to 8 groups

        return currentID;
    }
}
