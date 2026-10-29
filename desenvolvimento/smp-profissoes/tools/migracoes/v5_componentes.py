"""Migração v5 (25/10/2026) do progressao.json, pedida pelo usuário:
- pesquisas cruzadas (nível 4+) sem selos de papel: uma receita final com materiais diretos das profissões; nas receitas
  de máquina/ritual entra um componente (item do smpprofissoes) feito na bancada por quem tem a licença;
- licenças cruzadas que só serviam para fabricar selo são removidas;
- pesquisa de nível N não exige edifício acima do nível N.
Uso: python -I smp-profissoes/tools/migracoes/v5_componentes.py   (idempotente: não faz nada se já migrado)
"""
import json
from pathlib import Path

SRC = Path(__file__).resolve().parents[2] / "progressao.json"
cfg = json.loads(SRC.read_text(encoding="utf-8"))
if "componentes" in cfg:
    raise SystemExit("já migrado (v5)")

ITEM = lambda i: {"item": i}
COMPONENTES = [
    {"id": "runic_flux", "nome": "Runic Flux", "forma": "cristal", "cor": "#8E5BD6", "licencas": ["c3_fer"],
     "descricao": "Spirit-bound flux for runeforged alloys",
     "ingredientes": ["malum:soul_stained_steel_ingot", "malum:soul_stained_steel_ingot", "malum:arcane_spirit", "malum:arcane_spirit", "minecraft:netherite_scrap"]},
    {"id": "moonlit_ink_essence", "nome": "Moonlit Ink Essence", "forma": "frasco", "cor": "#6C7BE0", "licencas": ["c4_arc"],
     "descricao": "Distilled from moonlight hemlock; brews legendary ink",
     "ingredientes": ["toxony:moonlight_hemlock", "toxony:moonlight_hemlock", "botania:mana_pearl", "irons_spellbooks:arcane_essence",
                      "irons_spellbooks:arcane_essence", "minecraft:glow_ink_sac"]},
    {"id": "cursed_toxin", "nome": "Cursed Toxin", "forma": "frasco", "cor": "#5FA83A", "licencas": ["c5_nat"],
     "descricao": "Toxin steeped in wicked spirits",
     "ingredientes": ["toxony:toxin", "toxony:toxin", "malum:wicked_spirit", "malum:wicked_spirit", "malum:eldritch_spirit"]},
    {"id": "arcane_inscription", "nome": "Arcane Forge Inscription", "forma": "runa", "cor": "#3FA7E0", "licencas": ["c6_arc"],
     "descricao": "Spell lattice the Hephaestus Forge can bind into weapons",
     "ingredientes": ["irons_spellbooks:arcane_ingot", "irons_spellbooks:arcane_essence", "irons_spellbooks:arcane_essence",
                      "forbidden_arcanus:deorum_ingot", "forbidden_arcanus:arcane_crystal_dust"]},
    {"id": "stellar_core", "nome": "Stellar Core", "forma": "nucleo", "cor": "#F2E27A", "licencas": ["c7_fer"],
     "descricao": "Starforged heart for the Eternal Stella",
     "ingredientes": ["forbidden_arcanus:stellarite_piece", "forbidden_arcanus:stellarite_piece", "silentgear:tyrian_steel_ingot",
                      "minecraft:glowstone_dust", "minecraft:glowstone_dust"]},
    {"id": "terra_core", "nome": "Terra Core", "forma": "nucleo", "cor": "#4CC93C", "licencas": ["c8_arc"],
     "descricao": "Living root and alloy, ready for the terra plate",
     "ingredientes": ["vital_herbs:compacted_iron_root", "silentgear:azure_electrum_ingot", "silentgear:azure_electrum_ingot",
                      "botania:mana_diamond", "minecraft:moss_block"]},
    {"id": "ember_core", "nome": "Ember Core", "forma": "cristal", "cor": "#F08A2E", "licencas": ["c9_eng"],
     "descricao": "Condensed ember for the Tyrfing",
     "ingredientes": ["embers:ember_crystal", "embers:ember_crystal", "embers:dawnstone_ingot", "embers:dawnstone_ingot", "minecraft:blaze_powder"]},
    {"id": "alchemical_regulator", "nome": "Alchemical Regulator", "forma": "runa", "cor": "#B87333", "licencas": ["c10_nat"],
     "descricao": "Brass clockwork that meters toxins",
     "ingredientes": ["create:precision_mechanism", "create:electron_tube", "create:electron_tube", "toxony:toxin", "toxony:copper_crucible"]},
]
cfg["componentes"] = COMPONENTES
cfg.pop("selos", None)

