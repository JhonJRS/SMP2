package br.smp.profissoes.mixin;

import br.smp.profissoes.MarcadorProtecao;
import com.ldtteam.blockui.controls.Image;
import com.ldtteam.blockui.util.records.SizeI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Crash Quick Skin x MineColonies (26/10/2026): as telas da MineColonies (BlockUI) leem o tamanho da imagem abrindo o
 * arquivo, mas as skins do Quick Skin são texturas dinâmicas sem arquivo ("quickskin:local/sha256-..._full") e o BlockUI
 * lançava "No extension for file". Para textura sem arquivo, o tamanho vem da própria textura (ou 64x64, o de uma skin).
 */
@Mixin(value = Image.class, remap = false)
public abstract class ImagemBlockUIMixin implements MarcadorProtecao {

    @Inject(method = "getImageDimensions(Lnet/minecraft/resources/ResourceLocation;)Lcom/ldtteam/blockui/util/records/SizeI;",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void smpprofissoes$texturaSemArquivo(ResourceLocation loc, CallbackInfoReturnable<SizeI> cir) {
        if (loc == null || loc.getPath().lastIndexOf('.') != -1) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getResourceManager().getResource(loc).isPresent()) return;
        int w = 64, h = 64;
        AbstractTexture t = mc.getTextureManager().getTexture(loc, null);
        if (t instanceof DynamicTexture d && d.getPixels() != null) {
            w = d.getPixels().getWidth();
            h = d.getPixels().getHeight();
        }
        cir.setReturnValue(new SizeI(w, h));
    }
}
