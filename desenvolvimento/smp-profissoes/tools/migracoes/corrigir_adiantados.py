"""Regra do usuário (26/10/2026): nada pode ser liberado antes de existir a forma de fazê-lo.

Para cada item "adiantado" (todas as receitas pedem algum ingrediente cuja pesquisa não está na cadeia da etapa do item):
1. move o item para a etapa da mesma profissão cuja cadeia já tem tudo o que os ingredientes pedem (a menor cadeia);
2. se nenhuma etapa tem tudo, escolhe a etapa da mesma profissão que mais se aproxima (sem ficar abaixo do nível das
   pesquisas que faltam) e acrescenta o que falta como requisito extra da pesquisa dela; o item vai para lá.
Repete até não sobrar nada. Itens de mod inicial contam como disponíveis (sempre há quem comece por eles).
Uso: python -I smp-profissoes/tools/migracoes/corrigir_adiantados.py [--simular]
"""
import json
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[2]
SRC = RAIZ / "progressao.json"
sys.path.insert(0, str(RAIZ / "tools"))
import gerar  # noqa: E402

cfg = json.loads(SRC.read_text(encoding="utf-8"))
ix = gerar.indexar()
E = {e["licenca"]: e for e in cfg["etapas"]}
P = {p["id"]: p for p in cfg["pesquisas"]}
movidos, extras_add, vezes, travados, motivos = {}, {}, {}, set(), {}


def ancestrais(pid, vis=None):
    vis = vis if vis is not None else set()
    if pid in vis or pid not in P:
        return vis
    vis.add(pid)
    p = P[pid]
    for x in ([p["pai"]] if p.get("pai") else []) + p.get("pesquisas_extra", []):
        ancestrais(x, vis)
    return vis


def mover(i, atual, destino, explicito):
    E[atual].setdefault("excluir", [])
    if i not in E[atual]["excluir"]:
        E[atual]["excluir"].append(i)
    if i in E[destino].get("excluir", []):
        E[destino]["excluir"].remove(i)
    prof = E[destino]["profissao"]
    chave = "itens" if i.split(":", 1)[0] in cfg["profissoes"][prof]["namespaces"] else "itens_avulsos"
    if i not in E[destino].get(chave, []):
        E[destino].setdefault(chave, []).append(i)
    if not explicito:  # era "resto" (sem vedação total): continua assim no destino
        E[destino].setdefault("sem_vedacao", [])
        if i not in E[destino]["sem_vedacao"]:
            E[destino]["sem_vedacao"].append(i)
    movidos[i] = (movidos.get(i, (atual,))[0], destino)
    vezes[i] = vezes.get(i, 0) + 1


# decisões à mão (o automático escolheria algo estranho): item -> etapa
MANUAIS = {
    # armaduras de cristal combinam vestes do Iron's + runas: ficam todas em Crystal Knights
    **{f"crystal_chronicles:ender_mage_{p}": "arc_cavaleiros" for p in ("helmet", "chestplate", "leggings", "boots")},
    # o amuleto sai da montagem sequencial do Create com pepita de mithril do Create Wizardry
    "irons_spellbooks:teleportation_amulet": "c11_eng",
    "irons_spellbooks:autoloader_crossbow": "c11_eng",  # mechanical crafting do Create, com latão
    # poppets do vazio pedem o giz do Outrolugar (Otherwhere Rites)
    **{f"enchanted:{x}void_poppet": "nat_ritos" for x in ("", "sturdy_", "infused_")},
    # itens de teletransporte pedem a Singularity do AE2 (ME Networks): vão para pesquisas de nível 4 que já cruzam com ela
    "irons_spellbooks:portal_frame": "eng_ae2_quantico",  # a Singularity é de Quantum Networks (nível 5)
    # peças Embers + Create Aeronautics
    "embers:caminite_turngear": "c1_eng",
    "embers:ember_sightstone": "c1_eng",
}
r0 = gerar.main(json.loads(json.dumps(cfg)), ix, analise=True)
for i, destino in MANUAIS.items():
    if r0["dono"].get(i) != destino:
        mover(i, r0["dono"][i], destino, i in r0["explicito"])

