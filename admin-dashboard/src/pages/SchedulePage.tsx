import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Clock, Plus, Trash2 } from 'lucide-react';
import { PageHeader, Button, Input, Select, ConfirmDialog } from '@/components/ui';
import { api, ApiError } from '@/lib/api';
import type { Employee, WorkingHours, BlockedSlot } from '@/types';
import { toast } from 'sonner';

const dayNames = ['', 'Ponedjeljak', 'Utorak', 'Srijeda', 'Četvrtak', 'Petak', 'Subota', 'Nedjelja'];

const dayOptions = [
  { value: '1', label: 'Ponedjeljak' },
  { value: '2', label: 'Utorak' },
  { value: '3', label: 'Srijeda' },
  { value: '4', label: 'Četvrtak' },
  { value: '5', label: 'Petak' },
  { value: '6', label: 'Subota' },
  { value: '7', label: 'Nedjelja' },
];

const workingHoursSchema = z.object({
  dayOfWeek: z.string().min(1, 'Dan je obavezan'),
  startTime: z.string().min(1, 'Početak je obavezan'),
  endTime: z.string().min(1, 'Kraj je obavezan'),
}).refine(data => data.startTime < data.endTime, {
  message: 'Početak mora biti prije kraja',
  path: ['endTime'],
});

type WorkingHoursForm = z.infer<typeof workingHoursSchema>;

const blockedSlotSchema = z.object({
  employeeId: z.string().min(1, 'Odaberite zaposlenika'),
  date: z.string().min(1, 'Datum je obavezan'),
  allDay: z.boolean(),
  startTime: z.string().optional(),
  endTime: z.string().optional(),
  reason: z.string().optional(),
}).refine(data => data.allDay || (data.startTime && data.startTime.length > 0), {
  message: 'Početak je obavezan',
  path: ['startTime'],
}).refine(data => data.allDay || (data.endTime && data.endTime.length > 0), {
  message: 'Kraj je obavezan',
  path: ['endTime'],
});

type BlockedSlotForm = z.infer<typeof blockedSlotSchema>;

