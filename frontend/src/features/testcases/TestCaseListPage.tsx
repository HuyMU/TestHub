import React from 'react';
import { Table, Button, Space } from 'antd';
import { PlusOutlined, UploadOutlined, DownloadOutlined } from '@ant-design/icons';
import { PageHeader } from '../../components/PageHeader';

export const TestCaseListPage: React.FC = () => {
  return (
    <div>
      <PageHeader
        title="Test Cases"
        extra={
          <Space>
            <Button icon={<UploadOutlined />}>Import Excel</Button>
            <Button icon={<DownloadOutlined />}>Export Excel</Button>
            <Button type="primary" icon={<PlusOutlined />}>
              Create Test Case
            </Button>
          </Space>
        }
      />
      <Table dataSource={[]} columns={[{ title: 'Code', dataIndex: 'code' }, { title: 'Title', dataIndex: 'title' }]} rowKey="id" />
    </div>
  );
};
