import type { AuthUser, LoginResponse } from '@/types';

const API_BASE = '/api';

function isTokenExpired(): boolean {
  const token = localStorage.getItem('token');
  if (!token) return true;
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    return payload.exp * 1000 < Date.now();
  } catch {
    return true;
  }
}

function getToken(): string | null {
  if (isTokenExpired()) {
    localStorage.removeItem('token');
    return null;
  }
  return localStorage.getItem('token');
}

class ApiError extends Error {
  status: number;
  constructor(status: number, message: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getToken();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...((options.headers as Record<string, string>) || {}),
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
  });

  if (response.status === 401) {
    localStorage.removeItem('token');
    window.location.href = '/login';
    throw new ApiError(401, 'Neovlasten pristup');
  }

  if (response.status === 204) {
    return undefined as T;
  }

  if (!response.ok) {
    const body = await response.json().catch(() => ({ error: 'Nepoznata greška' }));
    throw new ApiError(response.status, body.error || body.message || `HTTP ${response.status}`);
  }

  return response.json();
}

export const api = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'POST', body: body ? JSON.stringify(body) : undefined }),
  put: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'PUT', body: body ? JSON.stringify(body) : undefined }),
  delete: <T>(path: string) => request<T>(path, { method: 'DELETE' }),
};

export async function login(email: string, password: string): Promise<LoginResponse> {
  const res = await api.post<LoginResponse>('/auth/login', { email, password });
  localStorage.setItem('token', res.token);
  return res;
}

export async function register(data: {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  salonName: string;
  address: string;
  contactEmail: string;
  phone: string;
  botToken: string;
  botUsername: string;
}): Promise<{ token: string; tenantId: string }> {
  const res = await api.post<{ token: string; tenantId: string }>('/auth/register', data);
  localStorage.setItem('token', res.token);
  return res;
}

export function logout(): void {
  localStorage.removeItem('token');
  window.location.href = '/login';
}

export function getMe(): Promise<AuthUser> {
  return api.get<AuthUser>('/auth/me');
}

export function changePassword(currentPassword: string, newPassword: string): Promise<{ message: string }> {
  return api.post<{ message: string }>('/auth/change-password', { currentPassword, newPassword });
}

export { isTokenExpired, ApiError };
