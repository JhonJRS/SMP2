import fnmatch
"""Testes do datapack gerado (moonlight-global-datapacks/smp_progressao) contra os jars instalados.
Uso (na pasta da instância): python -I smp-profissoes/tests/testes_datapack.py
Cobre: aba própria de pesquisas, árvore original intacta, vedação de rotas (latão em especial) e loot.
"""
import json, re, sys
from pathlib import Path

INST = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(INST / "smp-profissoes" / "tools"))
import gerar  # noqa: E402

ok = falhas = 0
def checar(cond, nome):
    global ok, falhas
    if cond:
        ok += 1
    else:
        falhas += 1
        print("FALHOU:", nome)

PACK = gerar.PACK
cfg = json.loads(gerar.SRC.read_text(encoding="utf-8"))
ix = gerar.indexar()

# ---------- aba própria
ramo = PACK / "data/smpprofissoes/researches/oficios.json"
checar(ramo.exists(), "arquivo do ramo 'oficios' existe")
if ramo.exists():
    r = json.loads(ramo.read_text(encoding="utf-8"))
    lang = json.loads(gerar.LANG.read_text(encoding="utf-8"))
    checar(lang.get(r.get("branch-name")) == "Guild of Trades", "nome da aba (en_us) é 'Guild of Trades'")
pesq = {f"smpprofissoes:oficios/{p.stem}": json.loads(p.read_text(encoding="utf-8")) for p in (PACK / "data/smpprofissoes/researches/oficios").glob("*.json")}
checar(len(pesq) == len(cfg["pesquisas"]) == 109, f"109 pesquisas geradas (achou {len(pesq)})")
# v9: cada linha tem ramos (ao menos 2 pesquisas de nível 2 por mod inicial, contando pelas raízes)
filhos = {}
for p in cfg["pesquisas"]:
    if p.get("pai"):
        filhos.setdefault(p["pai"], []).append(p["id"])
for raiz in [p["id"] for p in cfg["pesquisas"] if p["nivel"] == 1 and p["id"].split("/")[1] not in ("fundacao_neovitae", "fundacao_pm", "fundacao_embers")]:
    checar(len(filhos.get(raiz, [])) >= 2, f"{raiz}: linha com ramos no nível 2 ({len(filhos.get(raiz, []))})")
# v9: nível 5 só para o que muda o jogo
nivel5 = {p["id"].split("/")[1] for p in cfg["pesquisas"] if p["nivel"] == 5}
checar(nivel5 == {"railgun", "lamina_ember", "grimorios_lendarios", "arsenal_maligno", "portal_alfheim", "forja_estelar", "armas_miticas",
                  "pocoes_eternas", "fusao_cataclismica", "redes_quanticas", "replicacao_reliquias", "artefatos_aether"}, f"nível 5 = itens que mudam o jogo ({sorted(nivel5)})")
checar(next(p for p in cfg["pesquisas"] if p["id"].endswith("/regalia_praga"))["nivel"] == 4, "Regalia do Plaguebringer no nível 4")
# v9/v10: poções são do Naturalista
toml_rpe = (INST / "config/rpessentials/rpessentials-professions.toml").read_text(encoding="utf-8")
checar('"minecraft:brewing_stand;nat_boticario"' in toml_rpe and '"botania:brewery;c18_nat"' in toml_rpe,
       "suporte de poções e Botanical Brewery só abrem para o Naturalista com a pesquisa")
checar('"minecraft:brewing_stand"' in toml_rpe.split("globalBlockedCrafts", 1)[1].split("\n", 1)[0], "fabricar o suporte de poções é bloqueado para os outros")
for nome, item in (("brew_evasion_elixir", "toxony:whirlsnap"), ("brew_oakskin_elixir", "farmersdelight:tree_bark")):
    arq = PACK / f"data/irons_spellbooks/recipe/alchemist_cauldron/{nome}.json"
    checar(arq.exists() and item in arq.read_text(encoding="utf-8"), f"elixir {nome} pede {item}")
