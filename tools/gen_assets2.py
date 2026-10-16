"""Вторая часть генератора: скины прохожих, гранаты, гильзы, анимации оружия.
Запускать после gen_assets.py:  python3 tools/gen_assets.py && python3 tools/gen_assets2.py"""
import json, os, random
from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'gtacraft')
def p(*a):
    path = os.path.join(ROOT, *a)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    return path

# ------------------------------------------------------------------ скины прохожих (формат скина игрока 64x64)
SKIN_TONES = [(255, 219, 172), (241, 194, 125), (224, 172, 105), (198, 134, 66), (141, 85, 36), (94, 56, 30), (255, 205, 148)]
HAIRS = [(20, 16, 14), (60, 38, 22), (110, 70, 30), (180, 130, 60), (225, 190, 110), (150, 40, 28), (120, 120, 126), (30, 30, 60)]
SHIRTS = [(200, 50, 50), (50, 90, 200), (60, 160, 90), (230, 200, 60), (240, 240, 240), (40, 40, 46), (150, 60, 160),
          (240, 140, 40), (60, 170, 190), (130, 130, 138), (210, 90, 140), (90, 60, 40)]
PANTS = [(40, 50, 90), (30, 30, 36), (90, 78, 60), (70, 80, 100), (120, 120, 126), (60, 40, 30), (30, 60, 50)]

BOXES = {  # имя: (u, v, w, h, d)
    'head': (0, 0, 8, 8, 8), 'body': (16, 16, 8, 12, 4), 'rarm': (40, 16, 4, 12, 4),
    'rleg': (0, 16, 4, 12, 4), 'lleg': (16, 48, 4, 12, 4), 'larm': (32, 48, 4, 12, 4),
}

def face_rects(box):
    u, v, w, h, d = BOXES[box]
    return {
        'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d),
        'right': (u, v + d, d, h), 'front': (u + d, v + d, w, h),
        'left': (u + d + w, v + d, d, h), 'back': (u + 2 * d + w, v + d, w, h),
    }

def fill(img, rect, color, rnd, noise=7):
    x, y, w, h = rect
    for j in range(h):
        for i in range(w):
            dv = rnd.randint(-noise, noise)
            img.putpixel((x + i, y + j), tuple(max(0, min(255, c + dv)) for c in color) + (255,))

