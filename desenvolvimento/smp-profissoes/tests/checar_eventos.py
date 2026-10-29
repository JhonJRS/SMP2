"""Confere nos fontes do mod que todo método com @SubscribeEvent recebe um evento (um parâmetro cujo tipo termina em
Event, ou um evento interno como ServerTickEvent.Post). O NeoForge recusa carregar o mod se não (crash de 29/10:
a anotação ficou num método novo inserido entre ela e o método certo)."""
import re
import sys
from pathlib import Path

FONTES = Path(__file__).resolve().parents[1] / "mod" / "src" / "main" / "java"
DECL = re.compile(r"^\s*(public|private|protected|static|final|\s)*[\w<>\[\], ?.]+\s+(\w+)\s*\(([^)]*)\)")

falhas = total = 0
for arq in sorted(FONTES.rglob("*.java")):
    linhas = arq.read_text(encoding="utf-8").splitlines()
    for i, l in enumerate(linhas):
        if l.strip() != "@SubscribeEvent":
            continue
        total += 1
        # próxima declaração de método (pula comentários e outras anotações)
        for j in range(i + 1, min(i + 15, len(linhas))):
            m = DECL.match(linhas[j])
            if m and not linhas[j].strip().startswith(("*", "/", "@")):
                params = [p.strip() for p in m.group(3).split(",") if p.strip()]
                tipo = params[0].rsplit(" ", 1)[0] if len(params) == 1 else ""
                if not re.search(r"Event(\.\w+)?$", tipo):
                    falhas += 1
                    print(f"FALHA {arq.name}:{j + 1}: @SubscribeEvent em {m.group(2)}({m.group(3)}) — não recebe um evento")
                break
print(f"@SubscribeEvent: {total} conferidos, {falhas} falha(s).")
sys.exit(1 if falhas else 0)
