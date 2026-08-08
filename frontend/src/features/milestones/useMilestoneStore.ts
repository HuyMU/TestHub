import { create } from 'zustand';
import { Milestone } from '../../types';
import * as milestoneApi from './milestoneApi';

interface MilestoneState {
  milestones: Milestone[];
  loading: boolean;
  fetchMilestones: (projectId: number) => Promise<void>;
}

export const useMilestoneStore = create<MilestoneState>((set) => ({
  milestones: [],
  loading: false,
  fetchMilestones: async (projectId: number) => {
    set({ loading: true });
    try {
      const data = await milestoneApi.getMilestones(projectId);
      set({ milestones: data, loading: false });
    } catch (err) {
      set({ loading: false });
    }
  },
}));
