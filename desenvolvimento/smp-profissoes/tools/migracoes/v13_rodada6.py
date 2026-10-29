"""Migração v13 (26/10/2026): lista da rodada 6 (ver smp-profissoes/PENDENCIAS_R6.md). Roda uma vez.
Depois desta, rode tools/migracoes/corrigir_adiantados.py.
"""
import json
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[2]
SRC = RAIZ / "progressao.json"
sys.path.insert(0, str(RAIZ / "tools"))
import gerar  # noqa: E402

cfg = json.loads(SRC.read_text(encoding="utf-8"))
if cfg.get("_v13"):
    raise SystemExit("já migrado (v13)")
cfg["_v13"] = True
ix = gerar.indexar()

O = "smpprofissoes:oficios/"
P = {p["id"].split("/", 1)[1]: p for p in cfg["pesquisas"]}
E = {e["licenca"]: e for e in cfg["etapas"]}
I = lambda i: {"item": i}


def existe(*itens):
    for i in itens:
        if i not in ix.itens and not i.startswith("smpprofissoes:"):
            raise SystemExit(f"item inexistente: {i}")
    return list(itens)


def mover(i, de, para):
    """Mesmo efeito do corrigir_adiantados.mover para item explícito."""
    E[de]["itens"] = [x for x in E[de].get("itens", []) if x != i]
    E[de]["sem_vedacao"] = [x for x in E[de].get("sem_vedacao", []) if x != i]
    if i in E[para].get("excluir", []):
        E[para]["excluir"].remove(i)
    if i not in E[para]["itens"]:
        E[para]["itens"].append(i)


def componente(cid, nome, forma, cor, lic, desc, ings, qtd=1):
    existe(*[x for x in ings if not x.startswith("@")])
    cfg["componentes"] = [c for c in cfg["componentes"] if c["id"] != cid] + [
        {"id": cid, "nome": nome, "forma": forma, "cor": cor, "licencas": [lic], "quantidade": qtd, "descricao": desc, "ingredientes": ings}]


# ============================================================ 1. Singularity fora do Chorus Wine e do Teleportation Amulet
cfg["receitas_modificadas"] = [m for m in cfg["receitas_modificadas"]
                               if m["id"] not in ("vinery:wine_fermentation/chorus_wine", "irons_spellbooks:sequenced_assembly/teleportation_amulet")]
mover("vinery:chorus_wine", "nat_ritos", "nat_vinhos")

# ============================================================ 2. Singularity: sequência do Create com um item de fim de jogo de cada mod
passos = existe(
    "create:precision_mechanism",                 # Create (montagem sequencial)
    "immersiveengineering:component_electronic_adv",  # IE (bancada do engenheiro)
    "embers:wildfire_core",                       # Embers (mesa de alquimia)
    "irons_spellbooks:legendary_ink",             # Iron's (caldeirão do alquimista)
    "malum:malignant_pewter_ingot",               # Malum (altar espiritual)
    "botania:dragonstone",                        # Botania (portal dos elfos)
    "neovitae:hellforged_resonator",              # Neo Vitae (Hellfire Forge)
    "silentgear:tyrian_steel_ingot",              # Silent Gear (forja de ligas)
    "forbidden_arcanus:smelter_prism",            # Forbidden Arcanus (Forja de Hefesto)
    "toxony:lethal_dose",                         # Toxony (forja alquímica)
    "enchanted:soul_of_the_world",                # Enchanted (caldeirão da bruxa)
    "vinery:eiswein",                             # Vinery (barril de fermentação)
)
T = "create:incomplete_precision_mechanism"
for n in cfg["receitas_novas"]:
    if n["id"] == "smpprofissoes:ae2/singularity":
        n["_comentario"] = ("Singularity: montagem sequencial do Create com um item de fim de jogo de cada mod das profissões, "
                            "cada um feito na sua própria estação (o Condensador não a produz, ver CondensadorAE2Mixin).")
        n["json"] = {"type": "create:sequenced_assembly", "ingredient": I(existe("ae2:matter_ball")[0]), "loops": 1,
                     "results": [{"id": "ae2:singularity"}], "transitional_item": {"id": T},
                     "sequence": [{"type": "create:deploying", "ingredients": [I(T), I(p)], "results": [{"id": T}]} for p in passos]}
