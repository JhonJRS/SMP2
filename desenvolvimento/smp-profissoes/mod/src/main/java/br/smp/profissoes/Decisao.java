package br.smp.profissoes;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Regra de decisão pura (sem Minecraft), para poder ser testada isoladamente.
 * Uma etapa é devida quando o jogador tem a profissão-base E (escolheu aquele mod como inicial OU pertence a
 * pelo menos uma colônia que concluiu todas as pesquisas e tem todos os edifícios no nível exigido).
 */
public final class Decisao {
    private Decisao() {}

    /** Estado de uma colônia, já extraído da MineColonies. */
    public record Colonia(String nome, Set<String> pesquisasConcluidas, Map<String, Integer> nivelMaximoPorEdificio) {}

    public record Resultado(List<Regras.Etapa> conceder, List<Regras.Etapa> remover) {
        public boolean vazio() {
            return conceder.isEmpty() && remover.isEmpty();
        }
    }

    public static boolean cumpre(Colonia c, Regras.Etapa e) {
        for (String r : e.pesquisas) {
            if (!c.pesquisasConcluidas().contains(r)) return false;
        }
        for (Map.Entry<String, Integer> b : e.edificios.entrySet()) {
            if (c.nivelMaximoPorEdificio().getOrDefault(b.getKey(), 0) < b.getValue()) return false;
        }
        return true;
    }

    public static Resultado calcular(List<String> licencasAtuais, List<Colonia> colonias, Regras regras) {
        Set<String> atuais = licencasAtuais.stream().map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        List<Regras.Etapa> conceder = new ArrayList<>();
        List<Regras.Etapa> remover = new ArrayList<>();
        for (Regras.Etapa e : regras.etapas) {
            boolean inicial = e.inicio && atuais.contains(Regras.PREFIXO_INICIO + e.licenca);
            boolean devida = atuais.contains(e.profissao) && (inicial || colonias.stream().anyMatch(c -> cumpre(c, e)));
            boolean tem = atuais.contains(e.licenca);
            if (devida && !tem) conceder.add(e);
            else if (!devida && tem) remover.add(e);
        }
        return new Resultado(conceder, remover);
    }
}
