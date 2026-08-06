import React, { useEffect, useState } from 'react';
import { Table, Button, Modal, Form, Input, Switch, Tag, message } from 'antd';
import { UserAddOutlined, EditOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { PageHeader } from '../../components/PageHeader';
import axiosClient from '../../api/axiosClient';
import { User } from '../../types';

export const UserListPage: React.FC = () => {
  const { t } = useTranslation();
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(false);

  // Create Modal state
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [createForm] = Form.useForm();
  const [createLoading, setCreateLoading] = useState(false);

  // Edit Modal state
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [editForm] = Form.useForm();
  const [editLoading, setEditLoading] = useState(false);

  const fetchUsers = async () => {
    setLoading(true);
    try {
      const response: any = await axiosClient.get('/users');
      if (response && response.success && Array.isArray(response.data)) {
        setUsers(response.data);
      }
    } catch (err: any) {
      message.error(err?.response?.data?.message || 'Failed to fetch users');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  const handleCreateTester = async () => {
    try {
      const values = await createForm.validateFields();
      setCreateLoading(true);
      const response: any = await axiosClient.post('/users', values);
      if (response && response.success) {
        message.success(t('user.createdSuccess'));
        createForm.resetFields();
        setCreateModalOpen(false);
        fetchUsers();
      }
    } catch (err: any) {
      if (err?.response?.data?.message) {
        message.error(err.response.data.message);
      }
    } finally {
      setCreateLoading(false);
    }
  };

  const openEditModal = (user: User) => {
    setSelectedUser(user);
    editForm.setFieldsValue({
      email: user.email,
      fullName: user.fullName,
      isActive: user.isActive,
    });
    setEditModalOpen(true);
  };

  const handleEditTester = async () => {
    if (!selectedUser) return;
    try {
      const values = await editForm.validateFields();
      setEditLoading(true);
      const response: any = await axiosClient.put(`/users/${selectedUser.id}`, values);
      if (response && response.success) {
        message.success(t('user.updatedSuccess'));
        setEditModalOpen(false);
        setSelectedUser(null);
        fetchUsers();
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
    { title: t('user.username'), dataIndex: 'username', key: 'username' },
    { title: t('user.email'), dataIndex: 'email', key: 'email' },
    { title: t('user.fullName'), dataIndex: 'fullName', key: 'fullName' },
    {
      title: t('user.status'),
      dataIndex: 'isActive',
      key: 'isActive',
      render: (active: boolean) => (
        <Tag color={active ? 'success' : 'error'}>
          {active ? t('user.active') : t('user.inactive')}
        </Tag>
      ),
    },
    {
      title: t('user.actions'),
      key: 'actions',
      render: (_: any, record: User) => (
        <Button
          icon={<EditOutlined />}
          size="small"
          onClick={() => openEditModal(record)}
        >
          {t('user.edit')}
        </Button>
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        title={t('user.title')}
        extra={
          <Button
            type="primary"
            icon={<UserAddOutlined />}
            onClick={() => setCreateModalOpen(true)}
          >
            {t('user.createTester')}
          </Button>
        }
      />

      <Table
        dataSource={users}
        columns={columns}
        rowKey="id"
        loading={loading}
      />

      {/* Create Modal */}
      <Modal
        title={t('user.createTester')}
        open={createModalOpen}
        onCancel={() => setCreateModalOpen(false)}
        onOk={handleCreateTester}
        confirmLoading={createLoading}
        destroyOnClose
      >
        <Form form={createForm} layout="vertical">
          <Form.Item
            name="username"
            label={t('user.username')}
            rules={[
              { required: true, message: t('common.required') },
              { min: 3, max: 50, message: 'Username must be 3-50 characters' },
            ]}
          >
            <Input />
          </Form.Item>
          <Form.Item
            name="email"
            label={t('user.email')}
            rules={[
              { required: true, message: t('common.required') },
              { type: 'email', message: 'Invalid email address' },
            ]}
          >
            <Input />
          </Form.Item>
          <Form.Item
            name="fullName"
            label={t('user.fullName')}
            rules={[{ required: true, message: t('common.required') }]}
          >
            <Input />
          </Form.Item>
          <Form.Item
            name="password"
            label={t('user.password')}
            rules={[
              { required: true, message: t('common.required') },
              { min: 6, message: 'Password must be at least 6 characters' },
            ]}
          >
            <Input.Password />
          </Form.Item>
        </Form>
      </Modal>

      {/* Edit Modal */}
      <Modal
        title={t('user.edit')}
        open={editModalOpen}
        onCancel={() => setEditModalOpen(false)}
        onOk={handleEditTester}
        confirmLoading={editLoading}
        destroyOnClose
      >
        <Form form={editForm} layout="vertical">
          <Form.Item
            name="email"
            label={t('user.email')}
            rules={[
              { required: true, message: t('common.required') },
              { type: 'email', message: 'Invalid email address' },
            ]}
          >
            <Input />
          </Form.Item>
          <Form.Item
            name="fullName"
            label={t('user.fullName')}
            rules={[{ required: true, message: t('common.required') }]}
          >
            <Input />
          </Form.Item>
          <Form.Item
            name="isActive"
            label={t('user.status')}
            valuePropName="checked"
          >
            <Switch checkedChildren={t('user.active')} unCheckedChildren={t('user.inactive')} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};
