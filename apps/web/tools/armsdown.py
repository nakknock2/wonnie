# 들어 올린 오른손을 지우고, 왼팔을 좌우반전해 붙여 '두 팔 내린' 자세로
import sys, numpy as np
from PIL import Image
src, dst = sys.argv[1], sys.argv[2]
im = Image.open(src).convert('RGBA')
K = 4
big = im.resize((im.width * K, im.height * K), Image.LANCZOS)
a = np.asarray(big).astype(np.float32)
H, W = a.shape[:2]
al = a[..., 3]

def edge_right(y):
    x = W // 2
    while x < W - 1 and al[y, x] > 40: x += 1
    return x
def edge_left(y):
    x = W // 2
    while x > 0 and al[y, x] > 40: x -= 1
    return x

er = np.array([edge_right(y) for y in range(H)])
el = np.array([edge_left(y) for y in range(H)])
y0, y1 = 100 * K, 232 * K
# 팔이 붙어 있는 구간은 위·아래 몸 가장자리로 보간
ya, yb = 118 * K, 232 * K
for y in range(y0, y1):
    t = (y - ya) / (yb - ya)
    base = er[ya] * (1 - t) + er[yb] * t
    # 몸통 곡선(볼록)을 살짝 반영
    bulge = 7 * K * np.sin(np.pi * min(max(t, 0), 1))
    cut = int(base + bulge - 1.5 * K)
    er[y] = min(er[y], cut) if y < 140 * K else cut
out = a.copy()
for y in range(y0, y1):
    out[y, er[y] + 1:, 3] = 0
    # 가장자리 부드럽게
    for dx, f in ((0, 0.55), (-1, 0.85)):
        x = er[y] + dx
        out[y, x, 3] *= f
# 왼팔(몸 왼쪽 바깥) 복사 → 반전
# 팔이 붙은 구간의 몸 왼쪽 가장자리를 위·아래에서 보간
la, lb = 176 * K, 276 * K
el_body = el.copy()
for y in range(la, lb):
    t = (y - la) / (lb - la)
    el_body[y] = int(el[la] * (1 - t) + el[lb] * t - 6 * K * np.sin(np.pi * t))
arm = np.zeros_like(a)
for y in range(la, lb):
    e = el_body[y] + 2 * K
    seg = a[y, :e].copy()
    seg[..., 3] *= (np.arange(e) < el_body[y] + K)  # 몸 쪽은 제외
    m = seg[::-1]
    target = er[y] if y < y1 else edge_right(y)
    start = target - 2 * K
    end = min(W, start + m.shape[0])
    arm[y, start:end] = m[:end - start]
# 합성: 팔 알파가 있는 곳만 덮기
aa = arm[..., 3:4] / 255.0
res = out.copy()
res[..., :3] = out[..., :3] * (1 - aa) + arm[..., :3] * aa
res[..., 3] = np.maximum(out[..., 3], arm[..., 3])
Image.fromarray(res.clip(0, 255).astype(np.uint8), 'RGBA').resize(im.size, Image.LANCZOS).save(dst, optimize=True)
print('ok')
