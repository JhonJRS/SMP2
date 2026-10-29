"""Migração v15 (27/10/2026): lista da rodada 8 (ver smp-profissoes/PENDENCIAS_R8.md). Roda uma vez.
Depois desta, rode tools/migracoes/corrigir_adiantados.py.
"""
import json
import re
import sys
import zipfile
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[2]
INST = RAIZ.parent
SRC = RAIZ / "progressao.json"
sys.path.insert(0, str(RAIZ / "tools"))
import gerar  # noqa: E402

cfg = json.loads(SRC.read_text(encoding="utf-8"))
if cfg.get("_v15"):
    raise SystemExit("já migrado (v15)")
cfg["_v15"] = True
ix = gerar.indexar()

O = "smpprofissoes:oficios/"
P = {p["id"].split("/", 1)[1]: p for p in cfg["pesquisas"]}
E = {e["licenca"]: e for e in cfg["etapas"]}
PROF = cfg["profissoes"]
I = lambda i: {"item": i}


def existe(*itens):
    for i in itens:
        if i not in ix.itens and not i.startswith("smpprofissoes:"):
            raise SystemExit(f"item inexistente: {i}")
    return list(itens)


def do_jar(padrao_jar, caminho):
    jar = next((INST / "mods").glob(padrao_jar))
    with zipfile.ZipFile(jar) as z:
        return json.loads(z.read(caminho).decode("utf-8"))


def dado(caminho, obj, comentario):
    cfg["arquivos_dados"] = [a for a in cfg.get("arquivos_dados", []) if a["caminho"] != caminho]
    cfg["arquivos_dados"].append({"caminho": caminho, "_comentario": comentario, "json": obj})


def pesquisa(id_, nome, sub, nivel, icone, edificios, custos, pai, extra=()):
    existe(icone, *custos)
    p = {"id": O + id_, "nome": nome, "subtitulo": sub, "nivel": nivel, "icone": icone, "edificios": edificios, "custos": custos, "pai": O + pai}
    if extra:
        p["pesquisas_extra"] = [O + x for x in extra]
    lst = cfg["pesquisas"]
    i = max(k for k, x in enumerate(lst) if x["id"] == O + pai or x.get("pai") == O + pai)
    lst.insert(i + 1, p)
    P[id_] = p


def etapa(lic, nome, prof, pesq, edificios, **extra):
    e = {"licenca": lic, "nome": nome, "profissao": prof, "pesquisas": [O + pesq], "edificios": dict(edificios), "itens": []}
    e.update(extra)
    cfg["etapas"].append(e)
    E[lic] = e


VAZIO = {"replace": True, "values": []}

# ============================================================ mundo: estruturas e spawn
dado("data/netherman/tags/worldgen/biome/overworld_land.json", VAZIO,
     "Cult of Azazel: Believer House, Nether Geyser e Pipes não geram no Overworld.")
for s in ("maze", "monument", "head_altar"):
    j = do_jar("cultofazazel*.jar", f"data/netherman/worldgen/structure/{s}.json")
    j["biomes"] = []
    dado(f"data/netherman/worldgen/structure/{s}.json", j, f"Cult of Azazel: {s} não gera (era do Overworld).")
dado("data/ae2/tags/worldgen/biome/has_meteorites.json", VAZIO, "AE2: sem meteoros naturais (as prensas têm receita).")
jar_idas = next((INST / "mods").glob("idas-*.jar"))
with zipfile.ZipFile(jar_idas) as z:
    for n in z.namelist():
        if n.startswith("data/idas/worldgen/structure_set/") and n.endswith(".json"):
            j = json.loads(z.read(n).decode("utf-8"))
            pl = j.get("placement", {})
            if "spacing" in pl:
                pl["spacing"] = min(4096, int(pl["spacing"]) * 2)
                pl["separation"] = min(pl["spacing"] - 1, int(pl.get("separation", 0)) * 2)
                dado(n, j, "IDAS: estruturas duas vezes mais espaçadas.")
dado("data/relics/loot_modifiers/relic_loot.json",
     {"conditions": [{"condition": "minecraft:random_chance", "chance": 0.35}], "type": "relics:relic_loot"},
     "Relics: o sorteio de relíquia só acontece em 35% dos baús/saques.")

# ============================================================ Enhanced Celestials: mais raro e ligado às estações
dims = do_jar("Enhanced-Celestials-2-Core*.jar", "data/minecraft/enhancedcelestials2core/lunar/dimension_settings/overworld.json")
dims["min_days_between_all_events"] = 8
dado("data/minecraft/enhancedcelestials2core/lunar/dimension_settings/overworld.json", dims,
     "Enhanced Celestials: no mínimo 8 dias entre eventos lunares (era 3).")
