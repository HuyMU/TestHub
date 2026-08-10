import React, { useState } from 'react';
import { Modal, Form, Input, message } from 'antd';
import { TestRunCase } from '../../types';
import * as executionApi from './executionApi';

const { TextArea } = Input;

interface ReviewResultModalProps {
  runId: number;
  runCase: TestRunCase | null;
  open: boolean;
  onCancel: () => void;
  onSuccess: () => void;
}

export const ReviewResultModal: React.FC<ReviewResultModalProps> = ({
  runId,
  runCase,
  open,
  onCancel,
  onSuccess,
}) => {
  const [form] = Form.useForm();
  const [submitting, setSubmitting] = useState(false);

  if (!runCase) return null;

  const handleFinish = async (values: any) => {
    setSubmitting(true);
    try {
      await executionApi.reviewResult(runId, runCase.caseId, false, values.comment);
      message.success('Retest requested with comment');
      form.resetFields();
      onSuccess();
      onCancel();
    } catch (err: any) {
      message.error(err?.response?.data?.message || 'Failed to request retest');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      title={`Request Retest: ${runCase.code} - ${runCase.title}`}
      open={open}
      onCancel={onCancel}
      onOk={() => form.submit()}
      confirmLoading={submitting}
      destroyOnClose
      okText="Request Retest"
      okButtonProps={{ danger: true }}
    >
      <Form form={form} layout="vertical" onFinish={handleFinish}>
        <Form.Item
          name="comment"
          label="Retest Reason / Comment (Required)"
          rules={[{ required: true, message: 'Please provide a comment explaining the retest request' }]}
        >
          <TextArea rows={4} placeholder="Describe why retest is required..." />
        </Form.Item>
      </Form>
    </Modal>
  );
};
