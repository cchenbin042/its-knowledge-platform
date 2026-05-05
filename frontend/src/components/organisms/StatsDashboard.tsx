import { Card, CardHeader, CardBody, Spinner } from '../atoms';
import { StatCard } from '../molecules/StatCard';
import { useTranslation } from '../../stores';
import type { StatsOverview, DailyStat, TopQuestion, RetrievalStats, FeedbackStats } from '../../types';

interface StatsDashboardProps {
  overview: StatsOverview;
  dailyStats: DailyStat[];
  topQuestions: TopQuestion[];
  retrievalStats: RetrievalStats;
  feedbackStats: FeedbackStats;
  loading?: boolean;
}

// Simple CSS Bar Chart
function BarChart({ data }: { data: DailyStat[] }) {
  const maxCount = Math.max(...data.map((d) => d.count), 1);

  return (
    <div className="flex items-end justify-between gap-1 h-40">
      {data.map((item, index) => {
        const height = (item.count / maxCount) * 100;
        return (
          <div
            key={index}
            className="flex-1 flex flex-col items-center gap-1"
          >
            <div className="w-full relative h-32 flex items-end">
              <div
                className="w-full rounded-t bg-gradient-to-t from-primary-500 to-primary-400 hover:from-primary-600 hover:to-primary-500 transition-colors cursor-pointer group"
                style={{ height: `${Math.max(height, 4)}%` }}
              >
                <div className="absolute -top-6 left-1/2 -translate-x-1/2 bg-slate-700 text-white text-xs px-1.5 py-0.5 rounded opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap">
                  {item.count}
                </div>
              </div>
            </div>
            <span className="text-xs text-slate-500 truncate w-full text-center">
              {new Date(item.date).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}
            </span>
          </div>
        );
      })}
    </div>
  );
}