estacoes = {"blood_moon": ["AUTUMN", "WINTER"], "super_blood_moon": ["WINTER"], "harvest_moon": ["SUMMER", "AUTUMN"],
            "super_harvest_moon": ["AUTUMN"], "blue_moon": ["WINTER", "SPRING"], "super_blue_moon": ["WINTER"],
            "super_moon": ["SPRING", "SUMMER"]}
for ev, est in estacoes.items():
    caminho = f"data/enhancedcelestials2defaultlunarevents/enhancedcelestials2core/lunar/event_probability/{ev}.json"
    j = do_jar("Enhanced-Celestials-2-Default*.jar", caminho)
    for d in j["dimensions"].values():
        for regra in d["spawn_requirements"]["rules"]:
            if regra["type"] == "enhancedcelestials2core:min_nights_between":
                regra["settings"]["min_nights"] = int(regra["settings"]["min_nights"]) * 3
        d["spawn_requirements"]["rules"].append({"type": "smpprofissoes:season", "settings": {"seasons": est}})
    j = {"neoforge:conditions": [{"type": "neoforge:mod_loaded", "modid": "smpprofissoes"},
                                 {"type": "neoforge:mod_loaded", "modid": "sereneseasons"}], **j}
    dado(caminho, j, f"Enhanced Celestials: {ev} 3x mais raro e só em {', '.join(est)} (Serene Seasons).")

# ============================================================ Create Cobblestone: blocos naturais decorativos
for b in ("granite", "diorite", "andesite", "tuff", "calcite", "dripstone_block", "cobbled_deepslate", "blackstone", "netherrack",
          "end_stone", "sand", "red_sand", "gravel", "dirt", "clay", "mud", "moss_block", "sandstone", "red_sandstone", "terracotta",
          "smooth_basalt", "mossy_cobblestone"):
    existe(f"minecraft:{b}")
    dado(f"data/createcobblestone/generator_types/{b}.json", {"block": f"minecraft:{b}"},
         "Create Cobblestone: gerador de bloco natural decorativo.")

# ============================================================ receitas
fora = sorted(rid for rid, r in ix.receitas.items() if r["pasta"] == "recipe" and rid.split(":", 1)[0] in ("endrem", "easy_npc"))
cfg["receitas_removidas"] = sorted(set(cfg["receitas_removidas"]) | set(fora))
# AE2: prensas na bancada (sem meteoros) e Matter Cannon mais cara
for prensa, chave in (("calculation_processor", "ae2:certus_quartz_crystal"), ("engineering_processor", "minecraft:diamond_block"),
                      ("logic_processor", "minecraft:gold_block"), ("silicon", "ae2:silicon")):
    existe(chave, f"ae2:{prensa}_press")
    cfg["receitas_novas"].append({"id": f"smpprofissoes:ae2/{prensa}_press", "_comentario": "Prensa do AE2 na bancada (não há meteoros).",
                                  "json": {"type": "minecraft:crafting_shaped", "category": "misc",
                                           "key": {"I": I("minecraft:iron_block"), "K": I(chave), "P": I("create:precision_mechanism")},
                                           "pattern": ["IPI", "PKP", "IPI"], "result": {"count": 1, "id": f"ae2:{prensa}_press"}}})
cfg["receitas_modificadas"].append({
    "id": "ae2:tools/matter_cannon", "caminho": "data/ae2/recipe/tools/matter_cannon.json",
    "_comentario": "Matter Cannon mais cara: netherite, núcleo de aniquilação, célula densa e componente 16k.",
    "operacoes": [{"op": "definir", "caminho": "key.a", "valor": I("minecraft:netherite_ingot")},
                  {"op": "definir", "caminho": "key.c", "valor": I(existe("ae2:cell_component_16k")[0])},
                  {"op": "definir", "caminho": "key.d", "valor": I(existe("ae2:dense_energy_cell")[0])},
                  {"op": "definir", "caminho": "key.e", "valor": I(existe("ae2:annihilation_core")[0])},
                  {"op": "definir", "caminho": "pattern.1", "valor": "cde"}]})
# Schematicannon: pesquisa de meio de jogo
pesquisa("engenharia_esquemas", "Schematic Engineering", "Create: the Schematicannon and schematic table",
         3, "create:schematicannon", {"minecolonies:mechanic": 3}, {"create:brass_ingot": 16, "minecraft:paper": 64, "minecraft:dispenser": 4},
         "arquitetura_industrial")
etapa("eng_esquemas", "Engineer · Schematic Engineering", "engenheiro", "engenharia_esquemas", {"minecolonies:mechanic": 3},
      itens=existe("create:schematicannon", "create:schematic_table", "create:empty_schematic", "create:schematic_and_quill", "create:schematic"))