# selo antigo -> o que entra no lugar (None = sai da receita)
TROCA = {
    "catalisador_levitacao": ITEM("botania:mana_pearl"),
    "liga_trilho": ITEM("silentgear:crimson_steel_ingot"),
    "catalisador_runico": "@comp:runic_flux",
    "catalisador_tinta": "@comp:moonlit_ink_essence",
    "catalisador_praga": "@comp:cursed_toxin",
    "catalisador_estelar": "@comp:stellar_core",
    "catalisador_terra": "@comp:terra_core",
    "catalisador_regalia": "@comp:alchemical_regulator",
    "catalisador_forja_arcana": "@comp:arcane_inscription",
    "seiva_verdejante": ITEM("vital_herbs:compacted_sooth_blossom"),
}
COMENTARIO = {
    "aeronautics:mixing/levitite_blend": "Arcane Aeronautics: Mana Pearl (Arcanista, Botania) direto na mistura; o End Stone Powder já é da etapa c1_eng.",
    "immersiveengineering:crafting/railgun": "Ballistic Engineering: Crimson Steel (Ferreiro) direto na receita; a bancada confere a licença c2_eng.",
}


def trocar(o):
    if isinstance(o, str) and o.startswith("@selo:"):
        return TROCA[o[6:]]
    if isinstance(o, dict):
        return {k: trocar(v) for k, v in o.items()}
    if isinstance(o, list):
        return [trocar(v) for v in o]
    return o


for r in cfg["receitas_modificadas"]:
    if r["id"] in COMENTARIO:
        r["_comentario"] = COMENTARIO[r["id"]]
    if "json" in r:
        r["json"] = trocar(r["json"])
    if "substituir_primeiro" in r:
        r["substituir_primeiro"]["por"] = trocar(r["substituir_primeiro"]["por"])
    if r["id"] == "forbidden_arcanus:hephaestus_forge/ritual/eternal_stella":
        r["json"]["inputs"] += [{"amount": 1, "ingredient": ITEM("botania:mana_diamond")},
                                {"amount": 1, "ingredient": ITEM("vital_herbs:compacted_glow_leaf")}]
        r["_comentario"] = "Eternal Stella: Stellar Core (Ferreiro, Stellar Forging) + Mana Diamond (Arcanista) + Compacted Glow Leaf (Naturalista)."
    if r["id"] == "embers:alchemy/tyrfing":
        r["json"]["inputs"].append("@comp:ember_core")
for r in cfg.get("rituais_novos", []):
    r["json"] = trocar(r["json"])
    r["json"]["inputs"] = [i for i in r["json"]["inputs"] if i.get("ingredient") is not None]

# licenças cruzadas que só fabricavam selo
SO_SELO = {"c1_arc", "c2_fer", "c3_arc", "c4_nat", "c5_arc", "c6_fer", "c6_nat", "c7_arc", "c7_nat", "c8_nat", "c8_fer", "c10_eng"}
for e in cfg["etapas"]:
    if e["licenca"] in SO_SELO and (e.get("itens") or e.get("resto_de")):
        raise SystemExit(f"{e['licenca']} libera itens; revise antes de remover")
cfg["etapas"] = [e for e in cfg["etapas"] if e["licenca"] not in SO_SELO]

# pesquisa de nível N não exige edifício acima de N (o mesmo vale para a etapa, pelo maior nível das pesquisas dela)
nivel = {p["id"]: p["nivel"] for p in cfg["pesquisas"]}
for p in cfg["pesquisas"]:
    p["edificios"] = {b: min(lv, p["nivel"]) for b, lv in p.get("edificios", {}).items()}
for e in cfg["etapas"]:
    teto = max(nivel[r] for r in e["pesquisas"])
    e["edificios"] = {b: min(lv, teto) for b, lv in e.get("edificios", {}).items()}

cfg["_comentario"] = ("Fonte única da progressão (v5). Edite aqui e rode tools/gerar.py. Cada profissão escolhe um mod inicial "
                      "('inicios'); todo o resto entra por etapa de pesquisa. Níveis 4 e 5 são pesquisas cruzadas: a receita final pede "
                      "materiais diretos de outras profissões; nas receitas de máquina/ritual entra um componente ('@comp:<id>', item "
                      "do mod smpprofissoes) que só quem tem a licença da pesquisa fabrica na bancada. Pesquisa de nível N não exige "
                      "edifício acima de N.")
SRC.write_text(json.dumps(cfg, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print("progressao.json migrado para v5")
