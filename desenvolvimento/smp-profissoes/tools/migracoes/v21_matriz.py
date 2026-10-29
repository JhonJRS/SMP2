"""Migração v21 (28/10/2026), rodada 11: Replication Matrix sem Trial Key e sem Relic Experience Bottle. Roda uma vez."""
import json
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[2]
SRC = RAIZ / "progressao.json"
sys.path.insert(0, str(RAIZ / "tools"))
import gerar  # noqa: E402

cfg = json.loads(SRC.read_text(encoding="utf-8"))
if cfg.get("_v21"):
    raise SystemExit("já migrado (v21)")
cfg["_v21"] = True
ix = gerar.indexar()
novos = ["malum:hallowed_gold_ingot", "malum:hallowed_gold_ingot", "minecraft:nautilus_shell",
         "botania:mana_diamond", "forbidden_arcanus:arcane_crystal", "minecraft:totem_of_undying"]
for i in novos:
    if i not in ix.itens:
        raise SystemExit(f"item inexistente: {i}")
next(c for c in cfg["componentes"] if c["id"] == "replication_matrix")["ingredientes"] = novos
SRC.write_text(json.dumps(cfg, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print("v21 aplicada")
