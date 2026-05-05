import { HTMLAttributes, forwardRef } from 'react';
import { FileText, MessageSquare, Clock, ThumbsUp, TrendingUp, TrendingDown } from 'lucide-react';
import { useTranslation } from '../../stores';
import clsx from 'clsx';

type StatType = 'documents' | 'queries' | 'responseTime' | 'positiveRate';

interface StatCardProps extends HTMLAttributes<HTMLDivElement> {
  type: StatType;
  value: number;
  trend?: number;
  loading?: boolean;
}

export const StatCard = forwardRef<HTMLDivElement, StatCardProps>(
  ({ className, type, value, trend, loading }, ref) => {
    const { t } = useTranslation();

    const statConfig: Record<StatType, { icon: typeof FileText; labelKey: string; color: string; unit: string }> = {
      documents: {
        icon: FileText,
        labelKey: 'stats.documents',
        color: 'text-blue-500',
        unit: '',
      },
      queries: {
        icon: MessageSquare,
        labelKey: 'stats.totalQueries',
        color: 'text-green-500',
        unit: '',
      },
      responseTime: {
        icon: Clock,
        labelKey: 'stats.avgResponseTime',
        color: 'text-amber-500',
        unit: 'ms',
      },
      positiveRate: {
        icon: ThumbsUp,
        labelKey: 'stats.positiveRate',
        color: 'text-purple-500',
        unit: '%',
      },
    };

    const config = statConfig[type];
    const Icon = config.icon;

    const formatValue = (val: number): string => {
      if (type === 'responseTime') {
        return val.toFixed(0);
      }
      if (type === 'positiveRate') {
        return val.toFixed(1);
      }
      return val.toLocaleString();
    };

    return (
      <div
        ref={ref}
        className={clsx(
          'bg-white rounded-xl border border-slate-200 p-6 flex items-center gap-4',
          className
        )}
      >
        <div className={clsx('p-3 rounded-lg bg-slate-50', config.color)}>
          <Icon className="w-6 h-6" />
        </div>
        <div className="flex-1 min-w-0">
          <p className="text-sm text-slate-500 truncate">{t(config.labelKey)}</p>
          {loading ? (
            <div className="h-8 w-24 bg-slate-100 rounded animate-pulse mt-1" />
          ) : (
            <div className="flex items-baseline gap-2">
              <span className="text-2xl font-semibold text-slate-900">
                {formatValue(value)}
              </span>
              <span className="text-sm text-slate-500">{config.unit}</span>
            </div>
          )}
        </div>
        {trend !== undefined && !loading && (
          <div
            className={clsx(
              'flex items-center gap-1 text-sm font-medium',
              trend >= 0 ? 'text-green-600' : 'text-red-600'
            )}
          >
            {trend >= 0 ? (
              <TrendingUp className="w-4 h-4" />
            ) : (
              <TrendingDown className="w-4 h-4" />
            )}
            <span>{Math.abs(trend).toFixed(1)}%</span>
          </div>
        )}
      </div>
    );
  }
);

StatCard.displayName = 'StatCard';