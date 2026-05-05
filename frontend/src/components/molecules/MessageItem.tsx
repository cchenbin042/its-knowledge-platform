import clsx from 'clsx';
import { User, Bot, Copy, Check } from 'lucide-react';
import { useState, useCallback } from 'react';

export interface Message {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
  sources?: Array<{
    title: string;
    content: string;
    score: number;
  }>;
  feedback?: 'positive' | 'negative' | null;
}

interface MessageItemProps {
  message: Message;
  isStreaming?: boolean;
}

export function MessageItem({ message, isStreaming = false }: MessageItemProps) {
  const [copied, setCopied] = useState(false);
  const isUser = message.role === 'user';

  const handleCopy = useCallback(async () => {
    try {
      await navigator.clipboard.writeText(message.content);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      console.error('Failed to copy text');
    }
  }, [message.content]);

  const formatTime = (date: Date) => {
    return date.toLocaleTimeString('zh-CN', {
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  return (
    <div
      className={clsx(
        'flex gap-3',
        isUser ? 'flex-row-reverse' : 'flex-row'
      )}
    >
      {/* Avatar */}
      <div
        className={clsx(
          'shrink-0 w-8 h-8 rounded-full flex items-center justify-center',
          isUser
            ? 'bg-primary-600 text-white'
            : 'bg-white border border-slate-200 text-slate-600'
        )}
      >
        {isUser ? (
          <User className="w-4 h-4" />
        ) : (
          <Bot className="w-4 h-4" />
        )}
      </div>

      {/* Message content */}
      <div
        className={clsx(
          'flex-1 max-w-[80%]',
          isUser ? 'flex flex-col items-end' : ''
        )}
      >
        <div
          className={clsx(
            'rounded-2xl px-4 py-3 shadow-sm',
            isUser
              ? 'bg-primary-600 text-white rounded-br-md'
              : 'bg-white border border-slate-100 rounded-bl-md'
          )}
        >
          <p
            className={clsx(
              'text-sm leading-relaxed whitespace-pre-wrap',
              isUser ? 'text-white' : 'text-slate-700'
            )}
          >
            {message.content}
            {isStreaming && (
              <span className="inline-block w-2 h-4 ml-1 bg-primary-600 animate-pulse rounded-sm" />
            )}
          </p>
        </div>

        {/* Meta info */}
        <div
          className={clsx(
            'flex items-center gap-2 mt-1 text-xs text-slate-400',
            isUser ? 'flex-row-reverse' : 'flex-row'
          )}
        >
          <span>{formatTime(message.timestamp)}</span>
          {!isUser && (
            <button
              onClick={handleCopy}
              className="hover:text-slate-600 transition-colors"
              title="Copy"
            >
              {copied ? (
                <Check className="w-3.5 h-3.5" />
              ) : (
                <Copy className="w-3.5 h-3.5" />
              )}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}