package br.smp.profissoes.mixin;

import br.smp.profissoes.Contexto;
import br.smp.profissoes.MarcadorProtecao;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cliques em qualquer menu (cortador, ferraria, bancadas de mods...) e o livro de receitas: o jogador que clicou
 * é o contexto. Só no thread do servidor (no thread de rede o pacote é apenas reagendado).
 */
@Mixin(value = ServerGamePacketListenerImpl.class, remap = false)
public abstract class ServerGamePacketListenerImplMixin implements MarcadorProtecao {
    @Shadow(remap = false) public ServerPlayer player;

    private boolean smpprofissoes$noServidor() {
        return player != null && player.getServer() != null && player.getServer().isSameThread();
    }

    @Inject(method = "handleContainerClick(Lnet/minecraft/network/protocol/game/ServerboundContainerClickPacket;)V", at = @At("HEAD"), remap = false)
    private void smpprofissoes$clickEntrar(ServerboundContainerClickPacket pkt, CallbackInfo ci) {
        if (smpprofissoes$noServidor()) Contexto.entrar(player);
    }

    @Inject(method = "handleContainerClick(Lnet/minecraft/network/protocol/game/ServerboundContainerClickPacket;)V", at = @At("RETURN"), remap = false)
    private void smpprofissoes$clickSair(ServerboundContainerClickPacket pkt, CallbackInfo ci) {
        if (smpprofissoes$noServidor()) Contexto.sair();
    }

    @Inject(method = "handleContainerButtonClick(Lnet/minecraft/network/protocol/game/ServerboundContainerButtonClickPacket;)V", at = @At("HEAD"), remap = false)
    private void smpprofissoes$botaoEntrar(ServerboundContainerButtonClickPacket pkt, CallbackInfo ci) {
        if (smpprofissoes$noServidor()) Contexto.entrar(player);
    }

    @Inject(method = "handleContainerButtonClick(Lnet/minecraft/network/protocol/game/ServerboundContainerButtonClickPacket;)V", at = @At("RETURN"), remap = false)
    private void smpprofissoes$botaoSair(ServerboundContainerButtonClickPacket pkt, CallbackInfo ci) {
        if (smpprofissoes$noServidor()) Contexto.sair();
    }

    @Inject(method = "handlePlaceRecipe(Lnet/minecraft/network/protocol/game/ServerboundPlaceRecipePacket;)V", at = @At("HEAD"), remap = false)
    private void smpprofissoes$livroEntrar(ServerboundPlaceRecipePacket pkt, CallbackInfo ci) {
        if (smpprofissoes$noServidor()) Contexto.entrar(player);
    }

    @Inject(method = "handlePlaceRecipe(Lnet/minecraft/network/protocol/game/ServerboundPlaceRecipePacket;)V", at = @At("RETURN"), remap = false)
    private void smpprofissoes$livroSair(ServerboundPlaceRecipePacket pkt, CallbackInfo ci) {
        if (smpprofissoes$noServidor()) Contexto.sair();
    }
}
