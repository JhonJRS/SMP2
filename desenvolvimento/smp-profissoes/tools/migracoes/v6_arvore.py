"""Migração v6 (25/10/2026): mudanças da árvore de pesquisas pedidas na lista de pendências.
Uso: python -I smp-profissoes/tools/migracoes/v6_arvore.py   (idempotente)
"""
import json
from pathlib import Path

SRC = Path(__file__).resolve().parents[2] / "progressao.json"
cfg = json.loads(SRC.read_text(encoding="utf-8"))
if any(p["id"].endswith("/profundezas_eldritch") for p in cfg["pesquisas"]):
    raise SystemExit("já migrado (v6)")

P = {p["id"].split("/", 1)[1]: p for p in cfg["pesquisas"]}
E = {e["licenca"]: e for e in cfg["etapas"]}
PROF = cfg["profissoes"]
O = "smpprofissoes:oficios/"


def pesquisa(id_, nome, sub, nivel, icone, edificios, custos, pai=None, extra=None, depois_de=None):
    p = {"id": O + id_, "nome": nome, "subtitulo": sub, "nivel": nivel, "icone": icone, "edificios": edificios, "custos": custos}
    if pai:
        p["pai"] = O + pai
    if extra:
        p["pesquisas_extra"] = [O + x for x in extra]
    lst = cfg["pesquisas"]
    i = next((k for k, x in enumerate(lst) if x["id"] == O + depois_de), len(lst) - 1) if depois_de else len(lst) - 1
    lst.insert(i + 1, p)
    P[id_] = p
    return p


def etapa(lic, nome, prof, pesquisas, edificios, itens=(), resto=(), **extra):
    e = {"licenca": lic, "nome": nome, "profissao": prof, "pesquisas": [O + x for x in pesquisas], "edificios": edificios,
         "itens": list(itens)}
    if resto:
        e["resto_de"] = list(resto)
    e.update(extra)
    cfg["etapas"].append(e)
    E[lic] = e
    return e


# ---- Arcanista / Iron's: Expanded Grimoires no nível 2; Eldritch Depths (Deep Darker + Eldritch) no nível 3
g = P["grimorios_expandidos"]
g.update(nivel=2, pai=O + "fundacao_irons", edificios={"minecolonies:enchanter": 2},
         custos={"irons_spellbooks:arcane_essence": 24, "minecraft:book": 16, "minecraft:amethyst_shard": 16})
E["arc_grimorios"]["edificios"] = {"minecolonies:enchanter": 2}
E["arc_grimorios"]["resto_de"] = [ns for ns in E["arc_grimorios"]["resto_de"] if ns != "darkermagic"]
pesquisa("profundezas_eldritch", "Eldritch Depths", "Deeper and Darker spellbooks and warden magic; requires relics of the Deep Dark", 3,
         "darkermagic:volume_of_the_deep", {"minecolonies:enchanter": 3},
         {"deeperdarker:soul_crystal": 4, "deeperdarker:sculk_bone": 16, "minecraft:echo_shard": 8, "irons_spellbooks:arcane_essence": 32},
         pai="grimorios_expandidos", depois_de="grimorios_expandidos")
etapa("arc_eldritch", "Arcanist · Eldritch Depths", "arcanista", ["profundezas_eldritch"], {"minecolonies:enchanter": 3}, resto=["darkermagic"])
P["armas_encantadas"]["pai"] = O + "profundezas_eldritch"

# ---- Engenheiro: Deep Dark Engineering exige materiais do Deep Dark
P["engenharia_deep_dark"]["custos"] = {"deeperdarker:sculk_bone": 16, "deeperdarker:soul_crystal": 4, "minecraft:echo_shard": 8, "create:brass_ingot": 32}

# ---- The Ember Blade na linha do Embers (nível 4, cruzada com o Ferreiro)
lb = P["lamina_ember"]
lb.update(nivel=4, pai=O + "alquimia_artificios", pesquisas_extra=[O + "ligas_runicas"],
          edificios={"minecolonies:mechanic": 4, "minecolonies:blacksmith": 4, "minecolonies:enchanter": 3})

# ---- Naturalista: Kitchen + Cultivation Foundations viram uma pesquisa
k = P["fundacao_cozinha"]
k.update(nome="Farmstead Foundations", subtitulo="Farmer's Delight and Farm & Charm: kitchens, crops, mills and silos",
         custos={"minecraft:wheat_seeds": 48, "minecraft:bone_meal": 32})
