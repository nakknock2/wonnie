import { TabIcons } from './Icons.jsx';

// 하단 탭 바 (선택된 탭은 파란색)
export default function BottomNav({ tabs, active, onChange }) {
  return (
    <nav className="bottom-nav">
      {tabs.map((t) => {
        const Icon = TabIcons[t.id];
        const isActive = active === t.id;
        return (
          <button
            key={t.id}
            className={`tab ${isActive ? 'is-active' : ''}`}
            aria-current={isActive ? 'page' : undefined}
            onClick={() => onChange(t.id)}
          >
            <Icon />
            <span>{t.label}</span>
          </button>
        );
      })}
    </nav>
  );
}
