import React from 'react';
import { Typography, Space } from 'antd';

const { Title } = Typography;

interface PageHeaderProps {
  title: React.ReactNode;
  extra?: React.ReactNode;
}

export const PageHeader: React.FC<PageHeaderProps> = ({ title, extra }) => {
  return (
    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
      <Title level={3} style={{ margin: 0 }}>{title}</Title>
      {extra && <Space>{extra}</Space>}
    </div>
  );
};
