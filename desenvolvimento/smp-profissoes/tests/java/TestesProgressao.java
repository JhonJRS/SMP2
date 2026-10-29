import br.smp.profissoes.Decisao;
import br.smp.profissoes.Regras;
import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.rp.rpessentials.RpEssentialsPatternUtils;

/**
 * Testes sem jogo:
 *  - A/F: a config real do RPEssentials (gerada) aplicada com a função de padrão REAL do RPEssentials;
 *  - B: regras do mod (pesquisas/edifícios) via a lógica de decisão REAL do mod;
 *  - C: matriz profissão x etapa x item;
 *  - D: nenhuma restrição de posse/uso/equipamento;
 *  - E: Numismatics fora do Engenheiro e livre para todos.
 * Uso: veja tests/rodar_testes.ps1
 */
public final class TestesProgressao {
    static int ok = 0, falhas = 0;

    static void checar(boolean cond, String nome) {
        if (cond) ok++;
        else {
            falhas++;
            System.out.println("FALHOU: " + nome);
        }
    }

    // --- leitura das listas do TOML gerado
    static List<String> listaToml(String toml, String chave) {
        Matcher m = Pattern.compile("(?m)^\\s*" + chave + " = \\[(.*)\\]\\s*$").matcher(toml);
        if (!m.find()) throw new IllegalStateException("chave ausente: " + chave);
        List<String> out = new ArrayList<>();
        // as entradas geradas não têm aspas/escapes internos; [^"]*+ evita recursão do regex em linhas longas
        Matcher s = Pattern.compile("\"([^\"]*+)\"").matcher(m.group(1));
        while (s.find()) out.add(s.group(1));
        return out;
    }

    static List<String> globais, permitidos;

    /** Reimplementa ProfessionRestrictionManager.isGloballyBlocked + hasPermission (conferido no bytecode). */
    static boolean podeFabricar(List<String> licencas, String item) {
        boolean bloqueado = globais.stream().anyMatch(p -> RpEssentialsPatternUtils.matchesPattern(item, p));
        if (!bloqueado) return true;
        for (String lic : licencas) {
            for (String entrada : permitidos) {
                if (!entrada.contains(";")) continue;
                String[] partes = entrada.split(";", 2);
                if (!partes[0].toLowerCase().trim().equals(lic.toLowerCase())) continue;
                for (String p : partes[1].split(",")) if (RpEssentialsPatternUtils.matchesPattern(item, p)) return true;
            }
        }
        return false;
    }

