import React, { useEffect, useState } from 'react';
import {
  Modal,
  Form,
  Input,
  Select,
  Switch,
  Table,
  Tag,
  Space,
  Typography,
  message,
} from 'antd';
import { Milestone, TestCase, User } from '../../types';
import axiosClient from '../../api/axiosClient';
import * as testRunApi from './testRunApi';

const { Text } = Typography;

interface CreateTestRunModalProps {
  projectId: number;
  open: boolean;
  milestones: Milestone[];
  onCancel: () => void;
  onSuccess: () => void;
}

export const CreateTestRunModal: React.FC<CreateTestRunModalProps> = ({
  projectId,
  open,
  milestones,
  onCancel,
  onSuccess,
}) => {
  const [form] = Form.useForm();
  const [includeNonReady, setIncludeNonReady] = useState(false);
  const [cases, setCases] = useState<TestCase[]>([]);
  const [projectMembers, setProjectMembers] = useState<User[]>([]);
  const [loadingCases, setLoadingCases] = useState(false);
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const [assignments, setAssignments] = useState<Record<number, number>>({});
  const [submitting, setSubmitting] = useState(false);

  const fetchCasesAndMembers = async () => {
    setLoadingCases(true);
    try {
      // Fetch all cases in project
      const caseRes: any = await axiosClient.get('/cases', {
        params: { projectId, page: 0, size: 500 },
      });
      if (caseRes && caseRes.success && caseRes.data && Array.isArray(caseRes.data.content)) {
        setCases(caseRes.data.content);
      }

      // Fetch project details to get member list
      const projRes: any = await axiosClient.get(`/projects`);
      if (projRes && projRes.success && Array.isArray(projRes.data)) {
        const curProj = projRes.data.find((p: any) => p.id === projectId);
        if (curProj && Array.isArray(curProj.members)) {
          setProjectMembers(curProj.members);
        }
      }
    } catch (err: any) {
      message.error('Failed to load project test cases');
    } finally {
      setLoadingCases(false);
    }
  };

  useEffect(() => {
    if (open) {
      form.resetFields();
      setSelectedRowKeys([]);
      setAssignments({});
      setIncludeNonReady(false);
      fetchCasesAndMembers();
    }
  }, [open, projectId]);

  const filteredCases = cases.filter((c) => (includeNonReady ? true : c.status === 'READY'));

  const handleAssignmentChange = (caseId: number, userId?: number) => {
    setAssignments((prev) => {
      const next = { ...prev };
      if (userId) {
        next[caseId] = userId;
      } else {
        delete next[caseId];
      }
      return next;
    });
  };

  const handleFinish = async (values: any) => {
    if (selectedRowKeys.length === 0) {
      message.warning('Please select at least one test case to add to the Test Run');
      return;
    }

    setSubmitting(true);
    try {
      const casePayload: testRunApi.RunCaseItemPayload[] = selectedRowKeys.map((key) => {
        const cId = Number(key);
        return {
          caseId: cId,
          assignedToId: assignments[cId] || undefined,
        };
      });

      await testRunApi.createTestRun(projectId, {
        name: values.name,
        milestoneId: values.milestoneId || undefined,
        includeNonReady,
        cases: casePayload,
      });

      message.success('Test Run created successfully');
      onSuccess();
      onCancel();
    } catch (err: any) {
      message.error(err?.response?.data?.message || 'Failed to create Test Run');
    } finally {
      setSubmitting(false);
    }
  };

  const columns = [
    {
      title: 'Code',
      dataIndex: 'code',
      key: 'code',
      width: 90,
      render: (val: string) => <Text strong>{val}</Text>,
    },
    {
      title: 'Title',
      dataIndex: 'title',
      key: 'title',
      ellipsis: true,
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: string) => {
        if (status === 'READY') return <Tag color="green">Ready</Tag>;
        if (status === 'REVIEW') return <Tag color="orange">Review</Tag>;
        return <Tag color="blue">Draft</Tag>;
      },
    },
    {
      title: 'Assign To',
      key: 'assignTo',
      width: 180,
      render: (_: any, record: TestCase) => (
        <Select
          placeholder="Unassigned"
          allowClear
          style={{ width: '100%' }}
          value={assignments[record.id]}
          onChange={(val) => handleAssignmentChange(record.id, val)}
        >
          {projectMembers.map((m) => (
            <Select.Option key={m.id} value={m.id}>
              {m.fullName}
            </Select.Option>
          ))}
        </Select>
      ),
    },
  ];

  return (
    <Modal
      title="Create Test Run"
      open={open}
      onCancel={onCancel}
      onOk={() => form.submit()}
      confirmLoading={submitting}
      width={750}
      destroyOnClose
    >
      <Form form={form} layout="vertical" onFinish={handleFinish}>
        <Form.Item
          name="name"
          label="Test Run Name"
          rules={[{ required: true, message: 'Please enter Test Run name' }]}
        >
          <Input placeholder="e.g. Sprint 1 Regression Execution" />
        </Form.Item>

        <Space size="large" style={{ width: '100%', marginBottom: 16 }}>
          <Form.Item name="milestoneId" label="Link Milestone (Optional)" style={{ minWidth: 260, marginBottom: 0 }}>
            <Select placeholder="Select Milestone" allowClear>
              {milestones.map((m) => (
                <Select.Option key={m.id} value={m.id}>
                  {m.name}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item label="Include Draft / Review Cases" style={{ marginBottom: 0 }}>
            <Switch
              checked={includeNonReady}
              onChange={(checked) => setIncludeNonReady(checked)}
            />
          </Form.Item>
        </Space>

        <div style={{ marginTop: 12 }}>
          <Text strong style={{ display: 'block', marginBottom: 8 }}>
            Select Test Cases to Include ({selectedRowKeys.length} selected):
          </Text>

          <Table
            rowKey="id"
            columns={columns}
            dataSource={filteredCases}
            loading={loadingCases}
            pagination={{ pageSize: 5 }}
            rowSelection={{
              selectedRowKeys,
              onChange: (keys) => setSelectedRowKeys(keys),
            }}
          />
        </div>
      </Form>
    </Modal>
  );
};
