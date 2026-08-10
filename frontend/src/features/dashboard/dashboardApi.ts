import axiosClient from '../../api/axiosClient';
import { ApiResponse } from '../../types';

export interface MilestoneProgressDto {
  milestoneId: number;
  milestoneName: string;
  dueDate: string | null;
  status: string;
  totalRuns: number;
  totalCases: number;
  completedCases: number;
  progressPercentage: number;
}

export interface DashboardDto {
  totalCases: number;
  readyCases: number;
  reviewQueueCount: number;
  passedCount: number;
  failedCount: number;
  blockedCount: number;
  retestCount: number;
  untestedCount: number;
  milestoneProgress: MilestoneProgressDto[];
}

export const dashboardApi = {
  getDashboard: async (projectId: number): Promise<DashboardDto> => {
    const response: any = await axiosClient.get(`/dashboard/${projectId}`);
    return response.data;
  },
};
