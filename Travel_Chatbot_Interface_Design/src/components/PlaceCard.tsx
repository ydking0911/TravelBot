import { MapPin, Star, Tag, DollarSign, Map } from 'lucide-react';
import { Button } from './ui/button';
import { Badge } from './ui/badge';
import { ImageWithFallback } from './figma/ImageWithFallback';

interface PlaceData {
  id: string;
  name: string;
  address: string;
  category: string[];
  fee?: number;
  currency?: string;
  rating: number;
  image?: string;
}

interface PlaceCardProps {
  data: PlaceData;
}

export function PlaceCard({ data }: PlaceCardProps) {
  return (
    <div className="bg-gradient-to-br from-white to-green-50 rounded-xl border border-green-200 overflow-hidden hover:shadow-lg transition-shadow">
      <div className="flex flex-col sm:flex-row">
        {data.image && (
          <div className="sm:w-48 h-48 sm:h-auto flex-shrink-0">
            <ImageWithFallback
              src={data.image}
              alt={data.name}
              className="w-full h-full object-cover"
            />
          </div>
        )}
        <div className="flex-1 p-4">
          <div className="flex items-start justify-between gap-2 mb-2">
            <div>
              <h4 className="text-gray-900 mb-2">{data.name}</h4>
              <div className="flex flex-wrap gap-1.5 mb-2">
                {data.category.map((cat, index) => (
                  <Badge key={index} variant="outline" className="bg-green-100 text-green-800 border-green-200 text-xs">
                    <Tag className="size-3 mr-1" />
                    {cat}
                  </Badge>
                ))}
              </div>
            </div>
            <Badge className="bg-green-100 text-green-800 border-green-200">
              ⭐ {data.rating}점
            </Badge>
          </div>

          <div className="flex items-start gap-2 text-gray-600 mb-3">
            <MapPin className="size-4 mt-0.5 flex-shrink-0" />
            <span className="text-sm">{data.address}</span>
          </div>

          <div className="flex items-center justify-between">
            {data.fee !== undefined && (
              <div className="flex items-center gap-1 text-gray-900">
                <DollarSign className="size-4" />
                <span>{data.currency} {data.fee}</span>
                <span className="text-sm text-gray-500">입장료</span>
              </div>
            )}
            <Button
              size="sm"
              className="bg-gradient-to-r from-green-500 to-emerald-600 hover:from-green-600 hover:to-emerald-700 text-white ml-auto"
            >
              <Map className="size-4 mr-1" />
              길 찾기
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
