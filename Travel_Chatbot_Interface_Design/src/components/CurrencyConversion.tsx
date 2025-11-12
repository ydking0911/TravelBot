import { ArrowRight, TrendingUp, Calendar } from 'lucide-react';

interface CurrencyData {
  from: string;
  to: string;
  amount: number;
  converted: number;
  rate: number;
  timestamp: string;
}

interface CurrencyConversionProps {
  data: CurrencyData;
}

export function CurrencyConversion({ data }: CurrencyConversionProps) {
  return (
    <div className="bg-gradient-to-br from-purple-100 to-pink-100 rounded-xl border border-purple-200 p-6">
      <h4 className="text-gray-900 mb-4 flex items-center gap-2">
        💱 환율 변환
      </h4>

      <div className="flex items-center justify-center gap-4 mb-4">
        <div className="text-center">
          <div className="text-sm text-gray-600 mb-1">{data.from}</div>
          <div className="text-gray-900">{data.amount.toFixed(2)}</div>
        </div>

        <ArrowRight className="size-6 text-purple-600" />

        <div className="text-center">
          <div className="text-sm text-gray-600 mb-1">{data.to}</div>
          <div className="text-gray-900">{data.converted.toFixed(2)}</div>
        </div>
      </div>

      <div className="bg-white/50 rounded-lg p-3 space-y-2">
        <div className="flex items-center justify-between text-sm">
          <span className="text-gray-600 flex items-center gap-1">
            <TrendingUp className="size-4" />
            환율
          </span>
          <span className="text-gray-900">
            1 {data.from} = {data.rate.toFixed(4)} {data.to}
          </span>
        </div>

        <div className="flex items-center justify-between text-sm">
          <span className="text-gray-600 flex items-center gap-1">
            <Calendar className="size-4" />
            기준 시각
          </span>
          <span className="text-gray-900">
            {new Date(data.timestamp).toLocaleString()}
          </span>
        </div>
      </div>
    </div>
  );
}
