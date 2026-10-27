import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { AuthGate } from './features/auth/AuthGate'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
})

const root = createRoot(document.getElementById('root')!)

if (window.location.hostname.startsWith('demo.')) {
  Promise.all([import('./App.jsx'), import('./styles/global.css')]).then(([{ default: DemoApp }]) => {
    root.render(<StrictMode><DemoApp /></StrictMode>)
  })
} else {
  // Explicitly select the authenticated service app. There is also a legacy
  // App.jsx used by demo.wonnie.minngyuseong.com, and Vite resolves the
  // extensionless import to that file first.
  Promise.all([import('./App.tsx'), import('./styles/app.css')]).then(([{ App }]) => {
    root.render(
      <StrictMode>
        <QueryClientProvider client={queryClient}>
          <AuthGate>{() => <App />}</AuthGate>
        </QueryClientProvider>
      </StrictMode>,
    )
  })
}
