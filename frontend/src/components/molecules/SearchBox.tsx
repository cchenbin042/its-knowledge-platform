import { useRef, useEffect } from 'react';
import { Send, Loader2 } from 'lucide-react';
import { Button } from '../atoms';
import clsx from 'clsx';

interface SearchBoxProps {
  value: string;
  onChange: (value: string) => void;
  onSubmit: () => void;
  loading?: boolean;
  placeholder?: string;
  disabled?: boolean;
}

export function SearchBox({
  value,
  onChange,
  onSubmit,
  loading = false,
  placeholder = 'Enter your question...',
  disabled = false,
}: SearchBoxProps) {
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  // Auto-resize textarea
  useEffect(() => {
    const textarea = textareaRef.current;
    if (textarea) {
      textarea.style.height = 'auto';
      textarea.style.height = `${Math.min(textarea.scrollHeight, 200)}px`;
    }
  }, [value]);

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      if (value.trim() && !loading && !disabled) {
        onSubmit();
      }
    }
  };

  return (
    <div className="relative flex items-end gap-2 bg-white border border-slate-200 rounded-xl p-3 shadow-sm focus-within:ring-2 focus-within:ring-primary-500 focus-within:border-transparent">
      <textarea
        ref={textareaRef}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        onKeyDown={handleKeyDown}
        placeholder={placeholder}
        disabled={disabled || loading}
        rows={1}
        className={clsx(
          'flex-1 resize-none border-none outline-none text-slate-900 placeholder-slate-400',
          'disabled:bg-transparent disabled:text-slate-500'
        )}
      />
      <Button
        onClick={onSubmit}
        disabled={!value.trim() || loading || disabled}
        loading={loading}
        className="shrink-0"
      >
        {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
      </Button>
    </div>
  );
}