E["nat_cozinha"].update(nome="Naturalist · Farmstead Foundations", resto_de=["farmersdelight", "farm_and_charm"])
cfg["pesquisas"] = [p for p in cfg["pesquisas"] if p["id"] != O + "fundacao_cultivo"]
cfg["etapas"] = [e for e in cfg["etapas"] if e["licenca"] != "nat_cultivo"]
P["vinicultura"]["pai"] = O + "fundacao_cozinha"
PROF["naturalista"]["inicios"] = [op for op in PROF["naturalista"]["inicios"] if op["licenca"] != "nat_cultivo"]
for op in PROF["naturalista"]["inicios"]:
    if op["licenca"] == "nat_cozinha":
        op.update(rotulo="Farmer's Delight + Farm & Charm", descricao="Culinária, cultivos, moinhos e silos")

# ---- Create Wizardry: cruzada de nível 4 (Engenheiro + Arcanista)
PROF["engenheiro"]["namespaces"].append("create_wizardry")
pesquisa("engenharia_arcana", "Arcane Engineering", "Create Wizardry: mana pipes, blaze casters and arcane automation", 4,
         "create_wizardry:arcane_casing", {"minecolonies:mechanic": 4, "minecolonies:enchanter": 3},
         {"create:brass_ingot": 32, "irons_spellbooks:arcane_essence": 32, "minecraft:blaze_rod": 16},
         pai="combustiveis_industriais", extra=["maestria_arcana"], depois_de="aeronautica_arcana")
etapa("c11_eng", "Engineer · Arcane Engineering", "engenheiro", ["engenharia_arcana"], {}, resto=["create_wizardry"])

# ---- Protection Pixel: cruzada de nível 4 (Engenheiro + Ferreiro)
PROF["engenheiro"]["namespaces"].append("protection_pixel")
pesquisa("armaria_vapor", "Steam Armory", "Protection Pixel: steam exoskeletons, armor plates and arm cannons", 4,
         "protection_pixel:foldedsteamectoskeleton", {"minecolonies:mechanic": 4, "minecolonies:blacksmith": 3},
         {"create:brass_ingot": 48, "minecraft:iron_block": 8, "create:precision_mechanism": 4},
         pai="combustiveis_industriais", extra=["metalurgia_precisao"], depois_de="engenharia_arcana")
etapa("c13_eng", "Engineer · Steam Armory", "engenheiro", ["armaria_vapor"], {}, resto=["protection_pixel"])

# ---- SecurityCraft: básico em Logistics & Security; armas em Ballistic Engineering
PROF["engenheiro"]["namespaces"].append("securitycraft")
E["eng_logistica"]["resto_de"].append("securitycraft")
E["c2_eng"]["itens"] += ["securitycraft:*mine*", "securitycraft:claymore", "securitycraft:bouncing_betty", "securitycraft:ims",
                         "securitycraft:sentry", "securitycraft:remote_access_sentry*", "securitycraft:taser*",
                         "securitycraft:rift_stabilizer", "securitycraft:cage_trap", "securitycraft:floor_trap", "securitycraft:trophy_system"]
P["engenharia_balistica"]["subtitulo"] = "Railguns, sentries, mines and the teleport-blocking rift stabilizer"

# ---- Arcanista: Megatocha e Shrines (Warding Arts, nível 2)
PROF["arcanista"]["namespaces"] += ["torchmaster", "legendaryshrines"]
pesquisa("artes_protecao", "Warding Arts", "Mega torches, dread lamps and legendary shrines", 2, "torchmaster:megatorch",
         {"minecolonies:enchanter": 2}, {"minecraft:gold_block": 4, "minecraft:glowstone": 16, "irons_spellbooks:arcane_essence": 16},
         pai="fundacao_irons", depois_de="profundezas_eldritch")
etapa("arc_protecao", "Arcanist · Warding Arts", "arcanista", ["artes_protecao"], {"minecolonies:enchanter": 2}, resto=["torchmaster", "legendaryshrines"])

