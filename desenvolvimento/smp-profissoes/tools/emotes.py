"""Gera emotes do Emotecraft (formato JSON legado, radianos) em <instância>/emotes/smp2_*.json.

Convenções (conferidas nos emotes que vêm no jar do Emotecraft 2.4.12):
  braço  pitch -1.57 = esticado à frente, -3.1 = para cima; roll + (direito) / - (esquerdo) = abre para o lado
  braço  yaw - (direito) / + (esquerdo) = traz para o meio do peito
  perna  pitch + = para trás;  cabeça pitch + = olha para baixo, yaw = vira;  torso pitch + = inclina à frente
Uso: python -I smp-profissoes/tools/emotes.py
"""
import json
import uuid
from pathlib import Path

INST = Path(__file__).resolve().parents[2]
SAIDA = INST / "emotes"
AUTOR = "SMP 2"
NS_UUID = uuid.UUID("5f0c2b1e-8a9d-4c47-9a51-5d2c0f3e7a10")


def emote(nome, descricao, quadros, loop=False, volta=None, fim=None, suave="EASEINOUTQUAD"):
    """quadros: [(tick, {parte: {eixo: valor}}), ...]; loop repete de `volta` até o último quadro."""
    moves = []
    for tick, partes in quadros:
        for parte, eixos in partes.items():
            for eixo, v in eixos.items():
                moves.append({"tick": tick, "easing": suave, "turn": 0, parte: {eixo: round(v, 4)}})
    ultimo = fim or max(t for t, _ in quadros)
    return {
        "name": nome, "author": AUTOR, "description": descricao,
        "uuid": str(uuid.uuid5(NS_UUID, nome)),
        "emote": {"isLoop": "true" if loop else "false", "returnTick": volta if (loop and volta is not None) else 0,
                  "beginTick": 0, "endTick": ultimo, "stopTick": ultimo + (4 if loop else 6), "degrees": False, "moves": moves},
    }


def oscila(inicio, passo, n, a, b):
    """n quadros alternando a/b a cada `passo` ticks a partir de `inicio`."""
    return [(inicio + i * passo, a if i % 2 == 0 else b) for i in range(n)]


E = []
# ── cortesia e corte ──
E.append(emote("Bow", "A respectful bow", [
    (8, {"torso": {"pitch": 0.55}, "head": {"pitch": 0.35}, "rightArm": {"pitch": -0.9, "yaw": -0.7}, "leftArm": {"pitch": 0.35}}),
    (30, {"torso": {"pitch": 0.55}, "head": {"pitch": 0.35}, "rightArm": {"pitch": -0.9, "yaw": -0.7}, "leftArm": {"pitch": 0.35}}),
    (40, {"torso": {"pitch": 0.0}, "head": {"pitch": 0.0}, "rightArm": {"pitch": 0.0, "yaw": 0.0}, "leftArm": {"pitch": 0.0}})]))
E.append(emote("Royal Salute", "Fist to the heart, for the crown", [
    (6, {"rightArm": {"pitch": -1.1, "yaw": -0.85}, "head": {"pitch": -0.1}}),
    (40, {"rightArm": {"pitch": -1.1, "yaw": -0.85}, "head": {"pitch": -0.1}})]))
E.append(emote("Military Salute", "Hand to the brow", [
    (6, {"rightArm": {"pitch": -2.35, "yaw": -0.65, "roll": 0.2}}),
    (36, {"rightArm": {"pitch": -2.35, "yaw": -0.65, "roll": 0.2}})]))
E.append(emote("Sword Salute", "Raise the blade, then lower it", [
    (8, {"rightArm": {"pitch": -2.7, "yaw": -0.3}, "head": {"pitch": -0.15}}),
    (22, {"rightArm": {"pitch": -2.7, "yaw": -0.3}, "head": {"pitch": -0.15}}),
    (32, {"rightArm": {"pitch": -1.3, "yaw": 0.0}, "head": {"pitch": 0.1}}),
    (42, {"rightArm": {"pitch": -1.3, "yaw": 0.0}, "head": {"pitch": 0.1}})]))
E.append(emote("Pledge", "Hand on heart, head lowered", [
    (8, {"rightArm": {"pitch": -0.95, "yaw": -0.8}, "head": {"pitch": 0.4}, "torso": {"pitch": 0.15}}),
    (50, {"rightArm": {"pitch": -0.95, "yaw": -0.8}, "head": {"pitch": 0.4}, "torso": {"pitch": 0.15}})]))
