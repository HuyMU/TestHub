import React, { useEffect, useState } from 'react';
import { Table, Button, Card, Tag, Modal, Form, Input, Space, message, Typography, Tooltip, Badge } from 'antd';
import { CheckOutlined, CloseOutlined, EyeOutlined, AuditOutlined } from '@ant-design/icons';
import { PageHeader } from '../../components/PageHeader';
import { TestCase } from '../../types';
import * as testCaseApi from './testCaseApi';
import { TestCaseFormModal } from './TestCaseFormModal';
import { useAuthStore } from '../../store/authStore';

const { Text, Paragraph } = Typography;
const { TextArea } = Input;

export const ReviewQueuePage: React.FC = () => {
  const { user } = useAuthStore();
  const [queue, setQueue] = useState<TestCase[]>([]);
  const [loading, setLoading] = useState(false);

  // View Details Modal
  const [viewModalOpen, setViewModalOpen] = useState(false);
  const [activeCase, setActiveCase] = useState<TestCase | null>(null);

  // Reject Comment Modal
  const [rejectModalOpen, setRejectModalOpen] = useState(false);
  const [rejectCase, setRejectCase] = useState<TestCase | null>(null);
  const [rejectForm] = Form.useForm();
  const [rejectLoading, setRejectLoading] = useState(false);

  const fetchQueue = async () => {
    setLoading(true);
    try {
      const data = await testCaseApi.getReviewQueue();
      setQueue(data);
    } catch (err: any) {
      message.error(err?.response?.data?.message || 'Failed to fetch review queue');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchQueue();
  }, []);

  const handleApprove = async (record: TestCase) => {
    try {
      await testCaseApi.approveTestCase(record.id);
      message.success(`Test case ${record.code} approved successfully`);
      fetchQueue();
    } catch (err: any) {
      message.error(err?.response?.data?.message || 'Failed to approve test case');
    }
  };

  const openRejectModal = (record: TestCase) => {
    setRejectCase(record);
    rejectForm.resetFields();
    setRejectModalOpen(true);
  };

  const handleRejectSubmit = async () => {
    if (!rejectCase) return;
    try {
      const values = await rejectForm.validateFields();
      setRejectLoading(true);
      await testCaseApi.rejectTestCase(rejectCase.id, { reviewComment: values.reviewComment });
      message.success(`Test case ${rejectCase.code} rejected and returned to Draft`);
      setRejectModalOpen(false);
      fetchQueue();
    } catch (err: any) {
      if (err?.response?.data?.message) {
        message.error(err.response.data.message);
      }
    } finally {
      setRejectLoading(false);
    }
  };

  const handleViewOpen = (record: TestCase) => {
    setActiveCase(record);
    setViewModalOpen(true);
  };

  const columns = [
    {
      title: 'Code',
      dataIndex: 'code',
      key: 'code',
      width: 100,
      render: (code: string, record: TestCase) => (
        <a onClick={() => handleViewOpen(record)}>
          <Text strong>{code}</Text>
        </a>
      ),
    },
    {
      title: 'Title',
      dataIndex: 'title',
      key: 'title',
      ellipsis: true,
      render: (title: string, record: TestCase) => (
        <a onClick={() => handleViewOpen(record)} style={{ color: 'inherit' }}>
          {title}
        </a>
      ),
    },
    {
      title: 'Section',
      dataIndex: 'sectionName',
      key: 'sectionName',
      width: 140,
      ellipsis: true,
      render: (name?: string) => name || '-',
    },
    {
      title: 'Priority',
      dataIndex: 'priority',
      key: 'priority',
      width: 100,
      render: (priority: string) => {
        switch (priority) {
          case 'CRITICAL':
            return <Tag color="red">Critical</Tag>;
          case 'HIGH':
            return <Tag color="orange">High</Tag>;
          case 'MEDIUM':
            return <Tag color="blue">Medium</Tag>;
          default:
            return <Tag color="default">Low</Tag>;
        }
      },
    },
    {
      title: 'Creator',
      dataIndex: 'createdByFullName',
      key: 'createdByFullName',
      width: 140,
      render: (name?: string) => name || 'Unknown',
    },
    {
      title: 'Submitted Date',
      dataIndex: 'updatedAt',
      key: 'updatedAt',
      width: 160,
      render: (date?: string) => (date ? new Date(date).toLocaleString() : '-'),
    },
    {
      title: 'Actions',
      key: 'actions',
      width: 150,
      render: (_: any, record: TestCase) => (
        <Space size="small">
          <Tooltip title="View Details">
            <Button
              type="text"
              size="small"
              icon={<EyeOutlined />}
              onClick={() => handleViewOpen(record)}
            />
          </Tooltip>
          <Tooltip title="Approve Case (Ready)">
            <Button
              type="primary"
              size="small"
              icon={<CheckOutlined />}
              onClick={() => handleApprove(record)}
              style={{ backgroundColor: '#52c41a' }}
            >
              Approve
            </Button>
          </Tooltip>
          <Tooltip title="Reject Case (Return to Draft)">
            <Button
              danger
              size="small"
              icon={<CloseOutlined />}
              onClick={() => openRejectModal(record)}
            >
              Reject
            </Button>
          </Tooltip>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        title="Review Queue"
        extra={
          <Space>
            <AuditOutlined style={{ fontSize: 20, color: '#1890ff' }} />
            <Badge count={queue.length} overflowCount={99} color="#fa8c16" />
          </Space>
        }
      />
      <Paragraph type="secondary">
        Test cases submitted by Testers pending Leader review. Approving transitions a case to{' '}
        <Tag color="green">Ready</Tag>. Rejecting returns it to <Tag color="blue">Draft</Tag> with feedback.
      </Paragraph>

      <Card style={{ marginTop: 16 }}>
        <Table
          rowKey="id"
          columns={columns}
          dataSource={queue}
          loading={loading}
          pagination={{ pageSize: 15 }}
        />
      </Card>

      {/* View Details Modal */}
      <TestCaseFormModal
        open={viewModalOpen}
        mode="view"
        testCase={activeCase}
        sections={[]}
        currentUser={user}
        loading={false}
        onCancel={() => setViewModalOpen(false)}
        onSubmit={async () => {}}
      />

      {/* Reject Comment Modal */}
      <Modal
        title={`Reject Test Case (${rejectCase?.code})`}
        open={rejectModalOpen}
        onCancel={() => setRejectModalOpen(false)}
        onOk={handleRejectSubmit}
        confirmLoading={rejectLoading}
        okText="Reject & Return to Draft"
        okButtonProps={{ danger: true }}
        destroyOnClose
      >
        <Paragraph>
          Provide rejection feedback explaining what changes the creator needs to make before re-submitting.
        </Paragraph>
        <Form form={rejectForm} layout="vertical">
          <Form.Item
            name="reviewComment"
            label="Rejection Reason / Feedback"
            rules={[{ required: true, message: 'Please provide rejection feedback' }]}
          >
            <TextArea
              rows={4}
              placeholder="e.g. Expected result is incomplete. Please detail the expected UI validation messages."
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};
