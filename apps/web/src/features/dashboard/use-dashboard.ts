import { useQuery } from '@tanstack/react-query'
import { api } from '../../shared/api/client'
import { dashboardKeys } from '../../shared/api/query-keys'

export const useDashboard = () =>
  useQuery({
    queryKey: dashboardKeys.detail(),
    queryFn: api.getDashboard,
    // Vite가 API보다 먼저 시작하거나 API가 재시작되어도 대시보드를 자동 복구한다.
    refetchInterval: (query) => query.state.status === 'error' ? 3000 : false,
  })
