"""Migração v12 (26/10/2026): lista da rodada 5 (ver smp-profissoes/PENDENCIAS_R5.md). Roda uma vez.
Depois desta, rode tools/migracoes/corrigir_adiantados.py (regra: nada liberado antes de existir a forma de fazê-lo).
"""
import json
import re
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[2]
SRC = RAIZ / "progressao.json"
sys.path.insert(0, str(RAIZ / "tools"))
import gerar  # noqa: E402

cfg = json.loads(SRC.read_text(encoding="utf-8"))
if cfg.get("_v12"):
    raise SystemExit("já migrado (v12)")
cfg["_v12"] = True
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


def saidas_de(tipos, ns):
    """Itens do namespace produzidos por receitas desses tipos."""
    return sorted({s for r in ix.receitas.values() if r["pasta"] == "recipe" and r["tipo"] in tipos
                   for s in r["saidas"] if s.startswith(ns + ":")})


def pesquisa(id_, nome, sub, nivel, icone, edificios, custos, pai, extra=()):
    existe(icone, *custos)
    p = {"id": O + id_, "nome": nome, "subtitulo": sub, "nivel": nivel, "icone": icone, "edificios": edificios, "custos": custos, "pai": O + pai}
    if extra:
        p["pesquisas_extra"] = [O + x for x in extra]
    lst = cfg["pesquisas"]
    i = max(k for k, x in enumerate(lst) if x["id"] == O + pai or x.get("pai") == O + pai)
    lst.insert(i + 1, p)
    P[id_] = p
    return p


def etapa(lic, nome, prof, pesq, edificios, itens=(), resto=(), **extra):
    e = {"licenca": lic, "nome": nome, "profissao": prof, "pesquisas": [O + pesq], "edificios": dict(edificios), "itens": list(itens)}
    if resto:
        e["resto_de"] = list(resto)
    e.update(extra)
    cfg["etapas"].append(e)
    E[lic] = e
    return e


def modificar(rid, caminho, comentario, operacoes):
    cfg["receitas_modificadas"] = [m for m in cfg["receitas_modificadas"] if m["id"] != rid]
    cfg["receitas_modificadas"].append({"id": rid, "caminho": caminho, "_comentario": comentario, "operacoes": operacoes})


def receita_nova(rid, comentario, j):
    cfg["receitas_novas"] = [n for n in cfg["receitas_novas"] if n["id"] != rid]
    cfg["receitas_novas"].append({"id": rid, "_comentario": comentario, "json": j})


def estacao(bloco, licencas, comentario):
    existe(bloco)
    cfg["estacoes_exclusivas"] = [s for s in cfg["estacoes_exclusivas"] if s["bloco"] != bloco]
    cfg["estacoes_exclusivas"].append({"bloco": bloco, "licencas": licencas, "_comentario": comentario})


def ritual(rid, principal, entradas, saida, tier=5):
    """entradas: [(item ou @comp:id, quantidade)]"""
    ins = []
    for it, n in entradas:
        ins.append({"amount": n, "ingredient": it if it.startswith("@comp:") else I(existe(it)[0])})
    essencias = {5: {"aureal": 4000, "blood": 6000, "experience": 2500, "souls": 40}}[tier]
    j = {"essences": essencias, "forge_tier": tier, "inputs": ins, "magic_circle": "forbidden_arcanus:create_item",
         "main_ingredient": I(existe(principal)[0]),
         "result": {"type": "forbidden_arcanus:create_item", "result_item": {"count": 1, "id": existe(saida)[0]}}}
    cfg["rituais_novos"] = [r for r in cfg["rituais_novos"] if r["id"] != rid]
    cfg["rituais_novos"].append({"id": rid, "json": j})


# ============================================================ NATURALISTA: Enchanted (Witchcraft / "Witchery")
PROF["naturalista"]["namespaces"].append("enchanted")
pesquisa("bruxaria", "Hedge Witchcraft", "Enchanted: witch's altar, witch oven, distillery, ritual chalk and the witch's woods",
         2, "enchanted:altar", {"minecolonies:alchemist": 2}, {"minecraft:glass_bottle": 32, "minecraft:bone_meal": 32, "minecraft:redstone": 16},
         "fundacao_ervas")
pesquisa("caldeirao_bruxa", "Witch's Kettle", "Enchanted: the kettle and witch's cauldron, mutandis, ointments and brews",
         3, "enchanted:kettle", {"minecolonies:alchemist": 3}, {"minecraft:cauldron": 4, "minecraft:glowstone_dust": 32, "minecraft:nether_wart": 32},
         "bruxaria")
