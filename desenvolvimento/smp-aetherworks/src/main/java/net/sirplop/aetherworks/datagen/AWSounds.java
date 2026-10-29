package net.sirplop.aetherworks.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.SoundDefinition;
import net.neoforged.neoforge.common.data.SoundDefinitionsProvider;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.sirplop.aetherworks.AWRegistry;
import net.sirplop.aetherworks.Aetherworks;

public class AWSounds extends SoundDefinitionsProvider {
    public AWSounds(PackOutput output, ExistingFileHelper helper) {
        super(output, Aetherworks.MODID, helper);
    }

    //this is just here so the class loads, nothing else needs to happen here
    public static void init() {}

    //Sounds
    public static final DeferredHolder<SoundEvent, SoundEvent> FORGE_GROAN = registerSoundEvent("block.forge_groan");
    public static final DeferredHolder<SoundEvent, SoundEvent> AETHER_SHIELD_RAISE_1 = registerSoundEvent("item.aethershield.raise1");
    public static final DeferredHolder<SoundEvent, SoundEvent> AETHER_SHIELD_RAISE_2 = registerSoundEvent("item.aethershield.raise2");
    public static final DeferredHolder<SoundEvent, SoundEvent> AETHER_SHIELD_LOWER = registerSoundEvent("item.aethershield.lower");

    public static DeferredHolder<SoundEvent, SoundEvent> registerSoundEvent(String name) {
        return AWRegistry.SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Aetherworks.MODID, name)));
    }
    @Override
    public void registerSounds() {
        withSubtitle(FORGE_GROAN, definition().with(
                sound(resource("forge_groan"))));
        withSubtitle(AETHER_SHIELD_RAISE_1, definition().with(
                sound(resource("shield_raise_1"))), "item.aethershield.raise");
        withSubtitle(AETHER_SHIELD_RAISE_2, definition().with(
                sound(resource("shield_raise_2"))), "item.aethershield.raise");
        withSubtitle(AETHER_SHIELD_LOWER, definition().with(
                sound(resource("shield_lower"))));
    }


    public void withSubtitle(DeferredHolder<SoundEvent, SoundEvent> soundEvent, SoundDefinition definition) {
        add(soundEvent, definition.subtitle("subtitles." + Aetherworks.MODID + "." + soundEvent.getId().getPath()));
    }
    public void withSubtitle(DeferredHolder<SoundEvent, SoundEvent> soundEvent, SoundDefinition definition, String overridePath) {
        add(soundEvent, definition.subtitle("subtitles." + Aetherworks.MODID + "." + overridePath));
    }
    public ResourceLocation resource(String path) {
        return ResourceLocation.fromNamespaceAndPath(Aetherworks.MODID, path);
    }
}
