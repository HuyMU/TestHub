import axiosClient from '../../api/axiosClient';
import { Section } from '../../types';

export const getSectionTree = async (projectId: number): Promise<Section[]> => {
  const response: any = await axiosClient.get(`/projects/${projectId}/sections`);
  return response?.data || [];
};

export const createSection = async (
  projectId: number,
  data: { name: string; parentSectionId?: number | null; sortOrder?: number }
): Promise<Section> => {
  const response: any = await axiosClient.post(`/projects/${projectId}/sections`, data);
  return response?.data;
};

export const updateSection = async (
  sectionId: number,
  data: { name: string; parentSectionId?: number | null; sortOrder?: number }
): Promise<Section> => {
  const response: any = await axiosClient.put(`/sections/${sectionId}`, data);
  return response?.data;
};

export const reorderSections = async (
  projectId: number,
  items: Array<{ sectionId: number; sortOrder: number; parentSectionId?: number | null }>
): Promise<void> => {
  await axiosClient.put(`/projects/${projectId}/sections/reorder`, { items });
};

export const deleteSection = async (sectionId: number): Promise<void> => {
  await axiosClient.delete(`/sections/${sectionId}`);
};
