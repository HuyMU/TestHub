import axiosClient from '../../api/axiosClient';

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
  const response: any = await axiosClient.get('/tokens');
  return response.data;
};

export const generateToken = async (): Promise<ApiTokenCreatedDto> => {
  const response: any = await axiosClient.post('/tokens');
  return response.data;
};

export const revokeToken = async (id: number): Promise<void> => {
  await axiosClient.delete(`/tokens/${id}`);
};
