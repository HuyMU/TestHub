import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Tabs, Card, Table, Button, Modal, Select, Tag, Popconfirm, message, Space, Typography } from 'antd';
import { ArrowLeftOutlined, UserAddOutlined, DeleteOutlined, UserOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { PageHeader } from '../../components/PageHeader';
import axiosClient from '../../api/axiosClient';
import { useAuthStore } from '../../store/authStore';
import { Project, User } from '../../types';
import { SectionTree } from '../sections/SectionTree';

const { Text, Paragraph } = Typography;

export const ProjectDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { t } = useTranslation();
  const { user } = useAuthStore();
  const isLeader = user?.role === 'LEADER';

  const [project, setProject] = useState<Project | null>(null);
  const [members, setMembers] = useState<User[]>([]);
  const [allTesters, setAllTesters] = useState<User[]>([]);
  const [loading, setLoading] = useState(false);
  const [membersLoading, setMembersLoading] = useState(false);

  // Assign Modal
  const [assignModalOpen, setAssignModalOpen] = useState(false);
  const [selectedUserIds, setSelectedUserIds] = useState<number[]>([]);
  const [assignLoading, setAssignLoading] = useState(false);

  const fetchProjectDetails = async () => {
    if (!id) return;
    setLoading(true);
    try {
      const response: any = await axiosClient.get(`/projects/${id}`);
      if (response && response.success) {
        setProject(response.data);
      }
    } catch (err: any) {
      message.error(err?.response?.data?.message || 'Failed to fetch project details');
    } finally {
      setLoading(false);
    }
  };

  const fetchProjectMembers = async () => {
    if (!id) return;
    setMembersLoading(true);
    try {
      const response: any = await axiosClient.get(`/projects/${id}/members`);
      if (response && response.success && Array.isArray(response.data)) {
        setMembers(response.data);
      }
    } catch (err: any) {
      message.error(err?.response?.data?.message || 'Failed to fetch project members');
    } finally {
      setMembersLoading(false);
    }
  };

  const fetchAllTesters = async () => {
    if (!isLeader) return;
    try {
      const response: any = await axiosClient.get('/users');
      if (response && response.success && Array.isArray(response.data)) {
        setAllTesters(response.data.filter((u: User) => u.isActive));
      }
    } catch (err: any) {
      // ignore
    }
  };

  useEffect(() => {
    fetchProjectDetails();
    fetchProjectMembers();
    fetchAllTesters();
  }, [id]);

  const handleAssignMembers = async () => {
    if (!id || selectedUserIds.length === 0) return;
    setAssignLoading(true);
    try {
      const response: any = await axiosClient.post(`/projects/${id}/members`, {
        userIds: selectedUserIds,
      });
      if (response && response.success) {
        message.success(t('project.assignedSuccess'));
        setSelectedUserIds([]);
        setAssignModalOpen(false);
        fetchProjectMembers();
        fetchProjectDetails();
      }
    } catch (err: any) {
      message.error(err?.response?.data?.message || 'Failed to assign members');
    } finally {
      setAssignLoading(false);
    }
  };

  const handleRemoveMember = async (userId: number) => {
    if (!id) return;
    try {
      const response: any = await axiosClient.delete(`/projects/${id}/members/${userId}`);
      if (response && response.success) {
        message.success(t('project.removedSuccess'));
        fetchProjectMembers();
        fetchProjectDetails();
      }
    } catch (err: any) {
      message.error(err?.response?.data?.message || 'Failed to remove member');
    }
  };

  const assignedUserIds = new Set(members.map((m) => m.id));
  const availableTesters = allTesters.filter((t) => !assignedUserIds.has(t.id));

  const memberColumns = [
    { title: t('user.username'), dataIndex: 'username', key: 'username' },
    { title: t('user.fullName'), dataIndex: 'fullName', key: 'fullName' },
    { title: t('user.email'), dataIndex: 'email', key: 'email' },
    {
      title: t('user.status'),
      dataIndex: 'isActive',
      key: 'isActive',
      render: (active: boolean) => (
        <Tag color={active ? 'green' : 'red'}>
          {active ? t('user.active') : t('user.inactive')}
        </Tag>
      ),
    },
    ...(isLeader
      ? [
          {
            title: t('project.actions'),
            key: 'actions',
            render: (_: any, record: User) => (
              <Popconfirm
                title={t('project.removeMemberConfirm')}
                onConfirm={() => handleRemoveMember(record.id)}
                okText={t('common.confirm')}
                cancelText={t('common.cancel')}
              >
                <Button icon={<DeleteOutlined />} danger size="small">
                  {t('project.removeMember')}
                </Button>
              </Popconfirm>
            ),
          },
        ]
      : []),
  ];

  const tabItems = [
    {
      key: 'sections',
      label: t('project.tabs.sections'),
      children: (
        <div style={{ marginTop: 16 }}>
          {id && <SectionTree projectId={Number(id)} />}
        </div>
      ),
    },
    {
      key: 'runs',
      label: t('project.tabs.runs'),
      children: (
        <Card style={{ marginTop: 16 }}>
          <Text type="secondary">Test Runs & Execution module — Coming soon in Slice 6</Text>
        </Card>
      ),
    },
    {
      key: 'milestones',
      label: t('project.tabs.milestones'),
      children: (
        <Card style={{ marginTop: 16 }}>
          <Text type="secondary">Milestones module — Coming soon in Slice 6</Text>
        </Card>
      ),
    },
    {
      key: 'members',
      label: t('project.tabs.members'),
      children: (
        <Card style={{ marginTop: 16 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
            <Space>
              <UserOutlined />
              <Text strong>Assigned Testers ({members.length})</Text>
            </Space>
            {isLeader && (
              <Button
                type="primary"
                icon={<UserAddOutlined />}
                onClick={() => setAssignModalOpen(true)}
              >
                {t('project.assignMembers')}
              </Button>
            )}
          </div>
          <Table
            dataSource={members}
            columns={memberColumns}
            rowKey="id"
            loading={membersLoading}
          />
        </Card>
      ),
    },
  ];

  return (
    <div>
      <Button
        icon={<ArrowLeftOutlined />}
        onClick={() => navigate('/projects')}
        style={{ marginBottom: 16 }}
      >
        Back to Projects
      </Button>

      {project && (
        <Card loading={loading} style={{ marginBottom: 24 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
            <div>
              <PageHeader
                title={project.name}
                extra={
                  <Tag color={project.status === 'Active' ? 'green' : 'default'}>
                    {project.status === 'Active' ? t('project.active') : t('project.archived')}
                  </Tag>
                }
              />
              <Paragraph type="secondary">{project.description || 'No description provided.'}</Paragraph>
            </div>
          </div>
        </Card>
      )}

      <Tabs defaultActiveKey="sections" items={tabItems} />

      {/* Assign Members Modal */}
      <Modal
        title={t('project.assignMembers')}
        open={assignModalOpen}
        onCancel={() => setAssignModalOpen(false)}
        onOk={handleAssignMembers}
        confirmLoading={assignLoading}
        destroyOnClose
      >
        <div style={{ marginBottom: 16 }}>
          <Text>{t('project.selectTesters')}:</Text>
        </div>
        <Select
          mode="multiple"
          style={{ width: '100%' }}
          placeholder="Search and select active Testers"
          value={selectedUserIds}
          onChange={(values) => setSelectedUserIds(values)}
          optionFilterProp="children"
        >
          {availableTesters.map((u) => (
            <Select.Option key={u.id} value={u.id}>
              {u.fullName} ({u.username} - {u.email})
            </Select.Option>
          ))}
        </Select>
      </Modal>
    </div>
  );
};
