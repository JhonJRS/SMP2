"""Gera e valida a progressão de profissões do SMP 2.

Fonte: smp-profissoes/progressao.json
Saídas:
  - config/rpessentials/rpessentials-professions.toml   (professions / globalBlockedCrafts / professionAllowedCrafts)
  - moonlight-global-datapacks/smp_progressao/          (datapack 100% gerado: aba de pesquisas "Guilda dos Ofícios",
                                                          receitas desativadas, receitas substitutas, loot ajustado)
  - config/smpprofissoes/progressao.json                (regras lidas pelo mod smpprofissoes)
  - smp-profissoes/RELATORIO_GERADO.md

Uso (na pasta da instância):
  python -I smp-profissoes/tools/gerar.py            -> valida e escreve
  python -I smp-profissoes/tools/gerar.py --checar   -> só valida

Regra de vedação: todo item desbloqueado por pesquisa só pode ser produzido por receita de bancada/ferraria
(que o servidor confere contra a profissão e a etapa). Qualquer outra receita que produza o item é desativada,
a menos que ela consuma um ingrediente que já é exclusivo da mesma etapa (aí ela também só existe depois da pesquisa).
Lê apenas os jars instalados; não baixa nada.
"""
import io, json, re, shutil, sys, zipfile
from pathlib import Path

INST = Path(__file__).resolve().parents[2]
SRC = INST / "smp-profissoes" / "progressao.json"
MODS = INST / "mods"
TOML = INST / "config" / "rpessentials" / "rpessentials-professions.toml"
PACK = INST / "moonlight-global-datapacks" / "smp_progressao"
PACK_ANTIGO = INST / "moonlight-global-datapacks" / "profissoes" / "data" / "smpprofissoes"
REGRAS = INST / "config" / "smpprofissoes" / "progressao.json"
RELATORIO = INST / "smp-profissoes" / "RELATORIO_GERADO.md"
MC_EXTRA = Path.home() / "curseforge/minecraft/Install/libraries/net/minecraft/client/1.21.1-20240808.144430/client-1.21.1-20240808.144430-extra.jar"
NEOFORGE = Path.home() / "curseforge/minecraft/Install/libraries/net/neoforged/neoforge/21.1.253/neoforge-21.1.253-universal.jar"

NS = "smpprofissoes"
# Tipos de receita cujos mods leem o JSON sem acesso ao registro (JsonOps puro). O ingrediente neoforge:components
# precisa do registro para ler "items" e, nesses mods, a falha vira exceção que derruba o jogo ao carregar o mundo.
# Conferido por bytecode: AlchemyRecipe$Serializer do Embers usa fromJson/JsonObject próprios.
SEM_COMPONENTES = ("embers:",)
BRANCH_ID = "smpprofissoes:oficios"
# Rotas com contexto de jogador conferidas no servidor: bancada (ResultSlot) e mesa de ferraria (SmithingMenu).
GRID = re.compile(r"(crafting_shaped|crafting_shapeless|crafting_transmute|crafting_special|shaped|shapeless|gear_crafting|compound_part|turn_and_copy|no_container_item|smithing_transform)")

erros, avisos = [], []
NOMES_MC = {}


def desbloqueio_minecolonies():
    """Edifício -> menor nível de universidade em que uma pesquisa da MineColonies o libera (efeito blockhut*)."""
    saida = {}
    for jar in (INST / "mods").glob("minecolonies-*.jar"):
        with zipfile.ZipFile(jar) as z:
            for n in z.namelist():
                if n.startswith("data/minecolonies/researches/") and n.endswith(".json") and "/effects/" not in n:
                    j = json.loads(z.read(n))
                    for e in j.get("effects", []):
                        if e.get("id", "").startswith("minecolonies:effects/blockhut") and "researchLevel" in j:
                            b = "minecolonies:" + e["id"].split("blockhut", 1)[1]
                            saida[b] = min(saida.get(b, 99), j["researchLevel"])
    return saida


DESBLOQUEIO_MC = desbloqueio_minecolonies()


# ----------------------------------------------------------------- índice dos jars
class Indice:
    def __init__(self):
        self.itens = set()
        self.tags = {}            # tag -> set(valores brutos: ids ou #tags)
        self.receitas = {}        # id -> dict(tipo, pasta, saidas, saidas_tag, ent, ent_tag, fonte)
        self.loot = {}            # id da tabela -> (texto json, fonte)
        self.mc_pesquisas = {}
        self.predios = set()
        self.caminhos = set()     # todos os arquivos data/... dos jars
        self.arquivos = {}        # caminho -> texto, só dos arquivos não-receita que podemos editar (rituais da forja)
        self.arquivos = {}        # caminho -> texto, só dos arquivos não-receita que podemos editar (rituais da forja)

    def tag(self, t, vistos=None):
        vistos = vistos or set()
        if t in vistos:
            return set()
        vistos.add(t)
        out = set()
        for v in self.tags.get(t, ()):
            if v.startswith("#"):
                out |= self.tag(v[1:], vistos)
            else:
                out.add(v)
        return out


def _refs(o, chaves_saida=("result", "results", "output", "outputs", "result_item")):
    """Separa referências de item/tag em saídas e entradas."""
    saidas, saidas_tag, ent, ent_tag = [], [], [], []

    def coletar(v, itens, tags):
        if isinstance(v, str):
            if v.startswith("#"):
                tags.append(v[1:])
            elif ":" in v:
                itens.append(v)
        elif isinstance(v, dict):
            for k in ("id", "item"):
                if isinstance(v.get(k), str):
                    itens.append(v[k])
            if isinstance(v.get("tag"), str):
                tags.append(v["tag"])
            for k, x in v.items():
                if k not in ("id", "item", "tag", "count", "chance", "components", "nbt", "amount", "fluid", "type"):
                    coletar(x, itens, tags)
        elif isinstance(v, list):
            for x in v:
                coletar(x, itens, tags)

    def andar(x):
        if isinstance(x, dict):
            for k, v in x.items():
                if k in chaves_saida:
                    coletar(v, saidas, saidas_tag)
                elif k in ("conditions", "neoforge:conditions", "type", "category", "group"):
                    continue
                else:
                    coletar(v, ent, ent_tag)
        elif isinstance(x, list):
            for v in x:
                andar(v)

    andar(o)
    return saidas, saidas_tag, ent, ent_tag


def indexar():
    ix = Indice()

    def varrer(zf, fonte):
        for n in zf.namelist():
            if n.startswith("data/"):
                ix.caminhos.add(n)
                if "/hephaestus_forge/ritual/" in n and n.endswith(".json"):
                    ix.arquivos[n] = zf.read(n).decode("utf-8", "replace")
            m = re.match(r"assets/([^/]+)/models/item/(.+)\.json$", n)
            if m:
                ix.itens.add(f"{m.group(1)}:{m.group(2)}")
                continue
            m = re.match(r"data/([^/]+)/tags/items?/(.+)\.json$", n)
            if m:
                try:
                    obj = json.loads(zf.read(n))
                    vals = []
                    for v in obj.get("values", []):
                        vals.append(v if isinstance(v, str) else v.get("id", ""))
                    ix.tags.setdefault(f"{m.group(1)}:{m.group(2)}", set()).update(x for x in vals if x)
                except Exception:
                    pass
                continue
            m = re.match(r"data/([^/]+)/(recipes?)/(.+)\.json$", n)
            if m:
                try:
                    obj = json.loads(zf.read(n).decode("utf-8", "replace"))
                except Exception:
                    continue
                if not isinstance(obj, dict):
                    continue
                s, st, e, et = _refs(obj)
                rid = f"{m.group(1)}:{m.group(3)}"
                atual = {"tipo": str(obj.get("type", "?")), "pasta": m.group(2), "saidas": s, "saidas_tag": st, "ent": e, "ent_tag": et, "fonte": fonte, "json": obj}
                antigo = ix.receitas.get(rid)
                if antigo is None or (antigo["pasta"] == "recipes" and m.group(2) == "recipe"):
                    ix.receitas[rid] = atual
                continue
            m = re.match(r"data/([^/]+)/(loot_tables?)/(.+)\.json$", n)
            if m:
                ix.loot[(m.group(1), m.group(2), m.group(3))] = (zf.read(n).decode("utf-8", "replace"), fonte)
                continue
            m = re.match(r"data/minecolonies/researches/(.+)\.json$", n)
            if m and "/" in m.group(1) and not m.group(1).startswith("effects/"):
                try:
                    ix.mc_pesquisas["minecolonies:" + m.group(1)] = json.loads(zf.read(n))
                except Exception:
                    pass
                continue
            if n == "assets/minecolonies/lang/en_us.json":
                lang = json.loads(zf.read(n).decode("utf-8"))
                for k, v in lang.items():
                    mm = re.match(r"com\.minecolonies\.research\.(\w+)\.(\w+)\.name$", k)
                    if mm:
                        NOMES_MC[f"minecolonies:{mm.group(1)}/{mm.group(2)}"] = v
                    mm = re.match(r"block\.minecolonies\.blockhut(\w+)$", k)
                    if mm:
                        ix.predios.add("minecolonies:" + mm.group(1))
                        NOMES_MC["minecolonies:" + mm.group(1)] = v
            if n.startswith("META-INF/jarjar/") and n.endswith(".jar"):
                try:
                    varrer(zipfile.ZipFile(io.BytesIO(zf.read(n))), fonte + "!" + n.split("/")[-1])
                except Exception:
                    pass

    for jar in sorted(MODS.glob("*.jar")):
        if jar.name.startswith("smpprofissoes"):
            continue
        try:
            with zipfile.ZipFile(jar) as zf:
                varrer(zf, jar.name)
        except Exception as e:
            avisos.append(f"não consegui ler {jar.name}: {e}")
    if MC_EXTRA.exists():
        with zipfile.ZipFile(MC_EXTRA) as zf:
            varrer(zf, "minecraft")
    else:
        erros.append(f"jar de recursos do Minecraft não encontrado: {MC_EXTRA}")
    if NEOFORGE.exists():  # tags comuns (c:chests, c:leathers...) com os itens vanilla
        with zipfile.ZipFile(NEOFORGE) as zf:
            varrer(zf, "neoforge")
    else:
        erros.append(f"jar do NeoForge não encontrado: {NEOFORGE}")
    return ix


# ----------------------------------------------------------------- semântica do RPEssentials
def rpe_match(item, padrao):
    padrao = padrao.strip()
    if item == padrao:
        return True
    if "*" in padrao:
        return re.fullmatch(padrao.replace(".", "\\.").replace("*", ".*"), item) is not None
    return False


