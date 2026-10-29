"""Agrupa os erros do javac (build/erros.txt) por causa e por arquivo. Uso: python -I smp-aetherworks/tools/erros.py [filtro]"""
import collections
import re
import sys
from pathlib import Path

BASE = Path(__file__).resolve().parents[1]
linhas = (BASE / "build" / "erros.txt").read_text(encoding="utf-8", errors="replace").splitlines()
filtro = sys.argv[1] if len(sys.argv) > 1 else None
erros = []
i = 0
while i < len(linhas):
    m = re.match(r"(.+?\.java):(\d+): error: (.*)", linhas[i])
    if m:
        arq, ln, msg = m.groups()
        extra = []
        j = i + 1
        while j < len(linhas) and not re.match(r".+?\.java:\d+: error:", linhas[j]) and not re.match(r"\d+ errors?", linhas[j]):
            extra.append(linhas[j])
            j += 1
        simb = next((e.strip() for e in extra if e.strip().startswith("symbol:")), "")
        erros.append((Path(arq).name, int(ln), msg, simb, extra))
        i = j
    else:
        i += 1
if filtro == "override":  # métodos com @Override que não sobrescrevem mais nada: nome + arquivo
    caminhos = {p.name: p for p in (BASE / "src").rglob("*.java")}
    por_nome = collections.defaultdict(list)
    for a, l, m, s, ex in erros:
        if "does not override" in m:
            src = caminhos[a].read_text(encoding="utf-8").splitlines()
            trecho = " ".join(src[l - 1:l + 4])
            nome = re.search(r"(\w+)\s*\(", trecho.split("@Override", 1)[-1])
            por_nome[nome.group(1) if nome else "?"].append(f"{a}:{l}")
    for k, v in sorted(por_nome.items(), key=lambda x: -len(x[1])):
        print(f"{len(v):3} {k}: {', '.join(v[:6])}{' …' if len(v) > 6 else ''}")
    sys.exit()
if filtro:
    for a, l, m, s, ex in erros:
        if filtro in a or filtro in m or filtro in s:
            print(f"{a}:{l}: {m} {s}")
            print("   ", ex[0].strip() if ex else "")
    sys.exit()
causa = collections.Counter()
for a, l, m, s, _ in erros:
    chave = (m + " " + s) if "cannot find symbol" in m or "does not exist" in m else re.sub(r"\b\w+\.java\b", "", m)[:110]
    causa[chave] += 1
print(len(erros), "erros")
for k, v in causa.most_common(70):
    print(f"{v:4} {k}")
print("--- por arquivo")
for k, v in collections.Counter(a for a, *_ in erros).most_common(25):
    print(f"{v:4} {k}")