cfg["receitas_modificadas"].append({
    "id": "create:crafting/schematics/schematicannon", "caminho": "data/create/recipe/crafting/schematics/schematicannon.json",
    "_comentario": "Schematicannon de meio de jogo: tubo eletrônico, casings de latão e andesito.",
    "json": {"type": "minecraft:crafting_shaped", "category": "misc",
             "key": {"T": I("create:electron_tube"), "B": I("create:brass_casing"), "D": I("minecraft:dispenser"),
                     "S": I("minecraft:smooth_stone"), "A": I("create:andesite_casing")},
             "pattern": [" T ", "BDB", "SAS"], "result": {"count": 1, "id": "create:schematicannon"}}})
# Protection Pixel: Exo-Plating também nas armaduras de netherite (a receita já consome item da etapa)
E["c25_eng"]["componente_sempre"] = True

# ============================================================ Simply More junto com o Simply Swords
PROF["ferreiro"]["namespaces"].append("simplymore")
E["fer_armas"].setdefault("resto_de", []).append("simplymore")

# ============================================================ Animus (Neo Vitae)
PROF["arcanista"]["namespaces"].append("animusnv")
pesquisa("animus_fundamentos", "Animus Foundations", "Animus: bloodwood, spears and the Animus guide",
         2, "animusnv:spear_iron", {"minecolonies:enchanter": 2}, {"minecraft:iron_ingot": 32, "minecraft:redstone": 32}, "fundacao_neovitae")
pesquisa("sigilos_animus", "Animus Sigils", "Animus: sigils, reagents and the Hellfire Forge reagent arts",
         3, "animusnv:sigil_storm", {"minecolonies:enchanter": 3}, {"minecraft:ender_pearl": 8, "minecraft:glowstone_dust": 32},
         "arranjos_vitae")
pesquisa("animus_senciente", "Sentient Animus", "Animus: sentient bow, shield, spear and scythe, Hand of Death and the sanguine dynamo",
         4, "animusnv:hand_of_death", {"minecolonies:enchanter": 4}, {"minecraft:diamond": 16, "minecraft:ghast_tear": 8},
         "arsenal_senciente", extra=["rituais_sangue"])
anim = sorted(i for i in ix.itens if i.startswith("animusnv:"))
def da(*pads):
    return [i for i in anim if any(re.fullmatch(p, i.split(":", 1)[1]) for p in pads)]
sigilos = da(r"sigil_.*", r"reagent.*", r"spear_bound.*", r"key_binding.*")
senciente = da(r"hand_of_death.*", r"hellforged_bow.*", r"sentient_.*", r"spear_sentient.*", r"runic_sentient_scythe.*",
               r"sanguine_(dynamo|rectifier|diviner|scroll_.*)", r"blood_infused_spellbook", r"blood_orb_transcendent")
maquina = {s for rid, r in ix.receitas.items() if r["pasta"] == "recipe" and not gerar.GRID.search(r["tipo"]) for s in r["saidas"]}
etapa("arc_animus", "Arcanist · Animus Foundations", "arcanista", "animus_fundamentos", {"minecolonies:enchanter": 2}, resto_de=["animusnv"])
etapa("arc_animus_sigilos", "Arcanist · Animus Sigils", "arcanista", "sigilos_animus", {"minecolonies:enchanter": 3},
      itens=sigilos, sem_vedacao=[i for i in sigilos if i in maquina])
etapa("arc_animus_senciente", "Arcanist · Sentient Animus", "arcanista", "animus_senciente", {"minecolonies:enchanter": 4},
      itens=senciente, sem_vedacao=[i for i in senciente if i in maquina])

# ============================================================ Relics: replicação (fim de jogo)
reliquias = [f"relics:{x}" for x in ("chef_hat", "chorus_staff", "clot_of_time", "cut_glass_boot", "experience_disperser", "ghostly_mantle",
                                     "glitchy_mantle", "hunting_belt", "jellyfish_necklace", "kinetic_belt", "leafy_mantle", "midnight_mantle",
                                     "piglin_mask", "reflective_necklace", "rider_flute", "ring_of_the_seven_deadly_sins", "roller_skate",
                                     "shield_of_retaliation", "sphere_of_self_sacrifice", "springy_boot")]
existe(*reliquias)
pesquisa("replicacao_reliquias", "Relic Replication", "Duplicate Relics in the Hephaestus Forge around a Replication Matrix",
         5, "relics:ring_of_the_seven_deadly_sins", {"minecolonies:blacksmith": 5, "minecolonies:enchanter": 4},
         {"minecraft:nether_star": 2, "minecraft:echo_shard": 16, "minecraft:diamond_block": 4}, "armaria_lendaria", extra=["reliquias_mana"])
