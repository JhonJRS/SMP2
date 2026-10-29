package br.smp.profissoes.mixin;

import appeng.helpers.InventoryAction;
import appeng.menu.slot.CraftingTermSlot;
import br.smp.profissoes.Contexto;
import br.smp.profissoes.MarcadorProtecao;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * AE2 Crafting Terminal: retirar o resultado (clique ou shift) fabrica pela rede. Este slot não é o ResultSlot
 * vanilla, então o RPEssentials não o via; aqui a busca de receita passa pela guarda com o jogador do clique.
 */
@Mixin(value = CraftingTermSlot.class, remap = false)
public abstract class CraftingTermSlotMixin implements MarcadorProtecao {
    @Inject(method = "doClick(Lappeng/helpers/InventoryAction;Lnet/minecraft/world/entity/player/Player;)V", at = @At("HEAD"), remap = false)
    private void smpprofissoes$cliqueEntrar(InventoryAction acao, Player p, CallbackInfo ci) { Contexto.entrar(p); }

    @Inject(method = "doClick(Lappeng/helpers/InventoryAction;Lnet/minecraft/world/entity/player/Player;)V", at = @At("RETURN"), remap = false)
    private void smpprofissoes$cliqueSair(InventoryAction acao, Player p, CallbackInfo ci) { Contexto.sair(); }

    @Inject(method = "onTake(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"), remap = false)
    private void smpprofissoes$pegarEntrar(Player p, ItemStack s, CallbackInfo ci) { Contexto.entrar(p); }

    @Inject(method = "onTake(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V", at = @At("RETURN"), remap = false)
    private void smpprofissoes$pegarSair(Player p, ItemStack s, CallbackInfo ci) { Contexto.sair(); }
}
