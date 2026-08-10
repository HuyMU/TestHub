import axiosClient from '../../api/axiosClient';
import { TestCase, Priority, TestType, CaseStatus, AutomationStatus } from '../../types';

export interface TestCaseFilterParams {
  sectionId?: number | null;
  priority?: Priority;
  type?: TestType;
  status?: CaseStatus;
  automationStatus?: AutomationStatus;
  keyword?: string;
  page?: number;
  size?: number;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface CreateTestCasePayload {
  sectionId: number;
  title: string;
  precondition?: string;
  steps: string;
  expectedResult: string;
  testData?: string;
  priority: Priority;
  type: TestType;
  automationStatus: AutomationStatus;
}

export interface UpdateTestCasePayload {
  sectionId: number;
  title: string;
  precondition?: string;
  steps: string;
  expectedResult: string;
  testData?: string;
  priority: Priority;
  type: TestType;
  automationStatus: AutomationStatus;
}

export interface RejectTestCasePayload {
  reviewComment: string;
}

export const getTestCases = async (
  projectId: number,
  params: TestCaseFilterParams = {}
): Promise<PageResponse<TestCase>> => {
  const response: any = await axiosClient.get(`/projects/${projectId}/cases`, { params });
  return response.data;
};

export const getTestCaseById = async (caseId: number): Promise<TestCase> => {
  const response: any = await axiosClient.get(`/cases/${caseId}`);
  return response.data;
};

export const createTestCase = async (
  projectId: number,
  payload: CreateTestCasePayload
): Promise<TestCase> => {
  const response: any = await axiosClient.post(`/projects/${projectId}/cases`, payload);
  return response.data;
};

export const updateTestCase = async (
  caseId: number,
  payload: UpdateTestCasePayload
): Promise<TestCase> => {
  const response: any = await axiosClient.put(`/cases/${caseId}`, payload);
  return response.data;
};

export const deleteTestCase = async (caseId: number): Promise<void> => {
  await axiosClient.delete(`/cases/${caseId}`);
};

export const submitForReview = async (caseId: number): Promise<TestCase> => {
  const response: any = await axiosClient.post(`/cases/${caseId}/submit-review`);
  return response.data;
};

export const approveTestCase = async (caseId: number): Promise<TestCase> => {
  const response: any = await axiosClient.post(`/cases/${caseId}/approve`);
  return response.data;
};

export const rejectTestCase = async (
  caseId: number,
  payload: RejectTestCasePayload
): Promise<TestCase> => {
  const response: any = await axiosClient.post(`/cases/${caseId}/reject`, payload);
  return response.data;
};

export const cloneTestCase = async (caseId: number): Promise<TestCase> => {
  const response: any = await axiosClient.post(`/cases/${caseId}/clone`);
  return response.data;
};

export const getReviewQueue = async (): Promise<TestCase[]> => {
  const response: any = await axiosClient.get('/cases/review-queue');
  return response.data;
};
