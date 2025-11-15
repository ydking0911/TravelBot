import { useState, useRef, useEffect, KeyboardEvent } from 'react';
import { Send } from 'lucide-react';
import { Button } from './ui/button';

interface MessageInputProps {
  onSend: (message: string) => void;
  disabled?: boolean;
}

export function MessageInput({ onSend, disabled }: MessageInputProps) {
  const [input, setInput] = useState('');
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    // Auto-focus on mount
    textareaRef.current?.focus();
  }, []);

  const handleSubmit = () => {
    if (input.trim() && !disabled) {
      onSend(input);
      setInput('');
      // Reset textarea height
      if (textareaRef.current) {
        textareaRef.current.style.height = 'auto';
      }
    }
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit();
    }
  };

  const handleInput = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setInput(e.target.value);
    // Auto-resize textarea
    e.target.style.height = 'auto';
    e.target.style.height = Math.min(e.target.scrollHeight, 150) + 'px';
  };

  return (
    <div className="sticky bottom-0 bg-white border-t border-gray-200 shadow-lg">
      <div className="max-w-3xl mx-auto px-4 py-4">
        <div className="flex gap-2 items-end">
          <div className="flex-1 relative">
            <textarea
              ref={textareaRef}
              value={input}
              onChange={handleInput}
              onKeyDown={handleKeyDown}
              placeholder="메시지를 입력하세요... (줄바꿈은 Shift+Enter)"
              disabled={disabled}
              rows={1}
              className="w-full resize-none rounded-xl border border-gray-300 px-4 py-3 pr-12 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:bg-gray-100 disabled:cursor-not-allowed transition-all"
              style={{ maxHeight: '150px' }}
            />
            <div className="absolute bottom-3 right-3 text-xs text-gray-400">
              {input.length > 0 && `${input.length}자`}
            </div>
          </div>
          <Button
            onClick={handleSubmit}
            disabled={!input.trim() || disabled}
            className="bg-gradient-to-r from-blue-500 to-purple-600 hover:from-blue-600 hover:to-purple-700 text-white rounded-xl px-6 py-3 h-auto disabled:opacity-50 disabled:cursor-not-allowed transition-all"
          >
            <Send className="size-5" />
            <span className="sr-only">전송</span>
          </Button>
        </div>
        <p className="text-xs text-gray-500 mt-2 text-center">
          Enter 키로 전송, Shift+Enter 줄바꿈
        </p>
      </div>
    </div>
  );
}
