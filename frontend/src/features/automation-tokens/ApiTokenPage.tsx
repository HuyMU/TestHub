import React from 'react';
import { Table, Button } from 'antd';
import { KeyOutlined } from '@ant-design/icons';
import { PageHeader } from '../../components/PageHeader';

export const ApiTokenPage: React.FC = () => {
  return (
    <div>
      <PageHeader
        title="API Tokens for Automation (Leader Only)"
        extra={
          <Button type="primary" icon={<KeyOutlined />}>
            Generate New Token
          </Button>
        }
      />
      <Table dataSource={[]} columns={[{ title: 'Token', dataIndex: 'token' }]} rowKey="id" />
    </div>
  );
};
