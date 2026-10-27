# 3D 워니 원본에서 눈·입을 지우고(inpaint) 새 표정을 그려 변형 이미지를 만든다
import sys, numpy as np, cv2
from PIL import Image, ImageDraw
from scipy import ndimage as ndi

SRC = '/mnt/user-data/attachments/7e005065-8d89-5445-a09c-e1097e87f887/85c6dceb-image.png'
OUT = sys.argv[1]
CROP = (128, 51, 1176, 1221)          # 기존 wonnie-3d.png 와 같은 영역
W_OUT = 300

src = Image.open(SRC).convert('RGBA').crop(CROP)
arr = np.asarray(src).astype(np.uint8)
rgb, alpha = arr[..., :3].copy(), arr[..., 3]
H, W = alpha.shape
S = W / W_OUT  # 원본/출력 배율 (~3.49)

def comp_mask(box):
    """box(출력 좌표) 안의 어두운 덩어리 + 그 안의 하이라이트까지 포함한 마스크"""
    x0, y0, x1, y1 = [int(v * S) for v in box]
    sub = rgb[y0:y1, x0:x1].astype(int)
    dark = sub.sum(2) < 250
    m = np.zeros((H, W), bool)
    filled = ndi.binary_fill_holes(ndi.binary_closing(dark, iterations=3))
    m[y0:y1, x0:x1] = filled
    return m

def pink_mask(box):
    x0, y0, x1, y1 = [int(v * S) for v in box]
    sub = rgb[y0:y1, x0:x1].astype(int)
    pk = (sub[..., 0] > 190) & (sub[..., 1] < 175)
    m = np.zeros((H, W), bool); m[y0:y1, x0:x1] = pk
    return m

L_EYE = (98, 121, 128, 158)
R_EYE = (178, 106, 206, 142)
MOUTH = (130, 134, 182, 172)
eye_l, eye_r = comp_mask(L_EYE), comp_mask(R_EYE)
mouth = comp_mask(MOUTH) | pink_mask(MOUTH)
mouth = ndi.binary_fill_holes(ndi.binary_closing(mouth, iterations=4))

EYE_COL = tuple(int(v) for v in np.median(rgb[eye_l & (rgb.sum(2) < 120)], axis=0))
MOUTH_COL = tuple(int(v) for v in np.median(rgb[mouth & (rgb.sum(2) < 150)], axis=0))
TONGUE_COL = (240, 120, 112)

def erase(img, mask, grow=10):
    m = ndi.binary_dilation(mask, iterations=grow).astype(np.uint8) * 255
    return cv2.inpaint(img, m, 9, cv2.INPAINT_TELEA)

def center(mask):
    ys, xs = np.where(mask); return xs.mean(), ys.mean(), xs.max() - xs.min(), ys.max() - ys.min()

def draw_arc(d, cx, cy, w, h, up=True, width=None):
    """up=True: ∩ (웃는 감은 눈), up=False: ∪"""
    width = width or int(w * 0.2)
    box = [cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2]
    if up: d.arc(box, 195, 345, fill=EYE_COL, width=width)
    else: d.arc(box, 20, 160, fill=EYE_COL, width=width)

def finish(img, name):
    out = Image.fromarray(np.dstack([img, alpha]), 'RGBA')
    out = out.resize((W_OUT, round(H * W_OUT / W)), Image.LANCZOS)
    out.save(f'{OUT}/{name}.png', optimize=True)
    return out

def with_draw(img, fn):
    pil = Image.fromarray(img).convert('RGB')
    big = pil.resize((W * 2, H * 2), Image.LANCZOS)
    d = ImageDraw.Draw(big)
    fn(d, 2)
    return np.asarray(big.resize((W, H), Image.LANCZOS))

lx, ly, lw, lh = center(eye_l)
rx, ry, rw, rh = center(eye_r)
mx, my, mw, mh = center(mouth)

# 1) 활짝 웃음: 두 눈 ^ ^
img = erase(erase(rgb, eye_l), eye_r)
img = with_draw(img, lambda d, k: (draw_arc(d, lx*k, ly*k+lh*0.2*k, lw*1.9*k, lh*1.15*k, up=True, width=int(lw*0.42*k)),
                                   draw_arc(d, rx*k, ry*k+rh*0.2*k, rw*1.9*k, rh*1.15*k, up=True, width=int(rw*0.42*k))))
finish(img, 'happy')

# 2) 윙크: 왼쪽 눈만 ^
img = erase(rgb, eye_l)
img = with_draw(img, lambda d, k: draw_arc(d, lx*k, ly*k+lh*0.2*k, lw*1.9*k, lh*1.15*k, up=True, width=int(lw*0.42*k)))
finish(img, 'wink')

# 3) 노래: 눈은 편안하게 감고(∪), 입은 동그랗게 'o'
img = erase(erase(erase(rgb, eye_l), eye_r), mouth, grow=12)
def sing(d, k):
    draw_arc(d, lx*k, ly*k-lh*0.1*k, lw*1.7*k, lh*1.0*k, up=False, width=int(lw*0.38*k))
    draw_arc(d, rx*k, ry*k-rh*0.1*k, rw*1.7*k, rh*1.0*k, up=False, width=int(rw*0.38*k))
    ow, oh = mw*0.62*k, mh*1.0*k
    cx, cy = mx*k, (my+mh*0.05)*k
    d.ellipse([cx-ow/2, cy-oh/2, cx+ow/2, cy+oh/2], fill=MOUTH_COL)
    d.chord([cx-ow*0.36, cy, cx+ow*0.36, cy+oh*0.46], 0, 180, fill=TONGUE_COL)
img = with_draw(img, sing)
finish(img, 'sing')

# 4) 놀람: 눈은 그대로, 입은 작은 'o'
img = erase(rgb, mouth, grow=12)
def wow(d, k):
    ow, oh = mw*0.42*k, mh*0.8*k
    cx, cy = mx*k, (my+mh*0.1)*k
    d.ellipse([cx-ow/2, cy-oh/2, cx+ow/2, cy+oh/2], fill=MOUTH_COL)
img = with_draw(img, wow)
finish(img, 'wow')

print('eye', EYE_COL, 'mouth', MOUTH_COL, 'L', (lx/S, ly/S), 'R', (rx/S, ry/S), 'M', (mx/S, my/S))
