package br.smp.profissoes;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.research.ILocalResearchTree;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.rp.rpessentials.profession.LicenseManager;
import net.rp.rpessentials.profession.ProfessionRestrictionManager;
import net.rp.rpessentials.profession.ProfessionSyncHelper;

/** Lê o estado real das colônias (servidor) e sincroniza as licenças de etapa no RPEssentials. */
public final class Progressao {
    private Progressao() {}

    /** Colônias em que o jogador é membro, com pesquisas concluídas e nível máximo de cada edifício. */
    public static List<Decisao.Colonia> coloniasDo(ServerPlayer p) {
        Regras r = Regras.get();
        Set<String> pesquisasRelevantes = new HashSet<>();
        Set<String> edificiosRelevantes = new HashSet<>();
        for (Regras.Etapa e : r.etapas) {
            pesquisasRelevantes.addAll(e.pesquisas);
            edificiosRelevantes.addAll(e.edificios.keySet());
        }
        List<Decisao.Colonia> out = new ArrayList<>();
        for (IColony c : IColonyManager.getInstance().getAllColonies()) {
            if (c == null || !c.getPermissions().isColonyMember(p)) continue;
            Set<String> feitas = new HashSet<>();
            ILocalResearchTree arvore = c.getResearchManager().getResearchTree();
            for (String id : pesquisasRelevantes) {
                ResourceLocation rl = ResourceLocation.tryParse(id);
                if (rl != null && arvore.hasCompletedResearch(rl)) feitas.add(id);
            }
            Map<String, Integer> niveis = new HashMap<>();
            for (IBuilding b : c.getServerBuildingManager().getBuildings().values()) {
                if (b == null || b.getBuildingType() == null) continue;
                String tipo = b.getBuildingType().getRegistryName().toString();
                if (edificiosRelevantes.contains(tipo)) niveis.merge(tipo, b.getBuildingLevel(), Math::max);
            }
            out.add(new Decisao.Colonia(c.getName(), feitas, niveis));
        }
        return out;
    }

    /** Concede/remove licenças de etapa. Devolve true se algo mudou. */
    public static boolean sincronizar(ServerPlayer p, boolean avisar) {
        Regras regras = Regras.get();
        if (regras.etapas.isEmpty()) return false;
        UUID id = p.getUUID();
        Decisao.Resultado res = Decisao.calcular(new ArrayList<>(LicenseManager.getLicenses(id)), coloniasDo(p), regras);
        if (res.vazio()) return false;
        for (Regras.Etapa e : res.conceder()) {
            LicenseManager.addLicense(id, e.licenca);
            SmpProfissoes.LOG.info("{} recebeu a etapa {} ({})", p.getGameProfile().getName(), e.licenca, e.nome);
            if (avisar) {
                p.sendSystemMessage(Component.literal("✦ New stage unlocked: ").withStyle(ChatFormatting.GOLD)
                        .append(Component.literal(e.nome).withStyle(ChatFormatting.YELLOW)));
            }
        }
        for (Regras.Etapa e : res.remover()) {
            LicenseManager.removeLicense(id, e.licenca);
            SmpProfissoes.LOG.info("{} perdeu a etapa {} ({})", p.getGameProfile().getName(), e.licenca, e.nome);
            if (avisar) {
                p.sendSystemMessage(Component.literal("✘ Stage suspended (colony requirements no longer met): ").withStyle(ChatFormatting.RED)
                        .append(Component.literal(e.nome).withStyle(ChatFormatting.GRAY)));
            }
        }
        ProfessionRestrictionManager.invalidatePlayerCache(id);
        ProfessionSyncHelper.syncToPlayer(p);
        return true;
    }

    /** Status text for /progressao (/progression) e /smpprofissoes status. */
    public static List<Component> status(ServerPlayer p) {
        Regras r = Regras.get();
        List<Component> linhas = new ArrayList<>();
        List<String> lic = LicenseManager.getLicenses(p.getUUID());
        List<String> profs = r.profissoes.stream().filter(x -> lic.stream().anyMatch(l -> l.equalsIgnoreCase(x))).toList();
        linhas.add(Component.literal("── Progression of " + p.getGameProfile().getName() + " ──").withStyle(ChatFormatting.GOLD));
        if (profs.isEmpty()) {
            linhas.add(Component.literal("No profession chosen yet.").withStyle(ChatFormatting.GRAY));
            return linhas;
        }
        List<Decisao.Colonia> cols = coloniasDo(p);
        linhas.add(Component.literal("Profession: " + String.join(" + ", profs) + " · colonies: " + (cols.isEmpty() ? "none" : String.join(", ", cols.stream().map(Decisao.Colonia::nome).toList())))
                .withStyle(ChatFormatting.GRAY));
        for (Regras.Etapa e : r.etapas) {
            if (!profs.contains(e.profissao)) continue;
            boolean tem = lic.stream().anyMatch(l -> l.equalsIgnoreCase(e.licenca));
            StringBuilder falta = new StringBuilder();
            if (!tem) {
                Decisao.Colonia melhor = cols.isEmpty() ? null : cols.get(0);
                for (String pq : e.pesquisas) {
                    if (melhor == null || !melhor.pesquisasConcluidas().contains(pq)) falta.append(" research ").append(r.nomesPesquisas.getOrDefault(pq, pq)).append(';');
                }
                for (Map.Entry<String, Integer> b : e.edificios.entrySet()) {
                    int atual = melhor == null ? 0 : melhor.nivelMaximoPorEdificio().getOrDefault(b.getKey(), 0);
                    if (atual < b.getValue()) falta.append(' ').append(r.nomesEdificios.getOrDefault(b.getKey(), b.getKey())).append(' ').append(atual).append('/').append(b.getValue()).append(';');
                }
            }
            linhas.add(Component.literal((tem ? " ✔ " : " ✘ ") + e.nome).withStyle(tem ? ChatFormatting.GREEN : ChatFormatting.RED)
                    .append(Component.literal(tem ? "" : " — missing:" + falta).withStyle(ChatFormatting.GRAY)));
        }
        return linhas;
    }
}
