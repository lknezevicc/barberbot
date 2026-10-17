import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { ChevronDown, ChevronRight } from 'lucide-react';
import { PageHeader, Badge, Select } from '@/components/ui';
import { api } from '@/lib/api';
import type { AuditEntry, PageResponse } from '@/types';

const entityTypes = [
  { value: '', label: 'Svi tipovi' },
  { value: 'EMPLOYEE', label: 'Zaposlenik' },
  { value: 'SERVICE', label: 'Usluga' },
  { value: 'BOOKING', label: 'Rezervacija' },
  { value: 'USER', label: 'Korisnik' },
  { value: 'TENANT', label: 'Salon' },
  { value: 'WORKING_HOURS', label: 'Radno vrijeme' },
  { value: 'BLOCKED_SLOT', label: 'Blokiran termin' },
];

const actionTypes = [
  { value: '', label: 'Sve akcije' },
  { value: 'CREATE', label: 'Kreiranje' },
  { value: 'UPDATE', label: 'Ažuriranje' },
  { value: 'DELETE', label: 'Brisanje' },
  { value: 'CANCEL', label: 'Otkazivanje' },
  { value: 'RESCHEDULE', label: 'Premještanje' },
  { value: 'ACTIVATE', label: 'Aktivacija' },
  { value: 'DEACTIVATE', label: 'Deaktivacija' },
];

const actionBadge: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
  CREATE: 'success',
  UPDATE: 'warning',
  DELETE: 'danger',
  CANCEL: 'danger',
  RESCHEDULE: 'info',
  ACTIVATE: 'success',
  DEACTIVATE: 'danger',
  SET: 'info',
};

