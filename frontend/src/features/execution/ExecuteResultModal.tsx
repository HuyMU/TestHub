import React, { useState } from 'react';
import {
  Modal,
  Form,
  Radio,
  Input,
  Upload,
  Button,
  message,
  UploadFile,
} from 'antd';
import { UploadOutlined, CheckCircleOutlined, CloseCircleOutlined, MinusCircleOutlined, SyncOutlined } from '@ant-design/icons';
import { TestRunCase } from '../../types';
import * as executionApi from './executionApi';

const { TextArea } = Input;

interface ExecuteResultModalProps {
  runId: number;
  runCase: TestRunCase | null;
  open: boolean;
  onCancel: () => void;
  onSuccess: () => void;
}

export const ExecuteResultModal: React.FC<ExecuteResultModalProps> = ({
  runId,
  runCase,
  open,
  onCancel,
  onSuccess,
}) => {
  const [form] = Form.useForm();
  const [submitting, setSubmitting] = useState(false);
  const [fileList, setFileList] = useState<UploadFile[]>([]);

  if (!runCase) return null;

  const handleFinish = async (values: any) => {
    setSubmitting(true);
    try {
      // 1. Submit execution result first to get executionHistoryId
      const updatedCase: any = await executionApi.recordExecution(runId, runCase.caseId, {
        resultStatus: values.resultStatus,
        comment: values.comment,
        defectRef: values.defectRef,
      });

      const historyId = updatedCase?.latestExecutionHistoryId;

      // 2. Upload selected attachments tied to the executionHistoryId
      if (historyId && fileList.length > 0) {
        for (const fileItem of fileList) {
          const rawFile = fileItem.originFileObj;
          if (rawFile) {
            await executionApi.uploadAttachment('EXECUTION_HISTORY', historyId, rawFile);
          }
        }
      }

      message.success('Execution result recorded successfully');
      form.resetFields();
      setFileList([]);
      onSuccess();
      onCancel();
    } catch (err: any) {
      message.error(err?.response?.data?.message || 'Failed to record execution');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      title={`Execute Case: ${runCase.code} - ${runCase.title}`}
      open={open}
      onCancel={onCancel}
      onOk={() => form.submit()}
      confirmLoading={submitting}
      destroyOnClose
      width={600}
    >
      <Form
        form={form}
        layout="vertical"
        onFinish={handleFinish}
        initialValues={{
          resultStatus: runCase.resultStatus !== 'UNTESTED' ? runCase.resultStatus : 'PASSED',
          comment: runCase.comment,
          defectRef: runCase.defectRef,
        }}
      >
        <Form.Item
          name="resultStatus"
          label="Execution Status"
          rules={[{ required: true, message: 'Please select execution status' }]}
        >
          <Radio.Group buttonStyle="solid">
            <Radio.Button value="PASSED" style={{ color: '#52c41a' }}>
              <CheckCircleOutlined /> Passed
            </Radio.Button>
            <Radio.Button value="FAILED" style={{ color: '#ff4d4f' }}>
              <CloseCircleOutlined /> Failed
            </Radio.Button>
            <Radio.Button value="BLOCKED" style={{ color: '#fa8c16' }}>
              <MinusCircleOutlined /> Blocked
            </Radio.Button>
            <Radio.Button value="RETEST" style={{ color: '#722ed1' }}>
              <SyncOutlined /> Retest
            </Radio.Button>
          </Radio.Group>
        </Form.Item>

        <Form.Item name="comment" label="Comment / Execution Notes">
          <TextArea rows={3} placeholder="Add execution observations, logs, or error details..." />
        </Form.Item>

        <Form.Item name="defectRef" label="Defect Reference (Optional)">
          <Input placeholder="e.g. JIRA-1234 or GitHub #56" />
        </Form.Item>

        <Form.Item label="Attachments (Images / PDF, max 10MB per file)">
          <Upload
            beforeUpload={(file) => {
              if (file.size > 10 * 1024 * 1024) {
                message.error(`${file.name} exceeds 10MB limit`);
                return Upload.LIST_IGNORE;
              }
              const isAllowed = file.type.startsWith('image/') || file.type === 'application/pdf';
              if (!isAllowed) {
                message.error(`${file.name} is not an image or PDF`);
                return Upload.LIST_IGNORE;
              }
              return false; // prevent automatic upload
            }}
            fileList={fileList}
            onChange={({ fileList: newFileList }) => setFileList(newFileList)}
            multiple
            accept="image/*,application/pdf"
          >
            <Button icon={<UploadOutlined />}>
              Select Attachment Files
            </Button>
          </Upload>
        </Form.Item>
      </Form>
    </Modal>
  );
};