def make_skin(seed):
    rnd = random.Random(seed * 7919 + 13)
    img = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    skin = rnd.choice(SKIN_TONES)
    hair = rnd.choice(HAIRS)
    shirt = rnd.choice(SHIRTS)
    pants = rnd.choice(PANTS)
    shoes = rnd.choice([(24, 24, 28), (240, 240, 240), (80, 50, 30)])
    long_sleeve = rnd.random() < 0.55
    skirt = rnd.random() < 0.22
    jacket = rnd.random() < 0.35
    hair_style = rnd.choice(['short', 'short', 'long', 'bald', 'cap'])
    beard = rnd.random() < 0.2 and hair_style != 'long'
    cap = rnd.choice(SHIRTS)

    # голова
    for f, r in face_rects('head').items():
        fill(img, r, skin, rnd, 5)
    hf = face_rects('head')
    if hair_style != 'bald':
        c = cap if hair_style == 'cap' else hair
        fill(img, hf['top'], c, rnd, 8)
        for side in ('right', 'left', 'back'):
            x, y, w, h = hf[side]
            rows = 8 if (hair_style == 'long' and side == 'back') else (3 if hair_style != 'cap' else 2)
            if hair_style == 'long' and side != 'back':
                rows = 6
            fill(img, (x, y, w, rows), c, rnd, 8)
        x, y, w, h = hf['front']
        bang = 2 if hair_style != 'cap' else 2
        fill(img, (x, y, w, bang), c, rnd, 8)
        if hair_style == 'long':
            fill(img, (x, y, 1, 6), c, rnd, 8)
            fill(img, (x + w - 1, y, 1, 6), c, rnd, 8)
    # лицо
    fx, fy, fw, fh = hf['front']
    eye_y = fy + 4
    for ex in (fx + 1, fx + 5):
        img.putpixel((ex, eye_y), (255, 255, 255, 255))
        img.putpixel((ex + 1, eye_y), (rnd.randint(30, 90), rnd.randint(60, 140), rnd.randint(90, 200), 255))
    mouth = tuple(max(0, c - 55) for c in skin)
    for mx in (fx + 3, fx + 4):
        img.putpixel((mx, fy + 6), mouth + (255,))
    if beard:
        fill(img, (fx + 1, fy + 6, 6, 2), hair, rnd, 6)
        img.putpixel((fx + 3, fy + 6), mouth + (255,))
        img.putpixel((fx + 4, fy + 6), mouth + (255,))

    # туловище
    bf = face_rects('body')
    for f, r in bf.items():
        fill(img, r, shirt, rnd, 6)
    if jacket:
        x, y, w, h = bf['front']
        fill(img, (x + 3, y, 2, h), (230, 230, 230) if shirt != (240, 240, 240) else (60, 60, 66), rnd, 4)
    elif rnd.random() < 0.4:
        x, y, w, h = bf['front']
        for j in range(3, h, 4):
            fill(img, (x, y + j, w, 1), tuple(max(0, c - 40) for c in shirt), rnd, 3)
    # руки
    for arm in ('rarm', 'larm'):
        for f, r in face_rects(arm).items():
            x, y, w, h = r
            fill(img, r, skin, rnd, 5)
            if f in ('right', 'front', 'left', 'back'):
                sleeve = h if long_sleeve else 4
                fill(img, (x, y, w, sleeve), shirt, rnd, 6)
            if f in ('top',):
                fill(img, r, shirt, rnd, 6)
    # ноги
    for leg in ('rleg', 'lleg'):
        for f, r in face_rects(leg).items():
            x, y, w, h = r
            fill(img, r, pants, rnd, 6)
            if f in ('right', 'front', 'left', 'back'):
                if skirt:
                    fill(img, (x, y + 4, w, h - 4), skin, rnd, 5)
                    fill(img, (x, y, w, 4), pants, rnd, 5)
                fill(img, (x, y + h - 2, w, 2), shoes, rnd, 4)
    return img

for i in range(24):
    make_skin(i).save(p('textures', 'entity', 'ped', 'p%d.png' % i))

# ------------------------------------------------------------------ спрайты гранат (16x16)
def sprite(name, draw_fn):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    draw_fn(ImageDraw.Draw(img))
    img.save(p('textures', 'item', name + '.png'))

def grenade(d):
    d.ellipse((4, 5, 11, 13), fill=(70, 96, 52, 255), outline=(30, 44, 24, 255))
    d.rectangle((6, 3, 9, 5), fill=(120, 124, 134, 255), outline=(40, 40, 44, 255))
    d.line((9, 3, 12, 1), fill=(150, 154, 162, 255))
    d.point((12, 3), fill=(210, 150, 40, 255))
    for (x, y) in ((6, 8), (9, 8), (6, 11), (9, 11)):
        d.point((x, y), fill=(40, 60, 30, 255))

def smoke(d):
    d.rectangle((5, 4, 10, 13), fill=(150, 154, 162, 255), outline=(50, 52, 58, 255))
    d.rectangle((5, 7, 10, 9), fill=(236, 236, 240, 255))
    d.rectangle((6, 2, 9, 3), fill=(90, 92, 100, 255))
    d.point((11, 3), fill=(210, 150, 40, 255))

def molotov(d):
    d.rectangle((5, 7, 10, 14), fill=(60, 150, 80, 220), outline=(24, 70, 36, 255))
    d.rectangle((7, 3, 8, 7), fill=(60, 150, 80, 220), outline=(24, 70, 36, 255))
    d.rectangle((7, 1, 8, 3), fill=(240, 240, 230, 255))
    d.point((8, 0), fill=(250, 170, 40, 255))
    d.point((7, 0), fill=(240, 90, 30, 255))
    d.rectangle((6, 10, 9, 12), fill=(250, 120, 40, 200))

sprite('grenade', grenade)
sprite('smoke_grenade', smoke)
sprite('molotov', molotov)
for nm in ('grenade', 'smoke_grenade', 'molotov'):
    json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "gtacraft:item/" + nm}},
              open(p('models', 'item', nm + '.json'), 'w'), indent=1)
    json.dump({"model": {"type": "minecraft:model", "model": "gtacraft:item/" + nm}}, open(p('items', nm + '.json'), 'w'), indent=1)

