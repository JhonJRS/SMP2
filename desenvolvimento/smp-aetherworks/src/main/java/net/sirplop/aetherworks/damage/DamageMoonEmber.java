package net.sirplop.aetherworks.damage;

import com.rekindled.embers.damage.DamageEmber;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.sirplop.aetherworks.Aetherworks;
import javax.annotation.Nullable;

public class DamageMoonEmber extends DamageSource {
    public DamageMoonEmber(Holder<DamageType> type, @Nullable Entity directEntity, @Nullable Entity causingEntity, @Nullable Vec3 damageSourcePosition) {
        super(type, directEntity, causingEntity, damageSourcePosition);
    }

    public DamageMoonEmber(Holder<DamageType> type, @Nullable Entity directEntity, @Nullable Entity causingEntity) {
        super(type, directEntity, causingEntity, null);
    }

    public DamageMoonEmber(Holder<DamageType> type, Vec3 damageSourcePosition) {
        super(type, null, null, damageSourcePosition);
    }

    public DamageMoonEmber(Holder<DamageType> type, @Nullable Entity entity) {
        super(type, entity, entity);
    }

    public DamageMoonEmber(Holder<DamageType> type, @Nullable Entity entity, boolean indirect) {
        super(type, entity, entity);
        indirect = true;
    }

    public DamageMoonEmber(Holder<DamageType> type) {
        super(type, null, null, null);
    }
    public Component getLocalizedDeathMessage(LivingEntity livingEntity) {
        String s = "death.attack." + this.type().msgId();
        Entity entity = this.getEntity();
        if (entity == null) {
            entity = this.getDirectEntity();
        }

        if (entity == null) {
            entity = livingEntity.getKillCredit();
        }

        ItemStack itemstack;
        if (entity instanceof LivingEntity living) {
            itemstack = living.getMainHandItem();
        } else {
            itemstack = ItemStack.EMPTY;
        }

        if (entity == null) {
            return Component.translatable(s, new Object[]{livingEntity.getDisplayName()});
        } else {
            return !itemstack.isEmpty() && itemstack.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME) ? Component.translatable(s + ".item", new Object[]{livingEntity.getDisplayName(), ((Entity)entity).getDisplayName(), itemstack.getDisplayName()}) : Component.translatable(s + ".player", new Object[]{livingEntity.getDisplayName(), ((Entity)entity).getDisplayName()});
        }
    }
}
