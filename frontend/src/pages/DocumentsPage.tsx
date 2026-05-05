import { useEffect, useCallback, useState } from 'react';
import { UploadZone, DocumentDetailModal } from '../components/molecules';
import { DocumentTable, DocumentGrid } from '../components/organisms';
import { Button } from '../components/atoms';
import { useDocumentsStore, useTranslation } from '../stores';
import { FileText, RefreshCw, AlertCircle, LayoutGrid, List } from 'lucide-react';
import type { Document } from '../types';

type ViewMode = 'table' | 'grid';

export function DocumentsPage() {
  const { documents, loading, error, fetchDocuments, uploadDocument, deleteDocument } = useDocumentsStore();
  const { t } = useTranslation();
  const [deletingTitle, setDeletingTitle] = useState<string | null>(null);
  const [selectedDocument, setSelectedDocument] = useState<Document | null>(null);
  const [viewMode, setViewMode] = useState<ViewMode>('grid');

  // Initial fetch
  useEffect(() => {
    fetchDocuments();
  }, [fetchDocuments]);

  const handleUpload = useCallback(async (file: File) => {
    await uploadDocument(file);
  }, [uploadDocument]);

  const handleView = useCallback((document: Document) => {
    setSelectedDocument(document);
  }, []);

  const handleDelete = useCallback(async (title: string) => {
    if (!confirm(t('documents.deleteConfirm', { title }))) return;

    setDeletingTitle(title);
    try {
      await deleteDocument(title);
    } catch (err) {
      alert(t('documents.deleteFailed', { error: (err as Error).message }));
    } finally {
      setDeletingTitle(null);
    }
  }, [deleteDocument, t]);

  const handleCloseModal = useCallback(() => {
    setSelectedDocument(null);
  }, []);

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="shrink-0 px-6 py-4 border-b border-slate-200 bg-gradient-to-r from-slate-50 to-white">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-slate-700 flex items-center justify-center">
              <FileText className="w-5 h-5 text-white" />
            </div>
            <div>
              <h1 className="text-lg font-semibold text-slate-900">{t('documents.title')}</h1>
              <p className="text-sm text-slate-500">{t('documents.docCount', { count: documents?.length ?? 0 })}</p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            {/* View Mode Toggle */}
            <div className="flex items-center bg-slate-100 rounded-lg p-1">
              <button
                onClick={() => setViewMode('grid')}
                className={`p-1.5 rounded-md transition-colors ${
                  viewMode === 'grid'
                    ? 'bg-white text-slate-700 shadow-sm'
                    : 'text-slate-500 hover:text-slate-700'
                }`}
                title={t('documents.gridView')}
              >
                <LayoutGrid className="w-4 h-4" />
              </button>
              <button
                onClick={() => setViewMode('table')}
                className={`p-1.5 rounded-md transition-colors ${
                  viewMode === 'table'
                    ? 'bg-white text-slate-700 shadow-sm'
                    : 'text-slate-500 hover:text-slate-700'
                }`}
                title={t('documents.tableView')}
              >
                <List className="w-4 h-4" />
              </button>
            </div>
            <Button
              variant="secondary"
              size="sm"
              onClick={() => fetchDocuments()}
              disabled={loading}
            >
              <RefreshCw className={`w-4 h-4 mr-1 ${loading ? 'animate-spin' : ''}`} />
              {t('common.refresh')}
            </Button>
          </div>
        </div>
      </div>

      {/* Error Banner */}
      {error && (
        <div className="mx-6 mt-4 flex items-center gap-3 p-4 bg-red-50 rounded-xl">
          <AlertCircle className="w-5 h-5 text-red-600 shrink-0" />
          <p className="text-sm text-red-700">{error}</p>
        </div>
      )}

      {/* Content */}
      <div className="flex-1 overflow-auto p-6">
        <div className="max-w-5xl mx-auto space-y-6">
          {/* Upload Zone */}
          <UploadZone onUpload={handleUpload} disabled={loading} />

          {/* Documents View */}
          {viewMode === 'grid' ? (
            <DocumentGrid
              documents={documents ?? []}
              loading={loading}
              error={error}
              onDelete={handleDelete}
              onView={handleView}
              deletingTitle={deletingTitle}
            />
          ) : (
            <DocumentTable
              documents={documents ?? []}
              loading={loading}
              error={error}
              onView={handleView}
              onDelete={handleDelete}
              deletingTitle={deletingTitle}
            />
          )}
        </div>
      </div>

      {/* Document Detail Modal */}
      <DocumentDetailModal
        document={selectedDocument}
        onClose={handleCloseModal}
      />
    </div>
  );
}