package net.sirplop.aetherworks.item;
import net.minecraft.world.item.Item;
import net.sirplop.aetherworks.util.Compat121;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;

import net.minecraft.ChatFormatting;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.sirplop.aetherworks.AWConfig;
import net.sirplop.aetherworks.AWRegistry;
import net.sirplop.aetherworks.Aetherworks;

import java.util.*;

public class PotionGemItem extends Item {
    public PotionGemItem(Properties pProperties) {
        super(pProperties);
    }
    public static final String POTION_COLOR = "gem_color";
    public static final int DEFAULT_COLOR = 0x0b618f;

    public static int getColor(ItemStack stack) {
        if (Compat121.tag(stack).contains(PotionGemItem.POTION_COLOR))
            return Compat121.tag(stack).getInt(PotionGemItem.POTION_COLOR);
        return DEFAULT_COLOR;
    }

    // SMP 2 port: effects live in the POTION_CONTENTS component (custom effects only), the colour in custom data.
    public static List<MobEffectInstance> getEffects(ItemStack stack) {
        List<MobEffectInstance> list = new ArrayList<>();
        stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).getAllEffects().forEach(list::add);
        return list;
    }

    public void setEffects(ItemStack potion, ItemStack stack) {
        Map<Holder<MobEffect>, Holder<MobEffect>> repl = AWConfig.getPotionGemReplacements();
        PotionContents contents = potion.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        List<MobEffectInstance> effects = replaceEffects(contents.potion().map(p -> p.value().getEffects()).orElse(List.of()), repl);
        effects.addAll(replaceEffects(contents.customEffects(), repl));

        setEffects(effects, stack);
    }

    public void setEffects(List<MobEffectInstance> effects, ItemStack stack) {
        for (int i = 0; i < effects.size(); i++) {
            MobEffectInstance inst = effects.get(i);
            effects.set(i, new MobEffectInstance(inst.getEffect(), 200,
                    inst.getAmplifier(), true, inst.isVisible(), inst.showIcon()));
        }
        setEffectsForRecipe(effects, stack, PotionContents.getColor(effects));
    }

    public static void setEffectsForRecipe(List<MobEffectInstance> effects, ItemStack stack, int color) {
        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.empty(), Optional.empty(), List.copyOf(effects)));
        Compat121.edit(stack, tag -> tag.putInt(POTION_COLOR, color));
    }

    public boolean hasEffect(ItemStack stack) {
        return !getEffects(stack).isEmpty();
    }

    private static class EffectHelper {
        List<Holder<MobEffect>> effects;
        List<Integer> levels;

        public EffectHelper(List<MobEffectInstance> instance) {
            effects= new ArrayList<>();
            levels = new ArrayList<>();
            for (MobEffectInstance eff : instance) {
                effects.add(eff.getEffect());
                levels.add(eff.getAmplifier());
            }
        }
        public boolean isEmpty() {
            return effects.isEmpty();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof EffectHelper that)) return false;
            return Objects.equals(effects, that.effects) && Objects.equals(levels, that.levels);
        }

        @Override
        public int hashCode() {
            return Objects.hash(effects, levels);
        }
    }

    public static void getAllPotionGems(CreativeModeTab.Output output) {
        Map<Holder<MobEffect>, Holder<MobEffect>> repl = AWConfig.getPotionGemReplacements();
        Set<EffectHelper> dupeCheck = new HashSet<>();
        for (Potion pot : BuiltInRegistries.POTION) {
            List<MobEffectInstance> effects = replaceEffects(pot.getEffects(), repl);
            //check for duplicate (aka same effect level, different duration
            EffectHelper help = new EffectHelper(effects);
            if (help.isEmpty() || dupeCheck.contains(help)) {
                continue;
            }
            dupeCheck.add(help);

            ItemStack stack = new ItemStack(AWRegistry.POTION_GEM.get());
            ((PotionGemItem)stack.getItem()).setEffects(effects, stack);
            output.accept(stack);
        }
    }

    private static List<MobEffectInstance> replaceEffects(List<MobEffectInstance> list, Map<Holder<MobEffect>, Holder<MobEffect>> replacementEffects) {
        List<MobEffectInstance> repl = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            MobEffectInstance inst = list.get(i);
            if (replacementEffects.containsKey(inst.getEffect())) {
                repl.add(new MobEffectInstance(replacementEffects.get(inst.getEffect()), 200, inst.getAmplifier(),inst.isAmbient(), inst.isVisible(), inst.showIcon()));
            }
            else
                repl.add(inst);
        }
        return repl;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext level, List<Component> tooltip, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltip, isAdvanced);
        tooltip.add(Component.translatable(Aetherworks.MODID + ".tooltip.gem_effect").withStyle(ChatFormatting.GRAY));
        addTooltip(getEffects(stack), tooltip);
    }

    public static final MutableComponent NO_EFFECT = Component.literal("  ").append(Component.translatable("effect.none").withStyle(ChatFormatting.GRAY));

    public static void addTooltip(List<MobEffectInstance> effects, List<Component> tooltip) {
        MutableComponent component;
        MobEffect effect;
        if (effects.isEmpty()) {
            tooltip.add(NO_EFFECT);
        } else {
            for(Iterator<MobEffectInstance> iter = effects.iterator(); iter.hasNext(); tooltip.add(component.withStyle(effect.getCategory().getTooltipFormatting()))) {
                MobEffectInstance curEffect = iter.next();
                component = Component.literal("  ").append(Component.translatable(curEffect.getDescriptionId()));
                effect = curEffect.getEffect().value();
                if (curEffect.getAmplifier() > 0) {
                    component = Component.translatable("potion.withAmplifier", component, Component.translatable("potion.potency." + curEffect.getAmplifier()));
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class ColorHandler implements ItemColor {
        @Override
        public int getColor(ItemStack itemStack, int i) {
            if (i == 0 && Compat121.tag(itemStack).contains(PotionGemItem.POTION_COLOR)) {
                return Compat121.tag(itemStack).getInt(PotionGemItem.POTION_COLOR);
            }
            return 0xFFFFFFFF;
        }
    }
}
