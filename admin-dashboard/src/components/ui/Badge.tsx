import { cn } from '@/lib/utils';
import type { ReactNode } from 'react';
import type { LucideIcon } from 'lucide-react';

// Badge
type BadgeVariant = 'success' | 'warning' | 'danger' | 'info' | 'neutral';

const badgeStyles: Record<BadgeVariant, string> = {
  success: 'bg-emerald-500/15 text-emerald-400 border-emerald-500/20',
  warning: 'bg-amber-500/15 text-amber-400 border-amber-500/20',
  danger: 'bg-red-500/15 text-red-400 border-red-500/20',
  info: 'bg-blue-500/15 text-blue-400 border-blue-500/20',
  neutral: 'bg-slate-500/15 text-slate-400 border-slate-500/20',
};

interface BadgeProps {
  variant?: BadgeVariant;
  children: ReactNode;
  className?: string;
}

export function Badge({ variant = 'neutral', children, className }: BadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium border',
        badgeStyles[variant],
        className,
      )}
    >
      {children}
    </span>
  );
}

// Card
interface CardProps {
  icon: LucideIcon;
  label: string;
  value: string | number;
  trend?: string;
  color?: string;
  className?: string;
}

const colorMap: Record<string, string> = {
  indigo: 'bg-indigo-500/15 text-indigo-400',
  emerald: 'bg-emerald-500/15 text-emerald-400',
  amber: 'bg-amber-500/15 text-amber-400',
  red: 'bg-red-500/15 text-red-400',
  blue: 'bg-blue-500/15 text-blue-400',
  purple: 'bg-purple-500/15 text-purple-400',
};

export function Card({ icon: Icon, label, value, trend, color = 'indigo', className }: CardProps) {
  return (
    <div className={cn('glass-card rounded-2xl p-5', className)}>
      <div className="flex items-center justify-between">
        <div className={cn('p-2.5 rounded-xl', colorMap[color] || colorMap.indigo)}>
          <Icon className="h-5 w-5" />
        </div>
        {trend && <span className="text-xs font-medium text-emerald-400">{trend}</span>}
      </div>
      <div className="mt-4">
        <p className="text-2xl font-bold text-(--text-primary)">{value}</p>
        <p className="text-sm text-(--text-secondary) mt-0.5">{label}</p>
      </div>
    </div>
  );
}

// PageHeader
interface PageHeaderProps {
  title: string;
  description?: string;
  action?: ReactNode;
}

export function PageHeader({ title, description, action }: PageHeaderProps) {
  return (
    <div className="flex items-center justify-between mb-6">
      <div>
        <h1 className="text-2xl font-bold text-(--text-primary)">{title}</h1>
        {description && <p className="text-sm text-(--text-secondary) mt-1">{description}</p>}
      </div>
      {action}
    </div>
  );
}
