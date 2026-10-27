import { useEffect, useState } from 'react';
import { ChevronLeft, Check, ScanFace } from 'lucide-react';
import PhoneFrame from '../../components/PhoneFrame.jsx';
import splashReference from '../../assets/splash-reference.png';
import wonnieLeaning from '../../assets/wonnie-leaning.png';

const PhonePage = ({ children }) => <PhoneFrame>{children}</PhoneFrame>;

const Splash = () => (
  <PhonePage>
    <main className="relative h-full overflow-hidden bg-white" aria-label="WONNIE 시작 화면">
      <svg className="absolute left-[24.46%] top-[33.52%] block h-auto w-[51.45%] overflow-hidden" viewBox="285 659 452 322" role="img" aria-label="2026 우리금융 다함께 페스타">
        <image href={splashReference} width="970" height="1963" />
      </svg>
      <img className="absolute left-[39%] top-[28.65%] z-[1] h-auto w-[20.4%] select-none" src={wonnieLeaning} alt="웃는 워니" draggable="false" />
      <p className="absolute left-[4%] top-[53.81%] m-0 w-[92%] text-center text-[3.18cqw] font-semibold leading-[1.48] tracking-[-.045em] text-[#737777]">우리와 손잡는 순간,<br />비교할 수 없는 역대급 혜택이 펼쳐집니다</p>
      <svg className="absolute left-[34.81%] top-[83.42%] h-auto w-[30.74%] overflow-hidden" viewBox="376 1610 270 92" role="img" aria-label="Mobile Accessibility">
        <image href={splashReference} width="970" height="1963" />
      </svg>
      <svg className="absolute left-[41.75%] top-[92.71%] h-auto w-[16.85%] overflow-hidden" viewBox="437 1787 148 52" role="img" aria-label="우리은행">
        <image href={splashReference} width="970" height="1963" />
      </svg>
    </main>
  </PhonePage>
);

const Login = ({ onLogin }) => (
  <PhonePage>
    <main className="relative flex h-full flex-col bg-white px-7 pb-9 pt-12 text-[#353b43]" aria-label="로그인">
      <button className="absolute left-5 top-5 text-[#353b43]" aria-label="뒤로 가기" type="button"><ChevronLeft size={30} strokeWidth={2} /></button>
      <section className="flex flex-1 flex-col items-center pt-[25%]">
        <h1 className="m-0 text-[27px] font-bold tracking-[-.07em]">우리WON인증서</h1>
        <p className="mb-0 mt-3 text-[19px] font-medium text-[#737a83]">로그인하고 WONNIE를 시작해 보세요.</p>
        <button className="mt-28 grid size-[128px] place-items-center rounded-full bg-[#e3f2ff] text-[#28323b]" type="button" onClick={onLogin} aria-label="Google 로그인">
          <ScanFace size={66} strokeWidth={1.8} />
        </button>
        <p className="mt-12 flex items-center gap-3 text-[20px] font-semibold"><span className="grid size-8 place-items-center rounded-full border-[3px] border-[#353b43]"><Check size={21} strokeWidth={3} /></span>Google 자동 로그인</p>
      </section>
      <button className="h-16 rounded-full border-2 border-[#e0e4e8] text-[20px] font-semibold text-[#353b43]" type="button" onClick={onLogin}>다른 방법으로 로그인</button>
    </main>
  </PhonePage>
);

export const AuthGate = ({ children }) => {
  const [state, setState] = useState('splash');
  const [user, setUser] = useState(null);

  const check = () => {
    setState('loading');
    fetch('/api/auth/me', { credentials: 'include' })
      .then((response) => {
        if (!response.ok) throw new Error(`auth-${response.status}`);
        return response.json();
      })
      .then((nextUser) => { setUser(nextUser); setState('signed-in'); })
      .catch((error) => { setState(/401|403|unauthorized/i.test(error.message) ? 'signed-out' : 'error'); });
  };

  useEffect(() => {
    const timer = window.setTimeout(check, 2000);
    return () => window.clearTimeout(timer);
  }, []);

  const login = () => {
    const url = window.location.hostname === 'wonnie.minngyuseong.com'
      ? 'https://api.wonnie.minngyuseong.com/oauth2/authorization/google'
      : '/oauth2/authorization/google';
    window.location.assign(url);
  };

  if (state === 'splash' || state === 'loading') return <Splash />;
  if (state === 'signed-out' || state === 'error') return <Login onLogin={login} />;
  return user ? children(user) : <Splash />;
};
