import { DocumentItem } from '../molecules';
import { Spinner } from '../atoms';
import { FileText, AlertCircle } from 'lucide-react';
import { useTranslation } from '../../stores';
import type { Document } from '../../types';

interface DocumentGridProps {
  documents: Document[];
  loading?: boolean;
  error?: string | null;
  onDelete: (title: string) => void;
  onView?: (document: Document) => void;
  deletingTitle?: string | null;
}

export function DocumentGrid({
  documents,
  loading = false,
  error = null,
  onDelete,
  onView,
  deletingTitle = null,
}: DocumentGridProps) {
  const { t } = useTranslation();

  // Loading state with no documents
  if (loading && documents.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-16">
        <Spinner size="xl" className="text-primary-600 mb-4" />
        <p className="text-sm text-slate-500">{t('documents.loading')}</p>
      </div>
    );
  }

  // Error state
  if (error && documents.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-16">
        <div className="w-16 h-16 rounded-2xl bg-red-50 flex items-center justify-center mb-4">
          <AlertCircle className="w-8 h-8 text-red-500" />
        </div>
        <h3 className="text-lg font-medium text-slate-700 mb-2">{t('documents.failedToLoad')}</h3>
        <p className="text-sm text-slate-500 max-w-sm text-center">{error}</p>
      </div>
    );
  }

  // Empty state
  if (documents.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-16">
        <div className="w-16 h-16 rounded-2xl bg-slate-100 flex items-center justify-center mb-4">
          <FileText className="w-8 h-8 text-slate-400" />
        </div>
        <h3 className="text-lg font-medium text-slate-700 mb-2">{t('documents.noDocuments')}</h3>
        <p className="text-sm text-slate-500 max-w-sm text-center">
          {t('documents.noDocumentsHint')}
        </p>
      </div>
    );
  }

  // Grid layout: responsive columns
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
      {documents.map((doc) => (
        <DocumentItem
          key={doc.id}
          document={doc}
          onDelete={onDelete}
          onView={onView}
          deleting={deletingTitle === doc.title}
        />
      ))}
    </div>
  );
}