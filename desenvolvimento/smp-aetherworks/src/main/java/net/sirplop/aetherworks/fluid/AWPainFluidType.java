package net.sirplop.aetherworks.fluid;

import com.rekindled.embers.particle.GlowParticleOptions;
import com.rekindled.embers.particle.SparkParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.sirplop.aetherworks.damage.DamageMoonEmber;
import net.sirplop.aetherworks.datagen.AWDamageTypes;
import net.sirplop.aetherworks.util.Utils;

public class AWPainFluidType extends AWViscousFluidType {
    public AWPainFluidType(Properties properties, FluidInfo info) {
        super(properties, info);
    }

    public static final SparkParticleOptions SPARK = new SparkParticleOptions(Utils.AETHERIUM_COLOR, 1.0f);

    @Override
    public boolean move(FluidState state, LivingEntity entity, Vec3 movementVector, double gravity) {
        super.move(state, entity, movementVector, gravity);
        //damage entity really badly here.
        if (!entity.level().isClientSide()) {
            DamageSource source = new DamageMoonEmber(entity.level().registryAccess().registry(Registries.DAMAGE_TYPE).get()
                    .getHolderOrThrow(AWDamageTypes.MOON_EMBER_KEY), null, null);
            if (entity.hurt(source, 6.0F))
                entity.invulnerableTime = 15;
            Vec3 pos = entity.position();
            double y = entity.blockPosition().getY() + state.getOwnHeight();
            ((ServerLevel) entity.level()).sendParticles(SPARK,
                    pos.x,
                    y,
                    pos.z,
                    5,
                    0.375f, 0.25f, 0.375f, 0.0f);
        }
        return true;
    }
    @Override
    public void setItemMovement(ItemEntity entity) {
        Vec3 vec3 = entity.getDeltaMovement();
        entity.setDeltaMovement(vec3.x * (double)0.95F, vec3.y + (double)(vec3.y < (double)0.06F ? 5.0E-4F : 0.0F), vec3.z * (double)0.95F);
    }
}
