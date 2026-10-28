import watchaLogo from '../assets/service-logos/watcha.svg';
import netflixLogo from '../assets/service-logos/netflix.svg';
import youtubeLogo from '../assets/service-logos/youtube.svg';
import microsoft365Logo from '../assets/service-logos/microsoft-365.svg';
import tvingLogo from '../assets/service-logos/tving.svg';
import naverLogo from '../assets/service-logos/naver.svg';
import icloudLogo from '../assets/service-logos/icloud.svg';

const SERVICE_LOGOS = [
  { matches: ['watcha', '왓챠'], src: watchaLogo },
  { matches: ['netflix', '넷플릭스'], src: netflixLogo },
  { matches: ['youtube', '유튜브'], src: youtubeLogo },
  { matches: ['microsoft365', 'microsoft', 'ms365', '마이크로소프트'], src: microsoft365Logo },
  { matches: ['tving', '티빙'], src: tvingLogo },
  { matches: ['naver', '네이버'], src: naverLogo },
  { matches: ['icloud', '아이클라우드'], src: icloudLogo },
];

const normalize = (value) => String(value || '').toLowerCase().replace(/[^a-z0-9가-힣]/g, '');

export function resolveServiceLogo(service) {
  const identity = normalize([service?.id, service?.name, service?.serviceName].filter(Boolean).join(' '));
  return SERVICE_LOGOS.find(({ matches }) => matches.some((candidate) => identity.includes(candidate)))?.src;
}
