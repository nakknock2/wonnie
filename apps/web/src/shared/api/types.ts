export type SubscriptionStatus = 'ACTIVE' | 'CANCELLED'

export interface SubscriptionSummary {
  id: string
  name: string
  category: string
  amount: number
  billingCycle: string
  nextPaymentDate: string
  status: SubscriptionStatus
  brandColor: string
  initial: string
}

export interface CalendarSignal {
  id: string
  title: string
  startsOn: string
  endsOn: string
  signalType: string
  confidence: number
}

export interface DashboardResponse {
  customerName: string
  totalMonthlyCost: number
  activeCount: number
  nextPaymentName: string
  nextPaymentDate: string | null
  subscriptions: SubscriptionSummary[]
  calendarSignals: CalendarSignal[]
}

export interface AssistantMessage {
  id: string
  role: 'USER' | 'ASSISTANT'
  source: 'USER_INPUT' | 'ASSISTANT_REPLY' | 'PRICE_INCREASE' | 'FINANCE_NOTICE'
  content: string
  createdAt: string
}

export interface AssistantConversation {
  conversationId: string
  messages: AssistantMessage[]
  unreadCount: number
}

export interface AssistantMessageExchange {
  userMessage: AssistantMessage
  assistantMessage: AssistantMessage
}

export interface UnreadCount {
  unreadCount: number
}

export interface AuthUser {
  id: string
  name: string
  email: string
  picture?: string | null
}