# a Singularity agora é de fim de jogo: vai para Quantum Networks (nível 5)
mover("ae2:singularity", "eng_ae2", "eng_ae2_quantico")
P["redes_me"]["subtitulo"] = "Applied Energistics 2: ME storage and autocrafting"
P["redes_quanticas"]["subtitulo"] = "Applied Energistics 2: the Singularity, quantum bridges and spatial storage"

# ============================================================ 3. Protection Pixel: nada sai das máquinas do Create sem a pesquisa
componente("exo_plating", "Netherite Exo-Plating", "lingote", "#4A3B3B", "c25_eng",
           "Riveted netherite plating for Protection Pixel exo-armor",
           ["minecraft:netherite_scrap", "create:brass_sheet", "create:brass_sheet", "create:precision_mechanism"])
componente("steam_rivets", "Steam-Forged Rivets", "lingote", "#B08D57", "c13_eng",
           "Brass rivets for Protection Pixel steam armor",
           ["create:brass_ingot", "create:brass_ingot", "create:andesite_alloy", "minecraft:iron_nugget"], qtd=2)
E["c25_eng"]["sem_vedacao"] = ["protection_pixel:smallnetheritesheet", "protection_pixel:incompleteheatoverlockmechanism"]
E["c25_eng"]["componente_maquina"] = {"id": "exo_plating", "quantidade": 1}
E["c13_eng"]["itens"] = ["protection_pixel:*"]  # antes era "resto" (sem vedação total)
E["c13_eng"].pop("resto_de", None)
E["c13_eng"]["componente_maquina"] = {"id": "steam_rivets", "quantidade": 1}
PROF = cfg["profissoes"]

# ============================================================ 4. Otherwhere Rites: nível 3, mais barata, Waystone com Teleposer Focus
P["ritos_outrolugar"].update({"nivel": 3, "pai": O + "bruxaria", "edificios": {"minecolonies:alchemist": 3},
                              "custos": {"minecraft:ender_pearl": 4, "minecraft:chorus_fruit": 8, "minecraft:amethyst_shard": 8},
                              "pesquisas_extra": [O + "caldeirao_bruxa", O + "poppets"]})
E["nat_ritos"]["edificios"] = {"minecolonies:alchemist": 3}
for m in cfg["receitas_modificadas"]:
    if m["id"] == "enchanted:waystone":
        m["_comentario"] = "Waystone: Teleposer Focus do Neo Vitae (teletransporte)."
        m["operacoes"] = [{"op": "anexar", "caminho": "ingredients", "valor": I(existe("neovitae:teleposer_focus")[0])}]

# ============================================================ 5. Megatocha com materiais mágicos do mesmo nível (Grand Wards, nível 3)
cfg["receitas_modificadas"] = [m for m in cfg["receitas_modificadas"] if m["id"] != "torchmaster:megatorch"]
cfg["receitas_modificadas"].append({
    "id": "torchmaster:megatorch", "caminho": "data/torchmaster/recipe/megatorch.json",
    "_comentario": "Megatorch: materiais mágicos de nível 2-3 (Botania, Malum, Iron's) no lugar de diamante e ouro.",
    "json": {"type": "minecraft:crafting_shaped", "pattern": ["RTR", "MLM", "HLH"],
             "key": {"T": I("minecraft:torch"), "R": I(existe("botania:rune_fire")[0]), "M": I(existe("botania:mana_diamond")[0]),
                     "L": I(existe("malum:runewood_log")[0]), "H": I(existe("malum:hallowed_gold_ingot")[0])},
             "result": {"id": "torchmaster:megatorch"}}})

