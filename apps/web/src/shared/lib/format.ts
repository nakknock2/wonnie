const wonFormatter = new Intl.NumberFormat('ko-KR')

export const formatWon = (amount: number) => `${wonFormatter.format(amount)}원`

export const formatShortDate = (value: string) => {
  const date = new Date(`${value}T00:00:00`)
  return new Intl.DateTimeFormat('ko-KR', {
    month: 'short',
    day: 'numeric',
  }).format(date)
}

export const formatDateRange = (startsOn: string, endsOn: string) =>
  `${formatShortDate(startsOn)} – ${formatShortDate(endsOn)}`
