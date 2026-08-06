import React from 'react';
import { Row, Col, Card, Statistic } from 'antd';
import { PageHeader } from '../../components/PageHeader';

export const DashboardPage: React.FC = () => {
  return (
    <div>
      <PageHeader title="Project Dashboard" />
      <Row gutter={16}>
        <Col span={6}>
          <Card>
            <Statistic title="Total Test Cases" value={0} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="Ready Cases" value={0} valueStyle={{ color: '#3f8600' }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="Review Queue" value={0} valueStyle={{ color: '#1890ff' }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="Passed Executions" value={0} valueStyle={{ color: '#3f8600' }} />
          </Card>
        </Col>
      </Row>
    </div>
  );
};
