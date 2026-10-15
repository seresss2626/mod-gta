"""Генерирует текстуры и модели мода. Запуск: python3 tools/gen_assets.py"""
import json, os, random
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'gtacraft')
def p(*a):
    path = os.path.join(ROOT, *a)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    return path

# ---- спрайты оружия 16x16 (. прозрачно) ----
K = {'.': None, 'k': (28, 28, 32), 'g': (74, 76, 84), 'G': (120, 124, 134), 'w': (120, 78, 44), 'W': (160, 108, 62),
     'o': (210, 150, 40), 'r': (190, 40, 40), 'e': (60, 80, 56), 'E': (90, 118, 80), 'b': (30, 30, 30), 's': (200, 205, 215)}
SPR = {
 'pistol': [
  "................",
  "................",
  "................",
  "..kkkkkkkkkkk...",
  "..kGGGGGGGGGGk..",
  "..kggggggggggk..",
  "..kkkkkkkkkkkk..",
  "....kgggk.......",
  "....kggk........",
  "....kgbk........",
  "....kgk.........",
  "....kkk.........",
  "................",
  "................",
  "................",
  "................"],
 'smg': [
  "................",
  "................",
  "................",
  "...kkkkkkkkkkkk.",
  "..kGGGGGGGGGGGGk",
  "..kggggggggggggk",
  "..kkkkkkkkkkkkk.",
  "..kggk.kgk.kk...",
  "..kggk.kgk......",
  "..kgbk..kk......",
  "...kk...........",
  "................",
  "................",
  "................",
  "................",
  "................"],
 'shotgun': [
  "................",
  "................",
  "................",
  "................",
  "kkkkkkkkkkkkkkk.",
  "kGGGGGGGGGGGGGGk",
  "kggggggggggggggk",
  "kkkkkkwwwwwwwkk.",
  ".....kwWWWWWwwk.",
  ".....kwwwwwwwwk.",
  "......kwwwwkkk..",
  "......kwwk......",
  ".......kk.......",
  "................",
  "................",
  "................"],
 'sniper': [
  "................",
  "................",
  "................",
  "......kkkk......",
  "......kssk......",
  "kkkkkkkkkkkkkkkk",
  "kGGGGGGGGGGGGGGk",
  "kggggggggggggggk",
  "kkkkkkkkkkwwwwk.",
  "......kgk.kwWwwk",
  "......kgk..kwwwk",
  "......kk....kkk.",
  "................",
  "................",
  "................",
  "................"],
 'rpg': [
  "................",
  "................",
  ".....kkkkkkkk...",
  "...kkeEEEEEEEkk.",
  "..keEEEEEEEEEEok",
  "kkeEEEEEEEEEEEok",
  "..keeeeeeeeeeok.",
  "...kkeeeeeekkk..",
  "....kgk..kgk....",
  "....kgk..kgk....",
  ".....kk...kk....",
  "................",
  "................",
  "................",
  "................",
  "................"],
 'phone': [
  "................",
  "....kkkkkkkk....",
  "....kGGGGGGk....",
  "....kgbbbbgk....",
  "....kgbssbgk....",
  "....kgbbbbgk....",
  "....kgbssbgk....",
  "....kgbbbbgk....",
  "....kgbssbgk....",
  "....kgbbbbgk....",
  "....kggggggk....",
  "....kgkkkkgk....",
  "....kggggggk....",
  "....kkkkkkkk....",
  "................",
  "................"],
 'car_key': [
  "................",
  "................",
  "......kkkk......",
  ".....koooook....",
  ".....ko..ook....",
  ".....ko..ook....",
  ".....koooook....",
  "......kook......",
  ".......ok.......",
  ".......ok.......",
  ".......ook......",
  ".......ok.......",
  ".......ook......",
  "........k.......",
  "................",
  "................"],
}
for name, rows in SPR.items():
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row[:16]):
            c = K[ch]
            if c:
                img.putpixel((x, y), c + (255,))
    img.save(p('textures', 'item', name + '.png'))

