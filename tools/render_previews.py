#!/usr/bin/env python3
"""APhone by @Adryh01 - renders README previews (approximate, Minecraft-like) from the generated resource pack."""
import os
from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX = os.path.join(ROOT, "resourcepack", "assets", "aphone", "textures")
OUT = os.path.join(ROOT, "docs", "previews")
os.makedirs(OUT, exist_ok=True)
K = 4  # GUI scale
FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf"
FONT_R = "/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf"


def font(size, bold=True):
    try:
        return ImageFont.truetype(FONT if bold else FONT_R, size)
    except OSError:
        return ImageFont.load_default()


def icon(name):
    return Image.open(os.path.join(TEX, "item", "icon", name + ".png")).convert("RGBA")


TOP = -28  # glyph starts 28px above the GUI (dispenser window)


def slot_pos(raw):
    if raw < 9:
        r, c = divmod(raw, 3)
        return 62 + c * 18, 17 + r * 18
    if raw < 36:
        r, c = divmod(raw - 9, 9)
        return 8 + c * 18, 84 + r * 18
    return 8 + (raw - 36) * 18, 142


def screen(title, items, hover=None, tooltip=None, home=False, status="12:30"):
    gui = Image.open(os.path.join(TEX, "font", "phone_home.png" if home else "phone_app.png")).convert("RGBA")
    W, H = gui.size
    base = gui.copy()
    for raw, name in items.items():
        x, y = slot_pos(raw)
        base.alpha_composite(icon(name).resize((16, 16), Image.LANCZOS), (x, y - TOP))
    if hover is not None:
        x, y = slot_pos(hover)
        ov = Image.new("RGBA", base.size, (0, 0, 0, 0))
        ImageDraw.Draw(ov).rectangle((x, y - TOP, x + 15, y - TOP + 15), fill=(255, 255, 255, 80))
        base = Image.alpha_composite(base, ov)
    big_img = base.resize((W * K, H * K), Image.NEAREST)

    cw, ch = W * K + 300, H * K + 80
    canvas = Image.new("RGBA", (cw, ch))
    cd = ImageDraw.Draw(canvas)
    for y in range(ch):
        t = y / ch
        cd.line((0, y, cw, y), fill=(int(28 + 20 * t), int(40 + 14 * t), int(58 - 14 * t), 255))
    ox, oy = (cw - W * K) // 2, 40
    canvas.alpha_composite(big_img, (ox, oy))
    d = ImageDraw.Draw(canvas)
    f = font(30)
    sf = Image.open(os.path.join(TEX, "font", "status.png")).convert("RGBA")
    sx = 45
    for sc in status:
        i = "0123456789:".index(sc)
        g = sf.crop((i * 4, 0, i * 4 + 4, 5)).resize((4 * K, 5 * K), Image.NEAREST)
        canvas.alpha_composite(g, (ox + sx * K, oy + (-21 - TOP) * K))
        sx += 2 if sc == ":" else 4

    if tooltip and hover is not None:
        x, y = slot_pos(hover)
        tx = ox + (x + 14) * K
        ty = oy + (y - TOP - 8) * K
        f = font(26)
        w = max(d.textlength(t, font=f) for t, _ in tooltip) + 28
        h = len(tooltip) * 34 + 18
        if tx + w > cw:
            tx = ox + x * K - w - 8
        if ty + h > ch:
            ty = ch - h - 10
        d.rectangle((tx, ty, tx + w, ty + h), fill=(16, 0, 16, 240))
        d.rectangle((tx + 3, ty + 3, tx + w - 3, ty + h - 3), outline=(80, 0, 255, 200), width=3)
        for i, (text, col) in enumerate(tooltip):
            d.text((tx + 14, ty + 10 + i * 34), text, font=f, fill=col)
    return canvas.convert("RGB")


AQUA, WHITE, GRAY, GREEN, YELLOW, GOLD, RED = (85, 255, 255), (255, 255, 255), (170, 170, 170), (85, 255, 85), (255, 255, 85), (255, 170, 0), (255, 85, 85)

phone_tex = Image.open(os.path.join(TEX, "item", "phone.png")).convert("RGBA")

home_items = {0: "app_gps", 2: "app_weather", 6: "app_notes", 8: "app_calculator",
              11: "app_flashlight", 13: "app_settings", 15: "app_emergency", 38: "app_phone", 40: "app_messages", 42: "app_contacts"}
screen("Home", home_items, home=True).save(os.path.join(OUT, "home.png"))
screen("Home", home_items, home=True, hover=40, tooltip=[("Messages (2)", AQUA), ("Read and send SMS", GRAY)]
       ).save(os.path.join(OUT, "home_hover.png"))

