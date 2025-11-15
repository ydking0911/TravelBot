import { MapPin, Star, UtensilsCrossed, Map } from 'lucide-react';
import { Button } from './ui/button';
import { Badge } from './ui/badge';
import { ImageWithFallback } from './figma/ImageWithFallback';

interface RestaurantData {
  id: string;
  name: string;
  address: string;
  cuisine: string;
  priceRange: string;
  rating: number;
  image?: string;
}

interface RestaurantCardProps {
  data: RestaurantData;
}

export function RestaurantCard({ data }: RestaurantCardProps) {
  return (
    <div className="bg-gradient-to-br from-white to-orange-50 rounded-xl border border-orange-200 overflow-hidden hover:shadow-lg transition-shadow">
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
              <h4 className="text-gray-900 mb-1">{data.name}</h4>
              <div className="flex items-center gap-2 mb-2">
                <Badge variant="outline" className="bg-orange-100 text-orange-800 border-orange-200">
                  <UtensilsCrossed className="size-3 mr-1" />
                  {data.cuisine}
                </Badge>
                <span className="text-sm text-gray-600">가격대 {data.priceRange}</span>
              </div>
            </div>
            <Badge className="bg-orange-100 text-orange-800 border-orange-200">
              ⭐ {data.rating}점
            </Badge>
          </div>

          <div className="flex items-start gap-2 text-gray-600 mb-3">
            <MapPin className="size-4 mt-0.5 flex-shrink-0" />
            <span className="text-sm">{data.address}</span>
          </div>

          <div className="flex items-center justify-between">
            <div className="flex items-center gap-1">
              {Array.from({ length: 5 }).map((_, i) => (
                <Star
                  key={i}
                  className={`size-4 ${
                    i < Math.floor(data.rating)
                      ? 'fill-yellow-400 text-yellow-400'
                      : 'text-gray-300'
                  }`}
                />
              ))}
            </div>
            <Button
              size="sm"
              className="bg-gradient-to-r from-orange-500 to-red-600 hover:from-orange-600 hover:to-red-700 text-white"
            >
              <Map className="size-4 mr-1" />
              지도에서 보기
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
