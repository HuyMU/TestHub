export type Role = 'LEADER' | 'TESTER';

export type Priority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export type TestType =
  | 'FUNCTIONAL'
  | 'REGRESSION'
  | 'SMOKE'
  | 'PERFORMANCE'
  | 'SECURITY'
  | 'USABILITY'
  | 'OTHER';

export type AutomationStatus = 'MANUAL' | 'AUTOMATED' | 'TO_AUTOMATE';

export type CaseStatus = 'DRAFT' | 'REVIEW' | 'READY';

export type ResultStatus = 'PASSED' | 'FAILED' | 'BLOCKED' | 'RETEST' | 'UNTESTED';

export interface User {
  id: number;
  username: string;
  email: string;
  fullName: string;
  role: Role;
  isActive: boolean;
}

export interface Project {
  id: number;
  name: string;
  description?: string;
  status: 'Active' | 'Archived';
  createdBy?: User;
  createdAt?: string;
  memberCount?: number;
}

export interface Section {
  id: number;
  projectId: number;
  parentSectionId?: number | null;
  name: string;
  sortOrder: number;
  createdAt?: string;
  directTestCaseCount?: number;
  directSubsectionCount?: number;
  children?: Section[];
}

export interface TestCase {
  id: number;
  code: string;
  projectId?: number;
  sectionId: number;
  sectionName?: string;
  title: string;
  precondition?: string;
  steps: string;
  expectedResult: string;
  testData?: string;
  priority: Priority;
  type: TestType;
  automationStatus: AutomationStatus;
  status: CaseStatus;
  reviewComment?: string;
  createdById?: number;
  createdByFullName?: string;
  reviewedById?: number;
  reviewedByFullName?: string;
  reviewedAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface Milestone {
  id: number;
  projectId: number;
  name: string;
  dueDate?: string;
  status: 'OPEN' | 'CLOSED';
  createdById?: number;
  createdByName?: string;
  createdAt?: string;
}

export interface TestRunCase {
  id: number;
  runId: number;
  caseId: number;
  code: string;
  title: string;
  precondition?: string;
  steps: string;
  expectedResult: string;
  testData?: string;
  assignedToId?: number;
  assignedToName?: string;
  resultStatus: ResultStatus;
  executedBy?: string;
  executedAt?: string;
  comment?: string;
  defectRef?: string;
  isReviewed?: boolean;
  reviewedById?: number;
  reviewedByName?: string;
  reviewedAt?: string;
  reviewComment?: string;
}

export interface TestRun {
  id: number;
  projectId: number;
  milestoneId?: number | null;
  milestoneName?: string;
  name: string;
  status: 'OPEN' | 'CLOSED';
  createdById?: number;
  createdByName?: string;
  createdAt?: string;
  closedAt?: string;
  totalCases?: number;
  passedCases?: number;
  failedCases?: number;
  blockedCases?: number;
  untestedCases?: number;
  cases?: TestRunCase[];
}

export interface ApiResponse<T> {
  success: boolean;
  code: string;
  message: string;
  data: T;
}
