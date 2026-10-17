import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link, useNavigate } from 'react-router-dom';
import { Scissors, ArrowLeft, ArrowRight, Check } from 'lucide-react';
import { Button, Input } from '@/components/ui';
import { register as registerApi, ApiError } from '@/lib/api';
import { toast } from 'sonner';
import { motion, AnimatePresence } from 'framer-motion';

const step1Schema = z.object({
  firstName: z.string().min(1, 'Ime je obavezno'),
  lastName: z.string().min(1, 'Prezime je obavezno'),
  email: z.string().min(1, 'E-mail je obavezan').email('Neispravna e-mail adresa'),
  password: z.string().min(6, 'Lozinka mora imati najmanje 6 znakova'),
  confirmPassword: z.string().min(1, 'Potvrdite lozinku'),
}).refine(d => d.password === d.confirmPassword, {
  message: 'Lozinke se ne podudaraju',
  path: ['confirmPassword'],
});

const step2Schema = z.object({
  salonName: z.string().min(2, 'Naziv salona mora imati najmanje 2 znaka'),
  address: z.string().optional(),
  contactEmail: z.string().email('Neispravna e-mail adresa').optional().or(z.literal('')),
  phone: z.string().optional(),
});

const step3Schema = z.object({
  botToken: z.string().min(10, 'Bot token mora imati najmanje 10 znakova'),
  botUsername: z.string().min(3, 'Korisničko ime mora imati najmanje 3 znaka'),
});

type Step1 = z.infer<typeof step1Schema>;
type Step2 = z.infer<typeof step2Schema>;
type Step3 = z.infer<typeof step3Schema>;

const steps = ['Račun', 'Salon', 'Telegram Bot'];

