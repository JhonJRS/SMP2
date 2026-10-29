"""Migração v6d (25/10/2026): ajustes revelados quando o validador parou de aceitar receitas-laço como rota.
- frascos lingering/throwable do Neo Vitae saem da etapa (são transformações do frasco base, que já exige a pesquisa);
- template de Silver Steel do Toxony ganha receita de bancada (a única era a de duplicar, e itens de etapa saem dos baús).
Idempotente."""
import json
from pathlib import Path

SRC = Path(__file__).resolve().parents[2] / "progressao.json"
cfg = json.loads(SRC.read_text(encoding="utf-8"))
for e in cfg["etapas"]:
    if e["licenca"] == "c12_arc":
        e["itens"] = ["neovitae:alchemy_flask"]
subs = cfg.setdefault("receitas_substitutas", [])
rid = "smpprofissoes:substituta/silver_steel_upgrade_smithing_template"
if not any(s["id"] == rid for s in subs):
    subs.append({"id": rid, "_comentario": "Primeiro template de Silver Steel (a receita do Toxony só duplica um template existente).",
                 "json": {"type": "minecraft:crafting_shaped", "category": "misc",
                          "key": {"D": {"item": "minecraft:diamond"}, "S": {"item": "toxony:silver_steel_ingot"},
                                  "C": {"item": "minecraft:cobbled_deepslate"}},
                          "pattern": ["DSD", "DCD", "DDD"],
                          "result": {"count": 1, "id": "toxony:silver_steel_upgrade_smithing_template"}}})
SRC.write_text(json.dumps(cfg, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print("v6d aplicada")
