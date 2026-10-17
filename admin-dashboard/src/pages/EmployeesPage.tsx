import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Plus, Search } from 'lucide-react';
import { PageHeader, DataTable, Badge, Button, Input, Dialog, ConfirmDialog, type Column } from '@/components/ui';
import { api, ApiError } from '@/lib/api';
import type { Employee } from '@/types';
import { toast } from 'sonner';

const employeeSchema = z.object({
  name: z.string().min(2, 'Ime je obavezno'),
  specialization: z.string().optional(),
  contact: z.string().optional(),
});

type EmployeeForm = z.infer<typeof employeeSchema>;

export default function EmployeesPage() {
  const queryClient = useQueryClient();
  const [searchTerm, setSearchTerm] = useState('');
  const [createOpen, setCreateOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<Employee | null>(null);
  const [toggleTarget, setToggleTarget] = useState<Employee | null>(null);

  const { data: employees, isLoading } = useQuery<Employee[]>({
    queryKey: ['employees'],
    queryFn: () => api.get('/employees'),
  });

  const createForm = useForm<EmployeeForm>({ resolver: zodResolver(employeeSchema) });
  const editForm = useForm<EmployeeForm>({ resolver: zodResolver(employeeSchema) });

  const createMutation = useMutation({
    mutationFn: (data: EmployeeForm) => api.post('/employees', data),
    onSuccess: () => {
      toast.success('Zaposlenik dodan');
      queryClient.invalidateQueries({ queryKey: ['employees'] });
      setCreateOpen(false);
      createForm.reset();
    },
    onError: (err) => toast.error(err instanceof ApiError ? err.message : 'Greška'),
  });

  const editMutation = useMutation({
    mutationFn: (data: EmployeeForm & { id: string }) => api.put(`/employees/${data.id}`, data),
    onSuccess: () => {
      toast.success('Zaposlenik ažuriran');
      queryClient.invalidateQueries({ queryKey: ['employees'] });
      setEditTarget(null);
    },
    onError: (err) => toast.error(err instanceof ApiError ? err.message : 'Greška'),
  });

  const toggleMutation = useMutation({
    mutationFn: (emp: Employee) => api.put(`/employees/${emp.id}/${emp.active ? 'deactivate' : 'activate'}`),
    onSuccess: () => {
      toast.success('Status promijenjen');
      queryClient.invalidateQueries({ queryKey: ['employees'] });
      setToggleTarget(null);
    },
    onError: (err) => toast.error(err instanceof ApiError ? err.message : 'Greška'),
  });

  const openEdit = (emp: Employee) => {
    editForm.reset({ name: emp.name, specialization: emp.specialization || '', contact: emp.contact || '' });
    setEditTarget(emp);
  };

  const columns: Column<Employee>[] = [
    { key: 'name', header: 'Ime' },
    { key: 'specialization', header: 'Specijalizacija', render: (e) => e.specialization || '-' },
    { key: 'contact', header: 'Kontakt', render: (e) => e.contact || '-' },
    {
      key: 'active',
      header: 'Status',
      render: (e) => <Badge variant={e.active ? 'success' : 'danger'}>{e.active ? 'Aktivan' : 'Neaktivan'}</Badge>,
    },
    {
      key: 'actions',
      header: 'Akcije',
      render: (e) => (
        <div className="flex gap-2">
          <Button size="sm" variant="ghost" onClick={(ev) => { ev.stopPropagation(); openEdit(e); }}>Uredi</Button>
          <Button size="sm" variant={e.active ? 'danger' : 'primary'} onClick={(ev) => { ev.stopPropagation(); setToggleTarget(e); }}>
            {e.active ? 'Deaktiviraj' : 'Aktiviraj'}
          </Button>
        </div>
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        title="Zaposlenici"
        description="Upravljajte frizerima i zaposlenicima"
        action={<Button onClick={() => setCreateOpen(true)}><Plus className="h-4 w-4 mr-2" />Dodaj</Button>}
      />

      <div className="mb-4 relative">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-(--text-secondary)" />
        <input
          type="text"
          placeholder="Pretraži zaposlenike..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="glass-input w-full sm:w-72 pl-10 pr-4 py-2.5 rounded-xl text-sm text-(--text-primary) placeholder:text-(--text-secondary) focus:outline-none"
        />
      </div>

      <DataTable
        columns={columns}
        data={(employees || []).filter((e) => !searchTerm || e.name?.toLowerCase().includes(searchTerm.toLowerCase()))}
        loading={isLoading}
        keyExtractor={(e) => e.id}
      />

      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} title="Novi zaposlenik">
        <form onSubmit={createForm.handleSubmit(d => createMutation.mutate(d))} className="space-y-4">
          <Input label="Ime *" error={createForm.formState.errors.name?.message} {...createForm.register('name')} placeholder="npr. Marko Horvat" />
          <Input label="Specijalizacija" {...createForm.register('specialization')} placeholder="npr. Muško šišanje, fade, brijanje" />
          <Input label="Kontakt" {...createForm.register('contact')} placeholder="npr. 091-123-4567" />
          <div className="flex justify-end gap-3">
            <Button type="button" variant="ghost" onClick={() => setCreateOpen(false)}>Odustani</Button>
            <Button type="submit" loading={createMutation.isPending}>Spremi</Button>
          </div>
        </form>
      </Dialog>

      <Dialog open={!!editTarget} onClose={() => setEditTarget(null)} title="Uredi zaposlenika">
        <form onSubmit={editForm.handleSubmit(d => editTarget && editMutation.mutate({ ...d, id: editTarget.id }))} className="space-y-4">
          <Input label="Ime *" error={editForm.formState.errors.name?.message} {...editForm.register('name')} placeholder="npr. Marko Horvat" />
          <Input label="Specijalizacija" {...editForm.register('specialization')} placeholder="npr. Muško šišanje, fade, brijanje" />
          <Input label="Kontakt" {...editForm.register('contact')} placeholder="npr. 091-123-4567" />
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
        title={toggleTarget?.active ? 'Deaktiviraj zaposlenika' : 'Aktiviraj zaposlenika'}
        message={`Jeste li sigurni da želite ${toggleTarget?.active ? 'deaktivirati' : 'aktivirati'} zaposlenika ${toggleTarget?.name}?`}
        confirmText={toggleTarget?.active ? 'Deaktiviraj' : 'Aktiviraj'}
        variant={toggleTarget?.active ? 'danger' : 'primary'}
        loading={toggleMutation.isPending}
      />
    </div>
  );
}
