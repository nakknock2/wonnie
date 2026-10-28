import { resolveServiceLogo } from '../services/serviceLogos.js';
import './ServiceLogo.css';

export default function ServiceLogo({ service, size = 36, className = '' }) {
  const logo = service?.logo || resolveServiceLogo(service);
  const fallback = service?.letter || service?.initial || service?.name?.slice(0, 1) || service?.serviceName?.slice(0, 1) || 'S';

  return (
    <span
      className={`service-logo ${className}`.trim()}
      style={{ width: size, height: size, background: logo?.background || service?.color || service?.brandColor || '#2789ec' }}
      aria-hidden="true"
    >
      <span className="service-logo__fallback">{fallback}</span>
      {logo?.icon && (
        <svg className="service-logo__image" viewBox="0 0 24 24" style={{ padding: logo.padding }}>
          <path d={logo.icon.path} fill={logo.color || `#${logo.icon.hex}`} />
        </svg>
      )}
      {logo?.src && (
        <img className="service-logo__image" src={logo.src} alt="" style={{ padding: logo.padding }} onError={(event) => event.currentTarget.remove()} />
      )}
    </span>
  );
}
