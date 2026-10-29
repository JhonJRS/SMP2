package net.sirplop.aetherworks.compat.curios;

import com.rekindled.embers.item.EmberStorageItem;
import com.rekindled.embers.research.ResearchBase;
import com.rekindled.embers.research.ResearchManager;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.sirplop.aetherworks.AWRegistry;
import net.sirplop.aetherworks.item.AetherEmberBulbItem;
import net.sirplop.aetherworks.research.AWResearch;

public class CuriosCompat {

    public static final DeferredHolder<Item, Item> AETHER_EMBER_BULB = AWRegistry.ITEMS.register("aether_ember_bulb", () -> new AetherEmberBulbItem(new Item.Properties().stacksTo(1)));

    public static void init() {}

    @OnlyIn(Dist.CLIENT)
    public static void registerColorHandler(RegisterColorHandlersEvent.Item event, ItemColor itemColor) {
        event.register(itemColor, AETHER_EMBER_BULB.get());
    }

    public static void initCuriosCategory() {
        ItemStack fullBulb = EmberStorageItem.withFill(CuriosCompat.AETHER_EMBER_BULB.get(), ((EmberStorageItem)CuriosCompat.AETHER_EMBER_BULB.get()).getCapacity());

        AWResearch.moonsnare_bulb = new ResearchBase("aw.moonsnare_bulb", fullBulb, 5, 1).addAncestor(ResearchManager.mantle_bulb);

        ResearchManager.subCategoryBaubles.addResearch(AWResearch.moonsnare_bulb);
    }
}