contacts = {i: "contact" for i in range(0, 9)}
contacts.update({11: "contact", 12: "contact", 13: "contact", 38: "back", 40: "home", 42: "add"})
screen("Contacts", contacts, hover=1, tooltip=[
    ("Steve_Gamer", WHITE), ("555-4821", GRAY), ("\u25CF Online", GREEN), ("", WHITE),
    ("Left-click to call", YELLOW), ("Right-click to send SMS", YELLOW), ("Shift+Right to delete", YELLOW)]
    ).save(os.path.join(OUT, "contacts.png"))

msgs = {0: "sms", 1: "sms", 2: "sms", 3: "sms", 38: "back", 40: "home", 42: "add"}
screen("Messages", msgs, hover=0, tooltip=[
    ("Alex", AQUA), ("555-1093 \u2022 2m ago", GRAY), ("", WHITE), ("Meet me at spawn in 5", WHITE),
    ("minutes, bring diamonds!", WHITE), ("", WHITE), ("Left-click to reply", YELLOW), ("Right-click to delete", YELLOW)]
    ).save(os.path.join(OUT, "messages.png"))

settings = {12: "toggle_off", 14: "app_settings", 31: "info", 38: "back", 40: "home"}
screen("Settings", settings, hover=14, tooltip=[
    ("Ringtone", WHITE), ("Current: Classic", AQUA), ("", WHITE), ("Left-click next ringtone", YELLOW),
    ("Right-click preview", YELLOW)]).save(os.path.join(OUT, "settings.png"))

# ---- icon sheet + phone item
names = ["app_phone", "app_messages", "app_contacts", "app_gps", "app_flashlight", "app_weather", "app_notes",
         "app_calculator", "app_emergency", "app_settings", "home", "back", "add", "contact", "sms", "call_accept",
         "call_end", "info", "prev", "next", "toggle_on", "toggle_off", "location", "note"]
cols = 8
cell = 150
sheet = Image.new("RGBA", (cols * cell + 420, 3 * cell + 120), (24, 26, 34, 255))
d = ImageDraw.Draw(sheet)
d.text((30, 25), "APhone resource pack - by @Adryh01", font=font(34), fill=(255, 255, 255))
for i, n in enumerate(names):
    r, c = divmod(i, cols)
    im = icon(n).resize((96, 96), Image.NEAREST)
    x, y = 30 + c * cell, 90 + r * cell
    sheet.alpha_composite(im, (x + 12, y))
    d.text((x + 60 - d.textlength(n, font=font(15, False)) / 2, y + 102), n, font=font(15, False), fill=(190, 190, 200))
front = phone_tex.crop((0, 0, 14, 26)).resize((14 * 12, 26 * 12), Image.NEAREST)
back = phone_tex.crop((16, 0, 30, 26)).resize((14 * 12, 26 * 12), Image.NEAREST)
px = cols * cell + 30
sheet.alpha_composite(front, (px, 90))
sheet.alpha_composite(back, (px + 190, 90))
d.text((px + 40, 420), "front", font=font(18, False), fill=(190, 190, 200))
d.text((px + 240, 420), "back", font=font(18, False), fill=(190, 190, 200))
sheet.convert("RGB").save(os.path.join(OUT, "textures.png"))

# ---- chat preview (incoming call)
chat = Image.new("RGB", (1300, 330), (30, 36, 30))
d = ImageDraw.Draw(chat)
for y in range(330):
    d.line((0, y, 1300, y), fill=(60 + y // 8, 90 + y // 10, 60))
d.rectangle((0, 20, 1000, 300), fill=(0, 0, 0))
f = font(26)


def seg(y, parts):
    x = 16
    for t, col in parts:
        d.text((x, y), t, font=f, fill=col)
        x += d.textlength(t, font=f)


DG = (85, 85, 85)
seg(36, [("[", DG), ("APhone", AQUA), ("] ", DG), ("INCOMING CALL ", AQUA), ("from ", GRAY), ("Steve_Gamer ", WHITE), ("(555-4821)", DG)])
seg(76, [("   [ACCEPT]", GREEN), ("   ", WHITE), ("[DECLINE]", RED)])
seg(116, [("[", DG), ("APhone", AQUA), ("] ", DG), ("Call connected with ", GREEN), ("Steve_Gamer", WHITE), (".", GREEN)])
seg(156, [("[CALL] ", (0, 170, 170)), ("Steve_Gamer", WHITE), (": ", GRAY), ("yo where are you?", WHITE)])
seg(196, [("[CALL] ", (0, 170, 170)), ("You", WHITE), (": ", GRAY), ("near the village, sending GPS", WHITE)])
seg(236, [("[", DG), ("APhone", AQUA), ("] ", DG), ("Location shared with ", GREEN), ("Steve_Gamer", WHITE), (".", GREEN)])
d.text((470, 300), "☎ In call 0:42", font=font(26), fill=GREEN)
chat.save(os.path.join(OUT, "call.png"))
print("Previews saved in", OUT)
