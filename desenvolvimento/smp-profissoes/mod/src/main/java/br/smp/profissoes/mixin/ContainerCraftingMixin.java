package br.smp.profissoes.mixin;

import br.smp.profissoes.Contexto;
import br.smp.profissoes.MarcadorProtecao;
import com.minecolonies.api.inventory.container.ContainerCrafting;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** MineColonies (tela de ensinar receita de bancada): o jogador que ensina é o contexto. */
@Mixin(value = ContainerCrafting.class, remap = false)
public abstract class ContainerCraftingMixin implements MarcadorProtecao {
    @Inject(method = "slotsChanged(Lnet/minecraft/world/Container;)V", at = @At("HEAD"), remap = false)
    private void smpprofissoes$entrar(Container c, CallbackInfo ci) { Contexto.entrar(((ContainerCrafting) (Object) this).getPlayer()); }

    @Inject(method = "slotsChanged(Lnet/minecraft/world/Container;)V", at = @At("RETURN"), remap = false)
    private void smpprofissoes$sair(Container c, CallbackInfo ci) { Contexto.sair(); }

    @Inject(method = "getRemainingItems()Ljava/util/List;", at = @At("HEAD"), remap = false)
    private void smpprofissoes$restoEntrar(CallbackInfoReturnable<List<ItemStack>> cir) { Contexto.entrar(((ContainerCrafting) (Object) this).getPlayer()); }

    @Inject(method = "getRemainingItems()Ljava/util/List;", at = @At("RETURN"), remap = false)
    private void smpprofissoes$restoSair(CallbackInfoReturnable<List<ItemStack>> cir) { Contexto.sair(); }
}
