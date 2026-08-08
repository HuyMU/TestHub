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

export type ResultStatus = 'Passed' | 'Failed' | 'Blocked' | 'Retest' | 'Untested';

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
  status: 'Open' | 'Closed';
}

export interface TestRun {
  id: number;
  projectId: number;
  milestoneId?: number | null;
  name: string;
  status: 'Open' | 'Closed';
  createdAt: string;
}
