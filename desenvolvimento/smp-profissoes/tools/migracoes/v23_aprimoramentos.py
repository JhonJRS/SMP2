"""Migração v23 (28/10/2026), rodada 12: receita de bancada para cada aprimoramento de raridade do Iron's Restrictions
(além do caldeirão). Orbe de aprimoramento no meio, 4 tintas da raridade nos cantos e materiais que sobem com ela.
Cada aprimoramento continua preso à etapa da sua pesquisa (v20). Roda uma vez."""
import json
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[2]
SRC = RAIZ / "progressao.json"
sys.path.insert(0, str(RAIZ / "tools"))
import gerar  # noqa: E402

cfg = json.loads(SRC.read_text(encoding="utf-8"))
if cfg.get("_v23"):
    raise SystemExit("já migrado (v23)")
cfg["_v23"] = True
ix = gerar.indexar()

RARIDADES = {  # raridade -> (A: em cima/embaixo, B: nas laterais)
    "common": ("irons_spellbooks:arcane_essence", "irons_spellbooks:arcane_essence"),
    "uncommon": ("irons_spellbooks:arcane_essence", "minecraft:lapis_block"),
    "rare": ("irons_spellbooks:arcane_ingot", "minecraft:gold_block"),
    "epic": ("irons_spellbooks:arcane_ingot", "minecraft:diamond_block"),
    "legendary": ("minecraft:nether_star", "minecraft:netherite_ingot"),
}
for r, (a, b) in RARIDADES.items():
    tinta, saida = f"irons_spellbooks:{r}_ink", f"irons_restrictions:{r}_upgrade"
    for i in (tinta, saida, a, b, "irons_spellbooks:upgrade_orb"):
        if i not in ix.itens:
            raise SystemExit(f"item inexistente: {i}")
    cfg["receitas_novas"].append({
        "id": f"smpprofissoes:especial/{r}_upgrade",
        "_comentario": f"Aprimoramento de raridade {r} (Iron's Restrictions) na bancada.",
        "json": {"type": "minecraft:crafting_shaped", "category": "misc",
                 "key": {"I": {"item": tinta}, "A": {"item": a}, "B": {"item": b}, "O": {"item": "irons_spellbooks:upgrade_orb"}},
                 "pattern": ["IAI", "BOB", "IAI"],
                 "result": {"count": 1, "id": saida}}})

# o orbe-base (sem escola) desce para o Iron's Foundations: os aprimoramentos Comum/Incomum partem dele; os orbes de
# escola continuam em Arcane Mastery
maestria = next(e for e in cfg["etapas"] if e["licenca"] == "arc_maestria")
maestria["excluir"] = sorted(set(maestria.get("excluir", [])) | {"irons_spellbooks:upgrade_orb"})

SRC.write_text(json.dumps(cfg, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print("v23 aplicada: 5 receitas de aprimoramento de raridade")
