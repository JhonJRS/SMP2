package br.smp.translucidez;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.common.util.TriState;

/** Roda por último no evento de nome: desfaz o "pode mostrar" que outros mods (RPEssentials) põem em jogador invisível. */
public final class NomeInvisivel {
    private NomeInvisivel() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void aoNome(RenderNameTagEvent e) {
        if (!Config.mostrarNome && e.getEntity() instanceof Player p && p.isInvisible()) e.setCanRender(TriState.FALSE);
    }
}
