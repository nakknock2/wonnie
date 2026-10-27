import { ChevronRight } from './Icons.jsx';

// 적립 혜택 프로모션 카드 (노란 카드)
export function SavingsPromo({ onClick }) {
  return (
    <button className="savings-promo" onClick={onClick}>
      <div>
        <p className="savings-promo__title">최대 7.5% 적립 +</p>
        <p className="savings-promo__sub">추석맞이 복주머니 적금 이벤트</p>
      </div>
      <span className="savings-promo__art" aria-hidden="true">🧧</span>
    </button>
  );
}

// 가로 스크롤 서비스 묶음 카드 (보라 / 파랑)
export function ServiceCarousel({ groups, onClick }) {
  return (
    <div className="h-scroll service-carousel">
      {groups.map((g) => (
        <div key={g.id} className="service-group" style={{ background: g.bg }}>
          {g.items.map((s) => (
            <button key={s.id} className="service-item" onClick={() => onClick(s.title)}>
              <span className="service-item__icon">{s.emoji}</span>
              <span className="service-item__text">
                <strong>{s.title}</strong>
                <em>{s.desc}</em>
              </span>
            </button>
          ))}
        </div>
      ))}
    </div>
  );
}

// 금융그룹 서비스 (가로 스크롤 카드)
export function GroupServices({ title, items, onClick }) {
  return (
    <section className="group-services">
      <h2 className="section-title">{title}</h2>
      <div className="h-scroll group-scroll">
        {items.map((it) => (
          <button key={it.id} className="group-card" onClick={() => onClick(it.title)}>
            <strong style={{ color: it.color }}>{it.title}</strong>
            <span>
              {it.desc[0]}
              <br />
              {it.desc[1]}
            </span>
            <i className="group-card__art" aria-hidden="true">{it.emoji}</i>
          </button>
        ))}
      </div>
    </section>
  );
}

// 공지 바
export function NoticeBar({ text, onClick }) {
  return (
    <button className="notice" onClick={onClick}>
      <span className="notice__badge">공지</span>
      <span className="notice__text">{text}</span>
      <ChevronRight className="notice__chev" />
    </button>
  );
}
