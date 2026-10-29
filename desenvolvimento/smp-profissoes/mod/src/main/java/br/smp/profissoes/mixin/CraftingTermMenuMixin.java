package br.smp.profissoes.mixin;

import appeng.menu.AEBaseMenu;
import appeng.menu.me.items.CraftingTermMenu;
import br.smp.profissoes.Contexto;
import br.smp.profissoes.MarcadorProtecao;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** AE2 Crafting Terminal: o resultado mostrado é calculado com o jogador do terminal como contexto. */
@Mixin(value = CraftingTermMenu.class, remap = false)
public abstract class CraftingTermMenuMixin implements MarcadorProtecao {
    @Inject(method = "updateCurrentRecipeAndOutput(Z)V", at = @At("HEAD"), remap = false)
    private void smpprofissoes$entrar(boolean forcar, CallbackInfo ci) { Contexto.entrar(((AEBaseMenu) (Object) this).getPlayer()); }

    @Inject(method = "updateCurrentRecipeAndOutput(Z)V", at = @At("RETURN"), remap = false)
    private void smpprofissoes$sair(boolean forcar, CallbackInfo ci) { Contexto.sair(); }
}
