package br.smp.profissoes.mixin;

import appeng.blockentity.misc.CondenserBlockEntity;
import br.smp.profissoes.MarcadorProtecao;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A Singularity do AE2 só sai da receita da Guild of Trades (itens de todas as profissões); o Condensador não a produz. */
@Mixin(value = CondenserBlockEntity.class, remap = false)
public abstract class CondensadorAE2Mixin implements MarcadorProtecao {

    @Inject(method = "getOutput()Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"), cancellable = true, remap = false)
    private void smpprofissoes$semSingularidade(CallbackInfoReturnable<ItemStack> cir) {
        ItemStack s = cir.getReturnValue();
        if (s != null && !s.isEmpty() && "ae2:singularity".equals(BuiltInRegistries.ITEM.getKey(s.getItem()).toString())) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
