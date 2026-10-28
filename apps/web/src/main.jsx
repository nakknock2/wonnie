import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { AuthGate } from './features/auth/AuthGate.jsx';

const root = createRoot(document.getElementById('root'));

Promise.all([import('./App.jsx'), import('./styles/global.css')]).then(([{ default: ServiceApp }]) => {
  root.render(
    <StrictMode>
      <AuthGate>{(user) => <ServiceApp showDemoPanel={false} user={user} />}</AuthGate>
    </StrictMode>,
  );
});
