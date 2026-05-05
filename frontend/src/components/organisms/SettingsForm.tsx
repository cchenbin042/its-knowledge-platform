import { useEffect } from 'react';
import { Card, CardHeader, CardBody, Toggle, Spinner, Slider } from '../atoms';
import { useSettingsStore, useTranslation } from '../../stores';

export function SettingsForm() {
  const {
    topK,
    rrfK,
    minRelevance,
    defaultMode,
    collapseSteps,
    llmConfig,
    llmConfigLoading,
    llmConfigError,
    setTopK,
    setRrfK,
    setMinRelevance,
    setDefaultMode,
    setCollapseSteps,
    fetchLlmConfig,
  } = useSettingsStore();

  const { t } = useTranslation();

  useEffect(() => {
    fetchLlmConfig();
  }, [fetchLlmConfig]);

  return (
    <div className="space-y-6 max-w-3xl">
      {/* Retrieval Parameters */}
      <Card>
        <CardHeader>
          <h2 className="text-lg font-semibold text-slate-900">{t('settings.retrievalParams')}</h2>
          <p className="text-sm text-slate-500 mt-1">
            {t('settings.retrievalParamsDesc')}
          </p>
        </CardHeader>
        <CardBody className="space-y-6">
          {/* Top-K */}
          <div className="space-y-1">
            <Slider
              label={t('settings.topK')}
              min={1}
              max={50}
              value={topK}
              onChange={setTopK}
              hint={t('settings.topKHint')}
              formatValue={(v: number) => `${v} docs`}
            />
            <p className="text-xs text-slate-500">
              {t('settings.topKDesc')}
            </p>
          </div>

          {/* RRF K */}
          <div className="space-y-1">
            <Slider
              label={t('settings.rrfK')}
              min={20}
              max={100}
              value={rrfK}
              onChange={setRrfK}
              hint={t('settings.rrfKHint')}
            />
            <p className="text-xs text-slate-500">
              {t('settings.rrfKDesc')}
            </p>
          </div>

          {/* Minimum Relevance */}
          <div className="space-y-1">
            <Slider
              label={t('settings.minRelevance')}
              min={0}
              max={1}
              step={0.05}
              value={minRelevance}
              onChange={setMinRelevance}
              hint={t('settings.minRelevanceHint')}
              formatValue={(v: number) => `${(v * 100).toFixed(0)}%`}
            />
            <p className="text-xs text-slate-500">
              {t('settings.minRelevanceDesc')}
            </p>
          </div>
        </CardBody>
      </Card>

      {/* UI Preferences */}
      <Card>
        <CardHeader>
          <h2 className="text-lg font-semibold text-slate-900">{t('settings.uiPreferences')}</h2>
          <p className="text-sm text-slate-500 mt-1">
            {t('settings.uiPreferencesDesc')}
          </p>
        </CardHeader>
        <CardBody className="space-y-4">
          {/* Default Query Mode */}
          <div className="flex items-center justify-between py-2">
            <div>
              <h3 className="text-sm font-medium text-slate-700">{t('settings.defaultMode')}</h3>
              <p className="text-xs text-slate-500 mt-0.5">
                {t('settings.defaultModeDesc')}
              </p>
            </div>
            <div className="flex items-center gap-2 bg-slate-100 rounded-lg p-1">
              <button
                onClick={() => setDefaultMode('quick')}
                className={`px-3 py-1.5 text-sm font-medium rounded-md transition-colors ${
                  defaultMode === 'quick'
                    ? 'bg-white text-primary-600 shadow-sm'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                {t('settings.quick')}
              </button>
              <button
                onClick={() => setDefaultMode('deep')}
                className={`px-3 py-1.5 text-sm font-medium rounded-md transition-colors ${
                  defaultMode === 'deep'
                    ? 'bg-white text-primary-600 shadow-sm'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                {t('settings.deep')}
              </button>
            </div>
          </div>

          {/* Collapse Steps by Default */}
          <div className="flex items-center justify-between py-2 border-t border-slate-100">
            <div>
              <h3 className="text-sm font-medium text-slate-700">{t('settings.collapseSteps')}</h3>
              <p className="text-xs text-slate-500 mt-0.5">
                {t('settings.collapseStepsDesc')}
              </p>
            </div>
            <Toggle
              checked={collapseSteps}
              onChange={setCollapseSteps}
            />
          </div>
        </CardBody>
      </Card>

      {/* LLM Configuration (Read-only) */}
      <Card>
        <CardHeader>
          <h2 className="text-lg font-semibold text-slate-900">{t('settings.llmConfig')}</h2>
          <p className="text-sm text-slate-500 mt-1">
            {t('settings.llmConfigDesc')}
          </p>
        </CardHeader>
        <CardBody>
          {llmConfigLoading ? (
            <div className="flex items-center justify-center py-8">
              <Spinner size="md" />
            </div>
          ) : llmConfigError ? (
            <div className="text-center py-8 text-red-500">
              <p>{t('settings.loadFailed')}</p>
              <button
                onClick={fetchLlmConfig}
                className="mt-2 text-sm text-primary-600 hover:underline"
              >
                {t('common.retry')}
              </button>
            </div>
          ) : (
            <div className="space-y-4">
              {/* API Base URL */}
              <div className="flex items-center justify-between py-2 border-b border-slate-100">
                <div>
                  <h3 className="text-sm font-medium text-slate-700">{t('settings.apiBaseUrl')}</h3>
                  <p className="text-xs text-slate-500 mt-0.5">
                    {t('settings.apiBaseUrlDesc')}
                  </p>
                </div>
                <code className="text-sm text-slate-600 bg-slate-100 px-2 py-1 rounded">
                  {llmConfig.baseUrl || t('settings.notConfigured')}
                </code>
              </div>

              {/* Embedding Model */}
              <div className="flex items-center justify-between py-2 border-b border-slate-100">
                <div>
                  <h3 className="text-sm font-medium text-slate-700">{t('settings.embeddingModel')}</h3>
                  <p className="text-xs text-slate-500 mt-0.5">
                    {t('settings.embeddingModelDesc')}
                  </p>
                </div>
                <code className="text-sm text-slate-600 bg-slate-100 px-2 py-1 rounded">
                  {llmConfig.embeddingModel || t('settings.notConfigured')}
                </code>
              </div>

              {/* Chat Model */}
              <div className="flex items-center justify-between py-2">
                <div>
                  <h3 className="text-sm font-medium text-slate-700">{t('settings.chatModel')}</h3>
                  <p className="text-xs text-slate-500 mt-0.5">
                    {t('settings.chatModelDesc')}
                  </p>
                </div>
                <code className="text-sm text-slate-600 bg-slate-100 px-2 py-1 rounded">
                  {llmConfig.chatModel || t('settings.notConfigured')}
                </code>
              </div>
            </div>
          )}
        </CardBody>
      </Card>
    </div>
  );
}