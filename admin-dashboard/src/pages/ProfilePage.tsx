import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useQuery } from '@tanstack/react-query';
import { User, Key } from 'lucide-react';
import { Button, Input, PageHeader } from '@/components/ui';
import { getMe, changePassword, ApiError } from '@/lib/api';
import type { AuthUser } from '@/types';
import { toast } from 'sonner';

const passwordSchema = z.object({
  currentPassword: z.string().min(1, 'Trenutna lozinka je obavezna'),
  newPassword: z.string().min(6, 'Nova lozinka mora imati najmanje 6 znakova'),
  confirmPassword: z.string().min(1, 'Potvrdite novu lozinku'),
}).refine(d => d.newPassword === d.confirmPassword, {
  message: 'Lozinke se ne podudaraju',
  path: ['confirmPassword'],
});

type PasswordForm = z.infer<typeof passwordSchema>;

export default function ProfilePage() {
  const [loading, setLoading] = useState(false);
  const { data: user, isLoading } = useQuery<AuthUser>({ queryKey: ['me'], queryFn: getMe });

  const { register, handleSubmit, reset, formState: { errors } } = useForm<PasswordForm>({
    resolver: zodResolver(passwordSchema),
  });

  const onSubmit = async (data: PasswordForm) => {
    setLoading(true);
    try {
      await changePassword(data.currentPassword, data.newPassword);
      toast.success('Lozinka uspješno promijenjena');
      reset();
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : 'Greška pri promjeni lozinke');
    } finally {
      setLoading(false);
    }
  };

  if (isLoading) {
    return (
      <div className="space-y-4">
        <div className="skeleton h-8 w-48" />
        <div className="skeleton h-48 w-full rounded-2xl" />
      </div>
    );
  }

  return (
    <div className="max-w-2xl">
      <PageHeader title="Profil" description="Vaše osobne informacije i sigurnosne postavke" />

      <div className="space-y-8">
        <section className="glass rounded-2xl p-6">
          <div className="flex items-center gap-3 mb-4">
            <div className="p-2 rounded-xl bg-indigo-500/15 text-indigo-400">
              <User className="h-5 w-5" />
            </div>
            <h2 className="text-lg font-semibold">Osobni podaci</h2>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <p className="text-xs text-(--text-secondary)">Ime</p>
              <p className="text-sm font-medium">{user?.firstName || '-'}</p>
            </div>
            <div>
              <p className="text-xs text-(--text-secondary)">Prezime</p>
              <p className="text-sm font-medium">{user?.lastName || '-'}</p>
            </div>
            <div>
              <p className="text-xs text-(--text-secondary)">Email</p>
              <p className="text-sm font-medium">{user?.email || '-'}</p>
            </div>
            <div>
              <p className="text-xs text-(--text-secondary)">Uloga</p>
              <p className="text-sm font-medium">{user?.role || '-'}</p>
            </div>
            <div className="col-span-2">
              <p className="text-xs text-(--text-secondary)">Salon</p>
              <p className="text-sm font-medium">{user?.tenantName || '-'}</p>
            </div>
          </div>
        </section>

        <section className="glass rounded-2xl p-6">
          <div className="flex items-center gap-3 mb-4">
            <div className="p-2 rounded-xl bg-amber-500/15 text-amber-400">
              <Key className="h-5 w-5" />
            </div>
            <h2 className="text-lg font-semibold">Promjena lozinke</h2>
          </div>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <Input label="Trenutna lozinka" type="password" error={errors.currentPassword?.message} {...register('currentPassword')} />
            <Input label="Nova lozinka" type="password" error={errors.newPassword?.message} {...register('newPassword')} />
            <Input label="Potvrdi novu lozinku" type="password" error={errors.confirmPassword?.message} {...register('confirmPassword')} />
            <Button type="submit" loading={loading}>Promijeni lozinku</Button>
          </form>
        </section>
      </div>
    </div>
  );
}
