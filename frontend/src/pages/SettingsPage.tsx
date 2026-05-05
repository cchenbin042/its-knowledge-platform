import { SettingsForm } from '../components/organisms/SettingsForm';
import { useTranslation } from '../stores';

export function SettingsPage() {
  const { t } = useTranslation();

  return (
    <div className="flex-1 overflow-auto bg-slate-50">
      <div className="max-w-4xl mx-auto px-6 py-8">
        {/* Page Header */}
        <div className="mb-8">
          <h1 className="text-2xl font-bold text-slate-900">{t('settings.title')}</h1>
          <p className="text-slate-600 mt-1">
            {t('settings.subtitle')}
          </p>
        </div>

        {/* Settings Form */}
        <SettingsForm />
      </div>
    </div>
  );
}