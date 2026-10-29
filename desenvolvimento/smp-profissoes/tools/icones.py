"""Ícones animados dos itens do smpprofissoes (pixel art 16x16 desenhada por código, N quadros em tira vertical + .mcmeta).

Cada item tem uma função desenhar(cv, t) com t em [0, 1): o mesmo desenho base e um detalhe que se move (bolhas, ponteiro,
brilho, órbita...). Usado pelo gerar.py (escrever_recursos_componentes); item sem desenho próprio ganha um genérico.
Prévia ampliada: python smp-profissoes/tools/icones.py  →  smp-profissoes/tools/icones_previa.png
"""
import json
import math
from pathlib import Path

QUADROS = 16
TEMPO_QUADRO = 2  # ticks por quadro: ciclo de 1,6 s


# ------------------------------------------------------------------ cores
def hexc(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


def mix(a, b, f):
    f = max(0.0, min(1.0, f))
    return tuple(int(round(a[i] + (b[i] - a[i]) * f)) for i in range(3)) + (a[3] if len(a) > 3 else 255,)


def lit(c, f):
    return mix(c, (255, 255, 255, 255), f)


def drk(c, f):
    return mix(c, (0, 0, 0, 255), f)


def alfa(c, a):
    return c[:3] + (int(a),)


def hsv(h, s, v):
    i = int(h * 6) % 6
    f = h * 6 - int(h * 6)
    p, q, u = v * (1 - s), v * (1 - f * s), v * (1 - (1 - f) * s)
    r, g, b = [(v, u, p), (q, v, p), (p, v, u), (p, q, v), (u, p, v), (v, p, q)][i]
    return (int(r * 255), int(g * 255), int(b * 255), 255)


def onda(t, fase=0.0):
    """0..1..0 suave ao longo do ciclo."""
    return 0.5 - 0.5 * math.cos(2 * math.pi * (t + fase))


# ------------------------------------------------------------------ tela
class Tela:
    def __init__(self):
        self.p = [[None] * 16 for _ in range(16)]
        self.corpo = set()  # pixels do desenho (antes do contorno): onde o brilho passa

    def px(self, x, y, c):
        x, y = int(round(x)), int(round(y))
        if 0 <= x < 16 and 0 <= y < 16 and c is not None:
            if len(c) > 3 and c[3] < 255 and self.p[y][x] is not None:
                base = self.p[y][x]
                c = mix(base, c, c[3] / 255)[:3] + (max(base[3], c[3]),)
            self.p[y][x] = c if len(c) == 4 else c + (255,)

    def get(self, x, y):
        return self.p[y][x] if 0 <= x < 16 and 0 <= y < 16 else None

    def ret(self, x0, y0, x1, y1, c):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.px(x, y, c(x, y) if callable(c) else c)

    def disco(self, cx, cy, r, c):
        for y in range(16):
            for x in range(16):
                dx, dy = x - cx, y - cy
                d = math.hypot(dx, dy)
                if d <= r:
                    self.px(x, y, c(x, y, dx, dy, d) if callable(c) else c)

    def linhas(self, linhas, pal, x0=0, y0=0):
        for j, l in enumerate(linhas):
            for i, ch in enumerate(l):
                if ch in pal and pal[ch] is not None:
                    self.px(x0 + i, y0 + j, pal[ch])

    def fechar(self, f=0.62):
        """Marca o corpo e desenha o contorno escuro (cor do vizinho escurecida) em volta."""
        self.corpo = {(x, y) for y in range(16) for x in range(16) if self.p[y][x] is not None and self.p[y][x][3] > 60}
        novos = {}
        for y in range(16):
            for x in range(16):
                if (x, y) in self.corpo:
                    continue
                viz = [self.p[y + dy][x + dx] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))
                       if 0 <= x + dx < 16 and 0 <= y + dy < 16 and (x + dx, y + dy) in self.corpo]
                if viz:
                    novos[(x, y)] = drk(viz[0], f)
        for (x, y), c in novos.items():
            self.p[y][x] = c

    def brilho(self, t, largura=2.5, forca=0.55, inclinacao=1.0):
        """Faixa de luz diagonal que atravessa o corpo uma vez por ciclo."""
        pos = -8 + t * 44
        for (x, y) in self.corpo:
            d = abs(x + y * inclinacao - pos)
            if d < largura and self.p[y][x] is not None:
                self.p[y][x] = lit(self.p[y][x], forca * (1 - d / largura))[:3] + (self.p[y][x][3],)

    def faisca(self, x, y, k, cor=(255, 255, 255, 255)):
        """Estrelinha de 4 pontas; k 0..1 = intensidade."""
        if k <= 0.15:
            return
        self.px(x, y, alfa(cor, 255 * min(1, k * 1.4)))
        if k > 0.55:
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                self.px(x + dx, y + dy, alfa(cor, 150 * k))

    def imagem(self):
        from PIL import Image
        im = Image.new("RGBA", (16, 16))
        for y in range(16):
            for x in range(16):
                c = self.p[y][x]
                im.putpixel((x, y), c if c else (0, 0, 0, 0))
        return im


