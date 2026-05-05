import { useRef, useCallback, useState } from 'react';
import { Send, Sparkles, Plus, Zap, Brain } from 'lucide-react';
import { Button, Toggle } from '../atoms';
import {
  MessageItem,
  RetrievalSteps,
  FeedbackBar,
  SourceCard,
  type Message,
} from '../molecules';
import { useChatStore, useTranslation } from '../../stores';
import { api } from '../../api';
import clsx from 'clsx';
import type { RetrievalResult } from '../../types';

export function ChatContainer() {
  const {
    messages,
    mode,
    isStreaming,
    steps,
    error,
    setMode,
    addUserMessage,
    appendToAssistantMessage,
    setFeedback,
    clearMessages,
    updateStep,
    setError,
    setStreaming,
    resetSteps,
  } = useChatStore();

  const { t } = useTranslation();

  const [input, setInput] = useState('');
  const [currentSources, setCurrentSources] = useState<RetrievalResult[]>([]);
  const [expandedSource, setExpandedSource] = useState<number | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const abortControllerRef = useRef<AbortController | null>(null);

  const scrollToBottom = useCallback(() => {
    setTimeout(() => {
      messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, 100);
  }, []);

  const handleSubmit = useCallback(async () => {
    if (!input.trim() || isStreaming) return;

    const question = input.trim();
    setInput('');
    setError(null);
    resetSteps();

    // Add user message
    addUserMessage(question);
    scrollToBottom();

    // Create placeholder for assistant message
    const assistantId = `assistant-${Date.now()}`;
    const assistantMessage: Message = {
      id: assistantId,
      role: 'assistant',
      content: '',
      timestamp: new Date(),
    };
    useChatStore.setState((state) => ({
      messages: [...state.messages, assistantMessage],
    }));

    setStreaming(true);
    abortControllerRef.current = new AbortController();

    try {
      // Step 1: Query Understanding
      updateStep('understand', 'running');
      await new Promise((r) => setTimeout(r, 300)); // Simulate processing
      updateStep('understand', 'completed');

      // Step 2: Knowledge Retrieval
      updateStep('retrieve', 'running');

      for await (const event of api.streamQuery(question)) {
        const { eventType, data } = event;

        switch (eventType) {
          case 'query_start':
          case 'retrieval_start':
            break;
          case 'retrieval_complete':
            updateStep('retrieve', 'completed');
            updateStep('rerank', 'running');
            break;
          case 'context_built':
            updateStep('rerank', 'completed');
            updateStep('generate', 'running');
            break;
          case 'llm_start':
            break;
          case 'llm_token':
            const token = String(data);
            appendToAssistantMessage(assistantId, token);
            scrollToBottom();
            break;
          case 'llm_complete':
            break;
          case 'complete':
            const completeData = data as { sources?: RetrievalResult[] };
            setCurrentSources(completeData.sources || []);
            updateStep('generate', 'completed');
            break;
          case 'error':
            setError(String(data));
            updateStep('generate', 'error');
            break;
        }
      }
    } catch (err) {
      if ((err as Error).name === 'AbortError') {
        setError(t('common.cancel'));
      } else {
        setError((err as Error).message);
      }
      updateStep('generate', 'error');
    } finally {
      setStreaming(false);
      abortControllerRef.current = null;
    }
  }, [
    input,
    isStreaming,
    addUserMessage,
    appendToAssistantMessage,
    setError,
    setStreaming,
    updateStep,
    resetSteps,
    scrollToBottom,
    t,
  ]);

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit();
    }
  };

  const handleCancel = () => {
    abortControllerRef.current?.abort();
  };

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="shrink-0 px-4 py-3 border-b border-slate-200 bg-white">
        <div className="flex items-center justify-between max-w-4xl mx-auto">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-primary-600 flex items-center justify-center">
              <Sparkles className="w-5 h-5 text-white" />
            </div>
            <div>
              <h1 className="text-base font-semibold text-slate-900">
                {t('query.title')}
              </h1>
              <p className="text-xs text-slate-500">
                {mode === 'quick' ? t('query.quickMode') : t('query.deepMode')}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-4">
            {/* Mode Toggle */}
            <div className="flex items-center gap-2 px-3 py-1.5 bg-slate-50 rounded-lg">
              <Zap
                className={clsx(
                  'w-4 h-4',
                  mode === 'quick' ? 'text-primary-600' : 'text-slate-400'
                )}
              />
              <Toggle
                checked={mode === 'deep'}
                onChange={(checked) => setMode(checked ? 'deep' : 'quick')}
                size="sm"
                disabled={isStreaming}
              />
              <Brain
                className={clsx(
                  'w-4 h-4',
                  mode === 'deep' ? 'text-primary-600' : 'text-slate-400'
                )}
              />
            </div>
            {/* New Chat Button */}
            <Button
              variant="ghost"
              size="sm"
              onClick={clearMessages}
              disabled={isStreaming || messages.length === 0}
            >
              <Plus className="w-4 h-4 mr-1" />
              {t('common.new')}
            </Button>
          </div>
        </div>
      </div>

      {/* Messages Area */}
      <div className="flex-1 overflow-auto">
        <div className="max-w-4xl mx-auto py-6 px-4 space-y-4">
          {messages.length === 0 ? (
            <div className="text-center py-16">
              <div className="w-16 h-16 mx-auto mb-4 rounded-2xl bg-slate-100 flex items-center justify-center">
                <Sparkles className="w-8 h-8 text-slate-400" />
              </div>
              <h3 className="text-lg font-medium text-slate-700 mb-2">
                {t('query.startConversation')}
              </h3>
              <p className="text-sm text-slate-500 max-w-sm mx-auto">
                {t('query.startConversationDesc')}
              </p>
            </div>
          ) : (
            <>
              {messages.map((msg, index) => (
                <div key={msg.id}>
                  <MessageItem
                    message={msg}
                    isStreaming={isStreaming && index === messages.length - 1}
                  />
                  {/* Feedback for assistant messages */}
                  {msg.role === 'assistant' && !isStreaming && msg.content && (
                    <div className="flex justify-start mt-2 ml-11">
                      <FeedbackBar
                        feedback={msg.feedback}
                        onFeedback={(type) => setFeedback(msg.id, type)}
                      />
                    </div>
                  )}
                </div>
              ))}

              {/* Retrieval Steps - show during streaming */}
              {isStreaming && (
                <div className="ml-11">
                  <RetrievalSteps steps={steps} />
                </div>
              )}

              {/* Error Display */}
              {error && (
                <div className="flex items-center gap-2 p-3 bg-red-50 text-red-700 rounded-lg">
                  <span className="text-sm">{error}</span>
                </div>
              )}

              {/* Sources */}
              {!isStreaming && currentSources.length > 0 && (
                <div className="pt-4 border-t border-slate-100 mt-4">
                  <h4 className="text-sm font-medium text-slate-700 mb-3">
                    {t('query.sourcesCount', { count: currentSources.length })}
                  </h4>
                  <div className="grid gap-3 sm:grid-cols-2">
                    {currentSources.map((source, index) => (
                      <SourceCard
                        key={source.documentId || index}
                        source={source}
                        index={index}
                        expanded={expandedSource === index}
                        onToggle={() =>
                          setExpandedSource(
                            expandedSource === index ? null : index
                          )
                        }
                      />
                    ))}
                  </div>
                </div>
              )}
            </>
          )}
          <div ref={messagesEndRef} />
        </div>
      </div>

      {/* Input Area */}
      <div className="shrink-0 px-4 py-3 border-t border-slate-200 bg-white">
        <div className="max-w-4xl mx-auto">
          <div className="relative flex items-end gap-2 bg-white border border-slate-200 rounded-xl p-3 shadow-sm focus-within:ring-2 focus-within:ring-primary-500 focus-within:border-transparent">
            <textarea
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder={t('query.inputPlaceholder')}
              disabled={isStreaming}
              rows={1}
              className={clsx(
                'flex-1 resize-none border-none outline-none text-slate-900 placeholder-slate-400',
                'disabled:bg-transparent disabled:text-slate-500'
              )}
              style={{ minHeight: '24px', maxHeight: '120px' }}
            />
            {isStreaming ? (
              <Button
                variant="secondary"
                onClick={handleCancel}
                className="shrink-0"
              >
                {t('common.cancel')}
              </Button>
            ) : (
              <Button
                onClick={handleSubmit}
                disabled={!input.trim()}
                className="shrink-0"
              >
                <Send className="w-4 h-4" />
              </Button>
            )}
          </div>
          <p className="mt-2 text-xs text-slate-400 text-center">
            {t('query.inputHint')}
          </p>
        </div>
      </div>
    </div>
  );
}