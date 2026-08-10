import axiosClient from '../../api/axiosClient';
import { ApiResponse } from '../../types';

export interface ApiTokenDto {
  id: number;
  createdByFullName: string;
  createdAt: string;
  lastUsedAt?: string;
  revokedAt?: string;
}

export interface ApiTokenCreatedDto {
  id: number;
  plainTextToken: string;
  createdAt: string;
}

export const listTokens = async (): Promise<ApiTokenDto[]> => {
  const response = await axiosClient.get<ApiResponse<ApiTokenDto[]>>('/api/tokens');
  return response.data.data;
};

export const generateToken = async (): Promise<ApiTokenCreatedDto> => {
  const response = await axiosClient.post<ApiResponse<ApiTokenCreatedDto>>('/api/tokens');
  return response.data.data;
};

export const revokeToken = async (id: number): Promise<void> => {
  await axiosClient.delete<ApiResponse<void>>(`/api/tokens/${id}`);
};