def esfera(base, cx, cy, r, claro=0.55, escuro=0.45):
    """Sombreamento de esfera com luz no canto superior esquerdo."""
    def f(x, y, dx, dy, d):
        k = (-dx - dy) / (r * 1.6)
        c = lit(base, claro * k) if k > 0 else drk(base, escuro * -k)
        if d > r - 0.9:
            c = drk(c, 0.15)
        return c
    return f


VIDRO = (200, 225, 240, 120)
ROLHA = hexc("#8A5A33")
PERGAMINHO = hexc("#E9D7A6")
LACRE = hexc("#B3262E")
OURO = hexc("#E8B93A")
FERRO = hexc("#8C8C96")
PEDRA = hexc("#7D7A80")


# ------------------------------------------------------------------ peças reutilizáveis
def frasco_redondo(cv, liquido, nivel=8.5, t=0.0, onda_sup=True):
    cv.ret(7, 1, 8, 1, ROLHA)
    cv.ret(7, 2, 8, 4, VIDRO)
    cv.disco(7.5, 9.5, 5.2, lambda x, y, dx, dy, d: VIDRO)
    for y in range(16):
        for x in range(16):
            sup = nivel + (0.6 * math.sin(2 * math.pi * t + x * 0.9) if onda_sup else 0)
            if math.hypot(x - 7.5, y - 9.5) <= 5.2 and y >= sup:
                k = (x - 7.5) / 10
                cv.px(x, y, drk(liquido, 0.25 + k) if k > 0 else lit(liquido, -k * 0.8))
    cv.px(5, 7, (255, 255, 255, 200))
    cv.px(5, 8, (255, 255, 255, 140))


def garrafa_alta(cv, liquido, nivel=7, t=0.0):
    cv.ret(7, 0, 8, 1, ROLHA)
    cv.ret(7, 2, 8, 4, VIDRO)
    cv.ret(5, 5, 10, 5, VIDRO)
    cv.ret(4, 6, 11, 14, VIDRO)
    for y in range(6, 15):
        for x in range(4, 12):
            if y >= nivel + 0.5 * math.sin(2 * math.pi * t + x):
                cv.px(x, y, lit(liquido, 0.25) if x < 6 else (drk(liquido, 0.3) if x > 9 else liquido))
    cv.ret(5, 7, 5, 9, (255, 255, 255, 170))


def pote(cv, liquido, t=0.0, tampa=None):
    if tampa:
        cv.ret(4, 2, 11, 3, tampa)
        cv.ret(5, 1, 10, 1, lit(tampa, 0.2))
    cv.ret(3, 4, 12, 14, VIDRO)
    cv.ret(4, 15, 11, 15, VIDRO)
    for y in range(6, 15):
        for x in range(3, 13):
            cv.px(x, y, lit(liquido, 0.2) if x < 5 else (drk(liquido, 0.3) if x > 10 else liquido))
    cv.ret(4, 5, 4, 8, (255, 255, 255, 150))


def orbe(cv, base, cx=7.5, cy=8, r=6):
    cv.disco(cx, cy, r, esfera(base, cx, cy, r))
    cv.px(int(cx - r / 2), int(cy - r / 2), lit(base, 0.85))


def livro(cv, capa, borda, t):
    cv.ret(3, 2, 13, 14, capa)
    cv.ret(3, 2, 4, 14, drk(capa, 0.35))                 # lombada
    cv.ret(12, 3, 13, 13, (236, 228, 205, 255))          # folhas
    for y in range(4, 13, 2):
        cv.px(13, y, (200, 190, 165, 255))
    for x, y in ((5, 2), (11, 2), (5, 14), (11, 14)):     # cantoneiras
        cv.px(x, y, borda)
    cv.ret(5, 3, 11, 3, drk(borda, 0.1))
    cv.ret(5, 13, 11, 13, drk(borda, 0.1))


def tigela(cv, po, t):
    cv.ret(2, 10, 13, 10, lit(PEDRA, 0.25))
    cv.ret(3, 11, 12, 12, PEDRA)
    cv.ret(4, 13, 11, 13, drk(PEDRA, 0.2))
    cv.ret(6, 14, 9, 14, drk(PEDRA, 0.35))
    for x in range(3, 13):  # monte de pó
        h = 2.8 - abs(x - 7.5) * 0.5
        for y in range(int(10 - h), 10):
            cv.px(x, y, lit(po, 0.35) if y == int(10 - h) else (drk(po, 0.2) if x > 8 else po))


