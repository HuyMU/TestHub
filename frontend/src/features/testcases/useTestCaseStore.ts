import { create } from 'zustand';
import { TestCase } from '../../types';
import * as testCaseApi from './testCaseApi';

interface TestCaseState {
  cases: TestCase[];
  loading: boolean;
  page: number;
  pageSize: number;
  totalElements: number;
  filters: testCaseApi.TestCaseFilterParams;
  fetchCases: (projectId: number) => Promise<void>;
  setPage: (page: number, projectId?: number) => void;
  setFilters: (newFilters: Partial<testCaseApi.TestCaseFilterParams>, projectId?: number) => void;
  resetFilters: (projectId?: number) => void;
}

export const useTestCaseStore = create<TestCaseState>((set, get) => ({
  cases: [],
  loading: false,
  page: 0,
  pageSize: 20,
  totalElements: 0,
  filters: {},

  fetchCases: async (projectId: number) => {
    set({ loading: true });
    try {
      const { page, pageSize, filters } = get();
      const res = await testCaseApi.getTestCases(projectId, {
        ...filters,
        page,
        size: pageSize,
      });
      set({
        cases: res.content,
        totalElements: res.totalElements,
        loading: false,
      });
    } catch (err) {
      set({ loading: false });
      throw err;
    }
  },

  setPage: (page: number, projectId?: number) => {
    set({ page });
    if (projectId) {
      get().fetchCases(projectId);
    }
  },

  setFilters: (newFilters: Partial<testCaseApi.TestCaseFilterParams>, projectId?: number) => {
    const updatedFilters = { ...get().filters, ...newFilters };
    set({ filters: updatedFilters, page: 0 });
    if (projectId) {
      get().fetchCases(projectId);
    }
  },

  resetFilters: (projectId?: number) => {
    set({ filters: {}, page: 0 });
    if (projectId) {
      get().fetchCases(projectId);
    }
  },
}));
