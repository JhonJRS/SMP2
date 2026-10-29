"""Migração v19 (28/10/2026), rodada 11: pré-requisitos coerentes com a universidade da MineColonies.

1. Edifício exigido por uma pesquisa de nível N tem de estar disponível no nível N: livre, ou liberado por uma pesquisa
   da MineColonies de nível <= N (ex.: Mechanic sai em technology/whatyaneed, nível 2).
2. Pesquisa exigida (pesquisas_extra) tem de ser de nível ANTERIOR. Quando a pesquisa dependia de outra do mesmo nível
   (ingredientes), ela sobe um nível e passa a ficar embaixo da que exige (o pai antigo vira requisito, de nível menor),
   desde que o ramo inteiro continue <= 5. Senão a exigência vira a ancestral de nível menor da mesma linha, e depois
   corrigir_adiantados.py leva os itens que dependiam dela para uma etapa que já tenha o ingrediente.
Roda uma vez. Depois: python -I smp-profissoes/tools/migracoes/corrigir_adiantados.py
"""
import json
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[2]
SRC = RAIZ / "progressao.json"

cfg = json.loads(SRC.read_text(encoding="utf-8"))
if cfg.get("_v19"):
    raise SystemExit("já migrado (v19)")
cfg["_v19"] = True

TROCA_EDIFICIO = {  # pesquisa -> (edifício que sai, edifício que entra)
    "fundacao_create": ("minecolonies:mechanic", "minecolonies:blacksmith"),
    "fundacao_botania": ("minecolonies:florist", "minecolonies:farmer"),
    "fundacao_ervas": ("minecolonies:plantation", "minecolonies:farmer"),
    "bruxaria": ("minecolonies:alchemist", "minecolonies:enchanter"),
}
P = {p["id"]: p for p in cfg["pesquisas"]}
curto = lambda i: i.split("/")[-1]
for p in cfg["pesquisas"]:
    t = TROCA_EDIFICIO.get(curto(p["id"]))
    if t:
        lv = p["edificios"].pop(t[0])
        p["edificios"][t[1]] = max(p["edificios"].get(t[1], 0), lv)

filhos = {}
for p in cfg["pesquisas"]:
    if p.get("pai"):
        filhos.setdefault(p["pai"], []).append(p["id"])
prof = {}
for e in cfg["etapas"]:
    for x in e["pesquisas"]:
        prof.setdefault(x, e["profissao"])


def fundo(pid):
    """Maior nível do ramo que começa em pid."""
    return max([P[pid]["nivel"]] + [fundo(f) for f in filhos.get(pid, [])])


def subir(pid, delta):
    P[pid]["nivel"] += delta
    for f in filhos.get(pid, []):
        subir(f, delta)


def ramo(pid):
    return [pid] + [y for f in filhos.get(pid, []) for y in ramo(f)]


def cabe(pid, delta):
    """Subir o ramo não pode alcançar o nível de quem o exige (senão a exigência deles vira 'mesmo nível')."""
    if fundo(pid) + delta > 5:
        return False
    for x in ramo(pid):
        novo = P[x]["nivel"] + delta
        for q in cfg["pesquisas"]:
            if x in q.get("pesquisas_extra", []) and q["id"] not in ramo(pid) and q["nivel"] <= novo:
                return False
    return True


def cadeia(pid):
    vistos, pilha = set(), [pid]
    while pilha:
        x = pilha.pop()
        if x in vistos or x not in P:
            continue
        vistos.add(x)
        if P[x].get("pai"):
            pilha.append(P[x]["pai"])
        pilha += P[x].get("pesquisas_extra", [])
    return vistos


log = []
for _ in range(20):
    mudou = False
    for p in sorted(cfg["pesquisas"], key=lambda x: x["nivel"]):
        N = p["nivel"]
        iguais = [x for x in p.get("pesquisas_extra", []) if P[x]["nivel"] >= N]
        if not iguais:
            continue
        mudou = True
        alvo = max(iguais, key=lambda x: (P[x]["nivel"], prof.get(x) == prof.get(p["id"])))
        delta = P[alvo]["nivel"] + 1 - N
        if p["id"] not in cadeia(alvo) and cabe(p["id"], delta):
            velho = p.get("pai")
            p["pai"] = alvo
            p["pesquisas_extra"] = [x for x in p["pesquisas_extra"] if x != alvo] + ([velho] if velho else [])
            if velho:
                filhos[velho].remove(p["id"])
            filhos.setdefault(alvo, []).append(p["id"])
            subir(p["id"], delta)
            log.append(f"SOBE  {curto(p['id'])}: n{N} -> n{p['nivel']}, agora embaixo de {curto(alvo)}")
            break  # níveis mudaram: recomeça
        # não cabe: troca pelos pré-requisitos diretos dela (pai e extras, todos de nível menor), mantendo a cadeia
        propria = cadeia(p["pai"]) if p.get("pai") else set()
        resto = [y for y in p["pesquisas_extra"] if y != alvo]
        novos = []
        for x in [P[alvo].get("pai")] + P[alvo].get("pesquisas_extra", []):
            while x and P[x]["nivel"] >= N:
                x = P[x].get("pai")
            if x and x not in propria and x not in resto and x not in novos:
                novos.append(x)
        resto += novos
        log.append(f"TROCA {curto(p['id'])} (n{N}): {curto(alvo)} (n{P[alvo]['nivel']}) -> "
                   + (", ".join(f"{curto(x)} (n{P[x]['nivel']})" for x in novos) or "nada (já na cadeia)"))
        if resto:
            p["pesquisas_extra"] = resto
        else:
            p.pop("pesquisas_extra")
        break
    if not mudou:
        break

# os edifícios exigidos não podem passar do (novo) nível da pesquisa
for p in cfg["pesquisas"]:
    for b, lv in p.get("edificios", {}).items():
        p["edificios"][b] = min(lv, p["nivel"])

SRC.write_text(json.dumps(cfg, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print("v19 aplicada: 4 edifícios trocados")
for m in log:
    print("  " + m)
