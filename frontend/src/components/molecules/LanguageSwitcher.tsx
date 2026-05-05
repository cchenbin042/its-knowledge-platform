import { Globe } from 'lucide-react';
import clsx from 'clsx';
import { useI18nStore, type Language } from '../../stores';

export function LanguageSwitcher() {
  const { language, setLanguage } = useI18nStore();

  const languages: { code: Language; label: string }[] = [
    { code: 'zh', label: '中文' },
    { code: 'en', label: 'EN' },
  ];

  return (
    <div className="flex items-center gap-1 px-3 py-1.5 bg-slate-800 rounded-lg">
      <Globe className="w-4 h-4 text-slate-400" />
      {languages.map((lang) => (
        <button
          key={lang.code}
          onClick={() => setLanguage(lang.code)}
          className={clsx(
            'px-2 py-1 text-sm font-medium rounded transition-colors',
            language === lang.code
              ? 'bg-primary-600 text-white'
              : 'text-slate-400 hover:text-white hover:bg-slate-700'
          )}
          title={lang.code === 'zh' ? 'Chinese' : 'English'}
        >
          {lang.label}
        </button>
      ))}
    </div>
  );
}