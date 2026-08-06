import React from 'react';
import { Table, Button } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { PageHeader } from '../../components/PageHeader';

export const ProjectListPage: React.FC = () => {
  return (
    <div>
      <PageHeader
        title="Projects"
        extra={
          <Button type="primary" icon={<PlusOutlined />}>
            New Project
          </Button>
        }
      />
      <Table dataSource={[]} columns={[{ title: 'Project Name', dataIndex: 'name' }]} rowKey="id" />
    </div>
  );
};
