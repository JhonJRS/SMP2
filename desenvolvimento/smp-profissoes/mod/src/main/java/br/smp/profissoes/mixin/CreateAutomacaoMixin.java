package br.smp.profissoes.mixin;

import br.smp.profissoes.Guarda;
import br.smp.profissoes.MarcadorProtecao;
import com.simibubi.create.AllRecipeTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Create: o misturador (receitas sem forma), a prensa (receitas quadradas), o Mechanical Crafter e a serra
 * (cortador) reaproveitam receitas de bancada/cortador sem jogador. Todos consultam este filtro; itens das
 * profissões passam a ser ignorados pela automação do Create.
 */
@Mixin(value = AllRecipeTypes.class, remap = false)
public abstract class CreateAutomacaoMixin implements MarcadorProtecao {

    @Inject(method = "shouldIgnoreInAutomation(Lnet/minecraft/world/item/crafting/RecipeHolder;)Z", at = @At("RETURN"), cancellable = true, remap = false)
    private static void smpprofissoes$ignorar(RecipeHolder<?> receita, CallbackInfoReturnable<Boolean> cir) {
        try {
            if (cir.getReturnValueZ() || receita == null || !Guarda.tipoVigiado(receita.value().getType())) return;
            if (Guarda.licencasDaReceita(receita.id()) != null) { // selos nunca são automatizados
                cir.setReturnValue(true);
                return;
            }
            MinecraftServer s = ServerLifecycleHooks.getCurrentServer();
            if (s == null) return;
            if (Guarda.itemProtegido(Guarda.resultado(receita, null, s.registryAccess()))) cir.setReturnValue(true);
        } catch (Throwable ignorado) {
            // nunca derruba a automação do Create
        }
    }
}
