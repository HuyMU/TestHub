import axiosClient from '../../api/axiosClient';

export interface ExcelImportRow {
  rowNumber: number;
  sheetName: string;
  subsectionPath?: string;
  title: string;
  precondition?: string;
  steps: string;
  expectedResult: string;
  testData?: string;
  priority?: string;
  type?: string;
  automationStatus?: string;
  errors: string[];
}

export interface ExcelImportValidateResponse {
  importSessionId: string;
  totalRows: number;
  errorRowsCount: number;
  rows: ExcelImportRow[];
}

export interface ExcelImportConfirmResponse {
  createdCasesCount: number;
  createdSectionsCount: number;
  casesPerSheet: Record<string, number>;
}

export const validateImport = async (
  projectId: number,
  file: File
): Promise<ExcelImportValidateResponse> => {
  const formData = new FormData();
  formData.append('file', file);

  const response = await axiosClient.post(
    `/projects/${projectId}/cases/import/validate`,
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }
  );
  return response.data.data;
};

export const confirmImport = async (
  projectId: number,
  importSessionId: string
): Promise<ExcelImportConfirmResponse> => {
  const response = await axiosClient.post(`/projects/${projectId}/cases/import/confirm`, {
    importSessionId,
  });
  return response.data.data;
};

export const downloadTemplate = async (projectId: number): Promise<void> => {
  const response = await axiosClient.get(`/projects/${projectId}/cases/import/template`, {
    responseType: 'blob',
  });

  const blob = new Blob([response.data], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  });
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', 'TestHub_Import_Template.xlsx');
  document.body.appendChild(link);
  link.click();
  link.remove();
};

export const exportCases = async (
  projectId: number,
  sectionIds?: number[]
): Promise<void> => {
  const params: any = {};
  if (sectionIds && sectionIds.length > 0) {
    params.sectionIds = sectionIds.join(',');
  }

  const response = await axiosClient.get(`/projects/${projectId}/cases/export`, {
    params,
    responseType: 'blob',
  });

  const blob = new Blob([response.data], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  });
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', `TestHub_Export_Project_${projectId}.xlsx`);
  document.body.appendChild(link);
  link.click();
  link.remove();
};
