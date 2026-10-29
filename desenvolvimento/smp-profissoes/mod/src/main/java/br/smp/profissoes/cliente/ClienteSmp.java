package br.smp.profissoes.cliente;

import br.smp.profissoes.SmpProfissoes;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Só no cliente: tecla do painel de ADM (F7 por padrão; o servidor continua exigindo OP em cada comando). */
@Mod(value = SmpProfissoes.MODID, dist = Dist.CLIENT)
public final class ClienteSmp {
    static final KeyMapping PAINEL = new KeyMapping("key.smpprofissoes.painel_adm", InputConstants.Type.KEYSYM, 296 /* GLFW F7 */,
            "key.categories.smpprofissoes");

    public ClienteSmp(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(PAINEL));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            Minecraft mc = Minecraft.getInstance();
            while (PAINEL.consumeClick()) {
                if (mc.player == null || mc.screen != null) continue;
                if (mc.player.hasPermissions(2)) mc.setScreen(new PainelAdm());
                else mc.player.displayClientMessage(Component.literal("Only operators can open the admin panel."), true);
            }
        });
    }
}