# ---- текстуры багги ----
def solid(name, base, noise=14, alpha=255, seed=1):
    rnd = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            d = rnd.randint(-noise, noise)
            c = tuple(max(0, min(255, v + d)) for v in base)
            img.putpixel((x, y), c + (alpha,))
    img.save(p('textures', 'item', name + '.png'))
solid('buggy_paint', (214, 52, 40), 10, seed=2)
solid('buggy_dark', (34, 34, 38), 8, seed=3)
solid('buggy_metal', (150, 154, 162), 10, seed=4)
solid('buggy_light', (255, 240, 170), 6, seed=5)
solid('buggy_tail', (230, 20, 20), 6, seed=6)
solid('buggy_seat', (60, 40, 30), 8, seed=7)
solid('buggy_wheel', (18, 18, 20), 6, seed=8)
solid('sport_paint', (244, 196, 24), 8, seed=11)
solid('sport_stripe', (20, 20, 24), 4, seed=12)
solid('pickup_paint', (40, 92, 190), 8, seed=13)
solid('taxi_paint', (250, 214, 40), 8, seed=14)
solid('police_paint', (236, 238, 244), 6, seed=15)
solid('police_siren_r', (230, 30, 30), 5, seed=16)
solid('police_siren_b', (40, 90, 240), 5, seed=17)

# ---- модели машин (единицы модели; 1 ед = 3/16 блока при масштабе 3; нос к +Z) ----
def faces(tex):
    return {d: {"texture": "#" + tex} for d in ("north", "south", "east", "west", "up", "down")}

def write_model(name, boxes, extra_tex):
    els = [{"from": [a, b, c], "to": [d, e, f], "faces": faces(t)} for (a, b, c, d, e, f, t) in boxes]
    tex = {k: "gtacraft:item/buggy_" + k for k in ("dark", "metal", "light", "tail", "seat", "wheel")}
    tex.update({k: "gtacraft:item/" + v for k, v in extra_tex.items()})
    tex["particle"] = tex["paint"]
    json.dump({"textures": tex, "elements": els}, open(p('models', 'item', name + '_body.json'), 'w'), indent=1)

def wheels(big=5.0, front=(14.0, 19.0), rear=(-2.5, 2.5), x=((1.0, 3.2), (12.8, 15.0)), tex='wheel'):
    out = []
    for (wx1, wx2) in x:
        for (wz1, wz2) in (front, rear):
            out.append((wx1, 0.0, wz1, wx2, big, wz2, tex))
    return out

buggy = [
    (3.5, 1.0, -3, 12.5, 1.8, 19, 'paint'), (3.0, 1.8, -3, 4.5, 3.4, 19, 'paint'), (11.5, 1.8, -3, 13.0, 3.4, 19, 'paint'),
    (3.5, 1.8, 12, 12.5, 4.2, 19, 'paint'), (3.5, 1.8, -3, 12.5, 4.0, 0.5, 'paint'),
    (5.0, 2.5, 3.5, 11.0, 4.2, 5.2, 'seat'), (5.0, 2.5, 6.0, 11.0, 5.2, 7.0, 'seat'),
    (3.6, 3.4, 9.0, 4.4, 9.0, 9.8, 'metal'), (11.6, 3.4, 9.0, 12.4, 9.0, 9.8, 'metal'), (3.6, 8.2, 9.0, 12.4, 9.0, 9.8, 'metal'),
    (3.6, 3.4, 1.0, 4.4, 9.0, 1.8, 'metal'), (11.6, 3.4, 1.0, 12.4, 9.0, 1.8, 'metal'), (3.6, 8.2, 1.0, 12.4, 9.0, 1.8, 'metal'),
    (3.6, 8.2, 1.8, 4.4, 9.0, 9.0, 'metal'), (11.6, 8.2, 1.8, 12.4, 9.0, 9.0, 'metal'),
    (4.2, 2.6, 19.0, 6.2, 3.8, 19.6, 'light'), (9.8, 2.6, 19.0, 11.8, 3.8, 19.6, 'light'),
    (4.0, 2.6, -3.6, 5.6, 3.6, -3.0, 'tail'), (10.4, 2.6, -3.6, 12.0, 3.6, -3.0, 'tail')] + wheels()