# ---- Neo Vitae: frascos alquímicos numa pesquisa dupla com o Naturalista (nível 2)
pesquisa("frascos_vitais", "Vital Flasks", "Neo Vitae alchemy flasks, brewed with the naturalists' herbs", 2, "neovitae:alchemy_flask",
         {"minecolonies:enchanter": 2, "minecolonies:plantation": 2}, {"minecraft:glass_bottle": 32, "minecraft:honey_bottle": 8, "minecraft:ghast_tear": 2},
         pai="fundacao_neovitae", extra=["fundacao_ervas"], depois_de="fundacao_neovitae")
etapa("c12_arc", "Arcanist · Vital Flasks", "arcanista", ["frascos_vitais"], {}, itens=["neovitae:alchemy_flask*"])

# ---- Malum: linha mais longa (Spirit Crucible no nível 3; Void Arts cruzada no nível 4)
E["arc_malum_infusao"]["itens"] += ["malum:soul_stained_steel_scythe", "malum:soul_stained_steel_helmet", "malum:soul_stained_steel_chestplate",
                                    "malum:soul_stained_steel_leggings", "malum:soul_stained_steel_boots", "malum:soul_hunter_*"]
pesquisa("cadinho_espiritual", "Spirit Crucible", "Malum's spirit crucible, catalyzers, impetus and ore nodes", 3, "malum:spirit_crucible",
         {"minecolonies:enchanter": 3}, {"malum:soul_stained_steel_ingot": 16, "malum:hallowed_gold_ingot": 8, "malum:earthen_spirit": 32},
         pai="infusao_espiritos", depois_de="rituais_convergencia")
etapa("arc_malum_cadinho", "Arcanist · Spirit Crucible", "arcanista", ["cadinho_espiritual"], {"minecolonies:enchanter": 3},
      itens=["malum:spirit_crucible", "malum:spirit_catalyzer", "malum:tuning_fork", "malum:*_impetus", "malum:alchemical_calx",
             "malum:catalyst_lobber", "malum:accelerating_inlay", "malum:prismatic_focus_lens"])
E["arc_malum_rituais"]["itens"] += ["malum:runic_workbench", "malum:rune_of_*", "malum:runic_sap*"]
pesquisa("artes_vazio", "Void Arts", "Malum's void relics, malignant armor and soul-forged greatweapons", 4, "malum:weight_of_worlds",
         {"minecolonies:enchanter": 4, "minecolonies:blacksmith": 3},
         {"malum:malignant_pewter_ingot": 8, "malum:void_salts": 16, "minecraft:netherite_scrap": 4},
         pai="rituais_convergencia", extra=["metalurgia_arcana"], depois_de="cadinho_espiritual")
etapa("c14_arc", "Arcanist · Void Arts", "arcanista", ["artes_vazio"], {},
      itens=["malum:weight_of_worlds", "malum:edge_of_deliverance", "malum:mnemonic_hex_staff", "malum:erosion_scepter",
             "malum:malignant_stronghold_*", "malum:ring_of_*", "malum:necklace_of_*", "malum:belt_of_*", "malum:void_conduit", "malum:void_depot"])

# ---- Simply Swords com o Ferreiro, na linha do Forbidden Arcanus
PROF["ferreiro"]["namespaces"].append("simplyswords")
pesquisa("armaria_marcial", "Martial Weaponsmithing", "Simply Swords: claymores, katanas, glaives and other weapon shapes", 2,
         "simplyswords:iron_claymore", {"minecolonies:blacksmith": 2}, {"minecraft:iron_ingot": 48, "minecraft:leather": 16},
         pai="fundacao_arcanus", depois_de="metalurgia_arcana")
etapa("fer_armas", "Blacksmith · Martial Weaponsmithing", "ferreiro", ["armaria_marcial"], {"minecolonies:blacksmith": 2}, resto=["simplyswords"])
pesquisa("forja_runica", "Runic Weaponsmithing", "Simply Swords runic forge, runic tablets and runefused gems", 3, "simplyswords:runic_tablet",
         {"minecolonies:blacksmith": 3}, {"forbidden_arcanus:arcane_crystal": 16, "minecraft:diamond": 8, "minecraft:lapis_block": 4},
         pai="armaria_marcial", extra=["metalurgia_arcana"], depois_de="armaria_marcial")
etapa("fer_runica", "Blacksmith · Runic Weaponsmithing", "ferreiro", ["forja_runica"], {"minecolonies:blacksmith": 3},
      itens=["simplyswords:runic_*", "simplyswords:runefused_gem", "simplyswords:netherfused_gem"])

SRC.write_text(json.dumps(cfg, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print("progressao.json migrado para v6")
