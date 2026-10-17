import { useState, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { api } from '@/lib/api';
import { Spinner } from '@/components/ui';
import type { Booking, CalendarResponse, Employee } from '@/types';

interface CalendarViewProps {
  weekStart: string;
  onWeekChange: (weekStart: string) => void;
  onBookingClick: (booking: Booking) => void;
  onSlotClick: (employeeId: string, date: string, time: string) => void;
}

const DAY_LABELS = ['Pon', 'Uto', 'Sri', 'Cet', 'Pet', 'Sub', 'Ned'];
const SLOT_START_HOUR = 8;
const SLOT_END_HOUR = 20;
const SLOT_HEIGHT_PX = 32;
const TOTAL_SLOTS = (SLOT_END_HOUR - SLOT_START_HOUR) * 2;

function formatDate(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

function addDays(dateStr: string, days: number): string {
  const d = new Date(dateStr + 'T00:00:00');
  d.setDate(d.getDate() + days);
  return formatDate(d);
}

function formatDisplayDate(dateStr: string): string {
  const d = new Date(dateStr + 'T00:00:00');
  return `${d.getDate()}.${d.getMonth() + 1}.`;
}

function timeToMinutes(time: string): number {
  const [h, m] = time.split(':').map(Number);
  return h * 60 + m;
}

function generateTimeSlots(): string[] {
  const slots: string[] = [];
  for (let h = SLOT_START_HOUR; h < SLOT_END_HOUR; h++) {
    slots.push(`${String(h).padStart(2, '0')}:00`);
    slots.push(`${String(h).padStart(2, '0')}:30`);
  }
  return slots;
}

function getDayOfWeek(dateStr: string): number {
  const d = new Date(dateStr + 'T00:00:00');
  const jsDay = d.getDay();
  return jsDay === 0 ? 7 : jsDay;
}

function minutesToSlotOffset(minutes: number): number {
  return (minutes - SLOT_START_HOUR * 60) / 30;
}

const statusColors: Record<string, string> = {
  CONFIRMED: 'bg-emerald-500/30 border-emerald-500/50 text-emerald-300 hover:bg-emerald-500/40',
  HELD: 'bg-amber-500/30 border-amber-500/50 text-amber-300 hover:bg-amber-500/40',
  CANCELLED: 'bg-red-500/30 border-red-500/50 text-red-300 hover:bg-red-500/40',
  EXPIRED: 'bg-slate-500/30 border-slate-500/50 text-slate-300',
};

export default function CalendarView({ weekStart, onWeekChange, onBookingClick, onSlotClick }: CalendarViewProps) {
  const [selectedEmployeeId, setSelectedEmployeeId] = useState<string>('');
  const timeSlots = useMemo(() => generateTimeSlots(), []);

  const { data, isLoading } = useQuery<CalendarResponse>({
    queryKey: ['bookings-calendar', weekStart],
    queryFn: () => api.get(`/bookings/calendar?weekStart=${weekStart}`),
  });

  const employees = data?.employees ?? [];
  const activeEmployee = selectedEmployeeId || employees[0]?.id || '';

  const weekDates = useMemo(() => {
    return Array.from({ length: 7 }, (_, i) => addDays(weekStart, i));
  }, [weekStart]);

  const workingHoursMap = useMemo(() => {
    if (!data) return new Map<string, { start: number; end: number }>();
    const map = new Map<string, { start: number; end: number }>();
    for (const wh of data.workingHours) {
      if (wh.employeeId === activeEmployee) {
        const key = String(wh.dayOfWeek);
        map.set(key, { start: timeToMinutes(wh.startTime), end: timeToMinutes(wh.endTime) });
      }
    }
    return map;
  }, [data, activeEmployee]);

  const blockedSlotsMap = useMemo(() => {
    if (!data) return new Map<string, Array<{ start: number; end: number }>>();
    const map = new Map<string, Array<{ start: number; end: number }>>();
    for (const bs of data.blockedSlots) {
      if (bs.employeeId === activeEmployee) {
        const existing = map.get(bs.date) ?? [];
        if (bs.allDay) {
          existing.push({ start: SLOT_START_HOUR * 60, end: SLOT_END_HOUR * 60 });
        } else if (bs.startTime && bs.endTime) {
          existing.push({ start: timeToMinutes(bs.startTime), end: timeToMinutes(bs.endTime) });
        }
        map.set(bs.date, existing);
      }
    }
    return map;
  }, [data, activeEmployee]);

  const dayBookingsMap = useMemo(() => {
    if (!data) return new Map<string, Booking[]>();
    const map = new Map<string, Booking[]>();
    const empName = employees.find(e => e.id === activeEmployee)?.name;
    for (const b of data.bookings) {
      if (b.employeeId === activeEmployee || b.barber === empName) {
        const existing = map.get(b.date) ?? [];
        existing.push(b);
        map.set(b.date, existing);
      }
    }
    return map;
  }, [data, activeEmployee, employees]);

  function isWithinWorkingHours(dayOfWeek: number, slotMinutes: number): boolean {
    const wh = workingHoursMap.get(String(dayOfWeek));
    if (!wh) return false;
    return slotMinutes >= wh.start && slotMinutes < wh.end;
  }

  function isBlocked(date: string, slotMinutes: number): boolean {
    const blocks = blockedSlotsMap.get(date);
    if (!blocks) return false;
    return blocks.some(b => slotMinutes >= b.start && slotMinutes < b.end);
  }

  if (isLoading) {
    return (
      <div className="glass rounded-2xl p-12 flex items-center justify-center">
        <Spinner size="lg" />
      </div>
    );
  }

  return (
    <div className="glass rounded-2xl p-4 space-y-4">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <button
            onClick={() => onWeekChange(addDays(weekStart, -7))}
            className="p-2 rounded-xl hover:bg-white/10 transition-colors"
          >
            <ChevronLeft className="h-5 w-5" />
          </button>
          <span className="text-sm font-medium min-w-[180px] text-center">
            {formatDisplayDate(weekStart)} - {formatDisplayDate(addDays(weekStart, 6))}
          </span>
          <button
            onClick={() => onWeekChange(addDays(weekStart, 7))}
            className="p-2 rounded-xl hover:bg-white/10 transition-colors"
          >
            <ChevronRight className="h-5 w-5" />
          </button>
        </div>
      </div>

      {employees.length > 0 && (
        <div className="flex gap-1 overflow-x-auto pb-1">
          {employees.map((emp: Employee) => (
            <button
              key={emp.id}
              onClick={() => setSelectedEmployeeId(emp.id)}
              className={`px-4 py-2 rounded-xl text-sm font-medium whitespace-nowrap transition-all ${
                activeEmployee === emp.id
                  ? 'bg-(--primary-600) text-white shadow-md'
                  : 'bg-white/5 hover:bg-white/10 text-(--text-secondary)'
              }`}
            >
              {emp.name}
            </button>
          ))}
        </div>
      )}

      <div className="overflow-x-auto">
        <div className="min-w-[700px]">
          {/* Header row */}
          <div className="grid grid-cols-[60px_repeat(7,1fr)] gap-px mb-px">
            <div className="bg-white/5 rounded-tl-xl p-2" />
            {weekDates.map((date, i) => (
              <div key={date} className={`bg-white/5 p-2 text-center ${i === 6 ? 'rounded-tr-xl' : ''}`}>
                <div className="text-xs font-medium text-(--text-secondary)">{DAY_LABELS[i]}</div>
                <div className="text-sm font-semibold">{formatDisplayDate(date)}</div>
              </div>
            ))}
          </div>

          {/* Body */}
          <div className="grid grid-cols-[60px_repeat(7,1fr)] gap-px">
            {/* Time labels column */}
            <div className="flex flex-col">
              {timeSlots.map((time) => (
                <div
                  key={time}
                  className="bg-white/5 px-2 text-xs text-(--text-secondary) flex items-center justify-end"
                  style={{ height: SLOT_HEIGHT_PX }}
                >
                  {time}
                </div>
              ))}
            </div>

            {/* Day columns */}
            {weekDates.map((date) => {
              const dayOfWeek = getDayOfWeek(date);
              const bookings = dayBookingsMap.get(date) ?? [];

              return (
                <div key={date} className="relative">
                  {/* Background slots */}
                  {timeSlots.map((time) => {
                    const slotMinutes = timeToMinutes(time);
                    const inWorkHours = isWithinWorkingHours(dayOfWeek, slotMinutes);
                    const blocked = isBlocked(date, slotMinutes);

                    return (
                      <div
                        key={time}
                        className={`border-b border-white/5 transition-colors ${
                          blocked
                            ? 'bg-slate-600/30'
                            : inWorkHours
                            ? 'bg-white/5 hover:bg-white/10 cursor-pointer'
                            : 'bg-white/[0.02]'
                        }`}
                        style={{
                          height: SLOT_HEIGHT_PX,
                          ...(blocked
                            ? { backgroundImage: 'repeating-linear-gradient(45deg, transparent, transparent 4px, rgba(100,116,139,0.2) 4px, rgba(100,116,139,0.2) 8px)' }
                            : {}),
                        }}
                        onClick={() => {
                          if (inWorkHours && !blocked) onSlotClick(activeEmployee, date, time);
                        }}
                      />
                    );
                  })}

                  {/* Bookings overlay */}
                  {bookings.map((booking) => {
                    const bookingStartMinutes = timeToMinutes(booking.time);
                    const durationMinutes = booking.durationMinutes || 30;
                    const topOffset = minutesToSlotOffset(bookingStartMinutes) * SLOT_HEIGHT_PX;
                    const height = (durationMinutes / 30) * SLOT_HEIGHT_PX;

                    if (topOffset < 0 || topOffset >= TOTAL_SLOTS * SLOT_HEIGHT_PX) return null;

                    const colorClass = statusColors[booking.status] ?? statusColors.CONFIRMED;

                    return (
                      <button
                        key={booking.id}
                        onClick={() => onBookingClick(booking)}
                        className={`absolute left-1 right-1 rounded-lg border text-left px-2 py-1 overflow-hidden cursor-pointer transition-colors z-10 ${colorClass}`}
                        style={{ top: topOffset, height: Math.max(height - 2, SLOT_HEIGHT_PX - 2) }}
                      >
                        <div className="text-xs font-medium truncate">{booking.customerName || booking.service}</div>
                        <div className="text-[10px] opacity-80 truncate">{booking.time} - {booking.service}</div>
                      </button>
                    );
                  })}
                </div>
              );
            })}
          </div>
        </div>
      </div>

      <div className="flex items-center gap-4 pt-2 text-xs text-(--text-secondary)">
        <div className="flex items-center gap-1.5">
          <div className="w-3 h-3 rounded bg-emerald-500/30 border border-emerald-500/50" />
          <span>Potvrđen</span>
        </div>
        <div className="flex items-center gap-1.5">
          <div className="w-3 h-3 rounded bg-amber-500/30 border border-amber-500/50" />
          <span>Na čekanju</span>
        </div>
        <div className="flex items-center gap-1.5">
          <div className="w-3 h-3 rounded bg-red-500/30 border border-red-500/50" />
          <span>Otkazan</span>
        </div>
        <div className="flex items-center gap-1.5">
          <div
            className="w-3 h-3 rounded bg-slate-600/30"
            style={{
              backgroundImage: 'repeating-linear-gradient(45deg, transparent, transparent 2px, rgba(100,116,139,0.3) 2px, rgba(100,116,139,0.3) 4px)',
            }}
          />
          <span>Blokirano</span>
        </div>
      </div>
    </div>
  );
}
