export const dashboardKeys = {
  all: ['dashboard'] as const,
  detail: () => [...dashboardKeys.all, 'detail'] as const,
}

export const assistantKeys = {
  all: ['assistant'] as const,
  suggestions: () => [...assistantKeys.all, 'suggestions'] as const,
}
