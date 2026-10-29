"""Migração v11 (26/10/2026): lista da rodada 4 (ver smp-profissoes/PENDENCIAS_R4.md). Idempotente.
Depois desta, rode tools/migracoes/corrigir_adiantados.py (regra: nada liberado antes de existir a forma de fazê-lo).
"""
import json
import sys
import zipfile
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[2]
SRC = RAIZ / "progressao.json"
sys.path.insert(0, str(RAIZ / "tools"))
import gerar  # noqa: E402

cfg = json.loads(SRC.read_text(encoding="utf-8"))
if cfg.get("_v11"):
    raise SystemExit("já migrado (v11)")
cfg["_v11"] = True
ix = gerar.indexar()
com_bancada = {s for r in ix.receitas.values() if r["pasta"] == "recipe" and gerar.GRID.search(r["tipo"]) for s in r["saidas"]}

O = "smpprofissoes:oficios/"
P = {p["id"].split("/", 1)[1]: p for p in cfg["pesquisas"]}
E = {e["licenca"]: e for e in cfg["etapas"]}
PROF = cfg["profissoes"]
C = {c["id"]: c for c in cfg["componentes"]}
I = lambda i: {"item": i}


def pesquisa(id_, nome, sub, nivel, icone, edificios, custos, pai, extra=(), depois_de=None, pula=False):
    p = {"id": O + id_, "nome": nome, "subtitulo": sub, "nivel": nivel, "icone": icone, "edificios": edificios, "custos": custos, "pai": O + pai}
    if extra:
        p["pesquisas_extra"] = [O + x for x in extra]
    if pula:
        p["pula_nivel"] = True
    lst = cfg["pesquisas"]
    i = next(k for k, x in enumerate(lst) if x["id"] == O + (depois_de or pai))
    lst.insert(i + 1, p)
    P[id_] = p
    return p


def remover_pesquisa(id_):
    cfg["pesquisas"] = [p for p in cfg["pesquisas"] if p["id"] != O + id_]
    P.pop(id_, None)


def etapa(lic, nome, prof, pesq, itens=(), resto=(), **extra):
    e = {"licenca": lic, "nome": nome, "profissao": prof, "pesquisas": [O + x for x in pesq], "edificios": {}, "itens": list(itens)}
    if resto:
        e["resto_de"] = list(resto)
    e.update(extra)
    cfg["etapas"].append(e)
    E[lic] = e
    return e


def remover_etapa(lic):
    cfg["etapas"] = [e for e in cfg["etapas"] if e["licenca"] != lic]
    return E.pop(lic)


def tirar(lic, *itens):
    E[lic]["itens"] = [i for i in E[lic]["itens"] if i not in itens]


def componente(cid, nome, forma, cor, lic, desc, ings, qtd=1):
    C[cid] = {"id": cid, "nome": nome, "forma": forma, "cor": cor, "licencas": [lic] if lic else [], "quantidade": qtd, "descricao": desc,
              "ingredientes": ings}
    cfg["componentes"] = [c for c in cfg["componentes"] if c["id"] != cid] + [C[cid]]


# ============================================================ ENGENHEIRO
# 1. ComputerCraft fora da progressão (a ponte Create <-> ComputerCraft fica livre)
PROF["engenheiro"]["namespaces"].remove("cccbridge")
E["eng_logistica"]["resto_de"].remove("cccbridge")
P["logistica_seguranca"]["subtitulo"] = "Create Security and SecurityCraft keypads, cameras and reinforced blocks"
# 2. Arcane Aeronautics -> Aeronautics Engineering
P["aeronautica_arcana"]["nome"] = "Aeronautics Engineering"
E["c1_eng"]["nome"] = "Engineer · Aeronautics Engineering"
# 4/5. Steam Armory: latão no nível 3; netherite no nível 4
PP = "protection_pixel:"
sa = P["armaria_vapor"]
sa.update(nivel=3, pai=O + "metalurgia_latao", subtitulo="Protection Pixel brass exoskeletons, armor plates, linings and arm cannons",
          edificios={"minecolonies:mechanic": 3}, custos={"create:brass_ingot": 48, "minecraft:iron_block": 8, "create:andesite_alloy": 32})
