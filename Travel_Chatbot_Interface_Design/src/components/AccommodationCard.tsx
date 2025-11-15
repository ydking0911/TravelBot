import { MapPin, Star, Calendar, Users, ExternalLink } from 'lucide-react';
import { Button } from './ui/button';
import { Badge } from './ui/badge';
import { ImageWithFallback } from './figma/ImageWithFallback';

interface AccommodationData {
  id: string;
  name: string;
  address: string;
  price: number;
  currency: string;
  rating: number;
  stars: number;
  image?: string;
  checkIn?: string;
  checkOut?: string;
  guests?: number;
}

interface AccommodationCardProps {
  data: AccommodationData;
}

export function AccommodationCard({ data }: AccommodationCardProps) {
  return (
    <div className="bg-gradient-to-br from-white to-gray-50 rounded-xl border border-gray-200 overflow-hidden hover:shadow-lg transition-shadow">
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
              <div className="flex items-center gap-1 mb-2">
                {Array.from({ length: data.stars }).map((_, i) => (
                  <Star key={i} className="size-4 fill-yellow-400 text-yellow-400" />
                ))}
              </div>
            </div>
            <Badge className="bg-blue-100 text-blue-800 border-blue-200">
              ⭐ {data.rating}점
            </Badge>
          </div>

          <div className="flex items-start gap-2 text-gray-600 mb-3">
            <MapPin className="size-4 mt-0.5 flex-shrink-0" />
            <span className="text-sm">{data.address}</span>
          </div>

          {(data.checkIn || data.checkOut || data.guests) && (
            <div className="flex flex-wrap gap-3 mb-3 text-sm text-gray-600">
              {data.checkIn && data.checkOut && (
                <div className="flex items-center gap-1">
                  <Calendar className="size-4" />
                  <span>{new Date(data.checkIn).toLocaleDateString()} - {new Date(data.checkOut).toLocaleDateString()}</span>
                </div>
              )}
              {data.guests && (
                <div className="flex items-center gap-1">
                  <Users className="size-4" />
                  <span>{data.guests}명</span>
                </div>
              )}
            </div>
          )}

          <div className="flex items-center justify-between">
            <div>
              <span className="text-gray-900">{data.currency} {data.price}</span>
              <span className="text-gray-500 text-sm"> / 1박</span>
            </div>
            <Button
              size="sm"
              className="bg-gradient-to-r from-blue-500 to-purple-600 hover:from-blue-600 hover:to-purple-700 text-white"
            >
              <ExternalLink className="size-4 mr-1" />
              자세히 보기
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
