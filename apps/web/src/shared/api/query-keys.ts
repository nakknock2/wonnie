export const dashboardKeys = {
  all: ['dashboard'] as const,
  detail: () => [...dashboardKeys.all, 'detail'] as const,
}

export const assistantKeys = {
  all: ['assistant'] as const,
  conversation: () => [...assistantKeys.all, 'conversation'] as const,
  unread: () => [...assistantKeys.all, 'unread'] as const,
}
