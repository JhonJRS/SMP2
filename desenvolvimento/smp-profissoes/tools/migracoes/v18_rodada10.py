"""Migração v18 (28/10/2026), rodada 10: kits iniciais, adagas de arremesso do NeoVitae em Blood Foundations e
Replication Matrix mais acessível. Roda uma vez."""
import json
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[2]
SRC = RAIZ / "progressao.json"
sys.path.insert(0, str(RAIZ / "tools"))
import gerar  # noqa: E402

cfg = json.loads(SRC.read_text(encoding="utf-8"))
if cfg.get("_v18"):
    raise SystemExit("já migrado (v18)")
cfg["_v18"] = True
ix = gerar.indexar()


def existe(*itens):
    for i in itens:
        if i not in ix.itens:
            raise SystemExit(f"item inexistente: {i}")
    return list(itens)


def kit(lic):
    for p in cfg["profissoes"].values():
        for ini in p.get("inicios", []):
            if ini["licenca"] == lic:
                return ini["kit"]
    raise SystemExit(f"início inexistente: {lic}")


# NeoVitae: Novicius Orb of Vitae
existe("neovitae:blood_orb_weak")
k = kit("arc_neovitae")
k.insert(1, ["neovitae:blood_orb_weak", 1])

# Iron's Spells: sem Arcane Essence e Blank Rune; entra a Inscription Table (a magia inicial será definida depois)
existe("irons_spellbooks:inscription_table")
k = kit("arc_irons")
k[:] = [x for x in k if x[0] not in ("irons_spellbooks:arcane_essence", "irons_spellbooks:blank_rune")]
k.insert(1, ["irons_spellbooks:inscription_table", 1])

# Naturalista (Farmer's Delight + Farm & Charm): meio pack de farinha de osso
k = kit("nat_cozinha")
k.insert(len(k) - 2, ["minecraft:bone_meal", 32])

# Adagas de arremesso: saem do Sentient Arsenal e caem no resto do NeoVitae (Blood Foundations)
sen = next(e for e in cfg["etapas"] if e["licenca"] == "arc_senciente")
sen["itens"] = [i for i in sen["itens"] if i not in ("neovitae:throwing_dagger*", "neovitae:amethyst_throwing_dagger")]

# Replication Matrix mais acessível (continua sem os itens dos rituais de duplicação)
comp = next(c for c in cfg["componentes"] if c["id"] == "replication_matrix")
comp["ingredientes"] = existe("relics:relic_experience_bottle", "relics:relic_experience_bottle", "minecraft:trial_key",
                              "botania:mana_diamond", "forbidden_arcanus:arcane_crystal", "minecraft:totem_of_undying")

SRC.write_text(json.dumps(cfg, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print("v18 aplicada")
