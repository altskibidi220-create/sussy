import os, json, zipfile
from PIL import Image

# Order MUST match registration order in SoulRegistry (glyph = U+E000 + index)
SOULS = [
 ("vitality",0xFF7AA2),("might",0xE63946),("swiftness",0x2EE6C5),("inferno",0xFF8C1A),
 ("frost",0x9BE7FF),("tempest",0x5B6CFF),("gale",0xC8F7DC),("terra",0xA9743F),
 ("tide",0x1E90FF),("shadow",0x7A3FD1),("venom",0x5FD13A),("radiance",0xFFD84A)]

MASK = [
"................",
"....oooooooo....",
"..ooHHHHHHHHoo..",
".oHHHbbbbbbbbso.",
".oHbbbbbbbbbbso.",
".oHbkkbbbbkkbso.",
".oHbkkbbbbkkbso.",
".oHbbbbbbbbbbso.",
".oHbbbbkkbbbbso.",
".oHbbbbbbbbbbso.",
".oHbbbbbbbbbsso.",
"..oHbbbbbbbbsso.",
"...ooHbbbbbsoo..",
"....oooooooo.oso",
"............oso.",
"................"]

def mix(a,b,t): return tuple(int(a[i]*(1-t)+b[i]*t) for i in range(3))
def palette(rgb):
    base=((rgb>>16)&255,(rgb>>8)&255,rgb&255)
    return {
     'H': mix(base,(255,255,255),0.65),
     'b': mix(base,(255,255,255),0.18),
     's': mix(base,(0,0,0),0.35),
     'o': mix(base,(0,0,0),0.62),
     'k': (14,14,22)}

root="/home/claude/pack"
for d in ["assets/soulsmp/textures/item","assets/soulsmp/models/item","assets/soulsmp/items","assets/soulsmp/font"]:
    os.makedirs(f"{root}/{d}",exist_ok=True)

sheet=Image.new("RGBA",(len(SOULS)*17*6+6,16*6+12),(24,24,30,255))
providers=[]
for i,(sid,rgb) in enumerate(SOULS):
    pal=palette(rgb); img=Image.new("RGBA",(16,16),(0,0,0,0))
    for y,row in enumerate(MASK):
        assert len(row)==16,(y,row)
        for x,ch in enumerate(row):
            if ch!='.': img.putpixel((x,y),pal[ch]+(255,))
    img.save(f"{root}/assets/soulsmp/textures/item/soul_{sid}.png")
    json.dump({"parent":"minecraft:item/generated","textures":{"layer0":f"soulsmp:item/soul_{sid}"}},open(f"{root}/assets/soulsmp/models/item/soul_{sid}.json","w"),indent=2)
    json.dump({"model":{"type":"minecraft:model","model":f"soulsmp:item/soul_{sid}"}},open(f"{root}/assets/soulsmp/items/soul_{sid}.json","w"),indent=2)
    providers.append({"type":"bitmap","file":f"soulsmp:item/soul_{sid}.png","ascent":8,"height":9,"chars":[chr(0xE000+i)]})
    sheet.paste(img.resize((96,96),Image.NEAREST),(6+i*102,6),img.resize((96,96),Image.NEAREST))
json.dump({"providers":providers},open(f"{root}/assets/soulsmp/font/icons.json","w"),indent=2)
json.dump({"pack":{"description":"§bSoulSMP §7- Soul textures","pack_format":75,
  "supported_formats":{"min_inclusive":64,"max_inclusive":75}}},open(f"{root}/pack.mcmeta","w"),indent=2)
Image.open(f"{root}/assets/soulsmp/textures/item/soul_frost.png").resize((128,128),Image.NEAREST).save(f"{root}/pack.png")
sheet.save("/home/claude/soul_preview.png")

os.makedirs("/mnt/user-data/outputs",exist_ok=True)
with zipfile.ZipFile("/mnt/user-data/outputs/SoulSMP-ResourcePack.zip","w",zipfile.ZIP_DEFLATED) as z:
    for dp,_,fs in os.walk(root):
        for f in fs:
            full=os.path.join(dp,f); z.write(full,os.path.relpath(full,root))
print("ok")
