"""Gera smp-profissoes/arvore/arvore.html: a aba Guild of Trades desenhada por profissão e nível (dados do progressao.json).
Uso: python -I smp-profissoes/tools/arvore_html.py
"""
import json
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[1]
cfg = json.loads((RAIZ / "progressao.json").read_text(encoding="utf-8"))
O = "smpprofissoes:oficios/"
NOMES_PREDIO = {"mechanic": "Mechanic", "blacksmith": "Blacksmith", "enchanter": "Enchanter", "farmer": "Farmer", "plantation": "Plantation",
                "smeltery": "Smeltery", "florist": "Florist", "cook": "Cook"}

pesq = {p["id"]: p for p in cfg["pesquisas"]}
raiz_de = {}
for pid in pesq:
    r = pid
    while pesq[r].get("pai"):
        r = pesq[r]["pai"]
    raiz_de[pid] = r
etapas_de = {}
prof_de_raiz = {}
for e in cfg["etapas"]:
    for r in e["pesquisas"][:1]:
        etapas_de.setdefault(r, []).append(e)
    if not pesq.get(e["pesquisas"][0], {}).get("pai"):
        prof_de_raiz[e["pesquisas"][0]] = e["profissao"]
comps = {c["id"]: c for c in cfg["componentes"]}
comp_de_lic = {}
for c in cfg["componentes"]:
    for l in c["licencas"]:
        comp_de_lic.setdefault(l, []).append(c["nome"])

nos = []
for pid, p in pesq.items():
    libera = []
    for e in etapas_de.get(pid, []):
        quem = e["nome"].split(" · ")[0]
        itens = [i.split(":", 1)[1] for i in e.get("itens", []) + e.get("itens_avulsos", [])]
        resto = e.get("resto_de", [])
        libera.append({"quem": quem, "mods": resto, "itens": itens[:14], "mais": max(0, len(itens) - 14),
                       "componentes": comp_de_lic.get(e["licenca"], [])})
    nos.append({"id": pid.split("/", 1)[1], "nome": p["nome"], "sub": p["subtitulo"], "nivel": p["nivel"],
                "pai": p["pai"].split("/", 1)[1] if p.get("pai") else None,
                "extra": [pesq[x]["nome"] for x in p.get("pesquisas_extra", []) if x in pesq],
                "extraProf": sorted({prof_de_raiz.get(raiz_de[x], "?") for x in p.get("pesquisas_extra", []) if x in pesq}),
                "predios": [f"{NOMES_PREDIO.get(b.split(':')[1], b.split(':')[1].title())} {lv}" for b, lv in p.get("edificios", {}).items()],
                "custos": [f"{n}× {i.split(':', 1)[1].replace('_', ' ')}" for i, n in p["custos"].items()],
                "raiz": raiz_de[pid].split("/", 1)[1], "prof": prof_de_raiz.get(raiz_de[pid], "?"), "libera": libera})
profs = [{"id": k, "nome": v["nome"]} for k, v in cfg["profissoes"].items()]
dados = json.dumps({"nos": nos, "profs": profs}, ensure_ascii=False)

modelo = (Path(__file__).with_name("arvore_modelo.html")).read_text(encoding="utf-8")
saida = RAIZ / "arvore" / "arvore.html"
saida.parent.mkdir(exist_ok=True)
saida.write_text(modelo.replace("/*DADOS*/null", dados), encoding="utf-8")
print("Escrito:", saida, f"({len(nos)} pesquisas)")