pesquisa("poppets", "Poppetry", "Enchanted: spinning wheel, poppets and voodoo protection",
         3, "enchanted:poppet", {"minecolonies:alchemist": 3}, {"minecraft:string": 32, "minecraft:white_wool": 16, "minecraft:bone": 32},
         "bruxaria")
pesquisa("ritos_outrolugar", "Otherwhere Rites", "Enchanted: waystones, otherwhere and nether chalk, the enchanted broomstick",
         4, "enchanted:waystone", {"minecolonies:alchemist": 4}, {"minecraft:ender_pearl": 16, "minecraft:chorus_fruit": 32, "minecraft:amethyst_shard": 32},
         "caldeirao_bruxa")
kettle_n4 = existe("enchanted:otherwhere_chalk", "enchanted:nether_chalk", "enchanted:spirit_of_otherwhere")
saidas_kettle = [i for i in saidas_de({"enchanted:kettle"}, "enchanted") if i not in kettle_n4]
etapa("nat_bruxaria", "Naturalist · Hedge Witchcraft", "naturalista", "bruxaria", {"minecolonies:alchemist": 2}, resto=["enchanted"])
etapa("nat_caldeirao", "Naturalist · Witch's Kettle", "naturalista", "caldeirao_bruxa", {"minecolonies:alchemist": 3},
      itens=existe("enchanted:kettle", "enchanted:witch_cauldron") + saidas_kettle, sem_vedacao=saidas_kettle)
poppets = sorted(i for i in ix.itens if i.startswith("enchanted:") and "poppet" in i)
saidas_roda = saidas_de({"enchanted:wheel"}, "enchanted")
etapa("nat_poppets", "Naturalist · Poppetry", "naturalista", "poppets", {"minecolonies:alchemist": 3},
      itens=existe("enchanted:spinning_wheel") + poppets, sem_vedacao=[i for i in poppets if i in saidas_roda])
etapa("nat_ritos", "Naturalist · Otherwhere Rites", "naturalista", "ritos_outrolugar", {"minecolonies:alchemist": 4},
      itens=existe("enchanted:waystone", "enchanted:bound_waystone", "enchanted:blooded_waystone", "enchanted:enchanted_broomstick",
                   "enchanted:infinity_egg") + kettle_n4, sem_vedacao=kettle_n4)
estacao("enchanted:kettle", ["nat_caldeirao"], "o caldeirão da bruxa é do Naturalista com Witch's Kettle")
estacao("enchanted:witch_cauldron", ["nat_caldeirao"], "o caldeirão da bruxa é do Naturalista com Witch's Kettle")
estacao("enchanted:spinning_wheel", ["nat_poppets"], "a roca dos poppets é do Naturalista com Poppetry")
for b in ("enchanted:distillery", "enchanted:witch_oven", "enchanted:altar"):
    estacao(b, ["nat_bruxaria"], "estações básicas do Enchanted: Naturalista com Hedge Witchcraft")

# ============================================================ NATURALISTA: Gourmet Cuisine libera os pratos (máquinas por operador)
basicos = {"farmersdelight:cooked_rice", "farmersdelight:tomato_sauce", "farmersdelight:bone_broth", "farmersdelight:dog_food",
           "farm_and_charm:yeast", "farm_and_charm:cooked_cod", "farm_and_charm:cooked_salmon"}
pratos = saidas_de({"farmersdelight:cooking"}, "farmersdelight") + saidas_de(
    {"farm_and_charm:pot_cooking", "farm_and_charm:roaster", "farm_and_charm:stove"}, "farm_and_charm")
banquetes = existe("farmersdelight:roast_chicken_block", "farmersdelight:honey_glazed_ham_block", "farmersdelight:shepherds_pie_block",
                   "farmersdelight:rice_roll_medley_block", "farmersdelight:stuffed_pumpkin_block")
gourmet = sorted(set(pratos + banquetes) - basicos)
E["nat_culinaria"]["itens"] = gourmet
E["nat_culinaria"]["sem_vedacao"] = gourmet  # pratos saem do Cooking Pot/fogão: a máquina confere o operador
E["nat_culinaria"]["edificios"] = {"minecolonies:farmer": 2}
P["culinaria"]["subtitulo"] = "Cooking Pot meals and feasts; Farm & Charm stove, roaster and pot dishes"

