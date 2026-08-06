import React from 'react';
import { Table, Button, Space } from 'antd';
import { PageHeader } from '../../components/PageHeader';

export const ReviewQueuePage: React.FC = () => {
  return (
    <div>
      <PageHeader title="Review Queue (Leader)" />
      <Table
        dataSource={[]}
        columns={[
          { title: 'Code', dataIndex: 'code' },
          { title: 'Title', dataIndex: 'title' },
          {
            title: 'Action',
            key: 'action',
            render: () => (
              <Space>
                <Button type="primary" size="small">Approve (Ready)</Button>
                <Button danger size="small">Reject (Draft)</Button>
              </Space>
            ),
          },
        ]}
        rowKey="id"
      />
    </div>
  );
};
