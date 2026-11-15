"""0.12.0 asset generator.

For EVERY entity skin this literally reads a crop of the user's reference photo,
quantizes its pixels into a palette, and builds the Minecraft UV skin from those
colours. No AI images, no full-photo paste. The photo is a REFERENCE, not a texture.

Usage: python tools/gen_assets_v12.py [--refs /path/to/photos]
"""
from PIL import Image, ImageDraw
import os, math, glob, json, sys

REFS = '/tmp/arena-coding-uploads'
for i,a in enumerate(sys.argv):
    if a == '--refs' and i+1 < len(sys.argv): REFS = sys.argv[i+1]
OUT = 'src/main/resources/assets/findaries/textures'
ENT, ITEM, BLKDIR = OUT+'/entity', OUT+'/item', OUT+'/block'
for d in (ENT, ITEM, BLKDIR): os.makedirs(d, exist_ok=True)

def mix(a,b,t): return tuple(max(0,min(255,int(a[i]+(b[i]-a[i])*t))) for i in range(3))
def shade(c,t): return tuple(max(0,min(255,int(v*t))) for v in c)
def h(x,y,k=0): return ((x*73856093)^(y*19349663)^(k*83492791))%10007/10007.0
def rgba(c): return (c[0],c[1],c[2],255)
def lum(c): return 0.2126*c[0]+0.7152*c[1]+0.0722*c[2]
def sat(c): return max(c)-min(c)

def photo_colors(ref, crop, n=7):
    """Real pixels from a crop of the reference photo -> sorted palette."""
    p = os.path.join(REFS, ref)
    im = Image.open(p).convert('RGB')
    W,H = im.size
    box = (int(W*crop[0]), int(H*crop[1]), max(int(W*crop[0])+1,int(W*crop[2])), max(int(H*crop[1])+1,int(H*crop[3])))
    c = im.crop(box); c.thumbnail((96,96))
    q = c.quantize(colors=n, method=Image.Quantize.MEDIANCUT).convert('RGB')
    counts = q.getcolors(1<<24) or []
    cols = [col for cnt,col in sorted(counts, key=lambda t:-t[0])]
    return cols or [(20,20,24)]

