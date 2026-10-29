package br.smp.profissoes.mixin;

import br.smp.profissoes.Contexto;
import br.smp.profissoes.MarcadorProtecao;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Bancada e grade 2x2 do inventário (e menus que reutilizam este método): o jogador é o contexto. */
@Mixin(value = CraftingMenu.class, remap = false)
public abstract class CraftingMenuMixin implements MarcadorProtecao {
    private static final String ALVO = "slotChangedCraftingGrid(Lnet/minecraft/world/inventory/AbstractContainerMenu;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/inventory/CraftingContainer;Lnet/minecraft/world/inventory/ResultContainer;Lnet/minecraft/world/item/crafting/RecipeHolder;)V";

    @Inject(method = ALVO, at = @At("HEAD"), remap = false)
    private static void smpprofissoes$entrar(AbstractContainerMenu menu, Level level, Player p, CraftingContainer grade, ResultContainer res, RecipeHolder<CraftingRecipe> dica, CallbackInfo ci) {
        Contexto.entrar(p);
    }

    @Inject(method = ALVO, at = @At("RETURN"), remap = false)
    private static void smpprofissoes$sair(AbstractContainerMenu menu, Level level, Player p, CraftingContainer grade, ResultContainer res, RecipeHolder<CraftingRecipe> dica, CallbackInfo ci) {
        Contexto.sair();
    }
}
