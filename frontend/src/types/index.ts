export type Role = 'LEADER' | 'TESTER';

export type Priority = 'Low' | 'Medium' | 'High' | 'Critical';

export type TestType =
  | 'Functional'
  | 'Regression'
  | 'Smoke'
  | 'Performance'
  | 'Security'
  | 'Usability'
  | 'Other';

export type AutomationStatus = 'Manual' | 'Automated' | 'To Automate';

export type CaseStatus = 'Draft' | 'Review' | 'Ready';

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
}

export interface Section {
  id: number;
  projectId: number;
  parentSectionId?: number | null;
  name: string;
  sortOrder: number;
  children?: Section[];
}

export interface TestCase {
  id: number;
  code: string;
  sectionId: number;
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