    public static void main(String[] a) throws Exception {
        Path inst = Path.of(a[0]);
        String toml = Files.readString(inst.resolve("config/rpessentials/rpessentials-professions.toml"), StandardCharsets.UTF_8);
        globais = listaToml(toml, "globalBlockedCrafts");
        permitidos = listaToml(toml, "professionAllowedCrafts");
        Regras regras = new Gson().fromJson(Files.readString(inst.resolve("config/smpprofissoes/progressao.json"), StandardCharsets.UTF_8), Regras.class);

        // ---------- Teste E: Numismatics
        checar(globais.stream().noneMatch(g -> g.startsWith("numismatics:")), "E: numismatics não está bloqueado globalmente");
        checar(permitidos.stream().noneMatch(p -> p.contains("numismatics")), "E: numismatics não aparece em nenhuma profissão");
        checar(toml.lines().filter(l -> !l.trim().startsWith("#")).noneMatch(l -> l.contains("numismatics")), "E: nenhuma linha ativa do TOML cita numismatics");
        for (String prof : List.of("", "engenheiro", "arcanista", "ferreiro", "naturalista"))
            checar(podeFabricar(prof.isEmpty() ? List.of() : List.of(prof), "numismatics:vendor"), "E: numismatics:vendor livre para '" + prof + "'");
        checar(Files.list(inst.resolve("mods")).anyMatch(f -> f.getFileName().toString().startsWith("CreateNumismatics")), "E: jar do Numismatics continua instalado");
        checar(Files.list(inst.resolve("mods")).anyMatch(f -> f.getFileName().toString().startsWith("create-1.21.1")), "E: dependência Create continua instalada");

        // ---------- Teste A/F: profissões, mod inicial escolhido e regressão
        String[] profs = {"engenheiro", "arcanista", "ferreiro", "naturalista"};
        checar(Arrays.equals(profs, regras.profissoes.toArray()), "A: as 4 profissões continuam definidas nas regras");
        // a profissão sozinha não libera mod nenhum; o mod inicial vem da escolha (etapa de fundação com 'inicio')
        String[][] inicios = {{"engenheiro", "eng_create", "create:cogwheel"}, {"engenheiro", "eng_ie", "immersiveengineering:hammer"},
                {"engenheiro", "eng_embers", "embers:ember_crystal"}, {"arcanista", "arc_irons", "irons_spellbooks:copper_spell_book"},
                {"arcanista", "arc_malum", "malum:spirit_altar"}, {"arcanista", "arc_botania", "botania:apothecary_default"},
                {"arcanista", "arc_neovitae", null}, {"ferreiro", "fer_silentgear", "productivemetalworks:casting_table"},
                {"ferreiro", "fer_silentgear", "silentgear:pickaxe"}, {"ferreiro", "fer_arcanus", "forbidden_arcanus:arcane_crystal"},
                {"naturalista", "nat_cozinha", "farmersdelight:cooking_pot"}, {"naturalista", "nat_cozinha", "farm_and_charm:stove"},
                {"naturalista", "nat_ervas", "vital_herbs:teapot"}};
        for (String[] t : inicios) {
            Regras.Etapa e = regras.etapas.stream().filter(x -> x.licenca.equals(t[1])).findFirst().orElse(null);
            checar(e != null && e.inicio && e.profissao.equals(t[0]), "A: " + t[1] + " é mod inicial de " + t[0]);
            if (t[2] == null) continue;
            checar(!podeFabricar(List.of(t[0]), t[2]), "F: só a profissão não fabrica " + t[2]);
            checar(podeFabricar(List.of(t[0], t[1]), t[2]), "F: " + t[0] + " começando por " + t[1] + " fabrica " + t[2]);
            checar(!podeFabricar(List.of(), t[2]), "F: sem profissão não fabrica " + t[2]);
            for (String outra : profs)
                if (!outra.equals(t[0])) {
                    checar(!podeFabricar(List.of(outra), t[2]), "F: " + outra + " não fabrica " + t[2]);
                    // o mod só concede a etapa a quem tem a profissão dela
                    checar(Decisao.calcular(List.of(outra, "inicio_" + t[1]), List.of(), regras).vazio(), "F: " + outra + " não recebe " + t[1] + " nem com o marcador");
                }
        }
        // começar por um mod não libera os outros mods da profissão
        checar(!podeFabricar(List.of("engenheiro", "eng_create"), "immersiveengineering:hammer"), "F: começar pelo Create não libera o IE");
        checar(!podeFabricar(List.of("ferreiro", "fer_arcanus"), "productivemetalworks:casting_table"), "F: começar pelo Forbidden Arcanus não libera o PM");
        for (String livre : List.of("minecraft:crafting_table", "minecolonies:blockhutmechanic", "supplementaries:sack", "quark:backpack", "numismatics:vendor", "cataclysm:black_steel_ingot", "cataclysm:polished_end_stone"))
            checar(podeFabricar(List.of(), livre), "F: mod fora das profissões continua livre: " + livre);

        // ---------- Teste C: matriz profissão x etapa (amostra de itens por etapa)
        String[][] amostra = {
                {"eng_latao", "create:mechanical_arm"}, {"eng_latao", "create:empty_blaze_burner"}, {"eng_latao", "create:brass_ingot"}, {"eng_latao", "create:brass_casing"},
                {"eng_combustiveis", "createdieselgenerators:diesel_engine"}, {"c1_eng", "simulated:physics_assembler"}, {"c1_eng", "offroad:tire"},
                {"eng_industria_pesada", "immersiveengineering:heavy_engineering"}, {"eng_alta_potencia", "immersiveengineering:capacitor_hv"},
                {"eng_petroquimica", "immersivepetroleum:pumpjack"}, {"c15_eng", "immersiveengineering:railgun"},
                {"eng_arsenal_ie", "immersiveengineering:revolver"}, {"c15_eng", "immersiveengineering:drill"},
                {"eng_arsenal_ember", "embers:blazing_ray"}, {"eng_alquimia_artificios", "embers:ashen_cloak"},
                {"arc_cavaleiros", "irons_spellbooks:pyromancer_chestplate"}, {"arc_maestria", "irons_spellbooks:dragonskin_spell_book"}, {"c16_arc", "hazennstuff:seraph_chestplate"},
                {"arc_tecelagem", "malum:weavers_workbench"}, {"c17_arc", "malum:weight_of_worlds"}, {"arc_arsenal_mana", "botania:manasteel_sword"},
                {"c23_arc", "botania:flight_tiara"}, {"fer_armaria", "silentgear:chestplate_blueprint"}, {"fer_distancia", "silentgear:bow_blueprint"},
                {"fer_prismas", "forbidden_arcanus:apply_modifier_smithing_template"}, {"nat_pecuaria", "farm_and_charm:chicken_coop"},
                {"nat_boticario", "minecraft:brewing_stand"}, {"nat_boticario", "botania:vial"}, {"c19_nat", "toxony:eternal_plague"},
                {"eng_processamento_ember", "embers:melter"}, {"eng_alquimia_artificios", "embers:alchemy_tablet"},
                {"eng_integracao_create_embers", "embers:create_powered_actuator"}, {"c9_eng", "embers:tyrfing"},
                {"eng_arquitetura", "createdeco:industrial_iron_catwalk"}, {"eng_logistica", "create_security:card_reader"},
                {"arc_pergaminhos", "irons_spellbooks:scroll_forge"}, {"arc_pergaminhos", "irons_spellbooks:fire_rune"},
                {"arc_maestria", "irons_spellbooks:fire_upgrade_orb"}, {"c6_arc", "hazennstuff:miner_helmet"},
                {"arc_malum_infusao", "malum:soul_stained_steel_sword"}, {"arc_malum_rituais", "malum:totemic_staff"},
                {"arc_mana", "botania:mana_pool"}, {"c23_arc", "botania:alfheim_portal"}, {"c8_arc", "botania:terra_plate"}, {"c8_arc", "botania:terrasteel_ingot"},
                {"fer_precisao", "silentgear:pickaxe_blueprint"}, {"fer_precisao", "silentgear:alloy_forge"}, {"fer_precisao", "transmog:void_fragment"},
                {"fer_mestre", "silentgear:upgrade_base"}, {"c3_fer", "silentgear:tyrian_steel_ingot"},
                {"fer_fundicao", "productivemetalworks:powered_heating_coil"},
                {"fer_arcana", "forbidden_arcanus:clibano_core"}, {"fer_hefesto", "forbidden_arcanus:diamond_blacksmith_gavel"},
                {"c7_fer", "forbidden_arcanus:eternal_stella"},
                {"nat_vinicultura", "vinery:apple_press"}, {"nat_toxicologia", "toxony:mortar_pestle"}, {"nat_alquimia", "toxony:alembic"},
                {"c5_nat", "toxony:lethal_dose"}, {"c10_nat", "toxony:plaguebringer_mask"},
                // lista de pendências (25/10)
                {"eng_vapor", "create:steam_engine"}, {"eng_logistica", "securitycraft:keypad_item"}, {"c2_eng", "securitycraft:sentry"},
                {"c2_eng", "securitycraft:rift_stabilizer"}, {"eng_arsenal_ie", "securitycraft:claymore"}, {"c11_eng", "create_wizardry:mana_siphon"},
                {"c13_eng", "protection_pixel:armorplatekit"}, {"arc_eldritch", "darkermagic:warden_stick"}, {"arc_megatocha", "torchmaster:megatorch"},
                {"arc_protecao", "legendaryshrines:shrine"}, {"c12_nat", "neovitae:alchemy_flask"}, {"arc_malum_cadinho", "malum:spirit_crucible"},
                {"c14_arc", "malum:ring_of_the_hoarder"}, {"fer_armas", "simplyswords:iron_claymore"}, {"fer_runica", "simplyswords:runic_tablet"},
                {"c5_nat", "toxony:witching_blade"},
                // rodada 5
                {"eng_ae2", "ae2:controller"}, {"eng_ae2_quantico", "ae2:singularity"}, {"eng_ae2_quantico", "ae2:quantum_ring"},
                {"nat_bruxaria", "enchanted:altar"}, {"nat_caldeirao", "enchanted:kettle"}, {"nat_poppets", "enchanted:spinning_wheel"},
                {"nat_ritos", "enchanted:waystone"}, {"nat_culinaria", "farmersdelight:beef_stew"}, {"nat_culinaria", "farmersdelight:roast_chicken_block"},
                {"fer_cataclismo", "cataclysm:black_steel_sword"}, {"fer_reliquias", "cataclysm:ignitium_chestplate"},
                {"fer_fusao", "cataclysm:mechanical_fusion_anvil"}, {"c22_fer", "simplyswords:watcher_claymore"}, {"eng_ae2_quantico", "irons_spellbooks:portal_frame"},
                // rodada 6
                {"c25_eng", "protection_pixel:breakeras_chestplate"}, {"c13_eng", "protection_pixel:steamectoskeleton"}, {"fer_arcos", "simplybows:echo_bow/echo_bow"},
                {"fer_arcos", "simplybows:upgrades/rune_etching_chaos"}, {"nat_vinhos", "vinery:chorus_wine"}, {"nat_ritos", "enchanted:waystone"}};
        for (String[] t : amostra) {
            String lic = t[0], item = t[1];
            Regras.Etapa e = regras.etapas.stream().filter(x -> x.licenca.equals(lic)).findFirst().orElseThrow();
            checar(!podeFabricar(List.of(e.profissao), item), "C: " + e.profissao + " SEM a etapa não fabrica " + item);
            checar(podeFabricar(List.of(e.profissao, lic), item), "C: " + e.profissao + " COM " + lic + " fabrica " + item);
            for (String outra : profs)
                if (!outra.equals(e.profissao)) checar(!podeFabricar(List.of(outra), item), "C: " + outra + " não fabrica " + item);
        }
        // fundação não vaza itens das etapas seguintes
        checar(!podeFabricar(List.of("engenheiro", "eng_create"), "create:brass_ingot"), "C: Create Foundations não libera latão");
        checar(!podeFabricar(List.of("arcanista", "arc_irons"), "irons_spellbooks:scroll_forge"), "C: Iron's Foundations não libera a forja de pergaminhos");
        checar(!podeFabricar(List.of("arcanista", "arc_botania"), "botania:terrasteel_ingot"), "C: Botania Foundations não libera terrasteel");
        checar(!podeFabricar(List.of("ferreiro", "fer_silentgear"), "silentgear:tyrian_steel_ingot"), "C: Silent Gear Foundations não libera tyrian steel");
        // ---------- Teste B: lógica de decisão do mod (pesquisas + edifícios)
        Decisao.Colonia vazia = new Decisao.Colonia("A", Set.of(), Map.of());
        Decisao.Colonia soPesquisa = new Decisao.Colonia("B", Set.of("smpprofissoes:oficios/fundacao_create", "smpprofissoes:oficios/metalurgia_latao"), Map.of("minecolonies:mechanic", 1));
        Decisao.Colonia completa = new Decisao.Colonia("C", Set.of("smpprofissoes:oficios/fundacao_create", "smpprofissoes:oficios/metalurgia_latao", "smpprofissoes:oficios/fundacao_embers", "smpprofissoes:oficios/logistica_ember", "smpprofissoes:oficios/processamento_ember"),
                Map.of("minecolonies:mechanic", 3));
        checar(Decisao.calcular(List.of(), List.of(completa), regras).vazio(), "B: sem profissão não recebe etapa");
        checar(Decisao.calcular(List.of("engenheiro"), List.of(vazia), regras).vazio(), "B: colônia sem pesquisa não libera nada");
        checar(Decisao.calcular(List.of("engenheiro"), List.of(soPesquisa), regras).conceder().stream().noneMatch(e -> e.licenca.equals("eng_latao")), "B: pesquisa sem Mechanic 2 não libera Latão");
        var r = Decisao.calcular(List.of("engenheiro"), List.of(completa), regras);
        checar(r.conceder().stream().anyMatch(e -> e.licenca.equals("eng_latao")), "B: pesquisa + Mechanic 3 libera Latão");
        checar(r.conceder().stream().anyMatch(e -> e.licenca.equals("eng_integracao_create_embers")), "B: integração exige as duas linhas e é liberada");
        checar(r.conceder().stream().noneMatch(e -> e.licenca.equals("eng_combustiveis")), "B: etapa seguinte não é liberada antes da hora");
        checar(Decisao.calcular(List.of("engenheiro"), List.of(soPesquisa), regras).conceder().stream().noneMatch(e -> e.licenca.equals("eng_integracao_create_embers")),
                "B: integração não sai com só uma das pesquisas");
        checar(Decisao.calcular(List.of("arcanista"), List.of(completa), regras).vazio(), "B: profissão errada não recebe etapas do Engenheiro");
        checar(Decisao.calcular(List.of("engenheiro", "eng_latao"), List.of(vazia), regras).remover().stream().anyMatch(e -> e.licenca.equals("eng_latao")),
                "B: etapa é removida se a colônia perde o requisito");
        checar(Decisao.calcular(List.of("engenheiro", "eng_latao"), List.of(vazia, completa), regras).remover().isEmpty(), "B: basta uma colônia que cumpra");
        checar(Decisao.calcular(List.of("ENGENHEIRO"), List.of(completa), regras).conceder().size() > 0, "B: licença base sem diferenciar maiúsculas");
        // save de versão antiga: Brass Metallurgy concluída sem a Create Foundations → nenhuma etapa do latão
        Decisao.Colonia antiga = new Decisao.Colonia("Velha", Set.of("smpprofissoes:oficios/metalurgia_latao", "smpprofissoes:oficios/logistica_seguranca"), Map.of("minecolonies:mechanic", 5));
        checar(Decisao.calcular(List.of("engenheiro"), List.of(antiga), regras).vazio(), "B: nó concluído sem a cadeia (save antigo) não libera etapa");
        checar(regras.etapas.stream().filter(x -> x.licenca.equals("eng_logistica")).findFirst().orElseThrow().pesquisas
                .containsAll(List.of("smpprofissoes:oficios/fundacao_create", "smpprofissoes:oficios/metalurgia_latao", "smpprofissoes:oficios/logistica_seguranca")),
                "B: etapa exige a cadeia inteira (fundação → latão → logística)");
        // mod inicial: concedido sem colônia, só para a etapa marcada como inicio e só com a profissão certa
        var ini = Decisao.calcular(List.of("engenheiro", "inicio_eng_create"), List.of(), regras);
        checar(ini.conceder().stream().anyMatch(e -> e.licenca.equals("eng_create")), "B: escolher Create libera Create Foundations sem colônia");
        checar(ini.conceder().size() == 1, "B: o mod inicial não libera mais nada");
        checar(Decisao.calcular(List.of("arcanista", "inicio_eng_create"), List.of(), regras).vazio(), "B: marcador de outra profissão não vale");
        checar(Decisao.calcular(List.of("engenheiro", "inicio_eng_latao"), List.of(), regras).vazio(), "B: etapa que não é inicial não vem por marcador");
        checar(Decisao.calcular(List.of("engenheiro", "inicio_eng_create", "eng_create"), List.of(), regras).remover().isEmpty(), "B: mod inicial nunca é removido");
        // pesquisa cruzada (sem selos): a profissão que faz o item final recebe a licença; o componente é dela
        Set<String> cadeiaEstelar = new java.util.HashSet<>(regras.etapas.stream().filter(x -> x.licenca.equals("c7_fer")).findFirst().orElseThrow().pesquisas);
        Decisao.Colonia cruzada = new Decisao.Colonia("X", cadeiaEstelar,
                Map.of("minecolonies:blacksmith", 5, "minecolonies:enchanter", 5, "minecolonies:plantation", 5));
        checar(Decisao.calcular(List.of("ferreiro"), List.of(cruzada), regras).conceder().stream().anyMatch(e -> e.licenca.equals("c7_fer")),
                "B: Stellar Forging libera c7_fer para o Ferreiro");
        checar(Decisao.calcular(List.of("engenheiro"), List.of(cruzada), regras).vazio(), "B: Stellar Forging não libera nada ao Engenheiro");
        checar(regras.etapas.stream().noneMatch(x -> List.of("c7_arc", "c7_nat", "c2_fer", "c4_nat").contains(x.licenca)), "B: licenças que só faziam selo foram removidas");
        checar(regras.receitasComLicenca.keySet().stream().noneMatch(k -> k.startsWith("smpprofissoes:selo/")), "B: nenhuma receita de selo");
        checar(List.of("c7_fer").equals(regras.receitasComLicenca.get("smpprofissoes:componente/stellar_core")), "B: Stellar Core é feito pelo Ferreiro com a pesquisa");
        checar(List.of("c6_arc").equals(regras.receitasComLicenca.get("smpprofissoes:componente/arcane_inscription")), "B: Arcane Forge Inscription é do Arcanista com Spellforged Arms");
        // regra do usuário: universidade nível N só exige prédios até N (vale também para as etapas)
        for (Regras.Etapa e : regras.etapas)
            for (var b : e.edificios.entrySet())
                checar(b.getValue() <= 5, "B: etapa " + e.licenca + " pede " + b.getKey() + " " + b.getValue());
        for (Regras.Etapa e : regras.etapas) {
            checar(!e.pesquisas.isEmpty(), "B: etapa " + e.licenca + " exige pesquisa");
            checar(regras.profissoes.contains(e.profissao), "B: etapa " + e.licenca + " tem profissão válida");
        }

        // ---------- Teste D: posse/uso/equipamento livres
        for (String chave : List.of("globalUnbreakableBlocks", "globalBlockedItems", "globalBlockedEquipment"))
            checar(listaToml(toml, chave).isEmpty(), "D: " + chave + " vazio (nenhuma restrição de uso/posse)");

        System.out.println("\nTestes: " + ok + " OK, " + falhas + " falha(s).");
        System.exit(falhas == 0 ? 0 : 1);
    }
}
