#!/usr/bin/env python3
"""Generate Dragonsteel and Dragon Armor sprites with L palette transfer.

Requires Python 3 and Pillow. Run: python3 tools/generate_assets.py
Exact input PNGs are under tools/texture_sources. Existing iaftfc geometry
is preserved, using the upstream Ice and Fire ingot palette fitted to the
same form's TFC Wrought Iron OKLab lightness range. Dragon Armor uses its
TFC metal's ingot palette fitted to that metal's sheet lightness range.
Unfinished armor is excluded.
"""
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
SOURCES = ROOT / 'tools/texture_sources'
FORMS = {
    'item': ('axe_head', 'double_ingot', 'double_sheet', 'hoe_head', 'ingot',
             'pickaxe_head', 'rod', 'sheet', 'shovel_head', 'sword_blade'),
    'block': ('block', 'smooth'),
}

def pixels(image):
    return list(image.get_flattened_data() if hasattr(image, 'get_flattened_data') else image.getdata())

def palette(image, fixed=()):
    fixed = set(fixed)
    return sorted({p[:3] for i, p in enumerate(pixels(image)) if p[3] and i not in fixed},
                  key=lambda c: (.2126*c[0]+.7152*c[1]+.0722*c[2], c))

def lab(rgb):
    def linear(c):
        c /= 255
        return c/12.92 if c <= .04045 else ((c+.055)/1.055)**2.4
    r, g, b = map(linear, rgb)
    l = (.4122214708*r+.5363325363*g+.0514459929*b)**(1/3)
    m = (.2119034982*r+.6806995451*g+.1073969566*b)**(1/3)
    s = (.0883024619*r+.2817188376*g+.6299787005*b)**(1/3)
    return (.2104542553*l+.793617785*m-.0040720468*s,
            1.9779984951*l-2.428592205*m+.4505937099*s,
            .0259040371*l+.7827717662*m-.808675766*s)

def linear_rgb(L, a, b):
    l = (L+.3963377774*a+.2158037573*b)**3
    m = (L-.1055613458*a-.0638541728*b)**3
    s = (L-.0894841775*a-1.291485548*b)**3
    return (4.0767416621*l-3.3077115913*m+.2309699292*s,
            -1.2684380046*l+2.6097574011*m-.3413193965*s,
            -.0041960863*l-.7034186147*m+1.707614701*s)

def rgb(L, a, b):
    value = linear_rgb(L, a, b)
    if min(value) < -1e-7 or max(value) > 1+1e-7:
        lo, hi = 0., 1.
        for _ in range(24):
            mid = (lo+hi)/2
            test = linear_rgb(L, a*mid, b*mid)
            if min(test) >= 0 and max(test) <= 1:
                lo = mid
            else:
                hi = mid
        value = linear_rgb(L, a*lo, b*lo)
    def encode(c):
        c = min(1, max(0, c))
        return round(255*(12.92*c if c <= .0031308 else 1.055*c**(1/2.4)-.055))
    return tuple(map(encode, value))

def extent(values):
    values = list(values)
    return min(values), max(values)

def fit(value, old, new):
    t = (value-old[0])/(old[1]-old[0]) if old[1]-old[0] > 1e-7 else .5
    return new[0]+t*(new[1]-new[0])

def normalize(colors, reference):
    original = [lab(c) for c in colors]
    target = [lab(c) for c in reference]
    lightness = extent(c[0] for c in original), extent(c[0] for c in target)
    result = []
    for L, a, b in original:
        L = fit(L, *lightness)
        result.append(rgb(L, a, b))
    # Retain the source palette's entries, including any rounded duplicates.
    return result

def sample(colors, t):
    pos = t*(len(colors)-1)
    i = min(int(pos), max(0, len(colors)-2))
    f = pos-i
    return tuple(round(a*(1-f)+b*f) for a, b in zip(colors[i], colors[min(i+1,len(colors)-1)]))

def recolor(shape, colors, fixed=()):
    fixed = set(fixed)
    own = palette(shape, fixed)
    lookup = {c: sample(colors, i/(len(own)-1) if len(own)>1 else .5) for i,c in enumerate(own)}
    old = pixels(shape)
    out = Image.new('RGBA', shape.size)
    out.putdata([p if i in fixed else (*lookup[p[:3]],p[3]) if p[3] else (0,0,0,0) for i,p in enumerate(old)])
    assert out.getchannel('A').tobytes() == shape.getchannel('A').tobytes()
    assert all(pixels(out)[i] == old[i] for i in fixed)
    return out

def load(path):
    return Image.open(path).convert('RGBA')


def generate_dragon_armor():
    count = 0
    for metal in ('steel', 'black_steel', 'red_steel', 'blue_steel'):
        original = load(SOURCES / f'tfc/item/metal/ingot/{metal}.png')
        tone = load(SOURCES / f'tfc/item/metal/sheet/{metal}.png')
        colors = normalize(palette(original), palette(tone))
        for part in ('head', 'neck', 'body', 'tail'):
            for namespace, relative in (
                ('iaftfc', f'item/dragonarmor_{metal}_{part}.png'),
                ('iceandfire', f'entity/dragon_armor/armor_{part}_{metal}.png'),
            ):
                shape = load(SOURCES / namespace / relative)
                target = ROOT / 'src/main/resources/assets' / namespace / 'textures' / relative
                target.parent.mkdir(parents=True, exist_ok=True)
                recolor(shape, colors).save(target)
                count += 1
    return count


def main():
    count = 0
    for element in ('fire', 'ice', 'lightning'):
        metal = f'dragonsteel_{element}'
        original = load(SOURCES / f'iceandfire/item/{metal}_ingot.png')
        for kind, forms in FORMS.items():
            for form in forms:
                relative = f'{kind}/metal/{form}/{metal}.png'
                shape = load(SOURCES / 'iaftfc' / relative)
                tone = load(SOURCES / f'tfc/{kind}/metal/{form}/wrought_iron.png')
                image = recolor(shape, normalize(palette(original), palette(tone)))
                target = ROOT / 'src/main/resources/assets/iaftfc/textures' / relative
                target.parent.mkdir(parents=True, exist_ok=True)
                image.save(target)
                count += 1
    armor_count = generate_dragon_armor()
    print(f'Generated {count} Dragonsteel and {armor_count} Dragon Armor textures; unfinished armor excluded.')


if __name__ == '__main__':
    main()
