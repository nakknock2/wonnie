# 시트의 메인 포즈 + 표정 7종을 투명 PNG 로 (cut.py 와 같은 방식)
import sys, numpy as np
from PIL import Image
from scipy import ndimage as ndi
SRC = '/mnt/user-data/attachments/7e005065-8d89-5445-a09c-e1097e87f887/275cb80e-image.png'
OUT = sys.argv[1]
BOXES = {
    'main': (465, 35, 850, 505),
    'face-default': (40, 540, 205, 686), 'face-smile': (215, 540, 392, 686),
    'face-excited': (402, 535, 578, 686), 'face-thinking': (588, 540, 768, 686),
    'face-listening': (778, 535, 962, 686), 'face-surprised': (972, 540, 1148, 686),
    'face-wink': (1157, 540, 1332, 686),
}
im = np.asarray(Image.open(SRC).convert('RGB')).astype(np.float32)
for name, (x0, y0, x1, y1) in BOXES.items():
    a = im[y0:y1, x0:x1]
    mx, mn = a.max(2), a.min(2)
    sat = (mx - mn) / np.maximum(mx, 1); lum = a.mean(2)
    bgish = (sat < 0.10) & (lum > 195)
    lab, _ = ndi.label(bgish)
    border = np.unique(np.concatenate([lab[0], lab[:, 0], lab[:, -1]]))   # 아래 가장자리는 몸이 잘린 곳이라 제외
    bg = np.isin(lab, border[border > 0])
    fg = ~bg
    flab, n = ndi.label(fg)
    sizes = ndi.sum(fg, flab, range(1, n + 1))
    fg = ndi.binary_fill_holes(flab == 1 + int(np.argmax(sizes)))
    dist = np.linalg.norm(a - np.array([250, 250, 246]), axis=2)
    soft = np.clip(dist / 60.0, 0, 1)
    edge = fg & ~ndi.binary_erosion(fg, iterations=1)
    alpha = np.where(fg, 1.0, 0.0); alpha[edge] = np.maximum(soft[edge], 0.35)
    inner = ndi.binary_erosion(fg, iterations=2)
    idx = ndi.distance_transform_edt(~inner, return_distances=False, return_indices=True)
    rgb = a.copy(); rgb[edge] = a[idx[0], idx[1]][edge]
    ys, xs = np.where(alpha > 0)
    out = np.dstack([rgb, alpha * 255]).astype(np.uint8)[ys.min():ys.max() + 1, xs.min():xs.max() + 1]
    img = Image.fromarray(out, 'RGBA')
    if name.startswith('face-'):
        # 원형 아바타용 정사각형: 아래쪽(얼굴·몸) 기준으로 자르고 양옆 여백
        w, h = img.size; side = max(w, h - int(h * 0.12))
        sq = Image.new('RGBA', (side, side), (0, 0, 0, 0))
        sq.alpha_composite(img, ((side - w) // 2, side - h + int(h * 0.12) if h > side else side - h))
        img = sq.resize((140, 140), Image.LANCZOS)
    elif name == 'main':
        img = img.resize((260, round(img.height * 260 / img.width)), Image.LANCZOS)
    img.save(f'{OUT}/{name}.png', optimize=True)
    print(name, img.size)
