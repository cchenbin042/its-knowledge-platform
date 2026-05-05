import clsx from 'clsx';
import { Loader2, CheckCircle2 } from 'lucide-react';
import { useTranslation } from '../../stores';

export type StepStatus = 'pending' | 'running' | 'completed' | 'error';

export interface Step {
  id: string;
  name: string;
  status: StepStatus;
}

interface RetrievalStepsProps {
  steps?: Step[];
  className?: string;
}

export function RetrievalSteps({ steps, className }: RetrievalStepsProps) {
  const { t } = useTranslation();

  const defaultSteps: Step[] = [
    { id: 'understand', name: '理解问题', status: 'pending' },
    { id: 'retrieve', name: '检索知识库', status: 'pending' },
    { id: 'rerank', name: '筛选结果', status: 'pending' },
    { id: 'generate', name: '生成回答', status: 'pending' },
  ];

  const currentSteps = steps || defaultSteps;

  // Find current running step
  const runningStep = currentSteps.find((s) => s.status === 'running');
  const hasError = currentSteps.some((s) => s.status === 'error');
  const allComplete = currentSteps.every((s) => s.status === 'completed');

  // Calculate progress percentage
  const completedCount = currentSteps.filter((s) => s.status === 'completed').length;
  const progressPercent = allComplete ? 100 : (completedCount / currentSteps.length) * 100;

  if (hasError) {
    return (
      <div className={clsx('flex items-center gap-2 px-3 py-2 bg-red-50 rounded-lg', className)}>
        <span className="text-sm text-red-600">{t('retrieval.errorOccurred')}</span>
      </div>
    );
  }

  if (allComplete) {
    return (
      <div className={clsx('flex items-center gap-2 px-3 py-2 bg-green-50 rounded-lg', className)}>
        <CheckCircle2 className="w-4 h-4 text-green-500" />
        <span className="text-sm text-green-600">{t('query.complete')}</span>
      </div>
    );
  }

  // Show current step with progress bar
  return (
    <div className={clsx('flex flex-col gap-2 px-4 py-3 bg-slate-50 rounded-lg', className)}>
      {/* Current step name */}
      <div className="flex items-center gap-2">
        <Loader2 className="w-4 h-4 text-blue-500 animate-spin" />
        <span className="text-sm text-blue-600 font-medium">
          {runningStep ? runningStep.name : t('retrieval.preparing')}
        </span>
      </div>

      {/* Progress bar */}
      <div className="w-full h-1.5 bg-slate-200 rounded-full overflow-hidden">
        <div
          className="h-full bg-blue-500 rounded-full transition-all duration-300 ease-out"
          style={{ width: `${progressPercent}%` }}
        />
      </div>
    </div>
  );
}

// Helper to set all steps to pending
export function resetSteps(): Step[] {
  return [
    { id: 'understand', name: '理解问题', status: 'pending' },
    { id: 'retrieve', name: '检索知识库', status: 'pending' },
    { id: 'rerank', name: '筛选结果', status: 'pending' },
    { id: 'generate', name: '生成回答', status: 'pending' },
  ];
}

export type { StepStatus };