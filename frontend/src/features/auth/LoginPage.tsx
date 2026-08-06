import React, { useState } from 'react';
import { Form, Input, Button, Typography, Alert } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import axiosClient from '../../api/axiosClient';
import { useAuthStore } from '../../store/authStore';

const { Title } = Typography;

export const LoginPage: React.FC = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const setAuth = useAuthStore((state) => state.setAuth);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const onFinish = async (values: any) => {
    setLoading(true);
    setErrorMessage(null);
    try {
      const response: any = await axiosClient.post('/auth/login', {
        usernameOrEmail: values.usernameOrEmail,
        password: values.password,
      });

      if (response && response.success && response.data) {
        const { accessToken, refreshToken, user } = response.data;
        setAuth(user, accessToken, refreshToken);
        navigate('/dashboard', { replace: true });
      } else {
        setErrorMessage(response.message || t('auth.invalidCredentials'));
      }
    } catch (err: any) {
      const msg = err?.response?.data?.message || err?.message || t('auth.invalidCredentials');
      setErrorMessage(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <Title level={3} style={{ textAlign: 'center', marginBottom: 24 }}>
        {t('auth.loginTitle')}
      </Title>

      {errorMessage && (
        <Alert
          message={errorMessage}
          type="error"
          showIcon
          style={{ marginBottom: 16 }}
          closable
          onClose={() => setErrorMessage(null)}
        />
      )}

      <Form name="login" onFinish={onFinish} layout="vertical" disabled={loading}>
        <Form.Item
          name="usernameOrEmail"
          rules={[{ required: true, message: t('common.required') }]}
        >
          <Input prefix={<UserOutlined />} placeholder={t('auth.username')} size="large" />
        </Form.Item>
        <Form.Item
          name="password"
          rules={[{ required: true, message: t('common.required') }]}
        >
          <Input.Password prefix={<LockOutlined />} placeholder={t('auth.password')} size="large" />
        </Form.Item>
        <Form.Item>
          <Button type="primary" htmlType="submit" size="large" block loading={loading}>
            {t('auth.loginButton')}
          </Button>
        </Form.Item>
      </Form>
    </div>
  );
};
