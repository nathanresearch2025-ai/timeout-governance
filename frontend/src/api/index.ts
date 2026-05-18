import axios from 'axios';
import type { TimeoutPolicy, TimeoutEvent, PageResponse } from '../types';

const api = axios.create({
  baseURL: '/api/v1',
});

export const policyApi = {
  list(params: { page?: number; size?: number; teamId?: string; level?: string; enabled?: boolean }) {
    return api.get<PageResponse<TimeoutPolicy>>('/policies', { params });
  },

  getById(id: string) {
    return api.get<TimeoutPolicy>(`/policies/${id}`);
  },

  create(policy: Omit<TimeoutPolicy, 'id' | 'createdAt' | 'updatedAt'>) {
    return api.post<TimeoutPolicy>('/policies', policy);
  },

  update(id: string, policy: Omit<TimeoutPolicy, 'id' | 'createdAt' | 'updatedAt'>) {
    return api.put<TimeoutPolicy>(`/policies/${id}`, policy);
  },

  delete(id: string) {
    return api.delete(`/policies/${id}`);
  },
};

export const timeoutEventApi = {
  list(params: { page?: number; size?: number }) {
    return api.get<PageResponse<TimeoutEvent>>('/timeout-events', { params });
  },

  listUnresolved() {
    return api.get<TimeoutEvent[]>('/timeout-events/unresolved');
  },
};
