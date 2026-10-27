# 시트에서 포즈를 잘라 배경(흰/연회색·그림자) 제거 → 투명 PNG
import sys, numpy as np
from PIL import Image
from scipy import ndimage as ndi

SRC = '/mnt/user-data/attachments/7e005065-8d89-5445-a09c-e1097e87f887/275cb80e-image.png'
OUT = sys.argv[1]
BOXES = {
    'side2':  (1340, 90, 1480, 310),
    'side1':  (1040, 90, 1170, 310),
    'heart':  (1275, 770, 1470, 952),
    'news':   (990, 755, 1230, 952),
    'main':   (465, 35, 850, 505),
    'back':   (1175, 90, 1330, 310),
    'front':  (875, 90, 1035, 310),
}
im = np.asarray(Image.open(SRC).convert('RGB')).astype(np.float32)

for name, (x0, y0, x1, y1) in BOXES.items():
    a = im[y0:y1, x0:x1]
    mx, mn = a.max(2), a.min(2)
    sat = (mx - mn) / np.maximum(mx, 1)
    lum = a.mean(2)
    bgish = (sat < 0.10) & (lum > 195)            # 흰 배경, 연한 패널, 회색 그림자
    lab, _ = ndi.label(bgish)
    border = np.unique(np.concatenate([lab[0], lab[-1], lab[:, 0], lab[:, -1]]))
    bg = np.isin(lab, border[border > 0])
    fg = ~bg
    # 가장 큰 덩어리(캐릭터 + 붙은 소품)만 남기고 반짝이·효과선 제거
    flab, n = ndi.label(fg)
    sizes = ndi.sum(fg, flab, range(1, n + 1))
    keep = 1 + int(np.argmax(sizes))
    fg = flab == keep
    fg = ndi.binary_fill_holes(fg)
    # 가장자리 부드럽게: 배경색과의 거리로 알파 계산
    dist = np.linalg.norm(a - np.array([252, 252, 250]), axis=2)
    soft = np.clip(dist / 60.0, 0, 1)
    edge = fg & ~ndi.binary_erosion(fg, iterations=1)
    alpha = np.where(fg, 1.0, 0.0)
    alpha[edge] = np.maximum(soft[edge], 0.35)
    # 반투명 가장자리 색 오염 제거: 안쪽 색으로 채움
    inner = ndi.binary_erosion(fg, iterations=2)
    idx = ndi.distance_transform_edt(~inner, return_distances=False, return_indices=True)
    rgb = a.copy()
    rgb[edge] = a[idx[0], idx[1]][edge]
    ys, xs = np.where(alpha > 0)
    by0, by1, bx0, bx1 = ys.min(), ys.max() + 1, xs.min(), xs.max() + 1
    out = np.dstack([rgb, alpha * 255]).astype(np.uint8)[by0:by1, bx0:bx1]
    Image.fromarray(out, 'RGBA').save(f'{OUT}/{name}.png')
    print(name, out.shape[1], out.shape[0], 'crop origin', x0 + bx0, y0 + by0)
