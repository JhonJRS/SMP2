package net.sirplop.aetherworks.item;
import com.rekindled.embers.util.CapabilityCompat;

import com.rekindled.embers.EmbersClientEvents;
import com.rekindled.embers.api.capabilities.EmbersCapabilities;
import com.rekindled.embers.api.power.IEmberCapability;
import com.rekindled.embers.util.Misc;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import com.rekindled.embers.compat.legacy.LazyOptional;
import net.sirplop.aetherworks.util.Utils;

@OnlyIn(Dist.CLIENT)
public class AetherEmberColorHandler implements ItemColor {
    public int getColor(ItemStack stack, int tintIndex) {
        if (tintIndex == 0) {
            LazyOptional<IEmberCapability> opt = CapabilityCompat.getCapability(stack, EmbersCapabilities.EMBER_CAPABILITY, null);
            if (opt.isPresent()) {
                IEmberCapability capability = opt.resolveOptional().get();
                float coeff = (float)(capability.getEmber() / capability.getEmberCapacity());
                float timerSine = ((float)Math.sin(6.0 * Math.toRadians((double)(EmbersClientEvents.ticks % 360))) + 1.0F) / 2.0F;

                int r = (int)(255.0F * (1.0F - coeff) + (64.0F * timerSine + 128.0F) * coeff);
                int g = 255 - (int)(64 * timerSine * coeff);;
                int b = 255 - (int)(128 * timerSine * coeff);;
                return Misc.intColor(r, g, b);
            }
        }
        return 16777215;
    }
}
