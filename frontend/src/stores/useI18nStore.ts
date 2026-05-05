import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import en from '../locales/en.json';
import zh from '../locales/zh.json';

export type Language = 'en' | 'zh';

type TranslationKeys = typeof en;

const translations: Record<Language, TranslationKeys> = {
  en,
  zh,
};

interface I18nState {
  language: Language;
  t: TranslationKeys;
  setLanguage: (lang: Language) => void;
}

// Helper function to get nested value from object using dot notation
function getNestedValue(obj: Record<string, unknown>, path: string): string {
  const keys = path.split('.');
  let current: unknown = obj;

  for (const key of keys) {
    if (current && typeof current === 'object' && key in current) {
      current = (current as Record<string, unknown>)[key];
    } else {
      return path; // Return the key itself if not found
    }
  }

  return typeof current === 'string' ? current : path;
}

// Interpolation function to replace {{key}} with values
function interpolate(template: string, values: Record<string, string | number>): string {
  return template.replace(/\{\{(\w+)\}\}/g, (_, key) => {
    return values[key] !== undefined ? String(values[key]) : `{{${key}}}`;
  });
}

// Export a hook that provides translation function
export function useTranslation() {
  const { language, t } = useI18nStore();

  const translate = (key: string, values?: Record<string, string | number>): string => {
    const template = getNestedValue(t as unknown as Record<string, unknown>, key);
    if (values) {
      return interpolate(template, values);
    }
    return template;
  };

  return { t: translate, language };
}

export const useI18nStore = create<I18nState>()(
  persist(
    (set) => ({
      language: 'zh', // Default to Chinese
      t: translations.zh,

      setLanguage: (lang: Language) => {
        set({
          language: lang,
          t: translations[lang],
        });
      },
    }),
    {
      name: 'its-i18n',
      partialize: (state) => ({
        language: state.language,
      }),
    }
  )
);

// Re-export for convenience
export { useI18nStore as useLanguageStore };