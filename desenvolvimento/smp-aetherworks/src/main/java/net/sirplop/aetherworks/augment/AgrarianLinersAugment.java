package net.sirplop.aetherworks.augment;

import com.rekindled.embers.api.augment.AugmentUtil;
import com.rekindled.embers.augment.AugmentBase;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.bus.api.SubscribeEvent;

public class AgrarianLinersAugment extends AugmentBase {
    public AgrarianLinersAugment(ResourceLocation name) {
            super(name, 0.0);
            NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onCropTrample(BlockEvent.FarmlandTrampleEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player player) {
            Level world = (Level)event.getLevel();

            ItemStack bootsStack = player.getInventory().getArmor(0);
            if (AugmentUtil.hasHeat(bootsStack)) {
                int level = AugmentUtil.getAugmentLevel(bootsStack, this);
                if (!world.isClientSide() && level > 0)
                    event.setCanceled(true);
            }
        }
    }
}
