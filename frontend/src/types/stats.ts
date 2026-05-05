// Stats Overview
export interface StatsOverview {
  documents: number;
  queries: number;
  avgResponseTime: number;
  positiveRate: number;
  // Trend data
  documentsTrend?: number;
  queriesTrend?: number;
  responseTimeTrend?: number;
  positiveRateTrend?: number;
}

// Daily statistics
export interface DailyStat {
  date: string;
  count: number;
}

// Top question
export interface TopQuestion {
  question: string;
  count: number;
}

// Retrieval statistics
export interface RetrievalStats {
  vector: number;
  bm25: number;
}

// Feedback statistics
export interface FeedbackStats {
  positive: number;
  negative: number;
}

// All stats combined
export interface DashboardStats {
  overview: StatsOverview;
  dailyStats: DailyStat[];
  topQuestions: TopQuestion[];
  retrievalStats: RetrievalStats;
  feedbackStats: FeedbackStats;
}