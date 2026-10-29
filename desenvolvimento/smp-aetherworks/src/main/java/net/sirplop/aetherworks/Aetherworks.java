package net.sirplop.aetherworks;
import net.sirplop.aetherworks.AWRegistry;

import com.mojang.logging.LogUtils;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.sirplop.aetherworks.blockentity.render.*;
import net.sirplop.aetherworks.client.AWClientEvents;
import net.sirplop.aetherworks.client.AWKeybinds;
import net.sirplop.aetherworks.client.AetherShieldReflectHandler;
import net.sirplop.aetherworks.compat.curios.CuriosCompat;
import net.sirplop.aetherworks.datagen.AWItemProperties;
import net.sirplop.aetherworks.datagen.AWSounds;
import net.sirplop.aetherworks.entity.render.DummyAetherCrownRender;
import net.sirplop.aetherworks.item.AetherCrownItem;
import net.sirplop.aetherworks.item.AetherEmberColorHandler;
import net.sirplop.aetherworks.item.PotionGemItem;
import net.sirplop.aetherworks.lib.AWHarvestHelper;
import net.sirplop.aetherworks.model.AetherCrownGemLayer;
import net.sirplop.aetherworks.model.AetherCrownModel;
import net.sirplop.aetherworks.network.PacketHandler;
import net.sirplop.aetherworks.research.AWResearch;
import org.slf4j.Logger;

// SMP 2 port (1.21.1 NeoForge): datagen removed (the generated data ships in the jar), registration through the mod
// bus given to the constructor, capabilities bridged in AWRegistry#registerCapabilities.
@Mod(Aetherworks.MODID)
public class Aetherworks
{
    public static final String MODID = "aetherworks";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Aetherworks(IEventBus modEventBus, ModContainer modContainer)
    {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(AWRegistry::registerCapabilities);
        modEventBus.addListener(PacketHandler::register);

        AWRegistry.BLOCKS.register(modEventBus);
        AWRegistry.ITEMS.register(modEventBus);
        AWRegistry.DATA_COMPONENTS.register(modEventBus);
        AWRegistry.ATTACHMENT_TYPES.register(modEventBus);
        AWRegistry.FLUIDTYPES.register(modEventBus);
        AWRegistry.FLUIDS.register(modEventBus);
        AWRegistry.ENTITY_TYPES.register(modEventBus);
        AWRegistry.ATTRIBUTES.register(modEventBus);
        AWRegistry.BLOCK_ENTITY_TYPES.register(modEventBus);
        AWRegistry.CREATIVE_MODE_TAB.register(modEventBus);
        AWRegistry.MOB_EFFECTS.register(modEventBus);
        AWRegistry.SOUND_EVENTS.register(modEventBus);
        AWRegistry.RECIPE_TYPES.register(modEventBus);
        AWRegistry.RECIPE_SERIALIZERS.register(modEventBus);
        AWRegistry.STRUCTURE_TYPES.register(modEventBus);
        AWRegistry.STRUCTURE_PIECES.register(modEventBus);
        AWSounds.init();

        AWConfig.register(modContainer);

        if (ModList.get().isLoaded("curios")) {
            CuriosCompat.init();
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        LOGGER.atInfo().log("Refracting Aetherium...");
        AWRegistry.init(event);
        event.enqueueWork(AWResearch::initResearch);

        NeoForge.EVENT_BUS.addListener(AWHarvestHelper::onServerTick);
        NeoForge.EVENT_BUS.addListener(AWHarvestHelper::onLevelUnload);
        NeoForge.EVENT_BUS.addListener(AWHarvestHelper::onPlayerLeave);
    }

    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void clientSetup(FMLClientSetupEvent event) {
            NeoForge.EVENT_BUS.addListener(AetherShieldReflectHandler::onUpdateEvent);
            NeoForge.EVENT_BUS.addListener(AetherShieldReflectHandler::onLevelUnload);
            NeoForge.EVENT_BUS.addListener(AetherShieldReflectHandler::onEntityLeaveEvent);

            event.enqueueWork(() -> {
                AWItemProperties.register();

                ItemBlockRenderTypes.setRenderLayer(AWRegistry.AETHERIUM_GAS.FLUID.get(), RenderType.translucent());
                ItemBlockRenderTypes.setRenderLayer(AWRegistry.AETHERIUM_GAS.FLUID_FLOW.get(), RenderType.translucent());
                ItemBlockRenderTypes.setRenderLayer(AWRegistry.AETHERIUM_GAS_IMPURE.FLUID.get(), RenderType.translucent());
                ItemBlockRenderTypes.setRenderLayer(AWRegistry.AETHERIUM_GAS_IMPURE.FLUID_FLOW.get(), RenderType.translucent());
                ItemBlockRenderTypes.setRenderLayer(AWRegistry.ALCHEMIC_PRECURSOR.FLUID.get(), RenderType.translucent());
                ItemBlockRenderTypes.setRenderLayer(AWRegistry.ALCHEMIC_PRECURSOR.FLUID_FLOW.get(), RenderType.translucent());
                ItemBlockRenderTypes.setRenderLayer(AWRegistry.SEETHING_AETHERIUM.FLUID.get(), RenderType.translucent());
                ItemBlockRenderTypes.setRenderLayer(AWRegistry.SEETHING_AETHERIUM.FLUID_FLOW.get(), RenderType.translucent());
            });
        }

        @SubscribeEvent
        public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
            AWClientEvents.registerAdditional(event);
        }