def pergaminho(cv):
    cv.ret(3, 3, 12, 12, PERGAMINHO)
    cv.ret(2, 2, 13, 3, lit(PERGAMINHO, 0.2))
    cv.ret(2, 12, 13, 13, drk(PERGAMINHO, 0.15))
    for y in (5, 7, 9):
        cv.ret(5, y, 10 if y != 9 else 8, y, (150, 120, 80, 255))


# ------------------------------------------------------------------ desenhos por item
def runic_flux(cv, t, c):
    for x in range(2, 14):  # monte de pó
        h = 4 - abs(x - 7.5) * 0.65
        for y in range(int(14 - h), 15):
            cv.px(x, y, lit(c, 0.3) if y == int(14 - h) else (drk(c, 0.25) if (x + y) % 3 == 0 else c))
    b = round(math.sin(2 * math.pi * t))
    gl = lit(c, 0.6)
    for x, y in ((7, 3), (8, 3), (6, 4), (9, 4), (7, 5), (8, 5), (6, 6), (9, 6), (7, 7), (8, 7), (7, 4), (8, 6)):
        cv.px(x, y + b, gl)
    cv.fechar()
    k = onda(t)
    for x, y in ((7, 4), (8, 6)):
        cv.px(x, y + b, lit(c, 0.6 + 0.4 * k))
    for i, x in enumerate((3, 7, 11)):
        y = 12 - ((t * 10 + i * 3.3) % 10)
        cv.px(x, y, alfa(lit(c, 0.7), 220 * (y - 2) / 10))


def cursed_toxin(cv, t, c):
    frasco_redondo(cv, c, 8, t)
    cv.fechar()
    for i, x in enumerate((6, 9, 8)):
        y = 14 - ((t * 7 + i * 2.4) % 6)
        cv.px(x, y, lit(c, 0.6))
    for i in range(3):
        f = (t + i / 3) % 1
        cv.px(8 + math.sin(f * 6.3 + i) * 1.5, 1 - f * 2 + 1, alfa(lit(c, 0.5), 200 * (1 - f)))
    cv.px(7, 11, (30, 40, 25, 255))
    cv.px(9, 11, (30, 40, 25, 255))


def arcane_inscription(cv, t, c):
    pedra = (82, 92, 110, 255)
    cv.ret(3, 1, 12, 14, pedra)
    cv.ret(4, 1, 11, 1, lit(pedra, 0.2))
    cv.ret(3, 2, 3, 13, lit(pedra, 0.1))
    cv.fechar()
    runas = [((5, 3), (6, 4), (7, 3), (9, 3), (10, 4)), ((5, 7), (6, 6), (6, 8), (8, 7), (9, 6), (10, 8)),
             ((5, 11), (7, 10), (7, 12), (9, 11), (10, 10))]
    ativa = int(t * 3) % 3
    for i, linha in enumerate(runas):
        for x, y in linha:
            cv.px(x, y, lit(c, 0.55) if i == ativa else drk(c, 0.45))
    for x, y in runas[ativa]:
        cv.px(x, y - 1, alfa(c, 70))


def stellar_core(cv, t, c):
    orbe(cv, (34, 36, 78, 255), 7.5, 8, 5.5)
    cv.fechar()
    k = onda(t)
    cv.ret(7, 7, 8, 8, lit(c, 0.5))
    for d in range(1, 2 + int(k * 2)):
        for dx, dy in ((d, 0), (-d, 0), (0, d), (0, -d)):
            cv.px(7.5 + dx, 7.5 + dy, alfa(c, 255 - d * 60))
    for i in range(10):  # anel em elipse
        a = 2 * math.pi * i / 10
        x, y = 7.5 + 7.2 * math.cos(a), 8 + 2.2 * math.sin(a) - 1.4 * math.cos(a)
        if math.sin(a) > -0.2 or math.hypot(x - 7.5, y - 8) > 5.5:
            cv.px(x, y, alfa(drk(c, 0.2), 170))
    a = 2 * math.pi * t
    cv.faisca(7.5 + 7.2 * math.cos(a), 8 + 2.2 * math.sin(a) - 1.4 * math.cos(a), 1, lit(c, 0.6))
    for i, (x, y) in enumerate(((5, 6), (10, 10), (9, 5))):
        cv.px(x, y, alfa((255, 255, 255, 255), 230 * onda(t, i / 3)))


def terra_core(cv, t, c):
    orbe(cv, (120, 86, 52, 255), 7.5, 9, 5.5)
    for x in range(2, 14):  # cinturão de vinha
        y = 9 + round(1.5 * math.sin(x * 0.8))
        cv.px(x, y, c)
        cv.px(x, y + 1, drk(c, 0.3))
    s = round(math.sin(2 * math.pi * t))
    cv.ret(7, 2, 7, 3, drk(c, 0.2))
    cv.px(6 + s, 1, lit(c, 0.2))
    cv.px(8 + s, 2, c)
    cv.px(5 + s, 2, c)
    cv.fechar()
    k = onda(t)
    for x, y in ((5, 7), (10, 11), (6, 12)):
        cv.px(x, y, mix(c, lit(c, 0.7), k))


