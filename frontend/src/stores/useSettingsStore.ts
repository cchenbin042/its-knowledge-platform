import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export interface LlmConfig {
  baseUrl: string;
  embeddingModel: string;
  chatModel: string;
}

interface SettingsState {
  // Retrieval params
  topK: number;
  rrfK: number;
  minRelevance: number;

  // UI preferences
  defaultMode: 'quick' | 'deep';
  collapseSteps: boolean;

  // LLM config (read-only, from backend)
  llmConfig: LlmConfig;
  llmConfigLoading: boolean;
  llmConfigError: string | null;

  // Actions
  setTopK: (value: number) => void;
  setRrfK: (value: number) => void;
  setMinRelevance: (value: number) => void;
  setDefaultMode: (mode: 'quick' | 'deep') => void;
  setCollapseSteps: (collapse: boolean) => void;
  fetchLlmConfig: () => Promise<void>;
  reset: () => void;
}

const defaultLlmConfig: LlmConfig = {
  baseUrl: '',
  embeddingModel: '',
  chatModel: '',
};

const initialState = {
  topK: 8,
  rrfK: 60,
  minRelevance: 0.5,
  defaultMode: 'quick' as const,
  collapseSteps: false,
  llmConfig: defaultLlmConfig,
  llmConfigLoading: false,
  llmConfigError: null,
};

export const useSettingsStore = create<SettingsState>()(
  persist(
    (set) => ({
      ...initialState,

      setTopK: (value) => set({ topK: value }),
      setRrfK: (value) => set({ rrfK: value }),
      setMinRelevance: (value) => set({ minRelevance: value }),
      setDefaultMode: (mode) => set({ defaultMode: mode }),
      setCollapseSteps: (collapse) => set({ collapseSteps: collapse }),

      fetchLlmConfig: async () => {
        set({ llmConfigLoading: true, llmConfigError: null });
        try {
          const response = await fetch('/api/config/llm');
          if (!response.ok) {
            throw new Error('Failed to fetch LLM config');
          }
          const data = await response.json();
          set({
            llmConfig: {
              baseUrl: data.data?.baseUrl || '',
              embeddingModel: data.data?.embeddingModel || '',
              chatModel: data.data?.chatModel || '',
            },
            llmConfigLoading: false,
          });
        } catch (error) {
          set({
            llmConfigError: (error as Error).message,
            llmConfigLoading: false,
          });
        }
      },

      reset: () => set(initialState),
    }),
    {
      name: 'its-settings',
      partialize: (state) => ({
        topK: state.topK,
        rrfK: state.rrfK,
        minRelevance: state.minRelevance,
        defaultMode: state.defaultMode,
        collapseSteps: state.collapseSteps,
      }),
    }
  )
);