# ============================================================ FERREIRO: Cataclysm
pesquisa("forja_cataclismica", "Cataclysmic Smithing", "L_Ender's Cataclysm: black steel gear and the eyes that find the bosses",
         3, "cataclysm:black_steel_sword", {"minecolonies:blacksmith": 3}, {"minecraft:iron_block": 8, "minecraft:obsidian": 32, "minecraft:blaze_rod": 16},
         "armaria_marcial")
pesquisa("reliquias_cataclismo", "Boss Relic Armory", "L_Ender's Cataclysm: ignitium and cursium armor, the Incinerator, the Annihilator and other relics",
         4, "cataclysm:ignitium_chestplate", {"minecolonies:blacksmith": 4}, {"minecraft:netherite_scrap": 8, "minecraft:diamond_block": 2, "minecraft:ghast_tear": 8},
         "forja_cataclismica")
pesquisa("fusao_cataclismica", "Cataclysmic Fusion", "L_Ender's Cataclysm: the Mechanical Fusion Anvil and the fused boss weapons",
         5, "cataclysm:mechanical_fusion_anvil", {"minecolonies:blacksmith": 5, "minecolonies:mechanic": 4},
         {"minecraft:nether_star": 1, "minecraft:netherite_ingot": 4, "minecraft:echo_shard": 8}, "reliquias_cataclismo")
cat3 = existe(*[f"cataclysm:black_steel_{x}" for x in ("axe", "hoe", "pickaxe", "shovel", "sword", "targe")],
              "cataclysm:ancient_spear", "cataclysm:azure_sea_shield", "cataclysm:blazing_grips", "cataclysm:sticky_gloves",
              "cataclysm:belt_of_beginner", *[f"cataclysm:{x}_eye" for x in ("abyss", "cursed", "desert", "flame", "mech", "monstrous", "storm", "void")])
cat4 = existe("cataclysm:ignitium_upgrade_smithing_template", *[f"cataclysm:ignitium_{x}" for x in ("helmet", "chestplate", "leggings", "boots")],
              "cataclysm:cursium_upgrade_smithing_template", *[f"cataclysm:cursium_{x}" for x in ("helmet", "chestplate", "leggings", "boots")],
              "cataclysm:monstrous_helm", "cataclysm:bulwark_of_the_flame", "cataclysm:the_incinerator", "cataclysm:the_annihilator",
              "cataclysm:soul_render", "cataclysm:laser_gatling", "cataclysm:wither_assault_shoulder_weapon", "cataclysm:astrape",
              "cataclysm:ceraunus", "cataclysm:cursed_bow", "cataclysm:void_scatter_arrow", "cataclysm:netherite_effigy")
fusao = saidas_de({"cataclysm:weapon_fusion"}, "cataclysm")
etapa("fer_cataclismo", "Blacksmith · Cataclysmic Smithing", "ferreiro", "forja_cataclismica", {"minecolonies:blacksmith": 3}, itens_avulsos=cat3)
etapa("fer_reliquias", "Blacksmith · Boss Relic Armory", "ferreiro", "reliquias_cataclismo", {"minecolonies:blacksmith": 4}, itens_avulsos=cat4)
etapa("fer_fusao", "Blacksmith · Cataclysmic Fusion", "ferreiro", "fusao_cataclismica", {"minecolonies:blacksmith": 5, "minecolonies:mechanic": 4},
      itens_avulsos=existe("cataclysm:mechanical_fusion_anvil") + fusao, sem_vedacao=fusao)
estacao("cataclysm:mechanical_fusion_anvil", ["fer_fusao"], "fusão das armas de chefe: Ferreiro com Cataclysmic Fusion")

# ============================================================ ENGENHEIRO: Applied Energistics 2 (a partir do nível 4)
PROF["engenheiro"]["namespaces"].append("ae2")
pesquisa("redes_me", "ME Networks", "Applied Energistics 2: ME storage, autocrafting and the Singularity",
         4, "ae2:controller", {"minecolonies:mechanic": 4}, {"minecraft:quartz": 64, "minecraft:redstone_block": 8, "minecraft:diamond": 8},
         "logistica_seguranca")
pesquisa("redes_quanticas", "Quantum Networks", "Applied Energistics 2: quantum bridges and spatial storage",
         5, "ae2:quantum_ring", {"minecolonies:mechanic": 5}, {"minecraft:nether_star": 1, "minecraft:ender_eye": 16, "minecraft:diamond_block": 4},
         "redes_me")
