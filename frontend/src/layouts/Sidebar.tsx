import React from 'react';
import { Layout, Menu } from 'antd';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  DashboardOutlined,
  ProjectOutlined,
  FileTextOutlined,
  AuditOutlined,
  PlaySquareOutlined,
  FlagOutlined,
  UserOutlined,
  KeyOutlined,
} from '@ant-design/icons';
import { useAuthStore } from '../store/authStore';

const { Sider } = Layout;

export const Sidebar: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { user } = useAuthStore();

  const isLeader = user?.role === 'LEADER';

  const menuItems = [
    { key: '/dashboard', icon: <DashboardOutlined />, label: 'Dashboard' },
    { key: '/projects', icon: <ProjectOutlined />, label: 'Projects' },
    { key: '/testcases', icon: <FileTextOutlined />, label: 'Test Cases' },
    ...(isLeader ? [{ key: '/review-queue', icon: <AuditOutlined />, label: 'Review Queue' }] : []),
    { key: '/testruns', icon: <PlaySquareOutlined />, label: 'Test Runs' },
    { key: '/milestones', icon: <FlagOutlined />, label: 'Milestones' },
    ...(isLeader ? [{ key: '/users', icon: <UserOutlined />, label: 'Tester Accounts' }] : []),
    ...(isLeader ? [{ key: '/api-tokens', icon: <KeyOutlined />, label: 'API Tokens' }] : []),
  ];

  return (
    <Sider width={220} theme="dark">
      <div style={{ height: 48, margin: 16, color: '#fff', fontSize: 18, fontWeight: 'bold', textAlign: 'center' }}>
        TestFlow Lite
      </div>
      <Menu
        theme="dark"
        mode="inline"
        selectedKeys={[location.pathname]}
        items={menuItems}
        onClick={({ key }) => navigate(key)}
      />
    </Sider>
  );
};
