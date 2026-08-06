import { create } from 'zustand';
import { Section } from '../../types';
import * as sectionApi from './sectionApi';

interface SectionState {
  sections: Section[];
  selectedSectionId: number | null;
  loading: boolean;
  fetchSections: (projectId: number) => Promise<void>;
  selectSection: (id: number | null) => void;
}

export const useSectionStore = create<SectionState>((set) => ({
  sections: [],
  selectedSectionId: null,
  loading: false,

  fetchSections: async (projectId: number) => {
    set({ loading: true });
    try {
      const data = await sectionApi.getSectionTree(projectId);
      set({ sections: data, loading: false });
    } catch (err) {
      set({ loading: false });
      throw err;
    }
  },

  selectSection: (id: number | null) => set({ selectedSectionId: id }),
}));
