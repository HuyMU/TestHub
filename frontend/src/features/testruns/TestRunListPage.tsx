import React from 'react';
import { Table, Button } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { PageHeader } from '../../components/PageHeader';

export const TestRunListPage: React.FC = () => {
  return (
    <div>
      <PageHeader
        title="Test Runs"
        extra={
          <Button type="primary" icon={<PlusOutlined />}>
            Create Test Run
          </Button>
        }
      />
      <Table dataSource={[]} columns={[{ title: 'Run Name', dataIndex: 'name' }]} rowKey="id" />
    </div>
  );
};
