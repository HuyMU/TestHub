import React, { useState } from 'react';
import { Modal, Form, Input, message } from 'antd';
import { useTranslation } from 'react-i18next';
import axiosClient from '../../api/axiosClient';

interface MyAccountModalProps {
  open: boolean;
  onClose: () => void;
}

export const MyAccountModal: React.FC<MyAccountModalProps> = ({ open, onClose }) => {
  const { t } = useTranslation();
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);

      const response: any = await axiosClient.put('/users/me/password', {
        oldPassword: values.oldPassword,
        newPassword: values.newPassword,
      });

      if (response && response.success) {
        message.success(t('user.passwordChanged'));
        form.resetFields();
        onClose();
      }
    } catch (err: any) {
      if (err?.response?.data?.message) {
        message.error(err.response.data.message);
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      open={open}
      title={t('app.changePassword')}
      onCancel={onClose}
      onOk={handleSubmit}
      confirmLoading={loading}
      destroyOnClose
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="oldPassword"
          label={t('user.oldPassword')}
          rules={[{ required: true, message: t('common.required') }]}
        >
          <Input.Password />
        </Form.Item>
        <Form.Item
          name="newPassword"
          label={t('user.newPassword')}
          rules={[
            { required: true, message: t('common.required') },
            { min: 8, message: t('user.passwordMinLength') },
            {
              pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).+$/,
              message: t('user.passwordComplexity'),
            },
          ]}
        >
          <Input.Password />
        </Form.Item>
        <Form.Item
          name="confirmPassword"
          label={t('user.confirmPassword')}
          dependencies={['newPassword']}
          rules={[
            { required: true, message: t('common.required') },
            ({ getFieldValue }) => ({
              validator(_, value) {
                if (!value || getFieldValue('newPassword') === value) {
                  return Promise.resolve();
                }
                return Promise.reject(new Error('Passwords do not match'));
              },
            }),
          ]}
        >
          <Input.Password />
        </Form.Item>
      </Form>
    </Modal>
  );
};
