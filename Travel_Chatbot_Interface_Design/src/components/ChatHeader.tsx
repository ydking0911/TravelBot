import { Plane, RotateCcw, Trash2, Wifi, WifiOff } from 'lucide-react';
import { Button } from './ui/button';
import { Badge } from './ui/badge';

interface ChatHeaderProps {
  sessionId: string;
  onNewSession: () => void;
  onClearChat: () => void;
  isOnline: boolean;
}

export function ChatHeader({ sessionId, onNewSession, onClearChat, isOnline }: ChatHeaderProps) {
  return (
    <header className="sticky top-0 z-10 bg-white/95 backdrop-blur-sm border-b border-gray-200 shadow-sm">
      <div className="max-w-7xl mx-auto px-4 py-4 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="bg-gradient-to-br from-blue-500 to-purple-600 p-2 rounded-lg">
            <Plane className="size-6 text-white" />
          </div>
          <div>
            <h1 className="text-gray-900">트래블봇</h1>
            <div className="flex items-center gap-2">
              {sessionId && (
                <Badge variant="secondary" className="text-xs">
                  세션 진행 중
                </Badge>
              )}
              {isOnline ? (
                <div className="flex items-center gap-1 text-green-600 text-xs">
                  <Wifi className="size-3" />
                  <span>온라인</span>
                </div>
              ) : (
                <div className="flex items-center gap-1 text-red-600 text-xs">
                  <WifiOff className="size-3" />
                  <span>오프라인</span>
                </div>
              )}
            </div>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={onClearChat}
            className="hidden sm:flex gap-2"
          >
            <Trash2 className="size-4" />
            <span>대화 삭제</span>
          </Button>
          <Button
            variant="default"
            size="sm"
            onClick={onNewSession}
            className="flex gap-2 bg-gradient-to-r from-blue-500 to-purple-600 hover:from-blue-600 hover:to-purple-700"
          >
            <RotateCcw className="size-4" />
            <span className="hidden sm:inline">새 대화 시작</span>
            <span className="sm:hidden">새 대화</span>
          </Button>
        </div>
      </div>
    </header>
  );
}
