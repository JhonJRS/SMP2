package br.smp.profissoes;

import com.minecolonies.api.colony.buildings.IBuilding;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.rp.rpessentials.profession.LicenseManager;

/**
 * Nível da colônia controlado pela administração: subir a Prefeitura (Town Hall) para o nível N exige que quem pede
 * tenha a licença "colonia_N" (ou maior), dada por um ADM com /smpprofissoes colonia &lt;jogador&gt; &lt;N&gt;.
 * Os outros edifícios não passam do nível da Prefeitura (regra da MineColonies), então isso limita a colônia inteira.
 * OP não precisa de licença. Nível 1 (construir a Prefeitura) é livre.
 */
public final class Colonia {
    private Colonia() {}

    public static final String PREFEITURA = "minecolonies:townhall";
    public static final String PREFIXO = "colonia_";
    private static final Pattern NIVEL = Pattern.compile("colonia_(\\d+)");

    /** Maior nível de colônia liberado para o jogador pelas licenças (1 se não tiver nenhuma). */
    public static int nivelLiberado(Player p) {
        int max = 1;
        List<String> lic = LicenseManager.getLicenses(p.getUUID());
        for (String l : lic) {
            Matcher m = NIVEL.matcher(l.toLowerCase());
            if (m.matches()) max = Math.max(max, Integer.parseInt(m.group(1)));
        }
        return max;
    }

    /** /smpprofissoes colonia &lt;jogador&gt; &lt;nível&gt;: troca as licenças colonia_* pela do nível (1 = nenhuma). */
    public static void definirNivel(Player p, int nivel) {
        for (String l : List.copyOf(LicenseManager.getLicenses(p.getUUID()))) {
            if (NIVEL.matcher(l.toLowerCase()).matches()) LicenseManager.removeLicense(p.getUUID(), l);
        }
        if (nivel > 1) LicenseManager.addLicense(p.getUUID(), PREFIXO + nivel);
        SmpProfissoes.LOG.info("Nível de colônia de {} definido para {}", p.getGameProfile().getName(), nivel);
    }

    public static boolean podeSubir(Player p, IBuilding b) {
        try {
            // Rodada 11: vale para TODO edifício (antes só a Prefeitura) — nenhum prédio passa do nível liberado ao jogador
            if (p == null || b == null || b.getBuildingType() == null) return true;
            int alvo = b.getBuildingLevel() + 1;
            if (alvo <= 1 || (p instanceof ServerPlayer sp && sp.hasPermissions(2))) return true;
            if (alvo <= nivelLiberado(p)) return true;
            String nome = b.getBuildingType().getRegistryName().getPath().replace('_', ' ');
            p.sendSystemMessage(Component.literal("✘ You cannot build or upgrade buildings to level " + alvo + " yet (" + nome
                    + "): an admin must allow it (/smpprofissoes colonia <player> " + alvo + ").").withStyle(ChatFormatting.RED));
            SmpProfissoes.LOG.info("{} tentou levar {} ao nível {} sem a licença {}{}", p.getGameProfile().getName(),
                    b.getBuildingType().getRegistryName(), alvo, PREFIXO, alvo);
            return false;
        } catch (Throwable t) {
            SmpProfissoes.LOG.error("Falha ao conferir licença de nível da colônia", t);
            return true;
        }
    }
}