# ============================================================ 6. Simply Bows (Ferreiro, Legendary Bowyery, nível 4)
arcos = {  # registro "simplybows:<x>/<x>"
    "vine_bow": [("minecraft:vine", 2), ("botania:rune_spring", 1), ("forbidden_arcanus:terrastomp_prism", 1)],
    "ice_bow": [("minecraft:blue_ice", 2), ("botania:rune_winter", 1), ("forbidden_arcanus:sea_prism", 1)],
    "bubble_bow": [("minecraft:prismarine_crystals", 2), ("botania:rune_water", 1), ("forbidden_arcanus:sea_prism", 1)],
    "bee_bow": [("minecraft:honeycomb", 2), ("minecraft:honey_block", 1), ("forbidden_arcanus:terrastomp_prism", 1)],
    "blossom_bow": [("minecraft:cherry_leaves", 2), ("minecraft:pink_petals", 2), ("forbidden_arcanus:whirlwind_prism", 1)],
    "earth_bow": [("minecraft:dripstone_block", 2), ("botania:rune_earth", 1), ("forbidden_arcanus:terrastomp_prism", 1)],
    "echo_bow": [("minecraft:echo_shard", 2), ("minecraft:sculk_catalyst", 1), ("forbidden_arcanus:whirlwind_prism", 1)],
    "cosmic_bow": [("forbidden_arcanus:stellarite_piece", 2), ("minecraft:nether_star", 1), ("botania:mana_diamond", 1)],
}
lst = cfg["pesquisas"]
i = max(k for k, x in enumerate(lst) if x["id"] == O + "armaria_distancia" or x.get("pai") == O + "armaria_distancia")
lst.insert(i + 1, {"id": O + "arcos_lendarios", "nome": "Legendary Bowyery", "subtitulo": "Simply Bows: elemental bows and bow upgrades",
                   "nivel": 4, "icone": "simplybows:echo_bow/echo_bow", "edificios": {"minecolonies:blacksmith": 4, "minecolonies:fletcher": 3},
                   "custos": {"minecraft:bow": 8, "minecraft:diamond": 8, "minecraft:string": 64}, "pai": O + "armaria_distancia",
                   "pesquisas_extra": [O + "forja_runica", O + "prismas"]})
melhorias = [f"simplybows:upgrades/{x}" for x in ("enchanted_bow_string", "reinforced_bow_frame", "rune_etching_pain",
                                                   "rune_etching_grace", "rune_etching_bounty", "rune_etching_chaos")]
itens_arcos = [f"simplybows:{a}/{a}" for a in arcos] + melhorias
existe(*itens_arcos)
cfg["etapas"].append({"licenca": "fer_arcos", "nome": "Blacksmith · Legendary Bowyery", "profissao": "ferreiro",
                      "pesquisas": [O + "arcos_lendarios"], "edificios": {"minecolonies:blacksmith": 4, "minecolonies:fletcher": 3},
                      "itens": [], "itens_avulsos": itens_arcos})
for a, tema in arcos.items():
    ins = [{"amount": n, "ingredient": I(existe(it)[0])} for it, n in tema]
    ins += [{"amount": 2, "ingredient": I("simplyswords:runefused_gem")}, {"amount": 1, "ingredient": I("minecraft:netherite_ingot")}]
    j = {"essences": {"aureal": 2400, "blood": 3600, "experience": 1500, "souls": 20}, "forge_tier": 4, "inputs": ins,
         "magic_circle": "forbidden_arcanus:create_item", "main_ingredient": I("minecraft:bow"),
         "result": {"type": "forbidden_arcanus:create_item", "result_item": {"count": 1, "id": f"simplybows:{a}/{a}"}}}
    cfg["rituais_novos"] = [r for r in cfg["rituais_novos"] if r["id"] != f"simplybows/{a}"] + [{"id": f"simplybows/{a}", "json": j}]
receitas_melhoria = {
    "enchanted_bow_string": ["minecraft:string", "minecraft:string", "minecraft:string", "botania:mana_string", "minecraft:lapis_lazuli"],
    "reinforced_bow_frame": ["minecraft:netherite_scrap", "minecraft:iron_ingot", "minecraft:iron_ingot", "silentgear:crimson_steel_ingot"],
    "rune_etching_pain": ["minecraft:paper", "simplyswords:runefused_gem", "minecraft:red_dye", "minecraft:redstone"],
    "rune_etching_grace": ["minecraft:paper", "simplyswords:runefused_gem", "minecraft:white_dye", "minecraft:glowstone_dust"],
    "rune_etching_bounty": ["minecraft:paper", "simplyswords:runefused_gem", "minecraft:yellow_dye", "minecraft:emerald"],
    "rune_etching_chaos": ["minecraft:paper", "simplyswords:runefused_gem", "minecraft:purple_dye", "minecraft:ender_eye"],
}
for x, ings in receitas_melhoria.items():
    existe(*ings)
    rid = f"smpprofissoes:simplybows/{x}"
    cfg["receitas_novas"] = [n for n in cfg["receitas_novas"] if n["id"] != rid] + [{
        "id": rid, "_comentario": "Simply Bows não tem receita: melhorias na bancada (Legendary Bowyery).",
        "json": {"type": "minecraft:crafting_shapeless", "category": "equipment", "ingredients": [I(i) for i in ings],
                 "result": {"count": 1, "id": f"simplybows:upgrades/{x}"}}}]

SRC.write_text(json.dumps(cfg, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print("v13 aplicada")
