package br.smp.profissoes.mixin;

import br.smp.profissoes.MarcadorProtecao;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Simply More: o golpe das armas únicas (cast para ServerLevel) só no servidor; o cliente também chama hurtEnemy ao atacar. */
@Pseudo
@Mixin(targets = "net.rosemarythyme.simplymore.item.SimplyMoreUniqueSwordItem", remap = false)
public abstract class SimplyMoreGolpeMixin implements MarcadorProtecao {

    @Inject(method = "hurtEnemy(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)Z",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void smpprofissoes$soNoServidor(ItemStack st, LivingEntity alvo, LivingEntity atacante, CallbackInfoReturnable<Boolean> cir) {
        if (atacante.level().isClientSide) cir.setReturnValue(true);
    }
}