# Railgun com 50 de dano (haste de aço = 24 de base)
ie_cfg = (INST / "config/immersiveengineering-server.toml").read_text(encoding="utf-8")
mod = float(re.search(r"\[tools\.railgun\][\s\S]*?damage_modifier = ([\d.]+)", ie_cfg).group(1))
checar(abs(24 * mod - 50) < 0.1, f"Railgun: haste de aço dá 50 de dano ({24 * mod:.1f})")
lang = json.loads(gerar.LANG.read_text(encoding="utf-8"))
# correções da lista de pendências (25/10)
vapor = pesq.get("smpprofissoes:oficios/energia_vapor", {})
checar(vapor.get("researchLevel") == 2 and vapor.get("parentResearch") == "smpprofissoes:oficios/fundacao_create", "Steam Power: nível 2, depois de Create Foundations")
etapa_de = {i: e["licenca"] for e in cfg["etapas"] for i in e.get("itens", [])}
checar(etapa_de.get("create:steam_engine") == "eng_vapor", "Steam Engine liberada por Steam Power, não por Industrial Fuels")
for i in ("toxony:witching_blade", "toxony:flintlock", "toxony:iron_round", "toxony:professional_hunter_*"):
    checar(etapa_de.get(i) == "c5_nat", f"Venomous Alchemy libera {i}")
wb = PACK / "data/toxony/recipe/alchemical_forge/witching_blade.json"
checar(wb.exists() and "smpprofissoes:cursed_toxin" in wb.read_text(encoding="utf-8"), "Witching Blade exige o Cursed Toxin (componente da Venomous Alchemy)")
checar(all(c["item"] != "minecraft:soul_sand" for c in pesq.get("smpprofissoes:oficios/fundacao_malum", {}).get("costs", [])), "Spirit Foundations sem Soul Sand no custo")
for pid, p in pesq.items():
    desc = lang.get(f"smpprofissoes.research.effects.{pid.split('/')[1]}.description", "")
    partes = [s.split(" (")[0] for s in desc.removeprefix("Unlocks: ").split("; ")]
    checar(len(partes) == len(set(partes)), f"{pid}: descrição sem nome repetido ({desc!r})")
for pid, p in pesq.items():
    for campo in ("name", "subtitle"):
        texto = lang.get(p[campo], "")
        checar(bool(texto) and all(ord(c) < 128 or c in "·'" for c in texto), f"{pid}: {campo} traduzido em inglês ({texto!r})")
for pid in pesq:
    ef = json.loads((PACK / f"data/smpprofissoes/researches/effects/{pid.split('/')[1]}.json").read_text(encoding="utf-8"))
    checar(lang.get(ef["name"], "").startswith("Unlocks: "), f"{pid}: efeito descrito em inglês")
# mod inicial escolhido no menu
esperado = {"engenheiro": ["eng_create", "eng_ie", "eng_embers"], "arcanista": ["arc_irons", "arc_malum", "arc_botania", "arc_neovitae"],
            "ferreiro": ["fer_silentgear", "fer_arcanus"], "naturalista": ["nat_cozinha", "nat_ervas"]}
checar({k: [o["licenca"] for o in v["inicios"]] for k, v in cfg["profissoes"].items()} == esperado, "opções de mod inicial corretas")
checar(all(not v["inicial"] for v in cfg["profissoes"].values()), "nenhum mod liberado só pela profissão")
MENU = INST / "moonlight-global-datapacks/profissoes/data/profissoes/function"
for pid, ops in esperado.items():
    for lic in ops:
        f = MENU / "inicio" / f"{lic}.mcfunction"
        checar(f.exists(), f"menu: função do início {lic}")
        if f.exists():
            txt = f.read_text(encoding="utf-8")
            checar(f"rpessentials license give @s {pid}" in txt and f"smpprofissoes inicio @s {lic}" in txt, f"menu: {lic} dá a profissão e o mod inicial")
for nome in ("selecionar", "escolher_inicio", "confirmar", "finalizar", "pendente", "menu"):
    checar((MENU / f"{nome}.mcfunction").exists(), f"menu: {nome}.mcfunction existe")
for linha in (MENU / "selecionar.mcfunction").read_text(encoding="utf-8").splitlines() + (MENU / "escolher_inicio.mcfunction").read_text(encoding="utf-8").splitlines():
    if " tellraw @s " in linha:
        try:
            json.loads(linha.split(" tellraw @s ", 1)[1])
            ok_json = True
        except Exception:
            ok_json = False
        checar(ok_json, f"menu: JSON válido em {linha[:60]}")
# níveis 4 e 5 são cruzados (exigem pesquisa de outra linha)
for pid, p in pesq.items():
    if p["researchLevel"] >= 4:
        extras = [r for r in p["requirements"] if r["type"] == "minecolonies:research"]
        checar(len(extras) >= 1, f"{pid}: nível {p['researchLevel']} exige pesquisa de outra linha")
        checar(all(r["research"] in pesq for r in extras), f"{pid}: pesquisas exigidas existem")
