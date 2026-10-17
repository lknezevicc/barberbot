export interface Tenant {
  id: string;
  name: string;
  botToken: string;
  botUsername: string;
  secretToken: string;
  contactEmail: string;
  address: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface Employee {
  id: string;
  tenantId: string;
  name: string;
  specialization: string;
  contact: string;
  active: boolean;
  createdAt: string;
}

export interface ServiceItem {
  id: string;
  tenantId: string;
  name: string;
  durationMinutes: number;
  price: number;
  description: string;
  active: boolean;
  createdAt: string;
}

export interface Booking {
  id: string;
  userId: string;
  customerName: string | null;
  customerPhone: string | null;
  barber: string;
  service: string;
  date: string;
  time: string;
  durationMinutes: number;
  status: BookingStatus;
  tenantId: string;
  employeeId: string;
  serviceId: string;
  createdAt: string;
}

export type BookingStatus = 'CONFIRMED' | 'CANCELLED' | 'HELD' | 'EXPIRED';

export interface WorkingHours {
  id: string;
  employeeId: string;
  tenantId: string;
  dayOfWeek: number;
  startTime: string;
  endTime: string;
}

export interface BlockedSlot {
  id: string;
  employeeId: string;
  tenantId: string;
  date: string;
  startTime: string;
  endTime: string;
  reason: string;
  allDay: boolean;
}

export interface AppUser {
  id: string;
  tenantId: string;
  telegramId: string;
  chatId: number;
  firstName: string;
  lastName: string;
  phone: string;
  email: string;
  role: string;
  createdAt: string;
  updatedAt: string;
}

export interface AuditEntry {
  id: string;
  actorId: string;
  actorName: string;
  action: string;
  entityType: string;
  entityId: string;
  oldValue: string;
  newValue: string;
  createdAt: string;
}

export interface Stats {
  todayBookings: number;
  totalConfirmed: number;
  totalCancelled: number;
  cancellationRate: number;
  mostPopularBarber: string | null;
  mostPopularService: string | null;
  thisWeekBookings: number;
  weeklyRevenue: number;
}

export interface CalendarResponse {
  employees: Employee[];
  workingHours: WorkingHours[];
  blockedSlots: BlockedSlot[];
  bookings: Booking[];
}

export interface AuthUser {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  tenantId: string;
  tenantName: string;
  tenantBotUsername: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface LoginResponse {
  token: string;
  role: string;
  tenantId: string;
  userId: string;
  name: string;
}

export interface RegisterRequest {
  salonName: string;
  botToken: string;
  botUsername: string;
  secretToken: string;
  address: string;
  contactEmail: string;
  adminName: string;
  email: string;
  password: string;
}
