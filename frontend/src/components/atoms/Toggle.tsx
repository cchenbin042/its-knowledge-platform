import clsx from 'clsx';

interface ToggleProps {
  checked: boolean;
  onChange: (checked: boolean) => void;
  label?: string;
  disabled?: boolean;
  size?: 'sm' | 'md';
}

export function Toggle({
  checked,
  onChange,
  label,
  disabled = false,
  size = 'md',
}: ToggleProps) {
  const sizes = {
    sm: {
      toggle: 'w-8 h-4',
      dot: 'w-3 h-3',
      translate: checked ? 'translate-x-4' : 'translate-x-0.5',
    },
    md: {
      toggle: 'w-11 h-6',
      dot: 'w-5 h-5',
      translate: checked ? 'translate-x-5' : 'translate-x-0.5',
    },
  };

  return (
    <label className={clsx(
      'inline-flex items-center gap-2',
      disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'
    )}>
      <button
        type="button"
        role="switch"
        aria-checked={checked}
        disabled={disabled}
        onClick={() => !disabled && onChange(!checked)}
        className={clsx(
          'relative inline-flex shrink-0 rounded-full transition-colors duration-200 ease-in-out',
          'focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2',
          sizes[size].toggle,
          checked ? 'bg-primary-600' : 'bg-slate-200'
        )}
      >
        <span
          className={clsx(
            'inline-block rounded-full bg-white shadow-sm transform transition-transform duration-200 ease-in-out',
            sizes[size].dot,
            sizes[size].translate,
            'mt-0.5'
          )}
        />
      </button>
      {label && (
        <span className="text-sm text-slate-700">{label}</span>
      )}
    </label>
  );
}