#!/usr/bin/env python3
"""
Generates the APhone resource pack (by @Adryh01) textures/models/fonts (and the README previews).
Usage:  python3 tools/generate_pack.py
Needs:  pip install pillow
"""
import json
import math
import os
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PACK = os.path.join(ROOT, "resourcepack")
TEX = os.path.join(PACK, "assets", "aphone", "textures")
MODELS = os.path.join(PACK, "assets", "aphone", "models", "item")

SS = 4  # supersampling for icons

# CustomModelData values - must match dev.adryh01.aphone.gui.Icon
ICONS = {
    "app_phone": 2001, "app_messages": 2002, "app_contacts": 2003, "app_gps": 2004,
    "app_flashlight": 2005, "app_weather": 2006, "app_notes": 2007, "app_calculator": 2008,
    "app_emergency": 2009, "app_settings": 2010, "home": 2020, "back": 2021, "add": 2022,
    "contact": 2023, "sms": 2024, "call_accept": 2025, "call_end": 2026, "info": 2027,
    "prev": 2028, "next": 2029, "toggle_on": 2030, "toggle_off": 2031, "location": 2032,
    "note": 2033,
}
PHONE_CMD = 1001


def mk(path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    return path


def hexc(h, a=255):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (a,)


def lerp(c1, c2, t):
    return tuple(int(c1[i] + (c2[i] - c1[i]) * t) for i in range(len(c1)))


def gradient(w, h, top, bottom):
    img = Image.new("RGBA", (w, h))
    px = img.load()
    for y in range(h):
        c = lerp(top, bottom, y / max(1, h - 1))
        for x in range(w):
            px[x, y] = c
    return img


# ---------------------------------------------------------------- icons
def tile(top, bottom):
    """Rounded app tile (128x128 canvas) with a vertical gradient."""
    S = 32 * SS
    base = gradient(S, S, hexc(top), hexc(bottom))
    mask = Image.new("L", (S, S), 0)
    ImageDraw.Draw(mask).rounded_rectangle((2 * SS, 2 * SS, S - 2 * SS - 1, S - 2 * SS - 1), radius=8 * SS, fill=255)
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    img.paste(base, (0, 0), mask)
    # subtle top gloss
    gloss = Image.new("RGBA", (S, S), (255, 255, 255, 0))
    gd = ImageDraw.Draw(gloss)
    gd.rounded_rectangle((2 * SS, 2 * SS, S - 2 * SS - 1, S // 2), radius=8 * SS, fill=(255, 255, 255, 28))
    img = Image.alpha_composite(img, Image.composite(gloss, Image.new("RGBA", (S, S), (0, 0, 0, 0)), mask))
    return img


def blank():
    S = 32 * SS
    return Image.new("RGBA", (S, S), (0, 0, 0, 0))


def P(*v):
    """scale 32px coords to supersampled canvas"""
    return tuple(int(x * SS) for x in v)


def handset(d, cx, cy, size, color, rot=0):
    """Draws a phone handset glyph."""
    pts = []
    r = size
    # C shaped arc
    for a in range(200, 341, 5):
        ang = math.radians(a + rot)
        pts.append((cx + r * math.cos(ang), cy + r * math.sin(ang)))
    w = size * 0.42
    d.line([P(*p) for p in pts], fill=color, width=int(w * SS), joint="curve")
    for a in (200, 340):
        ang = math.radians(a + rot)
        ex, ey = cx + r * math.cos(ang), cy + r * math.sin(ang)
        d.ellipse(P(ex - w * 0.75, ey - w * 0.75, ex + w * 0.75, ey + w * 0.75), fill=color)


def person(d, cx, cy, s, color):
    d.ellipse(P(cx - s * 0.32, cy - s * 0.62, cx + s * 0.32, cy + 0.02 * s), fill=color)
    d.pieslice(P(cx - s * 0.62, cy + 0.1 * s, cx + s * 0.62, cy + 1.3 * s), 180, 360, fill=color)


def gear(d, cx, cy, r, color, hole):
    for i in range(8):
        a = math.radians(i * 45)
        pts = []
        for da, rr in ((-12, r * 0.75), (-8, r * 1.05), (8, r * 1.05), (12, r * 0.75)):
            aa = a + math.radians(da)
            pts.append(P(cx + rr * math.cos(aa), cy + rr * math.sin(aa)))
        d.polygon(pts, fill=color)
    d.ellipse(P(cx - r * 0.8, cy - r * 0.8, cx + r * 0.8, cy + r * 0.8), fill=color)
    d.ellipse(P(cx - r * 0.32, cy - r * 0.32, cx + r * 0.32, cy + r * 0.32), fill=hole)


def pin(d, cx, cy, s, color, hole):
    d.ellipse(P(cx - s, cy - s, cx + s, cy + s), fill=color)
    d.polygon([P(cx - s * 0.85, cy + s * 0.5), P(cx + s * 0.85, cy + s * 0.5), P(cx, cy + s * 2.1)], fill=color)
    d.ellipse(P(cx - s * 0.4, cy - s * 0.4, cx + s * 0.4, cy + s * 0.4), fill=hole)


def arrow(d, color, left=True, thick=3.2):
    if left:
        d.line([P(20, 16), P(10, 16)], fill=color, width=int(thick * SS))
        d.line([P(15, 10), P(9.5, 16), P(15, 22)], fill=color, width=int(thick * SS), joint="curve")
    else:
        d.line([P(12, 16), P(22, 16)], fill=color, width=int(thick * SS))
        d.line([P(17, 10), P(22.5, 16), P(17, 22)], fill=color, width=int(thick * SS), joint="curve")


WHITE = (255, 255, 255, 255)


def build_icons():
    icons = {}

    img = tile("#5BE37D", "#1FAA4A"); handset(ImageDraw.Draw(img), 17.5, 14.5, 8.5, WHITE, rot=-135)
    icons["app_phone"] = img

    img = tile("#5AC8FA", "#1A7CF0"); d = ImageDraw.Draw(img)
    d.ellipse(P(6, 7, 26, 23), fill=WHITE)
    d.polygon([P(9, 19), P(8, 26), P(15, 21)], fill=WHITE)
    for x in (11, 16, 21):
        d.ellipse(P(x - 1.4, 13.6, x + 1.4, 16.4), fill=hexc("#1A7CF0"))
    icons["app_messages"] = img

    img = tile("#FFB347", "#F57C00"); d = ImageDraw.Draw(img)
    person(d, 16, 14, 11, WHITE)
    icons["app_contacts"] = img

    img = tile("#4FD1C5", "#127A8A"); d = ImageDraw.Draw(img)
    d.polygon([P(5, 9), P(12, 6), P(20, 9), P(27, 6), P(27, 24), P(20, 27), P(12, 24), P(5, 27)], fill=(255, 255, 255, 90))
    pin(d, 16, 12.5, 5.2, hexc("#FF4D4D"), WHITE)
    icons["app_gps"] = img

    img = tile("#3A3A40", "#18181C"); d = ImageDraw.Draw(img)
    d.polygon([P(10, 5), P(22, 5), P(20, 12), P(12, 12)], fill=hexc("#FFE066"))
    d.rounded_rectangle(P(12.5, 12, 19.5, 27), radius=2 * SS, fill=hexc("#D9D9DF"))
    d.rectangle(P(14.6, 16, 17.4, 19), fill=hexc("#FF5A5A"))
    icons["app_flashlight"] = img

    img = tile("#7EC8FF", "#2E7BD6"); d = ImageDraw.Draw(img)
    d.ellipse(P(7, 6, 19, 18), fill=hexc("#FFD43B"))
    for cx, cy, r in ((14, 20, 5), (19, 17.5, 6), (24, 20.5, 4.5)):
        d.ellipse(P(cx - r, cy - r, cx + r, cy + r), fill=WHITE)
    d.rectangle(P(10, 20, 25, 25), fill=WHITE)
    icons["app_weather"] = img

    img = tile("#FFF3B0", "#FFD45C"); d = ImageDraw.Draw(img)
    d.rectangle(P(4, 4, 28, 10), fill=hexc("#F2B705"))
    for y in (14, 18, 22):
        d.line([P(8, y), P(24, y)], fill=hexc("#B08A2E"), width=int(1.6 * SS))
    icons["app_notes"] = img

    img = tile("#5C5C66", "#2A2A30"); d = ImageDraw.Draw(img)
    d.rounded_rectangle(P(7, 6, 25, 11), radius=1 * SS, fill=hexc("#B8F2C8"))
    for r in range(3):
        for c in range(3):
            col = hexc("#FF9F0A") if c == 2 else hexc("#D0D0D8")
            x, y = 7.5 + c * 6, 13.5 + r * 4.6
            d.rounded_rectangle(P(x, y, x + 4.5, y + 3.4), radius=1 * SS, fill=col)
    icons["app_calculator"] = img

    img = tile("#FF5E5E", "#C41919"); d = ImageDraw.Draw(img)
    d.rectangle(P(13, 6, 19, 26), fill=WHITE)
    d.rectangle(P(6, 13, 26, 19), fill=WHITE)
    icons["app_emergency"] = img

    img = tile("#A0A0AA", "#5E5E68"); d = ImageDraw.Draw(img)
    gear(d, 16, 16, 9.5, hexc("#F0F0F4"), hexc("#7A7A84"))
    icons["app_settings"] = img

    img = blank(); d = ImageDraw.Draw(img)
    d.ellipse(P(5, 5, 27, 27), fill=(255, 255, 255, 60), outline=WHITE, width=int(2.2 * SS))
    d.rounded_rectangle(P(11, 11, 21, 21), radius=2 * SS, outline=WHITE, width=int(1.8 * SS))
    icons["home"] = img

    img = blank(); arrow(ImageDraw.Draw(img), WHITE, True); icons["back"] = img
    img = blank(); arrow(ImageDraw.Draw(img), (255, 255, 255, 200), True, 2.6); icons["prev"] = img
    img = blank(); arrow(ImageDraw.Draw(img), (255, 255, 255, 200), False, 2.6); icons["next"] = img

    img = blank(); d = ImageDraw.Draw(img)
    d.ellipse(P(4, 4, 28, 28), fill=hexc("#34C759"))
    d.rectangle(P(14.5, 9, 17.5, 23), fill=WHITE); d.rectangle(P(9, 14.5, 23, 17.5), fill=WHITE)
    icons["add"] = img

    img = blank(); d = ImageDraw.Draw(img)
    d.ellipse(P(3, 3, 29, 29), fill=hexc("#8E8E99"))
    m = Image.new("L", img.size, 0); ImageDraw.Draw(m).ellipse(P(3, 3, 29, 29), fill=255)
    p2 = blank(); person(ImageDraw.Draw(p2), 16, 15, 12, hexc("#E6E6EE"))
    img.paste(p2, (0, 0), Image.composite(p2, blank(), m).getchannel("A"))
    icons["contact"] = img

    img = blank(); d = ImageDraw.Draw(img)
    d.rounded_rectangle(P(4, 8, 28, 25), radius=2 * SS, fill=hexc("#5AC8FA"))
    d.line([P(5, 9), P(16, 18), P(27, 9)], fill=WHITE, width=int(2 * SS), joint="curve")
    icons["sms"] = img

    img = blank(); d = ImageDraw.Draw(img)
    d.ellipse(P(3, 3, 29, 29), fill=hexc("#34C759")); handset(d, 17.2, 14.8, 7, WHITE, rot=-135)
    icons["call_accept"] = img
    img = blank(); d = ImageDraw.Draw(img)
    d.ellipse(P(3, 3, 29, 29), fill=hexc("#FF3B30")); handset(d, 16, 20, 7.5, WHITE, rot=0)
    icons["call_end"] = img

    img = blank(); d = ImageDraw.Draw(img)
    d.ellipse(P(4, 4, 28, 28), fill=hexc("#0A84FF"))
    d.ellipse(P(14.3, 8, 17.7, 11.4), fill=WHITE); d.rectangle(P(14.5, 13.5, 17.5, 23.5), fill=WHITE)
    icons["info"] = img

    for name, on in (("toggle_on", True), ("toggle_off", False)):
        img = blank(); d = ImageDraw.Draw(img)
        d.rounded_rectangle(P(3, 9, 29, 23), radius=7 * SS, fill=hexc("#34C759") if on else hexc("#6E6E78"))
        x = 22 if on else 10
        d.ellipse(P(x - 5.5, 10.5, x + 5.5, 21.5), fill=WHITE)
        icons[name] = img

    img = blank(); d = ImageDraw.Draw(img)
    d.polygon([P(3, 8), P(11, 5), P(21, 8), P(29, 5), P(29, 25), P(21, 28), P(11, 25), P(3, 28)], fill=hexc("#E9F5E1"))
    d.line([P(11, 5), P(11, 25)], fill=hexc("#B9D3A9"), width=SS); d.line([P(21, 8), P(21, 28)], fill=hexc("#B9D3A9"), width=SS)
    pin(d, 16, 12, 4.6, hexc("#FF3B30"), WHITE)
    icons["location"] = img

    img = blank(); d = ImageDraw.Draw(img)
    d.polygon([P(7, 4), P(21, 4), P(26, 9), P(26, 28), P(7, 28)], fill=hexc("#FFF6C8"))
    d.polygon([P(21, 4), P(26, 9), P(21, 9)], fill=hexc("#E5D58F"))
    for y in (13, 17, 21, 25):
        d.line([P(10, y), P(23, y)], fill=hexc("#C9B160"), width=int(1.2 * SS))
    icons["note"] = img

    out = {}
    for name, im in icons.items():
        small = im.resize((32, 32), Image.LANCZOS)
        small.save(mk(os.path.join(TEX, "item", "icon", name + ".png")))
        out[name] = small
    return out


# ---------------------------------------------------------------- phone item
def build_phone_texture():
    """32x32 texture: front (0,0 14x26), back (16,0 14x26), edge (0,28 32x4)."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # front
    d.rectangle((0, 0, 13, 25), fill=hexc("#16161A"))
    scr = gradient(12, 22, hexc("#3A1C71"), hexc("#FF8A65"))
    img.paste(scr, (1, 2))
    d.point([(6, 0), (7, 0)], fill=hexc("#3C3C44"))  # speaker
    for (x, y, c) in ((2, 5, "#34C759"), (5, 5, "#0A84FF"), (8, 5, "#FF9F0A"), (11, 5, "#FF3B30"),
                      (2, 8, "#FFD60A"), (5, 8, "#5AC8FA"), (8, 8, "#8E8E99"), (11, 8, "#BF5AF2")):
        d.rectangle((x, y, x + 1, y + 1), fill=hexc(c))
    d.rectangle((3, 21, 10, 21), fill=hexc("#34C759", 200))  # dock
    d.line((5, 24, 8, 24), fill=hexc("#E0E0E0", 200))  # home bar
    # back
    d.rectangle((16, 0, 29, 25), fill=hexc("#2B2B33"))
    d.rectangle((17, 1, 28, 24), fill=hexc("#33333D"))
    d.rounded_rectangle((17, 1, 22, 8), radius=1, fill=hexc("#1C1C22"))
    d.point([(18, 2), (21, 2), (18, 5)], fill=hexc("#5E7A9A"))
    d.point([(21, 5)], fill=hexc("#FFE680"))
    d.rectangle((22, 12, 23, 13), fill=hexc("#9A9AA6"))
    # edge
    d.rectangle((0, 28, 31, 31), fill=hexc("#0E0E11"))
    img.save(mk(os.path.join(TEX, "item", "phone.png")))
    return img


def write_models():
    # 3D phone: 7 x 13 x 1 px slab
    phone_model = {
        "credit": "APhone by @Adryh01",
        "texture_size": [32, 32],
        "textures": {"0": "aphone:item/phone", "particle": "aphone:item/phone"},
        "elements": [{
            "from": [4.5, 1.5, 7.5], "to": [11.5, 14.5, 8.5],
            "faces": {
                "south": {"uv": [0, 0, 7, 13], "texture": "#0"},
                "north": {"uv": [8, 0, 15, 13], "texture": "#0"},
                "east": {"uv": [0, 14, 0.5, 16], "texture": "#0"},
                "west": {"uv": [0, 14, 0.5, 16], "texture": "#0"},
                "up": {"uv": [0, 14, 7, 14.5], "texture": "#0"},
                "down": {"uv": [0, 14, 7, 14.5], "texture": "#0"}
            }
        }],
        "display": {
            "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 2.5, 1.5], "scale": [0.6, 0.6, 0.6]},
            "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 2.5, 1.5], "scale": [0.6, 0.6, 0.6]},
            "firstperson_righthand": {"rotation": [0, -15, 0], "translation": [-1, 3.5, -1], "scale": [0.7, 0.7, 0.7]},
            "firstperson_lefthand": {"rotation": [0, 15, 0], "translation": [-1, 3.5, -1], "scale": [0.7, 0.7, 0.7]},
            "ground": {"translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
            "gui": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1.15, 1.15, 1.15]},
            "head": {"translation": [0, 10, 0]},
            "fixed": {"rotation": [0, 180, 0], "scale": [1, 1, 1]}
        }
    }
    with open(mk(os.path.join(MODELS, "phone.json")), "w") as f:
        json.dump(phone_model, f, indent=2)

    for name in ICONS:
        with open(mk(os.path.join(MODELS, "icon", name + ".json")), "w") as f:
            json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "aphone:item/icon/" + name}}, f, indent=2)

    overrides = [{"predicate": {"custom_model_data": PHONE_CMD}, "model": "aphone:item/phone"}]
    for name, cmd in sorted(ICONS.items(), key=lambda kv: kv[1]):
        overrides.append({"predicate": {"custom_model_data": cmd}, "model": "aphone:item/icon/" + name})
    paper = {"parent": "minecraft:item/generated", "textures": {"layer0": "minecraft:item/paper"}, "overrides": overrides}
    with open(mk(os.path.join(PACK, "assets", "minecraft", "models", "item", "paper.json")), "w") as f:
        json.dump(paper, f, indent=2)


# ---------------------------------------------------------------- GUI background
# The phone window is a DISPENSER (176 x 166 GUI). Its texture is replaced by a transparent one
# (assets/minecraft/textures/gui/container/dispenser.png), so only our narrow phone is visible.
# Slots on the phone (5 middle columns):
#   dispenser 3x3 : x 62/80/98,           y 17/35/53     raw 0..8
#   inventory     : x 44..116 (cols 2-6), y 84/102/120   raw 9..35
#   hotbar (dock) : x 44..116 (cols 2-6), y 142          raw 36..44
# The glyph is 176 x GUI_H and starts TOP px above the GUI.
# NOTE: in a bitmap font "ascent" can never be bigger than "height".
GUI_W = 176
TOP = -28
GUI_H = 212           # -> bottom at GUI y 183
ASCENT = 13 - TOP     # title text top is at y=6 with ascent 7 -> glyph top = 13 - ascent
BODY = (36, 140)      # phone body x range
DISP_Y = [17, 35, 53]
INV_Y = [84, 102, 120]
HOTBAR_Y = 142


def gy(y):
    """GUI y -> image y"""
    return y - TOP


def slot_x(col):
    return 8 + col * 18


def disp_x(col):
    return 62 + col * 18


# Home labels: (center x, icon y, text) - MUST match HomeMenu.java
HOME_LABELS = [
    (disp_x(0) + 8, DISP_Y[0], "Maps"), (disp_x(2) + 8, DISP_Y[0], "Weather"),
    (disp_x(0) + 8, DISP_Y[2], "Notes"), (disp_x(2) + 8, DISP_Y[2], "Calc"),
    (slot_x(2) + 8, INV_Y[0], "Light"), (slot_x(4) + 8, INV_Y[0], "Settings"), (slot_x(6) + 8, INV_Y[0], "911"),
]

# tiny pixel font (rows 0-4 = body, row 5 = descender)
PIX = {
    "L": ["#..", "#..", "#..", "#..", "###"],
    "M": ["#...#", "##.##", "#.#.#", "#...#", "#...#"],
    "i": ["#", ".", "#", "#", "#"],
    "g": ["...", ".##", "#.#", ".##", "..#", "##."],
    "h": ["#..", "#..", "##.", "#.#", "#.#"],
    "t": [".#.", "###", ".#.", ".#.", "..#"],
    "W": ["#...#", "#...#", "#.#.#", "#.#.#", ".#.#."],
    "e": ["...", ".#.", "#.#", "##.", ".##"],
    "a": ["...", "##.", ".##", "#.#", "###"],
    "p": ["...", "##.", "#.#", "#.#", "##.", "#.."],
    "r": ["...", "#.#", "##.", "#..", "#.."],
    "N": ["#...#", "##..#", "#.#.#", "#..##", "#...#"],
    "o": ["...", ".#.", "#.#", "#.#", ".#."],
    "s": ["...", ".##", "#..", "..#", "##."],
    "C": [".##", "#..", "#..", "#..", ".##"],
    "l": ["#", "#", "#", "#", "#"],
    "c": ["...", ".##", "#..", "#..", ".##"],
    "9": ["###", "#.#", "###", "..#", "###"],
    "1": [".#", "##", ".#", ".#", ".#"],
    "S": [".##", "#..", ".#.", "..#", "##."],
    "n": ["...", "##.", "#.#", "#.#", "#.#"],
}


def pix_width(text):
    return sum(len(PIX[ch][0]) for ch in text) + len(text) - 1


def pix_text(img, cx, y, text):
    x = cx - pix_width(text) // 2
    px = img.load()
    for ch in text:
        g = PIX[ch]
        for ry, row in enumerate(g):
            for rx, v in enumerate(row):
                if v == "#":
                    r, gg, b, a = px[x + rx + 1, y + ry + 1]
                    px[x + rx + 1, y + ry + 1] = (r // 2, gg // 2, b // 2, a)
        for ry, row in enumerate(g):
            for rx, v in enumerate(row):
                if v == "#":
                    px[x + rx, y + ry] = (255, 255, 255, 255)
        x += len(g[0]) + 1


def phone_base():
    img = Image.new("RGBA", (GUI_W, GUI_H), (0, 0, 0, 0))
    bx0, bx1 = BODY
    body = Image.new("RGBA", (GUI_W, GUI_H), (0, 0, 0, 0))
    bd = ImageDraw.Draw(body)
    bd.rounded_rectangle((bx0, 0, bx1, GUI_H - 1), radius=16, fill=hexc("#111114"))
    bd.rounded_rectangle((bx0, 0, bx1, GUI_H - 1), radius=16, outline=hexc("#3B3B45"))
    bd.rounded_rectangle((bx0 + 1, 1, bx1 - 1, GUI_H - 2), radius=15, outline=hexc("#24242B"))
    img = Image.alpha_composite(img, body)

    sx0, sy0, sx1, sy1 = bx0 + 3, 3, bx1 - 3, GUI_H - 4
    sw, sh = sx1 - sx0 + 1, sy1 - sy0 + 1
    wall = Image.new("RGBA", (sw, sh))
    wp = wall.load()
    c1, c2, c3 = hexc("#14123A"), hexc("#4C1D95"), hexc("#BE185D")
    for y in range(sh):
        for x in range(sw):
            t = (y / sh) * 0.85 + (x / sw) * 0.15
            wp[x, y] = lerp(c1, c2, t / 0.5) if t < 0.5 else lerp(c2, c3, (t - 0.5) / 0.5)
    glow = Image.new("RGBA", (sw, sh), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.ellipse((-30, 110, 60, 220), fill=(236, 72, 153, 80))
    gd.ellipse((50, -20, 140, 80), fill=(99, 102, 241, 90))
    gd.ellipse((30, 140, 130, 230), fill=(251, 146, 60, 50))
    glow = glow.filter(ImageFilter.GaussianBlur(12))
    wall = Image.alpha_composite(wall, glow)
    mask = Image.new("L", (sw, sh), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, sw - 1, sh - 1), radius=13, fill=255)
    img.paste(wall, (sx0, sy0), mask)

    ov = Image.new("RGBA", (GUI_W, GUI_H), (0, 0, 0, 0))
    od = ImageDraw.Draw(ov)
    W = (255, 255, 255, 235)
    cx = (bx0 + bx1) // 2
    # dynamic island
    od.rounded_rectangle((cx - 13, gy(-24), cx + 13, gy(-17)), radius=3, fill=(0, 0, 0, 255))
    od.ellipse((cx + 7, gy(-22), cx + 10, gy(-19)), fill=hexc("#1E2A44"))
    # signal + battery (right); the clock (left, x=45) is live text
    for i, h in enumerate((1, 2, 3, 4)):
        od.rectangle((112 + i * 2, gy(-16) - h, 112 + i * 2, gy(-16)), fill=W)
    od.rectangle((121, gy(-21), 128, gy(-16)), outline=W)
    od.rectangle((129, gy(-19), 129, gy(-18)), fill=W)
    od.rectangle((123, gy(-19), 126, gy(-18)), fill=hexc("#34C759"))
    # dock around the hotbar row (cols 2-6)
    od.rounded_rectangle((bx0 + 4, gy(HOTBAR_Y - 4), bx1 - 4, gy(HOTBAR_Y + 19)), radius=7, fill=(255, 255, 255, 46))
    od.rounded_rectangle((bx0 + 4, gy(HOTBAR_Y - 4), bx1 - 4, gy(HOTBAR_Y + 19)), radius=7, outline=(255, 255, 255, 40))
    # home indicator
    od.rounded_rectangle((cx - 18, GUI_H - 9, cx + 18, GUI_H - 8), radius=1, fill=(255, 255, 255, 200))
    img = Image.alpha_composite(img, ov)
    d = ImageDraw.Draw(img)
    d.line((bx0, gy(16), bx0, gy(30)), fill=hexc("#4A4A55"))
    d.line((bx0, gy(36), bx0, gy(50)), fill=hexc("#4A4A55"))
    d.line((bx1, gy(26), bx1, gy(48)), fill=hexc("#4A4A55"))
    # 1 almost invisible pixel at the far right: keeps the glyph advance at exactly 177
    img.putpixel((GUI_W - 1, 0), (0, 0, 0, 1))
    return img


def build_gui():
    app = phone_base()
    app.save(mk(os.path.join(TEX, "font", "phone_app.png")))

    home = phone_base()
    for cx, icon_y, text in HOME_LABELS:
        pix_text(home, cx, gy(icon_y + 17), text)
    home.save(mk(os.path.join(TEX, "font", "phone_home.png")))

    # transparent dispenser texture -> only the phone is visible
    Image.new("RGBA", (256, 256), (0, 0, 0, 0)).save(
        mk(os.path.join(PACK, "assets", "minecraft", "textures", "gui", "container", "dispenser.png")))
    return home


ASCII_ROWS = 16


def ascii_chars():
    """chars grid of minecraft:font/ascii.png (rows 2..7 = 0x20..0x7F), other rows unused."""
    rows = []
    for r in range(ASCII_ROWS):
        if 2 <= r <= 7:
            row = ""
            for c in range(16):
                cp = r * 16 + c
                row += "\u0000" if cp in (0x20, 0x7F) else chr(cp)
            rows.append(row)
        else:
            rows.append("\u0000" * 16)
    return rows


def text_font(height, top_y):
    """vanilla ascii.png re-used at another size/position. top_y = GUI y of the text top."""
    scale = height / 8
    return {"providers": [
        {"type": "space", "advances": {" ": round(4 * scale)}},
        {"type": "bitmap", "file": "minecraft:font/ascii.png", "height": height,
         "ascent": 13 - top_y - (8 - 7) * 0, "chars": ascii_chars()},
    ]}


STATUS_CHARS = "0123456789:"
STATUS_CELL_W, STATUS_CELL_H = 4, 40
STATUS_TOP = -21   # GUI y of the status clock
DIGITS = {
    "0": ["###", "#.#", "#.#", "#.#", "###"], "1": [".#.", "##.", ".#.", ".#.", "###"],
    "2": ["###", "..#", "###", "#..", "###"], "3": ["###", "..#", "###", "..#", "###"],
    "4": ["#.#", "#.#", "###", "..#", "..#"], "5": ["###", "#..", "###", "..#", "###"],
    "6": ["###", "#..", "###", "#.#", "###"], "7": ["###", "..#", "..#", "..#", "..#"],
    "8": ["###", "#.#", "###", "#.#", "###"], "9": ["###", "#.#", "###", "..#", "###"],
    ":": [".", "#", ".", "#", "."],
}


def build_status_font():
    img = Image.new("RGBA", (STATUS_CELL_W * len(STATUS_CHARS), STATUS_CELL_H), (0, 0, 0, 0))
    px = img.load()
    for i, ch in enumerate(STATUS_CHARS):
        for ry, row in enumerate(DIGITS[ch]):
            for rx, v in enumerate(row):
                if v == "#":
                    px[i * STATUS_CELL_W + rx, ry] = (255, 255, 255, 255)
    img.save(mk(os.path.join(TEX, "font", "status.png")))


def write_font():
    build_status_font()
    adv = {}
    for bit in range(8):
        adv[chr(0xF801 + bit)] = -(1 << bit)
        adv[chr(0xF821 + bit)] = (1 << bit)
    fonts = {
        "gui": {"providers": [
            {"type": "space", "advances": adv},
            {"type": "bitmap", "file": "aphone:font/phone_app.png", "height": GUI_H, "ascent": ASCENT, "chars": [""]},
            {"type": "bitmap", "file": "aphone:font/phone_home.png", "height": GUI_H, "ascent": ASCENT, "chars": [""]},
        ]},
        # live status bar clock: own 3x5 pixel digits in a tall cell (ascent must be <= height)
        "status": {"providers": [
            {"type": "space", "advances": {" ": 3}},
            {"type": "bitmap", "file": "aphone:font/status.png", "height": STATUS_CELL_H,
             "ascent": 13 - STATUS_TOP, "chars": [STATUS_CHARS]},
        ]},
    }
    for name, data in fonts.items():
        with open(mk(os.path.join(PACK, "assets", "aphone", "font", name + ".json")), "w") as f:
            json.dump(data, f, indent=2, ensure_ascii=True)


def write_lang():
    # hides the "Inventory" label that vanilla draws over the phone
    for lang in ("en_us", "en_gb", "it_it", "es_es", "fr_fr", "de_de", "pt_br"):
        with open(mk(os.path.join(PACK, "assets", "minecraft", "lang", lang + ".json")), "w") as f:
            json.dump({"container.inventory": ""}, f, indent=2)


def write_meta(phone_tex):
    with open(mk(os.path.join(PACK, "pack.mcmeta")), "w") as f:
        json.dump({"pack": {"pack_format": 34,
                            "description": "§b§lAPhone §7resource pack\n§8by §b@Adryh01"}}, f, indent=2)
    icon = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    bg = gradient(128, 128, hexc("#14123A"), hexc("#BE185D"))
    m = Image.new("L", (128, 128), 0)
    ImageDraw.Draw(m).rounded_rectangle((0, 0, 127, 127), radius=24, fill=255)
    icon.paste(bg, (0, 0), m)
    front = phone_tex.crop((0, 0, 14, 26)).resize((14 * 4, 26 * 4), Image.NEAREST)
    icon.alpha_composite(front, (36, 12))
    icon.save(mk(os.path.join(PACK, "pack.png")))


if __name__ == "__main__":
    icons = build_icons()
    tex = build_phone_texture()
    write_models()
    gui = build_gui()
    write_font()
    write_lang()
    write_meta(tex)
    print("Resource pack generated in", PACK)