checar(sum(1 for p in pesq.values() if p["researchLevel"] == 5) >= 3, "há várias pesquisas de nível 5")

# componentes (pesquisas cruzadas) e receitas alteradas
regras = json.loads(gerar.REGRAS.read_text(encoding="utf-8"))
comps = {c["id"]: c for c in cfg["componentes"]}
RES = gerar.LANG.parents[1]
lang_mod = json.loads(gerar.LANG.read_text(encoding="utf-8"))
checar(json.loads((RES / "componentes.json").read_text(encoding="utf-8")) == sorted(comps), "componentes.json lista todos os componentes")
checar("selos" not in cfg and not list(PACK.glob("data/smpprofissoes/recipe/selo/*.json")), "selos de papel removidos")
for cid, c in comps.items():
    arq = PACK / f"data/smpprofissoes/recipe/componente/{cid}.json"
    checar(arq.exists(), f"componente {cid}: receita gerada")
    if arq.exists():
        r = json.loads(arq.read_text(encoding="utf-8"))
        checar(r["type"] == "minecraft:crafting_shapeless" and r["result"]["id"] == f"smpprofissoes:{cid}", f"componente {cid}: bancada, resultado é o item")
        checar(regras["receitasComLicenca"].get(f"smpprofissoes:componente/{cid}") == c["licencas"], f"componente {cid}: licença registrada no mod")
    checar((RES / "models/item" / f"{cid}.json").exists() and (RES / "textures/item" / f"{cid}.png").exists(), f"componente {cid}: modelo e textura")
    checar(lang_mod.get(f"item.smpprofissoes.{cid}") == c["nome"] and lang_mod.get(f"item.smpprofissoes.{cid}.desc"), f"componente {cid}: nome e descrição")
# profissões envolvidas: dono do namespace de cada item + profissão da licença de cada componente (e dos ingredientes dele)
ns_prof = {ns: pid for pid, p in cfg["profissoes"].items() for ns in p["namespaces"]}
dono_lic = {e["licenca"]: e["profissao"] for e in cfg["etapas"]}
def profs_de(texto, vis=None):
    vis = vis or set()
    out = set()
    for i in set(re.findall(r'"(?:item|id)": "([a-z0-9_.-]+:[a-z0-9_/.-]+)"', texto)):
        ns, nome = i.split(":", 1)
        if ns == "smpprofissoes" and nome in comps and nome not in vis:
            vis.add(nome)
            c = comps[nome]
            out |= {dono_lic[l] for l in c["licencas"]}
            out |= profs_de(json.dumps([{"item": x} for x in c["ingredientes"]]), vis)
        elif ns in ns_prof:
            out.add(ns_prof[ns])
    return out
CRUZADAS = {"aeronautics:mixing/levitite_blend", "immersiveengineering:crafting/railgun", "silentgear:alloying/metal/tyrian_steel_ingot",
            "irons_spellbooks:alchemist_cauldron/brew_legendary_ink", "toxony:alchemical_forge/eternal_plague", "toxony:alchemical_forge/lethal_dose",
            "forbidden_arcanus:hephaestus_forge/ritual/eternal_stella", "botania:terra_plate/terrasteel_ingot", "embers:alchemy/tyrfing",
            "toxony:alchemical_forge/witching_blade", "toxony:alchemical_forge/plaguebringer_mask", "toxony:alchemical_forge/plaguebringer_coat",
            "toxony:alchemical_forge/plaguebringer_leggings", "toxony:alchemical_forge/plaguebringer_boots"}
for m in cfg["receitas_modificadas"]:
    arq = PACK / m["caminho"]
    checar(arq.exists(), f"alterada {m['id']}: arquivo gerado")
    if not arq.exists():
        continue
    txt = arq.read_text(encoding="utf-8")
    checar("neoforge:components" not in txt or not json.loads(txt).get("type", "").startswith(gerar.SEM_COMPONENTES), f"alterada {m['id']}: parser sem registro sem neoforge:components")
    checar("@selo" not in txt and "@comp" not in txt and "smp_selo" not in txt, f"alterada {m['id']}: sem referência crua de selo/componente")
    if m["id"] in CRUZADAS:
        envolvidas = profs_de(txt)
        checar(len(envolvidas) >= 2, f"{m['id']}: exige 2+ profissões ({sorted(envolvidas)})")
