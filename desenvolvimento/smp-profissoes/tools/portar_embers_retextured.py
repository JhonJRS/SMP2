"""Converte o resource pack "Embers Retextured (1.20.1)" para 1.21.1 / Embers Re-Ignited.

  - pack_format 15 -> 34;  loader "forge:separate_transforms" -> "neoforge:separate_transforms"
  - só entram os namespaces de mods instalados (aetherworks/embers_extended/embersdelight ficam de fora até existirem)
  - descarta lixo do Windows (desktop.ini, "* - Copy.png")
  - confere: todo JSON lê, toda textura citada por um modelo existe no pack ou no jar do mod
Uso: python -I smp-profissoes/tools/portar_embers_retextured.py "<zip 1.20.1>"
"""
import io
import json
import re
import sys
import zipfile
from pathlib import Path

INST = Path(__file__).resolve().parents[2]
SAIDA = INST / "resourcepacks" / "Embers Retextured 1.21.1 (SMP 2).zip"


def assets_instalados():
    """namespace -> conjunto de caminhos de assets dentro dos jars instalados."""
    por_ns = {}
    for j in (INST / "mods").glob("*.jar"):
        try:
            with zipfile.ZipFile(j) as z:
                for n in z.namelist():
                    if n.startswith("assets/") and n.count("/") >= 2:
                        por_ns.setdefault(n.split("/")[1], set()).add(n)
        except zipfile.BadZipFile:
            pass
    return por_ns


def main(origem):
    instalados = assets_instalados()
    src = zipfile.ZipFile(origem)
    arquivos, pulados = {}, set()
    for n in src.namelist():
        if n.endswith("/") or n.endswith("desktop.ini") or " - Copy" in n:
            continue
        if n.startswith("assets/"):
            ns = n.split("/")[1]
            if ns not in instalados:
                pulados.add(ns)
                continue
        dados = src.read(n)
        if n.endswith(".json"):
            t = dados.decode("utf-8-sig").replace('"forge:separate_transforms"', '"neoforge:separate_transforms"')
            json.loads(t)  # falha alto se algum JSON estiver quebrado
            dados = t.encode("utf-8")
        arquivos[n] = dados
    meta = json.loads(arquivos["pack.mcmeta"])
    meta["pack"]["pack_format"] = 34
    meta["pack"]["description"] = "§6◆ Embers Retextured 5.0 — port 1.21.1 (Embers Re-Ignited)"
    arquivos["pack.mcmeta"] = json.dumps(meta, ensure_ascii=False, indent=2).encode("utf-8")

    # toda textura citada precisa existir no pack ou no mod
    erros = []
    for n, d in arquivos.items():
        if n.endswith(".json") and "/models/" in n:
            for tex in re.findall(r'"([a-z0-9_.-]+):((?:block|item|entity|particle|gui)/[a-z0-9_/.-]+)"', d.decode()):
                ns, cam = tex
                p = f"assets/{ns}/textures/{cam}.png"
                if ns != "minecraft" and p not in arquivos and p not in instalados.get(ns, ()):
                    if f"assets/{ns}/models/{cam}.json" in arquivos or f"assets/{ns}/models/{cam}.json" in instalados.get(ns, ()):
                        continue  # é um modelo (parent), não textura
                    erros.append(f"{n}: {ns}:{cam}")
    if erros:
        print("Texturas ausentes:", *erros, sep="\n  ")
        sys.exit(1)

    SAIDA.parent.mkdir(exist_ok=True)
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, "w", zipfile.ZIP_DEFLATED) as z:
        for n in sorted(arquivos):
            z.writestr(n, arquivos[n])
    SAIDA.write_bytes(buf.getvalue())
    print(f"{len(arquivos)} arquivos -> {SAIDA}")
    if pulados:
        print("Namespaces sem mod instalado (fora do pack):", ", ".join(sorted(pulados)))


if __name__ == "__main__":
    main(sys.argv[1])
