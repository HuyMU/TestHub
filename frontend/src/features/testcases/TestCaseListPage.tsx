import React, { useEffect, useState } from 'react';
import { Select, Space, Card, Row, Col, Typography, message } from 'antd';
import { PageHeader } from '../../components/PageHeader';
import axiosClient from '../../api/axiosClient';
import { Project } from '../../types';
import { SectionTree } from '../sections/SectionTree';
import { TestCaseList } from './TestCaseList';
import { useSectionStore } from '../sections/useSectionStore';

const { Text } = Typography;

export const TestCaseListPage: React.FC = () => {
  const [projects, setProjects] = useState<Project[]>([]);
  const [selectedProjectId, setSelectedProjectId] = useState<number | null>(null);
  const [loading, setLoading] = useState(false);
  const { sections, selectedSectionId } = useSectionStore();

  const fetchProjects = async () => {
    setLoading(true);
    try {
      const response: any = await axiosClient.get('/projects');
      if (response && response.success && Array.isArray(response.data)) {
        const activeProjects = response.data.filter((p: Project) => p.status === 'Active');
        setProjects(activeProjects);
        if (activeProjects.length > 0 && !selectedProjectId) {
          setSelectedProjectId(activeProjects[0].id);
        }
      }
    } catch (err: any) {
      message.error(err?.response?.data?.message || 'Failed to fetch projects');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProjects();
  }, []);

  return (
    <div>
      <PageHeader
        title="Test Cases Workbench"
        extra={
          <Space align="center">
            <Text strong>Active Project:</Text>
            <Select
              style={{ width: 240 }}
              placeholder="Select Project"
              value={selectedProjectId}
              onChange={(val) => setSelectedProjectId(val)}
              loading={loading}
            >
              {projects.map((p) => (
                <Select.Option key={p.id} value={p.id}>
                  {p.name}
                </Select.Option>
              ))}
            </Select>
          </Space>
        }
      />

      {selectedProjectId ? (
        <Row gutter={16} style={{ marginTop: 16 }}>
          <Col xs={24} md={8}>
            <SectionTree projectId={selectedProjectId} />
          </Col>
          <Col xs={24} md={16}>
            <TestCaseList
              projectId={selectedProjectId}
              sections={sections}
              selectedSectionId={selectedSectionId}
            />
          </Col>
        </Row>
      ) : (
        <Card style={{ marginTop: 16, textAlign: 'center' }}>
          <Text type="secondary">
            {loading ? 'Loading projects...' : 'No active projects found. Please create a project first.'}
          </Text>
        </Card>
      )}
    </div>
  );
};
