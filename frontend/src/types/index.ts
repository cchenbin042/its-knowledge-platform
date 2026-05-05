// API Response wrapper
export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

// Document entity
export interface Document {
  id: number;
  title: string;
  contentHash: string;
  filePath: string;
  chunkCount: number;
  createdAt: string;
  content?: string; // Available when fetching single document
}

// Document preview response
export interface DocumentPreview {
  document: Document;
  totalChunks: number;
  contentPreview: string;
  chunks: Chunk[];
}

// Document chunk
export interface Chunk {
  id: number;
  documentId: number;
  content: string;
  chunkIndex: number;
}

// Retrieval result
export interface RetrievalResult {
  documentId: string;
  title: string;
  content: string;
  score: number;
  source: 'vector' | 'bm25';
}

// Query response
export interface QueryResponse {
  question: string;
  answer: string;
  sources: RetrievalResult[];
}

// Query request
export interface QueryRequest {
  question: string;
}

// SSE Event types
export type SseEventType =
  | 'retrieval_start'
  | 'retrieval_vector'
  | 'retrieval_bm25'
  | 'retrieval_fused'
  | 'context_built'
  | 'llm_token'
  | 'llm_done'
  | 'error'
  | 'complete';

// SSE Event
export interface SseEvent {
  eventType: SseEventType;
  message: string;
  data: unknown;
}

// SSE State for streaming query
export interface StreamingState {
  status: 'idle' | 'retrieving' | 'generating' | 'complete' | 'error';
  progress: string;
  tokens: string[];
  sources: RetrievalResult[];
  error?: string;
}

// Chat Message
export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: string;
  feedback?: 'positive' | 'negative' | null;
  sources?: RetrievalResult[];
}

// Session
export interface Session {
  id: string;
  title: string;
  createdAt: string;
  updatedAt: string;
  messages: ChatMessage[];
}

// Feedback request
export interface FeedbackRequest {
  sessionId: string;
  messageId: string;
  type: 'positive' | 'negative';
  comment?: string;
}

// Stats types
export * from './stats';