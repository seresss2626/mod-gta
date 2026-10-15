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

# ---- модель багги (единицы модели, 1 ед = 3/16 блока при масштабе 3) ----
def faces(tex):
    return {d: {"texture": "#" + tex} for d in ("north", "south", "east", "west", "up", "down")}
els = []
def box(x1, y1, z1, x2, y2, z2, tex):
    els.append({"from": [x1, y1, z1], "to": [x2, y2, z2], "faces": faces(tex)})
# носом к +Z. Пол, борта, капот, багажник, дуга безопасности, колёса, фары.
box(3.5, 1.0, -3, 12.5, 1.8, 19, 'paint')          # днище
box(3.0, 1.8, -3, 4.5, 3.4, 19, 'paint')           # левый борт
box(11.5, 1.8, -3, 13.0, 3.4, 19, 'paint')         # правый борт
box(3.5, 1.8, 12, 12.5, 4.2, 19, 'paint')          # капот
box(3.5, 1.8, -3, 12.5, 4.0, 0.5, 'paint')         # задний блок двигателя
box(5.0, 2.5, 3.5, 11.0, 4.2, 5.2, 'seat')         # сиденье
box(5.0, 2.5, 6.0, 11.0, 5.2, 7.0, 'seat')         # спинка
box(3.6, 3.4, 9.0, 4.4, 9.0, 9.8, 'metal')         # стойка лобового л
box(11.6, 3.4, 9.0, 12.4, 9.0, 9.8, 'metal')       # стойка лобового п
box(3.6, 8.2, 9.0, 12.4, 9.0, 9.8, 'metal')        # верх рамы
box(3.6, 3.4, 1.0, 4.4, 9.0, 1.8, 'metal')         # дуга сзади л
box(11.6, 3.4, 1.0, 12.4, 9.0, 1.8, 'metal')       # дуга сзади п
box(3.6, 8.2, 1.0, 12.4, 9.0, 1.8, 'metal')        # верх дуги
box(3.6, 8.2, 1.8, 4.4, 9.0, 9.0, 'metal')         # крыша л
box(11.6, 8.2, 1.8, 12.4, 9.0, 9.0, 'metal')       # крыша п
box(4.2, 2.6, 19.0, 6.2, 3.8, 19.6, 'light')       # фара л
box(9.8, 2.6, 19.0, 11.8, 3.8, 19.6, 'light')      # фара п
box(4.0, 2.6, -3.6, 5.6, 3.6, -3.0, 'tail')        # стоп л
box(10.4, 2.6, -3.6, 12.0, 3.6, -3.0, 'tail')      # стоп п
for (wx1, wx2) in ((1.0, 3.2), (12.8, 15.0)):
    for (wz1, wz2) in ((14.0, 19.0), (-2.5, 2.5)):
        box(wx1, 0.0, wz1, wx2, 5.0, wz2, 'wheel')
model = {"textures": {k: "gtacraft:item/buggy_" + k for k in ("paint", "dark", "metal", "light", "tail", "seat", "wheel")},
         "elements": els}
model["textures"]["particle"] = "gtacraft:item/buggy_paint"
json.dump(model, open(p('models', 'item', 'buggy_body.json'), 'w'), indent=1)

# ---- модели и определения предметов ----
HAND = {
 "thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 4, 0.5], "scale": [0.85, 0.85, 0.85]},
 "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
}
for name in ("pistol", "smg", "shotgun", "sniper", "rpg"):
    json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "gtacraft:item/" + name}, "display": HAND},
              open(p('models', 'item', name + '.json'), 'w'), indent=1)
json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "gtacraft:item/car_key"}},
          open(p('models', 'item', 'car_key.json'), 'w'), indent=1)
for name in ("pistol", "smg", "shotgun", "sniper", "rpg", "car_key", "buggy_body"):
    json.dump({"model": {"type": "minecraft:model", "model": "gtacraft:item/" + name}},
              open(p('items', name + '.json'), 'w'), indent=1)

# ---- названия ----
ru = {"item.gtacraft.pistol": "Пистолет", "item.gtacraft.smg": "Пистолет-пулемёт", "item.gtacraft.shotgun": "Дробовик",
      "item.gtacraft.sniper": "Снайперская винтовка", "item.gtacraft.rpg": "Ракетомёт",
      "item.gtacraft.car_key": "Ключ от машины", "item.gtacraft.buggy_body": "Кузов багги"}
en = {"item.gtacraft.pistol": "Pistol", "item.gtacraft.smg": "SMG", "item.gtacraft.shotgun": "Shotgun",
      "item.gtacraft.sniper": "Sniper Rifle", "item.gtacraft.rpg": "Rocket Launcher",
      "item.gtacraft.car_key": "Car Key", "item.gtacraft.buggy_body": "Buggy Body"}
json.dump(ru, open(p('lang', 'ru_ru.json'), 'w'), ensure_ascii=False, indent=1)
json.dump(en, open(p('lang', 'en_us.json'), 'w'), indent=1)
print('assets ok')
