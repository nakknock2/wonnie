import { resolveServiceLogo } from '../services/serviceLogos.js';
import './ServiceLogo.css';

export default function ServiceLogo({ service, size = 36, className = '' }) {
  const logoSrc = service?.logoSrc || resolveServiceLogo(service);
  const fallback = service?.letter || service?.initial || service?.name?.slice(0, 1) || service?.serviceName?.slice(0, 1) || 'S';

  return (
    <span
      className={`service-logo ${className}`.trim()}
      style={{ width: size, height: size, background: service?.color || service?.brandColor || '#2789ec' }}
      aria-hidden="true"
    >
      <span className="service-logo__fallback">{fallback}</span>
      {logoSrc && <img className="service-logo__image" src={logoSrc} alt="" onError={(event) => event.currentTarget.remove()} />}
    </span>
  );
}
