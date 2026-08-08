import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Card,
  Row,
  Col,
  Tag,
  Button,
  Table,
  Space,
  Typography,
  Popconfirm,
  message,
  Statistic,
  Badge,
} from 'antd';
import {
  ArrowLeftOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  MinusCircleOutlined,
  ClockCircleOutlined,
  LockOutlined,
  DeleteOutlined,
} from '@ant-design/icons';
import { PageHeader } from '../../components/PageHeader';
import { TestRunCase } from '../../types';
import { useAuthStore } from '../../store/authStore';
import { useTestRunStore } from './useTestRunStore';
import * as testRunApi from './testRunApi';

const { Text, Paragraph } = Typography;

export const TestRunDetailPage: React.FC = () => {
  const { runId } = useParams<{ runId: string }>();
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const isLeader = user?.role === 'LEADER';

  const { currentRun, loading, fetchRunDetail } = useTestRunStore();
  const [closing, setClosing] = useState(false);

  useEffect(() => {
    if (runId) {
      fetchRunDetail(Number(runId));
    }
  }, [runId]);

  const handleCloseRun = async () => {
    if (!currentRun) return;
    setClosing(true);
    try {
      await testRunApi.closeTestRun(currentRun.id);
      message.success('Test Run closed');
      fetchRunDetail(currentRun.id);
    } catch (err: any) {
      message.error(err?.response?.data?.message || 'Failed to close Test Run');
    } finally {
      setClosing(false);
    }
  };

  const handleRemoveCase = async (runCaseId: number) => {
    if (!currentRun) return;
    try {
      await testRunApi.removeCaseFromRun(currentRun.id, runCaseId);
      message.success('Case removed from Test Run');
      fetchRunDetail(currentRun.id);
    } catch (err: any) {
      message.error(err?.response?.data?.message || 'Failed to remove case');
    }
  };

  if (!currentRun && !loading) {
    return (
      <Card style={{ marginTop: 16, textAlign: 'center' }}>
        <Text type="secondary">Test Run not found.</Text>
      </Card>
    );
  }

  const columns = [
    {
      title: 'Code',
      dataIndex: 'code',
      key: 'code',
      width: 90,
      render: (val: string) => <Text strong>{val}</Text>,
    },
    {
      title: 'Title (Snapshot)',
      dataIndex: 'title',
      key: 'title',
      ellipsis: true,
    },
    {
      title: 'Assigned To',
      dataIndex: 'assignedToName',
      key: 'assignedToName',
      width: 140,
      render: (val?: string) => val || <Text type="secondary">Unassigned</Text>,
    },
    {
      title: 'Result Status',
      dataIndex: 'resultStatus',
      key: 'resultStatus',
      width: 130,
      render: (val: string) => {
        switch (val) {
          case 'PASSED':
            return <Tag color="green" icon={<CheckCircleOutlined />}>Passed</Tag>;
          case 'FAILED':
            return <Tag color="red" icon={<CloseCircleOutlined />}>Failed</Tag>;
          case 'BLOCKED':
            return <Tag color="orange" icon={<MinusCircleOutlined />}>Blocked</Tag>;
          case 'RETEST':
            return <Tag color="purple">Retest</Tag>;
          case 'UNTESTED':
          default:
            return <Tag color="default" icon={<ClockCircleOutlined />}>Untested</Tag>;
        }
      },
    },
    ...(isLeader && currentRun?.status === 'OPEN'
      ? [
          {
            title: 'Actions',
            key: 'actions',
            width: 80,
            render: (_: any, record: TestRunCase) => (
              <Popconfirm
                title="Remove Case"
                description="Remove this case from Test Run?"
                onConfirm={() => handleRemoveCase(record.id)}
                okText="Remove"
                cancelText="Cancel"
              >
                <Button type="text" size="small" danger icon={<DeleteOutlined />} title="Remove Case" />
              </Popconfirm>
            ),
          },
        ]
      : []),
  ];

  return (
    <div>
      <PageHeader
        title={
          <Space>
            <Button icon={<ArrowLeftOutlined />} onClick={() => navigate(-1)} />
            <span>{currentRun?.name || 'Test Run Details'}</span>
            {currentRun?.status === 'OPEN' ? (
              <Tag color="green">Open</Tag>
            ) : (
              <Tag color="default" icon={<LockOutlined />}>Closed</Tag>
            )}
          </Space>
        }
        extra={
          isLeader && currentRun?.status === 'OPEN' ? (
            <Popconfirm
              title="Close Test Run"
              description="Are you sure you want to close this Test Run? No further case modifications will be allowed."
              onConfirm={handleCloseRun}
              okText="Close Run"
              cancelText="Cancel"
            >
              <Button type="primary" danger icon={<LockOutlined />} loading={closing}>
                Close Run
              </Button>
            </Popconfirm>
          ) : null
        }
      />

      {/* Metrics Header Cards */}
      <Row gutter={16} style={{ marginTop: 16 }}>
        <Col xs={12} sm={8} md={4}>
          <Card size="small">
            <Statistic title="Total Cases" value={currentRun?.totalCases || 0} />
          </Card>
        </Col>
        <Col xs={12} sm={8} md={4}>
          <Card size="small">
            <Statistic title="Passed" value={currentRun?.passedCases || 0} valueStyle={{ color: '#52c41a' }} />
          </Card>
        </Col>
        <Col xs={12} sm={8} md={4}>
          <Card size="small">
            <Statistic title="Failed" value={currentRun?.failedCases || 0} valueStyle={{ color: '#ff4d4f' }} />
          </Card>
        </Col>
        <Col xs={12} sm={8} md={4}>
          <Card size="small">
            <Statistic title="Blocked" value={currentRun?.blockedCases || 0} valueStyle={{ color: '#fa8c16' }} />
          </Card>
        </Col>
        <Col xs={12} sm={8} md={4}>
          <Card size="small">
            <Statistic title="Untested" value={currentRun?.untestedCases || 0} />
          </Card>
        </Col>
      </Row>

      {/* Case Snapshot Table */}
      <Card
        title="Snapshotted Test Cases"
        style={{ marginTop: 16 }}
        loading={loading}
      >
        <Table
          rowKey="id"
          columns={columns}
          dataSource={currentRun?.cases || []}
          pagination={{ pageSize: 10 }}
          expandable={{
            expandedRowRender: (record) => (
              <div style={{ padding: '8px 16px', background: '#fafafa', borderRadius: 6 }}>
                <Paragraph><strong>Precondition:</strong> {record.precondition || 'None'}</Paragraph>
                <Paragraph><strong>Steps:</strong></Paragraph>
                <pre style={{ background: '#fff', padding: 8, borderRadius: 4 }}>{record.steps}</pre>
                <Paragraph><strong>Expected Result:</strong></Paragraph>
                <pre style={{ background: '#fff', padding: 8, borderRadius: 4 }}>{record.expectedResult}</pre>
                {record.testData && (
                  <Paragraph><strong>Test Data:</strong> {record.testData}</Paragraph>
                )}
              </div>
            ),
          }}
        />
      </Card>
    </div>
  );
};
