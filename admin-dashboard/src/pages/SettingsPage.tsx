import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Settings, Globe, Bot, Eye, EyeOff } from 'lucide-react';
import { Button, Input } from '@/components/ui';
import { api, ApiError } from '@/lib/api';
import type { Tenant, AuthUser } from '@/types';
import { toast } from 'sonner';

const tenantSchema = z.object({
  name: z.string().min(2, 'Naziv je obavezan'),
  address: z.string().optional(),
  contactEmail: z.string().email('Neispravna e-mail adresa').optional().or(z.literal('')),
  botToken: z.string().min(10, 'Bot token je obavezan'),
  botUsername: z.string().optional(),
});

type TenantForm = z.infer<typeof tenantSchema>;

export default function SettingsPage() {
  const queryClient = useQueryClient();
  const [showToken, setShowToken] = useState(false);

  const { data: me } = useQuery<AuthUser>({
    queryKey: ['me'],
    queryFn: () => api.get('/auth/me'),
  });

  const { data: tenant, isLoading } = useQuery<Tenant>({
    queryKey: ['tenant', me?.tenantId],
    queryFn: () => api.get(`/tenants/${me?.tenantId}`),
    enabled: !!me?.tenantId,
  });

  const { register, handleSubmit, reset, formState: { errors, isDirty } } = useForm<TenantForm>({
    resolver: zodResolver(tenantSchema),
  });

  useEffect(() => {
    if (tenant) {
      reset({
        name: tenant.name,
        address: tenant.address || '',
        contactEmail: tenant.contactEmail || '',
        botToken: tenant.botToken || '',
        botUsername: tenant.botUsername || '',
      });
    }
  }, [tenant, reset]);

  const mutation = useMutation({
    mutationFn: (data: TenantForm) => api.put(`/tenants/${me?.tenantId}`, data),
    onSuccess: () => {
      toast.success('Postavke spremljene!');
      queryClient.invalidateQueries({ queryKey: ['tenant'] });
      queryClient.invalidateQueries({ queryKey: ['me'] });
    },
    onError: (err) => {
      toast.error(err instanceof ApiError ? err.message : 'Greška pri spremanju');
    },
  });

  if (isLoading) {
    return (
      <div className="space-y-4">
        <div className="skeleton h-8 w-48" />
        <div className="skeleton h-64 w-full rounded-2xl" />
      </div>
    );
  }

  return (
    <div className="max-w-2xl">
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-(--text-primary)">Postavke</h1>
        <p className="text-sm text-(--text-secondary) mt-1">
          Upravljajte informacijama salona i bot konfiguracijom
        </p>
      </div>

      <form onSubmit={handleSubmit(d => mutation.mutate(d))} className="space-y-8">
        <section className="glass rounded-2xl p-6">
          <div className="flex items-center gap-3 mb-4">
            <div className="p-2 rounded-xl bg-indigo-500/15 text-indigo-400">
              <Globe className="h-5 w-5" />
            </div>
            <h2 className="text-lg font-semibold text-(--text-primary)">Podaci o salonu</h2>
          </div>
          <div className="space-y-4">
            <Input label="Naziv salona" error={errors.name?.message} {...register('name')} />
            <Input label="Adresa" error={errors.address?.message} {...register('address')} />
            <Input
              label="Kontakt e-mail"
              type="email"
              error={errors.contactEmail?.message}
              {...register('contactEmail')}
            />
          </div>
        </section>

        <section className="glass rounded-2xl p-6">
          <div className="flex items-center gap-3 mb-4">
            <div className="p-2 rounded-xl bg-purple-500/15 text-purple-400">
              <Bot className="h-5 w-5" />
            </div>
            <h2 className="text-lg font-semibold text-(--text-primary)">Telegram Bot</h2>
          </div>
          <div className="space-y-4">
            <div>
              <Input
                label="Bot Token"
                type={showToken ? 'text' : 'password'}
                error={errors.botToken?.message}
                {...register('botToken')}
              />
              <button
                type="button"
                onClick={() => setShowToken(!showToken)}
                className="flex items-center gap-1.5 text-xs text-(--text-secondary) hover:text-(--text-primary) mt-1.5 transition-colors"
              >
                {showToken ? <EyeOff className="h-3.5 w-3.5" /> : <Eye className="h-3.5 w-3.5" />}
                {showToken ? 'Sakrij token' : 'Prikaži token'}
              </button>
            </div>
            <Input
              label="Bot korisničko ime"
              placeholder="@MojSalonBot"
              error={errors.botUsername?.message}
              {...register('botUsername')}
            />
          </div>
        </section>

        <Button type="submit" loading={mutation.isPending} disabled={!isDirty} size="lg" className="w-full">
          <Settings className="h-4 w-4 mr-2" /> Spremi postavke
        </Button>
      </form>
    </div>
  );
}
