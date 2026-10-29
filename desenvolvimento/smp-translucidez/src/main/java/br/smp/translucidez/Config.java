package br.smp.translucidez;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.neoforged.fml.loading.FMLPaths;

/** config/smptranslucidez.properties (criado na primeira vez). */
public final class Config {
    /** Opacidade do jogador invisível, em %. Vanilla usa ~15% (só para espectadores/time); mínimo aceito é 15. */
    public static int opacidade = 45;
    /** Mostrar o nome sobre a cabeça de jogador invisível (vanilla esconde). */
    public static boolean mostrarNome = false;
    /** Opacidade, em %, para quem usa o Ring of Invisibility do Iron's (que no mod revela criaturas invisíveis). */
    public static int opacidadeAnel = 80;
    /** Cores ARGB passadas ao modelo (branco com as opacidades acima). */
    public static int cor = argb(opacidade);
    public static int corAnel = argb(opacidadeAnel);

    private Config() {}

    static int argb(int pct) {
        int a = Math.round(Math.max(15, Math.min(100, pct)) * 255 / 100f);
        return (a << 24) | 0xFFFFFF;
    }

    static void carregar() {
        Path arq = FMLPaths.CONFIGDIR.get().resolve("smptranslucidez.properties");
        Properties p = new Properties();
        try {
            if (Files.exists(arq)) {
                try (Reader r = Files.newBufferedReader(arq, StandardCharsets.UTF_8)) { p.load(r); }
            }
            opacidade = Math.max(15, Math.min(100, Integer.parseInt(p.getProperty("opacidade", "45").trim())));
            mostrarNome = Boolean.parseBoolean(p.getProperty("mostrar_nome", "false").trim());
            opacidadeAnel = Math.max(15, Math.min(100, Integer.parseInt(p.getProperty("opacidade_anel", "80").trim())));
            if (!Files.exists(arq) || !p.containsKey("opacidade_anel")) {
                p.setProperty("opacidade", String.valueOf(opacidade));
                p.setProperty("mostrar_nome", String.valueOf(mostrarNome));
                p.setProperty("opacidade_anel", String.valueOf(opacidadeAnel));
                try (Writer w = Files.newBufferedWriter(arq, StandardCharsets.UTF_8)) {
                    p.store(w, " SMP Translucidez: opacidade (15-100, em %) de jogadores invisiveis; mostrar_nome = nome sobre a cabeca;"
                            + " opacidade_anel = como quem usa o Ring of Invisibility do Iron's ve os invisiveis");
                }
            }
        } catch (Exception e) {
            SmpTranslucidez.LOG.warn("Config inválida ({}), usando padrão", e.toString());
        }
        cor = argb(opacidade);
        corAnel = argb(opacidadeAnel);
    }
}
