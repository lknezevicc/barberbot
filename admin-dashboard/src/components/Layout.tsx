import { useState, useEffect } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  LayoutDashboard,
  Calendar,
  Users,
  Scissors,
  Clock,
  FileText,
  Settings,
  User,
  LogOut,
  Menu,
  X,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import { getMe, logout } from '@/lib/api';
import ErrorBoundary from '@/components/ErrorBoundary';
import type { AuthUser } from '@/types';

const navItems = [
  { to: '/', icon: LayoutDashboard, label: 'Nadzorna ploča' },
  { to: '/bookings', icon: Calendar, label: 'Rezervacije' },
  { to: '/employees', icon: Users, label: 'Zaposlenici' },
  { to: '/services', icon: Scissors, label: 'Usluge' },
  { to: '/schedule', icon: Clock, label: 'Raspored' },
  { to: '/audit-log', icon: FileText, label: 'Revizijski zapisi' },
];

const bottomNavItems = [
  { to: '/settings', icon: Settings, label: 'Postavke' },
  { to: '/profile', icon: User, label: 'Profil' },
];

export default function Layout() {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const navigate = useNavigate();

  const { data: user } = useQuery<AuthUser>({
    queryKey: ['me'],
    queryFn: getMe,
    staleTime: 60_000,
  });

  useEffect(() => {
    function handleResize() {
      if (window.innerWidth >= 1024) setSidebarOpen(false);
    }
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="flex h-screen overflow-hidden">
      {sidebarOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/30 backdrop-blur-sm lg:hidden"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      <aside
        className={cn(
          'glass-dark fixed inset-y-0 left-0 z-50 w-64 flex flex-col transition-transform duration-300 lg:relative lg:translate-x-0',
          sidebarOpen ? 'translate-x-0' : '-translate-x-full',
        )}
      >
        <div className="flex items-center gap-3 px-6 py-5 border-b border-white/10">
          <div className="h-9 w-9 rounded-xl bg-gradient-to-br from-indigo-400 to-purple-500 flex items-center justify-center">
            <Scissors className="h-5 w-5 text-white" />
          </div>
          <div>
            <p className="text-sm font-bold text-(--text-on-dark)">
              {user?.tenantName || 'BarberBot'}
            </p>
            <p className="text-xs text-(--text-muted-dark)">Admin Panel</p>
          </div>
          <button
            onClick={() => setSidebarOpen(false)}
            className="ml-auto p-1 rounded-lg hover:bg-white/10 lg:hidden"
          >
            <X className="h-5 w-5 text-(--text-muted-dark)" />
          </button>
        </div>

        {user && (
          <div className="px-4 py-4 border-b border-white/10">
            <div className="flex items-center gap-3">
              <div className="h-9 w-9 rounded-full bg-gradient-to-br from-indigo-400 to-purple-400 flex items-center justify-center text-white text-sm font-bold">
                {user.firstName?.charAt(0) || 'A'}
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-(--text-on-dark) truncate">
                  {user.firstName} {user.lastName}
                </p>
                <p className="text-xs text-(--text-muted-dark) truncate">{user.role}</p>
              </div>
            </div>
          </div>
        )}

        <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
          {navItems.map(({ to, icon: Icon, label }) => (
            <NavLink
              key={to}
              to={to}
              end={to === '/'}
              onClick={() => setSidebarOpen(false)}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all duration-200',
                  isActive
                    ? 'bg-white/15 text-white border-l-3 border-(--primary-400)'
                    : 'text-(--text-muted-dark) hover:bg-white/8 hover:text-white',
                )
              }
            >
              <Icon className="h-4.5 w-4.5 flex-shrink-0" />
              {label}
            </NavLink>
          ))}
        </nav>

        <div className="px-3 py-4 border-t border-white/10 space-y-1">
          {bottomNavItems.map(({ to, icon: Icon, label }) => (
            <NavLink
              key={to}
              to={to}
              onClick={() => setSidebarOpen(false)}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all duration-200',
                  isActive
                    ? 'bg-white/15 text-white'
                    : 'text-(--text-muted-dark) hover:bg-white/8 hover:text-white',
                )
              }
            >
              <Icon className="h-4.5 w-4.5 flex-shrink-0" />
              {label}
            </NavLink>
          ))}
          <button
            onClick={handleLogout}
            className="flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium text-red-300 hover:bg-red-500/15 hover:text-red-200 transition-all duration-200 w-full"
          >
            <LogOut className="h-4.5 w-4.5 flex-shrink-0" />
            Odjava
          </button>
        </div>
      </aside>

      <main className="flex-1 overflow-y-auto">
        <header className="sticky top-0 z-30 glass px-6 py-4 lg:hidden">
          <button onClick={() => setSidebarOpen(true)} className="p-2 rounded-xl hover:bg-white/10">
            <Menu className="h-5 w-5" />
          </button>
        </header>
        <div className="p-6 lg:p-8 animate-fade-in">
          <ErrorBoundary>
            <Outlet />
          </ErrorBoundary>
        </div>
      </main>
    </div>
  );
}
