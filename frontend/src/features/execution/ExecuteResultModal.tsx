import React, { useState } from 'react';
import {
  Modal,
  Form,
  Radio,
  Input,
  Upload,
  Button,
  message,
  Space,
  Tag,
  Typography,
} from 'antd';
import { UploadOutlined, CheckCircleOutlined, CloseCircleOutlined, MinusCircleOutlined, SyncOutlined } from '@ant-design/icons';
import { TestRunCase } from '../../types';
import * as executionApi from './executionApi';

const { TextArea } = Input;
const { Text } = Typography;

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
  const [uploading, setUploading] = useState(false);
  const [attachmentUrl, setAttachmentUrl] = useState<string | undefined>(undefined);

  if (!runCase) return null;

  const handleCustomUpload = async (options: any) => {
    const { file, onSuccess: onUploadSuccess, onError } = options;
    if (file.size > 10 * 1024 * 1024) {
      message.error('File size exceeds 10MB limit');
      onError(new Error('File size exceeds 10MB limit'));
      return;
    }

    setUploading(true);
    try {
      const url = await executionApi.uploadAttachment('EXECUTION', runCase.caseId, file);
      setAttachmentUrl(url);
      message.success('Attachment uploaded');
      onUploadSuccess(url);
    } catch (err: any) {
      message.error(err?.response?.data?.message || 'Failed to upload attachment');
      onError(err);
    } finally {
      setUploading(false);
    }
  };

  const handleFinish = async (values: any) => {
    setSubmitting(true);
    try {
      await executionApi.recordExecution(runId, runCase.caseId, {
        resultStatus: values.resultStatus,
        comment: values.comment,
        defectRef: values.defectRef,
        attachmentUrl,
      });

      message.success('Execution result recorded successfully');
      form.resetFields();
      setAttachmentUrl(undefined);
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

        <Form.Item label="Attachment (Image / PDF, max 10MB)">
          <Upload
            customRequest={handleCustomUpload}
            maxCount={1}
            accept="image/*,application/pdf"
            onRemove={() => setAttachmentUrl(undefined)}
          >
            <Button icon={<UploadOutlined />} loading={uploading}>
              Select Attachment File
            </Button>
          </Upload>

          {attachmentUrl && (
            <div style={{ marginTop: 8 }}>
              <Text type="success">Attachment linked: </Text>
              <a href={attachmentUrl} target="_blank" rel="noopener noreferrer">
                {attachmentUrl}
              </a>
            </div>
          )}
        </Form.Item>
      </Form>
    </Modal>
  );
};