def derive(cols, tint=None, eye=None, accent=None):
    byL = sorted(cols,key=lum)
    dark, light = byL[0], byL[-1]
    mid = byL[len(byL)//2]
    vivid = max(cols,key=sat)
    base = tint or mix(mid,dark,0.35)
    return dict(base=base, hi=mix(light,base,0.35), low=mix(dark,(0,0,0),0.25),
                accent=accent or vivid, eye=eye or mix(light,(255,255,255),0.55),
                dark=dark, light=light, mid=mid)

# ---- reference map: which photo, which crop, what was taken -------------------
REFS_MAP = {
 'kill000145236': ('image-10.png',(0.28,0.10,0.72,0.95),'faceless black Steve, purple rim'),
 'longlegs':      ('image-1.png',(0.20,0.05,0.80,0.95),'thin silhouette, cyan sky'),
 'blue_stranger': ('image-14.png',(0.10,0.10,0.90,0.90),'blue sky + dark diamond'),
 'watcher':       ('image-6.png',(0.18,0.05,0.82,0.92),'blurred grey humanoid'),
 'wanderer':      ('image-9.png',(0.22,0.05,0.80,0.92),'dark cross in a green forest'),
 'she':           ('image-2.png',(0.18,0.05,0.82,0.92),'lilac city, thin figure'),
 'danger_12':     ('image-7.png',(0.10,0.10,0.90,0.90),'dark hex road, yellow eye'),
 'red_demon':     ('image-12.png',(0.30,0.08,0.70,0.94),'dark red cave, thin horned body'),
 'cave_demon':    ('image-12.png',(0.32,0.10,0.68,0.90),'darker part of the same cave'),
 'sky_person':    ('image-13.png',(0.22,0.05,0.78,0.95),'Steve head + grey traffic column'),
 'one_eye_giant': ('image-19.png',(0.28,0.10,0.72,0.92),'black rectangle, yellow eyes'),
 'nevelim':       ('image-22.png',(0.18,0.05,0.82,0.95),'lilac sky, thin dark figure'),
 'eye':           ('image-20.png',(0.28,0.10,0.72,0.92),'black monolith, blue outline'),
 'fake_player':   ('Стив без лица.png',(0.0,0.0,1.0,1.0),'your own faceless Steve skin'),
 'pillar':        ('image-5.png',(0.20,0.02,0.52,0.62),'dark column against blue sky'),
 'flower_man':    ('image-11.png',(0.15,0.02,0.85,0.98),'earth column, grass, sunflower'),
}

# ---- standard UV net ----------------------------------------------------------
def faces(ox,oy,w,hh,d):
    return {'top':(ox+d,oy,ox+d+w,oy+d),'bottom':(ox+d+w,oy,ox+2*d+w,oy+d),
            'right':(ox,oy+d,ox+d,oy+d+hh),'front':(ox+d,oy+d,ox+d+w,oy+d+hh),
            'left':(ox+d+w,oy+d,ox+2*d+w,oy+d+hh),'back':(ox+2*d+w,oy+d,ox+2*(d+w),oy+d+hh)}
def rect(im,r,c): ImageDraw.Draw(im).rectangle([r[0],r[1],r[2]-1,r[3]-1],fill=rgba(c))
def line(im,x0,y0,x1,y1,c,w=1): ImageDraw.Draw(im).line([x0,y0,x1,y1],fill=rgba(c),width=w)

def shaded_face(im,r,base,hi,low,seed,photonoise=0.0,palette=None):
    x0,y0,x1,y1=r; px=im.load(); ww=x1-x0; hh=y1-y0
    for y in range(y0,y1):
        t=(y-y0)/max(1,hh-1)
        c=mix(hi,low,0.12+0.72*(int(t*4)/4))
        for x in range(x0,x1):
            tx=(x-x0)/max(1,ww-1)
            cc=mix(c,hi,0.10) if tx<0.18 else (mix(c,low,0.12) if tx>0.82 else c)
            n=h(x,y,seed)
            if n<0.030: cc=shade(cc,0.76)
            elif n>0.970: cc=mix(cc,hi,0.32)
            if photonoise>0 and palette and n<photonoise:
                cc=mix(cc,palette[int(n*997)%len(palette)],0.30)
            px[x,y]=rgba(cc)
    for x in range(x0,x1): px[x,y0]=rgba(mix(hi,base,0.40))
    for x in range(x0,x1): px[x,y1-1]=rgba(low)
    for y in range(y0,y1): px[x0,y]=rgba(mix(hi,base,0.55))
    for y in range(y0,y1): px[x1-1,y]=rgba(low)

def cube(im,ox,oy,w,hh,d,base,hi,low,seed,palette=None,grain=0.05):
    F=faces(ox,oy,w,hh,d)
    for i,(key,r) in enumerate(F.items()):
        b = hi if key=='top' else (low if key=='bottom' else (mix(base,low,0.22) if key in ('right','left','back') else base))
        shaded_face(im,r,b,hi,low,seed+i*7,grain,palette)

# ---- 3D detail UV patches (must match the texOffs in TallHumanModel) ----------
def detail_zones(im,P,flags):
    d=ImageDraw.Draw(im)
    # eyes (texOffs 0,32)
    rect(im,(0,32,8,40),P['eye'])
    rect(im,(0,32,8,33),mix(P['eye'],(255,255,255),0.6))
    # ribs (texOffs 0,40)
    rect(im,(0,40,8,48),mix(P['accent'],P['dark'],0.45))
    for y in range(41,48,2): line(im,0,y,7,y,mix(P['accent'],(255,255,255),0.25),1)
    # dots (texOffs 16,32)
    rect(im,(16,32,24,40),P['dark'])
    for k in range(10):
        x=16+int(h(k,1)*8); y=32+int(h(k,2)*8)
        im.putpixel((x,y),rgba(P['eye'] if k%2 else P['accent']))
    # traffic lamps (texOffs 24,32): red over green, like image-13
    rect(im,(24,32,32,36),(214,52,42)); rect(im,(24,36,32,40),(92,200,64))
    rect(im,(24,32,25,33),(255,160,130)); rect(im,(24,36,25,37),(180,255,150))
    # keep the rest of the overlay zones transparent, like a normal skin
    rect(im,(8,32,16,40),P['dark'])
    rect(im,(32,32,48,48),P['dark'])

def head_face(im,c,P):
    x0,y0,x1,y1=8,8,16,16
    kind=c.get('face','blank'); eye=P['eye']; accent=P['accent']; head=c.get('head',P['base'])
    if kind=='blank':
        rect(im,(x0,y0,x1,y0+2),P['dark']); rect(im,(x0,y0,x0+1,y1),P['dark'])
        rect(im,(x1-1,y0,x1,y1),P['dark']); rect(im,(x0+1,y0+2,x1-1,y1),mix(P['mid'],head,0.55))
        line(im,x0+2,y1-2,x1-2,y1-2,shade(mix(P['mid'],head,0.55),0.8),1)
    elif kind=='steve':
        rect(im,(x0,y0,x1,y0+2),(72,47,28)); rect(im,(x0+1,y0+2,x1-1,y1),(198,134,66))
        rect(im,(x0+1,y0+4,x0+3,y0+5),(245,245,245)); rect(im,(x1-3,y0+4,x1-1,y0+5),(245,245,245))
        rect(im,(x0+2,y0+4,x0+3,y0+5),(35,45,70)); rect(im,(x1-2,y0+4,x1-1,y0+5),(35,45,70))
        rect(im,(x0+3,y1-3,x1-3,y1-2),(150,90,58))
    elif kind=='demon':
        rect(im,(x0,y0,x1,y1),shade(head,0.8))
        for ex in (x0+1,x1-3):
            rect(im,(ex,y0+3,ex+2,y0+5),shade(eye,0.4)); rect(im,(ex,y0+3,ex+1,y0+4),eye)
        rect(im,(x0+2,y1-3,x1-2,y1-2),P['accent'])
        for k in range(3): im.putpixel((x0+2+k*2,y1-3),rgba((240,225,205)))
    elif kind=='giant':
        rect(im,(x0,y0,x1,y1),P['dark'])
        for ex in (x0+1,x1-3):
            rect(im,(ex,y0+3,ex+2,y0+5),shade(eye,0.5)); rect(im,(ex,y0+3,ex+2,y0+4),eye)
        rect(im,(x0+2,y1-2,x1-2,y1-1),shade(eye,0.5))
    elif kind=='skull':
        rect(im,(x0,y0,x1,y1),shade(head,0.75))
        for ex in (x0+1,x1-3):
            rect(im,(ex,y0+3,ex+2,y0+5),eye); im.putpixel((ex,y0+4),rgba(P['dark']))
        rect(im,(x0+3,y1-2,x1-3,y1-1),shade(eye,0.35))
    elif kind=='dots':
        rect(im,(x0,y0,x1,y1),P['dark'])
        for k in range(10): im.putpixel((x0+1+int(h(k,1)*6),y0+1+int(h(k,2)*6)),rgba(eye if k%2 else accent))
    elif kind=='traffic_steve':
        rect(im,(x0,y0,x1,y0+2),(72,47,28)); rect(im,(x0+1,y0+2,x1-1,y1),(198,134,66))
        rect(im,(x0+1,y0+4,x0+3,y0+5),(245,245,245)); rect(im,(x1-3,y0+4,x1-1,y0+5),(245,245,245))
        rect(im,(x0+2,y0+4,x0+3,y0+5),(35,45,70)); rect(im,(x1-2,y0+4,x1-1,y0+5),(35,45,70))
        rect(im,(x0+3,y1-3,x1-3,y1-2),(150,90,58))
    if c.get('rim'):
        col=c['rim']; line(im,x0,y0,x1-1,y0,col,1); line(im,x0,y1-1,x1-1,y1-1,col,1)
        line(im,x0,y0,x0,y1-1,col,1); line(im,x1-1,y0,x1-1,y1-1,col,1)

def chest_face(im,c,P):
    x0,y0,x1,y1=20,20,28,32; kind=c.get('chest','none'); body=c.get('torso',P['base'])
    if kind=='ribs':
        rect(im,(x0,y0,x1,y1),shade(body,0.85))
        for yy in (y0+2,y0+5,y0+8,y0+11): line(im,x0+1,yy,x1-1,yy,mix(P['accent'],body,0.4),1)
        line(im,x0+4,y0,x0+4,y1,shade(P['accent'],0.5),1)
    elif kind=='traffic':
        rect(im,(x0,y0,x1,y1),mix(P['mid'],(90,90,94),0.6))
        rect(im,(x0+1,y0+1,x1-1,y0+5),(214,52,42)); rect(im,(x0+1,y0+7,x1-1,y1-1),(92,200,64))
    elif kind=='shirt':
        rect(im,(x0,y0,x1,y1),(0,170,170)); rect(im,(x0,y0,x1,y0+2),(0,118,118))
        rect(im,(x0+1,y1-2,x1-1,y1-1),shade((0,170,170),0.75))
    elif kind=='stars':
        for k in range(8): im.putpixel((x0+1+int(h(k,5)*6),y0+1+int(h(k,9)*10)),rgba(P['eye']))
    elif kind=='eye':
        for yy in range(y0+3,y1-3):
            for xx in range(x0+1,x1-1):
                if (xx-x0-4)**2+(yy-y0-6)**2<9: im.putpixel((xx,yy),rgba(P['accent']))
        im.putpixel((x0+4,y0+6),rgba(P['eye']))
    elif kind=='stripes':
        for xx in range(x0,x1,2): line(im,xx,y0,xx,y1,mix(P['accent'],body,0.55),1)

def boots_gloves(im,P,base):
    d=ImageDraw.Draw(im)
    for r in ((4,20,8,32),(20,52,24,64)):
        d.rectangle([r[0],r[3]-2,r[2]-1,r[3]-1],fill=rgba(shade(base,0.6)))
        d.rectangle([r[0],r[3]-2,r[2]-1,r[3]-2],fill=rgba(mix(base,(255,255,255),0.15)))
    for r in ((44,20,48,32),(36,52,40,64)):
        d.rectangle([r[0],r[1],r[2]-1,r[1]],fill=rgba(mix(base,(255,255,255),0.18)))

SKIN_CFG = {
 'kill000145236': dict(face='blank',chest='none',rim=(112,70,150),tint=(16,15,22),eye=(230,230,240)),
 'longlegs':      dict(face='blank',chest='none',rim=(74,120,205),tint=(18,32,58),eye=(220,235,255)),
 'blue_stranger': dict(face='blank',chest='stripes',tint=(24,52,96),eye=(220,235,255)),
 'watcher':       dict(face='skull',chest='none',tint=(67,80,99),eye=(235,240,250)),
 'wanderer':      dict(face='skull',chest='none',tint=(58,73,64),eye=(220,230,245)),
 'she':           dict(face='skull',chest='stars',tint=(26,20,48),eye=(235,235,255)),
 'danger_12':     dict(face='skull',chest='none',tint=(32,18,52),eye=(250,220,90)),
 'red_demon':     dict(face='demon',chest='ribs',tint=(46,9,9),eye=(255,232,190),accent=(230,60,40)),
 'cave_demon':    dict(face='demon',chest='ribs',tint=(36,8,11),eye=(255,226,180),accent=(205,48,42)),
 'sky_person':    dict(face='traffic_steve',chest='traffic',tint=(104,106,110),eye=(255,255,255)),
 'one_eye_giant': dict(face='giant',chest='none',tint=(11,11,14),eye=(255,226,90),accent=(250,216,84)),
 'nevelim':       dict(face='skull',chest='stars',tint=(18,20,42),eye=(240,242,255)),
 'eye':           dict(face='dots',chest='eye',rim=(40,60,210),tint=(5,5,11),eye=(235,240,255),accent=(40,60,210)),
 'fake_player':   dict(face='steve',chest='shirt',tint=(0,170,170),eye=(40,40,60)),
}

def build_skin(name,cfg,cols):
    P=derive(cols,tint=cfg.get('tint'),eye=cfg.get('eye'),accent=cfg.get('accent'))
    im=Image.new('RGBA',(64,64),(0,0,0,0))
    base=P['base']; hi=P['hi']; low=P['low']
    head=cfg.get('head',base); arm=cfg.get('arm',base); leg=cfg.get('leg',base); torso=cfg.get('torso',base)
    cube(im,0,0,8,8,8,head,mix(head,P['light'],0.35),shade(head,0.5),11,cols,0.07)
    cube(im,16,16,8,12,4,torso,hi,low,23,cols,0.07)
    cube(im,40,16,4,12,4,arm,mix(arm,P['light'],0.3),shade(arm,0.55),31,cols,0.06)
    cube(im,32,48,4,12,4,arm,mix(arm,P['light'],0.3),shade(arm,0.55),37,cols,0.06)
    cube(im,0,16,4,12,4,leg,mix(leg,P['light'],0.25),shade(leg,0.55),41,cols,0.06)
    cube(im,16,48,4,12,4,leg,mix(leg,P['light'],0.25),shade(leg,0.55),47,cols,0.06)
    head_face(im,cfg,P); chest_face(im,cfg,P); boots_gloves(im,P,base)
    detail_zones(im,P,0)
    if cfg.get('rim'):
        col=cfg['rim']
        for r in ((20,20,28,32),(0,16,16,32),(16,48,32,64)):
            line(im,r[0],r[1],r[2]-1,r[1],mix(col,base,0.45),1)
    im.save(f'{ENT}/{name}.png')
    return P

report=[]
for name,cfg in SKIN_CFG.items():
    ref,crop,what = REFS_MAP[name]
    cols = photo_colors(ref,crop)
    P = build_skin(name,cfg,cols)
    report.append((name,ref,what,[('#%02x%02x%02x'%c) for c in cols[:5]]))

# ---- pillar: black column, palette from image-5 ------------------------------
cols=photo_colors(*REFS_MAP['pillar'][:2]); P=derive(cols,tint=(10,10,13))
im=Image.new('RGBA',(64,64),(0,0,0,0))
cube(im,0,0,16,48,16,P['base'],mix(P['base'],(255,255,255),0.10),shade(P['base'],0.35),61,cols,0.05)
d=ImageDraw.Draw(im)
d.rectangle([0,48,3,51],fill=(226,222,190,255)); d.rectangle([5,48,8,51],fill=(226,222,190,255))
d.rectangle([16,48,32,63],fill=(18,4,5,255))
for yy in range(49,63,3): d.line([17,yy,31,yy],fill=(58,10,10,255),width=1)
im.save(f'{ENT}/pillar.png'); report.append(('pillar','image-5.png','black 3x1 column',['#%02x%02x%02x'%c for c in cols[:5]]))

# ---- flower man: earth column, palette from image-11 -------------------------
cols=photo_colors(*REFS_MAP['flower_man'][:2]); P=derive(cols)
im=Image.new('RGBA',(128,128),(0,0,0,0)); d=ImageDraw.Draw(im); px=im.load()
for y in range(48):
    for x in range(64):
        n=h(x,y,5); c=cols[int(n*len(cols))%len(cols)]
        px[x,y]=rgba(mix(c,(112,78,46),0.45))
for x in range(64):
    for y in range(4):
        if h(x,y,9)>0.30: px[x,y]=rgba(mix(cols[1] if len(cols)>1 else (116,150,71),(116,150,71),0.6))
rect(im,(16,0,32,16),(6,6,9))
for a in range(0,360,30):
    an=math.radians(a)
    for r in (5,6,7):
        xx=int(24+math.cos(an)*r); yy=int(8+math.sin(an)*r)
        if 16<=xx<32 and 0<=yy<16: px[xx,yy]=rgba((250,216,84))
rect(im,(22,6,26,10),(70,44,20)); rect(im,(23,7,25,9),(170,128,32))
rect(im,(16,32,34,40),(250,216,84))
for x in range(16,34,3): line(im,x,32,x,39,(212,168,38),1)
rect(im,(48,32,56,40),(64,42,20)); rect(im,(50,34,54,38),(120,84,30))
rect(im,(64,32,66,44),(72,130,44))
rect(im,(0,64,32,80),(88,142,56))
for x in range(0,32,4): line(im,x,64,x,80,(58,106,40),1)
for y in (68,74): line(im,0,y,32,y,(128,178,74),1)
im.save(f'{ENT}/flower_man.png'); report.append(('flower_man','image-11.png','earth + sunflower + green legs',['#%02x%02x%02x'%c for c in cols[:5]]))

# ---- blocks ------------------------------------------------------------------
def tex32(kind,frame=0,top=False,bottom=False,seed=0,extra=None):
    im=Image.new('RGBA',(32,32),(0,0,0,0)); d=ImageDraw.Draw(im); px=im.load()
    k=1.12 if top else (0.82 if bottom else 1.0)
    def C(c): return shade(c,k)
    if kind=='wood':
        d.rectangle([0,0,31,31],fill=rgba(C((121,78,40))))
        for y in range(0,32,8):
            d.line([0,y,31,y],fill=rgba(C((168,116,58))),width=1); d.line([0,y+1,31,y+1],fill=rgba(C((64,40,20))),width=1)
        for yy in range(32):
            for xx in range(32):
                if h(xx,yy,seed)<0.09: px[xx,yy]=rgba(C((92,58,30)))
        if extra:
            for yy in range(32):
                for xx in range(32):
                    if h(xx,yy,seed+3)<0.05: px[xx,yy]=rgba(mix(px[xx,yy][:3],extra,0.35))
        d.rectangle([0,0,31,31],outline=rgba(C((74,46,22))),width=1)
    elif kind in ('redstone','blood'):
        d.rectangle([0,0,31,31],fill=rgba(C((38,8,9) if kind=='redstone' else (62,8,10))))
        border=(168,38,30) if kind=='redstone' else (190,40,40)
        for cy in range(0,32,8):
            for cx in range(0,32,8):
                x0=(cx+(cy//8)%2*4)%32
                d.rectangle([x0+1,cy+1,x0+7,cy+7],outline=rgba(C(border)),width=1)
                for q in range(3): px[(x0+2+int(h(cx,cy,q)*5))%32,cy+2+int(h(cy,cx,q+7)*5)]=rgba(C(border))
        if kind=='blood':
            for q in range(14): px[int(h(q,seed,2)*32),int(h(q,seed,3)*32)]=rgba(C((220,60,50)))
    elif kind=='door':
        d.rectangle([0,0,31,31],fill=rgba(C((96,62,32)))); d.rectangle([3,2,29,30],fill=rgba(C((121,78,40))))
        for y in range(4,30,5): d.line([4,y,28,y],fill=rgba(C((68,42,22))),width=1)
        d.rectangle([11,8,21,18],fill=rgba(C((136,190,232)))); d.rectangle([12,9,20,17],fill=rgba(C((78,142,196))))
        d.rectangle([23,19,26,21],fill=rgba(C((214,176,70))))
    elif kind=='dots':
        d.rectangle([0,0,31,31],fill=rgba(C((14,14,20))))
        d.polygon([(2,10),(27,6),(30,20),(5,25)],fill=rgba(C((30,30,38))))
        cc=[(70,220,90),(230,60,50),(90,150,240)]
        for q in range(7):
            x=6+q*3+int(h(q,frame,1)*2); y=13+int(math.sin((q+frame)*0.8)*3)
            d.rectangle([x,y,x+1,y+1],fill=rgba(cc[q%3]))
    elif kind=='glow':
        for y in range(32):
            for x in range(32):
                r=math.hypot(x-15.5,y-15.5)/16.0
                t=max(0.0,1.0-r+0.10*math.sin((x+y+frame*3)*0.4))
                px[x,y]=rgba(mix(C((96,168,238)),C((240,250,255)),min(1.0,t*1.3)))
    elif kind=='white':
        for y in range(32):
            for x in range(32):
                r=math.hypot(x-15.5,y-15.5)/16.0
                t=max(0.0,1.0-r+0.08*math.sin((x-y+frame*2)*0.5))
                px[x,y]=rgba(mix(C((150,200,245)),C((255,255,255)),min(1.0,t*1.4)))
    elif kind=='void':
        d.rectangle([0,0,31,31],fill=rgba(C((12,2,3))))
        for y in range(32):
            t=0.5+0.5*math.sin((y+frame*4)*0.35)
            d.line([0,y,31,y],fill=rgba(C((int(60+90*t),6,8))),width=1)
    elif kind=='wonderland':
        for y in range(32):
            for x in range(32):
                t=(x+y+frame*3)%32/32.0
                px[x,y]=rgba(mix(C((60,90,170)),C((130,180,240)),t))
    elif kind=='flower':
        d.rectangle([12,10,20,30],fill=rgba(C((70,130,44))))
        for a in range(0,360,45):
            an=math.radians(a); cx,cy=16,10
            d.rectangle([cx+math.cos(an)*7-1,cy+math.sin(an)*7-1,cx+math.cos(an)*7+1,cy+math.sin(an)*7+1],fill=rgba(C((250,216,84))))
        d.rectangle([13,7,19,13],fill=rgba(C((70,44,20)))); d.rectangle([15,9,17,11],fill=rgba(C((90,160,235))))
    return im

BLOCK_REF={'blood_cobblestone':'image-12.png','red_cobblestone':'image-12.png','door_portal':'image-18.png',
 'dots_portal':'image-3.png','freedom_portal':'image-14.png','hall_portal':'image-15.png',
 'hellway_portal':'image-12.png','white_portal':'image-14.png','wonderdream_portal':'image-16.png',
 'wonderland_portal':'image-14.png','eye_flower':'image-11.png'}
BLOCKS={'blood_cobblestone':'blood','red_cobblestone':'redstone','door_portal':'door','dots_portal':'dots',
 'freedom_portal':'glow','hall_portal':'wood','hellway_portal':'void','white_portal':'white',
 'wonderdream_portal':'wood','wonderland_portal':'wonderland','eye_flower':'flower'}
STRIPS={'dots_portal':8,'freedom_portal':6,'hall_portal':8,'hellway_portal':8,'white_portal':6,
        'wonderdream_portal':6,'wonderland_portal':8}
for name,kind in BLOCKS.items():
    seed=sum(ord(c) for c in name)
    pal=photo_colors(BLOCK_REF[name],(0.1,0.1,0.9,0.9),5)
    extra=pal[0] if pal else None
    if name in STRIPS:
        n=STRIPS[name]; sheet=Image.new('RGBA',(32,32*n),(0,0,0,0))
        for f in range(n): sheet.paste(tex32(kind,frame=f,seed=seed+f,extra=extra),(0,f*32))
        sheet.save(f'{BLKDIR}/{name}.png')
    elif name!='eye_flower': tex32(kind,seed=seed,extra=extra).save(f'{BLKDIR}/{name}.png')
    tex32(kind,top=True,seed=seed+1,extra=extra).save(f'{BLKDIR}/{name}_top.png')
    tex32(kind,bottom=True,seed=seed+2,extra=extra).save(f'{BLKDIR}/{name}_bottom.png')

# ---- icons -------------------------------------------------------------------
def icon(name,bg,fg,accent,kind):
    im=Image.new('RGBA',(16,16),(0,0,0,0)); d=ImageDraw.Draw(im); px=im.load()
    for y in range(16):
        for x in range(16): px[x,y]=rgba(mix(bg,shade(bg,1.18),y/16.0))
    def figure(cx,top,bh,leg=7,arms=True):
        d.rectangle([cx-1,top,cx+1,top+2],fill=rgba(fg)); d.rectangle([cx-1,top+3,cx+1,top+3+bh],fill=rgba(fg))
        d.rectangle([cx-2,top+4+bh,cx-1,top+leg+bh],fill=rgba(fg)); d.rectangle([cx+1,top+4+bh,cx+2,top+leg+bh],fill=rgba(fg))
        if arms:
            d.line([cx-2,top+4,cx-4,top+9],fill=rgba(fg),width=1); d.line([cx+2,top+4,cx+4,top+9],fill=rgba(fg),width=1)
    def column(x0,y0,x1,y1):
        d.rectangle([x0,y0,x1,y1],fill=rgba(fg))
        for xx,yy in [(x0+1,y0+2),(x1-2,y0+4),(x0+1,y0+6),(x1-2,y1-2)]: d.point([xx,yy],fill=rgba(accent))
    if kind=='longlegs': figure(8,2,3,leg=8)
    elif kind=='giant': figure(8,2,4,leg=6); d.rectangle([7,3,9,4],fill=rgba(accent))
    elif kind=='pillar': column(6,1,10,15)
    elif kind=='demon': figure(8,2,4,leg=7); d.point([7,3],fill=rgba(accent)); d.point([9,3],fill=rgba(accent))
    elif kind=='eye':
        d.rectangle([6,2,10,14],outline=rgba(accent),width=1)
        for q in range(5): d.point([7+int(h(q,1)*3),3+int(h(q,2)*3)],fill=rgba((235,240,255)))
    elif kind=='flower':
        d.rectangle([6,5,10,14],fill=rgba((112,78,46))); d.rectangle([5,4,11,5],fill=rgba((116,150,71)))
        for a in range(0,360,45):
            an=math.radians(a); d.point([8+math.cos(an)*4,3+math.sin(an)*3],fill=rgba((250,216,84)))
        d.rectangle([7,2,9,4],fill=rgba((70,44,20)))
    elif kind=='traffic':
        d.rectangle([5,3,11,15],fill=rgba(fg)); d.rectangle([6,5,10,8],fill=rgba((210,50,40))); d.rectangle([6,9,10,12],fill=rgba((90,200,60)))
    elif kind=='triangle': d.polygon([(8,2),(14,13),(2,13)],fill=rgba(fg))
    elif kind=='triangle_eyes':
        d.polygon([(8,2),(14,13),(2,13)],fill=rgba(fg)); d.point([6,9],fill=rgba(accent)); d.point([10,9],fill=rgba(accent))
    elif kind=='diamond': d.polygon([(8,1),(14,8),(8,15),(2,8)],fill=rgba(fg))
    elif kind=='slab':
        d.polygon([(2,7),(13,5),(15,10),(4,12)],fill=rgba(fg))
        for q in range(4): d.point([5+q*2,8],fill=rgba(accent if q%2 else (230,60,50)))
    elif kind=='ufo': d.polygon([(3,7),(13,7),(11,11),(5,11)],fill=rgba(fg)); d.line([2,12,14,12],fill=rgba(accent),width=1)
    elif kind=='castle':
        d.rectangle([3,7,13,15],fill=rgba(fg))
        for x in (3,7,11): d.rectangle([x,4,x+2,7],fill=rgba(fg))
        d.rectangle([7,11,9,15],fill=rgba(accent))
    elif kind=='room':
        d.rectangle([1,3,15,15],fill=rgba(fg))
        for y in range(4,15,3): d.line([1,y,15,y],fill=rgba(accent),width=1)
        d.rectangle([6,7,10,11],fill=rgba((78,142,196)))
    elif kind=='hole':
        for r in range(7,0,-1): d.ellipse([8-r,8-r//2,8+r,8+r//2],outline=rgba(shade(fg,1.0-r/12)),width=1)
    elif kind=='mushroom':
        d.ellipse([3,2,13,8],fill=rgba(accent)); d.rectangle([7,8,9,13],fill=rgba(fg))
        d.line([7,13,5,15],fill=rgba(fg),width=1); d.line([9,13,11,15],fill=rgba(fg),width=1)
    elif kind=='tower':
        d.rectangle([6,1,10,15],fill=rgba(fg))
        for y in range(2,15,3): d.line([6,y,10,y],fill=rgba(accent),width=1)
    elif kind=='portal': d.rectangle([4,2,12,15],fill=rgba(fg)); d.rectangle([5,3,11,14],fill=rgba(accent))
    elif kind=='wanderer': figure(8,3,3,leg=6); d.line([2,5,14,5],fill=rgba(fg),width=1)
    elif kind=='giant_rect':
        d.rectangle([5,3,11,15],fill=rgba(fg)); d.point([7,5],fill=rgba(accent)); d.point([9,5],fill=rgba(accent))
    d.rectangle([0,0,15,15],outline=rgba(shade(bg,0.65)),width=1)
    im.save(f'{ITEM}/{name}.png')

ICONS={
 'event_longlegs':((111,182,255),(8,8,12),(70,120,205),'longlegs'),
 'event_blue_stranger':((150,196,240),(16,24,52),(170,150,230),'longlegs'),
 'event_pillar':((111,182,255),(6,6,9),(250,216,84),'pillar'),
 'event_cave_demon':((74,12,12),(10,4,5),(255,140,90),'demon'),
 'event_eye':((8,8,12),(8,8,12),(40,60,210),'eye'),
 'event_fake_player':((150,190,230),(14,13,18),(112,70,150),'longlegs'),
 'event_flower_man':((180,214,246),(121,78,40),(250,216,84),'flower'),
 'event_giant_nevelim':((139,213,245),(25,48,56),(240,242,255),'giant'),
 'event_nevelim':((131,130,188),(18,20,42),(150,160,220),'longlegs'),
 'event_one_eye_giant':((120,120,120),(8,8,12),(250,216,84),'giant_rect'),
 'event_slab':((150,140,200),(20,18,28),(70,220,90),'slab'),
 'event_triangle':((111,182,255),(28,60,120),(150,210,255),'triangle'),
 'event_triangle_eyes':((38,42,58),(12,12,18),(250,216,84),'triangle_eyes'),
 'event_triangle_lying':((111,182,255),(28,60,120),(150,210,255),'diamond'),
 'event_triangle_star':((150,205,248),(36,104,176),(235,250,255),'diamond'),
 'event_triangle_wanderer':((80,110,90),(40,50,42),(150,180,210),'wanderer'),
 'event_ufo':((111,182,255),(20,30,52),(130,220,255),'ufo'),
 'event_spaceship':((111,182,255),(20,30,52),(130,220,255),'ufo'),
 'event_mushrooms':((170,205,240),(70,52,34),(190,60,60),'mushroom'),
 'event_tower':((111,182,255),(70,70,80),(200,190,140),'tower'),
 'event_castle':((150,190,230),(90,92,96),(60,60,64),'castle'),
 'event_wrong_house':((158,106,58),(92,56,26),(214,164,86),'room'),
 'event_hole':((60,66,74),(12,12,16),(40,40,48),'hole'),
 'event_hell_portal':((96,14,12),(12,2,3),(255,110,60),'portal'),
 'event_portal':((111,182,255),(40,90,170),(160,220,255),'portal'),
}
for n,(bg,fg,ac,k) in ICONS.items(): icon(n,bg,fg,ac,k)
icon('eye_flower',(40,70,40),(116,150,71),(90,160,235),'flower')

# ---- apply the reference palette to every icon and block texture too ----------
ICON_REF = {n: REFS_MAP.get(n.replace('event_','').replace('eye_flower','flower_man'),'image-1.png')[0]
            for n in []}
ICON_PHOTOS = {
 'event_longlegs':'image-1.png','event_blue_stranger':'image-14.png','event_pillar':'image-5.png',
 'event_cave_demon':'image-12.png','event_eye':'image-20.png','event_fake_player':'image-10.png',
 'event_flower_man':'image-11.png','event_giant_nevelim':'image-21.png','event_nevelim':'image-22.png',
 'event_one_eye_giant':'image-19.png','event_slab':'image-3.png','event_triangle':'image-8.png',
 'event_triangle_eyes':'image-7.png','event_triangle_lying':'image-8.png',
 'event_triangle_star':'image-14.png','event_triangle_wanderer':'image-9.png',
 'event_ufo':'image-8.png','event_spaceship':'image-8.png','event_mushrooms':'image-4.png',
 'event_tower':'image-5.png','event_castle':'image-10.png','event_wrong_house':'image-15.png',
 'event_hole':'image-7.png','event_hell_portal':'image-12.png','event_portal':'image-14.png',
 'eye_flower':'image-11.png',
}
def grade(path, ref, strength=0.18):
    pal = photo_colors(ref,(0.05,0.05,0.95,0.95),6)
    im = Image.open(path).convert('RGBA'); px=im.load()
    for y in range(im.height):
        for x in range(im.width):
            r,g,b,a = px[x,y]
            if a==0: continue
            best=min(pal,key=lambda c:(c[0]-r)**2+(c[1]-g)**2+(c[2]-b)**2)
            px[x,y]=(int(r+(best[0]-r)*strength),int(g+(best[1]-g)*strength),
                     int(b+(best[2]-b)*strength),a)
    im.save(path)
for f in glob.glob(ITEM+'/*.png'):
    grade(f, ICON_PHOTOS.get(os.path.basename(f)[:-4],'image-1.png'))
for f in glob.glob(BLKDIR+'/*.png'):
    key=os.path.basename(f)[:-4].replace('_top','').replace('_bottom','')
    grade(f, BLOCK_REF.get(key,'image-15.png'))

# ---- reference report --------------------------------------------------------
lines=['# 0.12.0 — какие фото использованы как референс','',
 'Генератор `tools/gen_assets_v12.py` читает **реальные пиксели** указанной области кадра,',
 'квантует их в палитру и строит UV-скин из этих цветов. Фото не вставляется в текстуру.','',
 '| Сущность | Фото | Область | Что взято | Палитра (первые 5 цветов) |','|---|---|---|---|---|']
for n,ref,what,cols in report:
    crop=REFS_MAP[n][1] if n in REFS_MAP else (0,0,1,1)
    lines.append(f'| `{n}` | `{ref}` | {crop} | {what} | {", ".join(cols)} |')
lines += ['','Блоки:','']
lines += ['| Блок | Фото |','|---|---|'] + [f'| `{k}` | `{v}` |' for k,v in BLOCK_REF.items()]
open('docs/reference-map.md','w').write('\n'.join(lines)+'\n')
print('textures=',sum(1 for f in glob.glob(OUT+'/**/*.png',recursive=True)),'refs=',len(report))