etapa("fer_replicacao", "Blacksmith · Relic Replication", "ferreiro", "replicacao_reliquias", {"minecolonies:blacksmith": 5, "minecolonies:enchanter": 4})
cfg["componentes"].append({"id": "replication_matrix", "nome": "Replication Matrix", "forma": "cristal", "cor": "#C9A0FF",
                           "licencas": ["fer_replicacao"], "quantidade": 1, "descricao": "Echoes the essence of a relic into a second vessel",
                           "ingredientes": existe("minecraft:nether_star", "minecraft:echo_shard", "minecraft:echo_shard", "minecraft:amethyst_block",
                                                  "minecraft:diamond_block")})
for r in reliquias:
    nome = r.split(":", 1)[1]
    cfg["rituais_novos"].append({"id": f"relics/duplicar_{nome}", "json": {
        "essences": {"aureal": 4000, "blood": 6000, "experience": 2500, "souls": 40}, "forge_tier": 5,
        "inputs": [{"amount": 1, "ingredient": "@comp:replication_matrix"}, {"amount": 2, "ingredient": I("minecraft:echo_shard")},
                   {"amount": 2, "ingredient": I("minecraft:diamond_block")}, {"amount": 1, "ingredient": I("minecraft:nether_star")}],
        "magic_circle": "forbidden_arcanus:create_item", "main_ingredient": I(r),
        "result": {"type": "forbidden_arcanus:create_item", "result_item": {"count": 2, "id": r}}}})

# ============================================================ Vagabundo: carta para escolher a profissão + kits iniciais
cfg["itens_especiais"].append({"id": "guild_letter", "nome": "Letter of Introduction", "forma": "runa", "cor": "#C8B27A",
                               "descricao": "Right-click to choose your guild and starting craft"})
cfg["receitas_novas"].append({"id": "smpprofissoes:especial/guild_letter", "_comentario": "Carta de Apresentação (Vagabundo perdeu a sua).",
                              "json": {"type": "minecraft:crafting_shapeless", "category": "misc",
                                       "ingredients": [I("minecraft:paper"), I("minecraft:ink_sac"), I("minecraft:feather")],
                                       "result": {"count": 1, "id": "smpprofissoes:guild_letter"}}})
comum = [["minecraft:bread", 16], ["minecraft:torch", 32]]
kits = {
    "eng_create": [["create:wrench", 1], ["create:goggles", 1], ["create:hand_crank", 1], ["create:andesite_alloy", 16], ["create:cogwheel", 8], ["create:shaft", 16]],
    "eng_ie": [["immersiveengineering:manual", 1], ["immersiveengineering:hammer", 1], ["immersiveengineering:screwdriver", 1],
               ["immersiveengineering:wirecutter", 1], ["immersiveengineering:treated_wood_horizontal", 32]],
    "eng_embers": [["embers:tinker_hammer", 1], ["embers:caminite_blend", 32], ["embers:ember_crystal", 4]],
    "arc_irons": [["irons_spellbooks:copper_spell_book", 1], ["irons_spellbooks:arcane_essence", 8], ["irons_spellbooks:blank_rune", 2]],
    "arc_malum": [["malum:encyclopedia_arcana", 1], ["malum:crude_scythe", 1], ["malum:runewood_sapling", 4]],
    "arc_botania": [["botania:lexicon", 1], ["botania:twig_wand", 1], ["botania:white_mystical_flower", 8], ["botania:fertilizer", 8]],
    "arc_neovitae": [["neovitae:guide_book", 1], ["neovitae:throwing_dagger", 8]],
    "fer_silentgear": [["silentgear:guide_book", 1], ["silentgear:blueprint_paper", 16], ["silentgear:template_board", 8],
                       ["productivemetalworks:casting_table", 1], ["minecraft:iron_ingot", 16]],
    "fer_arcanus": [["forbidden_arcanus:arcane_crystal", 8], ["forbidden_arcanus:mundabitur_dust", 4], ["minecraft:iron_ingot", 16]],
    "nat_cozinha": [["farmersdelight:flint_knife", 1], ["farmersdelight:cooking_pot", 1], ["farmersdelight:cabbage_seeds", 8],
                    ["farmersdelight:tomato_seeds", 8], ["farmersdelight:rice", 8], ["farm_and_charm:lettuce_seeds", 8]],
    "nat_ervas": [["vital_herbs:teapot", 1], ["herbalbrews:tea_kettle", 1], ["vital_herbs:sooth_blossom_seeds", 8],
                  ["vital_herbs:snap_pepper_seeds", 8], ["vital_herbs:bleeding_heart_seeds", 8]],
}
for p in PROF.values():
    for op in p.get("inicios", []):
        kit = kits.get(op["licenca"], []) + comum
        existe(*[i for i, _ in kit])
        op["kit"] = kit

SRC.write_text(json.dumps(cfg, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(f"v15 aplicada: {len(fora)} receitas removidas; {len(sigilos)} itens em Animus Sigils, {len(senciente)} em Sentient Animus; "
      f"{len(reliquias)} relíquias com replicação")
