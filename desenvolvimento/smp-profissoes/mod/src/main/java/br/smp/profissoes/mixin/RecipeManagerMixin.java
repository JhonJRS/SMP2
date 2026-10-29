package br.smp.profissoes.mixin;

import br.smp.profissoes.Contexto;
import br.smp.profissoes.Guarda;
import br.smp.profissoes.MarcadorProtecao;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ponto único por onde passam as buscas de receita. Bancada/cortador/ferraria: itens das profissões só com jogador
 * autorizado no contexto. Máquinas (qualquer outro tipo): com operador no contexto, só o que ele pode fabricar.
 */
@Mixin(value = RecipeManager.class, remap = false)
public abstract class RecipeManagerMixin implements MarcadorProtecao {

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Inject(method = "getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/crafting/RecipeHolder;)Ljava/util/Optional;",
            at = @At("RETURN"), cancellable = true, remap = false)
    private void smpprofissoes$guardarUma(RecipeType tipo, RecipeInput entrada, Level level, RecipeHolder dica, CallbackInfoReturnable<Optional> cir) {
        if (!(level instanceof ServerLevel)) return;
        Optional r = cir.getReturnValue();
        if (r == null || r.isEmpty()) return;
        RecipeHolder<?> h = (RecipeHolder<?>) r.get();
        boolean bloquear = Guarda.tipoVigiado(tipo) ? Guarda.bloquear(h, entrada, level.registryAccess())
                : Guarda.bloquearMaquina(h, entrada, level.registryAccess());
        if (bloquear) cir.setReturnValue(Optional.empty());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Inject(method = "getRecipesFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)Ljava/util/List;",
            at = @At("RETURN"), cancellable = true, remap = false)
    private void smpprofissoes$guardarVarias(RecipeType tipo, RecipeInput entrada, Level level, CallbackInfoReturnable<List> cir) {
        if (!(level instanceof ServerLevel)) return;
        boolean vigiado = Guarda.tipoVigiado(tipo);
        if (!vigiado && Contexto.operador() == null) return;
        List<?> lista = cir.getReturnValue();
        if (lista == null || lista.isEmpty()) return;
        List<Object> filtrada = new ArrayList<>(lista.size());
        for (Object o : lista) {
            RecipeHolder<?> h = (RecipeHolder<?>) o;
            boolean bloquear = vigiado ? Guarda.bloquear(h, entrada, level.registryAccess()) : Guarda.bloquearMaquina(h, entrada, level.registryAccess());
            if (!bloquear) filtrada.add(o);
        }
        if (filtrada.size() != lista.size()) cir.setReturnValue(filtrada);
    }

    /** Máquinas que varrem todas as receitas do tipo (ex.: Altar Espiritual do Malum, via Lodestone). Só com operador. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Inject(method = "getAllRecipesFor(Lnet/minecraft/world/item/crafting/RecipeType;)Ljava/util/List;",
            at = @At("RETURN"), cancellable = true, remap = false)
    private void smpprofissoes$guardarTodas(RecipeType tipo, CallbackInfoReturnable<List> cir) {
        if (Contexto.operador() == null || Guarda.tipoVigiado(tipo)) return;
        var server = ServerLifecycleHooks.getCurrentServer();
        List<?> lista = cir.getReturnValue();
        if (server == null || lista == null || lista.isEmpty()) return;
        List<Object> filtrada = new ArrayList<>(lista.size());
        for (Object o : lista) {
            if (!Guarda.bloquearMaquina((RecipeHolder<?>) o, null, server.registryAccess())) filtrada.add(o);
        }
        if (filtrada.size() != lista.size()) cir.setReturnValue(filtrada);
    }
}
