import React, { useEffect } from 'react';
import { Modal, Form, Input, Select, Row, Col, Alert, Tag, Space, Typography } from 'antd';
import { TestCase, Section, Priority, TestType, AutomationStatus, User } from '../../types';

const { TextArea } = Input;
const { Text } = Typography;

interface TestCaseFormModalProps {
  open: boolean;
  mode: 'create' | 'edit' | 'view';
  testCase: TestCase | null;
  sections: Section[];
  currentUser: User | null;
  defaultSectionId?: number | null;
  loading: boolean;
  onCancel: () => void;
  onSubmit: (values: any) => Promise<void>;
}

export const TestCaseFormModal: React.FC<TestCaseFormModalProps> = ({
  open,
  mode,
  testCase,
  sections,
  currentUser,
  defaultSectionId,
  loading,
  onCancel,
  onSubmit,
}) => {
  const [form] = Form.useForm();
  const isLeader = currentUser?.role === 'LEADER';
  const isOwner = testCase ? testCase.createdById === currentUser?.id : true;

  // Read-only / Edit lock conditions for Testers
  const isPendingReview = testCase?.status === 'REVIEW';
  const isReady = testCase?.status === 'READY';
  const isReadOnly = mode === 'view' || (!isLeader && (!isOwner || isPendingReview));

  useEffect(() => {
    if (open) {
      form.resetFields();
      if (mode === 'create') {
        form.setFieldsValue({
          sectionId: defaultSectionId || (sections.length > 0 ? sections[0].id : undefined),
          priority: 'MEDIUM',
          type: 'FUNCTIONAL',
          automationStatus: 'MANUAL',
        });
      } else if (testCase) {
        form.setFieldsValue({
          sectionId: testCase.sectionId,
          title: testCase.title,
          precondition: testCase.precondition,
          steps: testCase.steps,
          expectedResult: testCase.expectedResult,
          testData: testCase.testData,
          priority: testCase.priority,
          type: testCase.type,
          automationStatus: testCase.automationStatus,
        });
      }
    }
  }, [open, mode, testCase, defaultSectionId, sections]);

  // Flatten sections list for selector
  const flattenSections = (list: Section[], depth = 0): Array<{ id: number; name: string }> => {
    let result: Array<{ id: number; name: string }> = [];
    for (const item of list) {
      const prefix = '- '.repeat(depth);
      result.push({ id: item.id, name: `${prefix}${item.name}` });
      if (item.children && item.children.length > 0) {
        result = result.concat(flattenSections(item.children, depth + 1));
      }
    }
    return result;
  };

  const sectionOptions = flattenSections(sections);

  const handleFinish = async () => {
    try {
      const values = await form.validateFields();
      await onSubmit(values);
    } catch (err) {
      // validation error
    }
  };

  const getStatusColor = (status?: string) => {
    switch (status) {
      case 'READY':
        return 'green';
      case 'REVIEW':
        return 'orange';
      case 'DRAFT':
      default:
        return 'blue';
    }
  };

  return (
    <Modal
      title={
        <Space>
          <span>
            {mode === 'create'
              ? 'Create Test Case'
              : mode === 'edit'
              ? `Edit Test Case (${testCase?.code})`
              : `Test Case Details (${testCase?.code})`}
          </span>
          {testCase && <Tag color={getStatusColor(testCase.status)}>{testCase.status}</Tag>}
        </Space>
      }
      open={open}
      onCancel={onCancel}
      onOk={isReadOnly ? onCancel : handleFinish}
      okText={isReadOnly ? 'Close' : mode === 'create' ? 'Create' : 'Save Changes'}
      cancelButtonProps={{ style: isReadOnly ? { display: 'none' } : {} }}
      confirmLoading={loading}
      width={720}
      destroyOnClose
    >
      {/* Warning banner for non-owner Testers */}
      {!isLeader && testCase && !isOwner && (
        <Alert
          message="Read-Only Mode"
          description="This test case was created by another user. Only the creator or Leader can edit it."
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
        />
      )}

      {/* Warning banner for locked case in Review */}
      {!isLeader && isOwner && isPendingReview && (
        <Alert
          message="Edit Locked"
          description="This test case is currently submitted for review and cannot be edited. Please wait for the Leader's review."
          type="warning"
          showIcon
          style={{ marginBottom: 16 }}
        />
      )}

      {/* Revert banner for Ready case edited by Tester */}
      {!isLeader && isOwner && isReady && mode === 'edit' && (
        <Alert
          message="Automatic Status Reversion"
          description="Saving changes to this Ready test case will automatically revert its status to Draft for re-review."
          type="warning"
          showIcon
          style={{ marginBottom: 16 }}
        />
      )}

      {/* Review Comment banner if rejected */}
      {testCase?.status === 'DRAFT' && testCase?.reviewComment && (
        <Alert
          message="Rejection Feedback from Leader"
          description={testCase.reviewComment}
          type="error"
          showIcon
          style={{ marginBottom: 16 }}
        />
      )}

      <Form form={form} layout="vertical" disabled={isReadOnly}>
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item
              name="sectionId"
              label="Section"
              rules={[{ required: true, message: 'Please select a section' }]}
            >
              <Select placeholder="Select Section">
                {sectionOptions.map((opt) => (
                  <Select.Option key={opt.id} value={opt.id}>
                    {opt.name}
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item
              name="title"
              label="Title"
              rules={[
                { required: true, message: 'Please enter title' },
                { max: 255, message: 'Title must not exceed 255 characters' },
              ]}
            >
              <Input placeholder="e.g. Verify Login with Valid Credentials" />
            </Form.Item>
          </Col>
        </Row>

        <Row gutter={16}>
          <Col span={8}>
            <Form.Item
              name="priority"
              label="Priority"
              rules={[{ required: true, message: 'Please select priority' }]}
            >
              <Select>
                <Select.Option value="LOW">Low</Select.Option>
                <Select.Option value="MEDIUM">Medium</Select.Option>
                <Select.Option value="HIGH">High</Select.Option>
                <Select.Option value="CRITICAL">Critical</Select.Option>
              </Select>
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item
              name="type"
              label="Type"
              rules={[{ required: true, message: 'Please select test type' }]}
            >
              <Select>
                <Select.Option value="FUNCTIONAL">Functional</Select.Option>
                <Select.Option value="REGRESSION">Regression</Select.Option>
                <Select.Option value="SMOKE">Smoke</Select.Option>
                <Select.Option value="PERFORMANCE">Performance</Select.Option>
                <Select.Option value="SECURITY">Security</Select.Option>
                <Select.Option value="USABILITY">Usability</Select.Option>
                <Select.Option value="OTHER">Other</Select.Option>
              </Select>
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item
              name="automationStatus"
              label="Automation Status"
              rules={[{ required: true, message: 'Please select automation status' }]}
            >
              <Select>
                <Select.Option value="MANUAL">Manual</Select.Option>
                <Select.Option value="AUTOMATED">Automated</Select.Option>
                <Select.Option value="TO_AUTOMATE">To Automate</Select.Option>
              </Select>
            </Form.Item>
          </Col>
        </Row>

        <Form.Item name="precondition" label="Pre-condition (Optional)">
          <TextArea rows={2} placeholder="e.g. User account exists in database and is active" />
        </Form.Item>

        <Form.Item
          name="steps"
          label="Steps"
          rules={[{ required: true, message: 'Please enter execution steps' }]}
        >
          <TextArea
            rows={4}
            placeholder="1. Navigate to login page&#10;2. Enter valid username and password&#10;3. Click Login button"
          />
        </Form.Item>

        <Form.Item
          name="expectedResult"
          label="Expected Result"
          rules={[{ required: true, message: 'Please enter expected result' }]}
        >
          <TextArea
            rows={3}
            placeholder="User is redirected to Dashboard page and welcome message is displayed"
          />
        </Form.Item>

        <Form.Item name="testData" label="Test Data (Optional)">
          <TextArea rows={2} placeholder="Username: testuser@example.com, Password: Password@123" />
        </Form.Item>
      </Form>
    </Modal>
  );
};
