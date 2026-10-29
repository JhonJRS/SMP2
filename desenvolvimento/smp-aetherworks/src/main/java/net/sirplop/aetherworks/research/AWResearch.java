package net.sirplop.aetherworks.research;

import com.rekindled.embers.Embers;
import com.rekindled.embers.item.EmberStorageItem;
import com.rekindled.embers.research.ResearchBase;
import com.rekindled.embers.research.ResearchCategory;
import com.rekindled.embers.research.ResearchManager;
import com.rekindled.embers.research.subtypes.ResearchFakePage;
import com.rekindled.embers.research.subtypes.ResearchShowItem;
import com.rekindled.embers.research.subtypes.ResearchSwitchCategory;
import com.rekindled.embers.util.Vec2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;
import net.sirplop.aetherworks.AWRegistry;
import net.sirplop.aetherworks.Aetherworks;
import net.sirplop.aetherworks.compat.curios.CuriosCompat;

public class AWResearch {
    public static final ResourceLocation PAGE_ICONS = ResourceLocation.fromNamespaceAndPath(Aetherworks.MODID, "textures/gui/codex_index_icons.png");
    public static final double PAGE_ICON_SIZE = 48;

    public static ResearchShowImage.DisplayImage moon_harvester_image = new ResearchShowImage.DisplayImage(ResourceLocation.fromNamespaceAndPath(Aetherworks.MODID, "textures/gui/moonlight_harvester.png"),
            -21, 48, 0, 0, 160, 128, 256, 256);

    public static ResearchCategory categoryAether;
    public static ResearchCategory subCategoryTools;
    public static ResearchCategory subCategoryAlchemy;
    public static ResearchBase meteor, gauge, amalgam, moon_harvester, focus_matrix, purify_aetherium, alchemy, forge, heat_dial, forge_heat, metal_former, anvil, tool_station, pearls, tools, lexicon;
    public static ResearchBase pobs, pomd, aotr, aosa, sotc, soic, hovh, hohs, cosb, cosr, crown, shield; //TOOLS
    public static ResearchBase tuning_cylinder, volant_calcifier, agrarian_liners, aether_plate;
    public static ResearchBase moonsnare_jars, moonsnare_bulb;
    public static ResearchBase seething_aetherium, aetherium_glass, aspectus;

