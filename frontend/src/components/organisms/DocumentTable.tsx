import { useState, useMemo } from 'react';
import { FileText, AlertCircle, Eye, Trash2, ArrowUpDown, ArrowUp, ArrowDown, Search } from 'lucide-react';
import { Button, Spinner } from '../atoms';
import { useTranslation } from '../../stores';
import type { Document } from '../../types';

type SortField = 'title' | 'createdAt' | 'chunkCount';
type SortOrder = 'asc' | 'desc';

interface DocumentTableProps {
  documents: Document[];
  loading?: boolean;
  error?: string | null;
  onView: (document: Document) => void;
  onDelete: (title: string) => void;
  deletingTitle?: string | null;
}

export function DocumentTable({
  documents,
  loading = false,
  error = null,
  onView,
  onDelete,
  deletingTitle = null,
}: DocumentTableProps) {
  const { t } = useTranslation();
  const [searchQuery, setSearchQuery] = useState('');
  const [sortField, setSortField] = useState<SortField>('createdAt');
  const [sortOrder, setSortOrder] = useState<SortOrder>('desc');

  // Filter and sort documents
  const filteredDocuments = useMemo(() => {
    let result = [...documents];

    // Filter by search query
    if (searchQuery.trim()) {
      const query = searchQuery.toLowerCase();
      result = result.filter((doc) => doc.title.toLowerCase().includes(query));
    }

    // Sort
    result.sort((a, b) => {
      let comparison = 0;
      switch (sortField) {
        case 'title':
          comparison = a.title.localeCompare(b.title);
          break;
        case 'createdAt':
          comparison = new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime();
          break;
        case 'chunkCount':
          comparison = a.chunkCount - b.chunkCount;
          break;
      }
      return sortOrder === 'asc' ? comparison : -comparison;
    });

    return result;
  }, [documents, searchQuery, sortField, sortOrder]);

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

  const toggleSort = (field: SortField) => {
    if (sortField === field) {
      setSortOrder((prev) => (prev === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortField(field);
      setSortOrder('desc');
    }
  };

  const SortIcon = ({ field }: { field: SortField }) => {
    if (sortField !== field) {
      return <ArrowUpDown className="w-4 h-4 text-slate-400" />;
    }
    return sortOrder === 'asc' ? (
      <ArrowUp className="w-4 h-4 text-primary-600" />
    ) : (
      <ArrowDown className="w-4 h-4 text-primary-600" />
    );
  };

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

  return (
    <div className="space-y-4">
      {/* Search and Filter Bar */}
      <div className="flex flex-col sm:flex-row gap-3 sm:items-center sm:justify-between">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
          <input
            type="text"
            placeholder={t('documents.table.searchPlaceholder')}
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full sm:w-64 pl-10 pr-4 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
          />
        </div>
        <div className="flex items-center gap-2 text-sm text-slate-500">
          <span>{t('documents.table.sortBy')}:</span>
          <div className="flex gap-1">
            <button
              onClick={() => toggleSort('title')}
              className={`px-2 py-1 rounded-md text-xs font-medium transition-colors ${
                sortField === 'title' ? 'bg-primary-50 text-primary-600' : 'text-slate-600 hover:bg-slate-100'
              }`}
            >
              {t('documents.table.title')}
            </button>
            <button
              onClick={() => toggleSort('createdAt')}
              className={`px-2 py-1 rounded-md text-xs font-medium transition-colors ${
                sortField === 'createdAt' ? 'bg-primary-50 text-primary-600' : 'text-slate-600 hover:bg-slate-100'
              }`}
            >
              {t('documents.table.createdAt')}
            </button>
          </div>
        </div>
      </div>

      {/* Table */}
      {filteredDocuments.length === 0 ? (
        <div className="text-center py-12 text-slate-500">
          <p>{t('documents.table.noResults')}</p>
        </div>
      ) : (
        <div className="bg-white border border-slate-200 rounded-xl overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="bg-slate-50 border-b border-slate-200">
                  <th className="text-left px-4 py-3 text-sm font-medium text-slate-600">
                    <button
                      onClick={() => toggleSort('title')}
                      className="flex items-center gap-1 hover:text-slate-900 transition-colors"
                    >
                      {t('documents.table.title')}
                      <SortIcon field="title" />
                    </button>
                  </th>
                  <th className="text-left px-4 py-3 text-sm font-medium text-slate-600">
                    <button
                      onClick={() => toggleSort('chunkCount')}
                      className="flex items-center gap-1 hover:text-slate-900 transition-colors"
                    >
                      {t('documents.table.chunkCount')}
                      <SortIcon field="chunkCount" />
                    </button>
                  </th>
                  <th className="text-left px-4 py-3 text-sm font-medium text-slate-600">
                    <button
                      onClick={() => toggleSort('createdAt')}
                      className="flex items-center gap-1 hover:text-slate-900 transition-colors"
                    >
                      {t('documents.table.createdAt')}
                      <SortIcon field="createdAt" />
                    </button>
                  </th>
                  <th className="text-right px-4 py-3 text-sm font-medium text-slate-600">
                    {t('documents.table.actions')}
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filteredDocuments.map((doc) => (
                  <tr
                    key={doc.id}
                    className="hover:bg-slate-50 transition-colors"
                  >
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-3">
                        <div className="shrink-0 w-8 h-8 rounded-lg bg-primary-50 flex items-center justify-center">
                          <FileText className="w-4 h-4 text-primary-600" />
                        </div>
                        <span className="font-medium text-slate-900 truncate max-w-xs" title={doc.title}>
                          {doc.title}
                        </span>
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-slate-100 text-slate-700">
                        {doc.chunkCount} {t('documents.chunks')}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-sm text-slate-500">
                      {formatDate(doc.createdAt)}
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center justify-end gap-2">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => onView(doc)}
                          className="text-slate-500 hover:text-primary-600 hover:bg-primary-50"
                        >
                          <Eye className="w-4 h-4" />
                          <span className="ml-1 hidden sm:inline">{t('documents.table.view')}</span>
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => onDelete(doc.title)}
                          disabled={deletingTitle === doc.title}
                          className="text-slate-500 hover:text-red-600 hover:bg-red-50"
                        >
                          {deletingTitle === doc.title ? (
                            <Spinner size="sm" className="w-4 h-4" />
                          ) : (
                            <Trash2 className="w-4 h-4" />
                          )}
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Results count */}
      {searchQuery && filteredDocuments.length > 0 && (
        <p className="text-sm text-slate-500 text-center">
          {filteredDocuments.length} / {documents.length} {t('documents.docCount', { count: documents.length }).split(' ')[1]}
        </p>
      )}
    </div>
  );
}