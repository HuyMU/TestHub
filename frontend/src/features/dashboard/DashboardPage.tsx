import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Card, Row, Col, Progress, Table, Tag, Typography, Spin, Alert, Statistic, Space } from 'antd';
import {
  BarChartOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  ClockCircleOutlined,
  MinusCircleOutlined,
  SyncOutlined,
  ProjectOutlined,
  FlagOutlined,
  RiseOutlined,
  CalendarOutlined,
} from '@ant-design/icons';
import { dashboardApi, DashboardDto, MilestoneProgressDto } from './dashboardApi';

const { Title, Text, Paragraph } = Typography;

export const DashboardPage: React.FC = () => {
  const { projectId } = useParams<{ projectId: string }>();
  const [data, setData] = useState<DashboardDto | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!projectId) return;
    const fetchDashboard = async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await dashboardApi.getDashboard(Number(projectId));
        setData(data);
      } catch (err: any) {
        setError(err.response?.data?.message || err.message || 'Failed to load dashboard metrics');
      } finally {
        setLoading(false);
      }
    };

    fetchDashboard();
  }, [projectId]);

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '64px 0' }}>
        <Spin size="large" tip="Loading project dashboard metrics..." />
      </div>
    );
  }

  if (error) {
    return (
      <Alert
        type="error"
        message="Dashboard Error"
        description={error}
        showIcon
        style={{ marginTop: 16 }}
      />
    );
  }

  if (!data) return null;

  const totalExecuted = data.passedCount + data.failedCount + data.blockedCount + data.retestCount;
  const totalRunCases = totalExecuted + data.untestedCount;
  const overallPassRate = totalExecuted > 0 ? Number(((data.passedCount / totalExecuted) * 100).toFixed(1)) : 0;
  const overallCompletionRate = totalRunCases > 0 ? Number(((totalExecuted / totalRunCases) * 100).toFixed(1)) : 0;

  const milestoneColumns = [
    {
      title: 'Milestone Name',
      dataIndex: 'milestoneName',
      key: 'milestoneName',
      render: (val: string) => <Text strong>{val}</Text>,
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      width: 130,
      render: (status: string) => {
        const color = status === 'COMPLETED' ? 'green' : status === 'IN_PROGRESS' ? 'blue' : 'default';
        return <Tag color={color}>{status}</Tag>;
      },
    },
    {
      title: 'Due Date',
      dataIndex: 'dueDate',
      key: 'dueDate',
      width: 140,
      render: (val: string | null) => (val ? new Date(val).toLocaleDateString() : 'No Due Date'),
    },
    {
      title: 'Runs',
      dataIndex: 'totalRuns',
      key: 'totalRuns',
      width: 80,
    },
    {
      title: 'Cases (Completed / Total)',
      key: 'cases',
      width: 220,
      render: (_: any, record: MilestoneProgressDto) => (
        <Space direction="vertical" style={{ width: '100%' }} size={2}>
          <Progress percent={record.progressPercentage} size="small" status="active" />
          <Text type="secondary" style={{ fontSize: 12 }}>
            {record.completedCases} / {record.totalCases} cases ({record.progressPercentage}%)
          </Text>
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: '0 0 24px 0' }}>
      <div style={{ marginBottom: 24 }}>
        <Title level={2} style={{ margin: 0 }}>
          <BarChartOutlined style={{ marginRight: 8, color: '#1890ff' }} />
          Project Dashboard & Statistics
        </Title>
        <Text type="secondary">
          Real-time test repository statistics, execution pass rates, and milestone progress tracking.
        </Text>
      </div>

      {/* Top Metric Cards */}
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} md={6}>
          <Card size="small">
            <Statistic
              title="Repository Test Cases"
              value={data.totalCases}
              prefix={<ProjectOutlined style={{ color: '#1890ff' }} />}
              suffix={
                <Text type="success" style={{ fontSize: 13, marginLeft: 8 }}>
                  ({data.readyCases} Ready)
                </Text>
              }
            />
            <Text type="secondary" style={{ fontSize: 12 }}>
              {data.reviewQueueCount} cases in Review Queue
            </Text>
          </Card>
        </Col>

        <Col xs={24} sm={12} md={6}>
          <Card size="small">
            <Statistic
              title="Pass Rate"
              value={overallPassRate}
              suffix="%"
              valueStyle={{ color: '#52c41a' }}
              prefix={<RiseOutlined />}
            />
            <Progress percent={overallPassRate} strokeColor="#52c41a" showInfo={false} size="small" />
          </Card>
        </Col>

        <Col xs={24} sm={12} md={6}>
          <Card size="small">
            <Statistic
              title="Execution Completion"
              value={overallCompletionRate}
              suffix="%"
              valueStyle={{ color: '#1890ff' }}
              prefix={<CheckCircleOutlined />}
            />
            <Progress percent={overallCompletionRate} strokeColor="#1890ff" showInfo={false} size="small" />
          </Card>
        </Col>

        <Col xs={24} sm={12} md={6}>
          <Card size="small">
            <Statistic
              title="Review Queue (Leader)"
              value={data.reviewQueueCount}
              valueStyle={{ color: '#fa8c16' }}
              prefix={<ClockCircleOutlined />}
            />
            <Text type="secondary" style={{ fontSize: 12 }}>
              Pending Leader Approval
            </Text>
          </Card>
        </Col>
      </Row>

      {/* Test Run Results Breakdown */}
      <Card
        title={
          <span>
            <BarChartOutlined style={{ marginRight: 8, color: '#722ed1' }} />
            Test Run Execution Results Breakdown
          </span>
        }
        style={{ marginTop: 24 }}
      >
        <Row gutter={[16, 16]}>
          <Col xs={12} sm={8} md={4}>
            <Card size="small" style={{ background: '#f6ffed', borderColor: '#b7eb8f', textAlign: 'center' }}>
              <Statistic title="Passed" value={data.passedCount} valueStyle={{ color: '#52c41a' }} prefix={<CheckCircleOutlined />} />
            </Card>
          </Col>
          <Col xs={12} sm={8} md={4}>
            <Card size="small" style={{ background: '#fff2f0', borderColor: '#ffccc7', textAlign: 'center' }}>
              <Statistic title="Failed" value={data.failedCount} valueStyle={{ color: '#ff4d4f' }} prefix={<CloseCircleOutlined />} />
            </Card>
          </Col>
          <Col xs={12} sm={8} md={4}>
            <Card size="small" style={{ background: '#fffbe6', borderColor: '#ffe58f', textAlign: 'center' }}>
              <Statistic title="Blocked" value={data.blockedCount} valueStyle={{ color: '#fa8c16' }} prefix={<MinusCircleOutlined />} />
            </Card>
          </Col>
          <Col xs={12} sm={8} md={4}>
            <Card size="small" style={{ background: '#f9f0ff', borderColor: '#d3adf7', textAlign: 'center' }}>
              <Statistic title="Retest" value={data.retestCount} valueStyle={{ color: '#722ed1' }} prefix={<SyncOutlined />} />
            </Card>
          </Col>
          <Col xs={24} sm={8} md={8}>
            <Card size="small" style={{ background: '#fafafa', borderColor: '#d9d9d9', textAlign: 'center' }}>
              <Statistic title="Untested" value={data.untestedCount} valueStyle={{ color: '#8c8c8c' }} prefix={<ClockCircleOutlined />} />
            </Card>
          </Col>
        </Row>
      </Card>

      {/* Milestone Progress */}
      <Card
        title={
          <span>
            <FlagOutlined style={{ marginRight: 8, color: '#13c2c2' }} />
            Milestone Progress Tracking
          </span>
        }
        style={{ marginTop: 24 }}
      >
        <Table
          rowKey="milestoneId"
          columns={milestoneColumns}
          dataSource={data.milestoneProgress}
          pagination={false}
        />
      </Card>
    </div>
  );
};
