"""Migração v17 (28/10/2026), rodada 9: Replication Matrix com receita própria (nada do que os rituais de duplicação
na Forja de Hefesto pedem: eco, bloco de diamante e estrela do Nether). Roda uma vez."""
import json
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[2]
SRC = RAIZ / "progressao.json"
sys.path.insert(0, str(RAIZ / "tools"))
import gerar  # noqa: E402

cfg = json.loads(SRC.read_text(encoding="utf-8"))
if cfg.get("_v17"):
    raise SystemExit("já migrado (v17)")
cfg["_v17"] = True
ix = gerar.indexar()

novos = ["relics:relic_experience_bottle", "relics:relic_experience_bottle", "minecraft:heavy_core", "minecraft:ominous_trial_key",
         "botania:gaia_ingot", "forbidden_arcanus:dark_matter", "minecraft:totem_of_undying"]
for i in novos:
    if i not in ix.itens:
        raise SystemExit(f"item inexistente: {i}")
comp = next(c for c in cfg["componentes"] if c["id"] == "replication_matrix")
comp["ingredientes"] = novos
comp["descricao"] = "Holds a relic's earned essence so the Forge can echo it into a second vessel"

SRC.write_text(json.dumps(cfg, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print("v17 aplicada: Replication Matrix com", len(novos), "ingredientes novos")
