import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { CalendarDays, TableProperties } from 'lucide-react';
import { PageHeader, DataTable, Badge, Button, Select, ConfirmDialog, type Column } from '@/components/ui';
import { api, ApiError } from '@/lib/api';
import CalendarView from '@/components/CalendarView';
import BookingDetailDialog from '@/components/BookingDetailDialog';
import type { Booking } from '@/types';
import { toast } from 'sonner';

type ViewMode = 'table' | 'calendar';

const statusOptions = [
  { value: '', label: 'Svi statusi' },
  { value: 'CONFIRMED', label: 'Potvrđen' },
  { value: 'CANCELLED', label: 'Otkazan' },
  { value: 'HELD', label: 'Na čekanju' },
];

const statusBadge: Record<string, { variant: 'success' | 'danger' | 'warning' | 'info'; label: string }> = {
  CONFIRMED: { variant: 'success', label: 'Potvrđen' },
  CANCELLED: { variant: 'danger', label: 'Otkazan' },
  HELD: { variant: 'warning', label: 'Na čekanju' },
  EXPIRED: { variant: 'info', label: 'Istekao' },
};

function getMonday(d: Date): string {
  const date = new Date(d);
  const day = date.getDay();
  const diff = date.getDate() - day + (day === 0 ? -6 : 1);
  date.setDate(diff);
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const dd = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${dd}`;
}

export default function BookingsPage() {
  const queryClient = useQueryClient();
  const [viewMode, setViewMode] = useState<ViewMode>('table');
  const [dateFilter, setDateFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [cancelTarget, setCancelTarget] = useState<Booking | null>(null);
  const [detailBooking, setDetailBooking] = useState<Booking | null>(null);
  const [weekStart, setWeekStart] = useState(() => getMonday(new Date()));

  const params = new URLSearchParams();
  if (dateFilter) params.set('date', dateFilter);
  if (statusFilter) params.set('status', statusFilter);
  const queryStr = params.toString();

  const { data: bookings, isLoading } = useQuery<Booking[]>({
    queryKey: ['bookings', queryStr],
    queryFn: () => api.get(`/bookings${queryStr ? `?${queryStr}` : ''}`),
    enabled: viewMode === 'table',
  });

  const cancelMutation = useMutation({
    mutationFn: (id: string) => api.put(`/bookings/${id}/cancel`),
    onSuccess: () => {
      toast.success('Rezervacija otkazana');
      queryClient.invalidateQueries({ queryKey: ['bookings'] });
      queryClient.invalidateQueries({ queryKey: ['bookings-calendar'] });
      setCancelTarget(null);
    },
    onError: (err) => toast.error(err instanceof ApiError ? err.message : 'Greška'),
  });

  const columns: Column<Booking>[] = [
    { key: 'customerName', header: 'Klijent', render: (b) => b.customerName || b.userId || '-' },
    { key: 'barber', header: 'Frizer' },
    { key: 'service', header: 'Usluga' },
    { key: 'date', header: 'Datum' },
    { key: 'time', header: 'Vrijeme' },
    {
      key: 'durationMinutes',
      header: 'Trajanje',
      render: (b) => <span>{b.durationMinutes} min</span>,
    },
    {
      key: 'status',
      header: 'Status',
      render: (b) => {
        const s = statusBadge[b.status] || statusBadge.CONFIRMED;
        return <Badge variant={s.variant}>{s.label}</Badge>;
      },
    },
    {
      key: 'actions',
      header: 'Akcije',
      render: (b) => b.status === 'CONFIRMED' ? (
        <div className="flex gap-2">
          <Button size="sm" variant="ghost" onClick={(e) => { e.stopPropagation(); setCancelTarget(b); }}>
            Otkaži
          </Button>
        </div>
      ) : null,
    },
  ];

  return (
    <div>
      <PageHeader title="Rezervacije" description="Upravljajte svim rezervacijama" />

      <div className="flex flex-wrap items-center gap-3 mb-6">
        <div className="flex rounded-xl overflow-hidden glass">
          <button
            onClick={() => setViewMode('table')}
            className={`flex items-center gap-2 px-4 py-2 text-sm font-medium transition-colors ${
              viewMode === 'table'
                ? 'bg-(--primary-600) text-white'
                : 'hover:bg-white/10 text-(--text-secondary)'
            }`}
          >
            <TableProperties className="h-4 w-4" />
            Tablica
          </button>
          <button
            onClick={() => setViewMode('calendar')}
            className={`flex items-center gap-2 px-4 py-2 text-sm font-medium transition-colors ${
              viewMode === 'calendar'
                ? 'bg-(--primary-600) text-white'
                : 'hover:bg-white/10 text-(--text-secondary)'
            }`}
          >
            <CalendarDays className="h-4 w-4" />
            Kalendar
          </button>
        </div>

        {viewMode === 'table' && (
          <>
            <input
              type="date"
              value={dateFilter}
              onChange={e => setDateFilter(e.target.value)}
              className="glass-input px-4 py-2 rounded-xl text-sm"
            />
            <Select
              options={statusOptions}
              value={statusFilter}
              onChange={e => setStatusFilter(e.target.value)}
              className="w-40"
            />
            {(dateFilter || statusFilter) && (
              <Button variant="ghost" size="sm" onClick={() => { setDateFilter(''); setStatusFilter(''); }}>
                Poništi filtere
              </Button>
            )}
          </>
        )}
      </div>

      {viewMode === 'table' ? (
        <DataTable
          columns={columns}
          data={bookings || []}
          loading={isLoading}
          keyExtractor={(b) => b.id}
          emptyMessage="Nema rezervacija za prikaz"
          onRowClick={(b) => setDetailBooking(b)}
        />
      ) : (
        <CalendarView
          weekStart={weekStart}
          onWeekChange={setWeekStart}
          onBookingClick={(b) => setDetailBooking(b)}
          onSlotClick={() => {
            /* placeholder: create-booking dialog is not implemented yet */
          }}
        />
      )}

      <ConfirmDialog
        open={!!cancelTarget}
        onClose={() => setCancelTarget(null)}
        onConfirm={() => cancelTarget && cancelMutation.mutate(cancelTarget.id)}
        title="Otkaži rezervaciju"
        message={`Jeste li sigurni da želite otkazati rezervaciju za ${cancelTarget?.barber} (${cancelTarget?.date} ${cancelTarget?.time})?`}
        confirmText="Otkaži"
        loading={cancelMutation.isPending}
      />

      <BookingDetailDialog
        booking={detailBooking}
        open={!!detailBooking}
        onClose={() => setDetailBooking(null)}
      />
    </div>
  );
}