for arq in PACK.glob("data/*/recipe/**/*.json"):
    obj = json.loads(arq.read_text(encoding="utf-8"))
    if str(obj.get("type", "")).startswith(gerar.SEM_COMPONENTES):
        checar("neoforge:components" not in json.dumps(obj), f"{arq.relative_to(PACK)}: parser sem registro não recebe neoforge:components")
# nenhuma tabela de loot gerada pode citar item que não existe no pack (ex.: variantes Ice and Fire / Ars Nouveau do IDAS)
def _itens_loot(o):
    if isinstance(o, dict):
        if o.get("type") in ("minecraft:item", "item") and isinstance(o.get("name"), str):
            yield o["name"]
        for v in o.values():
            yield from _itens_loot(v)
    elif isinstance(o, list):
        for v in o:
            yield from _itens_loot(v)
for arq in PACK.glob("data/*/loot_table/**/*.json"):
    estranhos = sorted(i for i in _itens_loot(json.loads(arq.read_text(encoding="utf-8"))) if i not in ix.itens)
    checar(not estranhos, f"loot gerado só com itens instalados: {arq.relative_to(PACK)} ({estranhos[:3]})")
# rituais da Forja de Hefesto (manuais e automáticos) e receitas novas
ritual_dir = PACK / "data/smpprofissoes/forbidden_arcanus/hephaestus_forge/ritual"
itens_ok = ix.itens | {f"smpprofissoes:{c}" for c in comps}
for arq in ritual_dir.rglob("*.json"):
    obj = json.loads(arq.read_text(encoding="utf-8"))
    nome = arq.relative_to(ritual_dir).as_posix()
    checar(sum(i["amount"] for i in obj["inputs"]) <= 8, f"ritual {nome}: no máximo 8 pedestais")
    checar(1 <= obj["forge_tier"] <= 5, f"ritual {nome}: tier válido")
    checar(set(obj["essences"]) <= {"aureal", "blood", "souls", "experience"}, f"ritual {nome}: essências válidas")
    itens = [i["ingredient"].get("item") for i in obj["inputs"]] + [obj["main_ingredient"].get("item"), obj["result"]["result_item"]["id"]]
    checar(all(i in itens_ok for i in itens if i), f"ritual {nome}: itens existem")
    if nome.startswith(("hazen/", "auto/hazen/")):
        checar(any(c in json.dumps(obj) for c in ("smpprofissoes:arcane_inscription", "smpprofissoes:archmage_codex")), f"ritual {nome}: exige a Arcane Forge Inscription (Spellforged Arms) ou o Archmage Codex (Legendary Artifacts)")
        if any(x in json.dumps(obj) for x in ("hazennstuff:deus_essence", "hazennstuff:divine_mold")):
            checar("smpprofissoes:archmage_codex" in json.dumps(obj), f"ritual {nome}: Deus Essence/Divine Mold pedem o Archmage Codex")
n_auto = len(list((ritual_dir / "auto/hazen").glob("*.json")))
checar(n_auto >= 200, f"Hazen: armas/armaduras/cajados convertidos em rituais ({n_auto})")
for n in cfg.get("receitas_novas", []):
    arq = PACK / f"data/smpprofissoes/recipe/{n['id'].split(':', 1)[1]}.json"
    checar(arq.exists(), f"receita nova {n['id']}: gerada")
    if arq.exists():
        txt = arq.read_text(encoding="utf-8")
        checar("@selo" not in txt and "@comp" not in txt, f"receita nova {n['id']}: sem referência crua")
        if "spirit_infusion" in n["id"]:
            checar(len(profs_de(txt)) >= 2, f"receita nova {n['id']}: exige 2+ profissões ({sorted(profs_de(txt))})")
for rid in cfg.get("receitas_removidas", []):
    ns, cam = rid.split(":", 1)
    arq = PACK / f"data/{ns}/recipe/{cam}.json"
    checar(arq.exists() and json.loads(arq.read_text(encoding="utf-8")) == {"neoforge:conditions": [{"type": "neoforge:false"}]}, f"removida {rid}: desativada")
# Eternal Stella: tier 5, Dark Nether Star no centro, Stellar Core + Mana Diamond (Arcanista) + erva (Naturalista)
st = json.loads((PACK / "data/forbidden_arcanus/forbidden_arcanus/hephaestus_forge/ritual/eternal_stella.json").read_text(encoding="utf-8"))
checar(st["forge_tier"] == 5 and st["main_ingredient"]["item"] == "forbidden_arcanus:dark_nether_star", "Eternal Stella: tier 5 e Dark Nether Star")
txt_st = json.dumps(st)
checar("smpprofissoes:stellar_core" in txt_st and "botania:mana_diamond" in txt_st and "smpprofissoes:eternity_essence" in txt_st
       and sum(i["amount"] for i in st["inputs"]) <= 8, "Eternal Stella: Stellar Core, item mágico e Eternity Essence, até 8 pedestais")
