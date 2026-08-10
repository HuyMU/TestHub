import axiosClient from '../../api/axiosClient';
import { TestRunCase } from '../../types';

export interface ExecutionPayload {
  resultStatus: 'PASSED' | 'FAILED' | 'BLOCKED' | 'RETEST' | 'UNTESTED';
  comment?: string;
  defectRef?: string;
  attachmentUrl?: string;
}

export interface ExecutionHistoryItem {
  id: number;
  runCaseId: number;
  resultStatus: 'PASSED' | 'FAILED' | 'BLOCKED' | 'RETEST' | 'UNTESTED';
  comment?: string;
  executedBy?: string;
  executedAt: string;
}

export const recordExecution = async (
  runId: number,
  caseId: number,
  payload: ExecutionPayload
): Promise<TestRunCase> => {
  const response = await axiosClient.post(`/runs/${runId}/cases/${caseId}/execute`, payload);
  return response.data.data;
};

export const reviewResult = async (
  runId: number,
  caseId: number,
  reviewed: boolean,
  comment?: string
): Promise<TestRunCase> => {
  const response = await axiosClient.post(`/runs/${runId}/cases/${caseId}/review`, null, {
    params: { reviewed, comment },
  });
  return response.data.data;
};

export const uploadAttachment = async (
  entityType: string,
  entityId: number,
  file: File
): Promise<string> => {
  const formData = new FormData();
  formData.append('entityType', entityType);
  formData.append('entityId', String(entityId));
  formData.append('file', file);

  const response = await axiosClient.post('/attachments/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
  return response.data.data;
};

export const getExecutionHistory = async (
  runId: number,
  caseId: number
): Promise<ExecutionHistoryItem[]> => {
  const response = await axiosClient.get(`/runs/${runId}/cases/${caseId}/history`);
  return response.data.data;
};
