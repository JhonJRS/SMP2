package net.sirplop.aetherworks.augment;

import com.rekindled.embers.api.augment.AugmentUtil;
import com.rekindled.embers.api.event.EmberProjectileEvent;
import com.rekindled.embers.api.projectile.*;
import com.rekindled.embers.augment.AugmentBase;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.List;
import java.util.ListIterator;

public class VolantCalcifierAugment extends AugmentBase {
    public VolantCalcifierAugment(ResourceLocation name) {
        super(name, 3.0);
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onProjectileFire(EmberProjectileEvent event) {
        ListIterator<IProjectilePreset> projectiles = event.getProjectiles().listIterator();
        ItemStack weapon = event.getStack();
        if (!weapon.isEmpty() && AugmentUtil.hasHeat(weapon)) {
            int level = AugmentUtil.getAugmentLevel(weapon, this);
            if (level > 0) {
                while (projectiles.hasNext()) {
                    IProjectilePreset projectile = projectiles.next();
                    projectile.setEffect(adjustEffect(projectile.getEffect(), level));
                }
            }
        }
    }

    private IProjectileEffect adjustEffect(IProjectileEffect effect, int level) {
        if (effect instanceof EffectArea areaEffect) {
            areaEffect.setEffect(adjustEffect(areaEffect.getEffect(),level));
            return areaEffect;
        } else if (effect instanceof EffectMulti multi) {
            multi.addEffect(new ProjectileEffectPullDown(150 * level, level));
            return effect;
        } else {
            EffectMulti multiEffect = new EffectMulti(List.of(effect));
            return adjustEffect(multiEffect, level);
        }
    }
}
