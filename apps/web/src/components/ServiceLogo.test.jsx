import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import ServiceLogo from './ServiceLogo.jsx';

describe('ServiceLogo', () => {
  it('renders a normalized Spotify mark instead of its initial', () => {
    const { container } = render(<ServiceLogo service={{ name: '스포티파이', letter: 'S' }} size={68} />);
    const logo = container.querySelector('.service-logo');
    const svg = container.querySelector('svg');

    expect(logo).toHaveStyle({ width: '68px', height: '68px', background: '#1ED760' });
    expect(svg).toHaveStyle({ padding: '20%' });
    expect(svg?.querySelector('path')).toHaveAttribute('fill', '#fff');
    expect(screen.queryByText('S')).not.toBeInTheDocument();
  });

  it('keeps the NAVER mark inside a larger safe area', () => {
    const { container } = render(<ServiceLogo service={{ name: '네이버플러스 멤버십' }} />);
    expect(container.querySelector('svg')).toHaveStyle({ padding: '23%' });
  });

  it('retains the initial fallback for an unknown service', () => {
    const { container } = render(<ServiceLogo service={{ name: '동네 헬스장', letter: 'H' }} />);
    expect(container.querySelector('svg, img')).toBeNull();
    expect(screen.getByText('H')).toBeInTheDocument();
  });

  it('shows the initial only after a local brand asset fails to load', () => {
    const { container } = render(<ServiceLogo service={{ name: '왓챠', letter: 'W' }} />);
    expect(screen.queryByText('W')).not.toBeInTheDocument();

    fireEvent.error(container.querySelector('img'));

    expect(container.querySelector('img')).toBeNull();
    expect(screen.getByText('W')).toBeInTheDocument();
  });
});