# latão voltou às rotas originais
for rid in ("create:mixing/brass_ingot", "immersiveengineering:alloysmelter/brass", "productivemetalworks:casting/create/brass_ingot", "embers:stamping/ingots/brass"):
    ns, cam = rid.split(":", 1)
    checar(not (PACK / f"data/{ns}/recipe/{cam}.json").exists(), f"rota original do latão ativa: {rid}")
checar(not (PACK / "data/smpprofissoes/recipe/latao_artesanal.json").exists(), "receita substituta do latão removida")
for pid, p in pesq.items():
    checar(p["branch"] == "smpprofissoes:oficios", f"{pid} está na aba própria")
    pai = p.get("parentResearch")
    checar(pai is None or pai in pesq, f"{pid}: pai {pai} é da própria aba")
    checar(pai is None or p["researchLevel"] == pesq[pai]["researchLevel"] + 1, f"{pid}: nível = pai + 1 (a MineColonies descarta se pular)")
    checar(pai is not None or p["researchLevel"] == 1, f"{pid}: raiz no nível 1")
    for req in p["requirements"]:
        checar((req["type"] == "minecolonies:building" and req.get("building") in ix.predios) or (req["type"] == "minecolonies:research" and req.get("research") in pesq), f"{pid}: requisito válido {req}")
    for c in p["costs"]:
        checar(c["item"] in ix.itens, f"{pid}: custo existe {c['item']}")
    ef = PACK / f"data/smpprofissoes/researches/effects/{pid.split('/')[1]}.json"
    checar(ef.exists(), f"{pid}: efeito existe")
# nenhuma pesquisa gerada mexe na árvore da MineColonies
checar(not any((PACK / "data/minecolonies/researches").glob("**/*.json")), "nenhum arquivo em data/minecolonies/researches (árvore original intacta)")
checar(not (INST / "moonlight-global-datapacks/profissoes/data/smpprofissoes/researches").exists(), "pesquisas antigas do ramo Tecnologia removidas")
for orig in ("hittingiron", "strong", "whatyaneed", "biodegradable", "letitgrow"):
    checar(f"minecolonies:technology/{orig}" in ix.mc_pesquisas, f"original {orig} continua na MineColonies")
etapas_pesq = {r for e in cfg["etapas"] for r in e["pesquisas"]}
checar(all(r.startswith("smpprofissoes:oficios/") for r in etapas_pesq), "todas as etapas dependem só de pesquisas da aba")
regras = json.loads(gerar.REGRAS.read_text(encoding="utf-8"))
checar(all(r in pesq for e in regras["etapas"] for r in e["pesquisas"]), "regras do mod apontam para pesquisas existentes")
checar("create" in regras["namespacesProtegidos"] and "numismatics" not in regras["namespacesProtegidos"], "namespaces protegidos corretos")

# ---------- loot: nenhum item de etapa em tabelas geradas
# todos os itens de etapa (explícitos + mods inteiros)
dono = set()
for e in cfg["etapas"]:
    for pad in e["itens"]:
        ns = pad.split(":")[0]
        dono |= {i for i in ix.itens if i.startswith(ns + ":") and gerar.rpe_match(i, pad)}
    for ns in e.get("resto_de", []):
        dono |= {i for i in ix.itens if i.startswith(ns + ":")}
produzidos = set()
for r in ix.receitas.values():
    if r["pasta"] == "recipe":
        produzidos |= set(r["saidas"]) | {i for t in r["saidas_tag"] for i in ix.tag(t)}
ESTRUTURA = ("chests/", "inject/", "archeology/", "archaeology/", "gameplay/", "starter_blueprints")
nomes_de = lambda txt: set(re.findall(r'"name"\s*:\s*"([^"]+)"', txt))
geradas = list((PACK / "data").glob("*/loot_table/**/*.json"))
checar(len(geradas) > 0, "há tabelas de loot ajustadas")
for p in geradas:
    checar("/loot_table/blocks/" not in p.as_posix(), f"drop de bloco não foi alterado: {p.relative_to(PACK)}")
