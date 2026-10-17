import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link, useNavigate } from 'react-router-dom';
import { Scissors } from 'lucide-react';
import { Button, Input } from '@/components/ui';
import { login, ApiError } from '@/lib/api';
import { toast } from 'sonner';

const loginSchema = z.object({
  email: z.string().min(1, 'Email je obavezan').email('Neispravna email adresa'),
  password: z.string().min(1, 'Lozinka je obavezna'),
});

type LoginForm = z.infer<typeof loginSchema>;

export default function LoginPage() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);

  const { register, handleSubmit, formState: { errors } } = useForm<LoginForm>({
    resolver: zodResolver(loginSchema),
  });

  const onSubmit = async (data: LoginForm) => {
    setLoading(true);
    try {
      await login(data.email, data.password);
      toast.success('Uspješna prijava!');
      navigate('/');
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Greška pri prijavi';
      toast.error(message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-4">
      <div className="glass w-full max-w-md rounded-3xl p-8 animate-fade-in">
        <div className="flex flex-col items-center mb-8">
          <div className="h-14 w-14 rounded-2xl bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center mb-4 shadow-lg">
            <Scissors className="h-7 w-7 text-white" />
          </div>
          <h1 className="text-2xl font-bold text-(--text-primary)">BarberBot Admin</h1>
          <p className="text-sm text-(--text-secondary) mt-1">Prijavite se u svoj salon</p>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
          <Input
            label="Email"
            type="email"
            placeholder="admin@barberbot.hr"
            error={errors.email?.message}
            {...register('email')}
          />
          <Input
            label="Lozinka"
            type="password"
            placeholder="Unesite lozinku"
            error={errors.password?.message}
            {...register('password')}
          />
          <Button type="submit" loading={loading} className="w-full" size="lg">
            Prijava
          </Button>
        </form>

        <p className="text-center text-sm text-(--text-secondary) mt-6">
          Nemate račun?{' '}
          <Link to="/register" className="font-medium text-(--primary-600) hover:text-(--primary-700)">
            Registrirajte salon
          </Link>
        </p>
      </div>
    </div>
  );
}
