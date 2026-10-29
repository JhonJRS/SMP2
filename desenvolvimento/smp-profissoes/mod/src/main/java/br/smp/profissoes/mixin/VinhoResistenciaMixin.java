package br.smp.profissoes.mixin;

import br.smp.profissoes.MarcadorProtecao;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vinery: o nível do efeito do vinho sobe com a idade (até o maxLevel do config, 5 = Resistência VI = imunidade).
 * O Apple Wine dá Resistência: ele para de "envelhecer" em Resistência IV (amplificador 3). Vale para o efeito e a dica.
 */
@Pseudo
@Mixin(targets = "net.satisfy.vinery.core.util.WineYears", remap = false)
public abstract class VinhoResistenciaMixin implements MarcadorProtecao {
    @Inject(method = "getEffectLevel(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;)I", at = @At("RETURN"), cancellable = true, remap = false)
    private static void smpprofissoes$limitar(ItemStack st, Level level, CallbackInfoReturnable<Integer> cir) {
        if (cir.getReturnValueI() > 3 && st != null && "vinery:apple_wine".equals(BuiltInRegistries.ITEM.getKey(st.getItem()).toString())) {
            cir.setReturnValue(3); // Resistência IV
        }
    }
}