sa.pop("pesquisas_extra", None)
E["c13_eng"]["nome"] = "Engineer · Steam Armory"
pesquisa("armaria_netherite", "Netherite Exo-Armor", "Protection Pixel netherite-plated exoskeletons and overclocked mechanisms", 4,
         PP + "breakeras_chestplate", {"minecolonies:mechanic": 4, "minecolonies:blacksmith": 3},
         {"minecraft:netherite_ingot": 4, "create:precision_mechanism": 8, "create:brass_ingot": 32}, "armaria_vapor", extra=("metalurgia_precisao",))
etapa("c25_eng", "Engineer · Netherite Exo-Armor", "engenheiro", ["armaria_netherite"],
      [PP + "*as_(helmet|chestplate|leggings|boots)", PP + "smallnetheritesheet", PP + "heatoverlockingmechanism", PP + "incompleteheatoverlockmechanism"],
      sem_vedacao=[PP + "*"])
# 6. Torretas e minas do SecurityCraft em Immersive Arsenal; 7. mira de precisão também
SC = "securitycraft:"
armas_sc = [SC + "*mine*", SC + "claymore", SC + "bouncing_betty", SC + "ims", SC + "sentry", SC + "remote_access_sentry*"]
tirar("c2_eng", *armas_sc)
E["eng_arsenal_ie"]["itens"] += armas_sc + ["immersiveengineering:toolupgrade_railgun_scope"]
tirar("c15_eng", "immersiveengineering:toolupgrade_railgun_scope")
P["arsenal_ie"]["subtitulo"] = "Revolvers, chemical throwers, turrets, sentries, mines and the precision scope"
P["engenharia_balistica"]["subtitulo"] = "Tasers, traps, the trophy system and the teleport-blocking rift stabilizer"
# 8/9. Alchemy & Artifice (nível 3) com custo do próprio Embers e junto com a Ashen Regalia; 10. Ember Blade no 5 com o 4 vazio
aa = P["alquimia_artificios"]
aa.update(subtitulo="Embers alchemy, the dawnstone anvil, ashen armor, ember trinkets and augments",
          custos={"embers:archaic_brick": 32, "embers:dawnstone_ingot": 16, "embers:ember_crystal_cluster": 4, "embers:ancient_motive_core": 1})
reg = remover_etapa("c20_eng")
E["eng_alquimia_artificios"]["itens"] += reg["itens"]
E["eng_alquimia_artificios"]["componente_maquina"] = reg["componente_maquina"]
C["ashen_weave"]["licencas"] = ["eng_alquimia_artificios"]
remover_pesquisa("regalia_cinzenta")
lb = P["lamina_ember"]
lb.update(pai=O + "alquimia_artificios", pula_nivel=True, nivel=5)

# ============================================================ ARCANISTA
IR, HZ, ML, BO = "irons_spellbooks:", "hazennstuff:", "malum:", "botania:"
# 3. Arcane Essence mais difícil (só sai do Create Wizardry)
for m in cfg["receitas_modificadas"]:
    if m["id"] == "irons_spellbooks:mixing/arcane_essence":
        m["_comentario"] = "Arcane Essence no Create: superaquecida, 1 por mistura, e pede Glowstone e Mana Powder do Botania."
        m["operacoes"] = [{"op": "definir", "caminho": "results.0.count", "valor": 1},
                          {"op": "anexar", "caminho": "ingredients", "valor": I("minecraft:glowstone_dust")},
                          {"op": "anexar", "caminho": "ingredients", "valor": I(BO + "mana_powder")}]
cfg["receitas_modificadas"].append({"id": "create_wizardry:splashing/crushed_mithril", "caminho": "data/create_wizardry/recipe/splashing/crushed_mithril.json",
                                    "_comentario": "Arcane Essence ao lavar mithril: 1 com 25% de chance (era 3 com 75%).",
                                    "operacoes": [{"op": "definir", "caminho": "results.1", "valor": {"chance": 0.25, "count": 1, "id": IR + "arcane_essence"}}]})
