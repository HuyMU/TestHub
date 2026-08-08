import React, { useEffect, useState } from 'react';
import {
  Table,
  Button,
  Card,
  Space,
  Tag,
  Select,
  Modal,
  Form,
  Input,
  DatePicker,
  Popconfirm,
  message,
  Typography,
  Row,
  Col,
} from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, FlagOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import { PageHeader } from '../../components/PageHeader';
import { Milestone, Project } from '../../types';
import { useAuthStore } from '../../store/authStore';
import { useMilestoneStore } from './useMilestoneStore';
import * as milestoneApi from './milestoneApi';
import axiosClient from '../../api/axiosClient';

const { Text } = Typography;

export const MilestoneListPage: React.FC = () => {
  const { user } = useAuthStore();
  const isLeader = user?.role === 'LEADER';

  const [projects, setProjects] = useState<Project[]>([]);
  const [selectedProjectId, setSelectedProjectId] = useState<number | null>(null);
  const [projectsLoading, setProjectsLoading] = useState(false);

  const { milestones, loading, fetchMilestones } = useMilestoneStore();

  // Modal state
  const [modalOpen, setModalOpen] = useState(false);
  const [editingMilestone, setEditingMilestone] = useState<Milestone | null>(null);
  const [formLoading, setFormLoading] = useState(false);
  const [form] = Form.useForm();

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
      fetchMilestones(selectedProjectId);
    }
  }, [selectedProjectId]);

  const handleOpenCreate = () => {
    setEditingMilestone(null);
    form.resetFields();
    setModalOpen(true);
  };

  const handleOpenEdit = (record: Milestone) => {
    setEditingMilestone(record);
    form.setFieldsValue({
      name: record.name,
      dueDate: record.dueDate ? dayjs(record.dueDate) : null,
      status: record.status,
    });
    setModalOpen(true);
  };

  const handleFormSubmit = async (values: any) => {
    if (!selectedProjectId) return;
    setFormLoading(true);
    try {
      const payload = {
        name: values.name,
        dueDate: values.dueDate ? values.dueDate.format('YYYY-MM-DD') : undefined,
        status: values.status,
      };

      if (editingMilestone) {
        await milestoneApi.updateMilestone(selectedProjectId, editingMilestone.id, payload);
        message.success('Milestone updated');
      } else {
        await milestoneApi.createMilestone(selectedProjectId, payload);
        message.success('Milestone created');
      }

      setModalOpen(false);
      fetchMilestones(selectedProjectId);
    } catch (err: any) {
      message.error(err?.response?.data?.message || 'Failed to save milestone');
    } finally {
      setFormLoading(false);
    }
  };

  const handleDelete = async (record: Milestone) => {
    if (!selectedProjectId) return;
    try {
      await milestoneApi.deleteMilestone(selectedProjectId, record.id);
      message.success('Milestone deleted');
      fetchMilestones(selectedProjectId);
    } catch (err: any) {
      message.error(err?.response?.data?.message || 'Failed to delete milestone');
    }
  };

  const columns = [
    {
      title: 'Milestone Name',
      dataIndex: 'name',
      key: 'name',
      render: (name: string) => (
        <Space>
          <FlagOutlined style={{ color: '#1890ff' }} />
          <Text strong>{name}</Text>
        </Space>
      ),
    },
    {
      title: 'Due Date',
      dataIndex: 'dueDate',
      key: 'dueDate',
      render: (dueDate?: string) => dueDate || 'No due date',
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status: 'OPEN' | 'CLOSED') =>
        status === 'OPEN' ? <Tag color="blue">Open</Tag> : <Tag color="default">Closed</Tag>,
    },
    {
      title: 'Created By',
      dataIndex: 'createdByName',
      key: 'createdByName',
      render: (name?: string) => name || '-',
    },
    ...(isLeader
      ? [
          {
            title: 'Actions',
            key: 'actions',
            width: 120,
            render: (_: any, record: Milestone) => (
              <Space size="small">
                <Button
                  type="text"
                  size="small"
                  icon={<EditOutlined />}
                  onClick={() => handleOpenEdit(record)}
                />
                <Popconfirm
                  title="Delete Milestone"
                  description="Are you sure you want to delete this milestone?"
                  onConfirm={() => handleDelete(record)}
                  okText="Delete"
                  cancelText="Cancel"
                >
                  <Button type="text" size="small" danger icon={<DeleteOutlined />} />
                </Popconfirm>
              </Space>
            ),
          },
        ]
      : []),
  ];

  return (
    <div>
      <PageHeader
        title="Milestones Management"
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
                  Project Milestones
                </Text>
              </Col>
              <Col>
                {isLeader && (
                  <Button type="primary" icon={<PlusOutlined />} onClick={handleOpenCreate}>
                    Create Milestone
                  </Button>
                )}
              </Col>
            </Row>
          }
        >
          <Table
            rowKey="id"
            columns={columns}
            dataSource={milestones}
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

      {/* Create / Edit Modal */}
      <Modal
        title={editingMilestone ? 'Edit Milestone' : 'Create Milestone'}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={() => form.submit()}
        confirmLoading={formLoading}
        destroyOnClose
      >
        <Form form={form} layout="vertical" onFinish={handleFormSubmit}>
          <Form.Item
            name="name"
            label="Milestone Name"
            rules={[{ required: true, message: 'Please input milestone name' }]}
          >
            <Input placeholder="e.g. Sprint 1 Release" />
          </Form.Item>

          <Form.Item name="dueDate" label="Due Date">
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>

          {editingMilestone && (
            <Form.Item name="status" label="Status" rules={[{ required: true }]}>
              <Select>
                <Select.Option value="OPEN">Open</Select.Option>
                <Select.Option value="CLOSED">Closed</Select.Option>
              </Select>
            </Form.Item>
          )}
        </Form>
      </Modal>
    </div>
  );
};
