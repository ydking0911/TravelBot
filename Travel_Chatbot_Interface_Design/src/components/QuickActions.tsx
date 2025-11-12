import { Hotel, UtensilsCrossed, MapPin, DollarSign } from 'lucide-react';
import { Button } from './ui/button';

interface QuickActionsProps {
  onAction: (action: string) => void;
}

export function QuickActions({ onAction }: QuickActionsProps) {
  const actions = [
    {
      icon: Hotel,
      label: '숙소 추천받기',
      query: '서울에서 추천 호텔 알려줘',
      color: 'from-blue-500 to-cyan-500'
    },
    {
      icon: UtensilsCrossed,
      label: '맛집 찾기',
      query: '부산에서 인기 있는 맛집 추천해줘',
      color: 'from-orange-500 to-red-500'
    },
    {
      icon: MapPin,
      label: '관광지 탐색하기',
      query: '제주도에서 가볼 만한 관광지 알려줘',
      color: 'from-green-500 to-emerald-500'
    },
    {
      icon: DollarSign,
      label: '환율 변환',
      query: '100달러를 원화로 환전하면 얼마야?',
      color: 'from-purple-500 to-pink-500'
    }
  ];

  return (
    <div className="max-w-3xl mx-auto">
      <div className="bg-white rounded-2xl shadow-lg p-6 border border-gray-200">
        <h3 className="text-gray-900 mb-4">빠른 요청</h3>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          {actions.map((action, index) => {
            const Icon = action.icon;
            return (
              <Button
                key={index}
                onClick={() => onAction(action.query)}
                variant="outline"
                className="h-auto p-4 flex items-center gap-3 hover:shadow-md transition-all group"
              >
                <div className={`bg-gradient-to-br ${action.color} p-3 rounded-lg group-hover:scale-110 transition-transform`}>
                  <Icon className="size-5 text-white" />
                </div>
                <span className="text-gray-900">{action.label}</span>
              </Button>
            );
          })}
        </div>
      </div>
    </div>
  );
}
