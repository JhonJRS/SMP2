package net.sirplop.aetherworks.datagen;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageType;
import net.sirplop.aetherworks.Aetherworks;

// SMP 2 port: constants only (the data provider ran at build time; its output ships in the jar)
public final class AWDamageTypes {
    private AWDamageTypes() {}

    public static final ResourceKey<DamageType> MOON_EMBER_KEY = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Aetherworks.MODID, "moon_ember"));
    public static final DamageType MOON_EMBER = new DamageType("moon_ember", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1F);
}
