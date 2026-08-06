import React from 'react';
import { Layout, Button, Space, Typography } from 'antd';
import { LogoutOutlined, UserOutlined } from '@ant-design/icons';
import { useAuthStore } from '../store/authStore';

const { Header: AntHeader } = Layout;
const { Text } = Typography;

export const Header: React.FC = () => {
  const { user, logout } = useAuthStore();

  return (
    <AntHeader style={{ background: '#fff', padding: '0 24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid #f0f0f0' }}>
      <Text style={{ fontWeight: 500 }}>System Role: {user?.role || 'TESTER'}</Text>
      <Space size="large">
        <Space>
          <UserOutlined />
          <Text>{user?.fullName || user?.username || 'User'}</Text>
        </Space>
        <Button icon={<LogoutOutlined />} onClick={logout}>
          Logout
        </Button>
      </Space>
    </AntHeader>
  );
};
