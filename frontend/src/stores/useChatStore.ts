import { create } from 'zustand';
import type { Message, Step, StepStatus } from '../components/molecules';

export interface Session {
  id: string;
  title: string;
  createdAt: string;
  updatedAt: string;
  messages: Message[];
}

interface ChatState {
  sessionId: string | null;
  messages: Message[];
  mode: 'quick' | 'deep';
  isStreaming: boolean;
  steps: Step[];
  error: string | null;

  // Actions
  setMode: (mode: 'quick' | 'deep') => void;
  addUserMessage: (content: string) => string;
  updateAssistantMessage: (messageId: string, content: string) => void;
  appendToAssistantMessage: (messageId: string, token: string) => void;
  setFeedback: (messageId: string, feedback: 'positive' | 'negative' | null) => void;
  clearMessages: () => void;
  setSteps: (steps: Step[]) => void;
  updateStep: (stepId: string, status: StepStatus) => void;
  setError: (error: string | null) => void;
  setStreaming: (isStreaming: boolean) => void;
  loadSession: (session: Session) => void;
  resetSteps: () => void;
}

const initialSteps: Step[] = [
  { id: 'understand', name: '理解问题', status: 'pending' },
  { id: 'retrieve', name: '检索知识库', status: 'pending' },
  { id: 'rerank', name: '筛选结果', status: 'pending' },
  { id: 'generate', name: '生成回答', status: 'pending' },
];

export const useChatStore = create<ChatState>((set) => ({
  sessionId: null,
  messages: [],
  mode: 'quick',
  isStreaming: false,
  steps: initialSteps,
  error: null,

  setMode: (mode) => set({ mode }),

  addUserMessage: (content) => {
    const messageId = `msg-${Date.now()}-${Math.random().toString(36).slice(2, 9)}`;
    const message: Message = {
      id: messageId,
      role: 'user',
      content,
      timestamp: new Date(),
    };
    set((state) => ({
      messages: [...state.messages, message],
    }));
    return messageId;
  },

  updateAssistantMessage: (messageId, content) => {
    set((state) => ({
      messages: state.messages.map((msg) =>
        msg.id === messageId ? { ...msg, content } : msg
      ),
    }));
  },

  appendToAssistantMessage: (messageId, token) => {
    set((state) => ({
      messages: state.messages.map((msg) =>
        msg.id === messageId ? { ...msg, content: msg.content + token } : msg
      ),
    }));
  },

  setFeedback: (messageId, feedback) => {
    set((state) => ({
      messages: state.messages.map((msg) =>
        msg.id === messageId ? { ...msg, feedback } : msg
      ),
    }));
  },

  clearMessages: () =>
    set({
      messages: [],
      sessionId: null,
      steps: initialSteps,
      error: null,
      isStreaming: false,
    }),

  setSteps: (steps) => set({ steps }),

  updateStep: (stepId, status) => {
    set((state) => ({
      steps: state.steps.map((step) =>
        step.id === stepId ? { ...step, status } : step
      ),
    }));
  },

  setError: (error) => set({ error }),

  setStreaming: (isStreaming) => set({ isStreaming }),

  loadSession: (session) =>
    set({
      sessionId: session.id,
      messages: session.messages,
    }),

  resetSteps: () => set({ steps: initialSteps }),
}));