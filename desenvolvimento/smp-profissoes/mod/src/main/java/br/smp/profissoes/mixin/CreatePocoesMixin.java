package br.smp.profissoes.mixin;

import br.smp.profissoes.MarcadorProtecao;
import com.simibubi.create.content.fluids.potion.PotionMixingRecipes;
import com.simibubi.create.content.kinetics.mixer.MixingRecipe;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Poções são do Naturalista (suporte de poções exclusivo dele). O Create gera sozinho receitas de Mixer a partir das
 * receitas do suporte de poções; aqui elas deixam de existir, senão qualquer Engenheiro faria poção em série.
 */
@Mixin(value = PotionMixingRecipes.class, remap = false)
public abstract class CreatePocoesMixin implements MarcadorProtecao {

    @Inject(method = "createRecipes(Lnet/minecraft/world/level/Level;)Ljava/util/List;", at = @At("HEAD"), cancellable = true, remap = false)
    private static void smpprofissoes$semPocoes(Level level, CallbackInfoReturnable<List<RecipeHolder<MixingRecipe>>> cir) {
        cir.setReturnValue(List.of());
    }

    @Inject(method = "sortRecipesByItem(Lnet/minecraft/world/level/Level;)Ljava/util/Map;", at = @At("HEAD"), cancellable = true, remap = false)
    private static void smpprofissoes$semPocoesPorItem(Level level, CallbackInfoReturnable<Map<Item, List<MixingRecipe>>> cir) {
        cir.setReturnValue(Map.of());
    }
}
