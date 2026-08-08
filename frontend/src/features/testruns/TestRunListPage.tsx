import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Table,
  Button,
  Card,
  Space,
  Tag,
  Select,
  Typography,
  Row,
  Col,
  message,
} from 'antd';
import { PlusOutlined, PlayCircleOutlined, LockOutlined, EyeOutlined } from '@ant-design/icons';
import { PageHeader } from '../../components/PageHeader';
import { Milestone, Project, TestRun } from '../../types';
import { useAuthStore } from '../../store/authStore';
import { useTestRunStore } from './useTestRunStore';
import { useMilestoneStore } from '../milestones/useMilestoneStore';
import { CreateTestRunModal } from './CreateTestRunModal';
import axiosClient from '../../api/axiosClient';

const { Text } = Typography;

export const TestRunListPage: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const isLeader = user?.role === 'LEADER';

  const [projects, setProjects] = useState<Project[]>([]);
  const [selectedProjectId, setSelectedProjectId] = useState<number | null>(null);
  const [projectsLoading, setProjectsLoading] = useState(false);

  const { runs, loading, fetchRuns } = useTestRunStore();
  const { milestones, fetchMilestones } = useMilestoneStore();

  const [createModalOpen, setCreateModalOpen] = useState(false);

  const fetchProjects = async () => {
    setProjectsLoading(true);
    try {
      const response: any = await axiosClient.get('/projects');
      if (response && response.success && Array.isArray(response.data)) {
        const activeProjects = response.data.filter((p: Project) => p.status === 'Active');
        setProjects(activeProjects);
        if (activeProjects.length > 0 && !selectedProjectId) {
          setSelectedProjectId(activeProjects[0].id);
        }
      }
    } catch (err: any) {
      message.error(err?.response?.data?.message || 'Failed to fetch projects');
    } finally {
      setProjectsLoading(false);
    }
  };

  useEffect(() => {
    fetchProjects();
  }, []);

  useEffect(() => {
    if (selectedProjectId) {
      fetchRuns(selectedProjectId);
      fetchMilestones(selectedProjectId);
    }
  }, [selectedProjectId]);

  const columns = [
    {
      title: 'Test Run Name',
      dataIndex: 'name',
      key: 'name',
      render: (name: string, record: TestRun) => (
        <a onClick={() => navigate(`/runs/${record.id}`)}>
          <Space>
            <PlayCircleOutlined style={{ color: '#52c41a' }} />
            <Text strong>{name}</Text>
          </Space>
        </a>
      ),
    },
    {
      title: 'Milestone',
      dataIndex: 'milestoneName',
      key: 'milestoneName',
      render: (val?: string) => val || <Text type="secondary">None</Text>,
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: 'OPEN' | 'CLOSED') =>
        status === 'OPEN' ? (
          <Tag color="green">Open</Tag>
        ) : (
          <Tag color="default" icon={<LockOutlined />}>Closed</Tag>
        ),
    },
    {
      title: 'Total Cases',
      dataIndex: 'totalCases',
      key: 'totalCases',
      width: 100,
      render: (count?: number) => count || 0,
    },
    {
      title: 'Created By',
      dataIndex: 'createdByName',
      key: 'createdByName',
      render: (name?: string) => name || '-',
    },
    {
      title: 'Actions',
      key: 'actions',
      width: 100,
      render: (_: any, record: TestRun) => (
        <Button
          type="text"
          size="small"
          icon={<EyeOutlined />}
          onClick={() => navigate(`/runs/${record.id}`)}
        >
          View
        </Button>
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        title="Test Runs Execution"
        extra={
          <Space align="center">
            <Text strong>Active Project:</Text>
            <Select
              style={{ width: 240 }}
              placeholder="Select Project"
              value={selectedProjectId}
              onChange={(val) => setSelectedProjectId(val)}
              loading={projectsLoading}
            >
              {projects.map((p) => (
                <Select.Option key={p.id} value={p.id}>
                  {p.name}
                </Select.Option>
              ))}
            </Select>
          </Space>
        }
      />

      {selectedProjectId ? (
        <Card
          style={{ marginTop: 16 }}
          title={
            <Row justify="space-between" align="middle">
              <Col>
                <Text strong style={{ fontSize: 16 }}>
                  Project Test Runs
                </Text>
              </Col>
              <Col>
                {isLeader && (
                  <Button
                    type="primary"
                    icon={<PlusOutlined />}
                    onClick={() => setCreateModalOpen(true)}
                  >
                    Create Test Run
                  </Button>
                )}
              </Col>
            </Row>
          }
        >
          <Table
            rowKey="id"
            columns={columns}
            dataSource={runs}
            loading={loading}
            pagination={{ pageSize: 10 }}
          />
        </Card>
      ) : (
        <Card style={{ marginTop: 16, textAlign: 'center' }}>
          <Text type="secondary">
            {projectsLoading ? 'Loading projects...' : 'No active projects found. Please select or create a project.'}
          </Text>
        </Card>
      )}

      {selectedProjectId && (
        <CreateTestRunModal
          projectId={selectedProjectId}
          open={createModalOpen}
          milestones={milestones}
          onCancel={() => setCreateModalOpen(false)}
          onSuccess={() => fetchRuns(selectedProjectId)}
        />
      )}
    </div>
  );
};