# Megatocha numa pesquisa própria (nível 3) na linha do Warding Arts; receita da Shrine
pesquisa("megatocha", "Grand Wards", "The megatorch: no monster spawns in a huge radius", 3, "torchmaster:megatorch", {"minecolonies:enchanter": 3},
         {"minecraft:gold_block": 8, "minecraft:glowstone": 32, IR + "arcane_essence": 32}, "artes_protecao")
etapa("arc_megatocha", "Arcanist · Grand Wards", "arcanista", ["megatocha"], ["torchmaster:megatorch"])
cfg["receitas_novas"].append({"id": "smpprofissoes:legendaryshrines/shrine", "_comentario": "A Shrine não tinha receita.",
                              "json": {"type": "minecraft:crafting_shaped", "category": "misc",
                                       "key": {"G": I("minecraft:gold_block"), "A": I(IR + "arcane_essence"), "M": I("minecraft:amethyst_block"),
                                               "S": I("minecraft:chiseled_stone_bricks")},
                                       "pattern": ["GAG", "SMS", "SSS"], "result": {"count": 1, "id": "legendaryshrines:shrine"}}})
# Portal Frame usa a Singularity do Hazen
cfg["receitas_modificadas"].append({"id": IR + "portal_frame", "caminho": "data/irons_spellbooks/recipe/portal_frame.json",
                                    "_comentario": "Portal Frame: a Singularity do Hazen no centro.",
                                    "operacoes": [{"op": "definir", "caminho": "pattern.1", "valor": "ISI"},
                                                  {"op": "definir", "caminho": "key.S", "valor": I(HZ + "singularity")}]})
# Mithril e Mithril Weave liberados em Iron's Foundations
tirar("arc_maestria", IR + "mithril_weave", IR + "mithril_ingot")
# CrystalChronicles (addon do Iron's) na árvore
CC = "crystal_chronicles:"
PROF["arcanista"]["namespaces"].append("crystal_chronicles")
pesquisa("cronicas_cristal", "Crystal Chronicles", "Crystal shards, diffraction rings and holy beacons of the Crystal Chronicles", 2, CC + "diffraction_ring",
         {"minecolonies:enchanter": 2}, {IR + "arcane_essence": 24, "minecraft:amethyst_shard": 32}, "fundacao_irons", depois_de="vestes_arcanas")
etapa("arc_cristais", "Arcanist · Crystal Chronicles", "arcanista", ["cronicas_cristal"], resto=["crystal_chronicles"])
cav = [x for x in ix.itens if x.startswith(CC) and x.split("_")[-1] in ("helmet", "chestplate", "leggings", "boots") and "_knight_" in x]
pesquisa("cavaleiros_cristal", "Crystal Knights", "Elemental knight armor of the Crystal Chronicles", 3, CC + "holy_knight_chestplate",
         {"minecolonies:enchanter": 3}, {IR + "arcane_ingot": 16, "minecraft:diamond": 8}, "cronicas_cristal")
etapa("arc_cavaleiros", "Arcanist · Crystal Knights", "arcanista", ["cavaleiros_cristal"], sorted(cav), sem_vedacao=[CC + "*"])
armas_cc = [CC + x for x in ("blood_scythe", "ender_sickle", "ender_staff", "evocation_twinblade", "fire_chakram", "holy_sword", "holy_shield", "ice_hammer",
                              "lightning_bident", "lightning_staff", "nature_spear") if CC + x in ix.itens]
pesquisa("arsenal_cristal", "Crystal Arsenal", "Elemental weapons of the Crystal Chronicles", 3, CC + "lightning_bident", {"minecolonies:enchanter": 3},
         {IR + "arcane_ingot": 16, "minecraft:diamond": 8}, "cronicas_cristal", depois_de="cavaleiros_cristal")
