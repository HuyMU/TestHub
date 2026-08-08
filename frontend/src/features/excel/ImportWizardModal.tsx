import React, { useState } from 'react';
import { Modal, Upload, Button, Table, Tag, Space, Alert, Typography, Tooltip, message, Steps } from 'antd';
import { UploadOutlined, DownloadOutlined, FileExcelOutlined, CheckCircleOutlined, ExclamationCircleOutlined } from '@ant-design/icons';
import * as excelApi from './excelApi';

const { Text, Paragraph } = Typography;

interface ImportWizardModalProps {
  projectId: number;
  open: boolean;
  onCancel: () => void;
  onSuccess: () => void;
}

export const ImportWizardModal: React.FC<ImportWizardModalProps> = ({
  projectId,
  open,
  onCancel,
  onSuccess,
}) => {
  const [currentStep, setCurrentStep] = useState(0);
  const [fileList, setFileList] = useState<any[]>([]);
  const [uploading, setUploading] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const [validateResponse, setValidateResponse] = useState<excelApi.ExcelImportValidateResponse | null>(null);

  const handleDownloadTemplate = async () => {
    try {
      await excelApi.downloadTemplate(projectId);
      message.success('Template downloaded');
    } catch (err: any) {
      message.error('Failed to download template');
    }
  };

  const handleValidateUpload = async () => {
    if (fileList.length === 0) {
      message.warning('Please select an Excel file to upload');
      return;
    }
    setUploading(true);
    try {
      const file = fileList[0].originFileObj || fileList[0];
      const res = await excelApi.validateImport(projectId, file);
      setValidateResponse(res);
      setCurrentStep(1);
      message.success('File validated');
    } catch (err: any) {
      message.error(err?.response?.data?.message || 'Failed to validate Excel file');
    } finally {
      setUploading(false);
    }
  };

  const handleConfirmImport = async () => {
    if (!validateResponse) return;
    setConfirming(true);
    try {
      const res = await excelApi.confirmImport(projectId, validateResponse.importSessionId);
      message.success(`Successfully imported ${res.createdCasesCount} test cases (${res.createdSectionsCount} sections auto-created)`);
      onSuccess();
      handleClose();
    } catch (err: any) {
      message.error(err?.response?.data?.message || 'Failed to confirm import');
    } finally {
      setConfirming(false);
    }
  };

  const handleClose = () => {
    setCurrentStep(0);
    setFileList([]);
    setValidateResponse(null);
    onCancel();
  };

  const columns = [
    {
      title: 'Sheet',
      dataIndex: 'sheetName',
      key: 'sheetName',
      width: 120,
    },
    {
      title: 'Row',
      dataIndex: 'rowNumber',
      key: 'rowNumber',
      width: 70,
    },
    {
      title: 'Subsection Path',
      dataIndex: 'subsectionPath',
      key: 'subsectionPath',
      width: 140,
      render: (val?: string) => val || '-',
    },
    {
      title: 'Title',
      dataIndex: 'title',
      key: 'title',
      ellipsis: true,
    },
    {
      title: 'Status / Errors',
      key: 'errors',
      width: 220,
      render: (_: any, record: excelApi.ExcelImportRow) => {
        if (!record.errors || record.errors.length === 0) {
          return <Tag color="green" icon={<CheckCircleOutlined />}>Valid</Tag>;
        }
        return (
          <Tooltip title={record.errors.join('; ')}>
            <Tag color="red" icon={<ExclamationCircleOutlined />}>
              {record.errors.length} error(s): {record.errors[0]}
            </Tag>
          </Tooltip>
        );
      },
    },
  ];

  return (
    <Modal
      title={
        <Space>
          <FileExcelOutlined style={{ color: '#52c41a' }} />
          <span>Import Test Cases from Excel</span>
        </Space>
      }
      open={open}
      onCancel={handleClose}
      width={800}
      footer={
        currentStep === 0 ? (
          <Space>
            <Button onClick={handleClose}>Cancel</Button>
            <Button
              type="primary"
              icon={<UploadOutlined />}
              onClick={handleValidateUpload}
              loading={uploading}
              disabled={fileList.length === 0}
            >
              Validate & Preview
            </Button>
          </Space>
        ) : (
          <Space>
            <Button onClick={() => setCurrentStep(0)}>Back to Upload</Button>
            <Button
              type="primary"
              onClick={handleConfirmImport}
              loading={confirming}
              disabled={!validateResponse || validateResponse.errorRowsCount > 0}
            >
              Confirm Import ({validateResponse?.totalRows || 0} cases)
            </Button>
          </Space>
        )
      }
      destroyOnClose
    >
      <Steps
        current={currentStep}
        style={{ marginBottom: 24 }}
        items={[
          { title: 'Upload File' },
          { title: 'Validate & Confirm' },
        ]}
      />

      {currentStep === 0 && (
        <div>
          <Paragraph type="secondary">
            Upload an Excel (.xlsx) file with sheet-per-section layout. Download the official blank template below to ensure proper column formatting.
          </Paragraph>

          <Space style={{ marginBottom: 20 }}>
            <Button icon={<DownloadOutlined />} onClick={handleDownloadTemplate}>
              Download Blank Template (.xlsx)
            </Button>
          </Space>

          <Upload.Dragger
            accept=".xlsx"
            maxCount={1}
            fileList={fileList}
            beforeUpload={(file) => {
              setFileList([file]);
              return false;
            }}
            onRemove={() => setFileList([])}
          >
            <p className="ant-upload-drag-icon">
              <FileExcelOutlined style={{ fontSize: 48, color: '#1890ff' }} />
            </p>
            <p className="ant-upload-text">Click or drag Excel file (.xlsx) to this area</p>
            <p className="ant-upload-hint">Only official .xlsx templates are supported.</p>
          </Upload.Dragger>
        </div>
      )}

      {currentStep === 1 && validateResponse && (
        <div>
          {validateResponse.errorRowsCount > 0 ? (
            <Alert
              message={`Validation Failed: ${validateResponse.errorRowsCount} row(s) contain errors`}
              description="Please review the error details in the table below. You must fix all errors in your Excel file and re-upload before confirming import."
              type="error"
              showIcon
              style={{ marginBottom: 16 }}
            />
          ) : (
            <Alert
              message={`Validation Successful: ${validateResponse.totalRows} row(s) ready to import`}
              description="All rows parsed successfully! Clicking 'Confirm Import' will create the test cases and missing section hierarchies."
              type="success"
              showIcon
              style={{ marginBottom: 16 }}
            />
          )}

          <Table
            rowKey={(r) => `${r.sheetName}_${r.rowNumber}`}
            columns={columns}
            dataSource={validateResponse.rows}
            pagination={{ pageSize: 8 }}
            rowClassName={(record) => (record.errors && record.errors.length > 0 ? 'ant-table-row-selected' : '')}
          />
        </div>
      )}
    </Modal>
  );
};
