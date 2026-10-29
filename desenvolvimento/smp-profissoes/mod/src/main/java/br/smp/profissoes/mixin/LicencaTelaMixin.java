package br.smp.profissoes.mixin;

import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * RPEssentials 5.0.0_beta: o handler que abre a tela da licença (clique direito no item de licença) também recebe o
 * evento do servidor integrado e chama Minecraft.setScreen na thread do servidor — o jogo fechava (crash de 28/10 ao
 * beber o Draught of Oblivion: a licença de Vagabundo caiu na mão ainda com o botão apertado). Só deixa rodar no cliente.
 */
@Mixin(targets = "net.rp.rpessentials.client.LicenseClientHandler", remap = false)
public abstract class LicencaTelaMixin {
    @Inject(method = "onRightClickItem", at = @At("HEAD"), cancellable = true, remap = false)
    private static void smpprofissoes$soNoCliente(PlayerInteractEvent.RightClickItem ev, CallbackInfo ci) {
        if (!ev.getLevel().isClientSide()) ci.cancel();
    }
}
