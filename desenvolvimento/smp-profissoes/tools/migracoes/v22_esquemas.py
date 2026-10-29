"""Migração v22 (28/10/2026): Schematic Engineering subiu para o nível 4 (v19) e nível 4+ exige pesquisa de outra
linha; entra Precision Metallurgy (Ferreiro). Roda uma vez."""
import json
from pathlib import Path

SRC = Path(__file__).resolve().parents[2] / "progressao.json"
cfg = json.loads(SRC.read_text(encoding="utf-8"))
if cfg.get("_v22"):
    raise SystemExit("já migrado (v22)")
cfg["_v22"] = True
P = {p["id"]: p for p in cfg["pesquisas"]}
p = P["smpprofissoes:oficios/engenharia_esquemas"]
extra = "smpprofissoes:oficios/metalurgia_precisao"
assert P[extra]["nivel"] < p["nivel"], (P[extra]["nivel"], p["nivel"])
p["pesquisas_extra"] = sorted(set(p.get("pesquisas_extra", [])) | {extra})
SRC.write_text(json.dumps(cfg, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print("v22 aplicada:", p["nome"], "nível", p["nivel"], "exige também", P[extra]["nome"], "nível", P[extra]["nivel"])
