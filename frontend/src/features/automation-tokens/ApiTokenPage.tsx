import React, { useEffect, useState } from 'react';
import { Table, Button, Card, Tag, Space, Modal, Input, Alert, message, Popconfirm, Typography } from 'antd';
import { KeyOutlined, CopyOutlined, DeleteOutlined, CheckCircleOutlined, StopOutlined } from '@ant-design/icons';
import { PageHeader } from '../../components/PageHeader';
import { ApiTokenDto, ApiTokenCreatedDto, listTokens, generateToken, revokeToken } from './apiTokenApi';

const { Text, Paragraph } = Typography;

export const ApiTokenPage: React.FC = () => {
  const [tokens, setTokens] = useState<ApiTokenDto[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const [generating, setGenerating] = useState<boolean>(false);
  const [createdToken, setCreatedToken] = useState<ApiTokenCreatedDto | null>(null);
  const [modalOpen, setModalOpen] = useState<boolean>(false);

  const fetchTokens = async () => {
    setLoading(true);
    try {
      const data = await listTokens();
      setTokens(data);
    } catch (err: any) {
      message.error(err?.response?.data?.message || 'Failed to fetch API tokens');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTokens();
  }, []);

  const handleGenerate = async () => {
    setGenerating(true);
    try {
      const newToken = await generateToken();
      setCreatedToken(newToken);
      setModalOpen(true);
      fetchTokens();
    } catch (err: any) {
      message.error(err?.response?.data?.message || 'Failed to generate API token');
    } finally {
      setGenerating(false);
    }
  };

  const handleRevoke = async (id: number) => {
    try {
      await revokeToken(id);
      message.success('API token revoked');
      fetchTokens();
    } catch (err: any) {
      message.error(err?.response?.data?.message || 'Failed to revoke API token');
    }
  };

  const handleCopy = () => {
    if (createdToken?.plainTextToken) {
      navigator.clipboard.writeText(createdToken.plainTextToken);
      message.success('Plaintext token copied to clipboard!');
    }
  };

  const columns = [
    {
      title: 'Token ID',
      dataIndex: 'id',
      key: 'id',
      width: 100,
      render: (id: number) => <Text strong>#{id}</Text>,
    },
    {
      title: 'Created By',
      dataIndex: 'createdByFullName',
      key: 'createdByFullName',
    },
    {
      title: 'Created At',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (val: string) => new Date(val).toLocaleString(),
    },
    {
      title: 'Last Used At',
      dataIndex: 'lastUsedAt',
      key: 'lastUsedAt',
      render: (val?: string) => (val ? new Date(val).toLocaleString() : <Text type="secondary">Never</Text>),
    },
    {
      title: 'Status',
      key: 'status',
      width: 130,
      render: (_: any, record: ApiTokenDto) =>
        record.revokedAt ? (
          <Tag color="default" icon={<StopOutlined />}>Revoked</Tag>
        ) : (
          <Tag color="green" icon={<CheckCircleOutlined />}>Active</Tag>
        ),
    },
    {
      title: 'Actions',
      key: 'actions',
      width: 120,
      render: (_: any, record: ApiTokenDto) =>
        !record.revokedAt ? (
          <Popconfirm
            title="Revoke Token"
            description="Are you sure you want to revoke this API token? Automated test runs using this token will fail."
            onConfirm={() => handleRevoke(record.id)}
            okText="Revoke"
            cancelText="Cancel"
          >
            <Button danger size="small" icon={<DeleteOutlined />}>
              Revoke
            </Button>
          </Popconfirm>
        ) : null,
    },
  ];

  return (
    <div>
      <PageHeader
        title="API Tokens for Automation (Leader Only)"
        extra={
          <Button type="primary" icon={<KeyOutlined />} loading={generating} onClick={handleGenerate}>
            Generate New Token
          </Button>
        }
      />

      <Card style={{ marginTop: 16 }}>
        <Paragraph type="secondary">
          API Tokens allow automated test frameworks (Playwright, Cypress, Selenium, JUnit, pytest) to ingest execution results directly into Test Runs via <code>POST /api/automation/results</code> using the <code>X-API-TOKEN</code> HTTP header.
        </Paragraph>
        <Table
          dataSource={tokens}
          columns={columns}
          rowKey="id"
          loading={loading}
          pagination={{ pageSize: 10 }}
        />
      </Card>

      {/* Generated Token Modal */}
      <Modal
        title="API Token Generated"
        open={modalOpen}
        onOk={() => setModalOpen(false)}
        onCancel={() => setModalOpen(false)}
        cancelButtonProps={{ style: { display: 'none' } }}
        okText="I Have Saved My Token"
      >
        <Alert
          type="warning"
          showIcon
          message="Save your token now!"
          description="This plaintext token will NOT be shown again. Copy it immediately and store it in your CI/CD pipeline secrets."
          style={{ marginBottom: 16 }}
        />

        <div style={{ marginBottom: 16 }}>
          <Text strong>Plaintext Token:</Text>
          <Input.Group compact style={{ marginTop: 8 }}>
            <Input
              style={{ width: 'calc(100% - 90px)' }}
              value={createdToken?.plainTextToken || ''}
              readOnly
            />
            <Button type="primary" icon={<CopyOutlined />} onClick={handleCopy}>
              Copy
            </Button>
          </Input.Group>
        </div>
      </Modal>
    </div>
  );
};
