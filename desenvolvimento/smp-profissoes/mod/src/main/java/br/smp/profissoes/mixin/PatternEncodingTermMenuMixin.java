package br.smp.profissoes.mixin;

import appeng.menu.AEBaseMenu;
import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.menu.slot.PatternTermSlot;
import br.smp.profissoes.Contexto;
import br.smp.profissoes.Guarda;
import br.smp.profissoes.MarcadorProtecao;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * AE2 Pattern Encoding Terminal: o jogador do terminal é o contexto das buscas de receita (bancada, cortador,
 * ferraria) e não codifica padrão de bancada cujo resultado ele não pode fabricar. Sem isso o Molecular
 * Assembler fabricaria receitas protegidas sem contexto de jogador.
 */
@Mixin(value = PatternEncodingTermMenu.class, remap = false)
public abstract class PatternEncodingTermMenuMixin implements MarcadorProtecao {
    @Shadow(remap = false) private RecipeHolder<CraftingRecipe> currentRecipe;
    @Shadow(remap = false) @Final private PatternTermSlot craftOutputSlot;

    private Player smpprofissoes$jogador() {
        return ((AEBaseMenu) (Object) this).getPlayer();
    }

    @Inject(method = "getAndUpdateOutput()Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"), remap = false)
    private void smpprofissoes$saidaEntrar(CallbackInfoReturnable<ItemStack> cir) { Contexto.entrar(smpprofissoes$jogador()); }

    @Inject(method = "getAndUpdateOutput()Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"), remap = false)
    private void smpprofissoes$saidaSair(CallbackInfoReturnable<ItemStack> cir) { Contexto.sair(); }

    @Inject(method = "updateStonecuttingRecipes()V", at = @At("HEAD"), remap = false)
    private void smpprofissoes$cortadorEntrar(CallbackInfo ci) { Contexto.entrar(smpprofissoes$jogador()); }

    @Inject(method = "updateStonecuttingRecipes()V", at = @At("RETURN"), remap = false)
    private void smpprofissoes$cortadorSair(CallbackInfo ci) { Contexto.sair(); }

    @Inject(method = "encode()V", at = @At("HEAD"), remap = false)
    private void smpprofissoes$codificarEntrar(CallbackInfo ci) { Contexto.entrar(smpprofissoes$jogador()); }

    @Inject(method = "encode()V", at = @At("RETURN"), remap = false)
    private void smpprofissoes$codificarSair(CallbackInfo ci) { Contexto.sair(); }

    @Inject(method = "encodeCraftingPattern()Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"), cancellable = true, remap = false)
    private void smpprofissoes$verificarPadrao(CallbackInfoReturnable<ItemStack> cir) {
        if (cir.getReturnValue() == null) return;
        if (!(smpprofissoes$jogador() instanceof ServerPlayer sp)) return;
        ItemStack saida = craftOutputSlot.getItem();
        if (saida.isEmpty() && currentRecipe != null) saida = currentRecipe.value().getResultItem(sp.level().registryAccess());
        if (saida.isEmpty()) return;
        String recusa = Guarda.recusaParaItem(sp, saida);
        if (recusa != null) {
            sp.displayClientMessage(Component.literal(recusa), false);
            cir.setReturnValue(null);
        }
    }
}