prensas = existe(*[f"ae2:{x}_press" for x in ("calculation_processor", "engineering_processor", "logic_processor", "silicon")])
etapa("eng_ae2", "Engineer · ME Networks", "engenheiro", "redes_me", {"minecolonies:mechanic": 4}, itens=existe("ae2:singularity"),
      resto=["ae2"], saque_livre=prensas)
etapa("eng_ae2_quantico", "Engineer · Quantum Networks", "engenheiro", "redes_quanticas", {"minecolonies:mechanic": 5},
      itens=existe("ae2:quantum_ring", "ae2:quantum_link", "ae2:quantum_entangled_singularity") + ["ae2:spatial_*"])
receita_nova("smpprofissoes:ae2/singularity", "Singularity: um item de cada profissão (o Condensador não a produz mais, ver CondensadorAE2Mixin).",
             {"type": "minecraft:crafting_shaped", "category": "misc",
              "key": {"P": I("create:precision_mechanism"), "S": I("immersiveengineering:component_steel"), "A": I("irons_spellbooks:arcane_ingot"),
                      "M": I("botania:mana_diamond"), "N": I("minecraft:nether_star"), "C": I("silentgear:crimson_steel_ingot"),
                      "F": I("forbidden_arcanus:arcane_crystal"), "H": I("herbalbrews:herbal_infusion"), "V": I("vital_herbs:compacted_sooth_blossom")},
              "pattern": ["PSA", "MNC", "FHV"], "result": {"count": 1, "id": "ae2:singularity"}})
# a Singularity em todo item de teletransporte
modificar("irons_spellbooks:portal_frame", "data/irons_spellbooks/recipe/portal_frame.json", "Portal Frame: Singularity do AE2 no centro (teletransporte).",
          [{"op": "definir", "caminho": "pattern.1", "valor": "ISI"}, {"op": "definir", "caminho": "key.S", "valor": I("ae2:singularity")}])
modificar("irons_spellbooks:sequenced_assembly/teleportation_amulet", "data/irons_spellbooks/recipe/sequenced_assembly/teleportation_amulet.json",
          "Teleportation Amulet: um passo a mais com a Singularity do AE2 (teletransporte).",
          [{"op": "inserir", "caminho": "sequence", "indice": 1, "valor": {"type": "create:deploying",
            "ingredients": [I("minecraft:chain"), I("ae2:singularity")], "results": [{"id": "minecraft:chain"}]}}])
modificar("enchanted:waystone", "data/enchanted/recipe/waystone.json", "Waystone: Singularity do AE2 (teletransporte).",
          [{"op": "anexar", "caminho": "ingredients", "valor": I("ae2:singularity")}])
modificar("vinery:wine_fermentation/chorus_wine", "data/vinery/recipe/wine_fermentation/chorus_wine.json",
          "Chorus Wine dá teletransporte: fermenta com a Singularity do AE2.",
          [{"op": "anexar", "caminho": "ingredients", "valor": I("ae2:singularity")}])

# ============================================================ FORJA DE HEFESTO
# Sea Prism sem Heart of the Sea
modificar("forbidden_arcanus:hephaestus_forge/ritual/sea_prism", "data/forbidden_arcanus/forbidden_arcanus/hephaestus_forge/ritual/sea_prism.json",
          "Sea Prism sem Heart of the Sea: cristal arcano no centro e materiais do mar nos pedestais.",
          [{"op": "definir", "caminho": "main_ingredient", "valor": I("forbidden_arcanus:arcane_crystal_block")},
           {"op": "definir", "caminho": "inputs", "valor": [
               {"amount": 2, "ingredient": I("minecraft:prismarine_shard")}, {"amount": 2, "ingredient": I("minecraft:prismarine_crystals")},
               {"amount": 2, "ingredient": I("minecraft:nautilus_shell")}, {"amount": 2, "ingredient": I("minecraft:turtle_scute")}]}])
