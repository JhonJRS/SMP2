package br.smp.profissoes;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.neoforged.fml.loading.FMLPaths;

/** Regras geradas por smp-profissoes/tools/gerar.py em config/smpprofissoes/progressao.json. */
public final class Regras {
    public int intervaloTicks = 100;
    public List<String> profissoes = new ArrayList<>();
    public List<String> namespacesProtegidos = new ArrayList<>();
    /** id da receita (selos) → licenças que podem fabricá-la. */
    public Map<String, List<String>> receitasComLicenca = new LinkedHashMap<>();
    private transient Set<String> nsCache;

    /** Namespaces dos mods das profissões (itens que só saem com jogador autorizado). */
    public Set<String> namespaces() {
        Set<String> c = nsCache;
        if (c == null) nsCache = c = Set.copyOf(namespacesProtegidos == null ? List.of() : namespacesProtegidos);
        return c;
    }
    public Map<String, String> nomesPesquisas = new LinkedHashMap<>();
    public Map<String, String> nomesEdificios = new LinkedHashMap<>();
    public List<Etapa> etapas = new ArrayList<>();

    public static final class Etapa {
        public String licenca;
        public String nome;
        public String profissao;
        public List<String> pesquisas = new ArrayList<>();
        public Map<String, Integer> edificios = new LinkedHashMap<>();
        /** Pode ser o mod inicial escolhido no menu (marcador de licença "inicio_" + licenca). */
        public boolean inicio;
    }

    public static final String PREFIXO_INICIO = "inicio_";

    private static Regras atual = new Regras();

    public static Regras get() {
        return atual;
    }

    public static Path arquivo() {
        return FMLPaths.CONFIGDIR.get().resolve(SmpProfissoes.MODID).resolve("progressao.json");
    }

    /** Recarrega do disco. Em caso de erro mantém as regras anteriores e devolve a mensagem. */
    public static String recarregar() {
        Path p = arquivo();
        if (!Files.exists(p)) {
            atual = new Regras();
            String msg = "arquivo de regras não encontrado: " + p + " (nenhuma etapa será concedida)";
            SmpProfissoes.LOG.warn(msg);
            return msg;
        }
        try {
            Regras r = new Gson().fromJson(Files.readString(p, StandardCharsets.UTF_8), Regras.class);
            if (r == null || r.etapas == null) throw new IOException("JSON vazio");
            if (r.intervaloTicks < 20) r.intervaloTicks = 20;
            for (Etapa e : r.etapas) {
                if (e.licenca == null || e.profissao == null) throw new IOException("etapa sem licenca/profissao");
                e.licenca = e.licenca.toLowerCase();
                e.profissao = e.profissao.toLowerCase();
            }
            atual = r;
            SmpProfissoes.LOG.info("Regras carregadas: {} profissões, {} etapas", r.profissoes.size(), r.etapas.size());
            return null;
        } catch (Exception ex) {
            SmpProfissoes.LOG.error("Falha ao ler {}: {}", p, ex.toString());
            return "falha ao ler regras: " + ex.getMessage();
        }
    }
}
