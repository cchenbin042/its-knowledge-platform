import { useState, useCallback, useRef, useEffect } from 'react';
import { DocumentItem } from '../molecules';
import { Button } from '../atoms';
import { useDocumentsStore } from '../../stores';
import { Upload, RefreshCw, FileText, AlertCircle } from 'lucide-react';

export function DocumentList() {
  const { documents, loading, error, fetchDocuments, uploadDocument, deleteDocument } = useDocumentsStore();
  const [uploading, setUploading] = useState(false);
  const [deletingTitle, setDeletingTitle] = useState<string | null>(null);
  const [dragOver, setDragOver] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Initial fetch
  useEffect(() => {
    fetchDocuments();
  }, [fetchDocuments]);

  const handleFileSelect = useCallback(async (file: File) => {
    if (!file.name.endsWith('.md') && !file.name.endsWith('.markdown')) {
      alert('Only Markdown files are supported');
      return;
    }

    setUploading(true);
    try {
      await uploadDocument(file);
    } catch (err) {
      alert('Upload failed: ' + (err as Error).message);
    } finally {
      setUploading(false);
    }
  }, [uploadDocument]);

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(false);
    const file = e.dataTransfer.files[0];
    if (file) {
      handleFileSelect(file);
    }
  }, [handleFileSelect]);

  const handleDelete = useCallback(async (title: string) => {
    if (!confirm(`Delete document "${title}"?`)) return;

    setDeletingTitle(title);
    try {
      await deleteDocument(title);
    } catch (err) {
      alert('Delete failed: ' + (err as Error).message);
    } finally {
      setDeletingTitle(null);
    }
  }, [deleteDocument]);

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
              <h1 className="text-lg font-semibold text-slate-900">Document Management</h1>
              <p className="text-sm text-slate-500">{documents.length} documents in knowledge base</p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <Button
              variant="secondary"
              size="sm"
              onClick={() => fetchDocuments()}
              disabled={loading}
            >
              <RefreshCw className={`w-4 h-4 mr-1 ${loading ? 'animate-spin' : ''}`} />
              Refresh
            </Button>
            <Button
              size="sm"
              onClick={() => fileInputRef.current?.click()}
              loading={uploading}
            >
              <Upload className="w-4 h-4 mr-1" />
              Upload
            </Button>
            <input
              ref={fileInputRef}
              type="file"
              accept=".md,.markdown"
              className="hidden"
              onChange={(e) => {
                const file = e.target.files?.[0];
                if (file) handleFileSelect(file);
                e.target.value = '';
              }}
            />
          </div>
        </div>
      </div>

      {/* Error */}
      {error && (
        <div className="mx-6 mt-4 flex items-center gap-3 p-4 bg-red-50 rounded-xl">
          <AlertCircle className="w-5 h-5 text-red-600 shrink-0" />
          <p className="text-sm text-red-700">{error}</p>
        </div>
      )}

      {/* Content */}
      <div className="flex-1 overflow-auto p-6">
        <div className="max-w-4xl mx-auto">
          {/* Upload drop zone */}
          <div
            className={`
              mb-6 border-2 border-dashed rounded-xl p-8 text-center transition-colors cursor-pointer
              ${dragOver ? 'border-primary-500 bg-primary-50' : 'border-slate-200 hover:border-slate-300'}
            `}
            onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
            onDragLeave={() => setDragOver(false)}
            onDrop={handleDrop}
            onClick={() => fileInputRef.current?.click()}
          >
            <Upload className="w-8 h-8 mx-auto mb-3 text-slate-400" />
            <p className="text-sm text-slate-600">
              Drag and drop a Markdown file here, or click to browse
            </p>
            <p className="text-xs text-slate-400 mt-1">
              Only .md files are supported
            </p>
          </div>

          {/* Document list */}
          {loading && documents.length === 0 ? (
            <div className="text-center py-12">
              <RefreshCw className="w-8 h-8 mx-auto mb-3 text-slate-300 animate-spin" />
              <p className="text-sm text-slate-500">Loading documents...</p>
            </div>
          ) : documents.length === 0 ? (
            <div className="text-center py-12">
              <div className="w-16 h-16 mx-auto mb-4 rounded-2xl bg-slate-100 flex items-center justify-center">
                <FileText className="w-8 h-8 text-slate-400" />
              </div>
              <h3 className="text-lg font-medium text-slate-700 mb-2">No Documents</h3>
              <p className="text-sm text-slate-500 max-w-sm mx-auto">
                Upload Markdown files to build your knowledge base
              </p>
            </div>
          ) : (
            <div className="space-y-3">
              {documents.map((doc) => (
                <DocumentItem
                  key={doc.id}
                  document={doc}
                  onDelete={handleDelete}
                  deleting={deletingTitle === doc.title}
                />
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}