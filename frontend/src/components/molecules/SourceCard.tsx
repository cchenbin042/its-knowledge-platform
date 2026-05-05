import { Badge, Card } from '../atoms';
import { Database, Hash } from 'lucide-react';
import { useTranslation } from '../../stores';
import type { RetrievalResult } from '../../types';
import clsx from 'clsx';

interface SourceCardProps {
  source: RetrievalResult;
  index: number;
  expanded?: boolean;
  onToggle?: () => void;
}

export function SourceCard({ source, index, expanded = false, onToggle }: SourceCardProps) {
  const { t } = useTranslation();

  const truncateContent = (content: string, maxLength: number = 150) => {
    if (content.length <= maxLength) return content;
    return content.slice(0, maxLength) + '...';
  };

  const formatScore = (score: number) => {
    return (score * 100).toFixed(1) + '%';
  };

  return (
    <Card
      className={clsx(
        'cursor-pointer transition-all duration-200 hover:border-primary-300',
        expanded && 'ring-2 ring-primary-500'
      )}
    >
      <div className="p-4" onClick={onToggle}>
        <div className="flex items-start justify-between gap-2 mb-2">
          <div className="flex items-center gap-2 min-w-0">
            <span className="shrink-0 w-6 h-6 rounded-full bg-primary-100 text-primary-700 text-sm font-medium flex items-center justify-center">
              {index + 1}
            </span>
            <span className="text-sm font-medium text-slate-700 truncate">
              {source.title || t('source.untitled')}
            </span>
          </div>
          <div className="flex items-center gap-2 shrink-0">
            <Badge variant={source.source === 'vector' ? 'primary' : 'success'}>
              {source.source === 'vector' ? (
                <Hash className="w-3 h-3 mr-1" />
              ) : (
                <Database className="w-3 h-3 mr-1" />
              )}
              {source.source}
            </Badge>
          </div>
        </div>

        <div className="flex items-center gap-2 text-xs text-slate-500 mb-2">
          <span>{t('source.relevance')}: {formatScore(source.score)}</span>
          {source.documentId && (
            <>
              <span>|</span>
              <span className="truncate">ID: {source.documentId.slice(0, 8)}...</span>
            </>
          )}
        </div>

        <p className={clsx(
          'text-sm text-slate-600 leading-relaxed',
          !expanded && 'line-clamp-3'
        )}>
          {expanded ? source.content : truncateContent(source.content)}
        </p>

        {expanded && (
          <div className="mt-3 pt-3 border-t border-slate-100">
            <p className="text-xs text-slate-500">{t('source.fullContent')}</p>
            <p className="mt-1 text-sm text-slate-700 whitespace-pre-wrap">
              {source.content}
            </p>
          </div>
        )}
      </div>
    </Card>
  );
}