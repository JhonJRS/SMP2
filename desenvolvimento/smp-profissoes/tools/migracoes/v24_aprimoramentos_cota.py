"""Migração v24 (28/10/2026), rodada 13: aprimoramentos de raridade refeitos. Qualquer um que pesquisou consegue fazer,
mas o mod limita a 2 de cada por jogador (Cotas.java) — então a receita pode ser acessível em vez de cara:
orbe-base + 2 tintas da raridade + um material que sobe com ela. Roda uma vez."""
import json
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[2]
SRC = RAIZ / "progressao.json"
sys.path.insert(0, str(RAIZ / "tools"))
import gerar  # noqa: E402

cfg = json.loads(SRC.read_text(encoding="utf-8"))
if cfg.get("_v24"):
    raise SystemExit("já migrado (v24)")
cfg["_v24"] = True
ix = gerar.indexar()

MATERIAL = {  # raridade -> material (2x)
    "common": "minecraft:lapis_lazuli",
    "uncommon": "minecraft:amethyst_shard",
    "rare": "minecraft:gold_ingot",
    "epic": "minecraft:diamond",
    "legendary": "minecraft:netherite_scrap",
}
for r, m in MATERIAL.items():
    if m not in ix.itens:
        raise SystemExit(f"item inexistente: {m}")
    rec = next(x for x in cfg["receitas_novas"] if x["id"] == f"smpprofissoes:especial/{r}_upgrade")
    rec["_comentario"] = f"Aprimoramento de raridade {r}: acessível, mas limitado a 2 por jogador (Cotas.java)."
    rec["json"] = {"type": "minecraft:crafting_shaped", "category": "misc",
                   "key": {"I": {"item": f"irons_spellbooks:{r}_ink"}, "M": {"item": m}, "O": {"item": "irons_spellbooks:upgrade_orb"}},
                   "pattern": [" I ", "MOM", " I "],
                   "result": {"count": 1, "id": f"irons_restrictions:{r}_upgrade"}}

SRC.write_text(json.dumps(cfg, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print("v24 aplicada: 5 receitas de aprimoramento refeitas")
