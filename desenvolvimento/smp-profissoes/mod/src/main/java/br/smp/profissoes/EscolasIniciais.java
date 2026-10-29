package br.smp.profissoes;

import java.util.List;

/**
 * Escolas oferecidas na primeira escolha de escola de magia (as do Iron's + Symmetry, Spirit e Melodic dos addons; as
 * outras pelo Codex). Fora de EscolasMagia para a tela do cliente usar sem carregar classes do Iron's.
 */
public final class EscolasIniciais {
    private EscolasIniciais() {}

    public static final List<String> IDS = List.of("irons_spellbooks:fire", "irons_spellbooks:ice", "irons_spellbooks:lightning",
            "irons_spellbooks:holy", "irons_spellbooks:nature", "irons_spellbooks:ender", "irons_spellbooks:evocation",
            "irons_spellbooks:blood", "iss_magicfromtheeast:symmetry", "iss_magicfromtheeast:spirit", "tunes_n_tomes:melody");
}
