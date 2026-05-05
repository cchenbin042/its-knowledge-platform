import { create } from 'zustand';
import type { StatsOverview, DailyStat, TopQuestion, RetrievalStats, FeedbackStats } from '../types';
import { api } from '../api';

interface StatsState {
  overview: StatsOverview;
  dailyStats: DailyStat[];
  topQuestions: TopQuestion[];
  retrievalStats: RetrievalStats;
  feedbackStats: FeedbackStats;
  loading: boolean;
  error: string | null;

  fetchStats: () => Promise<void>;
}

const initialOverview: StatsOverview = {
  documents: 0,
  queries: 0,
  avgResponseTime: 0,
  positiveRate: 0,
};

const initialRetrievalStats: RetrievalStats = {
  vector: 0,
  bm25: 0,
};

const initialFeedbackStats: FeedbackStats = {
  positive: 0,
  negative: 0,
};

export const useStatsStore = create<StatsState>((set) => ({
  overview: initialOverview,
  dailyStats: [],
  topQuestions: [],
  retrievalStats: initialRetrievalStats,
  feedbackStats: initialFeedbackStats,
  loading: false,
  error: null,

  fetchStats: async () => {
    set({ loading: true, error: null });
    try {
      const [overview, dailyStats, topQuestions, retrievalStats, feedbackStats] = await Promise.all([
        api.getStats(),
        api.getDailyStats(),
        api.getTopQuestions(),
        api.getRetrievalStats(),
        api.getFeedbackStats(),
      ]);

      set({
        overview,
        dailyStats,
        topQuestions,
        retrievalStats,
        feedbackStats,
        loading: false,
      });
    } catch (error) {
      set({
        error: error instanceof Error ? error.message : 'Failed to fetch stats',
        loading: false,
      });
    }
  },
}));