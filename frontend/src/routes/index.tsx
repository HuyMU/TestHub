import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { ProtectedRoute } from './ProtectedRoute';
import { PublicRoute } from './PublicRoute';
import { LoginPage } from '../features/auth/LoginPage';
import { DashboardPage } from '../features/dashboard/DashboardPage';
import { ProjectListPage } from '../features/projects/ProjectListPage';
import { ProjectDetailPage } from '../features/projects/ProjectDetailPage';
import { TestCaseListPage } from '../features/testcases/TestCaseListPage';
import { ReviewQueuePage } from '../features/testcases/ReviewQueuePage';
import { TestRunListPage } from '../features/testruns/TestRunListPage';
import { TestRunDetailPage } from '../features/testruns/TestRunDetailPage';
import { MilestoneListPage } from '../features/milestones/MilestoneListPage';
import { UserListPage } from '../features/users/UserListPage';
import { ApiTokenPage } from '../features/automation-tokens/ApiTokenPage';
import { ExecutionPage } from '../features/execution/ExecutionPage';

export const AppRoutes: React.FC = () => {
  return (
    <Routes>
      <Route
        path="/login"
        element={
          <PublicRoute>
            <LoginPage />
          </PublicRoute>
        }
      />
      <Route
        path="/dashboard"
        element={
          <ProtectedRoute>
            <DashboardPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/projects"
        element={
          <ProtectedRoute>
            <ProjectListPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/projects/:id"
        element={
          <ProtectedRoute>
            <ProjectDetailPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/testcases"
        element={
          <ProtectedRoute>
            <TestCaseListPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/review-queue"
        element={
          <ProtectedRoute requireLeader>
            <ReviewQueuePage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/testruns"
        element={
          <ProtectedRoute>
            <TestRunListPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/runs/:runId"
        element={
          <ProtectedRoute>
            <TestRunDetailPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/execution/:runId"
        element={
          <ProtectedRoute>
            <ExecutionPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/milestones"
        element={
          <ProtectedRoute>
            <MilestoneListPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/users"
        element={
          <ProtectedRoute requireLeader>
            <UserListPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/api-tokens"
        element={
          <ProtectedRoute requireLeader>
            <ApiTokenPage />
          </ProtectedRoute>
        }
      />
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
};