write_model('buggy', buggy, {"paint": "buggy_paint"})

sport = [
    (3.5, 1.0, -4, 12.5, 2.6, 20, 'paint'), (4.0, 2.6, 12, 12.0, 3.8, 20, 'paint'), (3.2, 1.6, 0, 4.4, 3.6, 12, 'paint'), (11.6, 1.6, 0, 12.8, 3.6, 12, 'paint'),
    (3.5, 2.6, -4, 12.5, 3.6, 1, 'paint'),
    (5.0, 2.4, 4.0, 7.8, 3.6, 5.4, 'seat'), (8.2, 2.4, 4.0, 11.0, 3.6, 5.4, 'seat'), (5.0, 2.4, 5.6, 11.0, 5.4, 6.4, 'seat'),
    (4.0, 3.6, 10.2, 5.0, 7.0, 11.0, 'metal'), (11.0, 3.6, 10.2, 12.0, 7.0, 11.0, 'metal'), (4.0, 6.2, 10.2, 12.0, 7.0, 11.0, 'metal'),
    (4.0, 4.6, -4.4, 12.0, 5.0, -1.8, 'dark'), (5.0, 3.6, -3.6, 5.6, 4.6, -3.0, 'dark'), (10.4, 3.6, -3.6, 11.0, 4.6, -3.0, 'dark'),
    (4.2, 2.4, 20.0, 6.4, 3.4, 20.6, 'light'), (9.6, 2.4, 20.0, 11.8, 3.4, 20.6, 'light'),
    (4.0, 2.4, -4.6, 6.0, 3.4, -4.0, 'tail'), (10.0, 2.4, -4.6, 12.0, 3.4, -4.0, 'tail'),
    (6.9, 1.6, 12, 9.1, 3.9, 20, 'stripe')] + wheels(4.6, (14.5, 19.1), (-3.2, 1.4))
write_model('sport', sport, {"paint": "sport_paint", "stripe": "sport_stripe"})

pickup = [
    (3.5, 1.0, -4, 12.5, 2.0, 20, 'paint'), (3.0, 2.0, -4, 4.5, 4.6, 20, 'paint'), (11.5, 2.0, -4, 13.0, 4.6, 20, 'paint'),
    (3.5, 2.0, 12, 12.5, 5.2, 20, 'paint'),
    (3.0, 4.6, -4, 4.5, 6.8, 3, 'paint'), (11.5, 4.6, -4, 13.0, 6.8, 3, 'paint'), (3.0, 2.0, -4.6, 13.0, 6.8, -3.8, 'paint'),
    (5.0, 2.0, 4.0, 11.0, 3.6, 5.6, 'seat'), (5.0, 2.0, 5.8, 11.0, 6.6, 6.8, 'seat'),
    (3.6, 4.6, 9.0, 4.4, 11.4, 9.8, 'metal'), (11.6, 4.6, 9.0, 12.4, 11.4, 9.8, 'metal'), (3.6, 10.6, 9.0, 12.4, 11.4, 9.8, 'metal'),
    (3.6, 4.6, 3.0, 4.4, 11.4, 3.8, 'metal'), (11.6, 4.6, 3.0, 12.4, 11.4, 3.8, 'metal'), (3.6, 10.6, 3.0, 12.4, 11.4, 3.8, 'metal'),
    (3.6, 10.6, 3.8, 4.4, 11.4, 9.0, 'metal'), (11.6, 10.6, 3.8, 12.4, 11.4, 9.0, 'metal'),
    (3.8, 2.6, 20.0, 6.0, 3.8, 20.6, 'light'), (10.0, 2.6, 20.0, 12.2, 3.8, 20.6, 'light'),
    (4.0, 3.0, -4.9, 5.6, 4.0, -4.6, 'tail'), (10.4, 3.0, -4.9, 12.0, 4.0, -4.6, 'tail'),
    (3.8, 3.6, 20.0, 12.2, 4.4, 20.6, 'dark')] + wheels(5.8, (13.5, 19.3), (-3.3, 2.5), ((0.6, 3.2), (12.8, 15.4)))