for rodada in range(1, 25):
    r = gerar.main(json.loads(json.dumps(cfg)), ix, analise=True)
    adiant = {i: f for i, f in r["adiantados"].items() if i not in travados}
    print(f"rodada {rodada}: {len(r['adiantados'])} adiantado(s)")
    if not adiant:
        break
    pe, dono = r["pesq_etapa"], r["dono"]
    for i in sorted(adiant):
        atual = dono[i]
        prof = E[atual]["profissao"]
        precisa = r["necessario"][i]
        if vezes.get(i, 0) >= 3:  # não deixa um item ficar pulando de etapa em etapa
            travados.add(i)
            continue
        # 1º: uma etapa mais adiante na MESMA linha (cadeia contém a da etapa atual) que já tenha tudo
        candidatas = [l for l, e in E.items() if e["profissao"] == prof and precisa <= pe[l] and pe[atual] <= pe[l]]
        if candidatas:
            destino = min(candidatas, key=lambda l: (len(pe[l]), l))
            mover(i, atual, destino, i in r["explicito"])
            continue
        # 2º: a própria pesquisa do item ganha o que falta como requisito extra (se o nível dela comportar);
        #     senão, a etapa mais adiante na mesma linha com nível suficiente
        nivel_max = max(P[x]["nivel"] for x in precisa)
        opcoes = [l for l, e in E.items() if e["profissao"] == prof and pe[atual] <= pe[l]
                  and P[e["pesquisas"][0]]["nivel"] > nivel_max  # regra de 28/10: pesquisa exigida é de nível ANTERIOR
                  and not any(P[e["pesquisas"][0]]["id"] in ancestrais(x) for x in precisa - pe[l])]
        if not opcoes:  # 3º: qualquer etapa da profissão que já tenha tudo (sai da linha)
            candidatas = [l for l, e in E.items() if e["profissao"] == prof and precisa <= pe[l]]
            if candidatas:
                mover(i, atual, min(candidatas, key=lambda l: (len(pe[l]), l)), i in r["explicito"])
            else:
                travados.add(i)
            continue
        destino = min(opcoes, key=lambda l: (len(precisa - pe[l]), len(pe[l]), l))
        alvo = P[E[destino]["pesquisas"][0]]
        faltam = sorted(precisa - pe[destino])
        # só as pesquisas "de ponta" (as que não são ancestrais de outra que falta)
        pontas = [x for x in faltam if not any(x != y and x in ancestrais(y) for y in faltam)]
        alvo["pesquisas_extra"] = sorted(set(alvo.get("pesquisas_extra", [])) | set(pontas))
        extras_add.setdefault(alvo["id"], set()).update(pontas)
        motivos.setdefault(alvo["id"], []).append(i)
        if destino != atual:
            mover(i, atual, destino, i in r["explicito"])

nomes = {p["id"]: p["nome"] for p in cfg["pesquisas"]}
print(f"\n{len(movidos)} item(ns) movido(s); {len(extras_add)} pesquisa(s) com requisito extra novo; {len(travados)} para decisão manual")
for pid, xs in sorted(extras_add.items()):
    print(f"  REQUISITO {nomes[pid]} agora exige também: {', '.join(nomes[x] for x in sorted(xs))}  (por {', '.join(motivos[pid][:4])})")
for i, (de, para) in sorted(movidos.items(), key=lambda kv: (kv[1][1], kv[0])):
    print(f"  MOVIDO {i:55} {de:24} -> {para}")
r = gerar.main(json.loads(json.dumps(cfg)), ix, analise=True)
for i, falta in sorted(r["adiantados"].items()):
    print(f"  MANUAL {i:55} {r['dono'][i]:24} precisa de {', '.join(sorted(nomes.get(x, x) for x in r['necessario'][i]))}")
if "--simular" not in sys.argv:
    SRC.write_text(json.dumps(cfg, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("progressao.json atualizado")
