import { useEffect, useState } from 'react';
import { X, FileText, Hash, Clock, FileCode, Copy, Check } from 'lucide-react';
import { Button, Spinner } from '../atoms';
import { useTranslation } from '../../stores';
import { api } from '../../api';
import type { Document, DocumentPreview } from '../../types';

interface DocumentDetailModalProps {
  document: Document | null;
  onClose: () => void;
}

export function DocumentDetailModal({ document, onClose }: DocumentDetailModalProps) {
  const { t } = useTranslation();
  const [preview, setPreview] = useState<DocumentPreview | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    if (!document) {
      setPreview(null);
      return;
    }

    const fetchPreview = async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await api.getDocumentPreview(document.title);
        setPreview(data);
      } catch (err) {
        setError((err as Error).message);
      } finally {
        setLoading(false);
      }
    };

    fetchPreview();
  }, [document]);

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

  const handleCopyContent = async () => {
    if (preview?.contentPreview) {
      await navigator.clipboard.writeText(preview.contentPreview);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  const handleBackdropClick = (e: React.MouseEvent) => {
    if (e.target === e.currentTarget) {
      onClose();
    }
  };

  // Handle escape key
  useEffect(() => {
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };
    window.addEventListener('keydown', handleEscape);
    return () => window.removeEventListener('keydown', handleEscape);
  }, [onClose]);

  if (!document) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm"
      onClick={handleBackdropClick}
    >
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-4xl max-h-[90vh] flex flex-col m-4">
        {/* Header */}
        <div className="shrink-0 flex items-center justify-between px-6 py-4 border-b border-slate-200">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-primary-50 flex items-center justify-center">
              <FileText className="w-5 h-5 text-primary-600" />
            </div>
            <div>
              <h2 className="text-lg font-semibold text-slate-900">{t('documents.detail.title')}</h2>
              <p className="text-sm text-slate-500 truncate max-w-md">{document.title}</p>
            </div>
          </div>
          <Button variant="ghost" size="sm" onClick={onClose} className="text-slate-400 hover:text-slate-600">
            <X className="w-5 h-5" />
          </Button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-auto p-6">
          {/* Metadata Section */}
          <div className="mb-6">
            <h3 className="text-sm font-medium text-slate-700 mb-3">{t('documents.detail.metadata')}</h3>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div className="flex items-center gap-3 p-3 bg-slate-50 rounded-lg">
                <Hash className="w-4 h-4 text-slate-400" />
                <div>
                  <p className="text-xs text-slate-500">{t('documents.table.chunkCount')}</p>
                  <p className="text-sm font-medium text-slate-900">{document.chunkCount}</p>
                </div>
              </div>
              <div className="flex items-center gap-3 p-3 bg-slate-50 rounded-lg">
                <Clock className="w-4 h-4 text-slate-400" />
                <div>
                  <p className="text-xs text-slate-500">{t('documents.table.createdAt')}</p>
                  <p className="text-sm font-medium text-slate-900">{formatDate(document.createdAt)}</p>
                </div>
              </div>
              <div className="flex items-center gap-3 p-3 bg-slate-50 rounded-lg">
                <FileCode className="w-4 h-4 text-slate-400" />
                <div>
                  <p className="text-xs text-slate-500">{t('documents.detail.contentHash')}</p>
                  <p className="text-xs font-mono text-slate-900 truncate" title={document.contentHash}>
                    {document.contentHash.slice(0, 12)}...
                  </p>
                </div>
              </div>
            </div>
          </div>

          {/* File Path */}
          {document.filePath && (
            <div className="mb-6">
              <h3 className="text-sm font-medium text-slate-700 mb-2">{t('documents.detail.filePath')}</h3>
              <p className="text-sm text-slate-600 bg-slate-50 px-3 py-2 rounded-lg font-mono break-all">
                {document.filePath}
              </p>
            </div>
          )}

          {/* Content Section */}
          <div>
            <div className="flex items-center justify-between mb-3">
              <h3 className="text-sm font-medium text-slate-700">{t('documents.detail.content')}</h3>
              {preview?.contentPreview && (
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={handleCopyContent}
                  className="text-slate-500 hover:text-slate-700"
                >
                  {copied ? <Check className="w-4 h-4 mr-1" /> : <Copy className="w-4 h-4 mr-1" />}
                  {copied ? t('feedback.copied') : 'Copy'}
                </Button>
              )}
            </div>

            {loading && (
              <div className="flex items-center justify-center py-12">
                <Spinner size="lg" className="text-primary-600" />
              </div>
            )}

            {error && (
              <div className="p-4 bg-red-50 rounded-lg text-sm text-red-600">
                {error}
              </div>
            )}

            {!loading && !error && preview?.contentPreview && (
              <div className="prose prose-slate prose-sm max-w-none bg-slate-50 rounded-lg p-4 max-h-96 overflow-auto">
                <pre className="whitespace-pre-wrap break-words text-sm text-slate-700 m-0">
                  {preview.contentPreview}
                </pre>
                {preview.totalChunks > 5 && (
                  <p className="text-xs text-slate-400 mt-2 italic">
                    Showing preview of first 5 chunks out of {preview.totalChunks} total.
                  </p>
                )}
              </div>
            )}

            {!loading && !error && preview && !preview.contentPreview && (
              <div className="text-center py-8 text-slate-500">
                No content preview available
              </div>
            )}
          </div>
        </div>

        {/* Footer */}
        <div className="shrink-0 flex justify-end px-6 py-4 border-t border-slate-200 bg-slate-50 rounded-b-2xl">
          <Button variant="secondary" onClick={onClose}>
            {t('documents.detail.close')}
          </Button>
        </div>
      </div>
    </div>
  );
}