export default function SchedulePage() {
  const queryClient = useQueryClient();
  const [selectedEmployee, setSelectedEmployee] = useState('');
  const [showAddHours, setShowAddHours] = useState(false);
  const [deleteHours, setDeleteHours] = useState<{ employeeId: string; dayOfWeek: number } | null>(null);
  const [deleteSlot, setDeleteSlot] = useState<string | null>(null);

  const { data: employees } = useQuery<Employee[]>({
    queryKey: ['employees'],
    queryFn: () => api.get('/employees'),
  });

  const { data: hours, isLoading: hoursLoading } = useQuery<WorkingHours[]>({
    queryKey: ['working-hours', selectedEmployee],
    queryFn: () => api.get(`/schedule/working-hours/${selectedEmployee}`),
    enabled: !!selectedEmployee,
  });

  const { data: blockedSlots, isLoading: slotsLoading } = useQuery<BlockedSlot[]>({
    queryKey: ['blocked-slots'],
    queryFn: () => api.get('/schedule/blocked-slots'),
  });

  const whForm = useForm<WorkingHoursForm>({ resolver: zodResolver(workingHoursSchema) });
  const blockForm = useForm<BlockedSlotForm>({
    resolver: zodResolver(blockedSlotSchema),
    defaultValues: { allDay: false },
  });

  const allDay = blockForm.watch('allDay');

  const createHoursMutation = useMutation({
    mutationFn: (data: { employeeId: string; dayOfWeek: number; startTime: string; endTime: string }) =>
      api.put('/schedule/working-hours', data),
    onSuccess: () => {
      toast.success('Radno vrijeme dodano');
      queryClient.invalidateQueries({ queryKey: ['working-hours'] });
      whForm.reset();
      setShowAddHours(false);
    },
    onError: (err) => toast.error(err instanceof ApiError ? err.message : 'Greška'),
  });

  const deleteHoursMutation = useMutation({
    mutationFn: (data: { employeeId: string; dayOfWeek: number }) =>
      api.delete(`/schedule/working-hours/${data.employeeId}/${data.dayOfWeek}`),
    onSuccess: () => {
      toast.success('Radno vrijeme obrisano');
      queryClient.invalidateQueries({ queryKey: ['working-hours'] });
      setDeleteHours(null);
    },
    onError: (err) => toast.error(err instanceof ApiError ? err.message : 'Greška'),
  });

  const createSlotMutation = useMutation({
    mutationFn: (data: BlockedSlotForm) => {
      const payload: Record<string, unknown> = {
        employeeId: data.employeeId,
        date: data.date,
        allDay: data.allDay,
        reason: data.reason,
      };
      if (!data.allDay) {
        payload.startTime = data.startTime;
        payload.endTime = data.endTime;
      }
      return api.post('/schedule/blocked-slots', payload);
    },
    onSuccess: () => {
      toast.success('Blokiran termin dodan');
      queryClient.invalidateQueries({ queryKey: ['blocked-slots'] });
      blockForm.reset({ allDay: false });
    },
    onError: (err) => toast.error(err instanceof ApiError ? err.message : 'Greška'),
  });

  const deleteSlotMutation = useMutation({
    mutationFn: (id: string) => api.delete(`/schedule/blocked-slots/${id}`),
    onSuccess: () => {
      toast.success('Blokiran termin obrisan');
      queryClient.invalidateQueries({ queryKey: ['blocked-slots'] });
      setDeleteSlot(null);
    },
    onError: (err) => toast.error(err instanceof ApiError ? err.message : 'Greška'),
  });

  const employeeOptions = (employees || []).map(e => ({ value: e.id, label: e.name }));

  function handleCreateHours(data: WorkingHoursForm) {
    createHoursMutation.mutate({
      employeeId: selectedEmployee,
      dayOfWeek: Number(data.dayOfWeek),
      startTime: data.startTime,
      endTime: data.endTime,
    });
  }

  return (
    <div>
      <PageHeader title="Raspored" description="Upravljajte radnim vremenom i blokiranim terminima" />

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <section className="glass rounded-2xl p-6">
          <div className="flex items-center gap-3 mb-4">
            <div className="p-2 rounded-xl bg-indigo-500/15 text-indigo-400">
              <Clock className="h-5 w-5" />
            </div>
            <h2 className="text-lg font-semibold">Radno vrijeme</h2>
          </div>

          <Select
            options={employeeOptions}
            placeholder="Odaberite zaposlenika"
            value={selectedEmployee}
            onChange={e => setSelectedEmployee(e.target.value)}
            className="mb-4"
          />

          {selectedEmployee && (
            <>
              {!showAddHours ? (
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => setShowAddHours(true)}
                  className="mb-4 w-full flex items-center justify-center gap-2"
                >
                  <Plus className="h-4 w-4" />
                  Dodaj radno vrijeme
                </Button>
              ) : (
                <form
                  onSubmit={whForm.handleSubmit(handleCreateHours)}
                  className="space-y-3 mb-4 p-4 rounded-xl bg-white/5 border border-white/10"
                >
                  <Select
                    label="Dan u tjednu"
                    options={dayOptions}
                    placeholder="Odaberite dan"
                    error={whForm.formState.errors.dayOfWeek?.message}
                    {...whForm.register('dayOfWeek')}
                  />
                  <div className="grid grid-cols-2 gap-3">
                    <Input
                      label="Početak"
                      type="time"
                      error={whForm.formState.errors.startTime?.message}
                      {...whForm.register('startTime')}
                    />
                    <Input
                      label="Kraj"
                      type="time"
                      error={whForm.formState.errors.endTime?.message}
                      {...whForm.register('endTime')}
                    />
                  </div>
                  <div className="flex gap-2">
                    <Button type="submit" size="sm" loading={createHoursMutation.isPending} className="flex-1">
                      Spremi
                    </Button>
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      onClick={() => { setShowAddHours(false); whForm.reset(); }}
                    >
                      Odustani
                    </Button>
                  </div>
                </form>
              )}

              <div className="space-y-2">
                {hoursLoading ? (
                  <div className="space-y-2">{[1,2,3].map(i => <div key={i} className="skeleton h-10 rounded-xl" />)}</div>
                ) : hours && hours.length > 0 ? (
                  hours.map(h => (
                    <div key={h.id} className="flex items-center justify-between p-3 rounded-xl bg-white/5">
                      <div>
                        <span className="text-sm font-medium">{dayNames[h.dayOfWeek]}</span>
                        <span className="text-sm text-(--text-secondary) ml-2">{h.startTime} - {h.endTime}</span>
                      </div>
                      <button
                        onClick={() => setDeleteHours({ employeeId: selectedEmployee, dayOfWeek: h.dayOfWeek })}
                        className="p-1.5 rounded-lg hover:bg-red-500/15 text-red-400 transition-colors"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>
                  ))
                ) : (
                  <p className="text-sm text-(--text-secondary) text-center py-4">Nema definiranog radnog vremena</p>
                )}
              </div>
            </>
          )}
        </section>

        <section className="glass rounded-2xl p-6">
          <div className="flex items-center gap-3 mb-4">
            <div className="p-2 rounded-xl bg-red-500/15 text-red-400">
              <Plus className="h-5 w-5" />
            </div>
            <h2 className="text-lg font-semibold">Blokirani termini</h2>
          </div>

          <form onSubmit={blockForm.handleSubmit(d => createSlotMutation.mutate(d))} className="space-y-3 mb-4">
            <Select label="Zaposlenik" options={employeeOptions} placeholder="Odaberite" error={blockForm.formState.errors.employeeId?.message} {...blockForm.register('employeeId')} />
            <Input label="Datum" type="date" error={blockForm.formState.errors.date?.message} {...blockForm.register('date')} />

            <Controller
              control={blockForm.control}
              name="allDay"
              render={({ field }) => (
                <label className="flex items-center gap-2 cursor-pointer select-none">
                  <input
                    type="checkbox"
                    checked={field.value}
                    onChange={field.onChange}
                    className="w-4 h-4 rounded border-white/20 bg-white/5 text-(--primary-600) focus:ring-(--primary-400)"
                  />
                  <span className="text-sm text-(--text-primary)">Cijeli dan</span>
                </label>
              )}
            />

            {!allDay && (
              <div className="grid grid-cols-2 gap-3">
                <Input label="Od" type="time" error={blockForm.formState.errors.startTime?.message} {...blockForm.register('startTime')} />
                <Input label="Do" type="time" error={blockForm.formState.errors.endTime?.message} {...blockForm.register('endTime')} />
              </div>
            )}
            <Input label="Razlog" placeholder="Opcionalno" {...blockForm.register('reason')} />
            <Button type="submit" loading={createSlotMutation.isPending} className="w-full">Dodaj blokadu</Button>
          </form>

          <div className="space-y-2 max-h-64 overflow-y-auto">
            {slotsLoading ? (
              [1,2].map(i => <div key={i} className="skeleton h-12 rounded-xl" />)
            ) : blockedSlots && blockedSlots.length > 0 ? (
              blockedSlots.map(slot => (
                <div key={slot.id} className="flex items-center justify-between p-3 rounded-xl bg-white/5">
                  <div>
                    <p className="text-sm font-medium">{employees?.find(e => e.id === slot.employeeId)?.name || 'N/A'} - {slot.date}</p>
                    <p className="text-xs text-(--text-secondary)">
                      {slot.allDay ? 'Cijeli dan' : `${slot.startTime} - ${slot.endTime}`}
                      {slot.reason && ` (${slot.reason})`}
                    </p>
                  </div>
                  <button
                    onClick={() => setDeleteSlot(slot.id)}
                    className="p-1.5 rounded-lg hover:bg-red-500/15 text-red-400 transition-colors"
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                </div>
              ))
            ) : (
              <p className="text-sm text-(--text-secondary) text-center py-4">Nema blokiranih termina</p>
            )}
          </div>
        </section>
      </div>

      <ConfirmDialog
        open={!!deleteHours}
        onClose={() => setDeleteHours(null)}
        onConfirm={() => deleteHours && deleteHoursMutation.mutate(deleteHours)}
        title="Obriši radno vrijeme"
        message="Jeste li sigurni da želite obrisati ovo radno vrijeme?"
        confirmText="Obriši"
        loading={deleteHoursMutation.isPending}
      />

      <ConfirmDialog
        open={!!deleteSlot}
        onClose={() => setDeleteSlot(null)}
        onConfirm={() => deleteSlot && deleteSlotMutation.mutate(deleteSlot)}
        title="Obriši blokiran termin"
        message="Jeste li sigurni da želite obrisati ovaj blokiran termin?"
        confirmText="Obriši"
        loading={deleteSlotMutation.isPending}
      />
    </div>
  );
}
