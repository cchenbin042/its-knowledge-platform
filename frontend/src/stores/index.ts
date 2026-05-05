import { create } from 'zustand';
import type { Document, RetrievalResult } from '../types';
import { api } from '../api';

// Re-export i18n
export { useI18nStore, useLanguageStore, useTranslation, type Language } from './useI18nStore';

interface DocumentsState {
  documents: Document[];
  loading: boolean;
  error: string | null;
  fetchDocuments: () => Promise<void>;
  uploadDocument: (file: File) => Promise<void>;
  deleteDocument: (title: string) => Promise<void>;
}

export const useDocumentsStore = create<DocumentsState>((set, get) => ({
  documents: [],
  loading: false,
  error: null,

  fetchDocuments: async () => {
    set({ loading: true, error: null });
    try {
      const documents = await api.getDocuments();
      set({ documents, loading: false });
    } catch (error) {
      set({ error: (error as Error).message, loading: false });
    }
  },

  uploadDocument: async (file: File) => {
    set({ loading: true, error: null });
    try {
      await api.uploadDocument(file);
      // Refresh the list
      const documents = await api.getDocuments();
      set({ documents, loading: false });
    } catch (error) {
      set({ error: (error as Error).message, loading: false });
      throw error;
    }
  },

  deleteDocument: async (title: string) => {
    set({ loading: true, error: null });
    try {
      await api.deleteDocument(title);
      const documents = get().documents.filter((d) => d.title !== title);
      set({ documents, loading: false });
    } catch (error) {
      set({ error: (error as Error).message, loading: false });
    }
  },
}));

// Query state for streaming
interface QueryState {
  question: string;
  answer: string;
  sources: RetrievalResult[];
  status: 'idle' | 'retrieving' | 'generating' | 'complete' | 'error';
  progress: string;
  error: string | null;

  setQuestion: (question: string) => void;
  startQuery: () => Promise<void>;
  reset: () => void;
}

export const useQueryStore = create<QueryState>((set, get) => ({
  question: '',
  answer: '',
  sources: [],
  status: 'idle',
  progress: '',
  error: null,

  setQuestion: (question: string) => set({ question }),

  startQuery: async () => {
    const { question } = get();
    if (!question.trim()) return;

    set({
      status: 'retrieving',
      answer: '',
      sources: [],
      progress: 'Starting retrieval...',
      error: null,
    });

    try {
      for await (const event of api.streamQuery(question)) {
        const { eventType, data } = event as { eventType: string; data: Record<string, unknown> };

        switch (eventType) {
          case 'retrieval_start':
            set({ progress: 'Starting document retrieval...' });
            break;
          case 'retrieval_vector':
            set({ progress: 'Searching vector store...' });
            break;
          case 'retrieval_bm25':
            set({ progress: 'Searching full-text index...' });
            break;
          case 'retrieval_fused':
            set({ progress: 'Merging results...' });
            break;
          case 'context_built':
            set({ status: 'generating', progress: 'Building context...' });
            break;
          case 'llm_token':
            const token = String(data);
            set((state) => ({ answer: state.answer + token }));
            break;
          case 'llm_done':
            set({ progress: 'Generation complete' });
            break;
          case 'complete':
            const completeData = data as { sources?: RetrievalResult[] };
            set({
              status: 'complete',
              sources: completeData.sources || [],
              progress: '',
            });
            break;
          case 'error':
            set({
              status: 'error',
              error: String(data),
              progress: '',
            });
            break;
        }
      }
    } catch (error) {
      set({
        status: 'error',
        error: (error as Error).message,
        progress: '',
      });
    }
  },

  reset: () =>
    set({
      question: '',
      answer: '',
      sources: [],
      status: 'idle',
      progress: '',
      error: null,
    }),
}));

// Re-export useChatStore
export { useChatStore } from './useChatStore';
export type { Session } from './useChatStore';
export { useSettingsStore } from './useSettingsStore';
export type { LlmConfig } from './useSettingsStore';
export { useStatsStore } from './useStatsStore';