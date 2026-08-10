import axiosClient from '../../api/axiosClient';
import { Milestone } from '../../types';

export const getMilestones = async (projectId: number): Promise<Milestone[]> => {
  const response: any = await axiosClient.get(`/projects/${projectId}/milestones`);
  return response.data;
};

export const createMilestone = async (
  projectId: number,
  data: { name: string; dueDate?: string }
): Promise<Milestone> => {
  const response: any = await axiosClient.post(`/projects/${projectId}/milestones`, data);
  return response.data;
};

export const updateMilestone = async (
  projectId: number,
  milestoneId: number,
  data: { name?: string; dueDate?: string; status?: 'OPEN' | 'CLOSED' }
): Promise<Milestone> => {
  const response: any = await axiosClient.put(`/projects/${projectId}/milestones/${milestoneId}`, data);
  return response.data;
};

export const deleteMilestone = async (projectId: number, milestoneId: number): Promise<void> => {
  await axiosClient.delete(`/projects/${projectId}/milestones/${milestoneId}`);
};
