import { Message } from '../App';
import { Copy, Trash2, Check } from 'lucide-react';
import { Button } from './ui/button';
import { useState } from 'react';
import ReactMarkdown from 'react-markdown';
import { AccommodationCard } from './AccommodationCard';
import { RestaurantCard } from './RestaurantCard';
import { PlaceCard } from './PlaceCard';
import { CurrencyConversion } from './CurrencyConversion';

interface ChatMessageProps {
  message: Message;
  onDelete: (id: string) => void;
}

export function ChatMessage({ message, onDelete }: ChatMessageProps) {
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    await navigator.clipboard.writeText(message.content);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const isUser = message.type === 'user';

  return (
    <div className={`flex ${isUser ? 'justify-end' : 'justify-start'} max-w-3xl mx-auto`}>
      <div className={`group relative max-w-[85%] ${isUser ? 'order-2' : 'order-1'}`}>
        <div
          className={`rounded-2xl px-4 py-3 ${
            isUser
              ? 'bg-gradient-to-r from-blue-500 to-purple-600 text-white'
              : 'bg-white border border-gray-200 text-gray-900'
          } shadow-sm`}
        >
          <div className="prose prose-sm max-w-none">
            <ReactMarkdown
              components={{
                p: ({ children }) => <p className={`mb-2 last:mb-0 ${isUser ? 'text-white' : 'text-gray-900'}`}>{children}</p>,
                strong: ({ children }) => <strong className={isUser ? 'text-white' : 'text-gray-900'}>{children}</strong>,
                em: ({ children }) => <em className={isUser ? 'text-white' : 'text-gray-900'}>{children}</em>,
                a: ({ children, href }) => (
                  <a
                    href={href}
                    target="_blank"
                    rel="noopener noreferrer"
                    className={`underline ${isUser ? 'text-white hover:text-gray-100' : 'text-blue-600 hover:text-blue-700'}`}
                  >
                    {children}
                  </a>
                ),
                ul: ({ children }) => <ul className={`list-disc pl-4 mb-2 ${isUser ? 'text-white' : 'text-gray-900'}`}>{children}</ul>,
                ol: ({ children }) => <ol className={`list-decimal pl-4 mb-2 ${isUser ? 'text-white' : 'text-gray-900'}`}>{children}</ol>,
                li: ({ children }) => <li className={`mb-1 ${isUser ? 'text-white' : 'text-gray-900'}`}>{children}</li>,
              }}
            >
              {message.content}
            </ReactMarkdown>
          </div>

          {/* Render special data types */}
          {message.data && (
            <div className="mt-4 space-y-3">
              {message.data.type === 'accommodations' && (
                <div className="space-y-3">
                  {message.data.results.map((accommodation: any) => (
                    <AccommodationCard key={accommodation.id} data={accommodation} />
                  ))}
                </div>
              )}

              {message.data.type === 'restaurants' && (
                <div className="space-y-3">
                  {message.data.results.map((restaurant: any) => (
                    <RestaurantCard key={restaurant.id} data={restaurant} />
                  ))}
                </div>
              )}

              {message.data.type === 'places' && (
                <div className="space-y-3">
                  {message.data.results.map((place: any) => (
                    <PlaceCard key={place.id} data={place} />
                  ))}
                </div>
              )}

              {message.data.type === 'currency' && (
                <CurrencyConversion data={message.data} />
              )}
            </div>
          )}

          <div className="flex items-center gap-2 mt-2">
            <span className={`text-xs ${isUser ? 'text-white/70' : 'text-gray-500'}`}>
              {message.timestamp.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
            </span>
          </div>
        </div>

        {/* Action buttons */}
        <div className={`absolute top-0 ${isUser ? 'left-0 -translate-x-full' : 'right-0 translate-x-full'} opacity-0 group-hover:opacity-100 transition-opacity flex gap-1 px-2`}>
          <Button
            variant="ghost"
            size="sm"
            onClick={handleCopy}
            className="size-8 p-0"
          >
            {copied ? <Check className="size-4 text-green-600" /> : <Copy className="size-4" />}
          </Button>
          {message.id !== 'welcome' && (
            <Button
              variant="ghost"
              size="sm"
              onClick={() => onDelete(message.id)}
              className="size-8 p-0"
            >
              <Trash2 className="size-4 text-red-600" />
            </Button>
          )}
        </div>
      </div>
    </div>
  );
}
