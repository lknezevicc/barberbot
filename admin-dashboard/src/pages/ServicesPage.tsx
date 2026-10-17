import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Plus, Search } from 'lucide-react';
import { PageHeader, DataTable, Badge, Button, Input, Dialog, ConfirmDialog, type Column } from '@/components/ui';
import { api, ApiError } from '@/lib/api';
import type { ServiceItem } from '@/types';
import { toast } from 'sonner';

const serviceSchema = z.object({
  name: z.string().min(2, 'Naziv je obavezan'),
  durationMinutes: z.number().min(5, 'Minimalno 5 minuta'),
  price: z.number().min(0, 'Cijena ne može biti negativna'),
  description: z.string().optional(),
});

type ServiceForm = z.infer<typeof serviceSchema>;

export default function ServicesPage() {
  const queryClient = useQueryClient();
  const [searchTerm, setSearchTerm] = useState('');
  const [createOpen, setCreateOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<ServiceItem | null>(null);
  const [toggleTarget, setToggleTarget] = useState<ServiceItem | null>(null);

  const { data: services, isLoading } = useQuery<ServiceItem[]>({
    queryKey: ['services'],
    queryFn: () => api.get('/services'),
  });

  const createForm = useForm<ServiceForm>({ resolver: zodResolver(serviceSchema), defaultValues: { durationMinutes: 30, price: 15 } });
  const editForm = useForm<ServiceForm>({ resolver: zodResolver(serviceSchema) });

  const createMutation = useMutation({
    mutationFn: (data: ServiceForm) => api.post('/services', data),
    onSuccess: () => {
      toast.success('Usluga dodana');
      queryClient.invalidateQueries({ queryKey: ['services'] });
      setCreateOpen(false);
      createForm.reset();
    },
    onError: (err) => toast.error(err instanceof ApiError ? err.message : 'Greška'),
  });

  const editMutation = useMutation({
    mutationFn: (data: ServiceForm & { id: string }) => api.put(`/services/${data.id}`, data),
    onSuccess: () => {
      toast.success('Usluga ažurirana');
      queryClient.invalidateQueries({ queryKey: ['services'] });
      setEditTarget(null);
    },
    onError: (err) => toast.error(err instanceof ApiError ? err.message : 'Greška'),
  });

  const toggleMutation = useMutation({
    mutationFn: (s: ServiceItem) => api.put(`/services/${s.id}/${s.active ? 'deactivate' : 'activate'}`),
    onSuccess: () => {
      toast.success('Status promijenjen');
      queryClient.invalidateQueries({ queryKey: ['services'] });
      setToggleTarget(null);
    },
    onError: (err) => toast.error(err instanceof ApiError ? err.message : 'Greška'),
  });

  const openEdit = (s: ServiceItem) => {
    editForm.reset({ name: s.name, durationMinutes: s.durationMinutes, price: s.price, description: s.description || '' });
    setEditTarget(s);
  };

  const formatPrice = (price: number) => `${price.toFixed(2)} EUR`;
  const formatDuration = (min: number) => `${min} min`;

  const columns: Column<ServiceItem>[] = [
    { key: 'name', header: 'Naziv' },
    { key: 'durationMinutes', header: 'Trajanje', render: (s) => formatDuration(s.durationMinutes) },
    { key: 'price', header: 'Cijena', render: (s) => formatPrice(s.price) },
    { key: 'description', header: 'Opis', render: (s) => s.description || '-' },
    {
      key: 'active',
      header: 'Status',
      render: (s) => <Badge variant={s.active ? 'success' : 'danger'}>{s.active ? 'Aktivna' : 'Neaktivna'}</Badge>,
    },
    {
      key: 'actions',
      header: 'Akcije',
      render: (s) => (
        <div className="flex gap-2">
          <Button size="sm" variant="ghost" onClick={(e) => { e.stopPropagation(); openEdit(s); }}>Uredi</Button>
          <Button size="sm" variant={s.active ? 'danger' : 'primary'} onClick={(e) => { e.stopPropagation(); setToggleTarget(s); }}>
            {s.active ? 'Deaktiviraj' : 'Aktiviraj'}
          </Button>
        </div>
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        title="Usluge"
        description="Upravljajte uslugama salona"
        action={<Button onClick={() => setCreateOpen(true)}><Plus className="h-4 w-4 mr-2" />Dodaj</Button>}
      />

      <div className="mb-4 relative">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-(--text-secondary)" />
        <input
          type="text"
          placeholder="Pretraži usluge..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="glass-input w-full sm:w-72 pl-10 pr-4 py-2.5 rounded-xl text-sm text-(--text-primary) placeholder:text-(--text-secondary) focus:outline-none"
        />
      </div>

      <DataTable
        columns={columns}
        data={(services || []).filter((s) => !searchTerm || s.name?.toLowerCase().includes(searchTerm.toLowerCase()))}
        loading={isLoading}
        keyExtractor={(s) => s.id}
      />

      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} title="Nova usluga">
        <form onSubmit={createForm.handleSubmit(d => createMutation.mutate(d))} className="space-y-4">
          <Input label="Naziv *" error={createForm.formState.errors.name?.message} {...createForm.register('name')} />
          <Input label="Trajanje (min) *" type="number" error={createForm.formState.errors.durationMinutes?.message} {...createForm.register('durationMinutes', { valueAsNumber: true })} />
          <Input label="Cijena (EUR) *" type="number" step="0.01" error={createForm.formState.errors.price?.message} {...createForm.register('price', { valueAsNumber: true })} />
          <Input label="Opis" {...createForm.register('description')} />
          <div className="flex justify-end gap-3">
            <Button type="button" variant="ghost" onClick={() => setCreateOpen(false)}>Odustani</Button>
            <Button type="submit" loading={createMutation.isPending}>Spremi</Button>
          </div>
        </form>
      </Dialog>

      <Dialog open={!!editTarget} onClose={() => setEditTarget(null)} title="Uredi uslugu">
        <form onSubmit={editForm.handleSubmit(d => editTarget && editMutation.mutate({ ...d, id: editTarget.id }))} className="space-y-4">
          <Input label="Naziv *" error={editForm.formState.errors.name?.message} {...editForm.register('name')} />
          <Input label="Trajanje (min) *" type="number" error={editForm.formState.errors.durationMinutes?.message} {...editForm.register('durationMinutes', { valueAsNumber: true })} />
          <Input label="Cijena (EUR) *" type="number" step="0.01" error={editForm.formState.errors.price?.message} {...editForm.register('price', { valueAsNumber: true })} />
          <Input label="Opis" {...editForm.register('description')} />
          <div className="flex justify-end gap-3">
            <Button type="button" variant="ghost" onClick={() => setEditTarget(null)}>Odustani</Button>
            <Button type="submit" loading={editMutation.isPending}>Spremi</Button>
          </div>
        </form>
      </Dialog>

      <ConfirmDialog
        open={!!toggleTarget}
        onClose={() => setToggleTarget(null)}
        onConfirm={() => toggleTarget && toggleMutation.mutate(toggleTarget)}
        title={toggleTarget?.active ? 'Deaktiviraj uslugu' : 'Aktiviraj uslugu'}
        message={`Jeste li sigurni da želite ${toggleTarget?.active ? 'deaktivirati' : 'aktivirati'} uslugu "${toggleTarget?.name}"?`}
        confirmText={toggleTarget?.active ? 'Deaktiviraj' : 'Aktiviraj'}
        variant={toggleTarget?.active ? 'danger' : 'primary'}
        loading={toggleMutation.isPending}
      />
    </div>
  );
}