def ember_core(cv, t, c):
    ferro = (70, 66, 72, 255)
    cv.ret(4, 2, 11, 2, ferro)
    cv.ret(4, 14, 11, 14, ferro)
    cv.ret(3, 3, 12, 13, (60, 30, 20, 255))
    pulsa = [math.sin(2 * math.pi * t * 2 + i) for i in range(16)]
    for y in range(4, 14):
        larg = max(0, (y - 4) * 0.55 + 0.6 * pulsa[y]) if y < 11 else 4 - (y - 11)
        for x in range(16):
            if abs(x - 7.5) <= larg:
                f = abs(x - 7.5) / max(larg, 0.1)
                cv.px(x, y, mix((255, 240, 160, 255), c, f) if y > 6 else lit(c, 0.3))
    for x in (3, 6, 9, 12):
        cv.ret(x, 3, x, 13, ferro)
    cv.fechar()
    cv.px(7 + round(math.sin(t * 6.3)), 1, alfa(lit(c, 0.4), 200 * onda(t)))


def alchemical_regulator(cv, t, c):
    cv.disco(7.5, 7.5, 6, c)
    cv.disco(7.5, 7.5, 4.6, (238, 228, 200, 255))
    for i in range(7):  # marcações
        a = math.pi * (0.85 + 1.3 * i / 6)
        cv.px(7.5 + 4 * math.cos(a), 7.5 + 4 * math.sin(a), (90, 70, 50, 255))
    cv.px(10.5, 4, (200, 50, 40, 255))
    cv.ret(6, 13, 9, 15, drk(c, 0.15))
    cv.fechar()
    a = math.pi * (0.85 + 1.3 * onda(t) * 0.9 + 0.05 * math.sin(t * 40))
    for r in (1, 2, 3):
        cv.px(7.5 + r * math.cos(a), 7.5 + r * math.sin(a), (180, 30, 30, 255))
    cv.px(7.5, 7.5, drk(c, 0.3))
    cv.px(4, 4, lit(c, 0.7))


def spirit_binding(cv, t, c):
    pote(cv, (40, 30, 70, 255), t, tampa=hexc("#6B4A2E"))
    cv.fechar()
    for k in range(4):
        a = 2 * math.pi * (t - k * 0.06)
        x, y = 7.5 + 3 * math.cos(a), 10 + 2.5 * math.sin(a)
        cv.px(x, y, alfa(lit(c, 0.6 - k * 0.15), 255 - k * 55))
    cv.px(7.5, 10, alfa(c, 120))


def runic_slate(cv, t, c):
    cv.linhas(["..XXXXXXXXXX....", ".XXXXXXXXXXXX...", ".XXXXXXXXXXXXX..", "XXXXXXXXXXXXXX..",
               "XXXXXXXXXXXXXXX.", "XXXXXXXXXXXXXXX.", ".XXXXXXXXXXXXXX.", ".XXXXXXXXXXXXXX.",
               ".XXXXXXXXXXXXXX.", ".XXXXXXXXXXXXXXX", "..XXXXXXXXXXXXXX", "..XXXXXXXXXXXXX.",
               "...XXXXXXXXXXXX.", "....XXXXXXXXX...", "................", "................"], {"X": c}, 0, 1)
    for y in range(16):
        for x in range(16):
            if cv.get(x, y) and (x * 7 + y * 3) % 11 == 0:
                cv.px(x, y, drk(c, 0.2))
    cv.fechar()
    g = mix(drk(c, 0.5), (190, 140, 255, 255), onda(t))
    for x, y in ((5, 4), (6, 5), (7, 6), (8, 6), (9, 5), (10, 4), (7, 7), (7, 8), (8, 9), (6, 10), (9, 10), (7, 11)):
        cv.px(x, y, g)


def crucible_primer(cv, t, c):
    cv.ret(2, 5, 13, 6, drk(PEDRA, 0.1))
    cv.ret(3, 7, 12, 12, PEDRA)
    cv.ret(4, 13, 11, 13, drk(PEDRA, 0.25))
    cv.ret(3, 5, 12, 6, lambda x, y: mix(c, (255, 230, 140, 255), onda(t, x / 9) * 0.7))
    cv.ret(1, 7, 2, 8, drk(PEDRA, 0.2))
    cv.ret(13, 7, 14, 8, drk(PEDRA, 0.2))
    cv.fechar()
    for i, x in enumerate((5, 8, 11)):
        f = (t + i * 0.37) % 1
        cv.px(x + round(math.sin(f * 5)), 4 - f * 4, alfa(lit(c, 0.5), 255 * (1 - f)))


