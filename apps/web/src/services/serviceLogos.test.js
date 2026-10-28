import { describe, expect, it } from 'vitest';
import { resolveServiceLogo } from './serviceLogos.js';

describe('resolveServiceLogo', () => {
  it.each([
    { id: 'sub-watcha', name: '왓챠' },
    { id: 'sub-netflix', name: 'Netflix' },
    { name: 'Spotify' },
    { name: '유튜브 프리미엄' },
    { name: 'Microsoft 365' },
    { name: '티빙' },
    { name: '네이버플러스 멤버십' },
    { name: 'iCloud+' },
  ])('matches a known subscription service', (service) => {
    const logo = resolveServiceLogo(service);
    expect(logo?.icon || logo?.src).toBeTruthy();
    expect(logo?.background).toMatch(/^#/);
    expect(logo?.padding).toBeTruthy();
  });

  it('leaves an unknown service to the initial fallback', () => {
    expect(resolveServiceLogo({ name: '동네 헬스장' })).toBeUndefined();
  });
});
