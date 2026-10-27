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
  Promise.all([import('./App'), import('./styles/app.css')]).then(([{ App }]) => {
    root.render(
      <StrictMode>
        <QueryClientProvider client={queryClient}>
          <AuthGate>{() => <App />}</AuthGate>
        </QueryClientProvider>
      </StrictMode>,
    )
  })
}