write_model('pickup', pickup, {"paint": "pickup_paint"})

def sedan(name, paint, extra, roof_extra):
    boxes = [
        (3.5, 1.0, -4, 12.5, 3.2, 20, 'paint'), (3.5, 3.2, 11, 12.5, 4.2, 20, 'paint'), (3.5, 3.2, -4, 12.5, 4.4, 0.5, 'paint'),
        (3.2, 1.6, 0.5, 4.4, 3.8, 11, 'paint'), (11.6, 1.6, 0.5, 12.8, 3.8, 11, 'paint'),
        (5.0, 2.0, 3.5, 11.0, 3.4, 5.0, 'seat'), (5.0, 2.0, 5.2, 11.0, 6.0, 6.0, 'seat'),
        (3.6, 3.8, 10.0, 4.4, 11.0, 10.8, 'metal'), (11.6, 3.8, 10.0, 12.4, 11.0, 10.8, 'metal'),
        (3.6, 3.8, 0.2, 4.4, 11.0, 1.0, 'metal'), (11.6, 3.8, 0.2, 12.4, 11.0, 1.0, 'metal'),
        (3.4, 11.0, -0.4, 12.6, 11.8, 11.4, 'paint'),
        (4.2, 2.4, 20.0, 6.4, 3.4, 20.6, 'light'), (9.6, 2.4, 20.0, 11.8, 3.4, 20.6, 'light'),
        (4.0, 2.6, -4.6, 6.0, 3.6, -4.0, 'tail'), (10.0, 2.6, -4.6, 12.0, 3.6, -4.0, 'tail')] + wheels(4.8, (14.0, 18.8), (-3.0, 1.8))
    boxes += extra
    boxes += roof_extra
    write_model(name, boxes, paint)

sedan('taxi', {"paint": "taxi_paint"}, [(6.0, 3.2, -4.01, 10.0, 3.4, 20.01, 'dark')], [(6.2, 11.8, 4.0, 9.8, 13.2, 7.0, 'light')])
sedan('police', {"paint": "police_paint", "siren_r": "police_siren_r", "siren_b": "police_siren_b"}, [(3.5, 3.2, 11, 12.5, 4.2, 20, 'dark'), (3.15, 2.0, 1, 3.55, 3.4, 10, 'dark'), (12.45, 2.0, 1, 12.85, 3.4, 10, 'dark')],
      [(4.6, 11.8, 4.0, 7.8, 13.0, 7.0, 'siren_r'), (8.2, 11.8, 4.0, 11.4, 13.0, 7.0, 'siren_b')])

# ---- модели и определения предметов ----
HAND = {
 "thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 4, 0.5], "scale": [0.85, 0.85, 0.85]},
 "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
}
for name in ("pistol", "smg", "shotgun", "sniper", "rpg"):
    json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "gtacraft:item/" + name}, "display": HAND},
              open(p('models', 'item', name + '.json'), 'w'), indent=1)
for nm in ("car_key", "phone"):
    json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "gtacraft:item/" + nm}},
              open(p('models', 'item', nm + '.json'), 'w'), indent=1)
for name in ("pistol", "smg", "shotgun", "sniper", "rpg", "car_key", "phone", "buggy_body", "sport_body", "pickup_body", "taxi_body", "police_body"):
    json.dump({"model": {"type": "minecraft:model", "model": "gtacraft:item/" + name}},
              open(p('items', name + '.json'), 'w'), indent=1)

