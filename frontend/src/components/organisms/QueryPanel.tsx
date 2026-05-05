import { useState } from 'react';
import { SearchBox, SourceCard } from '../molecules';
import { Card, CardBody, Badge } from '../atoms';
import { useQueryStore, useTranslation } from '../../stores';
import { Sparkles, BookOpen, AlertCircle, Loader2, CheckCircle } from 'lucide-react';

export function QueryPanel() {
  const { question, setQuestion, answer, sources, status, progress, error, startQuery, reset } = useQueryStore();
  const { t } = useTranslation();
  const [expandedSource, setExpandedSource] = useState<number | null>(null);

  const isLoading = status === 'retrieving' || status === 'generating';
  const showResult = status === 'complete' || (answer.length > 0);

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="shrink-0 px-6 py-4 border-b border-slate-200 bg-gradient-to-r from-primary-50 to-white">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-primary-600 flex items-center justify-center">
            <Sparkles className="w-5 h-5 text-white" />
          </div>
          <div>
            <h1 className="text-lg font-semibold text-slate-900">{t('query.title')}</h1>
            <p className="text-sm text-slate-500">{t('query.subtitle')}</p>
          </div>
        </div>
      </div>

      {/* Main content area */}
      <div className="flex-1 overflow-auto p-6">
        <div className="max-w-4xl mx-auto space-y-6">
          {/* Search box */}
          <SearchBox
            value={question}
            onChange={setQuestion}
            onSubmit={startQuery}
            loading={isLoading}
            placeholder={t('query.placeholder')}
          />

          {/* Status indicator */}
          {isLoading && (
            <div className="flex items-center gap-3 p-4 bg-primary-50 rounded-xl animate-fade-in">
              <Loader2 className="w-5 h-5 text-primary-600 animate-spin" />
              <div>
                <p className="text-sm font-medium text-primary-900">{progress}</p>
                <div className="flex items-center gap-2 mt-1">
                  <div className="typing-indicator flex gap-1">
                    <span className="w-2 h-2 bg-primary-400 rounded-full"></span>
                    <span className="w-2 h-2 bg-primary-400 rounded-full"></span>
                    <span className="w-2 h-2 bg-primary-400 rounded-full"></span>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Error */}
          {error && (
            <div className="flex items-start gap-3 p-4 bg-red-50 rounded-xl animate-fade-in">
              <AlertCircle className="w-5 h-5 text-red-600 shrink-0 mt-0.5" />
              <div>
                <p className="text-sm font-medium text-red-900">{t('common.error')}</p>
                <p className="text-sm text-red-700">{error}</p>
              </div>
            </div>
          )}

          {/* Answer */}
          {showResult && (
            <div className="space-y-4 animate-fade-in">
              {/* Answer card */}
              <Card variant="elevated">
                <CardBody>
                  <div className="flex items-center gap-2 mb-3">
                    <Sparkles className="w-4 h-4 text-primary-600" />
                    <h2 className="font-medium text-slate-900">{t('query.answer')}</h2>
                    {status === 'complete' && (
                      <Badge variant="success" className="ml-auto">
                        <CheckCircle className="w-3 h-3 mr-1" />
                        {t('query.complete')}
                      </Badge>
                    )}
                  </div>
                  <div className="prose prose-slate prose-sm max-w-none">
                    <p className="whitespace-pre-wrap text-slate-700 leading-relaxed">
                      {answer}
                      {status === 'generating' && (
                        <span className="inline-block w-2 h-4 ml-1 bg-primary-600 animate-pulse"></span>
                      )}
                    </p>
                  </div>
                </CardBody>
              </Card>

              {/* Sources */}
              {sources.length > 0 && (
                <div className="space-y-3">
                  <div className="flex items-center gap-2">
                    <BookOpen className="w-4 h-4 text-slate-500" />
                    <h3 className="text-sm font-medium text-slate-700">
                      {t('query.sourcesCount', { count: sources.length })}
                    </h3>
                  </div>
                  <div className="grid gap-3 sm:grid-cols-2">
                    {sources.map((source, index) => (
                      <SourceCard
                        key={source.documentId || index}
                        source={source}
                        index={index}
                        expanded={expandedSource === index}
                        onToggle={() => setExpandedSource(expandedSource === index ? null : index)}
                      />
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}

          {/* Empty state */}
          {status === 'idle' && !answer && (
            <div className="text-center py-12">
              <div className="w-16 h-16 mx-auto mb-4 rounded-2xl bg-slate-100 flex items-center justify-center">
                <BookOpen className="w-8 h-8 text-slate-400" />
              </div>
              <h3 className="text-lg font-medium text-slate-700 mb-2">{t('query.startConversation')}</h3>
              <p className="text-sm text-slate-500 max-w-sm mx-auto">
                {t('query.startConversationDesc')}
              </p>
            </div>
          )}
        </div>
      </div>

      {/* Footer actions */}
      {showResult && (
        <div className="shrink-0 px-6 py-4 border-t border-slate-200 bg-slate-50">
          <div className="flex items-center justify-between max-w-4xl mx-auto">
            <p className="text-sm text-slate-500">
              {t('query.foundSources', { count: sources.length })}
            </p>
            <button
              onClick={reset}
              className="text-sm text-primary-600 hover:text-primary-700 font-medium"
            >
              {t('query.newQuestion')}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}