E.append(emote("Pray", "Hands together in prayer", [
    (8, {"rightArm": {"pitch": -1.25, "yaw": -0.5}, "leftArm": {"pitch": -1.25, "yaw": 0.5}, "head": {"pitch": 0.45}}),
    (60, {"rightArm": {"pitch": -1.25, "yaw": -0.5}, "leftArm": {"pitch": -1.25, "yaw": 0.5}, "head": {"pitch": 0.45}})],
    loop=True, volta=8))
E.append(emote("Toast", "Raise your cup!", [
    (8, {"rightArm": {"pitch": -2.75, "yaw": -0.2}, "head": {"pitch": -0.2}}),
    (20, {"rightArm": {"pitch": -2.75, "yaw": -0.2}, "head": {"pitch": -0.2}}),
    (28, {"rightArm": {"pitch": -1.9, "yaw": -0.45}, "head": {"pitch": -0.35}}),
    (38, {"rightArm": {"pitch": -1.9, "yaw": -0.45}, "head": {"pitch": -0.35}})]))
# ── conversa ──
E.append(emote("Nod", "Yes", oscila(4, 5, 6, {"head": {"pitch": 0.35}}, {"head": {"pitch": -0.05}})))
E.append(emote("Shake Head", "No", oscila(4, 5, 6, {"head": {"yaw": 0.45}}, {"head": {"yaw": -0.45}})))
E.append(emote("Shrug", "Who knows?", [
    (6, {"rightArm": {"pitch": -0.6, "roll": 0.45}, "leftArm": {"pitch": -0.6, "roll": -0.45}, "head": {"roll": 0.2}}),
    (24, {"rightArm": {"pitch": -0.6, "roll": 0.45}, "leftArm": {"pitch": -0.6, "roll": -0.45}, "head": {"roll": 0.2}})]))
E.append(emote("Think", "Hmm...", [
    (8, {"rightArm": {"pitch": -2.1, "yaw": -0.65}, "leftArm": {"pitch": -0.9, "yaw": 0.75}, "head": {"pitch": 0.15, "roll": -0.1}}),
    (40, {"rightArm": {"pitch": -2.1, "yaw": -0.65}, "leftArm": {"pitch": -0.9, "yaw": 0.75}, "head": {"pitch": 0.15, "roll": 0.12}}),
    (72, {"rightArm": {"pitch": -2.1, "yaw": -0.65}, "leftArm": {"pitch": -0.9, "yaw": 0.75}, "head": {"pitch": 0.15, "roll": -0.1}})],
    loop=True, volta=8))
E.append(emote("Arms Crossed", "Waiting...", [
    (6, {"rightArm": {"pitch": -1.15, "yaw": -0.95}, "leftArm": {"pitch": -1.0, "yaw": 0.95}}),
    (60, {"rightArm": {"pitch": -1.15, "yaw": -0.95}, "leftArm": {"pitch": -1.0, "yaw": 0.95}})], loop=True, volta=6))
E.append(emote("Hands on Hips", "Well then?", [
    (6, {"rightArm": {"pitch": -0.25, "roll": 0.65, "yaw": 0.3}, "leftArm": {"pitch": -0.25, "roll": -0.65, "yaw": -0.3}}),
    (60, {"rightArm": {"pitch": -0.25, "roll": 0.65, "yaw": 0.3}, "leftArm": {"pitch": -0.25, "roll": -0.65, "yaw": -0.3}})],
    loop=True, volta=6))
E.append(emote("Beckon", "Come here", oscila(6, 6, 7, {"rightArm": {"pitch": -1.5}}, {"rightArm": {"pitch": -2.15}})))
E.append(emote("Halt", "Stop right there!", [
    (5, {"rightArm": {"pitch": -1.65, "yaw": 0.1}, "head": {"pitch": -0.05}}),
    (35, {"rightArm": {"pitch": -1.65, "yaw": 0.1}, "head": {"pitch": -0.05}})]))
E.append(emote("Point Up", "Look up there!", [
    (6, {"rightArm": {"pitch": -3.0}, "head": {"pitch": -0.45}}),
    (40, {"rightArm": {"pitch": -3.0}, "head": {"pitch": -0.45}})]))
E.append(emote("Look Around", "Where am I?", [
    (10, {"head": {"yaw": 0.9}}), (28, {"head": {"yaw": 0.9}}), (44, {"head": {"yaw": -0.9}}), (62, {"head": {"yaw": -0.9}}),
    (74, {"head": {"yaw": 0.0}})]))
