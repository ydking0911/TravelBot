import { useState, useEffect, useRef } from 'react';
import { ChatHeader } from './components/ChatHeader';
import { ChatMessage } from './components/ChatMessage';
import { MessageInput } from './components/MessageInput';
import { QuickActions } from './components/QuickActions';
import { TypingIndicator } from './components/TypingIndicator';

export interface Message {
  id: string;
  type: 'user' | 'bot';
  content: string;
  timestamp: Date;
  data?: any;
}

const WELCOME_MESSAGE =
  '안녕하세요! 👋 여행 플래너 트래블봇입니다. 숙소, 맛집, 관광지, 환율 변환까지 도와드릴게요. 오늘은 어떤 여행을 계획해볼까요?';

export default function App() {
  const [messages, setMessages] = useState<Message[]>([]);
  const [sessionId, setSessionId] = useState<string>('');
  const [isTyping, setIsTyping] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const [isOnline, setIsOnline] = useState(navigator.onLine);

  // Load session from localStorage on mount
  useEffect(() => {
    const savedSession = localStorage.getItem('chatSession');
    if (savedSession) {
      try {
        const { sessionId: savedSessionId, messages: savedMessages } = JSON.parse(savedSession);
        setSessionId(savedSessionId);
        setMessages(savedMessages.map((msg: any) => ({
          ...msg,
          timestamp: new Date(msg.timestamp)
        })));
      } catch (e) {
        console.error('세션 정보를 불러오지 못했습니다:', e);
      }
    } else {
      // Welcome message
      setMessages([{
        id: 'welcome',
        type: 'bot',
        content: WELCOME_MESSAGE,
        timestamp: new Date()
      }]);
    }
  }, []);

  // Save session to localStorage when it changes
  useEffect(() => {
    if (messages.length > 0) {
      localStorage.setItem('chatSession', JSON.stringify({ sessionId, messages }));
    }
  }, [messages, sessionId]);

  // Auto-scroll to latest message
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, isTyping]);

  // Online/offline detection
  useEffect(() => {
    const handleOnline = () => setIsOnline(true);
    const handleOffline = () => setIsOnline(false);
    
    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);
    
    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  const sendMessage = async (content: string) => {
    if (!content.trim()) return;

    if (!isOnline) {
      setError('인터넷에 연결되어 있지 않습니다. 네트워크 상태를 확인한 뒤 다시 시도해주세요.');
      return;
    }

    const userMessage: Message = {
      id: Date.now().toString(),
      type: 'user',
      content,
      timestamp: new Date()
    };

    setMessages(prev => [...prev, userMessage]);
    setError(null);
    setIsTyping(true);

    try {
      // Real API call
      const response = await apiCall(content, sessionId);
      
      const botMessage: Message = {
        id: (Date.now() + 1).toString(),
        type: 'bot',
        content: response.message,
        timestamp: new Date(),
        data: response.data
      };

      setMessages(prev => [...prev, botMessage]);
      
      // 서버에서 반환한 sessionId를 항상 업데이트 (멀티턴 대화를 위해 필수)
      if (response.sessionId) {
        setSessionId(response.sessionId);
      }
    } catch (err) {
      setError('메시지를 전송하지 못했습니다. 잠시 후 다시 시도해주세요.');
      console.error('메시지 전송 중 오류가 발생했습니다:', err);
    } finally {
      setIsTyping(false);
    }
  };

  const handleQuickAction = (action: string) => {
    sendMessage(action);
  };

  const startNewSession = () => {
    setMessages([{
      id: 'welcome',
      type: 'bot',
      content: WELCOME_MESSAGE,
      timestamp: new Date()
    }]);
    setSessionId('');
    localStorage.removeItem('chatSession');
    setError(null);
  };

  const clearChat = () => {
    if (confirm('대화를 모두 삭제할까요?')) {
      startNewSession();
    }
  };

  const deleteMessage = (id: string) => {
    setMessages(prev => prev.filter(msg => msg.id !== id));
  };

  return (
    <div className="flex flex-col h-screen bg-gradient-to-br from-blue-50 via-purple-50 to-pink-50">
      <ChatHeader 
        sessionId={sessionId}
        onNewSession={startNewSession}
        onClearChat={clearChat}
        isOnline={isOnline}
      />

      <div className="flex-1 overflow-y-auto px-4 py-6 space-y-4">
        {!isOnline && (
          <div className="max-w-3xl mx-auto bg-yellow-100 border border-yellow-400 text-yellow-800 px-4 py-3 rounded-lg">
            ⚠️ 현재 오프라인 상태입니다. 일부 기능을 사용할 수 없어요.
          </div>
        )}

        {error && (
          <div className="max-w-3xl mx-auto bg-red-100 border border-red-400 text-red-800 px-4 py-3 rounded-lg flex justify-between items-center">
            <span>❌ {error}</span>
            <button 
              onClick={() => setError(null)}
              className="text-red-800 hover:text-red-900"
            >
              ✕
            </button>
          </div>
        )}

        {messages.length === 1 && messages[0].id === 'welcome' && (
          <QuickActions onAction={handleQuickAction} />
        )}

        {messages.map((message) => (
          <ChatMessage 
            key={message.id} 
            message={message}
            onDelete={deleteMessage}
          />
        ))}

        {isTyping && <TypingIndicator />}
        
        <div ref={messagesEndRef} />
      </div>

      <MessageInput onSend={sendMessage} disabled={isTyping || !isOnline} />
    </div>
  );
}

// Real API call function
async function apiCall(message: string, sessionId: string): Promise<any> {
  try {
    const response = await fetch('/api/chat', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        message: message,
        sessionId: sessionId || undefined
      })
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const data = await response.json();
    
    // 서버에서 반환한 sessionId를 우선 사용 (없으면 기존 sessionId 유지)
    return {
      sessionId: data.sessionId || sessionId,
      message: data.message || '죄송합니다. 요청을 처리하지 못했어요.',
      success: data.success !== false,
      data: data.data
    };
  } catch (error) {
    console.error('API 호출에 실패했습니다:', error);
    throw error;
  }
}
