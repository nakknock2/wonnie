import { useEffect, useState } from "react";
import { ChevronLeft, Check } from "lucide-react";
import PhoneFrame from "../../components/PhoneFrame.jsx";
import splashReference from "../../assets/splash-reference.png";
import wonnieLeaning from "../../assets/wonnie-leaning.png";
import wonnieMascot from "../../assets/wonnie-3d.png";

// 스플래시 디자인 작업 중에는 화면을 고정한다. 작업이 끝나면 false로 바꾼다.
const HOLD_SPLASH_FOR_DESIGN = false;

const PhonePage = ({ children }) => (
  <div className="stage">
    <PhoneFrame>{children}</PhoneFrame>
  </div>
);

const Splash = () => (
  <PhonePage>
    <main
      className="relative h-full overflow-hidden bg-white"
      aria-label="WONNIE 시작 화면"
    >
      <svg
        className="absolute left-[24.46%] top-[33.52%] block h-auto w-[51.45%] overflow-hidden"
        viewBox="285 659 452 322"
        role="img"
        aria-label="2026 우리금융 다함께 페스타"
      >
        <image href={splashReference} width="970" height="1963" />
      </svg>
      <img
        className="absolute left-[39%] top-[28.65%] z-[1] h-auto w-[20.4%] select-none"
        src={wonnieLeaning}
        alt="웃는 워니"
        draggable="false"
      />
      <p
        className="absolute left-[4%] top-[53.81%] m-0 w-[92%] text-center text-lg
       font-semibold leading-[1.48] tracking-[-.045em] text-[#737777]"
      >
        우리와 손잡는 순간,
        <br />
        비교할 수 없는 역대급 혜택이 펼쳐집니다
      </p>
      <svg
        className="absolute left-[34.81%] top-[83.42%] h-auto w-[30.74%] overflow-hidden"
        viewBox="376 1610 270 92"
        role="img"
        aria-label="Mobile Accessibility"
      >
        <image href={splashReference} width="970" height="1963" />
      </svg>
      <svg
        className="absolute left-[41.75%] top-[92.71%] h-auto w-[16.85%] overflow-hidden"
        viewBox="437 1787 148 52"
        role="img"
        aria-label="우리은행"
      >
        <image href={splashReference} width="970" height="1963" />
      </svg>
    </main>
  </PhonePage>
);

const GoogleButton = ({ onClick }) => (
  <button
    className="flex h-12 w-full items-center justify-center gap-3 rounded-[4px] border border-[#747775] bg-white px-4 text-[15px] font-medium tracking-[.01em] text-[#1f1f1f] shadow-sm transition hover:bg-[#f8fafd] active:bg-[#f1f3f4]"
    type="button"
    onClick={onClick}
    aria-label="Sign in with Google"
  >
    <svg aria-hidden="true" viewBox="0 0 18 18" className="size-[18px]">
      <path
        fill="#EA4335"
        d="M9 3.48c1.69 0 2.86.73 3.52 1.34l2.58-2.52C13.52.86 11.47 0 9 0 5.48 0 2.44 2.02.96 4.97l3.01 2.34C4.68 5.3 6.63 3.48 9 3.48Z"
      />
      <path
        fill="#4285F4"
        d="M17.64 9.2c0-.64-.06-1.25-.16-1.84H9v3.48h4.84a4.14 4.14 0 0 1-1.8 2.72l2.78 2.16c1.63-1.5 2.82-3.71 2.82-6.52Z"
      />
      <path
        fill="#FBBC05"
        d="M3.97 10.69A5.42 5.42 0 0 1 3.68 9c0-.59.1-1.16.29-1.69L.96 4.97A9 9 0 0 0 0 9c0 1.45.35 2.82.96 4.03l3.01-2.34Z"
      />
      <path
        fill="#34A853"
        d="M9 18c2.43 0 4.47-.8 5.96-2.28l-2.78-2.16c-.77.52-1.76.83-3.18.83-2.37 0-4.32-1.82-5.03-4.27L.96 12.46C2.44 15.98 5.48 18 9 18Z"
      />
    </svg>
    Google로 로그인
  </button>
);

const Login = ({ onLogin }) => (
  <PhonePage>
    <main
      className="relative flex h-full flex-col bg-white px-7 pb-9 pt-12 text-[#353b43]"
      aria-label="로그인"
    >
      <button
        className="absolute left-5 top-5 text-[#353b43]"
        aria-label="뒤로 가기"
        type="button"
      >
        <ChevronLeft size={30} strokeWidth={2} />
      </button>
      <section className="flex flex-1 flex-col items-center justify-between py-[18%]">
        <div className="text-center">
          <h1 className="m-0 text-[27px] font-bold tracking-[-.07em]">
            우리WON인증서
          </h1>
          <p className="mb-0 mt-3 text-[19px] font-medium text-[#737a83]">
            로그인하고 WONNIE를 시작해 보세요.
          </p>
        </div>
        <img
          className="h-44 w-44 object-contain"
          src={wonnieMascot}
          alt="WONNIE 마스코트"
        />
        <div className="w-full">
          <GoogleButton onClick={onLogin} />
        </div>
      </section>
    </main>
  </PhonePage>
);

export const AuthGate = ({ children }) => {
  const [state, setState] = useState("splash");
  const [user, setUser] = useState(null);

  const check = () => {
    setState("loading");
    fetch("/api/auth/me", { credentials: "include" })
      .then((response) => {
        if (!response.ok) throw new Error(`auth-${response.status}`);
        return response.json();
      })
      .then((nextUser) => {
        setUser(nextUser);
        setState("signed-in");
      })
      .catch((error) => {
        setState(
          /401|403|unauthorized/i.test(error.message) ? "signed-out" : "error",
        );
      });
  };

  useEffect(() => {
    if (HOLD_SPLASH_FOR_DESIGN) return undefined;
    const timer = window.setTimeout(check, 1000);
    return () => window.clearTimeout(timer);
  }, []);

  const login = () => {
    const url =
      window.location.hostname === "wonnie.minngyuseong.com"
        ? "https://api.wonnie.minngyuseong.com/oauth2/authorization/google"
        : "/oauth2/authorization/google";
    window.location.assign(url);
  };

  if (state === "splash" || state === "loading") return <Splash />;
  if (state === "signed-out" || state === "error")
    return <Login onLogin={login} />;
  return user ? children(user) : <Splash />;
};
