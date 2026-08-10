import axiosClient from '../../api/axiosClient';
import { TestRunCase } from '../../types';

export interface ExecutionPayload {
  resultStatus: 'PASSED' | 'FAILED' | 'BLOCKED' | 'RETEST' | 'UNTESTED';
  comment?: string;
  defectRef?: string;
}

export interface AttachmentDto {
  id: number;
  entityType: string;
  entityId: number;
  fileName: string;
  downloadUrl: string;
  uploadedById?: number;
  uploadedByName?: string;
  uploadedAt: string;
}

export interface ExecutionHistoryItem {
  id: number;
  runCaseId: number;
  resultStatus: 'PASSED' | 'FAILED' | 'BLOCKED' | 'RETEST' | 'UNTESTED';
  comment?: string;
  executedBy?: string;
  executedAt: string;
  durationMs?: number;
  attachments?: AttachmentDto[];
}

export const recordExecution = async (
  runId: number,
  caseId: number,
  payload: ExecutionPayload
): Promise<TestRunCase> => {
  const response: any = await axiosClient.post(`/runs/${runId}/cases/${caseId}/execute`, payload);
  return response.data;
};

export const reviewResult = async (
  runId: number,
  caseId: number,
  reviewed: boolean,
  comment?: string
): Promise<TestRunCase> => {
  const response: any = await axiosClient.post(`/runs/${runId}/cases/${caseId}/review`, null, {
    params: { reviewed, comment },
  });
  return response.data;
};

export const uploadAttachment = async (
  entityType: string,
  entityId: number,
  file: File
): Promise<AttachmentDto> => {
  const formData = new FormData();
  formData.append('entityType', entityType);
  formData.append('entityId', String(entityId));
  formData.append('file', file);

  const response: any = await axiosClient.post('/attachments/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
  return response.data;
};

export const getExecutionHistory = async (
  runId: number,
  caseId: number
): Promise<ExecutionHistoryItem[]> => {
  const response: any = await axiosClient.get(`/runs/${runId}/cases/${caseId}/history`);
  return response.data;
};

export const getExecutionAttachments = async (
  executionHistoryId: number
): Promise<AttachmentDto[]> => {
  const response: any = await axiosClient.get(`/executions/${executionHistoryId}/attachments`);
  return response.data;
};

export const fetchAttachmentBlob = async (downloadUrl: string): Promise<string> => {
  const response: any = await axiosClient.get(downloadUrl, {
    responseType: 'blob',
  });
  return URL.createObjectURL(response.data || response);
};