// Top Questions List
function TopQuestionsList({ questions }: { questions: TopQuestion[] }) {
  const { t } = useTranslation();

  if (questions.length === 0) {
    return (
      <div className="text-center text-slate-500 py-8">
        {t('stats.noQueries')}
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {questions.map((item, index) => (
        <div
          key={index}
          className="flex items-start gap-3 p-3 rounded-lg bg-slate-50 hover:bg-slate-100 transition-colors"
        >
          <span className="flex-shrink-0 w-6 h-6 rounded-full bg-primary-100 text-primary-700 flex items-center justify-center text-sm font-medium">
            {index + 1}
          </span>
          <div className="flex-1 min-w-0">
            <p className="text-sm text-slate-700 truncate">{item.question}</p>
            <p className="text-xs text-slate-500 mt-0.5">{item.count} {t('stats.queries')}</p>
          </div>
        </div>
      ))}
    </div>
  );
}

// Retrieval Distribution (Pie Chart using CSS)
function RetrievalPieChart({ stats }: { stats: RetrievalStats }) {
  const { t } = useTranslation();

  const total = stats.vector + stats.bm25;
  const vectorPercent = total > 0 ? (stats.vector / total) * 100 : 0;
  const bm25Percent = total > 0 ? (stats.bm25 / total) * 100 : 0;

  return (
    <div className="flex items-center gap-6">
      {/* CSS Pie Chart */}
      <div className="relative w-32 h-32">
        <div
          className="absolute inset-0 rounded-full"
          style={{
            background: `conic-gradient(
              #3b82f6 0deg ${vectorPercent * 3.6}deg,
              #10b981 ${vectorPercent * 3.6}deg 360deg
            )`,
          }}
        />
        <div className="absolute inset-4 bg-white rounded-full flex items-center justify-center">
          <span className="text-lg font-semibold text-slate-700">{total}</span>
        </div>
      </div>

      {/* Legend */}
      <div className="space-y-2">
        <div className="flex items-center gap-2">
          <div className="w-3 h-3 rounded-full bg-blue-500" />
          <span className="text-sm text-slate-600">{t('stats.vector')}</span>
          <span className="text-sm font-medium text-slate-900 ml-auto">
            {vectorPercent.toFixed(1)}%
          </span>
        </div>
        <div className="flex items-center gap-2">
          <div className="w-3 h-3 rounded-full bg-green-500" />
          <span className="text-sm text-slate-600">{t('stats.bm25')}</span>
          <span className="text-sm font-medium text-slate-900 ml-auto">
            {bm25Percent.toFixed(1)}%
          </span>
        </div>
      </div>
    </div>
  );
}

// Feedback Stats Bar
function FeedbackBarStats({ stats }: { stats: FeedbackStats }) {
  const { t } = useTranslation();

  const total = stats.positive + stats.negative;
  const positivePercent = total > 0 ? (stats.positive / total) * 100 : 0;

  return (
    <div className="space-y-3">
      {/* Progress Bar */}
      <div className="h-4 bg-slate-200 rounded-full overflow-hidden flex">
        <div
          className="h-full bg-green-500 transition-all duration-500"
          style={{ width: `${positivePercent}%` }}
        />
        <div
          className="h-full bg-red-400 transition-all duration-500"
          style={{ width: `${100 - positivePercent}%` }}
        />
      </div>

      {/* Stats */}
      <div className="flex justify-between text-sm">
        <div className="flex items-center gap-2">
          <div className="w-3 h-3 rounded-full bg-green-500" />
          <span className="text-slate-600">{t('stats.positive')}</span>
          <span className="font-medium text-slate-900">{stats.positive}</span>
        </div>
        <div className="flex items-center gap-2">
          <span className="font-medium text-slate-900">{stats.negative}</span>
          <span className="text-slate-600">{t('stats.negative')}</span>
          <div className="w-3 h-3 rounded-full bg-red-400" />
        </div>
      </div>

      {/* Rate */}
      <p className="text-center text-sm text-slate-500">
        {total > 0 ? (
          <>
            <span className="font-medium text-green-600">{positivePercent.toFixed(1)}%</span>
            {' '}{t('stats.positiveFeedbackRate')}
          </>
        ) : (
          t('stats.noFeedback')
        )}
      </p>
    </div>
  );
}

export function StatsDashboard({
  overview,
  dailyStats,
  topQuestions,
  retrievalStats,
  feedbackStats,
  loading,
}: StatsDashboardProps) {
  const { t } = useTranslation();

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Spinner size="lg" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Overview Row - 4 Stat Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          type="documents"
          value={overview.documents}
          trend={overview.documentsTrend}
        />
        <StatCard
          type="queries"
          value={overview.queries}
          trend={overview.queriesTrend}
        />
        <StatCard
          type="responseTime"
          value={overview.avgResponseTime}
          trend={overview.responseTimeTrend}
        />
        <StatCard
          type="positiveRate"
          value={overview.positiveRate}
          trend={overview.positiveRateTrend}
        />
      </div>

      {/* Charts Row - Daily Trends + Top Questions */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Daily Query Trends */}
        <Card>
          <CardHeader>
            <h3 className="text-lg font-semibold text-slate-900">{t('stats.dailyTrends')}</h3>
            <p className="text-sm text-slate-500 mt-1">{t('stats.last7Days')}</p>
          </CardHeader>
          <CardBody>
            {dailyStats.length > 0 ? (
              <BarChart data={dailyStats} />
            ) : (
              <div className="text-center text-slate-500 py-8">
                {t('stats.noData')}
              </div>
            )}
          </CardBody>
        </Card>

        {/* Top Questions */}
        <Card>
          <CardHeader>
            <h3 className="text-lg font-semibold text-slate-900">{t('stats.topQuestions')}</h3>
            <p className="text-sm text-slate-500 mt-1">{t('stats.mostFrequent')}</p>
          </CardHeader>
          <CardBody className="max-h-72 overflow-y-auto">
            <TopQuestionsList questions={topQuestions} />
          </CardBody>
        </Card>
      </div>

      {/* Retrieval & Feedback Row */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Retrieval Distribution */}
        <Card>
          <CardHeader>
            <h3 className="text-lg font-semibold text-slate-900">{t('stats.retrievalDistribution')}</h3>
            <p className="text-sm text-slate-500 mt-1">{t('stats.vectorVsBm25')}</p>
          </CardHeader>
          <CardBody className="flex items-center justify-center py-6">
            <RetrievalPieChart stats={retrievalStats} />
          </CardBody>
        </Card>

        {/* Feedback Stats */}
        <Card>
          <CardHeader>
            <h3 className="text-lg font-semibold text-slate-900">{t('stats.feedbackStats')}</h3>
            <p className="text-sm text-slate-500 mt-1">{t('stats.userSatisfaction')}</p>
          </CardHeader>
          <CardBody className="py-6">
            <FeedbackBarStats stats={feedbackStats} />
          </CardBody>
        </Card>
      </div>
    </div>
  );
}