etapa("arc_arsenal_cristal", "Arcanist · Crystal Arsenal", "arcanista", ["arsenal_cristal"], armas_cc, sem_vedacao=[CC + "*"])
# Blood Foundations sem Ghast Tear
P["fundacao_neovitae"]["custos"] = {"minecraft:redstone": 64, "minecraft:rotten_flesh": 32, "minecraft:bone": 16}
# Herbal Inks: tintas a partir da rara com plantas do Botania, Toxony e HerbalBrews (pigmentos feitos com a pesquisa)
componente("pigmento_raro", "Verdant Pigment", "frasco", "#3FA65A", "c4_arc", "Ground herbs that set rare ink",
           ["toxony:nightshade", "herbalbrews:green_tea_leaf", BO + "green_petal", "minecraft:glass_bottle"], 2)
componente("pigmento_epico", "Twilight Pigment", "frasco", "#7A4FC9", "c4_arc", "Petals and hemlock that deepen epic ink",
           ["toxony:moonlight_hemlock", "herbalbrews:lavender", BO + "purple_petal", BO + "mana_powder", "minecraft:glass_bottle"], 2)
c4 = C.pop("moonlit_ink_essence")
cfg["componentes"] = [c for c in cfg["componentes"] if c["id"] != "moonlit_ink_essence"]
componente("pigmento_lendario", "Moonlit Pigment", "frasco", "#6C7BE0", "c4_arc", "The rarest flowers, steeped for legendary ink",
           ["toxony:moonlight_hemlock", "toxony:moonlight_hemlock", "herbalbrews:hibiscus", BO + "mana_pearl", BO + "black_lotus", IR + "arcane_essence"])
for m in cfg["receitas_modificadas"]:
    if m["id"] == IR + "alchemist_cauldron/brew_rare_ink":
        m.update(_comentario="Rare Ink: Verdant Pigment (Toxony + HerbalBrews + Botania), feito com Herbal Inks.",
                 operacoes=[{"op": "definir", "caminho": "input", "valor": "@comp:pigmento_raro"}])
    if m["id"] == IR + "alchemist_cauldron/brew_epic_ink":
        m.update(_comentario="Epic Ink: Twilight Pigment (Toxony + HerbalBrews + Botania), feito com Herbal Inks.",
                 operacoes=[{"op": "definir", "caminho": "input", "valor": "@comp:pigmento_epico"}])
    if m["id"] == IR + "alchemist_cauldron/brew_legendary_ink":
        m["json"]["input"] = "@comp:pigmento_lendario"
P["tintas_herbais"].update(subtitulo="Rare, epic and legendary inks from Botania, Toxony and HerbalBrews plants",
                           custos={IR + "arcane_essence": 64, "toxony:nightshade": 16, "herbalbrews:lavender": 16, BO + "mana_powder": 16})
# Legendary Grimoires -> Legendary Artifacts (artefatos do Hazen com Deus Essence / Divine Mold, com Archmage Codex)
lg = P["grimorios_lendarios"]
lg.update(nome="Legendary Artifacts", icone=HZ + "singularity",
          subtitulo="Hazen artifacts forged with Deus Essence and the Divine Mold: legendary robes, crystals and the singularity")
lg["custos"] = {IR + "legendary_ink": 4, HZ + "deus_essence": 2, "minecraft:nether_star": 1}
artefatos = sorted({s for rid, r in ix.receitas.items() if r["pasta"] == "recipe" and rid.startswith("hazennstuff:")
                    and ({HZ + "deus_essence", HZ + "divine_mold"} & set(r["ent"])) for s in r["saidas"] if s.startswith(HZ)} - {HZ + "divine_mold"})
c16 = E["c16_arc"]
c16["nome"] = "Arcanist · Legendary Artifacts"
c16["itens"] = artefatos
c16["componente_maquina"] = {"id": "archmage_codex", "quantidade": 1}
E["c6_arc"]["excluir"] = artefatos
E["c6_arc"]["itens"] += [IR + "spellbreaker", IR + "boreal_blade", IR + "twilight_gale"]
for n in cfg["receitas_novas"]:  # as três espadas voltam a Spellforged Arms, sem o Codex
    if n["id"].startswith("smpprofissoes:spirit_infusion/"):
        n["json"]["extraInputs"] = [({"count": 1, "@comp": "arcane_inscription"} if e.get("@comp") == "archmage_codex" else e) for e in n["json"]["extraInputs"]]
        n["_comentario"] = "Spellforged Arms: Crimson Steel (Ferreiro) + Arcane Forge Inscription no Altar Espiritual."
