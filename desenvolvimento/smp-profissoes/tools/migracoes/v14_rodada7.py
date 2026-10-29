"""Migração v14 (26/10/2026): Singularity mais simples (ainda de fim de jogo); a sequência do Create passa a fazer um item
provisório (Convergence Matrix); receitas do Armourer's Workshop e do ComputerCraft (+ CCC Bridge) removidas. Roda uma vez.
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
if cfg.get("_v14"):
    raise SystemExit("já migrado (v14)")
cfg["_v14"] = True
ix = gerar.indexar()
I = lambda i: {"item": i}
O = "smpprofissoes:oficios/"

# ============================================================ item provisório no lugar da sequência antiga
cfg["itens_especiais"].append({"id": "convergence_matrix", "nome": "Convergence Matrix", "forma": "nucleo", "cor": "#7F5AF0",
                               "descricao": "Work in progress: the fruit of every guild's finest craft"})
for n in cfg["receitas_novas"]:
    if n["id"] == "smpprofissoes:ae2/singularity":
        n["id"] = "smpprofissoes:convergencia/convergence_matrix"
        n["_comentario"] = ("Item provisório (Convergence Matrix): a montagem sequencial do Create com um item de fim de jogo de cada mod, "
                            "cada um da sua estação. Será desenvolvido depois.")
        n["json"]["results"] = [{"id": "smpprofissoes:convergence_matrix"}]

# ============================================================ Singularity: bancada, fim de jogo mas sem exagero
ings = {"T": "botania:terrasteel_ingot",                   # Arcanista, Terrasteel Synthesis (n4)
        "S": "silentgear:tyrian_steel_ingot",              # Ferreiro, Runeforged Alloys (n4)
        "L": "toxony:lethal_dose",                         # Naturalista, Venomous Alchemy (n4)
        "H": "protection_pixel:heatoverlockingmechanism",  # Engenheiro, Netherite Exo-Armor (n4)
        "M": "ae2:matter_ball", "N": "minecraft:nether_star"}
for i in ings.values():
    if i not in ix.itens:
        raise SystemExit(f"item inexistente: {i}")
cfg["receitas_novas"].append({"id": "smpprofissoes:ae2/singularity",
                              "_comentario": "Singularity: um item de nível 4 de cada profissão + Nether Star (o Condensador não a faz).",
                              "json": {"type": "minecraft:crafting_shaped", "category": "misc", "key": {k: I(v) for k, v in ings.items()},
                                       "pattern": ["TMS", "MNM", "LMH"], "result": {"count": 1, "id": "ae2:singularity"}}})
# Quantum Networks volta ao requisito cruzado original; o corretor põe o que a receita nova exige
for p in cfg["pesquisas"]:
    if p["id"] == O + "redes_quanticas":
        p["pesquisas_extra"] = [O + "engenharia_arcana"]

# ============================================================ sem receitas: Armourer's Workshop, ComputerCraft e addons
ns_fora = ("armourers_workshop", "computercraft", "cccbridge")
fora = sorted(rid for rid, r in ix.receitas.items() if r["pasta"] == "recipe" and rid.split(":", 1)[0] in ns_fora)
cfg["receitas_removidas"] = sorted(set(cfg["receitas_removidas"]) | set(fora))

SRC.write_text(json.dumps(cfg, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(f"v14 aplicada: {len(fora)} receitas removidas ({', '.join(ns_fora)})")
