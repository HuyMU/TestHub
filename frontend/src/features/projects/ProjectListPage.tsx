import React, { useEffect, useState } from 'react';
import { Table, Button, Modal, Form, Input, Tag, Select, message, Space } from 'antd';
import { PlusOutlined, EditOutlined, FolderOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { PageHeader } from '../../components/PageHeader';
import axiosClient from '../../api/axiosClient';
import { useAuthStore } from '../../store/authStore';
import { Project } from '../../types';

export const ProjectListPage: React.FC = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const isLeader = user?.role === 'LEADER';

  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(false);

  // Modal states
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [createForm] = Form.useForm();
  const [createLoading, setCreateLoading] = useState(false);

  const [editModalOpen, setEditModalOpen] = useState(false);
  const [selectedProject, setSelectedProject] = useState<Project | null>(null);
  const [editForm] = Form.useForm();
  const [editLoading, setEditLoading] = useState(false);

  const fetchProjects = async () => {
    setLoading(true);
    try {
      const response: any = await axiosClient.get('/projects');
      if (response && response.success && Array.isArray(response.data)) {
        setProjects(response.data);
      }
    } catch (err: any) {
      message.error(err?.response?.data?.message || 'Failed to fetch projects');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProjects();
  }, []);

  const handleCreateProject = async () => {
    try {
      const values = await createForm.validateFields();
      setCreateLoading(true);
      const response: any = await axiosClient.post('/projects', values);
      if (response && response.success) {
        message.success(t('project.createdSuccess'));
        createForm.resetFields();
        setCreateModalOpen(false);
        fetchProjects();
      }
    } catch (err: any) {
      if (err?.response?.data?.message) {
        message.error(err.response.data.message);
      }
    } finally {
      setCreateLoading(false);
    }
  };

  const openEditModal = (project: Project, e: React.MouseEvent) => {
    e.stopPropagation();
    setSelectedProject(project);
    editForm.setFieldsValue({
      name: project.name,
      description: project.description,
      status: project.status,
    });
    setEditModalOpen(true);
  };

  const handleEditProject = async () => {
    if (!selectedProject) return;
    try {
      const values = await editForm.validateFields();
      setEditLoading(true);
      const response: any = await axiosClient.put(`/projects/${selectedProject.id}`, values);
      if (response && response.success) {
        message.success(t('project.updatedSuccess'));
        setEditModalOpen(false);
        setSelectedProject(null);
        fetchProjects();
      }
    } catch (err: any) {
      if (err?.response?.data?.message) {
        message.error(err.response.data.message);
      }
    } finally {
      setEditLoading(false);
    }
  };

  const columns = [
    {
      title: t('project.name'),
      dataIndex: 'name',
      key: 'name',
      render: (name: string, record: Project) => (
        <Button
          type="link"
          icon={<FolderOutlined />}
          style={{ padding: 0, fontWeight: 600 }}
          onClick={() => navigate(`/projects/${record.id}`)}
        >
          {name}
        </Button>
      ),
    },
    {
      title: t('project.description'),
      dataIndex: 'description',
      key: 'description',
      render: (desc?: string) => desc || '-',
    },
    {
      title: t('project.status'),
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag color={status === 'Active' ? 'green' : 'default'}>
          {status === 'Active' ? t('project.active') : t('project.archived')}
        </Tag>
      ),
    },
    {
      title: t('project.createdBy'),
      dataIndex: 'createdBy',
      key: 'createdBy',
      render: (createdBy?: any) => createdBy?.fullName || createdBy?.username || '-',
    },
    {
      title: t('project.memberCount'),
      dataIndex: 'memberCount',
      key: 'memberCount',
      render: (count?: number) => count ?? 0,
    },
    ...(isLeader
      ? [
          {
            title: t('project.actions'),
            key: 'actions',
            render: (_: any, record: Project) => (
              <Space>
                <Button
                  icon={<EditOutlined />}
                  size="small"
                  onClick={(e) => openEditModal(record, e)}
                >
                  {t('common.edit')}
                </Button>
              </Space>
            ),
          },
        ]
      : []),
  ];

  return (
    <div>
      <PageHeader
        title={t('project.title')}
        extra={
          isLeader && (
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => setCreateModalOpen(true)}
            >
              {t('project.createProject')}
            </Button>
          )
        }
      />

      <Table
        dataSource={projects}
        columns={columns}
        rowKey="id"
        loading={loading}
        onRow={(record) => ({
          onClick: () => navigate(`/projects/${record.id}`),
          style: { cursor: 'pointer' },
        })}
      />

      {/* Create Project Modal */}
      <Modal
        title={t('project.createProject')}
        open={createModalOpen}
        onCancel={() => setCreateModalOpen(false)}
        onOk={handleCreateProject}
        confirmLoading={createLoading}
        destroyOnClose
      >
        <Form form={createForm} layout="vertical">
          <Form.Item
            name="name"
            label={t('project.name')}
            rules={[
              { required: true, message: t('common.required') },
              { max: 100, message: 'Name must not exceed 100 characters' },
            ]}
          >
            <Input placeholder="Enter project name" />
          </Form.Item>
          <Form.Item name="description" label={t('project.description')}>
            <Input.TextArea rows={4} placeholder="Project description (optional)" />
          </Form.Item>
        </Form>
      </Modal>

      {/* Edit Project Modal */}
      <Modal
        title={t('project.editProject')}
        open={editModalOpen}
        onCancel={() => setEditModalOpen(false)}
        onOk={handleEditProject}
        confirmLoading={editLoading}
        destroyOnClose
      >
        <Form form={editForm} layout="vertical">
          <Form.Item
            name="name"
            label={t('project.name')}
            rules={[
              { required: true, message: t('common.required') },
              { max: 100, message: 'Name must not exceed 100 characters' },
            ]}
          >
            <Input />
          </Form.Item>
          <Form.Item name="description" label={t('project.description')}>
            <Input.TextArea rows={4} />
          </Form.Item>
          <Form.Item
            name="status"
            label={t('project.status')}
            rules={[{ required: true, message: t('common.required') }]}
          >
            <Select>
              <Select.Option value="Active">{t('project.active')}</Select.Option>
              <Select.Option value="Archived">{t('project.archived')}</Select.Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};