# prisma do elemento em armaduras e cajados feitos na forja
cfg["prismas_elementais"] = {
    "_comentario": "Armadura/cajado da Forja de Hefesto pede o prisma do seu elemento (nome do item e ingredientes, ex.: runas).",
    "saidas": r"(helmet|chestplate|leggings|boots|headgear|hat|hood|robe|staff|scepter|cane|wand)$",
    "elementos": [
        {"prisma": "forbidden_arcanus:smelter_prism", "padrao": r"fire|flame|blaze|pyro|infern|ember|magma|volcan|pyrium|burn|scorch|lava|cinder"},
        {"prisma": "forbidden_arcanus:terrastomp_prism", "padrao": r"nature|verdant|leaf|chloro|flora|mycel|blossom|druid|seed|vine|moss|bloom|lily|thorn|poison|acid|plant|herb|fung"},
        {"prisma": "forbidden_arcanus:whirlwind_prism", "padrao": r"lightning|thunder|storm|volt|electr|spark|shock|energi"},
        {"prisma": "forbidden_arcanus:sea_prism", "padrao": r"(?:^|[^a-z])ice(?:[^a-z]|$)|frost|glacial|snow|cryo|boreal|coral|(?:^|[^a-z])sea(?:[^a-z]|$)|aqua|water|tide|ocean|frozen|winter"},
    ]}
# Eternal Stella: Eternity Essence (Naturalista, Eternal Brews) no lugar da Compacted Glow Leaf
for m in cfg["receitas_modificadas"]:
    if m["id"] == "forbidden_arcanus:hephaestus_forge/ritual/eternal_stella":
        for ent in m["json"]["inputs"]:
            if ent["ingredient"] == I("vital_herbs:compacted_glow_leaf"):
                ent["ingredient"] = "@comp:eternity_essence"
        m["_comentario"] = "Eternal Stella: Stellar Core (Ferreiro) + Mana Diamond (Arcanista) + Eternity Essence (Naturalista, Eternal Brews)."
pesq_stella = E["c7_fer"]["pesquisas"][0].split("/", 1)[1]
P[pesq_stella].setdefault("pesquisas_extra", []).append(E["c19_nat"]["pesquisas"][0])

# Mythic Arms: mais armas, todas com Mythic Core
miticas = [  # (arma, base, ingredientes do tema)
    ("watcher_claymore", "simplyswords:netherite_claymore", [("malum:void_salts", 2)]),
    ("harbinger", "simplyswords:netherite_glaive", [("minecraft:wither_skeleton_skull", 1), ("malum:void_salts", 1)]),
    ("sunfire", "simplyswords:netherite_longsword", [("forbidden_arcanus:smelter_prism", 1), ("irons_spellbooks:fire_rune", 2)]),
    ("tempest", "simplyswords:netherite_warglaive", [("forbidden_arcanus:whirlwind_prism", 1), ("irons_spellbooks:lightning_rune", 2)]),
    ("thunderbrand", "simplyswords:netherite_greathammer", [("forbidden_arcanus:whirlwind_prism", 1), ("irons_spellbooks:lightning_rune", 2)]),
    ("stars_edge", "simplyswords:netherite_katana", [("forbidden_arcanus:stellarite_piece", 2)]),
    ("caelestis", "simplyswords:netherite_longsword", [("irons_spellbooks:holy_rune", 2)]),
    ("awakened_lichblade", "simplyswords:netherite_claymore", [("forbidden_arcanus:sea_prism", 1), ("irons_spellbooks:ice_rune", 2)]),
    ("livyatan", "simplyswords:netherite_scythe", [("forbidden_arcanus:sea_prism", 1), ("minecraft:heart_of_the_sea", 1)]),
    ("enigma", "simplyswords:netherite_rapier", [("irons_spellbooks:ender_rune", 2)]),
]
for nome, base, tema in miticas:
    ritual(f"simplyswords/{nome}", base, [("@comp:mythic_core", 1), ("simplyswords:runefused_gem", 2), ("minecraft:netherite_ingot", 1),
                                           ("minecraft:nether_star", 1)] + tema, f"simplyswords:{nome}")
    E["c22_fer"]["itens"].append(f"simplyswords:{nome}")
P["armas_miticas"]["subtitulo"] = "Mjolnir, The Devourer and ten more mythic blades, all forged around a Mythic Core"

# ============================================================ USO DO ASHEN WEAVE E DO SOUL THREAD
W, T = "@comp:ashen_weave", "@comp:soul_thread"
for item, ops in {
    "ashen_cloak": [{"op": "definir", "caminho": "key.D", "valor": W}],
    "ashen_goggles": [{"op": "definir", "caminho": "key.S", "valor": W}],
    "ashen_leggings": [{"op": "definir", "caminho": "pattern.0", "valor": "CWC"}, {"op": "definir", "caminho": "key.W", "valor": W}],
    "ashen_boots": [{"op": "definir", "caminho": "pattern.1", "valor": "W W"}, {"op": "definir", "caminho": "key.W", "valor": W}],
    "ashen_amulet": [{"op": "definir", "caminho": "key.C", "valor": W}],
}.items():
    modificar(f"embers:{item}", f"data/embers/recipe/{item}.json", "Armadura cinzenta: Ashen Weave (Alchemy & Artifice).", ops)
