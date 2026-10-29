"""Migração v25 (28/10/2026), rodada 13: sem limite por jogador (a cota da v24 saiu do mod). A raridade dos aprimoramentos
vem dos ingredientes: itens que ninguém consegue juntar em quantidade (só de estrutura ou de chefe).
Orbe-base no meio, 2 tintas da raridade em cima/embaixo, M e S nas laterais. Roda uma vez."""
import json
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[2]
SRC = RAIZ / "progressao.json"
sys.path.insert(0, str(RAIZ / "tools"))
import gerar  # noqa: E402

cfg = json.loads(SRC.read_text(encoding="utf-8"))
if cfg.get("_v25"):
    raise SystemExit("já migrado (v25)")
cfg["_v25"] = True
ix = gerar.indexar()

ESCASSOS = {  # raridade -> (M, S)
    "common": ("minecraft:nautilus_shell", "minecraft:nautilus_shell"),     # afogados, pesca, mercador
    "uncommon": ("minecraft:heart_of_the_sea", "minecraft:nautilus_shell"),  # um por tesouro enterrado
    "rare": ("minecraft:echo_shard", "minecraft:echo_shard"),                # só baús de Ancient City
    "epic": ("minecraft:heavy_core", "minecraft:echo_shard"),                # cofre sinistro das Trial Chambers
    "legendary": ("minecraft:nether_star", "minecraft:heavy_core"),          # Wither + cofre sinistro
}
for r, (m, s) in ESCASSOS.items():
    for i in (m, s):
        if i not in ix.itens:
            raise SystemExit(f"item inexistente: {i}")
    rec = next(x for x in cfg["receitas_novas"] if x["id"] == f"smpprofissoes:especial/{r}_upgrade")
    rec["_comentario"] = f"Aprimoramento de raridade {r}: ingredientes escassos ({m}, {s}), sem limite por jogador."
    rec["json"] = {"type": "minecraft:crafting_shaped", "category": "misc",
                   "key": {"I": {"item": f"irons_spellbooks:{r}_ink"}, "M": {"item": m}, "S": {"item": s},
                           "O": {"item": "irons_spellbooks:upgrade_orb"}},
                   "pattern": [" I ", "MOS", " I "],
                   "result": {"count": 1, "id": f"irons_restrictions:{r}_upgrade"}}

SRC.write_text(json.dumps(cfg, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print("v25 aplicada: 5 aprimoramentos com ingredientes escassos")
