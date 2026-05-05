import axios, { AxiosInstance } from 'axios';
import type {
  ApiResponse,
  Document,
  DocumentPreview,
  QueryResponse,
  Session,
  FeedbackRequest,
  StatsOverview,
  DailyStat,
  TopQuestion,
  RetrievalStats,
  FeedbackStats,
} from '../types';

const API_BASE = '/api';

class ApiClient {
  private client: AxiosInstance;

  constructor() {
    this.client = axios.create({
      baseURL: API_BASE,
      timeout: 60000,
      headers: {
        'Content-Type': 'application/json',
      },
    });
  }

  // Query
  async query(question: string): Promise<QueryResponse> {
    const response = await this.client.post<ApiResponse<QueryResponse>>('/query', {
      question,
    });
    return response.data.data;
  }

  // Documents
  async getDocuments(): Promise<Document[]> {
    const response = await this.client.get<ApiResponse<Document[]>>('/documents');
    return response.data.data;
  }

  async getDocument(title: string): Promise<Document> {
    const response = await this.client.get<ApiResponse<Document>>(`/documents/${encodeURIComponent(title)}`);
    return response.data.data;
  }

  async getDocumentPreview(title: string): Promise<DocumentPreview> {
    const response = await this.client.get<ApiResponse<DocumentPreview>>(`/documents/${encodeURIComponent(title)}/preview`);
    return response.data.data;
  }

  async uploadDocument(file: File): Promise<Document> {
    const formData = new FormData();
    formData.append('file', file);
    const response = await this.client.post<ApiResponse<Document>>('/documents/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data.data;
  }

  async deleteDocument(title: string): Promise<void> {
    await this.client.delete(`/documents/${encodeURIComponent(title)}`);
  }

  // Sessions
  async getSessions(): Promise<Session[]> {
    const response = await this.client.get<ApiResponse<Session[]>>('/sessions');
    return response.data.data;
  }

  async getSession(id: string): Promise<Session> {
    const response = await this.client.get<ApiResponse<Session>>(`/sessions/${id}`);
    return response.data.data;
  }

  async createSession(): Promise<Session> {
    const response = await this.client.post<ApiResponse<Session>>('/sessions');
    return response.data.data;
  }

  // Feedback
  async submitFeedback(request: FeedbackRequest): Promise<void> {
    await this.client.post('/feedback', request);
  }

  // Stats
  async getStats(): Promise<StatsOverview> {
    const response = await this.client.get<ApiResponse<StatsOverview>>('/stats');
    return response.data.data;
  }

  async getDailyStats(): Promise<DailyStat[]> {
    const response = await this.client.get<ApiResponse<DailyStat[]>>('/stats/daily');
    return response.data.data;
  }

  async getTopQuestions(): Promise<TopQuestion[]> {
    const response = await this.client.get<ApiResponse<TopQuestion[]>>('/stats/top-questions');
    return response.data.data;
  }

  async getRetrievalStats(): Promise<RetrievalStats> {
    const response = await this.client.get<ApiResponse<RetrievalStats>>('/stats/retrieval');
    return response.data.data;
  }

  async getFeedbackStats(): Promise<FeedbackStats> {
    const response = await this.client.get<ApiResponse<FeedbackStats>>('/stats/feedback');
    return response.data.data;
  }

  // Streaming query with POST (using fetch + ReadableStream)
  async *streamQuery(question: string): AsyncGenerator<{ eventType: string; data: unknown }> {
    const response = await fetch(`${API_BASE}/query/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
      },
      body: JSON.stringify({ question }),
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const reader = response.body?.getReader();
    if (!reader) {
      throw new Error('No reader available');
    }

    const decoder = new TextDecoder();
    let buffer = '';

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });

      // Parse SSE events
      const lines = buffer.split('\n');
      buffer = lines.pop() || '';

      let eventType = '';
      let data = '';

      for (const line of lines) {
        if (line.startsWith('event:')) {
          eventType = line.slice(6).trim();
        } else if (line.startsWith('data:')) {
          data = line.slice(5).trim();
        } else if (line === '' && eventType && data) {
          try {
            yield { eventType, data: JSON.parse(data) };
          } catch {
            yield { eventType, data };
          }
          eventType = '';
          data = '';
        }
      }
    }
  }
}

export const api = new ApiClient();