for item, ops in {
    "gilded_ring": [{"op": "definir", "caminho": "pattern.0", "valor": "#T "}, {"op": "definir", "caminho": "key.T", "valor": T}],
    "ornate_ring": [{"op": "definir", "caminho": "pattern.0", "valor": "#T "}, {"op": "definir", "caminho": "key.T", "valor": T}],
    "gilded_belt": [{"op": "definir", "caminho": "pattern.0", "valor": "XTX"}, {"op": "definir", "caminho": "key.T", "valor": T}],
    "ornate_necklace": [{"op": "definir", "caminho": "pattern.0", "valor": " T "}, {"op": "definir", "caminho": "key.T", "valor": T}],
    "elaborate_brooch": [{"op": "definir", "caminho": "pattern.0", "valor": " T "}, {"op": "definir", "caminho": "key.T", "valor": T}],
    "runic_brooch": [{"op": "definir", "caminho": "pattern.0", "valor": " T "}, {"op": "definir", "caminho": "key.T", "valor": T}],
    "soulwoven_pouch": [{"op": "definir", "caminho": "key.X", "valor": T}],
}.items():
    modificar(f"malum:{item}", f"data/malum/recipe/{item}.json", "Bijuteria tecida: Soul Thread (Soulwoven Arts).", ops)

# ============================================================ SHRINE mais difícil (vários mods mágicos, meio de jogo)
for n in cfg["receitas_novas"]:
    if n["id"] == "smpprofissoes:legendaryshrines/shrine":
        n["_comentario"] = "Shrine: um material de cada escola mágica (Botania, Iron's, Malum, Neo Vitae, Forbidden Arcanus)."
        n["json"] = {"type": "minecraft:crafting_shaped", "category": "misc",
                     "key": {"M": I("botania:manasteel_ingot"), "A": I("irons_spellbooks:arcane_ingot"), "H": I("malum:hallowed_gold_ingot"),
                             "R": I("minecraft:amethyst_block"), "C": I("forbidden_arcanus:arcane_crystal"), "B": I(existe("neovitae:blood_pearl")[0])},
                     "pattern": ["MAM", "HRH", "CBC"], "result": {"count": 1, "id": "legendaryshrines:shrine"}}

# ============================================================ CRYSTALCHRONICLES no Altar Espiritual do Malum
equip_cristal = sorted(i for lic in ("arc_cavaleiros", "arc_arsenal_cristal") for i in E[lic].get("itens", []) if i.startswith("crystal_chronicles:"))
cfg["conversoes_malum"] = [{
    "id": "cristal", "_comentario": "Armas e armaduras do CrystalChronicles saem do Altar Espiritual do Malum.",
    "saidas": "^(" + "|".join(re.escape(i) for i in equip_cristal) + ")$",
    "tipos": ["minecraft:crafting_shaped", "minecraft:crafting_shapeless", "minecraft:smithing_transform"],
    "espiritos_base": {"malum:arcane": 8},
    "espiritos_por_padrao": [
        {"padrao": "ice_shard|frost|snow|ice_", "espirito": "malum:aqueous", "quantidade": 8},
        {"padrao": "volcanite|fire|pyrium|blaze|magma", "espirito": "malum:infernal", "quantidade": 8},
        {"padrao": "voltite|lightning|storm|thunder", "espirito": "malum:aerial", "quantidade": 8},
        {"padrao": "hemalite|blood", "espirito": "malum:wicked", "quantidade": 8},
        {"padrao": "voidstone|void|ender", "espirito": "malum:eldritch", "quantidade": 8},
        {"padrao": "prismatic|holy|divine", "espirito": "malum:sacred", "quantidade": 8},
        {"padrao": "earth|terra|stone|nature", "espirito": "malum:earthen", "quantidade": 8},
    ]}]

SRC.write_text(json.dumps(cfg, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(f"v12 aplicada: {len(gourmet)} pratos em Gourmet Cuisine, {len(saidas_kettle)} itens do caldeirão, {len(poppets)} poppets, "
      f"{len(fusao)} armas de fusão, {len(equip_cristal)} equipamentos de cristal no altar do Malum, {len(miticas)} armas míticas novas")