# ------------------------------------------------------------------ 3D-модели для летящих гранат и гильз
def solid(name, base, noise=8, seed=1, alpha=255):
    rnd = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            dv = rnd.randint(-noise, noise)
            img.putpixel((x, y), tuple(max(0, min(255, c + dv)) for c in base) + (alpha,))
    img.save(p('textures', 'item', name + '.png'))

solid('gr_green', (74, 96, 56), 8, 21)
solid('gr_metal', (150, 154, 162), 8, 22)
solid('gr_dark', (40, 42, 48), 6, 23)
solid('gr_gray', (130, 134, 142), 8, 24)
solid('gr_white', (236, 236, 240), 4, 25)
solid('gr_glass', (60, 150, 80), 6, 26, 230)
solid('gr_rag', (250, 130, 40), 10, 27)
solid('gr_brass', (222, 178, 60), 8, 28)

def box(x0, y0, z0, x1, y1, z1, tex):
    return {"from": [x0, y0, z0], "to": [x1, y1, z1],
            "faces": {d: {"texture": "#" + tex} for d in ("north", "south", "east", "west", "up", "down")}}

def world_model(name, elements, textures):
    tex = {k: "gtacraft:item/" + v for k, v in textures.items()}
    tex['particle'] = list(tex.values())[0]
    json.dump({"textures": tex, "elements": elements}, open(p('models', 'item', name + '.json'), 'w'), indent=1)
    json.dump({"model": {"type": "minecraft:model", "model": "gtacraft:item/" + name}}, open(p('items', name + '.json'), 'w'), indent=1)

world_model('grenade_world', [
    box(6, 5.5, 6, 10, 10, 10, 'a'), box(6.7, 10, 6.7, 9.3, 11.2, 9.3, 'b'), box(10, 6, 7.4, 10.5, 11, 8.6, 'b'),
    box(5.6, 7, 5.6, 10.4, 8, 10.4, 'c')], {'a': 'gr_green', 'b': 'gr_metal', 'c': 'gr_dark'})
world_model('smoke_world', [
    box(6, 4.5, 6, 10, 10.5, 10, 'a'), box(5.9, 7, 5.9, 10.1, 8.6, 10.1, 'w'), box(6.7, 10.5, 6.7, 9.3, 11.6, 9.3, 'b'),
    box(10, 6, 7.4, 10.5, 11, 8.6, 'b')], {'a': 'gr_gray', 'w': 'gr_white', 'b': 'gr_dark'})
world_model('molotov_world', [
    box(6.2, 3.5, 6.2, 9.8, 9, 9.8, 'g'), box(7.2, 9, 7.2, 8.8, 11.6, 8.8, 'g'), box(7.4, 11.6, 7.4, 8.6, 13, 8.6, 'r'),
    box(6.4, 5, 6.4, 9.6, 7.2, 9.6, 'f')], {'g': 'gr_glass', 'r': 'gr_rag', 'f': 'gr_rag'})
world_model('casing_world', [box(7.3, 6.5, 7.3, 8.7, 9.5, 8.7, 'a'), box(7.3, 9.5, 7.3, 8.7, 10, 8.7, 'b')],
            {'a': 'gr_brass', 'b': 'gr_dark'})

# ------------------------------------------------------------------ анимации оружия (по доле кулдауна)
BASE = {
    "thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 4, 0.5], "scale": [0.85, 0.85, 0.85]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
}
GUNS = {  # имя: (kick_deg, back, pump, reload_ticks)
    'pistol': (22, 1.6, False), 'smg': (12, 1.0, False), 'shotgun': (30, 2.4, True),
    'sniper': (26, 2.2, True), 'rpg': (14, 3.0, False),
}

def pose(dx, dy, dz, rz, rx=0.0, third=1.0):
    """Сдвиг (в 1/16 блока) и поворот относительно базовой позы."""
    out = {}
    for k, base in BASE.items():
        s = third if k.startswith('third') else 1.0
        r = list(base["rotation"]); t = list(base["translation"])
        r[2] += rz * s; r[0] += rx * s
        t[0] += dx * s; t[1] += dy * s; t[2] += dz * s
        out[k] = {"rotation": r, "translation": [round(v, 3) for v in t], "scale": base["scale"]}
    return out

