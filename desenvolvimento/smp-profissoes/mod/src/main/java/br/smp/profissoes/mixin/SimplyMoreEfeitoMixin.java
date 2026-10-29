package br.smp.profissoes.mixin;

import br.smp.profissoes.MarcadorProtecao;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Simply More: efeitos (Stun, Misty, Revvengine Rush, Tidebreaker) agem só no servidor; no cliente o efeito apenas continua. */
@Pseudo
@Mixin(targets = {"net.rosemarythyme.simplymore.effect.StunEffect", "net.rosemarythyme.simplymore.effect.MistyEffect",
        "net.rosemarythyme.simplymore.effect.RevvengineRushEffect", "net.rosemarythyme.simplymore.effect.TidebreakerEffect"}, remap = false)
public abstract class SimplyMoreEfeitoMixin implements MarcadorProtecao {

    @Inject(method = "applyEffectTick(Lnet/minecraft/world/entity/LivingEntity;I)Z", at = @At("HEAD"), cancellable = true, remap = false)
    private void smpprofissoes$soNoServidor(LivingEntity quem, int nivel, CallbackInfoReturnable<Boolean> cir) {
        if (quem.level().isClientSide) cir.setReturnValue(true);
    }
}
