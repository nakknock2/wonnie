import { Bell, Menu, Search } from 'lucide-react'

interface AppHeaderProps {
  customerName: string
}

export const AppHeader = ({ customerName }: AppHeaderProps) => (
  <header className="flex items-center justify-between px-5 pb-4 pt-5">
    <div className="flex items-center gap-3">
      <button className="icon-button" aria-label="전체 메뉴">
        <Menu size={20} />
      </button>
      <div>
        <p className="m-0 text-[12px] font-medium text-woori-muted">안녕하세요</p>
        <p className="m-0 mt-0.5 text-[17px] font-bold tracking-[-0.02em]">
          {customerName}님
        </p>
      </div>
    </div>
    <div className="flex gap-2">
      <button className="icon-button" aria-label="검색">
        <Search size={19} />
      </button>
      <button className="icon-button relative" aria-label="알림">
        <Bell size={19} />
        <span className="absolute right-2.5 top-2.5 size-2 rounded-full bg-orange-500 ring-2 ring-white" />
      </button>
    </div>
  </header>
)
