import { Link } from 'react-router-dom';
import { Home } from 'lucide-react';
import { Button } from '@/components/ui';

export default function NotFoundPage() {
  return (
    <div className="min-h-screen flex items-center justify-center p-4">
      <div className="glass rounded-3xl p-12 text-center max-w-md animate-fade-in">
        <p className="text-7xl font-bold text-(--primary-600) mb-4">404</p>
        <h1 className="text-xl font-semibold text-(--text-primary) mb-2">Stranica nije pronađena</h1>
        <p className="text-sm text-(--text-secondary) mb-8">
          Stranica koju tražite ne postoji ili je premještena.
        </p>
        <Link to="/">
          <Button size="lg">
            <Home className="h-4 w-4 mr-2" /> Natrag na početnu
          </Button>
        </Link>
      </div>
    </div>
  );
}
