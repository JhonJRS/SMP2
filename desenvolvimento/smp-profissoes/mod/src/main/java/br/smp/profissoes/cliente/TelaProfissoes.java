package br.smp.profissoes.cliente;

import br.smp.profissoes.EscolasIniciais;
import br.smp.profissoes.SmpProfissoes;
import br.smp.profissoes.cliente.Fundo.Tema;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/**
 * Tela de escolha de profissão (Carta de Apresentação) e da segunda profissão (Guild Charter).
 * Etapas: profissões → especializações (mod inicial) → confirmação → selo. Fundos animados por profissão e por
 * especialização (Fundo). A tela não decide nada: ao confirmar manda o mesmo comando dos botões do chat
 * (/trigger prof.escolha set 1NN ou /guildcharter &lt;licença&gt;) e o servidor confere tudo.
 * Os dados vêm de assets/smpprofissoes/tela_profissoes.json (gerado pelo gerar.py junto com o menu do chat).
 */
public final class TelaProfissoes extends Screen {
    record Inicio(String licenca, String rotulo, String descricao, int codigo, List<ItemStack> kit) {}

    record Prof(String id, String nome, String icone, List<Inicio> inicios) {}

    /** Escola de magia do Iron's oferecida logo depois de escolher o Iron's Spells (runa da escola como ícone). */
    record Escola(String id, Component nome, ItemStack runa) {}

    /** Especialização que leva à escolha da escola de magia (EscolasMagia, comando /escola). */
    private static final String IRONS = "arc_irons";

    private record Alvo(int x, int y, int w, int h, Runnable acao) {
        boolean dentro(double mx, double my) {
            return mx >= x && my >= y && mx < x + w && my < y + h;
        }
    }

    private static final Map<String, String> LEMAS = Map.of(
            "engenheiro", "Máquinas, engrenagens, vapor e eletricidade",
            "arcanista", "Magia, espíritos, mana e sangue",
            "ferreiro", "Forja, metais e armas lendárias",
            "naturalista", "Cultivo, culinária, ervas e poções");

    private static final int ESC = 256;
    private static final float DURACAO_FIM = 2.2f;

    private final boolean guildCharter;
    private final List<String> jaTem;
    private final List<Prof> profs = carregar();
    private final List<Alvo> alvos = new ArrayList<>();
    private final Map<String, Float> realces = new HashMap<>();
    private final long abertura = Util.getMillis();

    private int etapa;
    private long desdeEtapa = Util.getMillis();
    private Prof prof;
    private Inicio escolhido;
    private final List<Escola> escolas = carregarEscolas();
    private Escola escola;
    private boolean escolaEnviada;
    private Tema temaAtual = Fundo.NEUTRO, temaAnterior = Fundo.NEUTRO;
    private long desdeTema;
    private ItemStack dica = ItemStack.EMPTY;

    private TelaProfissoes(boolean guildCharter, List<String> jaTem) {
        super(Component.literal(guildCharter ? "Guild Charter" : "Escolha sua Profissão"));
        this.guildCharter = guildCharter;
        this.jaTem = jaTem;
    }

    public static void abrir(boolean guildCharter, List<String> jaTem) {
        Minecraft.getInstance().setScreen(new TelaProfissoes(guildCharter, jaTem));
    }

    static List<Prof> carregar() {
        List<Prof> out = new ArrayList<>();
        try (InputStream in = TelaProfissoes.class.getResourceAsStream("/assets/" + SmpProfissoes.MODID + "/tela_profissoes.json")) {
            if (in == null) return out;
            for (JsonElement e : JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonArray()) {
                JsonObject o = e.getAsJsonObject();
                List<Inicio> inicios = new ArrayList<>();
                for (JsonElement ie : o.getAsJsonArray("inicios")) {
                    JsonObject i = ie.getAsJsonObject();
                    List<ItemStack> kit = new ArrayList<>();
                    for (JsonElement k : i.getAsJsonArray("kit")) {
                        ResourceLocation id = ResourceLocation.tryParse(k.getAsString());
                        if (id != null) BuiltInRegistries.ITEM.getOptional(id).ifPresent(it -> kit.add(new ItemStack(it)));
                    }
                    inicios.add(new Inicio(i.get("licenca").getAsString(), i.get("rotulo").getAsString(),
                            i.get("descricao").getAsString(), i.get("codigo").getAsInt(), kit));
                }
                out.add(new Prof(o.get("id").getAsString(), o.get("nome").getAsString(), o.get("icone").getAsString(), inicios));
            }
        } catch (Exception ex) {
            SmpProfissoes.LOG.error("Não consegui ler tela_profissoes.json", ex);
        }
        return out;
    }

