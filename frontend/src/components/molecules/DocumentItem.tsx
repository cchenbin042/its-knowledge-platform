import { FileText, Trash2, Clock, Hash } from 'lucide-react';
import { Card, Button } from '../atoms';
import { useTranslation } from '../../stores';
import type { Document } from '../../types';

interface DocumentItemProps {
  document: Document;
  onDelete: (title: string) => void;
  deleting?: boolean;
}

export function DocumentItem({ document, onDelete, deleting = false }: DocumentItemProps) {
  const { t } = useTranslation();

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  return (
    <Card className="hover:border-primary-300 transition-colors">
      <div className="p-4">
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-start gap-3 min-w-0 flex-1">
            <div className="shrink-0 w-10 h-10 rounded-lg bg-primary-50 flex items-center justify-center">
              <FileText className="w-5 h-5 text-primary-600" />
            </div>
            <div className="min-w-0 flex-1">
              <h3 className="font-medium text-slate-900 truncate">{document.title}</h3>
              <div className="flex items-center gap-3 mt-1 text-sm text-slate-500">
                <span className="flex items-center gap-1">
                  <Hash className="w-3.5 h-3.5" />
                  {document.chunkCount} {t('documents.chunks')}
                </span>
                <span className="flex items-center gap-1">
                  <Clock className="w-3.5 h-3.5" />
                  {formatDate(document.createdAt)}
                </span>
              </div>
            </div>
          </div>
          <Button
            variant="ghost"
            size="sm"
            onClick={() => onDelete(document.title)}
            disabled={deleting}
            className="shrink-0 text-slate-400 hover:text-red-600 hover:bg-red-50"
          >
            <Trash2 className="w-4 h-4" />
          </Button>
        </div>
      </div>
    </Card>
  );
}