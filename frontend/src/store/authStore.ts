import { create } from 'zustand';
import { User } from '../types';

interface AuthState {
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  setAuth: (user: User, accessToken: string, refreshToken: string) => void;
  updateUser: (user: User) => void;
  logout: () => void;
}

const savedUser = localStorage.getItem('user_info');
const initialUser: User | null = savedUser ? JSON.parse(savedUser) : null;
const initialAccessToken = localStorage.getItem('access_token');
const initialRefreshToken = localStorage.getItem('refresh_token');

export const useAuthStore = create<AuthState>((set) => ({
  user: initialUser,
  accessToken: initialAccessToken,
  refreshToken: initialRefreshToken,
  isAuthenticated: !!(initialAccessToken && initialUser),
  setAuth: (user, accessToken, refreshToken) => {
    localStorage.setItem('access_token', accessToken);
    localStorage.setItem('refresh_token', refreshToken);
    localStorage.setItem('user_info', JSON.stringify(user));
    set({ user, accessToken, refreshToken, isAuthenticated: true });
  },
  updateUser: (user) => {
    localStorage.setItem('user_info', JSON.stringify(user));
    set({ user });
  },
  logout: () => {
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    localStorage.removeItem('user_info');
    set({ user: null, accessToken: null, refreshToken: null, isAuthenticated: false });
  },
}));
