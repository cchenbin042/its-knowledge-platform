import { FileText, Trash2, Clock, Hash } from 'lucide-react';
import { Card, Button, Badge } from '../atoms';
import { useTranslation } from '../../stores';
import type { Document } from '../../types';

// Helper function to infer document type from file path
function getDocumentType(filePath: string): string {
  const ext = filePath?.split('.').pop()?.toLowerCase();
  switch (ext) {
    case 'md':
    case 'markdown':
      return 'Markdown';
    case 'txt':
      return 'Text';
    case 'pdf':
      return 'PDF';
    case 'doc':
    case 'docx':
      return 'Word';
    default:
      return ext?.toUpperCase() || 'Document';
  }
}

// Helper function to infer tags from title
function inferTagsFromTitle(title: string): string[] {
  const tags: string[] = [];
  const lowerTitle = title.toLowerCase();

  if (lowerTitle.includes('api') || lowerTitle.includes('接口')) {
    tags.push('API');
  }
  if (lowerTitle.includes('guide') || lowerTitle.includes('指南') || lowerTitle.includes('快速') || lowerTitle.includes('开始')) {
    tags.push('指南');
  }
  if (lowerTitle.includes('config') || lowerTitle.includes('配置') || lowerTitle.includes('设置')) {
    tags.push('配置');
  }
  if (lowerTitle.includes('install') || lowerTitle.includes('安装') || lowerTitle.includes('部署')) {
    tags.push('部署');
  }
  if (lowerTitle.includes('readme') || lowerTitle.includes('说明')) {
    tags.push('说明');
  }

  return tags.length > 0 ? tags : ['文档'];
}

interface DocumentItemProps {
  document: Document;
  onDelete: (title: string) => void;
  deleting?: boolean;
  onView?: (document: Document) => void;
}

export function DocumentItem({ document, onDelete, deleting = false, onView }: DocumentItemProps) {
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

  const docType = document.type || getDocumentType(document.filePath);
  const tags = document.tags || inferTagsFromTitle(document.title);

  return (
    <Card className="hover:border-primary-300 transition-colors cursor-pointer" onClick={() => onView?.(document)}>
      <div className="p-4">
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-start gap-3 min-w-0 flex-1">
            <div className="shrink-0 w-10 h-10 rounded-lg bg-primary-50 flex items-center justify-center">
              <FileText className="w-5 h-5 text-primary-600" />
            </div>
            <div className="min-w-0 flex-1">
              <h3 className="font-medium text-slate-900 truncate">{document.title}</h3>
              <div className="flex flex-wrap items-center gap-2 mt-1.5">
                {/* Type badge */}
                <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-xs font-medium bg-slate-100 text-slate-600">
                  {docType}
                </span>
                {/* Tags */}
                {tags.map((tag, index) => (
                  <Badge key={index} variant="primary" size="sm">
                    {tag}
                  </Badge>
                ))}
              </div>
              <div className="flex items-center gap-3 mt-2 text-sm text-slate-500">
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
            onClick={(e) => {
              e.stopPropagation();
              onDelete(document.title);
            }}
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