C["archmage_codex"]["descricao"] = "Binds a spell into the mightiest artifacts and spell-casting weapons"
C["archmage_codex"]["ingredientes"] = ["@comp:arcane_inscription", IR + "legendary_ink", HZ + "deus_essence", IR + "divine_pearl", "minecraft:nether_star"]
C["archmage_codex"]["quantidade"] = 2
conv = cfg["conversoes_forja"][0]
conv["componente_especial"] = [{"padrao": "(deus_essence|divine_mold)", "componente": "archmage_codex", "tier": 5}]
# Malignant Arsenal: armadura Malignant + Tyrving; Malignant Core mais caro
mal = [ML + "malignant_stronghold_*"]
tirar("c14_arc", *mal)
E["c17_arc"]["itens"] += mal + [ML + "tyrving"]
P["arsenal_maligno"]["subtitulo"] = "Malignant stronghold armor, the Tyrving, Weight of Worlds and the void scepters"
C["malignant_core"]["ingredientes"] = [ML + "malignant_pewter_ingot", ML + "malignant_pewter_ingot", ML + "malignant_pewter_ingot", ML + "null_slate",
                                       ML + "null_slate", ML + "eldritch_spirit", ML + "eldritch_spirit", "minecraft:nether_star",
                                       "forbidden_arcanus:obsidiansteel_ingot"]
C["malignant_core"]["quantidade"] = 1
P["artes_vazio"]["subtitulo"] = "Malum's void rings, belts, necklaces, the void conduit and depot"
# Enigmatic Arcana com pesquisa própria
pesquisa("arcana_enigmatica", "Enigmatic Arcana", "Enigmatic Arcana's relics, built from spellcraft and spirit magic", 3, "enigmatic_arcana:enigmatic_amulet"
         if "enigmatic_arcana:enigmatic_amulet" in ix.itens else IR + "arcane_essence", {"minecolonies:enchanter": 3},
         {IR + "arcane_essence": 32, ML + "arcane_spirit": 16, "minecraft:amethyst_block": 4}, "grimorios_expandidos", extra=("infusao_espiritos",),
         depois_de="profundezas_eldritch")
E["arc_enigmatic"]["pesquisas"] = [O + "arcana_enigmatica"]
# Natural Alchemy & Runes: descrição clara
P["alquimia_natural_runas"]["subtitulo"] = "Botania's runic altar, runes, pylons and alchemy catalysts (the elven portal comes later, in Alfheim)"
# itens de terrasteel na pesquisa de Terrasteel
terra = [BO + x for x in ("terra_sword", "terra_axe", "terra_pick", "terraform_rod")]
tirar("c23_arc", *terra)
E["c8_arc"]["itens"] += [t for t in terra if t in ix.itens]
P["sintese_terrasteel"]["subtitulo"] = "The terra plate, terrasteel, terrasteel armor, the terra blade, pick and axe"

# ============================================================ FERREIRO
FA = "forbidden_arcanus:"
cfg["receitas_novas"].append({"id": "smpprofissoes:forbidden_arcanus/obsidiansteel_ingot", "_comentario": "Obsidiansteel na bancada (antes só no Clibano).",
                              "json": {"type": "minecraft:crafting_shaped", "category": "misc",
                                       "key": {"O": I("minecraft:obsidian"), "I": I("minecraft:iron_ingot"), "B": I("minecraft:blaze_powder")},
                                       "pattern": ["OIO", "IBI", "OIO"], "result": {"count": 1, "id": FA + "obsidiansteel_ingot"}}})
# Productive Metalworks: Foundry Foundations + Toolsmith Foundations; única pesquisa do PM = as 3 bobinas
sg = E["fer_silentgear"]
sg["resto_de"] = ["silentgear", "productivemetalworks"]
sg["nome"] = "Blacksmith · Toolsmith & Foundry Foundations"
P["fundacao_silentgear"].update(nome="Toolsmith & Foundry Foundations", subtitulo="Silent Gear tools and the Productive Metalworks foundry")
for lic in ("fer_pm", "fer_fundicao_aquecida", "fer_moldes", "fer_fundicao_engrenagens"):
    remover_etapa(lic)