        @SubscribeEvent
        public static void afterModelBake(ModelEvent.BakingCompleted event) {
            AWClientEvents.afterModelBake(event);
        }

        @SubscribeEvent
        public static void overlayRegister(RegisterGuiLayersEvent event) {
            event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(MODID, "aw_overlay"), AWClientEvents.INGAME_OVERLAY);
        }

        @SubscribeEvent
        static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(AWRegistry.DUMMY_LOADER.get(), DummyAetherCrownRender::new);

            event.registerBlockEntityRenderer(AWRegistry.PRISM_BLOCK_ENTITY.get(), RenderPrism::new);
            event.registerBlockEntityRenderer(AWRegistry.AETHER_FORGE_BLOCK_ENTITY.get(), RenderAetherForge::new);
            event.registerBlockEntityRenderer(AWRegistry.METAL_FORMER_BLOCK_ENTITY.get(), RenderMetalFormer::new);
            event.registerBlockEntityRenderer(AWRegistry.AETHERIUM_ANVIL_BLOCK_ENTITY.get(), RenderAetherAnvil::new);
            event.registerBlockEntityRenderer(AWRegistry.TOOL_STATION_BLOCK_ENTITY.get(), RenderToolStation::new);
            event.registerBlockEntityRenderer(AWRegistry.LEXICON_RECEPTACLE_BLOCK_ENTITY.get(), RenderLexiconReceptacle::new);
        }

        @SubscribeEvent
        @SuppressWarnings({"unchecked", "rawtypes"})
        static void registerLayers(EntityRenderersEvent.AddLayers event) {
            for (var skin : event.getSkins()) {
                EntityRenderer<?> r = event.getSkin(skin);
                if (r instanceof LivingEntityRenderer living)
                    living.addLayer(new AetherCrownGemLayer(living, event.getEntityModels()));
            }
            for (var type : event.getEntityTypes()) {
                EntityRenderer<?> r = event.getRenderer(type);
                if (r instanceof LivingEntityRenderer living)
                    living.addLayer(new AetherCrownGemLayer(living, event.getEntityModels()));
            }
        }

        @SubscribeEvent
        static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(AetherCrownModel.CROWN_HEAD, () -> LayerDefinition.create(AetherCrownModel.createHeadMesh(), 32,32));
        }

        @SubscribeEvent
        static void registerItemColorHandlers(RegisterColorHandlersEvent.Item event){
            // SMP 2 port: 1.21 item tints are ARGB; the 1.20 handlers return RGB (alpha 0 = invisible layer), so make them opaque
            event.register(opaco(new PotionGemItem.ColorHandler()), AWRegistry.POTION_GEM.get());
            event.register(opaco(new AetherCrownItem.ColorHandler()), AWRegistry.AETHER_CROWN.get());

            net.minecraft.client.color.item.ItemColor emberColor = opaco(new AetherEmberColorHandler());

            if (ModList.get().isLoaded("curios"))
                CuriosCompat.registerColorHandler(event, emberColor);
            event.register(emberColor, AWRegistry.AETHER_EMBER_JAR.get(), AWRegistry.AETHER_EMBER_CARTRIDGE.get());
        }

        private static net.minecraft.client.color.item.ItemColor opaco(net.minecraft.client.color.item.ItemColor handler) {
            return (stack, tint) -> net.minecraft.util.FastColor.ARGB32.opaque(handler.getColor(stack, tint));
        }

        @SubscribeEvent
        public static void registerKeyMappings(final RegisterKeyMappingsEvent event) {
            event.register(AWKeybinds.MODE_CHANGE);
        }
    }
}