def gun_model(name, key, disp):
    json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "gtacraft:item/" + name}, "display": disp},
              open(p('models', 'item', name + '_' + key + '.json'), 'w'), indent=1)

def dispatch(base_model, frames):
    """frames: [(порог, имя модели)] по возрастанию порога."""
    return {"model": {"type": "minecraft:range_dispatch", "property": "minecraft:cooldown", "scale": 1.0,
                      "fallback": {"type": "minecraft:model", "model": base_model},
                      "entries": [{"threshold": t, "model": {"type": "minecraft:model", "model": m}} for t, m in frames]}}

for name, (kick, back, pump) in GUNS.items():
    # отдача: резкий толчок назад и вверх, затем возврат; у дробовика/винтовки в середине движение затвора
    steps = [(0.12, 0.2), (0.28, 0.4), (0.45, 0.7), (0.62, 1.0), (0.82, 1.0)]
    frames = []
    for i, (thr, amp) in enumerate(steps):
        extra_dz = 0.0; extra_rz = 0.0
        if pump and 0.28 <= thr <= 0.62:
            extra_dz = -2.0 if i % 2 == 0 else 1.5     # передёргивание
            extra_rz = -8.0
        d = pose(0, 0.4 * amp * back, -back * amp + extra_dz, kick * amp + extra_rz, rx=-4 * amp)
        gun_model(name, 'k%d' % i, d)
        frames.append((thr, "gtacraft:item/%s_k%d" % (name, i)))
    json.dump(dispatch("gtacraft:item/" + name, frames), open(p('items', name + '.json'), 'w'), indent=1)

    # перезарядка: опустить, наклонить, достать магазин, вставить, вернуть
    key = [  # порог (доля оставшегося времени), dx, dy, dz, rz, rx
        (0.06, 0.0, -0.5, 0.0, 4, 0), (0.18, 0.4, -2.5, 0.5, 30, -8), (0.34, 0.8, -4.0, 1.0, 48, -14),
        (0.50, -0.6, -4.0, 1.4, 52, -22), (0.64, 0.8, -3.6, 1.0, 46, -14), (0.80, 0.4, -2.2, 0.5, 30, -6), (0.92, 0.0, -0.8, 0.0, 10, 0)]
    rframes = []
    for i, (thr, dx, dy, dz, rz, rx) in enumerate(key):
        gun_model(name, 'r%d' % i, pose(dx, dy, dz, rz, rx, third=0.7))
        rframes.append((thr, "gtacraft:item/%s_r%d" % (name, i)))
    # перезаряжаемая версия предмета: базовая модель с опущенным стволом
    gun_model(name, 'rbase', pose(0.0, -0.6, 0.0, 10, 0, third=0.7))
    json.dump(dispatch("gtacraft:item/%s_rbase" % name, rframes), open(p('items', name + '_reload.json'), 'w'), indent=1)

# ------------------------------------------------------------------ названия
def merge_lang(fn, extra):
    path = p('lang', fn)
    data = json.load(open(path, encoding='utf-8'))
    data.update(extra)
    json.dump(data, open(path, 'w', encoding='utf-8'), ensure_ascii=False, indent=1)

ru = {"item.gtacraft.grenade": "Граната", "item.gtacraft.smoke_grenade": "Дымовая граната", "item.gtacraft.molotov": "Коктейль Молотова"}
en = {"item.gtacraft.grenade": "Frag Grenade", "item.gtacraft.smoke_grenade": "Smoke Grenade", "item.gtacraft.molotov": "Molotov Cocktail"}
for g, (nr, ne) in {'pistol': ('Пистолет', 'Pistol'), 'smg': ('Пистолет-пулемёт', 'SMG'), 'shotgun': ('Дробовик', 'Shotgun'),
                    'sniper': ('Снайперская винтовка', 'Sniper Rifle'), 'rpg': ('Ракетомёт', 'Rocket Launcher')}.items():
    ru["item.gtacraft.%s_reload" % g] = nr + " (перезарядка)"
    en["item.gtacraft.%s_reload" % g] = ne + " (reloading)"
merge_lang('ru_ru.json', ru)
merge_lang('en_us.json', en)
print('assets2 ok')