# ---- названия ----
ru = {"item.gtacraft.pistol": "Пистолет", "item.gtacraft.smg": "Пистолет-пулемёт", "item.gtacraft.shotgun": "Дробовик",
      "item.gtacraft.sniper": "Снайперская винтовка", "item.gtacraft.rpg": "Ракетомёт",
      "item.gtacraft.car_key": "Ключ от машины", "item.gtacraft.phone": "Телефон", "item.gtacraft.buggy_body": "Кузов багги",
      "item.gtacraft.sport_body": "Кузов спорткара", "item.gtacraft.pickup_body": "Кузов пикапа", "item.gtacraft.taxi_body": "Кузов такси", "item.gtacraft.police_body": "Кузов полицейской"}
en = {"item.gtacraft.pistol": "Pistol", "item.gtacraft.smg": "SMG", "item.gtacraft.shotgun": "Shotgun",
      "item.gtacraft.sniper": "Sniper Rifle", "item.gtacraft.rpg": "Rocket Launcher",
      "item.gtacraft.car_key": "Car Key", "item.gtacraft.phone": "Phone", "item.gtacraft.buggy_body": "Buggy Body",
      "item.gtacraft.sport_body": "Sports Car Body", "item.gtacraft.pickup_body": "Pickup Body", "item.gtacraft.taxi_body": "Taxi Body", "item.gtacraft.police_body": "Police Car Body"}
json.dump(ru, open(p('lang', 'ru_ru.json'), 'w'), ensure_ascii=False, indent=1)
json.dump(en, open(p('lang', 'en_us.json'), 'w'), indent=1)
print('assets ok')

# ---- GUI: цифры денег и звёзды розыска ----
from PIL import ImageDraw, ImageFont, ImageFilter
FONT = '/usr/share/fonts/truetype/google-fonts/Poppins-BoldItalic.ttf'
CW, CH = 32, 48
atlas = Image.new('RGBA', (CW * 11, CH), (0, 0, 0, 0))
font = ImageFont.truetype(FONT, 38)
for i, ch in enumerate("0123456789$"):
    cell = Image.new('RGBA', (CW * 3, CH * 3), (0, 0, 0, 0))
    d = ImageDraw.Draw(cell)
    bb = d.textbbox((0, 0), ch, font=font)
    tx = CW * 3 // 2 - (bb[0] + bb[2]) // 2
    ty = CH * 3 // 2 - (bb[1] + bb[3]) // 2
    # тень и чёрная обводка
    d.text((tx + 3, ty + 3), ch, font=font, fill=(0, 0, 0, 200), stroke_width=4, stroke_fill=(0, 0, 0, 200))
    d.text((tx, ty), ch, font=font, fill=(112, 190, 82, 255), stroke_width=4, stroke_fill=(0, 0, 0, 255))
    cell = cell.crop((CW, CH, CW * 2, CH * 2))
    atlas.paste(cell, (i * CW, 0))
os.makedirs(os.path.join(ROOT, 'textures', 'gui'), exist_ok=True)
atlas.save(p('textures', 'gui', 'money.png'))

def star(fill, outline):
    img = Image.new('RGBA', (128, 128), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    import math
    pts = []
    for k in range(10):
        r = 58 if k % 2 == 0 else 24
        a = -math.pi / 2 + k * math.pi / 5
        pts.append((64 + r * math.cos(a), 68 + r * math.sin(a)))
    d.polygon(pts, fill=fill, outline=outline)
    d.line(pts + [pts[0]], fill=outline, width=6, joint='curve')
    return img.resize((32, 32), Image.LANCZOS)
star((255, 255, 255, 255), (10, 10, 10, 255)).save(p('textures', 'gui', 'star_on.png'))
star((70, 70, 74, 150), (10, 10, 10, 200)).save(p('textures', 'gui', 'star_off.png'))
print('gui ok')
