"""Migração v8 (25/10/2026): Forja de Hefesto e Forbidden Arcanus.
- Hazen 'n Stuff: todas as armas, armaduras, cajados e livros com receita viram rituais da forja (conversão automática);
- Simply Swords: armas únicas ganham rituais com os prismas do Forbidden Arcanus; continuam no saque (tesouros);
- tiers da forja com função: 1-2 básicos/prismas/Quantum Catcher, 3 equipamento do Hazen, 4 upgrades e armas únicas,
  5 Eternal Stella e as armas lendárias;
- Whirlwind Prism (sem uso) passa a aplicar o modificador Magnetized.
Idempotente."""
import json
from pathlib import Path

SRC = Path(__file__).resolve().parents[2] / "progressao.json"
cfg = json.loads(SRC.read_text(encoding="utf-8"))
if cfg.get("conversoes_forja"):
    raise SystemExit("já migrado (v8)")

ESS = {"3": {"aureal": 1200, "blood": 1800, "experience": 800, "souls": 10},
       "4": {"aureal": 2400, "blood": 3600, "experience": 1500, "souls": 20},
       "5": {"aureal": 4000, "blood": 6000, "experience": 2500, "souls": 40}}
cfg["conversoes_forja"] = [{
    "id": "hazen",
    "_comentario": "Hazen 'n Stuff na Forja de Hefesto: a receita original (bancada/ferraria) vira ritual e é removida.",
    "saidas": r"^hazennstuff:(?!geckolib_)(?!legacy_).*(sword|staff|blade|axe|scythe|bow|spear|helmet|chestplate|leggings|boots|"
              r"wand|hammer|dagger|katana|mace|rapier|halberd|glaive|scepter|cane|headgear|mask|greatsword|spellbook)$",
    "tipos": ["minecraft:crafting_shaped", "minecraft:crafting_shapeless", "minecraft:smithing_transform"],
    "componente": "arcane_inscription",
    "tier_padrao": 3, "tier_smithing": 4,
    "tier_por_ingrediente": [{"padrao": "(nether_star|dragon|divine|ascended|cosmic|eldritch|abyssal|void|mithril_weave)", "tier": 4}],
    "essencias": ESS,
}]
for c in cfg["componentes"]:
    if c["id"] == "arcane_inscription":
        c["quantidade"] = 2

# Simply Swords: armas únicas (tesouros) ficam no saque; algumas ganham ritual
for e in cfg["etapas"]:
    if e["licenca"] == "fer_armas":
        e["saque_livre"] = ["simplyswords:*"]
I = lambda i: {"item": i}


def ritual(rid, tier, principal, entradas, saida):
    return {"id": f"simplyswords/{rid}", "json": {
        "essences": ESS[str(tier)], "forge_tier": tier,
        "inputs": [{"amount": n, "ingredient": I(i)} for i, n in entradas],
        "magic_circle": "forbidden_arcanus:create_item", "main_ingredient": I(principal),
        "result": {"type": "forbidden_arcanus:create_item", "result_item": {"count": 1, "id": saida}}}}


S, FA = "simplyswords:", "forbidden_arcanus:"
cfg["rituais_novos"] += [
    ritual("emberblade", 4, S + "diamond_longsword", [(FA + "smelter_prism", 1), ("minecraft:blaze_rod", 3), ("minecraft:magma_cream", 2), (S + "runefused_gem", 1)], S + "emberblade"),
    ritual("hearthflame", 4, S + "diamond_greataxe", [(FA + "smelter_prism", 1), ("minecraft:campfire", 2), ("minecraft:fire_charge", 2), (S + "netherfused_gem", 1)], S + "hearthflame"),
    ritual("frostfall", 4, S + "diamond_claymore", [(FA + "sea_prism", 1), ("minecraft:blue_ice", 3), ("minecraft:packed_ice", 2), (S + "runefused_gem", 1)], S + "frostfall"),
    ritual("stormbringer", 4, S + "diamond_greathammer", [(FA + "whirlwind_prism", 1), ("minecraft:lightning_rod", 3), ("minecraft:copper_block", 2), (S + "runefused_gem", 1)], S + "stormbringer"),
    ritual("bramblethorn", 4, S + "diamond_glaive", [(FA + "terrastomp_prism", 1), ("botania:livingwood_twig", 2), ("minecraft:rose_bush", 2), (S + "runefused_gem", 1)], S + "bramblethorn"),
    ritual("soulkeeper", 4, S + "diamond_twinblade", [(FA + "terrastomp_prism", 1), ("malum:soul_stained_steel_ingot", 2), ("minecraft:soul_lantern", 2), (S + "netherfused_gem", 1)], S + "soulkeeper"),
    ritual("mjolnir", 5, S + "diamond_greathammer", [(FA + "whirlwind_prism", 1), ("irons_spellbooks:lightning_rune", 2), ("minecraft:netherite_ingot", 1), ("minecraft:nether_star", 1), (S + "runefused_gem", 2)], S + "mjolnir"),
    ritual("the_devourer", 5, S + "diamond_scythe", [(S + "netherfused_gem", 2), ("minecraft:nether_star", 1), ("minecraft:netherite_ingot", 2), ("malum:void_salts", 2)], S + "the_devourer"),
]

# Eternal Stella no topo (tier 5)
for m in cfg["receitas_modificadas"]:
    if m["id"] == "forbidden_arcanus:hephaestus_forge/ritual/eternal_stella":
        m["json"]["forge_tier"] = 5

# Whirlwind Prism ganha uso: aplica Magnetized na mesa de ferraria
cfg["receitas_novas"].append({"id": "smpprofissoes:smithing/apply_magnetized_modifier", "_comentario": "Whirlwind Prism não tinha uso.",
                              "json": {"type": "forbidden_arcanus:apply_modifier", "addition": I(FA + "whirlwind_prism"),
                                       "modifier": "forbidden_arcanus:magnetized", "template": I(FA + "apply_modifier_smithing_template")}})
SRC.write_text(json.dumps(cfg, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print("progressao.json migrado para v8")
