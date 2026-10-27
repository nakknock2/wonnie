import { useEffect, useState } from 'react';
import MockFaceId from './MockFaceId/MockFaceId.jsx';
import '../styles/phone.css';

// 상단 상태바 (시간 / 신호 / LTE / 배터리)
function StatusBar() {
  const [now, setNow] = useState(new Date());

  useEffect(() => {
    const t = setInterval(() => setNow(new Date()), 10_000);
    return () => clearInterval(t);
  }, []);

  const h = now.getHours() % 12 || 12;
  const m = String(now.getMinutes()).padStart(2, '0');

  return (
    <div className="status-bar">
      <span className="status-time">
        {h}:{m}
        <svg viewBox="0 0 20 20" width="15" height="15" className="status-mute">
          <path d="M10 3a5 5 0 0 0-5 5v4l-1.5 2h13L15 12V8a5 5 0 0 0-5-5Z" fill="currentColor" />
          <path d="M8 16.5a2 2 0 0 0 4 0" fill="currentColor" />
          <path d="M3 3l14 14" stroke="#f2f3f5" strokeWidth="3.2" />
          <path d="M3 3l14 14" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
        </svg>
      </span>
      <span className="status-right">
        <span className="signal">
          <i style={{ height: 4 }} /><i style={{ height: 6 }} /><i style={{ height: 8 }} /><i style={{ height: 10 }} />
        </span>
        <b className="lte">LTE</b>
        <span className="battery">
          <span className="battery-body"><span className="battery-level">49</span></span>
          <span className="battery-cap" />
        </span>
      </span>
    </div>
  );
}

// 스마트폰 외형 틀. children 으로 앱 화면을 받습니다.
export default function PhoneFrame({ children, faceIdOpen = false, onFaceIdComplete, onFaceIdCancel }) {
  return (
    <div className="phone">
      <span className="phone-btn phone-btn--silent" />
      <span className="phone-btn phone-btn--vol-up" />
      <span className="phone-btn phone-btn--vol-down" />
      <span className="phone-btn phone-btn--power" />

      <div className="phone-screen">
        <MockFaceId open={faceIdOpen} onComplete={onFaceIdComplete} onCancel={onFaceIdCancel} />
        <StatusBar />
        {children}
        <div className="home-indicator" />
      </div>
    </div>
  );
}
