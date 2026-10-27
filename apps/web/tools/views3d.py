# 3D 워니 원본으로 '뒷모습'과 '옆(3/4)모습' 변형 만들기
import sys, numpy as np, cv2
from PIL import Image, ImageDraw
from scipy import ndimage as ndi

SRC = '/mnt/user-data/attachments/7e005065-8d89-5445-a09c-e1097e87f887/85c6dceb-image.png'
OUT = sys.argv[1]
CROP = (128, 51, 1176, 1221)
W_OUT = 300
src = Image.open(SRC).convert('RGBA').crop(CROP)
arr = np.asarray(src).astype(np.uint8)
rgb, alpha = arr[..., :3].copy(), arr[..., 3]
H, W = alpha.shape
S = W / W_OUT

def box_mask(box, test):
    x0, y0, x1, y1 = [int(v * S) for v in box]
    sub = rgb[y0:y1, x0:x1].astype(int)
    m = np.zeros((H, W), bool)
    m[y0:y1, x0:x1] = test(sub)
    return m

dark = lambda s: s.sum(2) < 250
brow = lambda s: (s.sum(2) < 420) & (s[..., 1] > s[..., 0])       # 진한 초록 눈썹
pink = lambda s: (s[..., 0] > 190) & (s[..., 0] - s[..., 1] > 22)

def fill(m, it=3):
    return ndi.binary_fill_holes(ndi.binary_closing(m, iterations=it))

eye_l = fill(box_mask((98, 121, 128, 158), dark))
eye_r = fill(box_mask((178, 106, 206, 142), dark))
mouth = fill(box_mask((130, 134, 182, 172), lambda s: dark(s) | pink(s)), 4)
brow_l = fill(box_mask((88, 95, 122, 116), brow), 2)
brow_r = fill(box_mask((172, 80, 205, 102), brow), 2)
blush_l = fill(box_mask((72, 148, 110, 178), pink), 3)
blush_r = fill(box_mask((196, 122, 244, 166), pink), 3)
# 배 패치: 몸통보다 밝은 영역
y0, y1, x0, x1 = int(178 * S), int(312 * S), int(70 * S), int(258 * S)
sub = rgb[y0:y1, x0:x1].astype(int)
lum = sub.mean(2); sat = sub.max(2) - sub.min(2)
belly = np.zeros((H, W), bool)
belly[y0:y1, x0:x1] = (lum > 200) & (sat < 120)
belly = ndi.binary_opening(belly, iterations=6)
lab, n = ndi.label(belly)
if n:
    sizes = ndi.sum(belly, lab, range(1, n + 1)); belly = lab == (1 + int(np.argmax(sizes)))
belly = ndi.binary_fill_holes(belly)

def inpaint(img, mask, grow=10, r=9):
    m = ndi.binary_dilation(mask, iterations=grow).astype(np.uint8) * 255
    return cv2.inpaint(img, m, r, cv2.INPAINT_TELEA)

def save(img, name, flip=False):
    out = Image.fromarray(np.dstack([img, alpha]), 'RGBA')
    if flip: out = out.transpose(Image.FLIP_LEFT_RIGHT)
    out = out.resize((200, round(H * 200 / W)), Image.LANCZOS)
    out.save(f'{OUT}/{name}.png', optimize=True)

# 뒷모습: 얼굴·볼·배 패치 자리를 '몸통 색 분포(2차 곡면 맞춤)'로 덮고 좌우 반전
def body_fit(mask_out):
    body = (alpha > 250) & ~ndi.binary_dilation(mask_out, iterations=12)
    body = ndi.binary_erosion(body, iterations=10)
    # 팔·다리 제외: 몸통 대략 범위
    yy, xx = np.mgrid[0:H, 0:W]
    core = (xx > 62 * S) & (xx < 250 * S) & (yy > 60 * S) & (yy < 300 * S)
    sel = body & core
    ys, xs = np.where(sel)
    idx = np.random.default_rng(0).choice(len(ys), min(40000, len(ys)), replace=False)
    ys, xs = ys[idx], xs[idx]
    def feats(x, y):
        x = x / W; y = y / H
        return np.stack([np.ones_like(x), x, y, x * x, x * y, y * y, x ** 3, y ** 3, x * x * y, x * y * y], -1)
    A = feats(xs.astype(float), ys.astype(float))
    coef = [np.linalg.lstsq(A, rgb[ys, xs, c].astype(float), rcond=None)[0] for c in range(3)]
    F = feats(xx.astype(float), yy.astype(float))
    return np.stack([F @ coef[c] for c in range(3)], -1)

face = eye_l | eye_r | mouth | brow_l | brow_r | blush_l | blush_r
cover = ndi.binary_dilation(face, iterations=12) | ndi.binary_dilation(belly, iterations=10)
fit = body_fit(face | belly)
w = cv2.GaussianBlur(cover.astype(np.float32), (0, 0), 10)[..., None]
w = np.clip(w * 1.6, 0, 1) * (alpha[..., None] > 0)
noise = cv2.GaussianBlur(np.random.default_rng(1).normal(0, 2.2, (H, W)).astype(np.float32), (0, 0), 2)[..., None]
back = (rgb * (1 - w) + (fit + noise) * w).clip(0, 255).astype(np.uint8)
save(back, 'back', flip=True)

# 옆(3/4) 모습: 왼쪽 눈·눈썹·볼과 입을 지우고, 오른쪽 가장자리에 작은 입
side = inpaint(rgb, eye_l | brow_l | blush_l | mouth, 10)
pil = Image.fromarray(side).resize((W * 2, H * 2), Image.LANCZOS)
d = ImageDraw.Draw(pil)
k = 2 * S
EYE = tuple(int(v) for v in np.median(rgb[eye_r & (rgb.sum(2) < 120)], axis=0))
# 작은 미소 (오른쪽 눈 아래·바깥쪽)
cx, cy = 214 * k, 150 * k
d.arc([cx - 13 * k, cy - 9 * k, cx + 13 * k, cy + 9 * k], 20, 150, fill=EYE, width=int(3.2 * k))
side = np.asarray(pil.resize((W, H), Image.LANCZOS))
save(side, 'side')
print('ok', belly.sum())
