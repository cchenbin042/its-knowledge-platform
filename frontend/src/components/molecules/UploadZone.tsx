import { useCallback, useRef, useState } from 'react';
import { Upload, FileText, CheckCircle, AlertCircle } from 'lucide-react';
import { Spinner } from '../atoms';
import { useTranslation } from '../../stores';

interface UploadZoneProps {
  onUpload: (file: File) => Promise<void>;
  accept?: string;
  disabled?: boolean;
}

type UploadStatus = 'idle' | 'uploading' | 'success' | 'error';

export function UploadZone({
  onUpload,
  accept = '.md,.markdown',
  disabled = false,
}: UploadZoneProps) {
  const { t } = useTranslation();

  const [dragOver, setDragOver] = useState(false);
  const [status, setStatus] = useState<UploadStatus>('idle');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const resetStatus = useCallback(() => {
    setTimeout(() => {
      setStatus('idle');
      setErrorMessage(null);
    }, 3000);
  }, []);

  const handleFileSelect = useCallback(async (file: File) => {
    if (disabled) return;

    const isValidExtension = file.name.endsWith('.md') || file.name.endsWith('.markdown');
    if (!isValidExtension) {
      setStatus('error');
      setErrorMessage(t('documents.onlyMarkdownError'));
      resetStatus();
      return;
    }

    setStatus('uploading');
    setErrorMessage(null);

    try {
      await onUpload(file);
      setStatus('success');
      resetStatus();
    } catch (err) {
      setStatus('error');
      setErrorMessage((err as Error).message || t('documents.uploadFailed'));
      resetStatus();
    }
  }, [onUpload, disabled, resetStatus, t]);

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(false);

    if (disabled) return;

    const file = e.dataTransfer.files[0];
    if (file) {
      handleFileSelect(file);
    }
  }, [handleFileSelect, disabled]);

  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    if (!disabled) {
      setDragOver(true);
    }
  }, [disabled]);

  const handleDragLeave = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(false);
  }, []);

  const handleClick = useCallback(() => {
    if (!disabled) {
      fileInputRef.current?.click();
    }
  }, [disabled]);

  const handleInputChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      handleFileSelect(file);
    }
    e.target.value = '';
  }, [handleFileSelect]);

  const getStatusIcon = () => {
    switch (status) {
      case 'uploading':
        return <Spinner size="lg" className="text-primary-600" />;
      case 'success':
        return <CheckCircle className="w-8 h-8 text-green-500" />;
      case 'error':
        return <AlertCircle className="w-8 h-8 text-red-500" />;
      default:
        return <Upload className="w-8 h-8 text-slate-400" />;
    }
  };

  const getStatusText = () => {
    switch (status) {
      case 'uploading':
        return t('documents.uploading');
      case 'success':
        return t('documents.uploadSuccess');
      case 'error':
        return errorMessage || t('documents.uploadFailed');
      default:
        return t('documents.dragDropHint');
    }
  };

  const getBorderColor = () => {
    if (disabled) return 'border-slate-100';
    if (dragOver) return 'border-primary-500 bg-primary-50';
    if (status === 'success') return 'border-green-300 bg-green-50';
    if (status === 'error') return 'border-red-300 bg-red-50';
    return 'border-slate-200 hover:border-slate-300';
  };

  return (
    <div
      className={`
        relative border-2 border-dashed rounded-xl p-6 text-center transition-all duration-200
        ${disabled ? 'cursor-not-allowed opacity-60' : 'cursor-pointer'}
        ${getBorderColor()}
      `}
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDrop}
      onClick={handleClick}
    >
      <input
        ref={fileInputRef}
        type="file"
        accept={accept}
        className="hidden"
        onChange={handleInputChange}
        disabled={disabled}
      />

      <div className="flex flex-col items-center gap-3">
        {status === 'uploading' ? (
          <div className="w-12 h-12 rounded-full bg-primary-50 flex items-center justify-center">
            {getStatusIcon()}
          </div>
        ) : (
          <div className={`
            w-12 h-12 rounded-full flex items-center justify-center transition-colors
            ${dragOver ? 'bg-primary-100' : 'bg-slate-100'}
          `}>
            {getStatusIcon()}
          </div>
        )}

        <div>
          <p className={`
            text-sm font-medium transition-colors
            ${status === 'error' ? 'text-red-600' : status === 'success' ? 'text-green-600' : 'text-slate-600'}
          `}>
            {getStatusText()}
          </p>
          {status === 'idle' && (
            <p className="text-xs text-slate-400 mt-1 flex items-center justify-center gap-1">
              <FileText className="w-3 h-3" />
              {t('documents.onlyMarkdown')}
            </p>
          )}
        </div>
      </div>
    </div>
  );
}