E.append(emote("Farewell", "Goodbye, friends", oscila(6, 7, 8, {"rightArm": {"pitch": -2.9, "roll": 0.35}, "leftArm": {"pitch": -2.9, "roll": -0.35}},
                                                      {"rightArm": {"pitch": -2.9, "roll": -0.15}, "leftArm": {"pitch": -2.9, "roll": 0.15}})))
# ── emoções ──
E.append(emote("Cheer", "Hooray!", oscila(4, 5, 11, {"rightArm": {"pitch": -3.0, "roll": 0.3}, "leftArm": {"pitch": -3.0, "roll": -0.3}, "head": {"pitch": -0.3}},
                                            {"rightArm": {"pitch": -2.6, "roll": 0.5}, "leftArm": {"pitch": -2.6, "roll": -0.5}, "head": {"pitch": -0.2}}),
                 loop=True, volta=4))
E.append(emote("Laugh", "Ha ha ha!", oscila(4, 3, 14, {"torso": {"pitch": 0.25}, "head": {"pitch": -0.3}, "rightArm": {"pitch": -0.7, "yaw": -0.5}, "leftArm": {"pitch": -0.7, "yaw": 0.5}},
                                          {"torso": {"pitch": 0.1}, "head": {"pitch": -0.45}, "rightArm": {"pitch": -0.8, "yaw": -0.5}, "leftArm": {"pitch": -0.8, "yaw": 0.5}})))
E.append(emote("Cower", "Please, don't hurt me!", [
    (5, {"rightArm": {"pitch": -2.5, "yaw": -0.6}, "leftArm": {"pitch": -2.5, "yaw": 0.6}, "head": {"pitch": 0.5}, "torso": {"pitch": 0.35}}),
    (9, {"head": {"yaw": 0.08}}), (13, {"head": {"yaw": -0.08}}), (17, {"head": {"yaw": 0.08}}), (21, {"head": {"yaw": -0.08}}),
    (50, {"rightArm": {"pitch": -2.5, "yaw": -0.6}, "leftArm": {"pitch": -2.5, "yaw": 0.6}, "head": {"pitch": 0.5}, "torso": {"pitch": 0.35}})],
    loop=True, volta=5))
E.append(emote("Yawn", "So sleepy...", [
    (12, {"rightArm": {"pitch": -2.9, "roll": 0.35}, "leftArm": {"pitch": -2.9, "roll": -0.35}, "head": {"pitch": -0.4}, "torso": {"pitch": -0.1}}),
    (30, {"rightArm": {"pitch": -2.9, "roll": 0.35}, "leftArm": {"pitch": -2.9, "roll": -0.35}, "head": {"pitch": -0.4}, "torso": {"pitch": -0.1}}),
    (44, {"rightArm": {"pitch": 0.0, "roll": 0.0}, "leftArm": {"pitch": 0.0, "roll": 0.0}, "head": {"pitch": 0.3}, "torso": {"pitch": 0.0}}),
    (60, {"head": {"pitch": 0.3}})]))
E.append(emote("Phew", "Wipe the sweat", [
    (8, {"rightArm": {"pitch": -2.55, "yaw": -0.7}}), (18, {"rightArm": {"pitch": -2.55, "yaw": 0.2}}),
    (24, {"rightArm": {"pitch": -1.0, "yaw": 0.0}, "head": {"pitch": 0.15}}), (34, {"head": {"pitch": 0.15}})]))
E.append(emote("Hug", "Come here, friend", [
    (8, {"rightArm": {"pitch": -1.5, "yaw": -0.55}, "leftArm": {"pitch": -1.5, "yaw": 0.55}}),
    (40, {"rightArm": {"pitch": -1.5, "yaw": -0.55}, "leftArm": {"pitch": -1.5, "yaw": 0.55}})]))
E.append(emote("Victory", "Fist pump!", oscila(4, 6, 8, {"rightArm": {"pitch": -3.05}, "head": {"pitch": -0.25}}, {"rightArm": {"pitch": -2.2}, "head": {"pitch": -0.1}})))
# ── dança e movimento ──
E.append(emote("Tavern Jig", "A merry tavern dance", oscila(4, 5, 13,
    {"rightLeg": {"pitch": -0.55}, "leftLeg": {"pitch": 0.2}, "rightArm": {"pitch": -0.4, "roll": 0.6}, "leftArm": {"pitch": 0.3, "roll": -0.6}, "torso": {"roll": 0.08}},
    {"rightLeg": {"pitch": 0.2}, "leftLeg": {"pitch": -0.55}, "rightArm": {"pitch": 0.3, "roll": 0.6}, "leftArm": {"pitch": -0.4, "roll": -0.6}, "torso": {"roll": -0.08}}),
    loop=True, volta=4))