function formatDate(dateStr: string): string {
  try {
    return new Date(dateStr).toLocaleString('hr-HR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch {
    return dateStr;
  }
}

function formatJson(raw: string | null | undefined): string {
  if (!raw) return 'N/A';
  try {
    const parsed: unknown = JSON.parse(raw);
    return JSON.stringify(parsed, null, 2);
  } catch {
    return raw;
  }
}

export default function AuditLogPage() {
  const [typeFilter, setTypeFilter] = useState('');
  const [actionFilter, setActionFilter] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [page, setPage] = useState(0);
  const [expandedId, setExpandedId] = useState<string | null>(null);

  const { data, isLoading } = useQuery<PageResponse<AuditEntry>>({
    queryKey: ['audit-log', page, typeFilter, actionFilter, startDate, endDate],
    queryFn: () => {
      const params = new URLSearchParams({ page: String(page), size: '20' });
      if (typeFilter) params.set('entityType', typeFilter);
      if (actionFilter) params.set('action', actionFilter);
      if (startDate) params.set('startDate', startDate);
      if (endDate) params.set('endDate', endDate);
      return api.get(`/audit-logs?${params}`);
    },
  });

  const entries = data?.content || [];
  const totalPages = data?.totalPages || 0;

  const toggleExpand = (id: string) => {
    setExpandedId((prev) => (prev === id ? null : id));
  };

  return (
    <div>
      <PageHeader title="Revizijski zapisi" description="Pregled svih administrativnih radnji" />

      <div className="mb-4 flex flex-wrap items-end gap-3">
        <Select
          options={entityTypes}
          value={typeFilter}
          onChange={(e) => { setTypeFilter(e.target.value); setPage(0); }}
          className="w-48"
        />
        <Select
          options={actionTypes}
          value={actionFilter}
          onChange={(e) => { setActionFilter(e.target.value); setPage(0); }}
          className="w-48"
        />
        <div className="space-y-1">
          <label className="block text-sm font-medium text-(--text-primary)">Od</label>
          <input
            type="date"
            value={startDate}
            onChange={(e) => { setStartDate(e.target.value); setPage(0); }}
            className="glass-input px-3 py-2 rounded-xl text-sm text-(--text-primary) focus:outline-none"
          />
        </div>
        <div className="space-y-1">
          <label className="block text-sm font-medium text-(--text-primary)">Do</label>
          <input
            type="date"
            value={endDate}
            onChange={(e) => { setEndDate(e.target.value); setPage(0); }}
            className="glass-input px-3 py-2 rounded-xl text-sm text-(--text-primary) focus:outline-none"
          />
        </div>
      </div>

      <div className="glass rounded-2xl overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-white/20">
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-(--text-secondary) w-8" />
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-(--text-secondary)">Akcija</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-(--text-secondary)">Tip entiteta</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-(--text-secondary)">Korisnik</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-(--text-secondary)">ID entiteta</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-(--text-secondary)">Datum</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/10">
              {isLoading ? (
                Array.from({ length: 5 }).map((_, i) => (
                  <tr key={i}>
                    {Array.from({ length: 6 }).map((__, j) => (
                      <td key={j} className="px-4 py-3">
                        <div className="skeleton h-4 w-3/4 rounded" />
                      </td>
                    ))}
                  </tr>
                ))
              ) : entries.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-4 py-12 text-center text-sm text-(--text-secondary)">
                    Nema revizijskih zapisa
                  </td>
                </tr>
              ) : (
                entries.map((entry) => {
                  const isExpanded = expandedId === entry.id;
                  const hasDetails = entry.oldValue || entry.newValue;
                  return (
                    <tr key={entry.id} className="group">
                      <td colSpan={6} className="p-0">
                        <div
                          className={`flex items-center cursor-pointer transition-colors hover:bg-white/10 ${isExpanded ? 'bg-white/5' : ''}`}
                          onClick={() => hasDetails && toggleExpand(entry.id)}
                        >
                          <div className="px-4 py-3 w-8 shrink-0">
                            {hasDetails && (
                              isExpanded
                                ? <ChevronDown className="h-4 w-4 text-(--text-secondary)" />
                                : <ChevronRight className="h-4 w-4 text-(--text-secondary)" />
                            )}
                          </div>
                          <div className="px-4 py-3 text-sm flex-1">
                            <Badge variant={actionBadge[entry.action] || 'neutral'}>{entry.action}</Badge>
                          </div>
                          <div className="px-4 py-3 text-sm flex-1">{entry.entityType}</div>
                          <div className="px-4 py-3 text-sm flex-1">{entry.actorName || '-'}</div>
                          <div className="px-4 py-3 text-sm flex-1">{entry.entityId ? entry.entityId.slice(0, 8) + '...' : '-'}</div>
                          <div className="px-4 py-3 text-sm flex-1">{formatDate(entry.createdAt)}</div>
                        </div>
                        {isExpanded && (
                          <div className="px-6 pb-4 pt-1">
                            <div className="glass-dark rounded-xl p-4 grid grid-cols-1 md:grid-cols-2 gap-4">
                              <div>
                                <p className="text-xs font-semibold text-(--text-secondary) mb-2 uppercase tracking-wider">Stara vrijednost</p>
                                <pre className="text-xs font-mono text-(--text-primary) bg-black/20 rounded-lg p-3 overflow-auto max-h-64 whitespace-pre-wrap wrap-break-word">
                                  {formatJson(entry.oldValue)}
                                </pre>
                              </div>
                              <div>
                                <p className="text-xs font-semibold text-(--text-secondary) mb-2 uppercase tracking-wider">Nova vrijednost</p>
                                <pre className="text-xs font-mono text-(--text-primary) bg-black/20 rounded-lg p-3 overflow-auto max-h-64 whitespace-pre-wrap wrap-break-word">
                                  {formatJson(entry.newValue)}
                                </pre>
                              </div>
                            </div>
                          </div>
                        )}
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
        {totalPages > 1 && (
          <div className="flex items-center justify-between px-4 py-3 border-t border-white/10">
            <span className="text-xs text-(--text-secondary)">
              Stranica {page + 1} od {totalPages}
            </span>
            <div className="flex gap-2">
              <button
                onClick={() => setPage(page - 1)}
                disabled={page === 0}
                className="px-3 py-1 text-xs rounded-lg glass hover:bg-white/10 disabled:opacity-40 transition-colors"
              >
                Prethodna
              </button>
              <button
                onClick={() => setPage(page + 1)}
                disabled={page >= totalPages - 1}
                className="px-3 py-1 text-xs rounded-lg glass hover:bg-white/10 disabled:opacity-40 transition-colors"
              >
                Sljedeća
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
