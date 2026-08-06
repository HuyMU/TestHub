import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { MainLayout } from '../layouts/MainLayout';

interface ProtectedRouteProps {
  children: React.ReactNode;
  requireLeader?: boolean;
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children, requireLeader }) => {
  const { isAuthenticated, user } = useAuthStore();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (requireLeader && user?.role !== 'LEADER') {
    return <Navigate to="/dashboard" replace />;
  }

  return <MainLayout>{children}</MainLayout>;
};
