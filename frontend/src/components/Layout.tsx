import { NavLink, Outlet } from 'react-router-dom';
import { Search, FileText, Database, Github, BarChart3, Settings } from 'lucide-react';
import clsx from 'clsx';
import { LanguageSwitcher } from './molecules/LanguageSwitcher';
import { useTranslation } from '../stores';

export function Layout() {
  const { t } = useTranslation();

  const navItems = [
    { to: '/', icon: Search, label: t('nav.query') },
    { to: '/documents', icon: FileText, label: t('nav.documents') },
    { to: '/stats', icon: BarChart3, label: t('nav.stats') },
    { to: '/settings', icon: Settings, label: t('nav.settings') },
  ];

  return (
    <div className="min-h-screen bg-slate-50 flex">
      {/* Sidebar */}
      <aside className="w-64 bg-slate-900 text-white flex flex-col shrink-0">
        {/* Logo */}
        <div className="p-6 border-b border-slate-700">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-primary-500 flex items-center justify-center">
              <Database className="w-5 h-5 text-white" />
            </div>
            <div>
              <h1 className="font-semibold text-lg">{t('brand.name')}</h1>
              <p className="text-xs text-slate-400">{t('brand.tagline')}</p>
            </div>
          </div>
        </div>

        {/* Navigation */}
        <nav className="flex-1 p-4">
          <ul className="space-y-1">
            {navItems.map((item) => (
              <li key={item.to}>
                <NavLink
                  to={item.to}
                  className={({ isActive }) =>
                    clsx(
                      'flex items-center gap-3 px-4 py-3 rounded-lg text-sm font-medium transition-colors',
                      isActive
                        ? 'bg-primary-600 text-white'
                        : 'text-slate-300 hover:bg-slate-800 hover:text-white'
                    )
                  }
                >
                  <item.icon className="w-5 h-5" />
                  {item.label}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>

        {/* Footer */}
        <div className="p-4 border-t border-slate-700">
          <div className="flex items-center justify-between">
            <LanguageSwitcher />
            <a
              href="https://github.com"
              target="_blank"
              rel="noopener noreferrer"
              className="flex items-center gap-2 text-slate-400 hover:text-white text-sm"
            >
              <Github className="w-4 h-4" />
            </a>
          </div>
        </div>
      </aside>

      {/* Main content */}
      <main className="flex-1 flex flex-col overflow-hidden">
        <Outlet />
      </main>
    </div>
  );
}