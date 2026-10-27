import { useEffect, useRef, useState } from 'react';
import { CloseIcon, PromoIcons } from './Icons.jsx';

// 상단 이벤트 배너: 3.5초마다 자동으로 넘어가고, 점(dot)을 눌러 이동, 좌우 스와이프 가능
export default function PromoBanner({ banners, onClose, onClick }) {
  const [index, setIndex] = useState(0);
  const [paused, setPaused] = useState(false);
  const startX = useRef(null);

  useEffect(() => {
    if (paused) return;
    const t = setInterval(() => setIndex((i) => (i + 1) % banners.length), 3500);
    return () => clearInterval(t);
  }, [paused, banners.length]);

  const go = (dir) => setIndex((i) => (i + dir + banners.length) % banners.length);

  const onPointerDown = (e) => {
    startX.current = e.clientX;
    setPaused(true);
  };
  const onPointerUp = (e) => {
    if (startX.current !== null) {
      const dx = e.clientX - startX.current;
      if (Math.abs(dx) > 40) go(dx < 0 ? 1 : -1);
      else onClick(banners[index]);
    }
    startX.current = null;
    setPaused(false);
  };

  return (
    <section className="promo">
      <div className="promo-card">
        <div
          className="promo-track"
          style={{ transform: `translateX(-${index * 100}%)` }}
          onPointerDown={onPointerDown}
          onPointerUp={onPointerUp}
          onPointerLeave={() => { startX.current = null; setPaused(false); }}
        >
          {banners.map((b) => (
            <div className="promo-slide" key={b.id}>
              <div className="promo-icon" style={{ background: b.bg }}>
                {PromoIcons[b.icon](b.color)}
              </div>
              <p className="promo-text">
                {b.line1}
                <br />
                {b.line2}
              </p>
            </div>
          ))}
        </div>
        <button className="close-btn promo-close" aria-label="배너 닫기" onClick={onClose}>
          <CloseIcon />
        </button>
      </div>

      <div className="dots">
        {banners.map((b, i) => (
          <button
            key={b.id}
            className={`dot ${i === index ? 'is-active' : ''}`}
            aria-label={`${i + 1}번 배너`}
            onClick={() => setIndex(i)}
          />
        ))}
      </div>
    </section>
  );
}
