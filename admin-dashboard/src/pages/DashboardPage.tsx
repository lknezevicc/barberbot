import { useQuery } from '@tanstack/react-query';
import { Calendar, CheckCircle, XCircle, TrendingDown, User, Scissors, Euro } from 'lucide-react';
import { Card, PageHeader, DataTable, Badge, type Column } from '@/components/ui';
import { api } from '@/lib/api';
import type { Stats, Booking } from '@/types';

const statusBadge: Record<string, { variant: 'success' | 'danger' | 'warning' | 'info'; label: string }> = {
  CONFIRMED: { variant: 'success', label: 'Potvrđen' },
  CANCELLED: { variant: 'danger', label: 'Otkazan' },
  HELD: { variant: 'warning', label: 'Na čekanju' },
  EXPIRED: { variant: 'info', label: 'Istekao' },
};

const columns: Column<Booking>[] = [
  { key: 'customerName', header: 'Klijent', render: (b) => b.customerName || '-' },
  { key: 'barber', header: 'Frizer' },
  { key: 'service', header: 'Usluga' },
  { key: 'time', header: 'Vrijeme' },
  {
    key: 'status',
    header: 'Status',
    render: (b) => {
      const badge = statusBadge[b.status] || statusBadge.CONFIRMED;
      return <Badge variant={badge.variant}>{badge.label}</Badge>;
    },
  },
];

export default function DashboardPage() {
  const { data: stats, isLoading: statsLoading } = useQuery<Stats>({
    queryKey: ['stats'],
    queryFn: () => api.get('/bookings/stats'),
  });

  const { data: recent, isLoading: recentLoading } = useQuery<Booking[]>({
    queryKey: ['bookings-recent'],
    queryFn: () => api.get('/bookings/recent'),
  });

  if (statsLoading) {
    return (
      <div className="space-y-6">
        <div className="skeleton h-8 w-48" />
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {[1, 2, 3, 4, 5, 6].map(i => <div key={i} className="skeleton h-32 rounded-2xl" />)}
        </div>
        <div className="skeleton h-64 rounded-2xl" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <PageHeader title="Nadzorna ploča" description="Pregled poslovanja vašeg salona" />

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        <Card icon={Calendar} label="Danas rezervacija" value={stats?.todayBookings ?? 0} color="indigo" />
        <Card icon={Calendar} label="Ovaj tjedan" value={stats?.thisWeekBookings ?? 0} color="cyan" />
        <Card icon={CheckCircle} label="Ukupno potvrđenih" value={stats?.totalConfirmed ?? 0} color="emerald" />
        <Card icon={XCircle} label="Ukupno otkazanih" value={stats?.totalCancelled ?? 0} color="red" />
        <Card icon={TrendingDown} label="Stopa otkazivanja" value={`${stats?.cancellationRate ?? 0}%`} color="amber" />
        <Card icon={Euro} label="Prihod ovaj tjedan" value={`${stats?.weeklyRevenue?.toFixed(2) ?? '0.00'} EUR`} color="green" />
        <Card icon={User} label="Najpopularniji frizer" value={stats?.mostPopularBarber || '-'} color="purple" />
        <Card icon={Scissors} label="Najpopularnija usluga" value={stats?.mostPopularService || '-'} color="blue" />
      </div>

      <div>
        <h2 className="text-lg font-semibold mb-3">Današnje nadolazeće rezervacije</h2>
        <DataTable
          columns={columns}
          data={recent || []}
          loading={recentLoading}
          keyExtractor={(b) => b.id}
          emptyMessage="Nema nadolazećih rezervacija za danas"
        />
      </div>
    </div>
  );
}
