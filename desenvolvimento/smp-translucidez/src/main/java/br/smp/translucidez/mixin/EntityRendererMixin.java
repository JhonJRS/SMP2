package br.smp.translucidez.mixin;

import br.smp.translucidez.Config;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Nome sobre jogador invisível: todo nome passa por EntityRenderer.renderNameTag, inclusive o do RPEssentials
 * (que força o nome a aparecer) e o do Figura. Cancela ali, salvo mostrar_nome=true.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    @Inject(method = "renderNameTag(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IF)V",
            at = @At("HEAD"), cancellable = true)
    private void smptranslucidez$semNome(Entity e, Component nome, PoseStack pose, MultiBufferSource buf, int luz, float pt, CallbackInfo ci) {
        if (!Config.mostrarNome && e instanceof Player && e.isInvisible()) ci.cancel();
    }
}
