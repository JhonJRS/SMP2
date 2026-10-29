package br.smp.profissoes.mixin;

import br.smp.profissoes.Contexto;
import br.smp.profissoes.MarcadorProtecao;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Botania Assembly Halo: fabrica direto do inventário; o jogador é o contexto. */
@Mixin(targets = "velrondevs.botania.common.item.AssemblyHaloItem", remap = false)
public abstract class AssemblyHaloMixin implements MarcadorProtecao {
    @Inject(method = "onItemCrafted(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/Container;)V", at = @At("HEAD"), remap = false)
    private static void smpprofissoes$entrar(Player p, Container c, CallbackInfo ci) { Contexto.entrar(p); }

    @Inject(method = "onItemCrafted(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/Container;)V", at = @At("RETURN"), remap = false)
    private static void smpprofissoes$sair(Player p, Container c, CallbackInfo ci) { Contexto.sair(); }
}
