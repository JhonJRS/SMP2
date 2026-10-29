package net.sirplop.aetherworks.util;

import com.rekindled.embers.api.power.IEmberCapability;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.sirplop.aetherworks.AWConfig;
import net.sirplop.aetherworks.Aetherworks;

public class MoonlightRepair {
    public final static float MOONRISE = 0.25f; //12786 time tick
    public final static float MOONSET = 0.75f; //23216 time tick

    public static void tryRepair(ItemStack stack, Level level, Entity entity, int effectLevel) {
        if (!level.isClientSide() && stack.getDamageValue() > 0
                && level.random.nextFloat() <= 0.025f * effectLevel) {
            if (isValidMoonlit(level, entity)) {
                stack.setDamageValue(stack.getDamageValue() - 1);
            }
        }
    }
    public static void tryFillWithEmber(IEmberCapability capability, Level level, Entity entity, int effectLevel) {
        if (!level.isClientSide() && capability.getEmber() < capability.getEmberCapacity()
                && level.random.nextFloat() <= 0.025f * effectLevel) {
            if (isValidMoonlit(level, entity))
            {
                capability.addAmount(1, true);
            }
        }
    }

    private static boolean isValidMoonlit(Level level, Entity entity) {
        var dim = level.dimensionType();
        float time = dim.timeOfDay(level.getDayTime());
        return (time >= MOONRISE && time <= MOONSET || AWConfig.isMoonlitDimension(level.dimensionTypeRegistration().unwrapKey().orElseThrow()))
                && (level.canSeeSky(entity.getOnPos().above(2)) || !dim.hasSkyLight());
    }
}
