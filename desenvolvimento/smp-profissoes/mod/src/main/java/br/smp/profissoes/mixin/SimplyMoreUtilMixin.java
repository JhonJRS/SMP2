package br.smp.profissoes.mixin;

import br.smp.profissoes.MarcadorProtecao;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Simply More: recarga de itens (chamada pelo Stun e pelas armas) só no servidor — o servidor manda a recarga ao cliente. */
@Pseudo
@Mixin(targets = "net.rosemarythyme.simplymore.util.EntityUtils", remap = false)
public abstract class SimplyMoreUtilMixin implements MarcadorProtecao {

    @Inject(method = "putAllItemsOnCooldown(Lnet/minecraft/world/entity/LivingEntity;I)V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void smpprofissoes$todosSoNoServidor(LivingEntity quem, int ticks, CallbackInfo ci) {
        if (quem.level().isClientSide) ci.cancel();
    }

    @Inject(method = "cooldown(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/Item;IZ)V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void smpprofissoes$umSoNoServidor(LivingEntity quem, Item item, int ticks, boolean b, CallbackInfo ci) {
        if (quem.level().isClientSide) ci.cancel();
    }
}
