package net.sirplop.aetherworks;
import net.sirplop.aetherworks.AWRegistry;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.world.InteractionResult;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.sirplop.aetherworks.item.tool.AmethystHoe;

@EventBusSubscriber(modid = Aetherworks.MODID)
public class AWEvents {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    //we need to do this in a very high priority event because right-click-harvest mods
    //would break the hoe otherwise because they cancel the event.
    public static void amethystHoeHandler(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity().getMainHandItem().is(AWRegistry.HOE_AMETHYST.get())
                && ((AmethystHoe)event.getEntity().getMainHandItem().getItem()).useOnBlock(event)) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }
}
