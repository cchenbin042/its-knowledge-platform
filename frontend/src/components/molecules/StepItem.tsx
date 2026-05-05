import clsx from 'clsx';
import { CheckCircle, Circle, Loader2, AlertCircle } from 'lucide-react';

export type StepStatus = 'pending' | 'running' | 'completed' | 'error';

export interface Step {
  id: string;
  name: string;
  status: StepStatus;
}

interface StepItemProps {
  step: Step;
  isLast?: boolean;
}

export function StepItem({ step, isLast = false }: StepItemProps) {
  const statusConfig = {
    pending: {
      icon: Circle,
      textClass: 'text-slate-400',
      iconClass: 'text-slate-300',
    },
    running: {
      icon: Loader2,
      textClass: 'text-blue-600 font-medium',
      iconClass: 'text-blue-500 animate-spin',
    },
    completed: {
      icon: CheckCircle,
      textClass: 'text-green-600',
      iconClass: 'text-green-500',
    },
    error: {
      icon: AlertCircle,
      textClass: 'text-red-600',
      iconClass: 'text-red-500',
    },
  };

  const config = statusConfig[step.status];
  const Icon = config.icon;

  return (
    <div className="flex items-center gap-2">
      <Icon className={clsx('w-4 h-4', config.iconClass)} />
      <span className={clsx('text-sm', config.textClass)}>{step.name}</span>
      {!isLast && (
        <div className="flex-1 h-px bg-slate-200 mx-2" />
      )}
    </div>
  );
}