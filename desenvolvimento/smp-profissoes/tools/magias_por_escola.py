"""Levanta, direto dos jars, todas as magias do Iron's Spells e addons: id, escola e raridade mínima (DefaultConfig no
construtor de cada AbstractSpell) e se a classe é registrada (aparece com `new` numa classe de registro).
Saída: smp-profissoes/MAGIAS_POR_ESCOLA.md (+ .json). Uso: python -I smp-profissoes/tools/magias_por_escola.py

Serve para conferir se cada escola tem manuscrito e quais magias o manuscrito mostra: o Iron's Restrictions lista
todas as magias HABILITADAS da escola cuja raridade mínima o jogador já alcançou (SchoolResearchScreen)."""
import json
import re
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path

INST = Path(__file__).resolve().parents[2]
JAVAP = r"C:\Program Files\Java\jdk-21.0.12\bin\javap.exe"
ABSTRATA = b"io/redspace/ironsspellbooks/api/spells/AbstractSpell"
RARIDADES = ["COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY"]


def javap(raiz, classes):
    out = {}
    for i in range(0, len(classes), 60):
        lote = classes[i:i + 60]
        r = subprocess.run([JAVAP, "-c", "-p", "-constants", "-cp", str(raiz)] + lote, capture_output=True, text=True,
                           encoding="utf-8", errors="replace")
        atual = None
        for linha in r.stdout.splitlines():
            m = re.match(r"^(?:public |abstract |final |class |\s)*class ([\w.$]+)", linha)
            if m and not linha.startswith(" "):
                atual = m.group(1)
                out[atual] = []
            elif atual:
                out[atual].append(linha)
    return out


def analisar(nome, linhas):
    texto = "\n".join(linhas)
    if "extends io.redspace.ironsspellbooks.api.spells.AbstractSpell" not in "\n".join(linhas[:3]) and "DefaultConfig" not in texto:
        return None
    ini = []
    dentro = False
    for l in linhas:
        if re.match(r"^\s+(public|protected|private)?\s*[\w.$]+\(.*\);$", l) and nome.split(".")[-1] in l.split("(")[0]:
            dentro = True
            continue
        if dentro and re.match(r"^\s+(public|protected|private|static).*\);$", l):
            break
        if dentro:
            ini.append(l)
    ini_txt = "\n".join(ini) or texto
    esc = None
    m = re.search(r"getstatic\s+#\d+\s+// Field ([\w/$]+)\.(\w+_RESOURCE):Lnet/minecraft/resources/ResourceLocation;[\s\S]{0,200}?setSchoolResource", ini_txt)
    if m:
        esc = m.group(2)[:-len("_RESOURCE")].lower()
        dono = m.group(1).split("/")[-1]
        if dono != "SchoolRegistry":  # escola de addon: registrada por outra classe (o namespace sai do relatório de escolas)
            esc = f"{dono}.{esc}"
    else:
        m = re.search(r'ldc\s+#\d+\s+// String ([\w]+)\s*\n.*ldc\s+#\d+\s+// String ([\w]+)\s*\n[\s\S]{0,160}?setSchoolResource', ini_txt)
        if m:
            esc = f"{m.group(1)}:{m.group(2)}"
    rar = re.search(r"getstatic\s+#\d+\s+// Field io/redspace/ironsspellbooks/api/spells/SpellRarity\.(\w+):[^\n]*\n[^\n]*setMinRarity", ini_txt)
    sid = None
    m = re.search(r'ldc\s+#\d+\s+// String (\w+)\s*\n\s*\d+: ldc\s+#\d+\s+// String (\w+)\s*\n[^\n]*\n\s*\d+: putfield\s+#\d+\s+// Field spellId', ini_txt)
    if m:
        sid = f"{m.group(1)}:{m.group(2)}"
    else:  # IronsSpellbooks.id("x") ou utilitário parecido do addon: namespace = o do jar
        m = re.search(r'ldc\s+#\d+\s+// String ([\w/]+)\s*\n[^\n]*\n\s*\d+: putfield\s+#\d+\s+// Field spellId', ini_txt)
        if m:
            sid = "?:" + m.group(1)
    if not esc and not sid:
        return None
    return {"classe": nome, "id": sid, "escola": esc, "raridade_min": rar.group(1) if rar else None}


def main():
    magias, registradas = [], set()
    for jar in sorted((INST / "mods").glob("*.jar")):
        with zipfile.ZipFile(jar) as z:
            cands, regs = [], []
            for n in z.namelist():
                if not n.endswith(".class"):
                    continue
                b = z.read(n)
                if ABSTRATA in b and b"DefaultConfig" in b:
                    cands.append(n)
                if b"registerSpell" in b or (b"SpellRegistry" in n.encode() or b"Registry" in n.encode()) and ABSTRATA in b:
                    regs.append(n)
            if not cands:
                continue
            with tempfile.TemporaryDirectory() as tmp:
                for n in set(cands + regs):
                    destino = Path(tmp) / n
                    destino.parent.mkdir(parents=True, exist_ok=True)
                    destino.write_bytes(z.read(n))
                saida = javap(tmp, [n[:-6].replace("/", ".") for n in cands])
                for nome, linhas in saida.items():
                    info = analisar(nome, linhas)
                    if info:
                        info["jar"] = jar.name
                        magias.append(info)
                for nome, linhas in javap(tmp, [n[:-6].replace("/", ".") for n in regs]).items():
                    for l in linhas:
                        m = re.search(r"// class ([\w/$]+)$", l.strip())
                        if m and "new" in l:
                            registradas.add(m.group(1).replace("/", "."))
    for m in magias:
        m["registrada"] = m["classe"] in registradas
    return magias


def relatorio(magias):
    por = {}
    for m in magias:
        if not m["registrada"]:
            continue
        por.setdefault(m["escola"] or "?", []).append(m)
    linhas = ["# Magias por escola (lido dos jars por tools/magias_por_escola.py)", "",
              "O manuscrito de uma escola (Iron's Restrictions) mostra toda magia HABILITADA dessa escola cuja raridade mínima o",
              "jogador já desbloqueou (Comum no início; Incomum/Raro/Épico/Lendário pelos aprimoramentos).", ""]
    for esc in sorted(por):
        lista = sorted(por[esc], key=lambda m: (RARIDADES.index(m["raridade_min"]) if m["raridade_min"] in RARIDADES else 9, m["id"] or ""))
        linhas.append(f"## {esc} — {len(lista)} magia(s)")
        for r in RARIDADES + [None]:
            ids = [m["id"] or m["classe"].split(".")[-1] for m in lista if m["raridade_min"] == r]
            if ids:
                linhas.append(f"- **{r or 'raridade não lida'}**: " + ", ".join(ids))
        linhas.append("")
    fora = [m for m in magias if not m["registrada"]]
    if fora:
        linhas += ["## Classes de magia NÃO registradas (não existem no jogo)", ""] + [f"- {m['classe']} ({m['jar']})" for m in fora] + [""]
    return "\n".join(linhas)


if __name__ == "__main__":
    ms = main()
    (INST / "smp-profissoes" / "MAGIAS_POR_ESCOLA.json").write_text(json.dumps(ms, indent=2, ensure_ascii=False), encoding="utf-8")
    (INST / "smp-profissoes" / "MAGIAS_POR_ESCOLA.md").write_text(relatorio(ms), encoding="utf-8")
    print(len(ms), "classes;", sum(m["registrada"] for m in ms), "registradas;",
          sum(1 for m in ms if m["registrada"] and (not m["escola"] or not m["raridade_min"] or not m["id"])), "com campo não lido")
    sys.exit(0)