LANG = INST / "smp-profissoes" / "mod" / "src" / "main" / "resources" / "assets" / NS / "lang" / "en_us.json"
MENU = INST / "moonlight-global-datapacks" / "profissoes" / "data" / "profissoes" / "function"
ICONES = {"engenheiro": "⚙", "arcanista": "✦", "ferreiro": "⚒", "naturalista": "❀"}
CORES_MC = {"engenheiro": "gold", "arcanista": "dark_purple", "ferreiro": "red", "naturalista": "green"}


def gerar_menu(profs, licencas):
    """Gera as funções do menu de escolha: profissão (1..4) → mod inicial (10p+k) → confirmação (100+10p+k).
    O menu principal (menu.mcfunction), o tick e o pendente continuam no datapack 'profissoes'."""
    def j(o):
        return json.dumps(o, ensure_ascii=False)
    ids = list(profs)
    sel, esc, conf = ["# GERADO por smp-profissoes/tools/gerar.py — escolha do mod inicial", "playsound minecraft:ui.button.click master @s ~ ~ ~ 1 1", 'tellraw @s {"text":"\\n"}'], \
        ["# GERADO — confirmação do mod inicial", "playsound minecraft:ui.button.click master @s ~ ~ ~ 1 1", 'tellraw @s {"text":"\\n"}'], \
        ["# GERADO — aplica a profissão e o mod inicial escolhidos"]
    tela = []  # a mesma escolha para a tela do mod (cliente/TelaProfissoes.java)
    (MENU / "inicio").mkdir(parents=True, exist_ok=True)
    for f in (MENU / "inicio").glob("*.mcfunction"):
        f.unlink()
    for p_idx, pid in enumerate(ids, start=1):
        p, cor, ico = profs[pid], CORES_MC.get(pid, "white"), ICONES.get(pid, "•")
        tela.append({"id": pid, "nome": p["nome"], "icone": ico, "inicios": []})
        cab = [{"text": "Você escolheu ", "color": "yellow"}, {"text": p["nome"], "color": cor, "bold": True},
               {"text": ". Por qual mod deseja começar? Os outros você libera depois pelas pesquisas da colônia (Guild of Trades).", "color": "yellow"}]
        sel.append(f"execute if score @s prof.escolha matches {p_idx} run tellraw @s {j(cab)}")
        for k, op in enumerate(p["inicios"], start=1):
            v = 10 * p_idx + k
            botao = ["  ", {"text": f"[{ico} {op['rotulo']}]", "color": cor, "bold": True,
                            "clickEvent": {"action": "run_command", "value": f"/trigger prof.escolha set {v}"},
                            "hoverEvent": {"action": "show_text", "contents": op["descricao"]}},
                     {"text": f"  {op['descricao']}", "color": "gray"}]
            sel.append(f"execute if score @s prof.escolha matches {p_idx} run tellraw @s {j(botao)}")
            txt = [{"text": "Você escolheu ", "color": "yellow"}, {"text": p["nome"], "color": cor, "bold": True},
                   {"text": " começando por ", "color": "yellow"}, {"text": op["rotulo"], "color": cor, "bold": True},
                   {"text": ". Essa escolha não pode ser desfeita. Tem certeza?", "color": "red"}]
            esc.append(f"execute if score @s prof.escolha matches {v} run tellraw @s {j(txt)}")
            botoes = ["  ", {"text": "[✔ Confirmar]", "color": "green", "bold": True,
                             "clickEvent": {"action": "run_command", "value": f"/trigger prof.escolha set {100 + v}"},
                             "hoverEvent": {"action": "show_text", "contents": f"{p['nome']} · {op['rotulo']}"}},
                      "   ", {"text": "[✘ Outro mod]", "color": "red", "bold": True,
                              "clickEvent": {"action": "run_command", "value": f"/trigger prof.escolha set {p_idx}"}},
                      "   ", {"text": "[↩ Profissões]", "color": "gray",
                              "clickEvent": {"action": "run_command", "value": "/trigger prof.escolha set 5"}}]
            esc.append(f"execute if score @s prof.escolha matches {v} run tellraw @s {j(botoes)}")
            conf.append(f"execute if score @s prof.escolha matches {100 + v} run function profissoes:inicio/{op['licenca']}")
            tela[-1]["inicios"].append({"licenca": op["licenca"], "rotulo": op["rotulo"], "descricao": op["descricao"],
                                        "codigo": 100 + v, "kit": [it for it, _ in op.get("kit", [])]})
            kit = [f"give @s {it} {n}" for it, n in op.get("kit", [])]  # kit inicial da especialização (mod inicial)
            (MENU / "inicio" / f"{op['licenca']}.mcfunction").write_text("\n".join([
                f"# GERADO — {p['nome']} começando por {op['rotulo']}",
                f"rpessentials license give @s {pid}",
                f"smpprofissoes inicio @s {op['licenca']}",
                f"tag @s add prof.{pid}"] + kit + [
                "function profissoes:finalizar"]) + "\n", encoding="utf-8")
        voltar = ["  ", {"text": "[↩ Voltar]", "color": "gray", "clickEvent": {"action": "run_command", "value": "/trigger prof.escolha set 5"}}]
        sel.append(f"execute if score @s prof.escolha matches {p_idx} run tellraw @s {j(voltar)}")
    (MENU / "selecionar.mcfunction").write_text("\n".join(sel) + "\n", encoding="utf-8")
    (MENU / "escolher_inicio.mcfunction").write_text("\n".join(esc) + "\n", encoding="utf-8")
    (MENU / "confirmar.mcfunction").write_text("\n".join(conf) + "\n", encoding="utf-8")
    (LANG.parents[1] / "tela_profissoes.json").write_text(json.dumps(tela, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


FORMAS_COMPONENTE = {
    # 16x16: '.' vazio, 'o' contorno, 'b' base, 'c' claro, 'w' brilho
    "nucleo": [
        "................", "......oooo......", "....oobbbboo....", "...obbbccbbbo...", "..obbbcwwcbbbo..", "..obbcwwwwcbbo..",
        ".obbbcwwwwcbbbo.", ".obbbbcwwcbbbbo.", ".obbbbbccbbbbbo.", ".obbbbbbbbbbbbo.", "..obbbbbbbbbbo..", "..oobbbbbbbboo..",
        "....oobbbboo....", "......oooo......", "................", "................"],
    "frasco": [
        "................", "......oooo......", "......ocwo......", "......obbo......", ".....oobboo.....", "....obbbbbbo....",
        "...obbccwwbbo...", "..obbcwwwwcbbo..", "..obbcwwwwcbbo..", "..obbbccccbbbo..", "..obbbbbbbbbbo..", "..obbbbbbbbbbo..",
        "...obbbbbbbbo...", "....oooooooo....", "................", "................"],
    "lingote": [
        "................", "................", "................", "................", ".....oooooooo...", "....owwwccccbo..",
        "...owcccccccbbo.", "..obbbbbbbbbbbo.", "..obccccccccbbo.", "..obbbbbbbbbbo..", "..oooooooooooo..", "................",
        "................", "................", "................", "................"],
    "runa": [
        "................", "...oooooooooo...", "..obbbbbbbbbbo..", "..obccbbbbccbo..", "..obbwobbowbbo..", "..obbbwoowbbbo..",
        "..obbbbwwbbbbo..", "..obbbbwwbbbbo..", "..obbbwoowbbbo..", "..obbwobbowbbo..", "..obccbbbbccbo..", "..obbbbbbbbbbo..",
        "...oooooooooo...", "................", "................", "................"],
    "cristal": [
        "................", ".......oo.......", "......owco......", ".....owccbo.....", ".....owccbo.....", "....owcccbbo....",
        "....owcccbbo....", "...owwccccbbo...", "...owccccbbbo...", "...obccccbbbo...", "....obcccbbo....", "....obbcbbbo....",
        ".....obbbbo.....", "......oooo......", "................", "................"],
}


def aplicar_operacoes(original, ops):
    """Edita uma cópia do JSON. Caminho com pontos ("key.M", "pattern.2", "results.0.count").
    definir: põe valor; anexar: acrescenta à lista; remover_onde: tira da lista os elementos cujo JSON contém o texto;
    inserir: põe o valor na posição 'indice' da lista."""
    obj = json.loads(json.dumps(original))
    def pai_e_chave(caminho):
        partes = caminho.split(".")
        alvo = obj
        for p in partes[:-1]:
            alvo = alvo[int(p)] if isinstance(alvo, list) else alvo[p]
        ult = partes[-1]
        return alvo, (int(ult) if isinstance(alvo, list) else ult)
    for op in ops:
        alvo, k = pai_e_chave(op["caminho"])
        if op["op"] == "definir":
            alvo[k] = op["valor"]
        elif op["op"] == "anexar":
            alvo[k].append(op["valor"])
        elif op["op"] == "inserir":
            alvo[k].insert(int(op["indice"]), op["valor"])
        elif op["op"] == "remover_onde":
            antes = len(alvo[k])
            alvo[k] = [x for x in alvo[k] if op["contem"] not in json.dumps(x)]
            if len(alvo[k]) == antes:
                raise ValueError(f"nada contém {op['contem']} em {op['caminho']}")
        else:
            raise ValueError(f"op desconhecida {op['op']}")
    return obj


EQUIPAMENTO = re.compile(r"(helmet|chestplate|leggings|boots|headgear|mask|sword|staff|blade|axe|scythe|bow|spear|wand|hammer|dagger|"
                         r"katana|mace|rapier|halberd|glaive|scepter|cane|spell_?book|greatsword|claymore)$")


def _principal_e_pares(j, tipo):
    """Receita de bancada/ferraria -> (item central, [(ingrediente, quantidade)]); (None, None) se não der."""
    def norm(ing):
        if isinstance(ing, list):
            ing = ing[0] if ing else None
        if not isinstance(ing, dict):
            return None
        return {k: ing[k] for k in ("item", "tag") if k in ing} or None
    pares = []  # (ingrediente, quantidade)
    principal = None
    if tipo == "minecraft:smithing_transform":
        principal = norm(j.get("base"))
        pares = [(norm(j.get("template")), 1), (norm(j.get("addition")), 1)]
    elif tipo == "minecraft:crafting_shaped":
        cont = {}
        for linha in j.get("pattern", []):
            for ch in linha:
                if ch != " ":
                    cont[ch] = cont.get(ch, 0) + 1
        pares = [(norm(j["key"][ch]), n) for ch, n in cont.items()]
    elif tipo == "minecraft:crafting_shapeless":
        for ing in j.get("ingredients", []):
            pares.append((norm(ing), 1))
    else:
        return None, None
    if principal is None and tipo != "minecraft:smithing_transform":
        def chave(par):
            ing, n = par
            nome = ing.get("item", "") if ing else ""
            return (0 if EQUIPAMENTO.search(nome) else 1, 0 if n == 1 else 1)
        pares.sort(key=chave)
        principal, n0 = pares[0]
        pares = ([(principal, n0 - 1)] if n0 > 1 else []) + pares[1:]
    if principal is None or any(i is None for i, _ in pares):
        return None, None
    return principal, pares


def ritual_de_receita(j, tipo, saida, conv, comps):
    """Converte uma receita de bancada/ferraria em ritual da Forja de Hefesto (None se não couber em 8 pedestais)."""
    principal, pares = _principal_e_pares(j, tipo)
    if principal is None:
        return None
    agreg = {}
    for ing, n in pares:
        k = json.dumps(ing, sort_keys=True)
        agreg[k] = agreg.get(k, 0) + n
    componente_conv, tier_min = conv.get("componente"), 0
    textos_ing = json.dumps([p for p, _ in pares] + [principal])
    for esp in conv.get("componente_especial", []):  # ex.: Deus Essence/Divine Mold -> Archmage Codex e tier 5
        if re.search(esp["padrao"], textos_ing):
            componente_conv, tier_min = esp["componente"], esp.get("tier", 0)
            break
    if componente_conv:
        agreg[json.dumps({"item": f"{NS}:{componente_conv}"}, sort_keys=True)] = 1
    while sum(agreg.values()) > 8:
        k = max(agreg, key=agreg.get)
        if agreg[k] <= 1:
            return None
        agreg[k] -= 1
    textos = " ".join(list(agreg) + [json.dumps(principal)])
    tier = conv["tier_smithing"] if tipo == "minecraft:smithing_transform" else conv["tier_padrao"]
    for regra in conv.get("tier_por_ingrediente", []):
        if re.search(regra["padrao"], textos):
            tier = max(tier, regra["tier"])
    tier = max(tier, tier_min)
    return {"essences": conv["essencias"][str(tier)], "forge_tier": tier,
            "inputs": [{"amount": n, "ingredient": json.loads(k)} for k, n in agreg.items()],
            "magic_circle": "forbidden_arcanus:create_item", "main_ingredient": principal,
            "result": {"type": "forbidden_arcanus:create_item", "result_item": {"count": 1, "id": saida}}}


def infusao_de_receita(j, tipo, saida, conv):
    """Converte uma receita de bancada/ferraria em infusão do Altar Espiritual do Malum (None se não der).
    Espíritos: os de base + os das regras cujo padrão aparece nos ingredientes ou no item final."""
    principal, pares = _principal_e_pares(j, tipo)
    if principal is None:
        return None
    agreg = {}
    for ing, n in pares:
        if n > 0:
            k = json.dumps(ing, sort_keys=True)
            agreg[k] = agreg.get(k, 0) + n
    textos = json.dumps([principal] + [p for p, _ in pares]) + " " + saida
    espiritos = dict(conv["espiritos_base"])
    for regra in conv.get("espiritos_por_padrao", []):
        if re.search(regra["padrao"], textos):
            espiritos[regra["espirito"]] = espiritos.get(regra["espirito"], 0) + regra["quantidade"]
    original = j.get("result") if isinstance(j.get("result"), dict) else {}
    resultado = {"count": 1, "id": saida}
    if original.get("components"):  # ex.: spell_container das armaduras de cristal
        resultado["components"] = original["components"]
    obj = {"type": "malum:spirit_infusion", "input": {"count": 1, **principal},
           "extraInputs": [{"count": n, **json.loads(k)} for k, n in agreg.items()],
           "result": resultado, "spirits": [{"type": t, "count": n} for t, n in espiritos.items()]}
    if tipo == "minecraft:smithing_transform":
        obj["carryOverComponentData"] = True  # a peça-base mantém encantamentos e magias gravadas
    return obj


def prisma_elemental(obj, cfg_prismas):
    """Põe no ritual o prisma do elemento do item (fogo, natureza, eletricidade, água/gelo). Devolve o prisma ou None.
    O elemento é o que mais aparece no nome do item e nos ingredientes (runas, materiais)."""
    saida = obj["result"]["result_item"]["id"]
    textos = (saida + " " + json.dumps(obj.get("main_ingredient", {})) + " " + json.dumps(obj.get("inputs", []))).lower()
    pontos = []
    for el in cfg_prismas["elementos"]:
        n = len(re.findall(el["padrao"], saida.split(":", 1)[1])) * 2 + len(re.findall(el["padrao"], textos))
        if n:
            pontos.append((n, -cfg_prismas["elementos"].index(el), el["prisma"]))
    if not pontos:
        return None
    prisma = max(pontos)[2]
    if prisma in json.dumps(obj.get("inputs", [])):
        return prisma
    while sum(int(i.get("amount", 1)) for i in obj["inputs"]) >= 8:  # abre um pedestal tirando do maior monte
        maior = max(obj["inputs"], key=lambda i: int(i.get("amount", 1)))
        if int(maior.get("amount", 1)) <= 1:
            return "SEM_ESPACO"  # 8 ingredientes diferentes: não dá para pôr o prisma sem perder um deles
        maior["amount"] = int(maior["amount"]) - 1
    obj["inputs"].append({"amount": 1, "ingredient": {"item": prisma}})
    return prisma


def com_condicao_itens(obj):
    """Rituais da Forja são dados de REGISTRO: um item inexistente faz o jogo recusar o datapack inteiro (mundo não abre;
    26/10: Starweave do Simply Bows tinha modelo mas não era registrado). Com neoforge:item_exists, o ritual com item
    ausente é só ignorado."""
    itens = []
    def coletar(o):
        if isinstance(o, dict):
            for k, v in o.items():
                if k in ("item", "id") and isinstance(v, str) and ":" in v and not o.get("amount") and v not in itens:
                    itens.append(v)
                else:
                    coletar(v)
        elif isinstance(o, list):
            for v in o:
                coletar(v)
    coletar(obj)
    itens = [i for i in itens if not i.startswith(("minecraft:", NS + ":", "forbidden_arcanus:create_item"))]
    if not itens:
        return obj
    return {"neoforge:conditions": [{"type": "neoforge:item_exists", "item": i} for i in itens], **obj}


def com_componente(original, cm):
    """Cópia da receita de máquina exigindo o componente cm ({"id", "quantidade"}); None se o tipo não é suportado."""
    obj = json.loads(json.dumps(original))
    item, q = f"{NS}:{cm['id']}", int(cm.get("quantidade", 1))
    tipo = obj.get("type", "")
    if tipo == "malum:spirit_infusion" and isinstance(obj.get("extraInputs"), list):
        obj["extraInputs"].append({"count": q, "item": item})
    elif tipo == "malum:runeworking":
        obj["input"] = {"count": q, "item": item}
    elif tipo == "neovitae:ara_vitae_recipe" and isinstance(obj.get("input"), dict):
        obj["input"] = {"item": item}  # o Ara Vitae aceita um item só: o componente já leva o ingrediente original
    elif tipo == "embers:alchemy" and isinstance(obj.get("inputs"), list):
        obj["inputs"] += [{"item": item}] * q  # item simples: o parser do Embers não aceita neoforge:components
    elif tipo == "neovitae:hellfire_forge_transform" and isinstance(obj.get("catalysts"), list):
        obj["catalysts"] += [{"item": item}] * q
    elif tipo == "create:sequenced_assembly" and isinstance(obj.get("sequence"), list) and isinstance(obj.get("transitional_item"), dict):
        trans = obj["transitional_item"].get("id") or obj["transitional_item"].get("item")
        passo = {"type": "create:deploying", "ingredients": [{"item": trans}, {"item": item}], "results": [{"id": trans}]}
        obj["sequence"] = [passo] * q + obj["sequence"]  # primeiro passo: o componente na peça em montagem
    elif tipo == "create:mechanical_crafting" and isinstance(obj.get("pattern"), list) and isinstance(obj.get("key"), dict):
        letra = next(ch for ch in "Z9876543210" if ch not in obj["key"])
        linhas, w = list(obj["pattern"]), max(len(l) for l in obj["pattern"])
        linhas = [l.ljust(w) for l in linhas]
        for _ in range(q):
            vaga = next(((i, j) for i, l in enumerate(linhas) for j, ch in enumerate(l) if ch == " "), None)
            if vaga:
                i, j = vaga
                linhas[i] = linhas[i][:j] + letra + linhas[i][j + 1:]
            else:  # grade cheia: uma linha a mais (a mesa mecânica vai até 9x9)
                linhas.append(letra + " " * (w - 1))
        obj["pattern"] = linhas
        obj["key"][letra] = {"item": item}
    elif isinstance(obj.get("ingredients"), list) and not tipo.startswith("minecraft:crafting") and \
            (not tipo.startswith("create:") or tipo in ("create:mixing", "create:compacting")):  # deploying/assembly têm nº fixo
        obj["ingredients"] += [{"item": item}] * q
    else:
        return None
    return obj


def _png(caminho, pixels):
    """Grava PNG RGBA 16x16 sem bibliotecas externas."""
    import struct
    import zlib
    bruto = b"".join(b"\x00" + b"".join(bytes(p) for p in linha) for linha in pixels)
    def bloco(tipo, dados):
        return struct.pack(">I", len(dados)) + tipo + dados + struct.pack(">I", zlib.crc32(tipo + dados) & 0xFFFFFFFF)
    png = b"\x89PNG\r\n\x1a\n" + bloco(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0)) + \
        bloco(b"IDAT", zlib.compress(bruto, 9)) + bloco(b"IEND", b"")
    caminho.parent.mkdir(parents=True, exist_ok=True)
    caminho.write_bytes(png)


MENSAGENS_ESPECIAIS = {  # textos dos itens especiais (Java usa estas chaves)
    "key.categories.smpprofissoes": "SMP 2",
    "key.smpprofissoes.painel_adm": "Admin panel (operators)",
    "smpprofissoes.carta.max": "You already have two professions.",
    "smpprofissoes.carta.sem": "Choose your first profession before using a Guild Charter.",
    "smpprofissoes.carta.escolha": "Choose your second profession:",
    "smpprofissoes.carta.ok": "You joined a second guild: %s. Now choose its starting mod.",
    "smpprofissoes.esquecer.ok": "Your professions fade from memory. Choose again.",
    "smpprofissoes.carta_apresentacao.ja": "You already belong to a guild.",
}


def escrever_recursos_componentes(comps, especiais=None):
    """Listas, modelos e texturas dos componentes e itens especiais dentro dos recursos do mod (o build.ps1 empacota)."""
    especiais = especiais or {}
    base = LANG.parents[1]  # .../assets/smpprofissoes
    (base / "componentes.json").write_text(json.dumps(sorted(comps), indent=2) + "\n", encoding="utf-8")
    (base / "especiais.json").write_text(json.dumps(sorted(especiais), indent=2) + "\n", encoding="utf-8")
    for d in ("models/item", "textures/item"):
        for velho in (base / d).glob("*"):
            velho.unlink()
    for cid, c in list(comps.items()) + list(especiais.items()):
        (base / "models/item").mkdir(parents=True, exist_ok=True)
        (base / "models/item" / f"{cid}.json").write_text(json.dumps(
            {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{cid}"}}, indent=2) + "\n", encoding="utf-8")
        h = c["cor"].lstrip("#")
        r, g, b = int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16)
        def mix(f, alvo):
            return tuple(int(x + (alvo - x) * f) for x in (r, g, b))
        try:  # ícone animado próprio (tools/icones.py, precisa do Pillow)
            import icones
            icones.escrever(base / "textures/item", cid, c["cor"], FORMAS_COMPONENTE[c["forma"]])
            continue
        except ImportError:
            avisos.append("Pillow ausente: ícones estáticos")
        pal = {".": (0, 0, 0, 0), "o": (*mix(0.65, 0), 255), "b": (r, g, b, 255), "c": (*mix(0.4, 255), 255), "w": (*mix(0.8, 255), 255)}
        _png(base / "textures/item" / f"{cid}.png", [[pal[ch] for ch in linha] for linha in FORMAS_COMPONENTE[c["forma"]]])


def main(cfg=None, ix=None, analise=False):
    """analise=True: não escreve nada e devolve {'adiantados', 'dono', 'pesq_etapa', 'etapas', 'erros'} (usado pelas migrações)."""
    erros.clear()
    avisos.clear()
    so_checar = "--checar" in sys.argv
    cfg = cfg if cfg is not None else json.loads(SRC.read_text(encoding="utf-8"))
    ix = ix if ix is not None else indexar()
    profs = cfg["profissoes"]
    aba = cfg["aba"]

    # ------------------------------------------------ pesquisas (aba própria)
    nossas = {p["id"]: p for p in cfg["pesquisas"]}
    if len(nossas) != len(cfg["pesquisas"]):
        erros.append("IDs de pesquisa duplicados")
    for pid, p in nossas.items():
        if not pid.startswith(BRANCH_ID + "/"):
            erros.append(f"{pid}: ID deve começar com {BRANCH_ID}/")
        pai = p.get("pai")
        if pai:
            if pai not in nossas:
                erros.append(f"{pid}: pai {pai} não está na aba")
            elif p["nivel"] != nossas[pai]["nivel"] + 1:  # a MineColonies descarta a pesquisa se pular nível (testado em jogo)
                erros.append(f"{pid}: nível {p['nivel']} deveria ser {nossas[pai]['nivel'] + 1} (para pular um nível, "
                             f"pendure num pai do nível anterior e ponha a outra pesquisa em pesquisas_extra)")
        elif p["nivel"] != 1:
            erros.append(f"{pid}: raiz precisa ter nível 1")
        if not 1 <= p["nivel"] <= 5:
            erros.append(f"{pid}: nível fora de 1..5")
        for b, lv in p.get("edificios", {}).items():
            if b not in ix.predios:
                erros.append(f"{pid}: edifício inexistente {b}")
            if not 1 <= lv <= 5:
                erros.append(f"{pid}: nível de edifício inválido {b}={lv}")
            if lv > p["nivel"]:  # regra do usuário: universidade N só pede prédios até N
                erros.append(f"{pid}: pesquisa nível {p['nivel']} exige {b} nível {lv} (máximo {p['nivel']})")
            # regra de 28/10: cabana liberada por pesquisa da MineColonies só pode ser exigida a partir daquele nível
            if DESBLOQUEIO_MC.get(b, 0) > p["nivel"]:
                erros.append(f"{pid}: pesquisa nível {p['nivel']} exige {b}, que a MineColonies só libera no nível "
                             f"{DESBLOQUEIO_MC[b]} da universidade")
        for it in list(p.get("custos", {})) + [p.get("icone")]:
            if it not in ix.itens:
                erros.append(f"{pid}: item inexistente {it}")
        for r in p.get("pesquisas_extra", []):
            if r not in nossas:
                erros.append(f"{pid}: pesquisa extra {r} não está na aba")
            elif r == pid:
                erros.append(f"{pid}: não pode exigir a si mesma")
            elif nossas[r]["nivel"] >= p["nivel"]:  # regra de 28/10: pesquisa exigida é de nível anterior
                erros.append(f"{pid}: exige {r}, de nível {nossas[r]['nivel']} (precisa ser menor que {p['nivel']})")
    estado = {}
    def dependencias(n):
        p = nossas[n]
        return ([p["pai"]] if p.get("pai") else []) + p.get("pesquisas_extra", [])
    def dfs(n, pilha):
        estado[n] = 1
        for d in dependencias(n):
            if d not in nossas:
                continue
            if estado.get(d) == 1:
                erros.append("dependência circular: " + " -> ".join(pilha + [n, d]))
            elif d not in estado:
                dfs(d, pilha + [n])
        estado[n] = 2
    for n in nossas:
        if n not in estado:
            dfs(n, [])

    def ancestrais(pid, _visto=None):
        """A pesquisa + tudo que precisa estar concluído antes dela (pais e pesquisas extras, recursivamente)."""
        vis = _visto if _visto is not None else set()
        if pid in vis or pid not in nossas:
            return vis
        vis.add(pid)
        for d in dependencias(pid):
            ancestrais(d, vis)
        return vis

    # ------------------------------------------------ profissões: mods iniciais
    ns_de_prof, inicial = {}, {}
    for pid, p in profs.items():
        for ns in p["namespaces"]:
            if ns in ns_de_prof:
                erros.append(f"namespace {ns} em duas profissões")
            ns_de_prof[ns] = pid
        inicial[pid] = set(p.get("inicial", []))
        for ns in inicial[pid]:
            if ns not in p["namespaces"]:
                erros.append(f"{pid}: mod inicial {ns} não está nos namespaces da profissão")

    # ------------------------------------------------ etapas: itens explícitos + resto dos mods
    licencas, dono, explicito, so_saque, avulsos = {}, {}, set(), set(), set()
    resto_dono = {}
    for e in cfg["etapas"]:
        lic = e["licenca"]
        if lic in licencas or lic in profs:
            erros.append(f"licença duplicada {lic}")
        pid = e["profissao"]
        if pid not in profs:
            erros.append(f"{lic}: profissão inexistente")
            continue
        for r in e["pesquisas"]:
            if r not in nossas:
                erros.append(f"{lic}: pesquisa {r} não está na aba")
        for b in e.get("edificios", {}):
            if b not in ix.predios:
                erros.append(f"{lic}: edifício inexistente {b}")
        achados_total = set()
        for pad in e.get("itens", []):
            ns = pad.split(":", 1)[0]
            if ns_de_prof.get(ns) != pid:
                erros.append(f"{lic}: {pad} fora dos mods da profissão")
            achados = {i for i in ix.itens if i.startswith(ns + ":") and rpe_match(i, pad)}
            if not achados:
                erros.append(f"{lic}: padrão {pad} não corresponde a nenhum item instalado")
            achados_total |= achados
        # itens avulsos: itens soltos de qualquer mod que passam a ser desta profissão (ex.: tudo de poção é do Naturalista)
        for pad in e.get("itens_avulsos", []):
            ns = pad.split(":", 1)[0]
            achados = {i for i in ix.itens if i.startswith(ns + ":") and rpe_match(i, pad)}
            if not achados:
                erros.append(f"{lic}: item avulso {pad} não corresponde a nenhum item instalado")
            if ns in ns_de_prof and ns_de_prof[ns] == pid:
                erros.append(f"{lic}: {pad} já é de um mod da profissão; use 'itens'")
            achados_total |= achados
            avulsos |= achados
        achados_total -= {i for i in achados_total for pad in e.get("excluir", []) if rpe_match(i, pad)}  # tirados de um padrão
        livres_vedacao = {i for i in achados_total for pad in e.get("sem_vedacao", []) if rpe_match(i, pad)}
        # itens que de propósito só vêm de saque/comércio: receitas de outros mods que os produzem são vedadas
        so_saque |= {i for i in achados_total for pad in e.get("so_saque", []) if rpe_match(i, pad)}
        for i in achados_total:
            if i in dono:
                erros.append(f"item {i} em duas etapas: {dono[i]} e {lic}")
            dono[i] = lic
            if i not in livres_vedacao:
                explicito.add(i)
        for ns in e.get("resto_de", []):
            if ns_de_prof.get(ns) != pid:
                erros.append(f"{lic}: resto_de {ns} não é mod da profissão")
            if ns in inicial[pid]:
                erros.append(f"{lic}: resto_de {ns} é mod inicial (já liberado)")
            if ns in resto_dono:
                erros.append(f"{lic}: resto_de {ns} já pertence a {resto_dono[ns]}")
            resto_dono[ns] = lic
        licencas[lic] = (e, achados_total)
    for i in ix.itens:  # resto: itens dos mods não-iniciais que ninguém reivindicou
        ns = i.split(":", 1)[0]
        if ns in resto_dono and i not in dono:
            dono[i] = resto_dono[ns]
            licencas[resto_dono[ns]][1].add(i)
    for ns, pid in ns_de_prof.items():  # cobertura: todo mod não-inicial tem que entrar por alguma etapa
        if ns not in inicial[pid]:
            soltos = [i for i in ix.itens if i.startswith(ns + ":") and i not in dono]
            if soltos:
                erros.append(f"{ns} ({pid}) não é inicial e {len(soltos)} item(ns) não pertencem a nenhuma etapa: use resto_de")
    licencas = {k: (v[0], sorted(v[1])) for k, v in licencas.items()}

    # ------------------------------------------------ mods iniciais escolhíveis
    for pid, p in profs.items():
        if not p.get("inicios"):
            erros.append(f"{pid}: nenhuma opção de mod inicial")
        for op in p.get("inicios", []):
            e = licencas.get(op["licenca"], (None,))[0]
            if not e:
                erros.append(f"{pid}: início {op['licenca']} não é uma etapa")
            elif e["profissao"] != pid or not e.get("inicio"):
                erros.append(f"{pid}: início {op['licenca']} precisa ser etapa da profissão com 'inicio': true")
            for it, n in op.get("kit", []):
                if it not in ix.itens or not 1 <= int(n) <= 64:
                    erros.append(f"{pid}: kit de {op['licenca']}: item inexistente ou quantidade inválida ({it} x{n})")
    for lic, (e, _) in licencas.items():
        if e.get("inicio") and not any(op["licenca"] == lic for op in profs[e["profissao"]].get("inicios", [])):
            erros.append(f"{lic}: marcada como inicio mas não aparece no menu da profissão")

    # ------------------------------------------------ componentes (pesquisas cruzadas) e receitas alteradas
    # Componente = item do mod smpprofissoes, feito na bancada só por quem tem a licença da pesquisa (Guarda + receitasComLicenca).
    # Entra nas receitas de máquina/ritual, que não sabem quem é o jogador. '@comp:<id>' vira {"item": "smpprofissoes:<id>"}.
    comps = {c["id"]: c for c in cfg.get("componentes", [])}
    especiais = {c["id"]: c for c in cfg.get("itens_especiais", [])}  # itens do mod com comportamento próprio (não são componentes)
    for cid in list(comps) + list(especiais):
        ix.itens.add(f"{NS}:{cid}")
    def expandir(o, origem):
        if isinstance(o, str) and o.startswith("@comp:"):
            cid = o[6:]
            if cid not in comps:
                erros.append(f"{origem}: componente inexistente {cid}")
            return {"item": f"{NS}:{cid}"}
        if isinstance(o, dict) and "@comp" in o:  # forma "plana" (SizedIngredient): {"count": n, "@comp": id}
            cid = o["@comp"]
            if cid not in comps:
                erros.append(f"{origem}: componente inexistente {cid}")
            return {**{k: v for k, v in o.items() if k != "@comp"}, "item": f"{NS}:{cid}"}
        if (isinstance(o, str) and o.startswith("@selo:")) or (isinstance(o, dict) and "@selo" in o):
            erros.append(f"{origem}: selos foram substituídos por componentes (@comp:<id>)")
            return o
        if isinstance(o, dict):
            return {k: expandir(v, origem) for k, v in o.items()}
        if isinstance(o, list):
            return [expandir(v, origem) for v in o]
        return o
    receitas_comp, licenca_receita = {}, {}
    for cid, c in comps.items():
        if not re.fullmatch(r"[a-z0-9_]+", cid):
            erros.append(f"componente {cid}: id inválido")
        for l in c["licencas"]:
            if l not in licencas:
                erros.append(f"componente {cid}: licença inexistente {l}")
        if not 1 <= len(c["ingredientes"]) <= 9:
            erros.append(f"componente {cid}: 1..9 ingredientes")
        if c.get("forma") not in FORMAS_COMPONENTE or any(len(l) != 16 for l in FORMAS_COMPONENTE.get(c.get("forma"), [])):
            erros.append(f"componente {cid}: forma {c.get('forma')} (use {', '.join(FORMAS_COMPONENTE)})")
        for ing in c["ingredientes"]:
            if not ing.startswith(("@comp:", "#")) and ing not in ix.itens:
                erros.append(f"componente {cid}: item inexistente {ing}")
        rid = f"{NS}:componente/{cid}"
        receitas_comp[rid] = {
            "type": "minecraft:crafting_shapeless", "category": "misc",
            "ingredients": [expandir(i, f"componente {cid}") if i.startswith("@comp:")
                            else ({"tag": i[1:]} if i.startswith("#") else {"item": i}) for i in c["ingredientes"]],
            "result": {"id": f"{NS}:{cid}", "count": c.get("quantidade", 1)}}
        licenca_receita[rid] = c["licencas"]
    for lic, (e, _) in licencas.items():
        cm = e.get("componente_maquina")
        if cm and (cm.get("id") not in comps or lic not in comps[cm["id"]]["licencas"]):
            erros.append(f"{lic}: componente_maquina {cm.get('id')} precisa existir e ser fabricado com a licença {lic}")
    def itens_declarados(o, chave=None):  # só valores de "item"/"id" (afinidades, fluidos e círculos não são itens)
        if isinstance(o, dict):
            for k, v in o.items():
                if k in ("item", "id") and isinstance(v, str) and not o.get("amount"):
                    yield v
                elif k == "result_item" and isinstance(v, str):
                    yield v
                else:
                    yield from itens_declarados(v, k)
        elif isinstance(o, list):
            for v in o:
                yield from itens_declarados(v, chave)
    def validar_ritual(obj, origem):
        """Forja de Hefesto: 8 pedestais, tier 1..5, essências conhecidas, item central e resultado existentes."""
        total = sum(int(i.get("amount", 1)) for i in obj.get("inputs", []))
        if total > 8:
            erros.append(f"{origem}: {total} itens nos pedestais (a forja só tem 8)")
        if not 1 <= int(obj.get("forge_tier", 1)) <= 5:  # sem o campo, a forja usa tier 1
            erros.append(f"{origem}: forge_tier fora de 1..5")
        for k in obj.get("essences", {}):
            if k not in ("aureal", "blood", "souls", "experience"):
                erros.append(f"{origem}: essência desconhecida {k}")
        if "main_ingredient" not in obj or "result" not in obj:
            erros.append(f"{origem}: falta main_ingredient/result")
        for i in itens_declarados(obj):
            if i not in ix.itens:
                erros.append(f"{origem}: item inexistente {i}")
    modificadas = {}
    for m in cfg.get("receitas_modificadas", []):
        rid, caminho = m["id"], m["caminho"]
        eh_receita = "/recipe/" in caminho
        if eh_receita:
            orig = ix.receitas.get(rid)
            if not orig or orig["pasta"] != "recipe":
                erros.append(f"receita alterada {rid}: original não encontrada nos jars")
                continue
        elif caminho not in ix.caminhos:
            erros.append(f"arquivo alterado {caminho}: original não encontrado nos jars")
            continue
        if m.get("operacoes"):  # edições cirúrgicas na receita original (ver aplicar_operacoes)
            orig_json = ix.receitas[rid]["json"] if eh_receita else json.loads(ix.arquivos[caminho])
            try:
                obj = aplicar_operacoes(orig_json, m["operacoes"])
            except (KeyError, IndexError, TypeError, ValueError) as ex:
                erros.append(f"receita alterada {rid}: operação inválida ({ex})")
                continue
        elif m.get("copiar_original"):
            obj = json.loads(json.dumps(ix.receitas[rid]["json"]))
            sub = m["substituir_primeiro"]
            lista = obj.get(sub["campo"])
            if not isinstance(lista, list) or not lista:
                erros.append(f"receita alterada {rid}: campo {sub['campo']} não é lista")
                continue
            lista[0] = sub["por"]
        else:
            obj = m["json"]
        obj = expandir(obj, f"receita alterada {rid}")
        for i in itens_declarados(obj):
            if i not in ix.itens:
                erros.append(f"receita alterada {rid}: item inexistente {i}")
        usa_comp = any(i.startswith(NS + ":") for i in itens_declarados(obj))
        tipo = str(obj.get("type", ""))
        if "neoforge:components" in json.dumps(obj) and tipo.startswith(SEM_COMPONENTES):
            erros.append(f"receita alterada {rid}: o tipo {tipo} lê receitas sem registro e NÃO aceita neoforge:components (derruba o jogo)")
        if not usa_comp and not m.get("_comentario"):
            erros.append(f"receita alterada {rid}: não exige componente (documente o cruzamento em _comentario)")
        if "hephaestus_forge/ritual" in caminho:
            validar_ritual(obj, f"ritual alterado {rid}")
        modificadas[rid] = (caminho, obj, eh_receita)

    # ------------------------------------------------ receitas removidas, receitas novas e rituais novos
    removidas = {}
    for rid in cfg.get("receitas_removidas", []):
        r = ix.receitas.get(rid)
        if not r or r["pasta"] != "recipe":
            erros.append(f"receita removida {rid}: não existe nos jars")
            continue
        removidas[rid] = r
    def validar_tags(obj, origem):
        for t in re.findall(r'"tag": "([^"#]+)"', json.dumps(obj)):
            if not ix.tag(t) and not t.startswith(("c:water", "minecraft:water")):
                erros.append(f"{origem}: tag vazia ou inexistente #{t}")
    novas, rotas_extra = {}, {}
    for n in cfg.get("receitas_novas", []):
        rid = n["id"]
        if not rid.startswith(NS + ":") or rid in ix.receitas:
            erros.append(f"receita nova {rid}: precisa ser {NS}:... e não pode sobrescrever outra")
        obj = expandir(n["json"], f"receita nova {rid}")
        tipo = str(obj.get("type", ""))
        if tipo.startswith(SEM_COMPONENTES) and "neoforge:components" in json.dumps(obj):
            erros.append(f"receita nova {rid}: tipo {tipo} não aceita selo")
        for i in itens_declarados(obj):
            if i not in ix.itens:
                erros.append(f"receita nova {rid}: item inexistente {i}")
        validar_tags(obj, f"receita nova {rid}")
        s, _, _, _ = _refs(obj)
        for i in s:
            rotas_extra.setdefault(i, []).append(f"{tipo} ({rid})")
        novas[rid] = obj
    rituais = {}
    for r in cfg.get("rituais_novos", []):
        rid = r["id"]
        if not re.fullmatch(r"[a-z0-9_/]+", rid):
            erros.append(f"ritual novo {rid}: id inválido")
        obj = expandir(r["json"], f"ritual novo {rid}")
        validar_ritual(obj, f"ritual novo {rid}")
        validar_tags(obj, f"ritual novo {rid}")
        saida = obj.get("result", {}).get("result_item", {})
        saida = saida.get("id") if isinstance(saida, dict) else saida
        rotas_extra.setdefault(saida, []).append(f"Forja de Hefesto ({NS}:{rid})")
        rituais[rid] = obj
    feitos_por_ritual = {s for s in rotas_extra}

    # conversões automáticas para a Forja de Hefesto: cada receita de bancada/ferraria dos itens escolhidos vira ritual
    for conv in cfg.get("conversoes_forja", []):
        rx_saida = re.compile(conv["saidas"])
        convertidos = 0
        for rid_o, r in sorted(ix.receitas.items()):
            if r["pasta"] != "recipe" or r["tipo"] not in conv["tipos"] or rid_o in removidas:
                continue
            alvo = [s for s in r["saidas"] if rx_saida.search(s)]
            if len(alvo) != 1 or alvo[0] in feitos_por_ritual:
                continue
            obj = ritual_de_receita(r["json"], r["tipo"], alvo[0], conv, comps)
            if obj is None:
                avisos.append(f"conversão {conv['id']}: não consegui converter {rid_o}")
                continue
            rid = f"auto/{conv['id']}/{alvo[0].split(':', 1)[1]}"
            validar_ritual(obj, f"ritual automático {rid}")
            rituais[rid] = obj
            removidas[rid_o] = r
            rotas_extra.setdefault(alvo[0], []).append(f"Forja de Hefesto tier {obj['forge_tier']} ({NS}:{rid})")
            convertidos += 1
        if not convertidos:
            erros.append(f"conversão {conv['id']}: nenhuma receita convertida (padrão {conv['saidas']})")

    # conversões para o Altar Espiritual do Malum: a receita de bancada/ferraria vira infusão (ex.: equipamento de cristal)
    for conv in cfg.get("conversoes_malum", []):
        rx_saida = re.compile(conv["saidas"])
        convertidos = 0
        for rid_o, r in sorted(ix.receitas.items()):
            if r["pasta"] != "recipe" or r["tipo"] not in conv["tipos"] or rid_o in removidas:
                continue
            alvo = [s for s in r["saidas"] if rx_saida.search(s)]
            if len(alvo) != 1 or alvo[0] in rotas_extra:
                continue
            obj = infusao_de_receita(r["json"], r["tipo"], alvo[0], conv)
            if obj is None:
                avisos.append(f"conversão malum {conv['id']}: não consegui converter {rid_o}")
                continue
            for i in itens_declarados(obj):
                if i not in ix.itens:
                    erros.append(f"conversão malum {rid_o}: item inexistente {i}")
            rid = f"{NS}:auto/malum/{alvo[0].split(':', 1)[1]}"
            novas[rid] = obj
            removidas[rid_o] = r
            rotas_extra.setdefault(alvo[0], []).append(f"Altar Espiritual do Malum ({rid})")
            convertidos += 1
        if not convertidos:
            erros.append(f"conversão malum {conv['id']}: nenhuma receita convertida (padrão {conv['saidas']})")

    # prisma do elemento nos rituais de armadura/cajado (fogo → Smelter, natureza → Terrastomp, raio → Whirlwind, água/gelo → Sea)
    cfg_prismas = cfg.get("prismas_elementais")
    if cfg_prismas:
        rx_prisma = re.compile(cfg_prismas["saidas"])
        com_prisma = 0
        for rid, obj in rituais.items():
            if not rx_prisma.search(obj["result"]["result_item"]["id"]):
                continue
            p = prisma_elemental(obj, cfg_prismas)
            if p == "SEM_ESPACO":
                avisos.append(f"ritual {rid}: 8 ingredientes diferentes, sem pedestal para o prisma")
            elif p:
                com_prisma += 1
        if not com_prisma:
            erros.append("prismas_elementais: nenhum ritual recebeu prisma")
        avisos.append(f"prismas elementais em {com_prisma} ritual(is) de armadura/cajado")

    # etapa → conjunto de pesquisas efetivas (pesquisa + pais)
    pesq_etapa = {lic: set().union(*[ancestrais(r) for r in e["pesquisas"]]) for lic, (e, _) in licencas.items()}

    # custos de pesquisa precisam ser obtíveis antes da própria pesquisa
    for pid, p in nossas.items():
        antes = ancestrais(pid) - {pid}
        for it in p.get("custos", {}):
            lic = dono.get(it)
            if lic and not pesq_etapa[lic] <= antes:
                erros.append(f"{pid}: custo {it} é da etapa {lic}, que só existe depois desta pesquisa")

    # ------------------------------------------------ receitas substitutas
    substitutas = {}
    for s in cfg.get("receitas_substitutas", []):
        rid = s["id"]
        if not rid.startswith(NS + ":"):
            erros.append(f"receita substituta {rid} precisa do namespace {NS}")
        saidas, saidas_tag, ent, ent_tag = _refs(s["json"])
        for i in saidas + ent:
            if i not in ix.itens:
                erros.append(f"receita substituta {rid}: item inexistente {i}")
        for t in ent_tag:
            if not ix.tag(t):
                erros.append(f"receita substituta {rid}: tag vazia ou inexistente #{t}")
        if not GRID.search(s["json"].get("type", "")):
            erros.append(f"receita substituta {rid} precisa ser de bancada/ferraria")
        substitutas[rid] = (s, saidas)

    # ------------------------------------------------ vedação dos itens explícitos
    def saidas_resolvidas(r):
        out = set(r["saidas"])
        for t in r["saidas_tag"]:
            out |= ix.tag(t)
        return out

    def consome_item_da_etapa(r, lic):
        for i in r["ent"]:
            if dono.get(i) == lic:
                return True
        for t in r["ent_tag"]:
            itens_t = ix.tag(t)
            if itens_t and all(dono.get(i) == lic for i in itens_t):
                return True
        return False

    desativar, rotas_ok, produzidos = {}, {i: [] for i in dono}, set()
    for rid, r in ix.receitas.items():
        if r["pasta"] != "recipe":
            continue
        res = saidas_resolvidas(r)
        produzidos |= res
        alvo = [i for i in res if i in dono]
        if not alvo or rid in removidas:
            continue
        if rid in modificadas:  # receita alterada por nós: exige componente/materiais de outras profissões
            for i in alvo:
                rotas_ok[i].append(f"{r['tipo']} alterada ({rid})")
            continue
        if GRID.search(r["tipo"]):
            for i in alvo:
                if i not in r["ent"]:  # "enxaguar"/recolorir (o item volta a si mesmo) não é rota para obter o primeiro
                    rotas_ok[i].append(f"{r['tipo']} ({rid})")
            continue
        alvo_exp = [i for i in alvo if i in explicito]
        lics_alvo = {dono[i] for i in alvo_exp}
        # componente_sempre: a etapa exige o componente mesmo quando a receita já consome um item dela (que pode ser fácil de fazer)
        forcar = any(licencas[l][0].get("componente_sempre") for l in lics_alvo)
        if not alvo_exp or (not forcar and all(consome_item_da_etapa(r, l) for l in lics_alvo)):
            for i in alvo:
                rotas_ok[i].append(f"{r['tipo']}" + (" a partir de item da etapa" if alvo_exp else "") + f" ({rid})")
            continue
        # etapa com componente_maquina: a receita de máquina ganha o componente (feito na bancada só com a licença) em vez de sumir
        lics = {dono[i] for i in alvo_exp}
        cm = licencas[next(iter(lics))][0].get("componente_maquina") if len(lics) == 1 else None
        obj = com_componente(r["json"], cm) if cm else None
        if obj is not None:
            ns, caminho = rid.split(":", 1)
            modificadas[rid] = (f"data/{ns}/recipe/{caminho}.json", obj, True)
            for i in alvo:
                rotas_ok[i].append(f"{r['tipo']} + componente {cm['id']} ({rid})")
            continue
        desativar[rid] = (r["tipo"], sorted(alvo_exp), r["fonte"])
    for rid, (s, saidas) in substitutas.items():
        for i in saidas:
            if i in rotas_ok:
                rotas_ok[i].append(f"substituta ({rid})")
    for i, rotas in rotas_extra.items():
        if i in rotas_ok:
            rotas_ok[i].extend(rotas)
    for a in cfg.get("arquivos_dados", []):
        if not re.fullmatch(r"data/[a-z0-9_.-]+/[a-z0-9_./-]+\.json", a.get("caminho", "")):
            erros.append(f"arquivos_dados: caminho inválido {a.get('caminho')}")
        validar_tags(a.get("json", {}), f"arquivo {a.get('caminho')}")
    sem_receita = set(cfg.get("sem_receita", []))  # itens que ficam SEM receita de propósito (pedido do usuário)
    for i in sem_receita:
        if i not in ix.itens:
            erros.append(f"sem_receita: item inexistente {i}")
    for rid, r in removidas.items():  # itens cujas receitas de bancada removemos precisam de rota nova
        for i in r["saidas"]:
            if i in dono and not rotas_ok.get(i) and i not in sem_receita:
                erros.append(f"{i}: a receita {rid} foi removida e não sobrou rota (crie ritual/receita nova)")
    # ------------------------------------------------ regra do usuário: nada liberado antes de existir a forma de fazê-lo
    # Um item da etapa L só pode depender (em pelo menos uma receita) de ingredientes livres ou de etapas cuja cadeia de
    # pesquisas já está na cadeia de L. Senão ele é "liberado no futuro" e vai para 'adiantados' (erro).
    cadeia_comp = {f"{NS}:{cid}": set().union(*[pesq_etapa[l] for l in c["licencas"] if l in pesq_etapa]) for cid, c in comps.items()}
    # itens de etapa que continuam no saque de propósito (saque_livre) e de fato caem de alguma tabela: qualquer um acha
    saque_aberto = {i for e, _ in licencas.values() for pad in e.get("saque_livre", []) for i in dono if rpe_match(i, pad)}
    saque_aberto &= {n for txt_loot, _ in ix.loot.values() for n in re.findall(r'"name"\s*:\s*"([a-z0-9_.-]+:[a-z0-9_./-]+)"', txt_loot)}
    def precisa(x):
        """Cadeia de pesquisas exigida para ter o item x (vazia = livre)."""
        if x in cadeia_comp:
            return cadeia_comp[x]
        l = dono.get(x)
        if not l or licencas[l][0].get("inicio") or x in saque_aberto:  # livre, mod inicial (sempre há quem venda) ou saque
            return set()
        return pesq_etapa[l]
    receitas_de = {}
    for rid, r in ix.receitas.items():
        if r["pasta"] != "recipe" or rid in desativar or rid in removidas:
            continue
        obj = modificadas[rid][1] if rid in modificadas else r["json"]
        s, st, e_, et = _refs(obj) if rid in modificadas else (r["saidas"], r["saidas_tag"], r["ent"], r["ent_tag"])
        for i in s:
            if i in dono and i not in e_:
                receitas_de.setdefault(i, []).append((rid, e_, et))
    for rid, obj in list(rituais.items()) + [(k, v) for k, v in novas.items()]:
        s, _, e_, et = _refs(obj)
        for i in s:
            if i in dono:
                receitas_de.setdefault(i, []).append((rid, e_, et))
    # receitas em ciclo (lingote <-> bloco/pepita) não são rota: só vale receita cujos ingredientes têm origem "de fora"
    # (sem receita, ou que cai de alguma tabela de saque: colheita, mineração, mob, baú)
    de_saque = set()
    for txt_loot, _ in ix.loot.values():
        de_saque.update(re.findall(r'"name"\s*:\s*"([a-z0-9_.-]+:[a-z0-9_./-]+)"', txt_loot))
    aterrado = {i for i in receitas_de if i in de_saque}
    def aterrada(rec):
        _, ents, tags = rec
        return (all(x not in receitas_de or x in aterrado for x in ents)
                and all(not ix.tag(t) or any(o not in receitas_de or o in aterrado for o in ix.tag(t)) for t in tags))
    mudou = True
    while mudou:
        mudou = False
        for i, rs in receitas_de.items():
            if i not in aterrado and any(aterrada(rec) for rec in rs):
                aterrado.add(i)
                mudou = True
    adiantados, necessario = {}, {}
    for i, rs in receitas_de.items():
        rs = [rec for rec in rs if aterrada(rec)]
        if not rs or i in saque_aberto:  # só se obtém por saque/ciclo, ou continua no saque: não há "forma de fazer" a esperar
            continue
        minha = pesq_etapa[dono[i]]
        opcoes_rec = []  # (o que falta na etapa atual, tudo o que a receita exige)
        for rid, ents, tags in rs:
            exige = set()
            for x in ents:
                if x != i:
                    exige |= precisa(x)
            for t in tags:
                opcoes = [o for o in ix.tag(t) if o != i]
                if opcoes:
                    exige |= min((precisa(x) for x in opcoes), key=lambda s: (len(s - minha), len(s)))
            opcoes_rec.append((exige - minha, exige))
        falta, exige = min(opcoes_rec, key=lambda fe: (len(fe[0]), len(fe[1])))
        if falta:
            adiantados[i] = falta
            necessario[i] = exige
    for i in sorted(explicito):
        if not rotas_ok[i] and i in produzidos and i not in so_saque and i not in sem_receita:
            erros.append(f"{i} ({dono[i]}) ficaria sem nenhuma receita após a vedação: adicione substituta ou tire da etapa")

    # ------------------------------------------------ loot
    # Estruturas (baús, injeções em baús vanilla, arqueologia, presentes de aldeão, kit inicial): sai todo item de etapa.
    # Demais (mobs, chefes, dados, conquistas, bolsas): sai só item explícito que tem rota de bancada depois da pesquisa;
    # itens que só existem como drop (ex.: Gaia Spirit, sinew) ficam, senão o mod quebraria mesmo após a pesquisa.
    ESTRUTURA = ("chests/", "inject/", "archeology/", "archaeology/", "gameplay/", "starter_blueprints")
    # rota de bancada "de verdade": ignora laços de compactação (bloco/pepita feitos do próprio item)
    produz = {}
    for rid, r in ix.receitas.items():
        if r["pasta"] == "recipe":
            for s in saidas_resolvidas(r):
                produz.setdefault(s, []).append(r)
    def derivado_de(ing, alvo_item):
        rs = produz.get(ing, [])
        return bool(rs) and all(alvo_item in r["ent"] for r in rs)
    def tem_rota_bancada(i):
        if any(s.startswith("substituta") for s in rotas_ok[i]):
            return True
        for r in produz.get(i, []):
            if GRID.search(r["tipo"]):
                ents = set(r["ent"]) - {i}
                if ents and not all(derivado_de(x, i) for x in ents):
                    return True
                if r["ent_tag"]:
                    return True
        return False
    com_rota_bancada = {i for i in explicito if tem_rota_bancada(i)}
    loot_ajustes, loot_ignorados = {}, {}
    def _entradas_item(o):
        if isinstance(o, dict):
            if o.get("type") in ("minecraft:item", "item") and isinstance(o.get("name"), str):
                yield o
            for v in o.values():
                yield from _entradas_item(v)
        elif isinstance(o, list):
            for v in o:
                yield from _entradas_item(v)
    # etapa com "saque_livre": esses itens continuam no saque (ex.: armas únicas do Simply Swords, que são tesouros)
    saque_livre = {i for e, _ in licencas.values() for pad in e.get("saque_livre", []) for i in dono if rpe_match(i, pad)}
    for (ns, pasta, caminho), (txt, fonte) in ix.loot.items():
        if pasta != "loot_table" or caminho.startswith("blocks/"):
            continue
        alvo = (set(dono) if caminho.startswith(ESTRUTURA) else com_rota_bancada) - saque_livre
        nomes = set(re.findall(r'"name"\s*:\s*"([^"]+)"', txt))
        achados = sorted(i for i in nomes if i in alvo)
        # Tabela com item de mod não instalado = variante de overlay/condição (ex.: IDAS para Ice and Fire).
        # Sobrescrevê-la ressuscitaria a versão quebrada e o baú ficaria vazio; nesse caso ela fica intocada.
        try:
            itens_tabela = {e.get("name") for e in _entradas_item(json.loads(txt))}
        except Exception:
            itens_tabela = set()
        estranhos = sorted(i for i in itens_tabela if i and i not in ix.itens)
        if achados and estranhos:
            loot_ignorados[(ns, caminho)] = (estranhos[:3], fonte)
            continue
        if achados:
            loot_ajustes[(ns, caminho)] = (txt, achados, fonte, alvo)

    # ------------------------------------------------ padrões do RPEssentials
    global_bloq, permitidos = [], []
    def com_excecoes(ns, excl):
        return f"{ns}:(?!({'|'.join(sorted(excl))})$)*" if excl else f"{ns}:*"
    for pid, p in profs.items():
        partes = []
        for ns in p["namespaces"]:
            global_bloq.append(f"{ns}:*")
            if ns in inicial[pid]:
                partes.append(com_excecoes(ns, {i.split(":", 1)[1] for i in dono if i.startswith(ns + ":")}))
        permitidos.append(f"{pid};" + ",".join(partes))
    # avulsos de mods fora das profissões (ex.: minecraft:brewing_stand) precisam de bloqueio global próprio
    global_bloq += sorted(i for i in avulsos if i.split(":", 1)[0] not in ns_de_prof)
    # componentes do próprio mod: só fabrica quem concluiu a pesquisa da etapa dona (antes ficavam livres na bancada)
    comp_por_lic = {}
    # (e itens especiais que declaram "licencas", ex.: o Codex of Schools)
    for cid, c in list(comps.items()) + [(k, v) for k, v in especiais.items() if v.get("licencas")]:
        global_bloq.append(f"{NS}:{cid}")
        for l in c["licencas"]:
            if l not in licencas:
                erros.append(f"{NS}:{cid}: licença inexistente {l}")
            comp_por_lic.setdefault(l, []).append(f"{NS}:{cid}")
    for lic, (e, its) in licencas.items():
        partes = sorted(i for i in its if i.split(":", 1)[0] not in e.get("resto_de", [])) + sorted(comp_por_lic.get(lic, []))
        for ns in e.get("resto_de", []):
            partes.append(com_excecoes(ns, {i.split(":", 1)[1] for i, l in dono.items() if i.startswith(ns + ":") and l != lic}))
        permitidos.append(f"{lic};" + ",".join(partes))
    # estações exclusivas: o RPEssentials barra o clique direito no bloco para quem não tem a licença
    estacoes = []
    for est in cfg.get("estacoes_exclusivas", []):
        if est["bloco"] not in ix.itens:
            erros.append(f"estação exclusiva: bloco inexistente {est['bloco']}")
        for l in est["licencas"]:
            if l not in licencas and l not in profs:
                erros.append(f"estação exclusiva {est['bloco']}: licença inexistente {l}")
        estacoes.append(f"{est['bloco']};{','.join(est['licencas'])}")
    profissoes_cfg = [f"{pid};{p['nome']};{p['cor']}" for pid, p in profs.items()]
    profissoes_cfg += [f"{lic};{e['nome']};{profs[e['profissao']]['cor']}" for lic, (e, _) in licencas.items()]
    # marcadores de mod inicial (concedidos pelo mod na escolha; não liberam nada sozinhos)
    profissoes_cfg += [f"inicio_{op['licenca']};{p['nome']} · Start: {op['rotulo']};{p['cor']}" for p in profs.values() for op in p.get("inicios", [])]
    # colonia_N: nível de colônia liberado por ADM (estar na lista de profissões é o que deixa o
    # "/rpessentials license give" aceitar a licença). Vagabundo NÃO é licença: só a tag prof.vagabundo de quem não escolheu.
    profissoes_cfg += [f"colonia_{n};Colony level {n};§e" for n in range(2, 6)]

    def pode(lics_jogador, item):
        if not any(rpe_match(item, g) for g in global_bloq):
            return True
        for lic in lics_jogador:
            for entrada in permitidos:
                nome, lista = entrada.split(";", 1)
                if nome.lower() == lic.lower() and any(rpe_match(item, x) for x in lista.split(",")):
                    return True
        return False
    todas_por_prof = {o: [l for l in licencas if licencas[l][0]["profissao"] == o] for o in profs}
    falhas_auto = 0
    for i in sorted(ix.itens):
        ns = i.split(":", 1)[0]
        lic = dono.get(i)
        pid = licencas[lic][0]["profissao"] if lic else ns_de_prof.get(ns)  # avulsos: vale a profissão da etapa
        if not pid:
            continue
        if lic:
            ok = not pode([pid], i) and pode([pid, lic], i) and not pode([pid] + [l for l in todas_por_prof[pid] if l != lic], i)
        else:
            ok = pode([pid], i)
        ok = ok and all(not pode([o] + todas_por_prof[o], i) for o in profs if o != pid)
        if not ok:
            falhas_auto += 1
            if falhas_auto <= 15:
                erros.append(f"autoteste: permissão errada para {i}")

    if analise:
        return {"adiantados": adiantados, "necessario": necessario, "dono": dict(dono), "pesq_etapa": pesq_etapa, "explicito": set(explicito),
                "etapas": {lic: e for lic, (e, _) in licencas.items()}, "erros": list(erros)}
    if adiantados:
        erros.append(f"{len(adiantados)} itens liberados antes da pesquisa que libera a forma de fazê-los "
                     f"(rode tools/migracoes/corrigir_adiantados.py ou veja com --adiantados)")
        print(f"ITENS ADIANTADOS: {len(adiantados)} (liberados antes da pesquisa que libera a forma de fazê-los)")
        if "--adiantados" in sys.argv:
            for i, f in sorted(adiantados.items(), key=lambda kv: (dono[kv[0]], kv[0])):
                print(f"  {dono[i]:24} {i:55} precisa de: {', '.join(sorted(nossas[x]['nome'] for x in f))}")
    for a in avisos:
        print("AVISO:", a)
    if erros:
        for e in erros[:60]:
            print("ERRO:", e)
        print(f"\n{len(erros)} erro(s). Nada foi escrito.")
        sys.exit(1)

    total = sum(len(v[1]) for v in licencas.values())
    print(f"OK: {len(nossas)} pesquisas na aba '{aba['nome']}', {len(licencas)} etapas, {total} itens liberados por pesquisa "
          f"({len(explicito)} explícitos com vedação total).")
    print(f"    {len(desativar)} receitas de máquina/ritual desativadas, {len(substitutas)} substituta(s), {len(loot_ajustes)} tabela(s) de loot ajustada(s).")
    if so_checar:
        return

    # ------------------------------------------------ escrever
    txt = TOML.read_text(encoding="utf-8")
    def lista_toml(v):
        return "[" + ", ".join(json.dumps(x, ensure_ascii=False) for x in v) + "]"
    for chave, valor in (("professions", profissoes_cfg), ("globalBlockedCrafts", global_bloq), ("professionAllowedCrafts", permitidos),
                         ("containerOpenRestrictions", estacoes)):
        txt, n = re.subn(rf"(?m)^(\s*){chave} = .*$", lambda m: f"{m.group(1)}{chave} = {lista_toml(valor)}", txt)
        if n != 1:
            print(f"ERRO: chave {chave} não encontrada no TOML"); sys.exit(1)
    TOML.write_text(txt, encoding="utf-8")

    if PACK_ANTIGO.exists():
        shutil.rmtree(PACK_ANTIGO)
    if PACK.exists():
        shutil.rmtree(PACK)
    def escrever(rel, obj):
        p = PACK / rel
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text((obj if isinstance(obj, str) else json.dumps(obj, ensure_ascii=False, indent=2)) + "\n", encoding="utf-8")
    escrever("pack.mcmeta", {"pack": {"pack_format": 48, "description": "SMP 2 - profession progression (GENERATED by smp-profissoes/tools/gerar.py)"}})

    # textos da aba: chaves de tradução + en_us.json no mod (inglês por padrão; outros idiomas caem no inglês)
    lang = {}
    ramo = BRANCH_ID.split(":", 1)[1]
    chave_aba = f"{NS}.research.branch.{ramo}"
    lang[chave_aba] = aba["nome"]
    escrever(f"data/{NS}/researches/{ramo}.json", {"base-time": aba.get("tempo_base", 1.0), "branch-name": chave_aba, "sortOrder": aba.get("ordem", 500)})
    # Nome da etapa sem a profissão → profissões que a recebem (etapas cruzadas têm o mesmo nome em cada profissão).
    etapas_por_pesquisa = {}
    for lic, (e, _) in licencas.items():
        quem, _, nome_etapa = e["nome"].rpartition(" · ")
        for r in e["pesquisas"]:
            quem_recebe = etapas_por_pesquisa.setdefault(r, {}).setdefault(nome_etapa, [])
            if quem and quem not in quem_recebe:
                quem_recebe.append(quem)

    def descricao(pid):
        grupos = etapas_por_pesquisa.get(pid)
        if not grupos:
            return "Unlocks: next research in this line"
        return "Unlocks: " + "; ".join(f"{n} ({', '.join(q)})" if q else n for n, q in grupos.items())
    for ordem, (pid, p) in enumerate(nossas.items(), start=1):
        nome = pid.split("/", 1)[1]
        k = f"{NS}.research.{ramo}.{nome}"
        lang[k + ".name"], lang[k + ".subtitle"] = p["nome"], p["subtitulo"]
        lang[f"{NS}.research.effects.{nome}.description"] = descricao(pid)
        obj = {"branch": BRANCH_ID, "name": k + ".name", "subtitle": k + ".subtitle", "icon": p["icone"], "researchLevel": p["nivel"],
               "sortOrder": ordem,
               "costs": [{"count": n, "item": i} for i, n in p["custos"].items()],
               "requirements": [{"type": "minecolonies:building", "building": b, "level": lv} for b, lv in p.get("edificios", {}).items()]
                               + [{"type": "minecolonies:research", "research": r} for r in p.get("pesquisas_extra", [])],
               "effects": [{"id": f"{NS}:effects/{nome}", "level": 1}]}
        if p.get("pai"):
            obj["parentResearch"] = p["pai"]
        escrever(f"data/{NS}/researches/{ramo}/{nome}.json", obj)
        escrever(f"data/{NS}/researches/effects/{nome}.json", {"effect": True, "name": f"{NS}.research.effects.{nome}.description"})
    for cid, c in list(comps.items()) + list(especiais.items()):
        lang[f"item.{NS}.{cid}"] = c["nome"]
        lang[f"item.{NS}.{cid}.desc"] = c["descricao"]
    lang.update(MENSAGENS_ESPECIAIS)
    LANG.parent.mkdir(parents=True, exist_ok=True)
    LANG.write_text(json.dumps(lang, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    escrever_recursos_componentes(comps, especiais)

    falso = {"neoforge:conditions": [{"type": "neoforge:false"}]}
    for rid in desativar:
        ns, caminho = rid.split(":", 1)
        escrever(f"data/{ns}/recipe/{caminho}.json", falso)
    for rid, (s, _) in substitutas.items():
        escrever(f"data/{NS}/recipe/{rid.split(':', 1)[1]}.json", s["json"])
    for rid, obj in receitas_comp.items():
        escrever(f"data/{NS}/recipe/{rid.split(':', 1)[1]}.json", obj)
    for rid, (caminho, obj, _) in modificadas.items():
        escrever(caminho, com_condicao_itens(obj) if "hephaestus_forge/ritual" in caminho else obj)
    for rid in removidas:
        ns, caminho = rid.split(":", 1)
        escrever(f"data/{ns}/recipe/{caminho}.json", falso)
    for rid, obj in novas.items():
        escrever(f"data/{NS}/recipe/{rid.split(':', 1)[1]}.json", obj)
    for rid, obj in rituais.items():
        escrever(f"data/{NS}/forbidden_arcanus/hephaestus_forge/ritual/{rid}.json", com_condicao_itens(obj))
    for a in cfg.get("arquivos_dados", []):  # dados avulsos (tipos de combustível, tags...) copiados como estão
        escrever(a["caminho"], a["json"])
    gerar_menu(profs, licencas)

    for (ns, caminho), (txt_loot, achados, _, alvo) in loot_ajustes.items():
        obj = json.loads(txt_loot)
        def limpar(o):
            if isinstance(o, dict):
                if o.get("type") in ("minecraft:item", "item") and o.get("name") in alvo:
                    return {"type": "minecraft:empty", "weight": o["weight"]} if "weight" in o else {"type": "minecraft:empty"}
                return {k: limpar(v) for k, v in o.items()}
            if isinstance(o, list):
                return [limpar(v) for v in o]
            return o
        escrever(f"data/{ns}/loot_table/{caminho}.json", limpar(obj))

    REGRAS.parent.mkdir(parents=True, exist_ok=True)
    usadas = {r for e, _ in licencas.values() for r in e["pesquisas"]}
    regras = {"_gerado_por": "smp-profissoes/tools/gerar.py - não edite à mão", "intervaloTicks": 100,
              "profissoes": list(profs),
              "namespacesProtegidos": sorted(ns_de_prof),
              "nomesPesquisas": {r: nossas[r]["nome"] for r in sorted(usadas)},
              "nomesEdificios": {b: NOMES_MC.get(b, b) for e, _ in licencas.values() for b in e.get("edificios", {})},
              "receitasComLicenca": licenca_receita,
              # cadeia completa (pesquisa + pais + exigências cruzadas): um save antigo com só o nó concluído não libera a etapa
              "etapas": [{"licenca": lic, "nome": e["nome"], "profissao": e["profissao"], "pesquisas": sorted(pesq_etapa[lic]),
                          "edificios": e.get("edificios", {}), "inicio": bool(e.get("inicio"))} for lic, (e, _) in licencas.items()]}
    REGRAS.write_text(json.dumps(regras, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    # ------------------------------------------------ relatório
    L = ["# Relatório gerado da progressão", "", "Gerado por `smp-profissoes/tools/gerar.py` a partir dos jars instalados. Não edite à mão.", "",
         "## Mod inicial (escolhido no menu de profissão)", ""]
    for pid, p in profs.items():
        L.append(f"- **{p['nome']}**: " + " | ".join(f"{op['rotulo']} (`{op['licenca']}`)" for op in p.get("inicios", [])))
    def comps_de(obj):
        return sorted({i.split(":", 1)[1] for i in itens_declarados(obj) if i.startswith(NS + ":")})
    L += ["", "## Componentes (pesquisas cruzadas)", "",
          "Itens do mod smpprofissoes feitos na bancada só por quem tem a licença da pesquisa; entram nas receitas de máquina/ritual.", ""]
    for cid, c in comps.items():
        L.append(f"- **{c['nome']}** (`{NS}:{cid}`) — licença: {', '.join(c['licencas'])}; ingredientes: {', '.join(c['ingredientes'])}")
    L += ["", "## Rituais novos da Forja de Hefesto", ""]
    for rid, obj in rituais.items():
        L.append(f"- `{NS}:{rid}` → {obj['result']['result_item']['id']} (tier {obj['forge_tier']}) — componentes: {', '.join(comps_de(obj)) or '—'}")
    L += ["", "## Receitas novas (outras estações)", ""]
    for rid, obj in novas.items():
        L.append(f"- `{rid}` ({obj['type']}) → {', '.join(_refs(obj)[0])} — componentes: {', '.join(comps_de(obj)) or '—'}")
    L += ["", "## Receitas de bancada removidas (o item passou para forja/altar)", ""]
    L += [f"- `{rid}`" for rid in removidas]
    L += ["", "## Receitas e rituais alterados", ""]
    for rid, (caminho, obj, _) in modificadas.items():
        L.append(f"- `{rid}` (`{caminho}`) — componentes: {', '.join(comps_de(obj)) or '— (materiais diretos)'}")
    L += ["", f"## Aba de pesquisas: {aba['nome']} (`{BRANCH_ID}`)", ""]
    for pid, p in nossas.items():
        req = ", ".join(f"{NOMES_MC.get(b, b)} {lv}" for b, lv in p.get("edificios", {}).items())
        L.append(f"- **{p['nome']}** (`{pid}`) — level {p['nivel']}, parent: {nossas[p['pai']]['nome'] if p.get('pai') else '— (root)'}; "
                 f"buildings: {req or '—'}; cost: {', '.join(f'{n}x {i}' for i, n in p['custos'].items())}")
    L += ["", "## Etapas", "", "Itens **explícitos** têm vedação total (máquinas, autocraft e saque). Mods inteiros (`resto_de`) são liberados na bancada e nas máquinas desses mods, que só existem depois da pesquisa.", ""]
    for lic, (e, its) in licencas.items():
        resto = e.get("resto_de", [])
        exp = [i for i in its if i in explicito]
        L += [f"### `{lic}` — {e['nome']}", "", f"Pesquisas: {', '.join(nossas[r]['nome'] for r in e['pesquisas'])}" +
              (f" · mods inteiros: {', '.join(resto)} ({len(its) - len(exp)} itens)" if resto else ""), ""]
        for i in exp:
            L.append(f"- `{i}` — " + ("; ".join(rotas_ok[i]) if rotas_ok[i] else "sem receita (só saque/comércio)"))
        L.append("")
    L += ["## Receitas desativadas", ""]
    L += [f"- `{rid}` ({t}, de {f}) → {', '.join(a)}" for rid, (t, a, f) in sorted(desativar.items())]
    L += ["", "## Tabelas de loot ajustadas (item de etapa trocado por vazio)", ""]
    L += [f"- `{ns}:{c}` ({f}) → {', '.join(a[:12])}{' …' if len(a) > 12 else ''}" for (ns, c), (_, a, f, _) in sorted(loot_ajustes.items())]
    L += ["", "## Tabelas de loot NÃO alteradas (citam itens de mods não instalados: variantes de overlay/condição)", ""]
    L += [f"- `{ns}:{c}` ({f}) — ex.: {', '.join(x)}" for (ns, c), (x, f) in sorted(loot_ignorados.items())]
    RELATORIO.write_text("\n".join(L) + "\n", encoding="utf-8")
    print("Escrito:", TOML, PACK, REGRAS, LANG, RELATORIO, sep="\n  ")


if __name__ == "__main__":
    main()
