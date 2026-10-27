import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { AuthGate } from './features/auth/AuthGate.jsx';

const root = createRoot(document.getElementById('root'));

if (window.location.hostname.startsWith('demo.')) {
  Promise.all([import('./App.jsx'), import('./styles/global.css')]).then(([{ default: DemoApp }]) => {
    root.render(<StrictMode><DemoApp showDemoPanel /></StrictMode>);
  });
} else {
  Promise.all([import('./App.jsx'), import('./styles/global.css')]).then(([{ default: ServiceApp }]) => {
    root.render(
      <StrictMode>
        <AuthGate>{(user) => <ServiceApp showDemoPanel={false} user={user} />}</AuthGate>
      </StrictMode>,
    );
  });
}
