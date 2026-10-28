import { describe, expect, it } from 'vitest';
import { resolveServiceLogo } from './serviceLogos.js';

describe('resolveServiceLogo', () => {
  it.each([
    [{ id: 'sub-watcha', name: '왓챠' }, 'watcha'],
    [{ id: 'sub-netflix', name: 'Netflix' }, 'netflix'],
    [{ name: '유튜브 프리미엄' }, 'youtube'],
    [{ name: 'Microsoft 365' }, 'microsoft-365'],
    [{ name: '티빙' }, 'tving'],
    [{ name: '네이버플러스 멤버십' }, 'naver'],
    [{ name: 'iCloud+' }, 'icloud'],
  ])('matches a known subscription service', (service, filename) => {
    expect(resolveServiceLogo(service)).toContain(filename);
  });

  it('leaves an unknown service to the initial fallback', () => {
    expect(resolveServiceLogo({ name: '동네 헬스장' })).toBeUndefined();
  });
});