for pid in ("fundacao_pm", "fundicao_aquecida", "arte_moldes", "fundicao_engrenagens"):
    remover_pesquisa(pid)
PROF["ferreiro"]["inicios"] = [op for op in PROF["ferreiro"]["inicios"] if op["licenca"] != "fer_pm"]
for op in PROF["ferreiro"]["inicios"]:
    if op["licenca"] == "fer_silentgear":
        op.update(rotulo="Silent Gear + Productive Metalworks", descricao="Ferramentas modulares e a fundição")
pesquisa("melhorias_fundicao", "Foundry Upgrades", "The three heating coils of the foundry and Silent Gear metalworks", 2,
         "productivemetalworks:powered_heating_coil", {"minecolonies:blacksmith": 2}, {"minecraft:copper_block": 8, "minecraft:redstone": 32},
         "fundacao_silentgear")
etapa("fer_fundicao", "Blacksmith · Foundry Upgrades", "ferreiro", ["melhorias_fundicao"], ["productivemetalworks:*heating_coil"], resto=["sgearmetalworks"])
for p in cfg["pesquisas"]:
    if O + "fundicao_engrenagens" in p.get("pesquisas_extra", []):
        p["pesquisas_extra"] = [O + "melhorias_fundicao" if x == O + "fundicao_engrenagens" else x for x in p["pesquisas_extra"]]

# ============================================================ GERAL
# Warden não dropa mais Echo Ingot: volta a tabela vanilla (o Create: Deep Dark a substitui)
with zipfile.ZipFile(gerar.MC_EXTRA) as zf:
    warden = json.loads(zf.read("data/minecraft/loot_table/entities/warden.json"))
cfg.setdefault("arquivos_dados", []).append({"caminho": "data/minecraft/loot_table/entities/warden.json", "json": warden,
                                             "_comentario": "Warden sem Echo Ingot (tabela vanilla; o Create: Deep Dark a substituía)."})
# itens especiais do mod: segunda profissão e esquecimento (receitas livres: qualquer jogador fabrica)
cfg["itens_especiais"] = [
    {"id": "guild_charter", "nome": "Guild Charter", "forma": "runa", "cor": "#D4A93A",
     "descricao": "Grants a second profession (one use; at most two professions)"},
    {"id": "draught_of_oblivion", "nome": "Draught of Oblivion", "forma": "frasco", "cor": "#4A6B8A",
     "descricao": "Forget every profession and choose again from the start"},
]
cfg["receitas_novas"] += [
    {"id": "smpprofissoes:especial/guild_charter", "_comentario": "Carta de guilda: difícil, pede o melhor de cada profissão.",
     "json": {"type": "minecraft:crafting_shaped", "category": "misc",
              "key": {"N": I("minecraft:nether_star"), "E": I("create:precision_mechanism"), "A": I(IR + "arcane_ingot"),
                      "F": I("silentgear:crimson_steel_ingot"), "T": I("toxony:toxin"), "B": I("minecraft:written_book"), "D": I("minecraft:diamond_block")},
              "pattern": ["EAF", "TNT", "DBD"], "result": {"count": 1, "id": "smpprofissoes:guild_charter"}}},
    {"id": "smpprofissoes:especial/draught_of_oblivion", "_comentario": "Esquecimento: Soul Crystal, Soul Dust e a Tincture of Forgetfulness.",
     "json": {"type": "minecraft:crafting_shapeless", "category": "misc",
              "ingredients": [I("deeperdarker:soul_crystal"), I("deeperdarker:soul_dust"), I("deeperdarker:soul_dust"), I(IR + "tincture_of_forgetfulness"),
                              I("minecraft:echo_shard"), I("minecraft:glass_bottle")],
              "result": {"count": 1, "id": "smpprofissoes:draught_of_oblivion"}}},
]
SRC.write_text(json.dumps(cfg, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print("progressao.json migrado para v11")
