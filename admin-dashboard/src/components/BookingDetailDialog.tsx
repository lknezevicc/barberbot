import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { Dialog, Badge, Button, Input } from '@/components/ui';
import { api, ApiError } from '@/lib/api';
import type { Booking } from '@/types';
import { toast } from 'sonner';

interface BookingDetailDialogProps {
  booking: Booking | null;
  open: boolean;
  onClose: () => void;
}

const statusBadge: Record<string, { variant: 'success' | 'danger' | 'warning' | 'info'; label: string }> = {
  CONFIRMED: { variant: 'success', label: 'Potvrđen' },
  CANCELLED: { variant: 'danger', label: 'Otkazan' },
  HELD: { variant: 'warning', label: 'Na čekanju' },
  EXPIRED: { variant: 'info', label: 'Istekao' },
};

function formatDateTime(dateStr: string): string {
  if (!dateStr) return '-';
  const d = new Date(dateStr);
  return `${d.getDate()}.${d.getMonth() + 1}.${d.getFullYear()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
}

export default function BookingDetailDialog({ booking, open, onClose }: BookingDetailDialogProps) {
  const queryClient = useQueryClient();
  const [showReschedule, setShowReschedule] = useState(false);
  const { register, handleSubmit, reset } = useForm<{ date: string; time: string }>();

  const cancelMutation = useMutation({
    mutationFn: (id: string) => api.put(`/bookings/${id}/cancel`),
    onSuccess: () => {
      toast.success('Rezervacija otkazana');
      queryClient.invalidateQueries({ queryKey: ['bookings'] });
      queryClient.invalidateQueries({ queryKey: ['bookings-calendar'] });
      onClose();
    },
    onError: (err) => toast.error(err instanceof ApiError ? err.message : 'Greška'),
  });

  const rescheduleMutation = useMutation({
    mutationFn: (data: { id: string; date: string; time: string }) =>
      api.put(`/bookings/${data.id}/reschedule`, { date: data.date, time: data.time }),
    onSuccess: () => {
      toast.success('Rezervacija premještena');
      queryClient.invalidateQueries({ queryKey: ['bookings'] });
      queryClient.invalidateQueries({ queryKey: ['bookings-calendar'] });
      setShowReschedule(false);
      reset();
      onClose();
    },
    onError: (err) => toast.error(err instanceof ApiError ? err.message : 'Greška'),
  });

  function handleClose() {
    setShowReschedule(false);
    reset();
    onClose();
  }

  if (!booking) return null;

  const status = statusBadge[booking.status] ?? statusBadge.CONFIRMED;

  return (
    <Dialog open={open} onClose={handleClose} title="Detalji rezervacije">
      <div className="space-y-4">
        <div className="grid grid-cols-2 gap-4">
          <DetailRow label="Klijent" value={booking.customerName || '-'} />
          <DetailRow label="Telefon klijenta" value={booking.customerPhone || '-'} />
          <DetailRow label="Frizer" value={booking.barber} />
          <DetailRow label="Usluga" value={booking.service} />
          <DetailRow label="Datum" value={booking.date} />
          <DetailRow label="Vrijeme" value={booking.time} />
          <DetailRow label="Trajanje" value={`${booking.durationMinutes ?? '-'} min`} />
          <div className="space-y-1">
            <p className="text-xs text-(--text-secondary)">Status</p>
            <Badge variant={status.variant}>{status.label}</Badge>
          </div>
          <DetailRow label="Kreiran" value={formatDateTime(booking.createdAt)} className="col-span-2" />
        </div>

        {showReschedule && (
          <form
            onSubmit={handleSubmit((data) => rescheduleMutation.mutate({ id: booking.id, ...data }))}
            className="space-y-3 pt-3 border-t border-white/10"
          >
            <p className="text-sm font-medium">Premjesti rezervaciju</p>
            <div className="grid grid-cols-2 gap-3">
              <Input label="Novi datum" type="date" {...register('date', { required: true })} />
              <Input label="Novo vrijeme" type="time" {...register('time', { required: true })} />
            </div>
            <div className="flex justify-end gap-2">
              <Button
                type="button"
                variant="ghost"
                size="sm"
                onClick={() => { setShowReschedule(false); reset(); }}
              >
                Odustani
              </Button>
              <Button type="submit" size="sm" loading={rescheduleMutation.isPending}>
                Spremi
              </Button>
            </div>
          </form>
        )}

        {booking.status === 'CONFIRMED' && !showReschedule && (
          <div className="flex justify-end gap-3 pt-3 border-t border-white/10">
            <Button
              variant="ghost"
              onClick={() => setShowReschedule(true)}
            >
              Premjesti
            </Button>
            <Button
              variant="danger"
              onClick={() => cancelMutation.mutate(booking.id)}
              loading={cancelMutation.isPending}
            >
              Otkaži
            </Button>
          </div>
        )}
      </div>
    </Dialog>
  );
}

function DetailRow({ label, value, className }: { label: string; value: string; className?: string }) {
  return (
    <div className={`space-y-1 ${className ?? ''}`}>
      <p className="text-xs text-(--text-secondary)">{label}</p>
      <p className="text-sm font-medium">{value}</p>
    </div>
  );
}