    public static void initResearch() {
        categoryAether = new ResearchCategory("aw.aether", PAGE_ICONS, 192, 0);
        Vec2i[] ringPositions = {new Vec2i(1, 1), new Vec2i(0, 3), new Vec2i(0, 5), new Vec2i(1, 7), new Vec2i(11, 7), new Vec2i(12, 5), new Vec2i(12, 3), new Vec2i(11, 1), new Vec2i(4, 1), new Vec2i(2, 4), new Vec2i(4, 7), new Vec2i(8, 7), new Vec2i(10, 4),new Vec2i(8, 1)};
        subCategoryTools = new ResearchCategory("aw.tools", 0).pushGoodLocations(ringPositions);
        subCategoryAlchemy = new ResearchCategory("aw.alchemy", 0);

        meteor = new ResearchBase("aw.meteor", new ItemStack(AWRegistry.AETHER_SHARD.get()), 12, 0);
        meteor.addPage(new ResearchShowItem("aw.meteor_2", ItemStack.EMPTY, 0, 0).addItem(new ResearchShowItem.DisplayItem(new ItemStack(AWRegistry.SUEVITE.get()), new ItemStack(AWRegistry.AETHERIUM_ORE.get()))));
        gauge = new ResearchBase("aw.aetheriometer", new ItemStack(AWRegistry.AETHERIOMETER.get()), 9, 0).addAncestor(meteor);
        amalgam = new ResearchBase("aw.amalgam", new ItemStack(AWRegistry.AETHER_AMALGAM.get()), 11, 3).addAncestor(meteor);
        moon_harvester = new ResearchBase("aw.moon_harvester", new ItemStack(AWRegistry.PRISM.get()), 12, 7).addAncestor(amalgam);
        moon_harvester.addPage(new ResearchShowItem("aw.moon_harvester_2", ItemStack.EMPTY, 0, 0).addItem(new ResearchShowItem.DisplayItem(new ItemStack(AWRegistry.PRISM.get()), new ItemStack(AWRegistry.PRISM_SUPPORT.get()), new ItemStack(AWRegistry.MOONLIGHT_AMPLIFIER.get()))));
        moon_harvester.addPage(new ResearchShowItem("aw.moon_harvester_3", ItemStack.EMPTY, 0, 0).addItem(new ResearchShowItem.DisplayItem(new ItemStack(AWRegistry.PRISM.get()), new ItemStack(AWRegistry.PRISM_SUPPORT.get()), new ItemStack(AWRegistry.MOONLIGHT_AMPLIFIER.get()))));
        moon_harvester.addPage(new ResearchShowImage("aw.moon_harvester_4", ItemStack.EMPTY, 0, 0).addImage(moon_harvester_image));
        focus_matrix = new ResearchBase("aw.focus_matrix", new ItemStack(AWRegistry.CONTROL_MATRIX.get()), 9, 6).addAncestor(moon_harvester);
        purify_aetherium = new ResearchBase("aw.purify_aetherium", new ItemStack(AWRegistry.AETHERIUM_GAS.FLUID_BUCKET.get()), 7, 7).addAncestor(moon_harvester);
        purify_aetherium.addPage(new ResearchShowItem("aw.purify_aetherium_2", ItemStack.EMPTY, 0, 0).addItem(new ResearchShowItem.DisplayItem(new ItemStack(AWRegistry.AETHERIUM_GAS.FLUID_BUCKET.get()), new ItemStack(AWRegistry.SUEVITE_COBBLE.get()), new ItemStack(Items.WATER_BUCKET))));
        alchemy = new ResearchBase("aw.alchemy", new ItemStack(AWRegistry.AETHER_ASPECTUS.get()), 7, 4).addAncestor(purify_aetherium);

        forge = new ResearchBase("aw.forge", new ItemStack(AWRegistry.AETHER_FORGE.get()), 2, 5).addAncestor(purify_aetherium);
        forge.addPage(new ResearchBase("aw.forge_2", ItemStack.EMPTY, 0, 0));
        heat_dial = new ResearchBase("aw.heat_dial", new ItemStack(AWRegistry.HEAT_DIAL.get()), 1, 7).addAncestor(forge);
        forge_heat = new ResearchShowItem("aw.forge_heat", new ItemStack(AWRegistry.FORGE_HEATER.get()), 3, 7).addItem(new ResearchShowItem.DisplayItem(new ItemStack(AWRegistry.FORGE_VENT.get()), new ItemStack(AWRegistry.FORGE_HEATER.get()), new ItemStack(AWRegistry.FORGE_COOLER.get()))).addAncestor(forge);
        forge_heat.addPage(new ResearchShowItem("aw.forge_heat_2", ItemStack.EMPTY, 0, 0).addItem(new ResearchShowItem.DisplayItem(new ItemStack(AWRegistry.FORGE_HEATER.get()))));
        forge_heat.addPage(new ResearchShowItem("aw.forge_heat_3", ItemStack.EMPTY, 0, 0).addItem(new ResearchShowItem.DisplayItem(new ItemStack(AWRegistry.FORGE_COOLER.get()))));
        forge_heat.addPage(new ResearchShowItem("aw.forge_heat_4", ItemStack.EMPTY, 0, 0).addItem(new ResearchShowItem.DisplayItem(new ItemStack(AWRegistry.FORGE_VENT.get()))));
        metal_former = new ResearchBase("aw.metal_former", new ItemStack(AWRegistry.FORGE_METAL_FORMER.get()), 4, 3).addAncestor(forge);
        anvil = new ResearchBase("aw.anvil", new ItemStack(AWRegistry.FORGE_ANVIL.get()), 2, 2).addAncestor(forge);
        anvil.addPage(new ResearchBase("aw.anvil_2", ItemStack.EMPTY, 0, 0));
        tool_station = new ResearchBase("aw.tool_station", new ItemStack(AWRegistry.FORGE_TOOL_STATION.get()), 0, 3).addAncestor(forge);

        pearls = new ResearchBase("aw.pearls", new ItemStack(AWRegistry.AETHER_PEARL.get()), 4, 0).addAncestor(metal_former);
        tools = new ResearchBase("aw.tools", new ItemStack(AWRegistry.TOOL_ROD_INFUSED.get()), 0, 0).addAncestor(tool_station).addAncestor(pearls);
        lexicon = new ResearchBase("aw.lexicon", new ItemStack(AWRegistry.LEXICON.get()), 6.5, 1).addAncestor(pearls);
        lexicon.addPage(new ResearchShowItem("aw.lexicon_2", new ItemStack(AWRegistry.LEXICON_RECEPTACLE.get()), 0, 0).addItem(new ResearchShowItem.DisplayItem(new ItemStack(AWRegistry.LEXICON_RECEPTACLE.get()))));

        ResearchBase toolsFake = new ResearchFakePage(tools, 6, 4);

        pobs = new ResearchBase("aw.pobs", new ItemStack(AWRegistry.PICKAXE_AETHER.get()), subCategoryTools.popGoodLocation()).addAncestor(toolsFake);
        pomd = new ResearchBase("aw.pomd", new ItemStack(AWRegistry.PICKAXE_EMBER.get()), subCategoryTools.popGoodLocation()).addAncestor(toolsFake);
        aotr = new ResearchBase("aw.aotr", new ItemStack(AWRegistry.AXE_ENDER.get()), subCategoryTools.popGoodLocation()).addAncestor(toolsFake);
        aosa = new ResearchBase("aw.aosa", new ItemStack(AWRegistry.AXE_SCULK.get()), subCategoryTools.popGoodLocation()).addAncestor(toolsFake);
        soic = new ResearchBase("aw.soic", new ItemStack(AWRegistry.SHOVEL_SLIME.get()), subCategoryTools.popGoodLocation()).addAncestor(toolsFake);
        sotc = new ResearchBase("aw.sotc", new ItemStack(AWRegistry.SHOVEL_PRISMARINE.get()), subCategoryTools.popGoodLocation()).addAncestor(toolsFake);
        hovh = new ResearchBase("aw.hovh", new ItemStack(AWRegistry.HOE_AMETHYST.get()), subCategoryTools.popGoodLocation()).addAncestor(toolsFake);
        hohs = new ResearchBase("aw.hohs", new ItemStack(AWRegistry.HOE_HONEY.get()), subCategoryTools.popGoodLocation()).addAncestor(toolsFake);
        cosb = new ResearchBase("aw.cosb", new ItemStack(AWRegistry.CROSSBOW_QUARTZ.get()), subCategoryTools.popGoodLocation()).addAncestor(toolsFake);
        cosr = new ResearchBase("aw.cosr", new ItemStack(AWRegistry.CROSSBOW_MAGMA.get()), subCategoryTools.popGoodLocation()).addAncestor(toolsFake);

        crown = new ResearchBase("aw.crown", new ItemStack(AWRegistry.AETHER_CROWN.get()), subCategoryTools.popGoodLocation()).addAncestor(toolsFake);
        crown.addPage(new ResearchShowItem("aw.crown_gem", new ItemStack(AWRegistry.POTION_GEM.get()), 0, 0).addItem(new ResearchShowItem.DisplayItem(new ItemStack(AWRegistry.POTION_GEM.get()))));

        shield = new ResearchBase("aw.shield", new ItemStack(AWRegistry.AETHER_SHIELD.get()), subCategoryTools.popGoodLocation()).addAncestor(toolsFake);

        aspectus = new ResearchBase("aw.aspectus", new ItemStack(AWRegistry.AETHER_ASPECTUS.get()), 7, 4);
        seething_aetherium = new ResearchBase("aw.seething", new ItemStack(AWRegistry.SEETHING_AETHERIUM.FLUID_BUCKET.get()), 4, 4).addAncestor(aspectus);
        aetherium_glass = new ResearchShowItem("aw.glass", new ItemStack(AWRegistry.GLASS_AETHERIUM.get()), 2, 6).addItem(new ResearchShowItem.DisplayItem(new ItemStack(AWRegistry.GLASS_AETHERIUM.get()), new ItemStack(AWRegistry.GLASS_AETHERIUM_BORDERLESS.get())));

        tuning_cylinder = new ResearchBase("aw.tuning_cylinder", new ItemStack(AWRegistry.TUNING_CYLINDER.get()), ResearchManager.subCategoryWeaponAugments.popGoodLocation()).addAncestor(ResearchManager.inferno_forge);
        ResearchManager.subCategoryWeaponAugments.addResearch(tuning_cylinder);
        volant_calcifier = new ResearchBase("aw.volant_calcifier", new ItemStack(AWRegistry.VOLANT_CALCIFIER.get()), ResearchManager.subCategoryProjectileAugments.popGoodLocation()).addAncestor(ResearchManager.inferno_forge);
        ResearchManager.subCategoryProjectileAugments.addResearch(volant_calcifier);
        agrarian_liners = new ResearchBase("aw.agrarian_liners", new ItemStack(AWRegistry.AGRARIAN_LINERS.get()), ResearchManager.subCategoryArmorAugments.popGoodLocation()).addAncestor(ResearchManager.inferno_forge);
        ResearchManager.subCategoryArmorAugments.addResearch(agrarian_liners);
        aether_plate = new ResearchBase("aw.aether_plate", new ItemStack(AWRegistry.AETHERIAL_PLATING.get()), ResearchManager.subCategoryArmorAugments.popGoodLocation()).addAncestor(ResearchManager.inferno_forge);
        ResearchManager.subCategoryArmorAugments.addResearch(aether_plate);

        ItemStack fullJar = EmberStorageItem.withFill(AWRegistry.AETHER_EMBER_JAR.get(), ((EmberStorageItem)AWRegistry.AETHER_EMBER_JAR.get()).getCapacity());
        moonsnare_jars = new ResearchBase("aw.moonsnare_jars", fullJar, 6.5, 7).addAncestor(ResearchManager.jars);
        ResearchManager.categoryMetallurgy.addResearch(moonsnare_jars);

        if (ModList.get().isLoaded("curios")) {
            CuriosCompat.initCuriosCategory();
        }

        subCategoryTools
                .addResearch(toolsFake)
                .addResearch(pobs)
                .addResearch(pomd)
                .addResearch(aotr)
                .addResearch(aosa)
                .addResearch(soic)
                .addResearch(sotc)
                .addResearch(hovh)
                .addResearch(hohs)
                .addResearch(cosb)
                .addResearch(cosr)
                .addResearch(crown)
                .addResearch(shield);

        subCategoryAlchemy
                .addResearch(aspectus)
                .addResearch(aetherium_glass)
                .addResearch(seething_aetherium);

        ResearchBase toolsSwitch = makeCategorySwitch(subCategoryTools, 0, 0, new ItemStack(AWRegistry.AETHER_CROWN.get()), 0, 1);
        tools.subCategory = toolsSwitch.addAncestor(tool_station).addAncestor(pearls);

        ResearchBase alchemySwitch = makeCategorySwitch(subCategoryAlchemy, 7, 4, new ItemStack(AWRegistry.AETHER_ASPECTUS.get()), 0, 1);
        alchemy.subCategory = alchemySwitch.addAncestor(purify_aetherium);

        categoryAether
                .addResearch(meteor)
                .addResearch(gauge)
                .addResearch(pearls)
                .addResearch(lexicon)
                .addResearch(amalgam)
                .addResearch(moon_harvester)
                .addResearch(focus_matrix)
                .addResearch(purify_aetherium)
                .addResearch(forge)
                .addResearch(heat_dial)
                .addResearch(forge_heat)
                .addResearch(metal_former)
                .addResearch(anvil)
                .addResearch(tool_station)
                .addResearch(toolsSwitch)
                .addResearch(alchemySwitch);

        categoryAether.addPrerequisite(ResearchManager.dawnstone);
        ResearchManager.researches.add(categoryAether);
    }

    private static ResearchSwitchCategory makeCategorySwitch(ResearchCategory targetCategory, int x, int y, ItemStack icon, int u, int v) {
        return (ResearchSwitchCategory) new ResearchSwitchCategory(targetCategory.name+"_category", icon, x, y).setTargetCategory(targetCategory).setIconBackground(ResearchManager.PAGE_ICONS, ResearchManager.PAGE_ICON_SIZE * u, ResearchManager.PAGE_ICON_SIZE * v);
    }
}
