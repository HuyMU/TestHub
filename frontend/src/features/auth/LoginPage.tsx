import React from 'react';
import { Form, Input, Button, Typography } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import { useAuthStore } from '../../store/authStore';

const { Title } = Typography;

export const LoginPage: React.FC = () => {
  const setAuth = useAuthStore((state) => state.setAuth);

  const onFinish = (values: any) => {
    // TODO: Connect with /api/auth/login endpoint in business task
    setAuth(
      { id: 1, username: values.username, email: 'leader@testhub.com', fullName: 'System Leader', role: 'LEADER', isActive: true },
      'mock-jwt-token'
    );
  };

  return (
    <div>
      <Title level={3} style={{ textAlign: 'center', marginBottom: 24 }}>
        TestFlow Lite Sign In
      </Title>
      <Form name="login" onFinish={onFinish} layout="vertical">
        <Form.Item name="username" rules={[{ required: true, message: 'Please input your Username or Email!' }]}>
          <Input prefix={<UserOutlined />} placeholder="Username or Email" size="large" />
        </Form.Item>
        <Form.Item name="password" rules={[{ required: true, message: 'Please input your Password!' }]}>
          <Input.Password prefix={<LockOutlined />} placeholder="Password" size="large" />
        </Form.Item>
        <Form.Item>
          <Button type="primary" htmlType="submit" size="large" block>
            Sign In
          </Button>
        </Form.Item>
      </Form>
    </div>
  );
};
