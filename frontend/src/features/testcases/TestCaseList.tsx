import React, { useEffect, useState } from 'react';
import {
  Table,
  Button,
  Input,
  Select,
  Space,
  Tag,
  Badge,
  Popconfirm,
  message,
  Card,
  Tooltip,
  Typography,
  Row,
  Col,
} from 'antd';
import {
  PlusOutlined,
  SearchOutlined,
  EditOutlined,
  DeleteOutlined,
  CopyOutlined,
  SendOutlined,
  EyeOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import { TestCase, Section, Priority, TestType, CaseStatus, AutomationStatus } from '../../types';
import { useAuthStore } from '../../store/authStore';
import { useTestCaseStore } from './useTestCaseStore';
import { TestCaseFormModal } from './TestCaseFormModal';
import * as testCaseApi from './testCaseApi';

const { Text } = Typography;

interface TestCaseListProps {
  projectId: number;
  sections: Section[];
  selectedSectionId?: number | null;
}

export const TestCaseList: React.FC<TestCaseListProps> = ({ projectId, sections, selectedSectionId }) => {
  const { user } = useAuthStore();
  const isLeader = user?.role === 'LEADER';
  const {
    cases,
    loading,
    page,
    pageSize,
    totalElements,
    filters,
    fetchCases,
    setPage,
    setFilters,
    resetFilters,
  } = useTestCaseStore();

  // Modal State
  const [modalOpen, setModalOpen] = useState(false);
  const [modalMode, setModalMode] = useState<'create' | 'edit' | 'view'>('create');
  const [activeCase, setActiveCase] = useState<TestCase | null>(null);
  const [modalLoading, setModalLoading] = useState(false);

  // Sync selectedSectionId from section tree with store filters
  useEffect(() => {
    setFilters({ sectionId: selectedSectionId || undefined }, projectId);
  }, [selectedSectionId, projectId]);

  const handleSearchKeyword = (val: string) => {
    setFilters({ keyword: val || undefined }, projectId);
  };

  const handlePriorityFilter = (val?: Priority) => {
    setFilters({ priority: val }, projectId);
  };

  const handleTypeFilter = (val?: TestType) => {
    setFilters({ type: val }, projectId);
  };

  const handleStatusFilter = (val?: CaseStatus) => {
    setFilters({ status: val }, projectId);
  };

  const handleAutomationFilter = (val?: AutomationStatus) => {
    setFilters({ automationStatus: val }, projectId);
  };

  const handleCreateOpen = () => {
    setModalMode('create');
    setActiveCase(null);
    setModalOpen(true);
  };

  const handleEditOpen = (record: TestCase) => {
    setActiveCase(record);
    const isOwner = record.createdById === user?.id;
    if (!isLeader && (!isOwner || record.status === 'REVIEW')) {
      setModalMode('view');
    } else {
      setModalMode('edit');
    }
    setModalOpen(true);
  };

  const handleModalSubmit = async (values: any) => {
    setModalLoading(true);
    try {
      if (modalMode === 'create') {
        await testCaseApi.createTestCase(projectId, values);
        message.success('Test case created successfully');
      } else if (activeCase) {
        await testCaseApi.updateTestCase(activeCase.id, values);
        message.success('Test case updated successfully');
      }
      setModalOpen(false);
      fetchCases(projectId);
    } catch (err: any) {
      message.error(err?.response?.data?.message || 'Failed to save test case');
    } finally {
      setModalLoading(false);
    }
  };

  const handleSubmitForReview = async (record: TestCase) => {
    try {
      await testCaseApi.submitForReview(record.id);
      message.success(`Test case ${record.code} submitted for review`);
      fetchCases(projectId);
    } catch (err: any) {
      message.error(err?.response?.data?.message || 'Failed to submit test case');
    }
  };

  const handleClone = async (record: TestCase) => {
    try {
      const cloned = await testCaseApi.cloneTestCase(record.id);
      message.success(`Test case cloned as ${cloned.code}`);
      fetchCases(projectId);
    } catch (err: any) {
      message.error(err?.response?.data?.message || 'Failed to clone test case');
    }
  };

  const handleDelete = async (record: TestCase) => {
    try {
      await testCaseApi.deleteTestCase(record.id);
      message.success(`Test case ${record.code} deleted`);
      fetchCases(projectId);
    } catch (err: any) {
      message.error(err?.response?.data?.message || 'Failed to delete test case');
    }
  };

  const getPriorityTag = (p: Priority) => {
    switch (p) {
      case 'CRITICAL':
        return <Tag color="red">Critical</Tag>;
      case 'HIGH':
        return <Tag color="orange">High</Tag>;
      case 'MEDIUM':
        return <Tag color="blue">Medium</Tag>;
      case 'LOW':
      default:
        return <Tag color="default">Low</Tag>;
    }
  };

  const getStatusBadge = (s: CaseStatus) => {
    switch (s) {
      case 'READY':
        return <Badge status="success" text="Ready" />;
      case 'REVIEW':
        return <Badge status="warning" text="Pending Review" />;
      case 'DRAFT':
      default:
        return <Badge status="processing" text="Draft" />;
    }
  };

  const columns = [
    {
      title: 'Code',
      dataIndex: 'code',
      key: 'code',
      width: 100,
      render: (code: string, record: TestCase) => (
        <a onClick={() => handleEditOpen(record)}>
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
        <a onClick={() => handleEditOpen(record)} style={{ color: 'inherit' }}>
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
      render: (priority: Priority) => getPriorityTag(priority),
    },
    {
      title: 'Type',
      dataIndex: 'type',
      key: 'type',
      width: 120,
      render: (type: TestType) => <Tag color="purple">{type}</Tag>,
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      width: 130,
      render: (status: CaseStatus) => getStatusBadge(status),
    },
    {
      title: 'Actions',
      key: 'actions',
      width: 140,
      render: (_: any, record: TestCase) => {
        const isOwner = record.createdById === user?.id;
        const canSubmit = (isLeader || isOwner) && record.status === 'DRAFT';
        const canDelete = isLeader || (isOwner && record.status === 'DRAFT');

        return (
          <Space size="small">
            <Tooltip title={!isLeader && (!isOwner || record.status === 'REVIEW') ? 'View Details' : 'Edit'}>
              <Button
                type="text"
                size="small"
                icon={!isLeader && (!isOwner || record.status === 'REVIEW') ? <EyeOutlined /> : <EditOutlined />}
                onClick={() => handleEditOpen(record)}
              />
            </Tooltip>

            {canSubmit && (
              <Tooltip title="Submit for Review">
                <Button
                  type="text"
                  size="small"
                  icon={<SendOutlined style={{ color: '#1890ff' }} />}
                  onClick={() => handleSubmitForReview(record)}
                />
              </Tooltip>
            )}

            <Tooltip title="Clone Test Case">
              <Button
                type="text"
                size="small"
                icon={<CopyOutlined />}
                onClick={() => handleClone(record)}
              />
            </Tooltip>

            {canDelete && (
              <Popconfirm
                title="Delete Test Case"
                description={`Are you sure you want to delete ${record.code}?`}
                onConfirm={() => handleDelete(record)}
                okText="Delete"
                cancelText="Cancel"
              >
                <Button type="text" size="small" danger icon={<DeleteOutlined />} title="Delete" />
              </Popconfirm>
            )}
          </Space>
        );
      },
    },
  ];

  return (
    <Card
      title={
        <Row justify="space-between" align="middle" style={{ width: '100%' }}>
          <Col>
            <Space>
              <Text strong style={{ fontSize: 16 }}>
                Test Cases
              </Text>
              <Text type="secondary">({totalElements} total)</Text>
            </Space>
          </Col>
          <Col>
            <Button type="primary" icon={<PlusOutlined />} onClick={handleCreateOpen}>
              Add Test Case
            </Button>
          </Col>
        </Row>
      }
    >
      {/* Filter Toolbar */}
      <Row gutter={[8, 8]} style={{ marginBottom: 16 }}>
        <Col xs={24} sm={8} md={6}>
          <Input
            placeholder="Search code or title..."
            prefix={<SearchOutlined />}
            allowClear
            onChange={(e) => handleSearchKeyword(e.target.value)}
          />
        </Col>
        <Col xs={12} sm={4} md={4}>
          <Select
            placeholder="Priority"
            allowClear
            style={{ width: '100%' }}
            onChange={handlePriorityFilter}
            value={filters.priority}
          >
            <Select.Option value="LOW">Low</Select.Option>
            <Select.Option value="MEDIUM">Medium</Select.Option>
            <Select.Option value="HIGH">High</Select.Option>
            <Select.Option value="CRITICAL">Critical</Select.Option>
          </Select>
        </Col>
        <Col xs={12} sm={4} md={4}>
          <Select
            placeholder="Type"
            allowClear
            style={{ width: '100%' }}
            onChange={handleTypeFilter}
            value={filters.type}
          >
            <Select.Option value="FUNCTIONAL">Functional</Select.Option>
            <Select.Option value="REGRESSION">Regression</Select.Option>
            <Select.Option value="SMOKE">Smoke</Select.Option>
            <Select.Option value="PERFORMANCE">Performance</Select.Option>
            <Select.Option value="SECURITY">Security</Select.Option>
            <Select.Option value="USABILITY">Usability</Select.Option>
            <Select.Option value="OTHER">Other</Select.Option>
          </Select>
        </Col>
        <Col xs={12} sm={4} md={4}>
          <Select
            placeholder="Status"
            allowClear
            style={{ width: '100%' }}
            onChange={handleStatusFilter}
            value={filters.status}
          >
            <Select.Option value="DRAFT">Draft</Select.Option>
            <Select.Option value="REVIEW">Pending Review</Select.Option>
            <Select.Option value="READY">Ready</Select.Option>
          </Select>
        </Col>
        <Col xs={12} sm={4} md={4}>
          <Select
            placeholder="Automation"
            allowClear
            style={{ width: '100%' }}
            onChange={handleAutomationFilter}
            value={filters.automationStatus}
          >
            <Select.Option value="MANUAL">Manual</Select.Option>
            <Select.Option value="AUTOMATED">Automated</Select.Option>
            <Select.Option value="TO_AUTOMATE">To Automate</Select.Option>
          </Select>
        </Col>
        <Col xs={12} sm={4} md={2}>
          <Button icon={<ReloadOutlined />} title="Reset Filters" onClick={() => resetFilters(projectId)} />
        </Col>
      </Row>

      {/* Test Cases Table */}
      <Table
        rowKey="id"
        columns={columns}
        dataSource={cases}
        loading={loading}
        pagination={{
          current: page + 1,
          pageSize,
          total: totalElements,
          onChange: (p) => setPage(p - 1, projectId),
          showSizeChanger: false,
        }}
      />

      {/* Create / Edit Modal */}
      <TestCaseFormModal
        open={modalOpen}
        mode={modalMode}
        testCase={activeCase}
        sections={sections}
        currentUser={user}
        defaultSectionId={selectedSectionId}
        loading={modalLoading}
        onCancel={() => setModalOpen(false)}
        onSubmit={handleModalSubmit}
      />
    </Card>
  );
};