E.append(emote("Sway", "Swaying to the bard's song", oscila(6, 10, 9,
    {"torso": {"roll": 0.14}, "head": {"roll": 0.12}, "rightArm": {"roll": 0.35}, "leftArm": {"roll": -0.1}},
    {"torso": {"roll": -0.14}, "head": {"roll": -0.12}, "rightArm": {"roll": 0.1}, "leftArm": {"roll": -0.35}}), loop=True, volta=6))
E.append(emote("March", "Left, right, left!", oscila(4, 6, 13,
    {"rightLeg": {"pitch": -0.7}, "leftLeg": {"pitch": 0.1}, "rightArm": {"pitch": 0.45}, "leftArm": {"pitch": -0.6}},
    {"rightLeg": {"pitch": 0.1}, "leftLeg": {"pitch": -0.7}, "rightArm": {"pitch": -0.6}, "leftArm": {"pitch": 0.45}}), loop=True, volta=4))
# ── ofícios ──
E.append(emote("Hammering", "Strike while the iron is hot", oscila(4, 5, 13,
    {"rightArm": {"pitch": -2.7, "yaw": -0.2}, "head": {"pitch": 0.35}, "torso": {"pitch": 0.1}},
    {"rightArm": {"pitch": -0.8, "yaw": -0.2}, "head": {"pitch": 0.4}, "torso": {"pitch": 0.2}}), loop=True, volta=4))
E.append(emote("Spellcasting", "Channel arcane power", oscila(6, 8, 9,
    {"rightArm": {"pitch": -1.7, "roll": 0.25}, "leftArm": {"pitch": -1.7, "roll": -0.25}, "head": {"pitch": -0.1}},
    {"rightArm": {"pitch": -1.9, "roll": -0.05}, "leftArm": {"pitch": -1.9, "roll": 0.05}, "head": {"pitch": -0.2}}), loop=True, volta=6))
E.append(emote("Tend the Fields", "Hoeing the soil", oscila(4, 8, 9,
    {"rightArm": {"pitch": -1.4, "yaw": -0.3}, "leftArm": {"pitch": -1.2, "yaw": 0.3}, "torso": {"pitch": 0.2}, "head": {"pitch": 0.4}},
    {"rightArm": {"pitch": -0.4, "yaw": -0.3}, "leftArm": {"pitch": -0.3, "yaw": 0.3}, "torso": {"pitch": 0.4}, "head": {"pitch": 0.5}}), loop=True, volta=4))
E.append(emote("Inspect", "Examining something closely", [
    (8, {"rightArm": {"pitch": -1.3, "yaw": -0.3}, "head": {"pitch": 0.55, "yaw": 0.0}, "torso": {"pitch": 0.2}}),
    (24, {"rightArm": {"pitch": -1.4, "yaw": -0.1}, "head": {"pitch": 0.55, "yaw": 0.15}, "torso": {"pitch": 0.2}}),
    (40, {"rightArm": {"pitch": -1.3, "yaw": -0.3}, "head": {"pitch": 0.55, "yaw": -0.15}, "torso": {"pitch": 0.2}}),
    (56, {"rightArm": {"pitch": -1.3, "yaw": -0.3}, "head": {"pitch": 0.55, "yaw": 0.0}, "torso": {"pitch": 0.2}})], loop=True, volta=8))


def main():
    for e in E:  # loop: o último quadro tem de ser igual ao de retorno, senão a animação "pula" a cada volta
        em = e["emote"]
        if em["isLoop"] == "true":
            def pose(t):
                return sorted((k, json.dumps(m[k])) for m in em["moves"] if m["tick"] == t for k in m if k not in ("tick", "easing", "turn"))
            assert pose(em["returnTick"]) == pose(em["endTick"]), e["name"]
    SAIDA.mkdir(exist_ok=True)
    for velho in SAIDA.glob("smp2_*.json"):
        velho.unlink()
    for e in E:
        nome = "smp2_" + e["name"].lower().replace(" ", "_").replace("'", "")
        (SAIDA / f"{nome}.json").write_text(json.dumps(e, indent=1) + "\n", encoding="utf-8")
    print(f"{len(E)} emotes em {SAIDA}")


if __name__ == "__main__":
    main()
