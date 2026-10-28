import watchaLogo from '../assets/service-logos/watcha.svg';
import microsoft365Logo from '../assets/service-logos/microsoft-365.svg';
import tvingLogo from '../assets/service-logos/tving.svg';
import { siIcloud, siNaver, siNetflix, siSpotify, siYoutube } from 'simple-icons';

const SERVICE_LOGOS = [
  { matches: ['watcha', '왓챠'], src: watchaLogo, background: '#000', padding: '0' },
  { matches: ['netflix', '넷플릭스'], icon: siNetflix, background: '#141414', color: '#E50914', padding: '16%' },
  { matches: ['spotify', '스포티파이'], icon: siSpotify, background: '#1ED760', color: '#fff', padding: '20%' },
  { matches: ['youtube', '유튜브'], icon: siYoutube, background: '#fff', color: '#FF0033', padding: '18%' },
  { matches: ['microsoft365', 'microsoft', 'ms365', '마이크로소프트'], src: microsoft365Logo, background: '#fff', padding: '16%' },
  { matches: ['tving', '티빙'], src: tvingLogo, background: '#fff', padding: '12% 7%' },
  { matches: ['naver', '네이버'], icon: siNaver, background: '#03C75A', color: '#fff', padding: '23%' },
  { matches: ['icloud', '아이클라우드'], icon: siIcloud, background: '#fff', color: '#3693F3', padding: '17%' },
];

const normalize = (value) => String(value || '').toLowerCase().replace(/[^a-z0-9가-힣]/g, '');

export function resolveServiceLogo(service) {
  const identity = normalize([service?.id, service?.name, service?.serviceName].filter(Boolean).join(' '));
  return SERVICE_LOGOS.find(({ matches }) => matches.some((candidate) => identity.includes(candidate)));
}
