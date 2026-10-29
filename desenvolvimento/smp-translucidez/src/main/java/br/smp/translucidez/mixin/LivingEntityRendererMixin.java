package br.smp.translucidez.mixin;

import br.smp.translucidez.Anel;
import br.smp.translucidez.Config;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    /** Vanilla desenha o corpo translúcido com 0x26FFFFFF (~15%); aqui fica com a opacidade da config. */
    @ModifyConstant(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            constant = @Constant(intValue = 654311423))
    private int smptranslucidez$opacidade(int original) {
        return Anel.jogadorLocalUsa() ? Config.corAnel : Config.cor;
    }

    /** O nome continua oculto como no vanilla, salvo se mostrar_nome=true. */
    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("RETURN"), cancellable = true)
    private void smptranslucidez$nome(LivingEntity entidade, CallbackInfoReturnable<Boolean> cir) {
        if (!Config.mostrarNome && entidade instanceof Player && entidade.isInvisible() && cir.getReturnValueZ()) {
            cir.setReturnValue(false);
        }
    }
}
