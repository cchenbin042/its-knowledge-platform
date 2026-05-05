import clsx from 'clsx';
import { ThumbsUp, ThumbsDown } from 'lucide-react';
import { useTranslation } from '../../stores';

interface FeedbackBarProps {
  feedback?: 'positive' | 'negative' | null;
  onFeedback?: (type: 'positive' | 'negative') => void;
  disabled?: boolean;
  className?: string;
}

export function FeedbackBar({
  feedback,
  onFeedback,
  disabled = false,
  className,
}: FeedbackBarProps) {
  const { t } = useTranslation();

  return (
    <div className={clsx('flex items-center gap-1', className)}>
      <button
        type="button"
        onClick={() => onFeedback?.('positive')}
        disabled={disabled}
        className={clsx(
          'p-1.5 rounded-lg transition-all',
          disabled
            ? 'opacity-50 cursor-not-allowed'
            : 'hover:bg-green-50 active:scale-95',
          feedback === 'positive'
            ? 'text-green-600 bg-green-50'
            : 'text-slate-400 hover:text-green-600'
        )}
        title={t('feedback.helpful')}
      >
        <ThumbsUp className="w-4 h-4" />
      </button>
      <button
        type="button"
        onClick={() => onFeedback?.('negative')}
        disabled={disabled}
        className={clsx(
          'p-1.5 rounded-lg transition-all',
          disabled
            ? 'opacity-50 cursor-not-allowed'
            : 'hover:bg-red-50 active:scale-95',
          feedback === 'negative'
            ? 'text-red-600 bg-red-50'
            : 'text-slate-400 hover:text-red-600'
        )}
        title={t('feedback.notHelpful')}
      >
        <ThumbsDown className="w-4 h-4" />
      </button>
    </div>
  );
}