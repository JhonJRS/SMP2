package br.smp.profissoes.mixin;

import br.smp.profissoes.Contexto;
import br.smp.profissoes.MarcadorProtecao;
import com.simibubi.create.content.equipment.blueprint.BlueprintMenu;
import com.simibubi.create.foundation.gui.menu.MenuBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Create Blueprint (tela de configurar a receita): o jogador que edita é o contexto. */
@Mixin(value = BlueprintMenu.class, remap = false)
public abstract class BlueprintMenuMixin implements MarcadorProtecao {
    @Inject(method = "onCraftMatrixChanged()V", at = @At("HEAD"), remap = false)
    private void smpprofissoes$entrar(CallbackInfo ci) { Contexto.entrar(((MenuBase<?>) (Object) this).player); }

    @Inject(method = "onCraftMatrixChanged()V", at = @At("RETURN"), remap = false)
    private void smpprofissoes$sair(CallbackInfo ci) { Contexto.sair(); }
}