for (ns, pasta, cam), (txt, _) in ix.loot.items():
    if pasta != "loot_table" or cam.startswith("blocks/"):
        continue
    gerada = PACK / f"data/{ns}/loot_table/{cam}.json"
    antes = nomes_de(txt) & dono
    depois = nomes_de(gerada.read_text(encoding="utf-8")) & dono if gerada.exists() else antes
    depois = {i for i in depois if not any(fnmatch.fnmatch(i, pad) for e in cfg["etapas"] for pad in e.get("saque_livre", []))}  # tesouros de propósito
    def _itens(o):
        if isinstance(o, dict):
            if o.get("type") in ("minecraft:item", "item") and isinstance(o.get("name"), str):
                yield o["name"]
            for v in o.values():
                yield from _itens(v)
        elif isinstance(o, list):
            for v in o:
                yield from _itens(v)
    try:
        itens_orig = set(_itens(json.loads(txt)))
    except Exception:
        itens_orig = set()
    if any(i not in ix.itens for i in itens_orig):
        continue  # variante de mod não instalado: o jogo nem carrega essa tabela (não gera loot)
    if cam.startswith(ESTRUTURA):
        # estruturas: nenhum item de etapa pode sobrar
        checar(not depois, f"estrutura sem item de etapa: {ns}:{cam} ({sorted(depois)[:3]})")
    else:
        # mobs/chefes/dados/conquistas: só pode sair item que tem receita (senão o mod quebraria após a pesquisa)
        removidos = antes - depois
        checar(all(i in produzidos for i in removidos), f"drop exclusivo preservado em {ns}:{cam} ({sorted(i for i in removidos if i not in produzidos)[:3]})")
for item in ("botania:gaia_spirit", "silentgear:sinew", "irons_spellbooks:pyrium_ingot"):
    tabelas = [(ns, cam) for (ns, pasta, cam), (txt, _) in ix.loot.items() if pasta == "loot_table" and not cam.startswith(ESTRUTURA) and item in nomes_de(txt)]
    for ns, cam in tabelas:
        g = PACK / f"data/{ns}/loot_table/{cam}.json"
        checar(not g.exists() or item in nomes_de(g.read_text(encoding="utf-8")), f"{item} continua dropando em {ns}:{cam}")
# 26/10 (rodada 6): ritual é dado de registro — item inexistente derruba o mundo. Todo ritual gerado leva neoforge:item_exists.
for arq in (PACK / "data").rglob("hephaestus_forge/ritual/**/*.json"):
    o = json.loads(arq.read_text(encoding="utf-8"))
    externos = {i for i in re.findall(r'"(?:item|id)": "([a-z0-9_.-]+:[a-z0-9_./-]+)"', json.dumps({k: v for k, v in o.items() if k != "neoforge:conditions"}))
                if not i.startswith(("minecraft:", "smpprofissoes:", "forbidden_arcanus:create_item"))}
    conds = {c.get("item") for c in o.get("neoforge:conditions", []) if c.get("type") == "neoforge:item_exists"}
    checar(externos <= conds, f"ritual {arq.name}: condição item_exists para {sorted(externos - conds)[:3]}")

# 28/10 (rodada 9): componentes do mod só na bancada de quem fez a pesquisa (antes ficavam livres)
bloq_global = toml_rpe.split("globalBlockedCrafts", 1)[1].split("\n", 1)[0]
permitidos_rpe = toml_rpe.split("professionAllowedCrafts", 1)[1].split("\n", 1)[0]
for c in cfg["componentes"]:
    item = f"smpprofissoes:{c['id']}"
    checar(f'"{item}"' in bloq_global, f"{item} bloqueado na bancada para quem não pesquisou")
    for lic in c["licencas"]:
        entrada = next((e for e in re.findall(r'"([^"]+)"', permitidos_rpe) if e.startswith(lic + ";")), "")
        checar(item in entrada.split(";", 1)[-1].split(","), f"{item} liberado para a licença {lic}")
for livre in ("guild_letter", "guild_charter", "draught_of_oblivion"):
    checar(f'"smpprofissoes:{livre}"' not in bloq_global, f"smpprofissoes:{livre} continua livre (para qualquer um)")
rep = next(c for c in cfg["componentes"] if c["id"] == "replication_matrix")
checar(not set(rep["ingredientes"]) & {"minecraft:echo_shard", "minecraft:diamond_block", "minecraft:nether_star"},
       "Replication Matrix não usa os itens dos rituais de duplicação")

print(f"\nTestes do datapack: {ok} OK, {falhas} falha(s).")
sys.exit(1 if falhas else 0)
