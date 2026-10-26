export type SubscriptionStatus =
  | 'ACTIVE'
  | 'PAYMENT_PAUSE_SCHEDULED'
  | 'PAYMENT_PAUSED'
  | 'RESUME_PENDING'
  | 'PAYMENT_FAILED'

export interface WalletSummary {
  balance: number
  autoFundingEnabled: boolean
  walletStatus: string
  virtualCardLast4: string
  virtualCardStatus: string
}

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
  wallet: WalletSummary
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
  pausedCount: number
  nextPaymentName: string
  nextPaymentDate: string
  subscriptions: SubscriptionSummary[]
  calendarSignals: CalendarSignal[]
}

export interface ActionPlan {
  id: string
  subscriptionId: string
  subscriptionName: string
  actionType: string
  startsOn: string
  endsOn: string
  reason: string
  warning: string
  status: string
}

export interface AssistantResponse {
  reply: string
  actionPlan?: ActionPlan
  highlights: string[]
}

export interface AuthChallenge {
  challengeId: string
  challenge: string
  expiresAt: string
}

export interface SagaResult {
  sagaId: string
  status: string
  message: string
  subscriptionStatus: SubscriptionStatus
}
