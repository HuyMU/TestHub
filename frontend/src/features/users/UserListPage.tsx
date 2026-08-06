import React from 'react';
import { Table, Button } from 'antd';
import { UserAddOutlined } from '@ant-design/icons';
import { PageHeader } from '../../components/PageHeader';

export const UserListPage: React.FC = () => {
  return (
    <div>
      <PageHeader
        title="Tester Accounts (Leader Only)"
        extra={
          <Button type="primary" icon={<UserAddOutlined />}>
            Create Tester
          </Button>
        }
      />
      <Table dataSource={[]} columns={[{ title: 'Username', dataIndex: 'username' }, { title: 'Full Name', dataIndex: 'fullName' }]} rowKey="id" />
    </div>
  );
};
