import { useQuery } from '@tanstack/react-query'
import { api } from '../../shared/api/client'
import { dashboardKeys } from '../../shared/api/query-keys'

export const useDashboard = () =>
  useQuery({
    queryKey: dashboardKeys.detail(),
    queryFn: api.getDashboard,
  })
