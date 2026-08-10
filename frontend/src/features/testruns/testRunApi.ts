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
  const response: any = await axiosClient.get(`/projects/${projectId}/runs`);
  return response.data;
};

export const getTestRunDetail = async (runId: number): Promise<TestRun> => {
  const response: any = await axiosClient.get(`/runs/${runId}`);
  return response.data;
};

export const createTestRun = async (
  projectId: number,
  data: CreateTestRunPayload
): Promise<TestRun> => {
  const response: any = await axiosClient.post(`/projects/${projectId}/runs`, data);
  return response.data;
};

export const addCasesToRun = async (
  runId: number,
  cases: RunCaseItemPayload[],
  includeNonReady?: boolean
): Promise<TestRun> => {
  const response: any = await axiosClient.post(`/runs/${runId}/cases`, {
    includeNonReady,
    cases,
  });
  return response.data;
};

export const removeCaseFromRun = async (runId: number, runCaseId: number): Promise<void> => {
  await axiosClient.delete(`/runs/${runId}/cases/${runCaseId}`);
};

export const closeTestRun = async (runId: number): Promise<TestRun> => {
  const response: any = await axiosClient.post(`/runs/${runId}/close`);
  return response.data;
};

export interface TestRunCaseReport {
  caseId: number;
  code: string;
  title: string;
  assignedToName: string;
  resultStatus: string;
  executedBy: string;
  executedAt: string | null;
  comment: string | null;
  defectRef: string | null;
}

export interface TestRunReport {
  runId: number;
  runName: string;
  projectName: string;
  milestoneName: string;
  runStatus: string;
  closedAt: string | null;
  totalCases: number;
  passedCases: number;
  failedCases: number;
  blockedCases: number;
  retestCases: number;
  untestedCases: number;
  passRatePercentage: number;
  completionPercentage: number;
  cases: TestRunCaseReport[];
}

export const getTestRunReport = async (runId: number): Promise<TestRunReport> => {
  const response: any = await axiosClient.get(`/runs/${runId}/report`);
  return response.data;
};

export const exportTestRunReport = async (runId: number, runName: string): Promise<void> => {
  const response: any = await axiosClient.get(`/runs/${runId}/report/export`, {
    responseType: 'blob',
  });
  const blob = new Blob([response.data || response], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  });
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', `TestRun_Report_${runName.replace(/[^a-zA-Z0-9_-]/g, '_')}_${runId}.xlsx`);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
};
