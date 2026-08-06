import React, { useState } from 'react';
import { Layout, Button, Space, Typography } from 'antd';
import { LogoutOutlined, UserOutlined, KeyOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useAuthStore } from '../store/authStore';
import { MyAccountModal } from '../features/users/MyAccountModal';

const { Header: AntHeader } = Layout;
const { Text } = Typography;

export const Header: React.FC = () => {
  const { t } = useTranslation();
  const { user, logout } = useAuthStore();
  const [accountModalOpen, setAccountModalOpen] = useState(false);

  return (
    <AntHeader
      style={{
        background: '#fff',
        padding: '0 24px',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        borderBottom: '1px solid #f0f0f0',
      }}
    >
      <Text style={{ fontWeight: 500 }}>
        Role: <Text type="secondary">{user?.role || 'TESTER'}</Text>
      </Text>
      <Space size="large">
        <Space>
          <UserOutlined />
          <Text strong>{user?.fullName || user?.username || 'User'}</Text>
        </Space>
        <Button icon={<KeyOutlined />} onClick={() => setAccountModalOpen(true)}>
          {t('app.changePassword')}
        </Button>
        <Button icon={<LogoutOutlined />} danger onClick={logout}>
          {t('app.logout')}
        </Button>
      </Space>

      <MyAccountModal open={accountModalOpen} onClose={() => setAccountModalOpen(false)} />
    </AntHeader>
  );
};
