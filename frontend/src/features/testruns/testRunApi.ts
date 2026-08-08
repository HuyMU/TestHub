import axiosClient from '../../api/axiosClient';
import { TestRun } from '../../types';

export interface RunCaseItemPayload {
  caseId: number;
  assignedToId?: number;
}

export interface CreateTestRunPayload {
  name: string;
  milestoneId?: number;
  includeNonReady?: boolean;
  cases: RunCaseItemPayload[];
}

export const getTestRuns = async (projectId: number): Promise<TestRun[]> => {
  const response = await axiosClient.get(`/projects/${projectId}/runs`);
  return response.data.data;
};

export const getTestRunDetail = async (runId: number): Promise<TestRun> => {
  const response = await axiosClient.get(`/runs/${runId}`);
  return response.data.data;
};

export const createTestRun = async (
  projectId: number,
  data: CreateTestRunPayload
): Promise<TestRun> => {
  const response = await axiosClient.post(`/projects/${projectId}/runs`, data);
  return response.data.data;
};

export const addCasesToRun = async (
  runId: number,
  cases: RunCaseItemPayload[]
): Promise<TestRun> => {
  const response = await axiosClient.post(`/runs/${runId}/cases`, { cases });
  return response.data.data;
};

export const removeCaseFromRun = async (runId: number, runCaseId: number): Promise<void> => {
  await axiosClient.delete(`/runs/${runId}/cases/${runCaseId}`);
};

export const closeTestRun = async (runId: number): Promise<TestRun> => {
  const response = await axiosClient.post(`/runs/${runId}/close`);
  return response.data.data;
};
