import { create } from 'zustand';
import axios from 'axios';
import { User } from '../types';

interface AuthState {
  user: User | null;
  accessToken: string | null;
  isAuthenticated: boolean;
  isInitializing: boolean;
  setAuth: (user: User, accessToken: string) => void;
  updateUser: (user: User) => void;
  logout: () => void;
  initializeAuth: () => Promise<void>;
}

const savedUser = localStorage.getItem('user_info');
const initialUser: User | null = savedUser ? JSON.parse(savedUser) : null;

export const useAuthStore = create<AuthState>((set, get) => ({
  user: initialUser,
  accessToken: null,
  isAuthenticated: false,
  isInitializing: true,

  setAuth: (user, accessToken) => {
    localStorage.setItem('user_info', JSON.stringify(user));
    set({ user, accessToken, isAuthenticated: true });
  },

  updateUser: (user) => {
    localStorage.setItem('user_info', JSON.stringify(user));
    set({ user });
  },

  logout: async () => {
    const token = get().accessToken;
    if (token) {
      try {
        await axios.post(
          `${import.meta.env.VITE_API_BASE_URL || '/api'}/auth/logout`,
          {},
          {
            headers: { Authorization: `Bearer ${token}` },
            withCredentials: true,
          }
        );
      } catch {
        // Ignore backend logout errors - local state must always be cleared
      }
    }
    localStorage.removeItem('user_info');
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    set({ user: null, accessToken: null, isAuthenticated: false });
  },

  initializeAuth: async () => {
    try {
      const response: any = await axios.post(
        `${import.meta.env.VITE_API_BASE_URL || '/api'}/auth/refresh`,
        {},
        { withCredentials: true }
      );
      if (response.data && response.data.success && response.data.data) {
        const { accessToken, user } = response.data.data;
        get().setAuth(user, accessToken);
      } else {
        await get().logout();
      }
    } catch {
      await get().logout();
    } finally {
      set({ isInitializing: false });
    }
  },
}));