    /** Só com Iron's Restrictions (é ele que trava as escolas); addon ausente = escola fora da lista. */
    static List<Escola> carregarEscolas() {
        List<Escola> out = new ArrayList<>();
        if (!ModList.get().isLoaded("irons_restrictions")) return out;
        for (String s : EscolasIniciais.IDS) {
            ResourceLocation id = ResourceLocation.tryParse(s);
            if (id == null || !ModList.get().isLoaded(id.getNamespace())) continue;
            ItemStack runa = BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), id.getPath() + "_rune"))
                    .map(ItemStack::new).orElse(ItemStack.EMPTY);
            out.add(new Escola(s, Component.translatable("school." + id.getNamespace() + "." + id.getPath()), runa));
        }
        return out;
    }

    private boolean pedeEscola() {
        return escolhido != null && IRONS.equals(escolhido.licenca) && !escolas.isEmpty();
    }

    private Tema temaEscola(Escola e) {
        return Fundo.ESCOLAS.getOrDefault(e.id, Fundo.daEspecializacao(IRONS, "arcanista"));
    }

    // ------------------------------------------------------------------ utilidades
    private static float ease(float f) {
        f = Math.max(0, Math.min(1, f));
        return 1 - (1 - f) * (1 - f) * (1 - f);
    }

    private float seg(long desde) {
        return (Util.getMillis() - desde) / 1000f;
    }

    private float realce(String chave, boolean alvo) {
        float r = realces.getOrDefault(chave, 0f);
        r += ((alvo ? 1 : 0) - r) * 0.22f;
        realces.put(chave, r);
        return r;
    }

    private void mudarEtapa(int nova) {
        etapa = nova;
        desdeEtapa = Util.getMillis();
        realces.clear();
    }

    private void tema(Tema novo) {
        if (novo == temaAtual) return;
        float f = Math.min(1, seg(desdeTema) / 0.4f);
        temaAnterior = f < 0.5f ? temaAnterior : temaAtual;
        temaAtual = novo;
        desdeTema = Util.getMillis();
    }

    private void som(net.minecraft.sounds.SoundEvent s, float tom) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(s, tom));
    }

    private void clique() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
    }

    private void textoEscala(GuiGraphics g, String s, float x, float y, float esc, int cor, float alfa, boolean centro) {
        if (alfa < 0.03f) return;
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(x, y, 0);
        ps.scale(esc, esc, 1);
        g.drawString(font, s, centro ? -font.width(s) / 2 : 0, 0, Fundo.a(cor, alfa), true);
        ps.popPose();
    }

    private int paragrafo(GuiGraphics g, String s, int cx, int y, int largura, int cor, float alfa, boolean centro, int maxLinhas) {
        List<FormattedCharSequence> ls = font.split(Component.literal(s), largura);
        int n = 0;
        for (FormattedCharSequence l : ls) {
            if (n >= maxLinhas) break;
            if (alfa >= 0.03f) g.drawString(font, l, centro ? cx - font.width(l) / 2 : cx, y + n * (font.lineHeight + 1), Fundo.a(cor, alfa), false);
            n++;
        }
        return n * (font.lineHeight + 1);
    }

    private void botao(GuiGraphics g, int x, int y, int w, int h, String rotulo, int cor, int mx, int my, float t, float alfa, Runnable acao) {
        boolean sobre = mx >= x && my >= y && mx < x + w && my < y + h;
        float r = realce("botao:" + rotulo, sobre);
        g.fillGradient(x, y, x + w, y + h, Fundo.a(Fundo.mistura(cor, 0xFF000000, 0.35f - 0.25f * r), alfa),
                Fundo.a(Fundo.mistura(cor, 0xFF000000, 0.6f - 0.25f * r), alfa));
        Fundo.moldura(g, x, y, w, h, cor, t, alfa, r);
        textoEscala(g, rotulo, x + w / 2f, y + (h - 8) / 2f, 1, Fundo.claro(cor, 0.6f + 0.4f * r), alfa, true);
        alvos.add(new Alvo(x, y, w, h, acao));
    }

    // ------------------------------------------------------------------ desenho
    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        alvos.clear();
        dica = ItemStack.EMPTY;
        float t = seg(abertura);
        float e = ease(seg(desdeEtapa) / 0.45f);

        // fundo de tela cheia, com cruzamento suave entre temas
        float ft = ease(seg(desdeTema) / 0.4f);
        if (ft < 1) Fundo.desenhar(g, font, temaAnterior, 0, 0, width, height, t, 1);
        Fundo.desenhar(g, font, temaAtual, 0, 0, width, height, t, ft < 1 ? ft : 1);
        g.fill(0, 0, width, height, 0x38000000);

        if (profs.isEmpty()) {
            textoEscala(g, "Dados da tela de profissões ausentes (rode o gerar.py).", width / 2f, height / 2f, 1, 0xFFFF8080, 1, true);
            return;
        }
        switch (etapa) {
            case 0 -> profissoes(g, mx, my, t, e);
            case 1 -> especializacoes(g, mx, my, t, e);
            case 2 -> confirmacao(g, mx, my, t, e);
            case 4 -> escolasMagia(g, mx, my, t, e);
            default -> fim(g, t);
        }
        if (!dica.isEmpty()) g.renderTooltip(font, dica, mx, my);
    }

    private void cabecalho(GuiGraphics g, String titulo, String sub, int cor, float t, float e) {
        float y = 10 - (1 - e) * 12;
        textoEscala(g, titulo, width / 2f, y, 2, cor, e, true);
        int tw = font.width(titulo) + 30, cx = width / 2;
        for (int k = 0; k < 2; k++) {
            int s = k == 0 ? -1 : 1;
            int x0 = cx + s * tw, x1 = cx + s * (tw + 60);
            g.fillGradient(Math.min(x0, x1), (int) y + 8, Math.max(x0, x1), (int) y + 9, Fundo.a(cor, e * 0.8f), Fundo.a(cor, e * 0.8f));
            float brilho = 0.5f + 0.5f * (float) Math.sin(t * 2 + k);
            g.fill(x1 - 1, (int) y + 7, x1 + 2, (int) y + 10, Fundo.a(Fundo.claro(cor, 0.5f), e * brilho));
        }
        if (sub != null) paragrafo(g, sub, width / 2, (int) y + 24, Math.min(width - 40, 420), 0xFFD8D0C0, e, true, 2);
    }

    private void profissoes(GuiGraphics g, int mx, int my, float t, float e) {
        tema(Fundo.NEUTRO);
        cabecalho(g, guildCharter ? "Guild Charter" : "Escolha sua Profissão",
                guildCharter ? "Escolha a segunda guilda. Você continua na que já tem."
                        : "Cada guilda fabrica os itens dos próprios mods. A escolha é permanente.",
                0xFFF0C878, t, e);
        int n = profs.size(), gap = 10;
        int cw = Math.max(70, Math.min(124, (width - 30 - gap * (n - 1)) / n));
        int top = 58, ch = Math.max(110, Math.min(190, height - top - 14));
        int x0 = (width - (n * cw + (n - 1) * gap)) / 2;
        for (int i = 0; i < n; i++) {
            Prof p = profs.get(i);
            Tema tm = Fundo.PROFISSOES.getOrDefault(p.id, Fundo.NEUTRO);
            boolean bloqueado = jaTem.contains(p.id);
            float ent = ease((seg(desdeEtapa) - i * 0.09f) / 0.5f);
            int cx = x0 + i * (cw + gap);
            boolean sobre = !bloqueado && mx >= cx && my >= top && mx < cx + cw && my < top + ch;
            float r = realce(p.id, sobre);
            int cy = top + Math.round((1 - ent) * 40 - 4 * r);
            float alfa = ent * (bloqueado ? 0.45f : 1f);

            Fundo.desenhar(g, font, tm, cx, cy, cw, ch, t + i * 3.7f, alfa);
            g.fill(cx, cy, cx + cw, cy + ch, Fundo.a(0x55000000, alfa * (1 - r)));
            Fundo.moldura(g, cx, cy, cw, ch, tm.destaque(), t + i, alfa, r);

            float pulso = 3 + r * (0.35f + 0.15f * (float) Math.sin(t * 5));
            textoEscala(g, p.icone, cx + cw / 2f, cy + 14 - pulso * 2, pulso, Fundo.claro(tm.destaque(), 0.15f + 0.3f * r), alfa, true);
            textoEscala(g, p.nome, cx + cw / 2f, cy + 50, cw > 100 ? 1.5f : 1.2f, 0xFFFFFFFF, alfa, true);
            paragrafo(g, LEMAS.getOrDefault(p.id, ""), cx + cw / 2, cy + 66, cw - 12, 0xFFE0D8C8, alfa, true, 3);

            int ly = cy + ch - 8 - 11 * p.inicios.size();
            g.fill(cx + 8, ly - 6, cx + cw - 8, ly - 5, Fundo.a(tm.destaque(), alfa * 0.5f));
            for (int k = 0; k < p.inicios.size(); k++) {
                String rot = p.inicios.get(k).rotulo;
                while (font.width("• " + rot) > cw - 12 && rot.length() > 4) rot = rot.substring(0, rot.length() - 2) + "…";
                float onda = r * (float) Math.sin(t * 4 - k * 0.8) * 1.5f;
                textoEscala(g, "• " + rot, cx + 7 + onda, ly + k * 11, 1, Fundo.claro(tm.destaque(), 0.45f), alfa, false);
            }
            if (bloqueado) {
                g.fill(cx, cy + ch / 2 - 9, cx + cw, cy + ch / 2 + 9, 0xB0000000);
                textoEscala(g, "Já é sua guilda", cx + cw / 2f, cy + ch / 2f - 4, 1, 0xFFB0B0B0, 1, true);
            } else {
                alvos.add(new Alvo(cx, cy, cw, ch, () -> {
                    prof = p;
                    clique();
                    som(SoundEvents.BOOK_PAGE_TURN, 1f);
                    mudarEtapa(1);
                }));
            }
        }
    }

    private void especializacoes(GuiGraphics g, int mx, int my, float t, float e) {
        Tema tp = Fundo.PROFISSOES.getOrDefault(prof.id, Fundo.NEUTRO);
        Tema sobreTema = null;
        cabecalho(g, prof.icone + " " + prof.nome + " " + prof.icone,
                "Por qual especialização quer começar? As outras vêm depois, pelas pesquisas da colônia (Guild of Trades).",
                Fundo.claro(tp.destaque(), 0.2f), t, e);
        botao(g, 8, 8, 56, 16, "‹ Voltar", 0xFF909090, mx, my, t, e, () -> {
            clique();
            mudarEtapa(0);
        });

        int n = prof.inicios.size(), gap = 10;
        int cols = n <= 3 ? n : 2, rows = (n + cols - 1) / cols;
        int top = 62, bottom = height - 12;
        int cw = Math.max(110, Math.min(220, (width - 30 - (cols - 1) * gap) / cols));
        int ch = Math.max(80, Math.min(124, (bottom - top - (rows - 1) * gap) / rows));
        int gx = (width - (cols * cw + (cols - 1) * gap)) / 2;
        int gy = top + Math.max(0, (bottom - top - (rows * ch + (rows - 1) * gap)) / 2);
        for (int i = 0; i < n; i++) {
            Inicio ini = prof.inicios.get(i);
            Tema tm = Fundo.daEspecializacao(ini.licenca, prof.id);
            float ent = ease((seg(desdeEtapa) - i * 0.08f) / 0.5f);
            int cx = gx + (i % cols) * (cw + gap) + Math.round((1 - ent) * 60);
            int cy0 = gy + (i / cols) * (ch + gap);
            boolean sobre = mx >= cx && my >= cy0 && mx < cx + cw && my < cy0 + ch;
            float r = realce(ini.licenca, sobre);
            int cy = cy0 - Math.round(3 * r);
            if (sobre) sobreTema = tm;

            Fundo.desenhar(g, font, tm, cx, cy, cw, ch, t + i * 2.3f, ent);
            g.fill(cx, cy, cx + cw, cy + ch, Fundo.a(0x50000000, ent * (1 - r * 0.7f)));
            Fundo.moldura(g, cx, cy, cw, ch, tm.destaque(), t + i, ent, r);

            float esc = 2 + 0.25f * r * (float) Math.sin(t * 5);
            textoEscala(g, tm.icone(), cx + 16, cy + 8 - (esc - 2) * 4, esc, Fundo.claro(tm.destaque(), 0.2f + 0.3f * r), ent, true);
            textoEscala(g, ini.rotulo, cx + 30, cy + 9, ini.rotulo.length() > 22 ? 1f : 1.25f, 0xFFFFFFFF, ent, false);
            paragrafo(g, ini.descricao, cx + 8, cy + 28, cw - 16, 0xFFE8E0D0, ent, false, 3);

            int max = Math.max(1, (cw - 16) / 18), ky = cy + ch - 24;
            g.fill(cx + 6, ky - 4, cx + cw - 6, ky - 3, Fundo.a(tm.destaque(), ent * 0.4f));
            for (int k = 0; k < Math.min(max, ini.kit.size()); k++) {
                int ix = cx + 8 + k * 18;
                float bob = (float) Math.sin(t * 3 + k * 0.7) * (0.6f + 1.4f * r);
                PoseStack ps = g.pose();
                ps.pushPose();
                ps.translate(0, bob, 0);
                if (ent > 0.6f) g.renderItem(ini.kit.get(k), ix, ky);
                ps.popPose();
                if (mx >= ix && my >= ky && mx < ix + 16 && my < ky + 16) dica = ini.kit.get(k);
            }
            if (r > 0.05f) textoEscala(g, "Escolher ›", cx + cw - 8 - font.width("Escolher ›"), cy + ch - 34 + (1 - r) * 4, 1,
                    Fundo.claro(tm.destaque(), 0.5f), r * ent, false);
            alvos.add(new Alvo(cx, cy, cw, ch, () -> {
                escolhido = ini;
                escola = null;
                clique();
                som(SoundEvents.BOOK_PAGE_TURN, 0.8f);
                mudarEtapa(pedeEscola() ? 4 : 2);
            }));
        }
        tema(sobreTema != null ? sobreTema : tp);
    }

    /** Escola de magia (só quando a especialização é o Iron's Spells): a runa de cada escola, tema próprio no fundo. */
    private void escolasMagia(GuiGraphics g, int mx, int my, float t, float e) {
        Tema ti = Fundo.daEspecializacao(IRONS, prof.id);
        Tema sobreTema = null;
        cabecalho(g, "✦ Escola de Magia ✦",
                "Você só aprende magias da escola escolhida e recebe o manuscrito dela. Outras escolas vêm depois pelo Codex of Schools (pesquisa Cross-School Studies).",
                Fundo.claro(ti.destaque(), 0.2f), t, e);
        botao(g, 8, 8, 56, 16, "‹ Voltar", 0xFF909090, mx, my, t, e, () -> {
            clique();
            mudarEtapa(1);
        });

        int n = escolas.size(), gap = 8;
        int top = 66, bottom = height - 10;
        int cols = Math.max(1, Math.min(n <= 8 ? 4 : 6, (width - 20 + gap) / (78 + gap)));
        int rows = (n + cols - 1) / cols;
        int cw = Math.min(104, (width - 20 - (cols - 1) * gap) / cols);
        int ch = Math.max(46, Math.min(80, (bottom - top - (rows - 1) * gap) / rows));
        int gx = (width - (cols * cw + (cols - 1) * gap)) / 2;
        int gy = top + Math.max(0, (bottom - top - (rows * ch + (rows - 1) * gap)) / 2);
        for (int i = 0; i < n; i++) {
            Escola es = escolas.get(i);
            Tema tm = temaEscola(es);
            float ent = ease((seg(desdeEtapa) - i * 0.05f) / 0.45f);
            int cx = gx + (i % cols) * (cw + gap);
            int cy0 = gy + (i / cols) * (ch + gap) + Math.round((1 - ent) * 30);
            boolean sobre = mx >= cx && my >= cy0 && mx < cx + cw && my < cy0 + ch;
            float r = realce("escola:" + es.id, sobre);
            int cy = cy0 - Math.round(3 * r);
            if (sobre) sobreTema = tm;

            Fundo.desenhar(g, font, tm, cx, cy, cw, ch, t + i * 1.9f, ent);
            g.fill(cx, cy, cx + cw, cy + ch, Fundo.a(0x50000000, ent * (1 - r * 0.7f)));
            Fundo.moldura(g, cx, cy, cw, ch, tm.destaque(), t + i, ent, r);

            // runa da escola em tamanho grande, flutuando (o símbolo do tema se ela não existir)
            float bob = (float) Math.sin(t * 2.5 + i * 0.8) * (1 + 1.5f * r);
            float esc = 1.6f + 0.3f * r;
            int meio = cx + cw / 2, iy = cy + (ch - 12) / 2 - 2;
            if (!es.runa.isEmpty() && ent > 0.5f) {
                PoseStack ps = g.pose();
                ps.pushPose();
                ps.translate(meio, iy + bob, 0);
                ps.scale(esc, esc, 1);
                g.renderItem(es.runa, -8, -8);
                ps.popPose();
            } else {
                textoEscala(g, tm.icone(), meio, iy - 8 + bob, 2, Fundo.claro(tm.destaque(), 0.3f), ent, true);
            }
            String nome = es.nome.getString();
            textoEscala(g, nome, meio, cy + ch - 12, 1, r > 0.5f ? 0xFFFFFFFF : Fundo.claro(tm.destaque(), 0.55f), ent, true);
            alvos.add(new Alvo(cx, cy, cw, ch, () -> {
                escola = es;
                clique();
                som(SoundEvents.ENCHANTMENT_TABLE_USE, 1.4f);
                mudarEtapa(2);
            }));
        }
        tema(sobreTema != null ? sobreTema : ti);
    }

    private void confirmacao(GuiGraphics g, int mx, int my, float t, float e) {
        Tema tm = escola != null ? temaEscola(escola) : Fundo.daEspecializacao(escolhido.licenca, prof.id);
        tema(tm);
        g.fill(0, 0, width, height, Fundo.a(0x80000000, e));
        int extra = escola != null ? 12 : 0;  // linha da escola de magia
        int pw = Math.min(310, width - 30), ph = 168 + extra;
        int px = (width - pw) / 2, py = (height - ph) / 2;
        PoseStack ps = g.pose();
        ps.pushPose();
        float esc = 0.85f + 0.15f * e;
        ps.translate(width / 2f, height / 2f, 0);
        ps.scale(esc, esc, 1);
        ps.translate(-width / 2f, -height / 2f, 0);

        Fundo.desenhar(g, font, tm, px, py, pw, ph, t, e);
        g.fill(px, py, px + pw, py + ph, Fundo.a(0x60000000, e));
        Fundo.moldura(g, px, py, pw, ph, tm.destaque(), t, e, 0.6f + 0.4f * (float) Math.sin(t * 3));

        textoEscala(g, "Confirmar escolha", width / 2f, py + 10, 1.5f, 0xFFFFFFFF, e, true);
        textoEscala(g, prof.icone + " " + prof.nome + "  ·  " + escolhido.rotulo, width / 2f, py + 30, 1, Fundo.claro(tm.destaque(), 0.3f), e, true);
        String aviso = guildCharter ? "A Guild Charter já foi gasta: esta será sua segunda guilda, para sempre."
                : "Essa escolha não pode ser desfeita (só com o Draught of Oblivion).";
        if (escola != null) textoEscala(g, tm.icone() + " Escola de " + escola.nome.getString() + " (com o manuscrito dela)",
                width / 2f, py + 42, 1, Fundo.claro(tm.destaque(), 0.4f), e, true);
        int dy = paragrafo(g, aviso, width / 2, py + 46 + extra, pw - 24, 0xFFFF9A8A, e, true, 2);
        if (!escolhido.kit.isEmpty()) {
            int ky = py + 50 + extra + dy;
            textoEscala(g, "Você recebe:", width / 2f, ky, 1, 0xFFD8D0C0, e, true);
            int n = Math.min(escolhido.kit.size(), (pw - 20) / 18);
            int kx = width / 2 - n * 9;
            for (int k = 0; k < n; k++) {
                float bob = (float) Math.sin(t * 3 + k * 0.7);
                ps.pushPose();
                ps.translate(0, bob, 0);
                if (e > 0.6f) g.renderItem(escolhido.kit.get(k), kx + k * 18, ky + 11);
                ps.popPose();
            }
        }
        ps.popPose();

        int by = py + ph - 26;
        botao(g, width / 2 - 98, by, 92, 18, "✔ Confirmar", 0xFF3FBF5A, mx, my, t, e, () -> {
            enviar();
            mudarEtapa(3);
        });
        botao(g, width / 2 + 6, by, 92, 18, "↩ Voltar", 0xFF909090, mx, my, t, e, () -> {
            clique();
            mudarEtapa(pedeEscola() ? 4 : 1);
        });
        // dica dos itens (fora da escala, posição aproximada)
        if (!escolhido.kit.isEmpty()) {
            int n = Math.min(escolhido.kit.size(), (pw - 20) / 18);
            int ky = py + 50 + extra + 11 + paragrafoAltura(aviso, pw - 24);
            int kx = width / 2 - n * 9;
            for (int k = 0; k < n; k++) if (mx >= kx + k * 18 && my >= ky && mx < kx + k * 18 + 16 && my < ky + 16) dica = escolhido.kit.get(k);
        }
    }

    private int paragrafoAltura(String s, int largura) {
        return Math.min(2, font.split(Component.literal(s), largura).size()) * (font.lineHeight + 1);
    }

    private void enviar() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (guildCharter) mc.player.connection.sendCommand("guildcharter " + escolhido.licenca);
        else mc.player.connection.sendCommand("trigger prof.escolha set " + escolhido.codigo);
        som(SoundEvents.ENCHANTMENT_TABLE_USE, 1f);
        som(SoundEvents.PLAYER_LEVELUP, 0.7f);
    }

    /** Selo: anéis que se expandem e o símbolo da especialização; fecha sozinho. */
    private void fim(GuiGraphics g, float t) {
        Tema tm = escola != null ? temaEscola(escola) : Fundo.daEspecializacao(escolhido.licenca, prof.id);
        float s = seg(desdeEtapa);
        // a escola só pode ser escolhida depois que o servidor aplicar a profissão (o /trigger roda no tick seguinte)
        if (escola != null && !escolaEnviada && s > 1.0f) {
            escolaEnviada = true;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) mc.player.connection.sendCommand("escola " + escola.id);
        }
        float sai = s > DURACAO_FIM - 0.4f ? 1 - (s - (DURACAO_FIM - 0.4f)) / 0.4f : 1;
        g.fill(0, 0, width, height, Fundo.a(0x70000000, sai));
        float cx = width / 2f, cy = height / 2f - 10;
        for (int k = 0; k < 3; k++) {
            float f = s - k * 0.22f;
            if (f <= 0) continue;
            float raio = ease(f / 1.2f) * Math.max(width, height) * 0.6f;
            float al = (1 - Math.min(1, f / 1.2f)) * sai;
            int pontos = Math.max(40, (int) (raio * 1.5f));
            for (int i = 0; i < pontos; i++) {
                double a = 2 * Math.PI * i / pontos + k;
                int x = (int) (cx + Math.cos(a) * raio), y = (int) (cy + Math.sin(a) * raio);
                g.fill(x, y, x + 2, y + 2, Fundo.a(Fundo.claro(tm.destaque(), 0.3f * k), al));
            }
        }
        float esc = 4 + 1.5f * (1 - ease(s / 0.6f)) + 0.2f * (float) Math.sin(t * 6);
        textoEscala(g, tm.icone(), cx, cy - esc * 4, esc, Fundo.claro(tm.destaque(), 0.2f), ease(s / 0.3f) * sai, true);
        float tx = ease((s - 0.3f) / 0.5f) * sai;
        textoEscala(g, guildCharter ? "Nova guilda!" : "Bem-vindo à guilda!", cx, cy + 26, 2, 0xFFFFFFFF, tx, true);
        textoEscala(g, prof.nome + "  ·  " + escolhido.rotulo + (escola != null ? "  ·  " + escola.nome.getString() : ""),
                cx, cy + 46, 1, Fundo.claro(tm.destaque(), 0.4f), tx, true);
        if (s >= DURACAO_FIM) onClose();
    }

    // ------------------------------------------------------------------ entrada
    @Override
    public boolean mouseClicked(double mx, double my, int botao) {
        if (botao != 0 || etapa == 3) return super.mouseClicked(mx, my, botao);
        for (int i = alvos.size() - 1; i >= 0; i--) {  // o último desenhado fica por cima
            if (alvos.get(i).dentro(mx, my)) {
                alvos.get(i).acao().run();
                return true;
            }
        }
        return super.mouseClicked(mx, my, botao);
    }

    @Override
    public boolean keyPressed(int tecla, int scan, int mods) {
        if (tecla == ESC && (etapa == 1 || etapa == 2 || etapa == 4)) {
            clique();
            mudarEtapa(etapa == 1 ? 0 : etapa == 4 ? 1 : pedeEscola() ? 4 : 1);
            return true;
        }
        if (tecla == ESC && etapa == 3) return true;
        return super.keyPressed(tecla, scan, mods);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
