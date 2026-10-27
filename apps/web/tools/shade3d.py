# 평면 워니 PNG(투명 배경)에 3D 점토 질감 입히기
# 실루엣 → 높이맵(색이 다른 부위는 따로 볼록) → 법선 → half-lambert 조명 + 넓은 은은한 광택 + 부드러운 AO
import sys, numpy as np
from PIL import Image
from scipy import ndimage as ndi

def dome(mask, R, blur):
    dist = ndi.distance_transform_edt(mask)
    t = np.clip(dist / R, 0, 1)
    h = R * np.sqrt(1 - (1 - t) ** 2)
    return ndi.gaussian_filter(h, blur) * mask, dist

def shade(src, dst, scale=2, light=(-0.5, -0.8, 0.85)):
    im = Image.open(src).convert('RGBA')
    im = im.resize((im.width * scale, im.height * scale), Image.LANCZOS)
    a = np.asarray(im).astype(np.float32) / 255.0
    rgb, al = a[..., :3], a[..., 3]
    mask = al > 0.5
    size = min(mask.shape)
    h, dist = dome(mask, max(4.0, 0.2 * size), 0.04 * size)

    # 색이 다른 부위(분홍 하트, 아주 밝은 꼬리)는 위에 한 번 더 볼록하게
    r, g, b = rgb[..., 0], rgb[..., 1], rgb[..., 2]
    pink = mask & (r > g + 0.15) & (r > 0.6)
    tail = mask & (r > 0.85) & (g > 0.9) & (b > 0.75)
    for sub, amp in ((pink, 0.9), (tail, 0.6)):
        sub = ndi.binary_opening(sub, iterations=2)
        if sub.sum() > 30:
            hs, _ = dome(sub, max(3.0, 0.12 * size), 0.02 * size)
            h = h + hs * amp

    gy, gx = np.gradient(h)
    k = 0.9
    n = np.dstack([-gx * k, -gy * k, np.ones_like(h)])
    n /= np.linalg.norm(n, axis=2, keepdims=True)
    L = np.array(light, np.float32); L /= np.linalg.norm(L)
    Hv = L + np.array([0, 0, 1.0]); Hv /= np.linalg.norm(Hv)
    ndl = n @ L
    wrap = ((ndl * 0.5 + 0.5) ** 1.6)                   # half-lambert: 부드러운 명암
    sheen = np.clip(n @ Hv, 0, 1) ** 7                  # 넓고 은은한 광택 (매트)
    ao = 0.9 + 0.1 * np.clip(dist / (0.04 * size), 0, 1)
    bounce = np.clip(n[..., 1], 0, 1) * np.clip(1 - n[..., 2], 0, 1)   # 아래쪽 반사광
    lum = rgb.mean(2)
    feature = lum < 0.35

    out = rgb * ((0.7 + 0.42 * wrap) * ao)[..., None]
    out += (sheen * np.where(feature, 0.22, 0.1))[..., None] * np.array([1.0, 1.0, 0.92])
    out += (bounce * 0.12)[..., None] * np.array([1.0, 1.0, 0.7])
    # 밝은 쪽은 3D 워니처럼 노란 기 도는 연두로
    warm = np.clip(wrap - 0.55, 0, 1)[..., None] * (~feature)[..., None]
    out = out * (1 - 0.35 * warm) + np.array([0.86, 0.95, 0.52]) * 0.35 * warm
    gm = out.mean(2, keepdims=True); out = gm + (out - gm) * 1.1
    # 둥근 눈(어두운 작은 덩어리)에 반짝이 하이라이트
    lab, nlab = ndi.label(feature & mask)
    cands = []
    for i in range(1, nlab + 1):
        ys, xs = np.where(lab == i)
        area = len(ys)
        hgt, wid = np.ptp(ys) + 1, np.ptp(xs) + 1
        if area < 0.0006 * mask.size or area > 0.02 * mask.size:
            continue
        if not (0.6 < hgt / wid < 1.8) or area < 0.55 * hgt * wid:   # 길쭉한 입·감은 눈 제외
            continue
        cands.append((area, ys, xs, hgt, wid))
    min_area = min((c[0] for c in cands), default=0)
    for area, ys, xs, hgt, wid in cands:
        if area > 1.7 * min_area:                                 # 눈보다 큰 덩어리(벌린 입) 제외
            continue
        cy, cx = ys.min() + hgt * 0.32, xs.min() + wid * 0.66
        rr = max(1.2, 0.2 * min(hgt, wid))
        yy, xx = np.ogrid[:mask.shape[0], :mask.shape[1]]
        dot = np.clip(1.5 - np.sqrt((yy - cy) ** 2 + (xx - cx) ** 2) / rr, 0, 1)
        out = out * (1 - dot[..., None]) + dot[..., None]
    out = np.clip(out, 0, 1)
    Image.fromarray((np.dstack([out, al]) * 255).astype(np.uint8), 'RGBA').save(dst, optimize=True)

if __name__ == '__main__':
    # 사용법: shade3d.py SCALE src dst [src dst ...]
    sc = int(sys.argv[1])
    for s, d in zip(sys.argv[2::2], sys.argv[3::2]):
        shade(s, d, scale=sc); print('ok', d)
