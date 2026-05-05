import { useEffect } from 'react';
import { StatsDashboard } from '../components/organisms/StatsDashboard';
import { useStatsStore, useTranslation } from '../stores';

export function StatsPage() {
  const { overview, dailyStats, topQuestions, retrievalStats, feedbackStats, loading, error, fetchStats } =
    useStatsStore();
  const { t } = useTranslation();

  useEffect(() => {
    fetchStats();
  }, [fetchStats]);

  if (error) {
    return (
      <div className="h-full flex items-center justify-center">
        <div className="text-center">
          <p className="text-red-500 mb-4">{error}</p>
          <button
            onClick={() => fetchStats()}
            className="px-4 py-2 bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors"
          >
            {t('common.retry')}
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="h-full overflow-auto">
      <div className="p-6">
        <div className="mb-6">
          <h1 className="text-2xl font-bold text-slate-900">{t('stats.title')}</h1>
          <p className="text-slate-500 mt-1">{t('stats.subtitle')}</p>
        </div>
        <StatsDashboard
          overview={overview}
          dailyStats={dailyStats}
          topQuestions={topQuestions}
          retrievalStats={retrievalStats}
          feedbackStats={feedbackStats}
          loading={loading}
        />
      </div>
    </div>
  );
}