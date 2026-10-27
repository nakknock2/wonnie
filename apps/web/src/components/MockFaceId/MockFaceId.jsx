import { useEffect, useRef, useState } from 'react';
import './MockFaceId.css';

const DURATION_MS = 2400;

// Visual demo only. Completion does not authenticate a user or approve a payment.
export default function MockFaceId({ open = false, onComplete, onCancel }) {
  const [phase, setPhase] = useState('idle');
  const callbacks = useRef({ onComplete, onCancel });
  callbacks.current = { onComplete, onCancel };

  useEffect(() => {
    if (!open) {
      setPhase('idle');
      return undefined;
    }

    let finished = false;
    setPhase('opening');
    const timers = [
      setTimeout(() => setPhase('scanning'), 320),
      setTimeout(() => setPhase('settling'), 1150),
      setTimeout(() => setPhase('success'), 1650),
      setTimeout(() => setPhase('closing'), 2050),
      setTimeout(() => {
        finished = true;
        setPhase('idle');
        callbacks.current.onComplete?.();
      }, DURATION_MS),
    ];
    const cancelOnEscape = (event) => {
      if (event.key !== 'Escape' || finished) return;
      event.stopImmediatePropagation();
      finished = true;
      timers.forEach(clearTimeout);
      setPhase('idle');
      callbacks.current.onCancel?.();
    };
    window.addEventListener('keydown', cancelOnEscape, true);
    return () => {
      timers.forEach(clearTimeout);
      window.removeEventListener('keydown', cancelOnEscape, true);
    };
  }, [open]);

  const active = open && phase !== 'idle';
  const success = phase === 'success' || phase === 'closing';

  return (
    <div className={`dynamic-island mock-face-id${active ? ' is-active' : ''}`}
      data-phase={active ? phase : 'idle'} style={{ '--face-id-duration': `${DURATION_MS}ms` }}>
      <span className="mock-face-id__status" role="status" aria-label="Face ID 시연 상태"
        aria-live="polite" aria-atomic="true">
        {active && (success ? 'Face ID 데모 인증 완료' : 'Face ID 데모 인증 중')}
      </span>
      <div className="mock-face-id__visual" aria-hidden="true">
        <div className="mock-face-id__orb">
          <svg className="mock-face-id__halo" viewBox="0 0 100 100" fill="none">
            <circle cx="50" cy="50" r="30" />
          </svg>
          <svg className="mock-face-id__strand mock-face-id__strand--one" viewBox="0 0 100 100" fill="none">
            <circle cx="50" cy="50" r="30" />
          </svg>
          <svg className="mock-face-id__strand mock-face-id__strand--two" viewBox="0 0 100 100" fill="none">
            <circle cx="50" cy="50" r="30" />
          </svg>
          <svg className="mock-face-id__strand mock-face-id__strand--three" viewBox="0 0 100 100" fill="none">
            <circle cx="50" cy="50" r="30" />
          </svg>
          {/* The rotating rim itself settles into the success ring; it is never swapped out. */}
          <svg className="mock-face-id__rim" viewBox="0 0 100 100" fill="none">
            <circle cx="50" cy="50" r="30" />
          </svg>
        </div>
        <svg className="mock-face-id__confirmation" viewBox="0 0 100 100" fill="none">
          <circle className="mock-face-id__dot" cx="36" cy="50" r="1.8" />
          <path className="mock-face-id__tick" d="m36 50 10 11 19-25" pathLength="1" />
        </svg>
      </div>
    </div>
  );
}