def void_shard(cv, t, c):
    cv.linhas([".........X......", "........XX......", ".......XXX......", "......XXXXX.....", ".....XXXXXX.....",
               ".....XXXXXXX....", "....XXXXXXXX....", "....XXXXXXXXX...", "...XXXXXXXXX....", "...XXXXXXXX.....",
               "..XXXXXXXXX.....", "..XXXXXXXX......", "...XXXXXX.......", "....XXXX........", ".....XX.........",
               "................"], {"X": (18, 12, 32, 255)})
    cv.fechar(0)
    borda = mix(c, (220, 90, 255, 255), onda(t))
    for (x, y) in list(cv.corpo):
        if any((x + dx, y + dy) not in cv.corpo for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            cv.px(x, y, borda)
    for x in range(16):
        for y in range(16):
            if cv.get(x, y) and cv.get(x, y)[3] == 255 and cv.get(x, y)[0] == 0 and (x, y) not in cv.corpo:
                cv.p[y][x] = drk(c, 0.5)
    for i, (x, y) in enumerate(((7, 6), (9, 8), (5, 10), (7, 12), (8, 4))):
        cv.px(x, y, alfa((230, 220, 255, 255), 255 * max(0, math.sin(2 * math.pi * (t + i * 0.21)))))


def vital_flask_base(cv, t, c):
    batida = max(math.exp(-((t - 0.1) * 14) ** 2), 0.8 * math.exp(-((t - 0.3) * 14) ** 2))
    garrafa_alta(cv, mix(c, lit(c, 0.5), batida), 8, t)
    cv.fechar()
    cv.px(9, 3, hexc("#4FA34A"))
    cv.px(10, 2, hexc("#7BCB6A"))
    cv.px(10, 3, hexc("#4FA34A"))


def ashen_weave(cv, t, c):
    for y, (a, b) in enumerate([(3, 12), (2, 13), (2, 13), (2, 13), (3, 13), (2, 13), (2, 12), (2, 13), (2, 13), (3, 12)], start=4):
        cv.ret(a, y, b, y, lit(c, 0.15) if y in (4, 8, 12) else (drk(c, 0.2) if y in (7, 11) else c))
    cv.fechar()
    p = int(t * 12)
    for i in range(12):
        x = 2 + i
        cv.px(x, 9, (210, 190, 255, 255) if abs(i - p) <= 1 else ((150, 130, 190, 255) if i % 2 == 0 else None))
    for i, x in enumerate((4, 9, 12)):
        f = (t + i * 0.33) % 1
        cv.px(x + math.sin(f * 6), f * 5 - 1, alfa((190, 185, 180, 255), 200 * (1 - f)))


def archmage_codex(cv, t, c):
    livro(cv, (70, 30, 90, 255), c, t)
    cv.fechar()
    k = onda(t)
    cv.disco(7.5, 8, 1.6, lit((90, 170, 255, 255), 0.2 + 0.5 * k))
    for d in (2.6,):
        for i in range(8):
            a = 2 * math.pi * (i / 8 + t / 4)
            cv.px(7.5 + d * math.cos(a), 8 + d * math.sin(a), alfa(c, 120 + 120 * ((i + int(t * 8)) % 2)))
    cv.brilho(t, forca=0.35)


def soul_thread(cv, t, c):
    madeira = hexc("#A0703E")
    cv.ret(3, 3, 10, 4, madeira)
    cv.ret(3, 12, 10, 13, madeira)
    cv.ret(4, 5, 9, 11, lambda x, y: lit(c, 0.2) if (x + y) % 2 == 0 else c)
    cv.fechar()
    for i in range(6):
        x = 10 + i * 0.8
        y = 8 + 2 * math.sin(2 * math.pi * t + i * 0.9)
        cv.px(x, y, lit(c, 0.3))
    p = int(t * 6)
    cv.faisca(10 + p * 0.8, 8 + 2 * math.sin(2 * math.pi * t + p * 0.9), 0.6, (255, 240, 255, 255))


def malignant_core(cv, t, c):
    batida = math.exp(-((t - 0.15) * 10) ** 2) + 0.7 * math.exp(-((t - 0.35) * 10) ** 2)
    r = 5.4 + 0.7 * batida
    coracao = lambda x, y: ((x - 7.5) / r) ** 2 + (-(y - 9.5) / r - 0.8 * math.sqrt(abs((x - 7.5) / r))) ** 2 <= 0.8
    for y in range(16):
        for x in range(16):
            if coracao(x, y + 0.5):
                cv.px(x, y, lit(c, 0.25) if x + y < 11 else (drk(c, 0.25) if x + y > 18 else c))
    cv.fechar()
    veia = mix(drk(c, 0.5), (255, 70, 120, 255), batida)
    for x, y in ((7, 5), (7, 6), (8, 7), (6, 8), (9, 9), (5, 9), (10, 10)):
        if (x, y) in cv.corpo:
            cv.px(x, y, veia)


def prism_matrix(cv, t, c):
    for cx, cy in ((7.5, 3.5), (4, 7.5), (11, 7.5), (7.5, 11.5), (7.5, 7.5)):
        for y in range(16):
            for x in range(16):
                if abs(x - cx) + abs(y - cy) <= 2.6:
                    cv.px(x, y, lit(c, 0.3) if y < cy else drk(c, 0.15))
    cv.fechar()
    pos = -6 + t * 40
    for (x, y) in cv.corpo:
        d = abs(x + y - pos)
        if d < 3:
            cv.p[y][x] = mix(cv.p[y][x], hsv(((x - y) / 16 + t) % 1, 0.6, 1), 0.7 * (1 - d / 3))


def legend_core(cv, t, c):
    orbe(cv, c, 7.5, 8, 6)
    cv.fechar()
    prata = (225, 228, 235, 255)
    cv.ret(7, 3, 8, 10, prata)
    cv.ret(5, 10, 10, 10, OURO)
    cv.ret(7, 11, 8, 12, drk(OURO, 0.3))
    cv.px(8, 3, (255, 255, 255, 255))
    cv.brilho(t, forca=0.5)
    cv.faisca(12, 3, onda(t, 0.5), lit(c, 0.6))


def mythic_core(cv, t, c):
    a0 = 2 * math.pi * t
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 8
            d = math.hypot(dx, dy)
            if d <= 6:
                lado = math.cos(math.atan2(dy, dx) - a0) + 0.3 * math.sin(d * 0.9 - a0 * 2)
                base = c if lado > 0 else (36, 22, 70, 255)
                cv.px(x, y, esfera(base, 7.5, 8, 6)(x, y, dx, dy, d))
    cv.fechar()
    for i in range(4):
        a = a0 + math.pi + (i - 1.5) * 0.5
        cv.px(7.5 + 3.5 * math.cos(a), 8 + 3.5 * math.sin(a), (255, 255, 255, 230))


def vintners_yeast(cv, t, c):
    barro = hexc("#9C6B44")
    cv.ret(4, 6, 11, 14, barro)
    cv.ret(3, 7, 12, 13, barro)
    cv.ret(5, 15, 10, 15, drk(barro, 0.3))
    cv.ret(4, 6, 11, 6, drk(barro, 0.2))
    cv.ret(4, 4, 11, 5, c)
    cv.ret(5, 3, 10, 3, lit(c, 0.4))
    cv.ret(4, 8, 4, 12, lit(barro, 0.25))
    cv.fechar()
    for i, x in enumerate((5, 8, 10)):
        f = (t * 2 + i * 0.4) % 1
        cv.px(x, 3 - f * 2.5, alfa(lit(c, 0.6), 255 * (1 - f)))
    cv.px(7, 8, (110, 40, 90, 255))
    cv.px(8, 9, (110, 40, 90, 255))
    cv.px(7, 10, (110, 40, 90, 255))


def eternity_essence(cv, t, c):
    liq = hsv((0.42 + 0.08 * math.sin(2 * math.pi * t)) % 1, 0.5, 0.9)
    cv.ret(7, 0, 8, 1, OURO)
    cv.ret(7, 2, 8, 3, VIDRO)
    cv.disco(7.5, 9.5, 5.5, lambda x, y, dx, dy, d: lit(liq, 0.3) if dx < -2 else (drk(liq, 0.25) if dx > 2 else liq) if y > 5 else VIDRO)
    cv.fechar()
    for i in range(12):  # símbolo do infinito correndo
        a = 2 * math.pi * i / 12
        x, y = 7.5 + 3 * math.sin(a), 10 + 1.5 * math.sin(2 * a)
        v = (i / 12 - t) % 1
        cv.px(x, y, alfa((255, 255, 255, 255), 240 * (1 - v) ** 2))


def _pigmento(cv, t, c, enfeite):
    tigela(cv, c, t)
    enfeite(cv)
    cv.fechar()
    for i, (x, y) in enumerate(((5, 7), (10, 6), (8, 5))):
        cv.faisca(x, y - 1, max(0, math.sin(2 * math.pi * (t + i / 3))), lit(c, 0.7))


def pigmento_raro(cv, t, c):
    _pigmento(cv, t, c, lambda v: (v.px(11, 4, hexc("#6FCB5A")), v.px(12, 3, hexc("#8FE07A")), v.px(11, 5, hexc("#3F8A35"))))


def pigmento_epico(cv, t, c):
    _pigmento(cv, t, c, lambda v: (v.px(3, 4, hexc("#E08AE0")), v.px(4, 4, hexc("#C060C8")), v.px(3, 5, hexc("#C060C8"))))


def pigmento_lendario(cv, t, c):
    def lua(v):
        for x, y in ((11, 1), (12, 1), (10, 2), (10, 3), (10, 4), (11, 5), (12, 5)):
            v.px(x, y, (235, 240, 255, 255))
    _pigmento(cv, t, c, lua)
    cv.px(13, 3, alfa((200, 210, 255, 255), 160 * onda(t)))


def exo_plating(cv, t, c):
    cv.linhas(["................", "..XXXX....XXXX..", ".XXXXXX..XXXXXX.", ".XXXXXXXXXXXXXX.", ".XXXXXXXXXXXXXX.",
               "..XXXXXXXXXXXX..", "..XXXXXXXXXXXX..", "..XXXXXXXXXXXX..", "..XXXXXXXXXXXX..", "...XXXXXXXXXX...",
               "...XXXXXXXXXX...", "....XXXXXXXX....", ".....XXXXXX.....", "................", "................",
               "................"], {"X": c}, 0, 1)
    for (x, y) in [(x, y) for y in range(16) for x in range(16) if cv.get(x, y)]:
        if x + y < 9:
            cv.px(x, y, lit(c, 0.25))
    cv.fechar()
    quente = mix((120, 30, 20, 255), (255, 120, 40, 255), onda(t))
    for y in range(4, 13):
        cv.px(7, y, quente)
        cv.px(8, y, drk(quente, 0.2))
    for x, y in ((3, 4), (12, 4), (4, 9), (11, 9)):
        cv.px(x, y, (190, 180, 170, 255))


def steam_rivets(cv, t, c):
    for cx, cy in ((4.5, 11), (11, 11), (7.5, 6.5)):
        cv.disco(cx, cy, 2.4, esfera(c, cx, cy, 2.4, 0.6, 0.5))
        cv.ret(int(cx), int(cy) + 2, int(cx), int(cy) + 3, drk(c, 0.3))
    cv.fechar()
    for i in range(4):
        f = (t + i / 4) % 1
        x = 7.5 + 2.5 * math.sin(f * 7 + i)
        y = 3 - f * 3
        cv.px(x, y + 1, alfa((235, 235, 235, 255), 200 * (1 - f)))
        if f < 0.5:
            cv.px(x + 1, y + 1, alfa((235, 235, 235, 255), 130 * (1 - f)))


def _cristal(cv, cx, base, alt=11, topo=2):
    for y in range(topo, topo + alt):
        meia = min(y - topo, (topo + alt - y) * 0.7, 2.6)
        for x in range(16):
            if abs(x - cx) <= meia:
                cv.px(x, y, lit(base, 0.45) if x < cx - 0.3 else (drk(base, 0.2) if x > cx + 0.7 else base))


def replication_matrix(cv, t, c):
    _cristal(cv, 5, c, 12, 2)
    cv.fechar()
    eco = onda(t)
    for y in range(3, 15):
        meia = min(y - 3, (15 - y) * 0.7, 2.6)
        for x in range(16):
            if abs(x - 11) <= meia:
                cv.px(x, y, alfa(lit(c, 0.3), 60 + 160 * eco))
    for i in range(3):
        f = (t + i / 3) % 1
        cv.px(6 + f * 5, 8 + math.sin(f * 6.3) * 2, alfa((255, 255, 255, 255), 255 * math.sin(math.pi * f)))


def guild_charter(cv, t, c):
    pergaminho(cv)
    cv.ret(2, 6, 13, 7, c)
    cv.fechar()
    cv.disco(10.5, 11, 2.3, esfera(LACRE, 10.5, 11, 2.3))
    cv.px(10, 15, LACRE)
    cv.px(11, 14, drk(LACRE, 0.2))
    cv.corpo = {(x, y) for (x, y) in cv.corpo} | {(x, y) for y in range(8, 14) for x in range(8, 14) if cv.get(x, y)}
    cv.brilho(t, forca=0.45)


def draught_of_oblivion(cv, t, c):
    garrafa_alta(cv, drk(c, 0.2), 6, t)
    cv.fechar()
    for i in range(7):
        a = 2 * math.pi * (t + i / 7)
        cv.px(7.5 + 2.6 * math.cos(a), 10.5 + 3 * math.sin(a) * math.cos(a * 0.5), alfa((200, 215, 235, 255), 90 + 20 * i))
    cv.faisca(7, 9, onda(t, 0.3), (230, 240, 255, 255))


def convergence_matrix(cv, t, c):
    orbe(cv, lit(c, 0.2), 7.5, 7.5, 3.6)
    cv.fechar()
    cv.px(7, 7, (255, 255, 255, 255))
    cores = [hexc("#E8A33A"), hexc("#A35BE8"), hexc("#D9443A"), hexc("#4CC93C")]
    for i, cor in enumerate(cores):
        a = 2 * math.pi * (t + i / 4)
        x, y = 7.5 + 6 * math.cos(a), 7.5 + 6 * math.sin(a)
        for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
            cv.px(x - 0.5 + dx, y - 0.5 + dy, cor if (dx, dy) != (0, 0) else lit(cor, 0.5))
        x2, y2 = 7.5 + 6 * math.cos(a - 0.35), 7.5 + 6 * math.sin(a - 0.35)
        cv.px(x2, y2, alfa(cor, 110))


def guild_letter(cv, t, c):
    cv.ret(1, 4, 14, 12, c)
    for x in range(1, 15):  # aba em V
        y = 4 + min(x - 1, 14 - x) * 0.6
        for yy in range(4, int(y) + 1):
            cv.px(x, yy, lit(c, 0.2))
        cv.px(x, int(y) + 1, drk(c, 0.25))
    cv.fechar()
    cv.disco(7.5, 9, 1.8, esfera(LACRE, 7.5, 9, 1.8))
    cv.brilho(t, forca=0.4)


def school_codex(cv, t, c):
    livro(cv, drk(c, 0.45), OURO, t)
    cv.fechar()
    cores = [hexc(h) for h in ("#FF6A2E", "#7FD0FF", "#FFE45C", "#E8F0FF", "#3FC24A", "#A35BE8", "#7A1F1F", "#E46ACF")]
    ativo = int(t * 8)
    for i, cor in enumerate(cores):
        a = 2 * math.pi * i / 8 - math.pi / 2
        x, y = 7.5 + 3 * math.cos(a), 8.5 + 3 * math.sin(a)
        cv.px(x, y, lit(cor, 0.5) if i == ativo else drk(cor, 0.2))
    cv.px(7.5, 8.5, alfa(cores[ativo], 255))


def generico(forma_png):
    """Item sem desenho próprio: forma antiga com um brilho passando."""
    def f(cv, t, c):
        cv.linhas(forma_png, {"o": None, "b": c, "c": lit(c, 0.4), "w": lit(c, 0.8)})
        cv.fechar()
        cv.brilho(t)
    return f


DESENHOS = {f.__name__: f for f in (
    runic_flux, cursed_toxin, arcane_inscription, stellar_core, terra_core, ember_core, alchemical_regulator,
    spirit_binding, runic_slate, crucible_primer, void_shard, vital_flask_base, ashen_weave, archmage_codex,
    soul_thread, malignant_core, prism_matrix, legend_core, mythic_core, vintners_yeast, eternity_essence,
    pigmento_raro, pigmento_epico, pigmento_lendario, exo_plating, steam_rivets, replication_matrix,
    guild_charter, draught_of_oblivion, convergence_matrix, guild_letter, school_codex)}


def quadros(item_id, cor, forma=None):
    from PIL import Image
    desenho = DESENHOS.get(item_id) or generico(forma)
    tira = Image.new("RGBA", (16, 16 * QUADROS))
    for i in range(QUADROS):
        cv = Tela()
        desenho(cv, i / QUADROS, hexc(cor))
        tira.paste(cv.imagem(), (0, 16 * i))
    return tira


def escrever(pasta, item_id, cor, forma=None):
    """Grava <pasta>/<id>.png (tira de quadros) e <id>.png.mcmeta."""
    pasta = Path(pasta)
    pasta.mkdir(parents=True, exist_ok=True)
    quadros(item_id, cor, forma).save(pasta / f"{item_id}.png")
    (pasta / f"{item_id}.png.mcmeta").write_text(json.dumps(
        {"animation": {"frametime": TEMPO_QUADRO, "interpolate": False}}, indent=2) + "\n", encoding="utf-8")


def previa(saida):
    """Folha ampliada: uma linha por item, 8 quadros por linha."""
    from PIL import Image
    cfg = json.loads((Path(__file__).resolve().parents[1] / "progressao.json").read_text(encoding="utf-8"))
    itens = [(x["id"], x["cor"]) for x in cfg["componentes"] + cfg["itens_especiais"]]
    z, cols = 5, 8
    folha = Image.new("RGBA", (cols * 16 * z + (cols + 1) * 4, len(itens) * (16 * z + 4) + 4), (40, 40, 48, 255))
    for j, (iid, cor) in enumerate(itens):
        tira = quadros(iid, cor)
        for i in range(cols):
            q = tira.crop((0, 16 * i * 2, 16, 16 * i * 2 + 16)).resize((16 * z, 16 * z), Image.NEAREST)
            folha.alpha_composite(q, (4 + i * (16 * z + 4), 4 + j * (16 * z + 4)))
    folha.save(saida)
    return saida


if __name__ == "__main__":
    print(previa(Path(__file__).with_name("icones_previa.png")))