export default function RegisterPage() {
  const navigate = useNavigate();
  const [step, setStep] = useState(0);
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState<Partial<Step1 & Step2 & Step3>>({});

  const form1 = useForm<Step1>({ resolver: zodResolver(step1Schema), defaultValues: formData });
  const form2 = useForm<Step2>({ resolver: zodResolver(step2Schema), defaultValues: formData });
  const form3 = useForm<Step3>({ resolver: zodResolver(step3Schema), defaultValues: formData });

  const handleStep1 = (data: Step1) => {
    setFormData(prev => ({ ...prev, ...data }));
    setStep(1);
  };

  const handleStep2 = (data: Step2) => {
    setFormData(prev => ({ ...prev, ...data }));
    setStep(2);
  };

  const handleStep3 = async (data: Step3) => {
    setLoading(true);
    const merged = { ...formData, ...data };
    try {
      await registerApi({
        firstName: merged.firstName!,
        lastName: merged.lastName!,
        email: merged.email!,
        password: merged.password!,
        salonName: merged.salonName!,
        address: merged.address || '',
        contactEmail: merged.contactEmail || '',
        phone: merged.phone || '',
        botToken: merged.botToken!,
        botUsername: merged.botUsername!,
      });
      toast.success('Salon uspješno registriran!');
      navigate('/login');
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Greška pri registraciji';
      toast.error(message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-4">
      <div className="glass w-full max-w-lg rounded-3xl p-8 animate-fade-in">
        <div className="flex flex-col items-center mb-6">
          <div className="h-12 w-12 rounded-2xl bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center mb-3 shadow-lg">
            <Scissors className="h-6 w-6 text-white" />
          </div>
          <h1 className="text-xl font-bold text-(--text-primary)">Registracija salona</h1>
        </div>

        <div className="flex items-center justify-center gap-2 mb-8">
          {steps.map((s, i) => (
            <div key={s} className="flex items-center gap-2">
              <div className={`h-8 w-8 rounded-full flex items-center justify-center text-xs font-bold transition-colors ${
                i < step ? 'bg-emerald-500 text-white' :
                i === step ? 'bg-(--primary-600) text-white' :
                'bg-white/10 text-(--text-secondary)'
              }`}>
                {i < step ? <Check className="h-4 w-4" /> : i + 1}
              </div>
              <span className="text-xs font-medium text-(--text-secondary) hidden sm:inline">{s}</span>
              {i < steps.length - 1 && <div className="w-8 h-0.5 bg-white/10 mx-1" />}
            </div>
          ))}
        </div>

        <AnimatePresence mode="wait">
          {step === 0 && (
            <motion.form
              key="step1"
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -20 }}
              onSubmit={form1.handleSubmit(handleStep1)}
              className="space-y-4"
            >
              <div className="grid grid-cols-2 gap-3">
                <Input label="Ime *" placeholder="Ivan" error={form1.formState.errors.firstName?.message} {...form1.register('firstName')} />
                <Input label="Prezime *" placeholder="Horvat" error={form1.formState.errors.lastName?.message} {...form1.register('lastName')} />
              </div>
              <Input label="E-mail adresa *" type="email" placeholder="ivan@primjer.hr" error={form1.formState.errors.email?.message} {...form1.register('email')} />
              <Input label="Lozinka *" type="password" placeholder="Min. 6 znakova" error={form1.formState.errors.password?.message} {...form1.register('password')} />
              <Input label="Potvrdi lozinku *" type="password" placeholder="Ponovite lozinku" error={form1.formState.errors.confirmPassword?.message} {...form1.register('confirmPassword')} />
              <Button type="submit" className="w-full" size="lg">
                Dalje <ArrowRight className="h-4 w-4 ml-2" />
              </Button>
            </motion.form>
          )}
          {step === 1 && (
            <motion.form
              key="step2"
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -20 }}
              onSubmit={form2.handleSubmit(handleStep2)}
              className="space-y-4"
            >
              <Input label="Naziv salona *" placeholder="Moj Salon" error={form2.formState.errors.salonName?.message} {...form2.register('salonName')} />
              <Input label="Adresa" placeholder="Ilica 1, Zagreb" error={form2.formState.errors.address?.message} {...form2.register('address')} />
              <Input label="Kontakt e-mail" type="email" placeholder="info@salon.hr" error={form2.formState.errors.contactEmail?.message} {...form2.register('contactEmail')} />
              <Input label="Telefon" type="tel" placeholder="+385 91 234 5678" error={form2.formState.errors.phone?.message} {...form2.register('phone')} />
              <div className="flex gap-3">
                <Button type="button" variant="secondary" onClick={() => setStep(0)} className="flex-1">
                  <ArrowLeft className="h-4 w-4 mr-2" /> Natrag
                </Button>
                <Button type="submit" className="flex-1">
                  Dalje <ArrowRight className="h-4 w-4 ml-2" />
                </Button>
              </div>
            </motion.form>
          )}
          {step === 2 && (
            <motion.form
              key="step3"
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -20 }}
              onSubmit={form3.handleSubmit(handleStep3)}
              className="space-y-4"
            >
              <div className="space-y-1">
                <Input label="Bot Token *" placeholder="123456:ABC-DEF..." error={form3.formState.errors.botToken?.message} {...form3.register('botToken')} />
                <p className="text-xs text-(--text-secondary) pl-1">
                  Otvorite Telegram, pronađite @BotFather, pošaljite /newbot i kopirajte token ovdje.
                </p>
              </div>
              <div className="space-y-1">
                <Input label="Bot korisničko ime *" placeholder="@MojSalonBot" error={form3.formState.errors.botUsername?.message} {...form3.register('botUsername')} />
                <p className="text-xs text-(--text-secondary) pl-1">
                  Korisničko ime vašeg bota (npr. @MojSalonBot)
                </p>
              </div>
              <div className="flex gap-3">
                <Button type="button" variant="secondary" onClick={() => setStep(1)} className="flex-1">
                  <ArrowLeft className="h-4 w-4 mr-2" /> Natrag
                </Button>
                <Button type="submit" loading={loading} className="flex-1">
                  Registriraj se <Check className="h-4 w-4 ml-2" />
                </Button>
              </div>
            </motion.form>
          )}
        </AnimatePresence>

        <p className="text-center text-sm text-(--text-secondary) mt-6">
          Već imate račun?{' '}
          <Link to="/login" className="font-medium text-(--primary-600) hover:text-(--primary-700)">
            Prijavite se
          </Link>
        </p>
      </div>
    </div>
  );
}
