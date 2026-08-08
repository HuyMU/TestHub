import { create } from 'zustand';
import { TestRun } from '../../types';
import * as testRunApi from './testRunApi';

interface TestRunState {
  runs: TestRun[];
  currentRun: TestRun | null;
  loading: boolean;
  fetchRuns: (projectId: number) => Promise<void>;
  fetchRunDetail: (runId: number) => Promise<void>;
}

export const useTestRunStore = create<TestRunState>((set) => ({
  runs: [],
  currentRun: null,
  loading: false,
  fetchRuns: async (projectId: number) => {
    set({ loading: true });
    try {
      const data = await testRunApi.getTestRuns(projectId);
      set({ runs: data, loading: false });
    } catch (err) {
      set({ loading: false });
    }
  },
  fetchRunDetail: async (runId: number) => {
    set({ loading: true });
    try {
      const data = await testRunApi.getTestRunDetail(runId);
      set({ currentRun: data, loading: false });
    } catch (err) {
      set({ loading: false });
    }
  },
}));
