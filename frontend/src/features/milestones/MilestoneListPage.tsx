import React from 'react';
import { Table, Button } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { PageHeader } from '../../components/PageHeader';

export const MilestoneListPage: React.FC = () => {
  return (
    <div>
      <PageHeader
        title="Milestones"
        extra={
          <Button type="primary" icon={<PlusOutlined />}>
            Create Milestone
          </Button>
        }
      />
      <Table dataSource={[]} columns={[{ title: 'Milestone Name', dataIndex: 'name' }, { title: 'Due Date', dataIndex: 'dueDate' }]} rowKey="id" />
    </div>
  );
};
